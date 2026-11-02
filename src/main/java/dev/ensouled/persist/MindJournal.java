package dev.ensouled.persist;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.CRC32;

import dev.ensouled.EnsouledMod;

/**
 * V4 §49.3: identity and lifecycle transitions are written before the system reports them complete,
 * and a crash between stages is replayable without duplicate agents, lost memories or false terminal
 * states.
 *
 * <p>One append-only text file beside the snapshot. Every line carries a CRC32 of its own body, so a
 * write that was interrupted mid-line is recognisably incomplete: it is copied aside and truncated
 * rather than parsed as a record. The file is opened {@code rws}, so each transition is on disk
 * before {@link #append} returns; the calls that mint a life or bind a body treat a failed append as
 * a refused transition, never as a warning.
 */
public final class MindJournal implements AutoCloseable {
	public enum Kind {
		/** A new life was minted: the identity exists from this line onward, with or without a snapshot. */
		GENESIS,
		/** A body was attached to an existing life. */
		BIND,
		/** A body was lost (disconnect, death, removal). The mind, and its memory, stay. */
		UNBIND,
		/** The being stopped in the middle of an action; its outcome is indeterminate. */
		INTERRUPT,
		/**
		 * A decision that is about to be acted on. §49.3: it is written before the action exists, so a
		 * process that dies mid-action comes back to the decision rather than to its consequences.
		 */
		DECISION,
		/** How that decision ended: completed, refused, interrupted or recovered. */
		OUTCOME,
		/** A recovering life was reconciled and may act again. */
		RECONCILE,
		/** A snapshot generation was committed to disk. */
		SNAPSHOT,
		/** A continuity condition changed (for example INTEGRITY_BLOCKED at boot). */
		RECOVERY,
		/**
		 * A commitment, plan or objective window changed: what the being owes and what it did about
		 * it. Separate from DECISION because a decision is one action and this outlives it.
		 */
		WORK
	}

	public record Entry(long seq, long atMilli, Kind kind, String agentId, String txn,
			Map<String, String> fields) {

		public String field(String key, String fallback) {
			String value = fields.get(key);
			return value == null ? fallback : value;
		}

		public long lng(String key, long fallback) {
			try {
				return Long.parseLong(field(key, ""));
			} catch (NumberFormatException e) {
				return fallback;
			}
		}

		public String describe() {
			StringBuilder sb = new StringBuilder("#").append(seq).append(' ').append(kind)
					.append(agentId.isEmpty() ? "" : " " + agentId);
			if (!txn.isEmpty()) {
				sb.append(" txn=").append(txn);
			}
			for (Map.Entry<String, String> field : fields.entrySet()) {
				sb.append(' ').append(field.getKey()).append('=').append(field.getValue());
			}
			return sb.toString();
		}
	}

	private final File file;
	private final List<Entry> entries = new ArrayList<>();
	private final Set<String> transactions = new LinkedHashSet<>();
	private RandomAccessFile out;
	private long lastSeq;

	/** The journal the running server opened, for code that has no handle of its own (§49.3). */
	private static volatile MindJournal active;

	public static void install(MindJournal journal) {
		active = journal;
	}

	public static void uninstall(MindJournal journal) {
		if (active == journal) {
			active = null;
		}
	}

	/**
	 * The journal a decision must reach before its action exists. A null journal is not a licence to
	 * act: callers treat "no durable store" as a refusal, because a decision nobody can recover is
	 * not a decision the system may report as taken.
	 */
	public static MindJournal active() {
		return active;
	}

	public MindJournal(File file) {
		this.file = file;
		File parent = file.getParentFile();
		if (parent != null) {
			try {
				Files.createDirectories(parent.toPath());
			} catch (IOException e) {
				EnsouledMod.LOG.error("[ensouled] could not create journal directory {}", parent, e);
			}
		}
		long validEnd = replay();
		try {
			discardIncompleteTail(validEnd);
			this.out = new RandomAccessFile(file, "rws");
			this.out.seek(validEnd);
		} catch (IOException e) {
			// A journal that cannot be written is a journal that cannot promise durability. Every
			// append throws from here on, so no transition is reported complete on a broken record.
			EnsouledMod.LOG.error("[ensouled] journal {} cannot be opened for append", file, e);
			this.out = null;
		}
	}

	public File file() {
		return file;
	}

	/** Records a transition and returns it. A transaction id that is already on disk is not repeated. */
	public synchronized Entry append(Kind kind, String agentId, String txn, Map<String, String> fields) {
		if (!txn.isEmpty()) {
			Entry existing = byTransaction(txn);
			if (existing != null) {
				return existing;
			}
		}
		long seq = ++lastSeq;
		Entry entry = new Entry(seq, System.currentTimeMillis(), kind, agentId, txn, Map.copyOf(fields));
		String body = seq + "\t" + entry.atMilli() + "\t" + kind.name() + "\t" + encode(agentId)
				+ "\t" + encode(txn) + "\t" + encodeFields(entry.fields());
		String line = crc(body) + "\t" + body + "\n";
		if (out == null) {
			throw new IllegalStateException("journal " + file + " is not open; transition not durable");
		}
		try {
			out.write(line.getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new IllegalStateException("journal " + file + " write failed; transition not durable", e);
		}
		entries.add(entry);
		if (!txn.isEmpty()) {
			transactions.add(txn);
		}
		return entry;
	}

	public boolean recorded(String txn) {
		return byTransaction(txn) != null;
	}

	/** Decisions this being wrote ahead that never got an outcome: the ones a crash left open. */
	public synchronized List<Entry> pendingDecisions(String agentId) {
		List<Entry> out = new ArrayList<>();
		for (Entry entry : entries) {
			if (entry.kind() != Kind.DECISION || !entry.agentId().equals(agentId)) {
				continue;
			}
			if (!recorded(outcomeTxn(entry.txn()))) {
				out.add(entry);
			}
		}
		return out;
	}

	/** Closes a decision that was written ahead, from wherever its outcome became known. */
	public synchronized Entry closeDecision(Entry decision, String status, Map<String, String> extra) {
		Map<String, String> fields = new LinkedHashMap<>();
		fields.put("decision", decision.txn());
		fields.put("status", status);
		if (extra != null) {
			fields.putAll(extra);
		}
		return append(Kind.OUTCOME, decision.agentId(), outcomeTxn(decision.txn()), fields);
	}

	private static String outcomeTxn(String decisionTxn) {
		return "outcome:" + decisionTxn;
	}

	public synchronized Entry byTransaction(String txn) {
		if (txn == null || txn.isEmpty()) {
			return null;
		}
		for (int i = entries.size() - 1; i >= 0; i--) {
			if (entries.get(i).txn().equals(txn)) {
				return entries.get(i);
			}
		}
		return null;
	}

	public synchronized List<Entry> entries() {
		return List.copyOf(entries);
	}

	public synchronized List<Entry> recent(int count) {
		int from = Math.max(0, entries.size() - Math.max(0, count));
		return List.copyOf(entries.subList(from, entries.size()));
	}

	public synchronized List<Entry> entriesFor(String agentId) {
		List<Entry> out = new ArrayList<>();
		for (Entry entry : entries) {
			if (entry.agentId().equals(agentId)) {
				out.add(entry);
			}
		}
		return out;
	}

	/** Sequence number of the most recent committed transition, or 0 for a new journal. */
	public synchronized long lastSeq() {
		return lastSeq;
	}

	public synchronized int size() {
		return entries.size();
	}

	@Override
	public synchronized void close() {
		if (out != null) {
			try {
				out.close();
			} catch (IOException e) {
				EnsouledMod.LOG.warn("[ensouled] closing journal {} failed", file, e);
			}
			out = null;
		}
	}

	// ---------------------------------------------------------------- file format

	/** Parses every committed line; returns the byte offset just past the last complete record. */
	private long replay() {
		if (!file.exists()) {
			return 0;
		}
		byte[] bytes;
		try {
			bytes = Files.readAllBytes(file.toPath());
		} catch (IOException e) {
			EnsouledMod.LOG.error("[ensouled] journal {} could not be read", file, e);
			return 0;
		}
		long offset = 0;
		long validEnd = 0;
		long lines = 0;
		while (offset < bytes.length) {
			int end = (int) offset;
			while (end < bytes.length && bytes[end] != '\n') {
				end++;
			}
			if (end >= bytes.length) {
				// No terminating newline: this write never completed, so it is not a record.
				break;
			}
			String line = new String(bytes, (int) offset, end - (int) offset, StandardCharsets.UTF_8);
			Entry entry = parse(line);
			if (entry == null) {
				// A complete-looking line that fails its checksum means the tail is not trustworthy;
				// stop here rather than reading a torn transition as fact.
				break;
			}
			entries.add(entry);
			if (!entry.txn().isEmpty()) {
				transactions.add(entry.txn());
			}
			lastSeq = Math.max(lastSeq, entry.seq());
			validEnd = end + 1;
			offset = end + 1;
			lines++;
		}
		if (validEnd < bytes.length) {
			EnsouledMod.LOG.warn("[ensouled] journal {}: {} byte(s) after the last committed record ignored",
					file, bytes.length - validEnd);
		}
		if (lines > 0) {
			EnsouledMod.LOG.info("[ensouled] journal {} replayed {} transition(s) up to #{}, {} txn(s)",
					file, lines, lastSeq, transactions.size());
		}
		return validEnd;
	}

	/** Keeps the uncommitted bytes as evidence, then removes them from the live journal. */
	private void discardIncompleteTail(long validEnd) throws IOException {
		if (validEnd >= file.length()) {
			return;
		}
		byte[] tail;
		try (RandomAccessFile reader = new RandomAccessFile(file, "r")) {
			reader.seek(validEnd);
			tail = new byte[(int) (reader.length() - validEnd)];
			reader.readFully(tail);
		}
		File evidence = new File(file.getParentFile(), file.getName() + ".tail-" + System.currentTimeMillis());
		Files.write(evidence.toPath(), tail, StandardOpenOption.CREATE_NEW);
		try (RandomAccessFile writer = new RandomAccessFile(file, "rw")) {
			writer.setLength(validEnd);
		}
		EnsouledMod.LOG.warn("[ensouled] journal {}: incomplete tail ({} bytes) preserved as {}",
				file, tail.length, evidence.getName());
	}

	private Entry parse(String line) {
		int tab = line.indexOf('\t');
		if (tab <= 0) {
			return null;
		}
		String expected = line.substring(0, tab);
		String body = line.substring(tab + 1);
		if (!expected.equals(crc(body))) {
			return null;
		}
		String[] parts = body.split("\t", -1);
		if (parts.length != 6) {
			return null;
		}
		long seq;
		long at;
		try {
			seq = Long.parseLong(parts[0]);
			at = Long.parseLong(parts[1]);
		} catch (NumberFormatException e) {
			return null;
		}
		Kind kind;
		try {
			kind = Kind.valueOf(parts[2]);
		} catch (IllegalArgumentException e) {
			return null;
		}
		return new Entry(seq, at, kind, decode(parts[3]), decode(parts[4]), decodeFields(parts[5]));
	}

	private static String crc(String body) {
		CRC32 crc = new CRC32();
		crc.update(body.getBytes(StandardCharsets.UTF_8));
		return Long.toHexString(crc.getValue());
	}

	private static String encode(String value) {
		return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
	}

	private static String decode(String value) {
		try {
			return URLDecoder.decode(value, StandardCharsets.UTF_8);
		} catch (IllegalArgumentException e) {
			return value;
		}
	}

	private static String encodeFields(Map<String, String> fields) {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, String> field : fields.entrySet()) {
			if (sb.length() > 0) {
				sb.append('&');
			}
			sb.append(field.getKey()).append('=').append(encode(field.getValue()));
		}
		return sb.toString();
	}

	private static Map<String, String> decodeFields(String value) {
		Map<String, String> fields = new LinkedHashMap<>();
		if (value.isEmpty()) {
			return fields;
		}
		for (String pair : value.split("&")) {
			int eq = pair.indexOf('=');
			if (eq <= 0) {
				continue;
			}
			fields.put(pair.substring(0, eq), decode(pair.substring(eq + 1)));
		}
		return fields;
	}
}

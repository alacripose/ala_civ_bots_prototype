package dev.ensouled.surface;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * `secure-shell.ledger/1` — the register the stream is announced from.
 *
 * One JSON object per line, a SHA-256 chain over the canonical entry without its own `hash`, and
 * an fsync before the head advances, exactly as `secure-shell` `docs/LEDGER.md` §3 states. The
 * streamer is the only writer; the display reads. A row that arrives is the commit, so a note that
 * is not in the ledger did not happen.
 *
 * A line that cannot be verified makes the ledger read-only rather than repaired: a writer that
 * rewrites a broken chain is a writer that can hide a drop.
 */
public final class StreamLedger {
	public static final String SCHEMA = "secure-shell.ledger/1";
	public static final List<String> KINDS = List.of("baseline", "milestone", "recipe", "record", "patch",
			"test", "build", "mount", "prompt", "convention", "code-change", "retention");
	public static final List<String> OUTCOMES = List.of("committed", "rejected", "pending", "indeterminate",
			"compensated");

	private static final Gson GSON = new Gson();

	public record Entry(int seq, String eventId, String kind, String outcome, String subject, String hash,
			JsonObject json) {
	}

	private final Path path;
	private final String project;
	private final List<Entry> entries = new ArrayList<>();
	private final Map<String, Entry> byOperation = new HashMap<>();
	private final List<Consumer<Entry>> listeners = new CopyOnWriteArrayList<>();
	private final List<String> errors = new ArrayList<>();
	private boolean writable = true;
	private String head = "genesis";

	public StreamLedger(Path path, String project) {
		this.path = path;
		this.project = project;
		load();
	}

	private void load() {
		try {
			if (!Files.exists(path)) {
				if (path.getParent() != null) {
					Files.createDirectories(path.getParent());
				}
				return;
			}
			for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
				if (line.isBlank()) {
					continue;
				}
				JsonObject json;
				try {
					json = JsonParser.parseString(line).getAsJsonObject();
				} catch (RuntimeException malformed) {
					corrupt("line " + (entries.size() + 1) + " is not a JSON object");
					return;
				}
				String stored = json.has("hash") && json.get("hash").isJsonPrimitive() ? json.get("hash").getAsString() : "";
				String expected = SurfaceDocument.sha256(GSON.toJson(body(json)));
				if (!expected.equals(stored)) {
					corrupt("entry " + (entries.size() + 1) + " fails its own digest");
					return;
				}
				String prev = json.has("prev") ? json.get("prev").getAsString() : "";
				if (!prev.equals(head)) {
					corrupt("entry " + (entries.size() + 1) + " does not follow the head");
					return;
				}
				Entry entry = new Entry(json.get("seq").getAsInt(), json.get("event_id").getAsString(),
						json.get("kind").getAsString(), json.get("outcome").getAsString(),
						json.get("subject").getAsString(), stored, json);
				entries.add(entry);
				byOperation.put(json.get("operation_id").getAsString(), entry);
				head = stored;
			}
		} catch (IOException unreadable) {
			corrupt("the ledger could not be read: " + unreadable.getMessage());
		}
	}

	private void corrupt(String why) {
		writable = false;
		errors.add(why);
	}

	private static JsonObject body(JsonObject entry) {
		JsonObject body = new JsonObject();
		for (String field : List.of("schema", "project", "seq", "event_id", "at", "kind", "actor",
				"authority_ref", "subject", "digest", "operation_id", "outcome", "evidence", "detail", "prev")) {
			if (entry.has(field)) {
				body.add(field, entry.get(field));
			}
		}
		return body;
	}

	public synchronized Entry append(String kind, String actor, String subject, String digest, String outcome,
			String operationId, JsonObject evidence, JsonObject detail) {
		if (!writable) {
			throw new SurfaceRefusal("ledger-corrupt", path.getFileName().toString(),
					errors.isEmpty() ? "the chain could not be verified" : errors.get(0));
		}
		if (!KINDS.contains(kind)) {
			throw new SurfaceRefusal("ledger-schema", kind, "not one of " + String.join(", ", KINDS));
		}
		if (!OUTCOMES.contains(outcome)) {
			throw new SurfaceRefusal("ledger-schema", outcome, "not one of " + String.join(", ", OUTCOMES));
		}
		if (operationId == null || operationId.length() < 8 || operationId.length() > 128) {
			throw new SurfaceRefusal("ledger-schema", "operation_id", "an operation id is 8–128 characters");
		}
		Entry existing = byOperation.get(operationId);
		if (existing != null) {
			return existing;
		}
		int seq = entries.size() + 1;
		JsonObject body = new JsonObject();
		body.addProperty("schema", SCHEMA);
		body.addProperty("project", project);
		body.addProperty("seq", seq);
		body.addProperty("event_id", project + ":" + String.format("%06d", seq));
		body.addProperty("at", DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
		body.addProperty("kind", kind);
		body.addProperty("actor", actor);
		body.add("authority_ref", JsonNull.INSTANCE);
		body.addProperty("subject", subject);
		body.addProperty("digest", digest == null ? "-" : digest);
		body.addProperty("operation_id", operationId);
		body.addProperty("outcome", outcome);
		body.add("evidence", evidence == null ? JsonNull.INSTANCE : evidence);
		body.add("detail", detail == null ? JsonNull.INSTANCE : detail);
		body.addProperty("prev", head);
		String hash = SurfaceDocument.sha256(GSON.toJson(body));
		JsonObject line = body.deepCopy();
		line.addProperty("hash", hash);
		String text = GSON.toJson(line);
		try (FileChannel channel = FileChannel.open(path, StandardOpenOption.CREATE,
				StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
			channel.write(ByteBuffer.wrap((text + "\n").getBytes(StandardCharsets.UTF_8)));
			// The fsync return is the commit: the row is not announced until the bytes are on disk.
			channel.force(true);
		} catch (IOException unwritable) {
			throw new SurfaceRefusal("ledger-unavailable", path.toString(), unwritable.getMessage());
		}
		Entry entry = new Entry(seq, body.get("event_id").getAsString(), kind, outcome, subject, hash, line);
		entries.add(entry);
		byOperation.put(operationId, entry);
		head = hash;
		for (Consumer<Entry> listener : listeners) {
			listener.accept(entry);
		}
		return entry;
	}

	public void listen(Consumer<Entry> listener) {
		listeners.add(listener);
	}

	public synchronized List<Entry> entries() {
		return List.copyOf(entries);
	}

	public synchronized Entry last(String kind, String subject) {
		for (int i = entries.size() - 1; i >= 0; i--) {
			Entry entry = entries.get(i);
			if (entry.kind().equals(kind) && (subject == null || entry.subject().equals(subject))) {
				return entry;
			}
		}
		return null;
	}

	public synchronized Entry after(int seq) {
		for (Entry entry : entries) {
			if (entry.seq() > seq) {
				return entry;
			}
		}
		return null;
	}

	public synchronized int headSeq() {
		return entries.size();
	}

	public synchronized String headHash() {
		return head;
	}

	public boolean writable() {
		return writable;
	}

	public List<String> errors() {
		return List.copyOf(errors);
	}

	public Path path() {
		return path;
	}

	public static JsonObject detail(String... pairs) {
		JsonObject detail = new JsonObject();
		for (int i = 0; i + 1 < pairs.length; i += 2) {
			JsonElement value = pairs[i + 1] == null ? JsonNull.INSTANCE : new com.google.gson.JsonPrimitive(pairs[i + 1]);
			detail.add(pairs[i], value);
		}
		return detail;
	}
}

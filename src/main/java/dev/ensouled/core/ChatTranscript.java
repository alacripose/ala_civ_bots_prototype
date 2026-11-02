package dev.ensouled.core;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.ensouled.EnsouledMod;

/**
 * §47.4/§47.5: the chat transcript and its delivery trace.
 *
 * <p>Ordinary player chat is ephemeral by nature — the server broadcasts a line and forgets it. That
 * is not enough for a world of beings whose exchanges have to be inspectable afterwards, so every
 * line that arrives at the accepted chat ingress is written here once, with one row per recipient
 * decision: {@code delivered}, {@code self_excluded} (the echo guard), {@code out_of_earshot}, or
 * {@code unknown_agent}. The transport is named on every row as §47.2 requires, and the file is
 * append-only, so a line that was said cannot later be edited into one that was not.
 *
 * <p>This records ordinary chat that actually arrived as a player-chat packet. It is not a place to
 * write lines the server invented: a bridge or system message is a different transport and does not
 * belong in this transcript.
 */
public final class ChatTranscript {
	public static final String SCHEMA = "ensouled.chat/1";
	public static final String TRANSPORT_PLAYER_CHAT = "PLAYER_CHAT_PACKET";

	/** How many messages a history read returns when the caller does not say. */
	public static final int DEFAULT_HISTORY = 20;

	/**
	 * What the operator gets to see of the tagged fallback mirror. §47.4: this hides the operator's
	 * copy only; a line that reached a player as real chat is not affected, and delivery is not
	 * changed by it.
	 */
	public enum Visibility {
		ALL,
		OFF;

		public static Visibility parse(String word) {
			for (Visibility value : values()) {
				if (value.name().equalsIgnoreCase(word)) {
					return value;
				}
			}
			return null;
		}
	}

	/** One delivery row: the line, who it reached, and what happened to it. */
	public record Row(String messageId, long tick, String speakerId, String speakerName,
			String speakerAgent, String text, String recipientId, String recipientName, String outcome,
			boolean named, double distance, String transport) {
	}

	private static final Object LOCK = new Object();
	private static Path path;
	private static long sequence;
	private static long kept;
	private static volatile Visibility visibility = Visibility.ALL;

	private ChatTranscript() {
	}

	/** Point the transcript at the server's own directory; called once when the server starts. */
	public static void open(Path root) {
		synchronized (LOCK) {
			path = root.resolve("chat").resolve("transcript.jsonl");
			try {
				Files.createDirectories(path.getParent());
			} catch (IOException denied) {
				EnsouledMod.LOG.warn("[ensouled] chat transcript directory is not writable: {}",
						denied.getMessage());
				path = null;
			}
		}
	}

	public static String file() {
		Path current = path;
		return current == null ? "(not open)" : current.toString();
	}

	public static Visibility visibility() {
		return visibility;
	}

	public static void setVisibility(Visibility next) {
		visibility = next == null ? Visibility.ALL : next;
	}

	/**
	 * A new line at the ingress. The id is minted here, once, so every row about this line shares it
	 * and an operator can ask for its trace by that id.
	 */
	public static String nextId(long tick) {
		synchronized (LOCK) {
			return "chat:" + tick + ":" + (++sequence);
		}
	}

	/**
	 * One recipient's delivery outcome. Writing is best-effort and never blocks chat: a transcript
	 * that cannot be written is a loud warning, not a reason to drop somebody's sentence.
	 */
	public static void delivery(String messageId, long tick, String speakerId, String speakerName,
			String speakerAgent, String text, String recipientId, String recipientName, String outcome,
			boolean named, double distance) {
		JsonObject row = new JsonObject();
		row.addProperty("schema", SCHEMA);
		row.addProperty("message_id", messageId);
		row.addProperty("tick", tick);
		row.addProperty("speaker", speakerId == null ? "" : speakerId);
		row.addProperty("speaker_name", speakerName == null ? "" : speakerName);
		row.addProperty("speaker_agent", speakerAgent == null ? "" : speakerAgent);
		row.addProperty("text", text == null ? "" : text);
		row.addProperty("recipient", recipientId == null ? "" : recipientId);
		row.addProperty("recipient_name", recipientName == null ? "" : recipientName);
		row.addProperty("outcome", outcome == null ? "" : outcome);
		row.addProperty("named", named);
		row.addProperty("distance", Math.round(distance * 10) / 10.0);
		row.addProperty("transport", TRANSPORT_PLAYER_CHAT);
		write(row);
	}

	private static void write(JsonObject row) {
		synchronized (LOCK) {
			kept++;
			if (path == null) {
				return;
			}
			try {
				Files.writeString(path, row + System.lineSeparator(), StandardCharsets.UTF_8,
						StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			} catch (IOException failed) {
				// Once per failure burst is enough to be actionable without becoming the log.
				if (kept % 50 == 1) {
					EnsouledMod.LOG.warn("[ensouled] chat transcript write failed: {}", failed.getMessage());
				}
			}
		}
	}

	/** Every row in the transcript, oldest first. A read failure is reported, never repaired. */
	public static List<Row> all() {
		Path current = path;
		if (current == null || !Files.exists(current)) {
			return List.of();
		}
		List<Row> rows = new ArrayList<>();
		try {
			for (String line : Files.readAllLines(current, StandardCharsets.UTF_8)) {
				if (line.isBlank()) {
					continue;
				}
				try {
					rows.add(parse(JsonParser.parseString(line).getAsJsonObject()));
				} catch (RuntimeException malformed) {
					// A damaged row is skipped by name; the rest of the transcript stays readable.
					EnsouledMod.LOG.warn("[ensouled] chat transcript row is unreadable: {}", malformed.getMessage());
				}
			}
		} catch (IOException unreadable) {
			EnsouledMod.LOG.warn("[ensouled] chat transcript is unreadable: {}", unreadable.getMessage());
		}
		return rows;
	}

	private static Row parse(JsonObject json) {
		return new Row(text(json, "message_id"), json.has("tick") ? json.get("tick").getAsLong() : 0L,
				text(json, "speaker"), text(json, "speaker_name"), text(json, "speaker_agent"),
				text(json, "text"), text(json, "recipient"), text(json, "recipient_name"),
				text(json, "outcome"), json.has("named") && json.get("named").getAsBoolean(),
				json.has("distance") ? json.get("distance").getAsDouble() : 0.0,
				text(json, "transport"));
	}

	private static String text(JsonObject json, String key) {
		return json.has(key) && json.get(key).isJsonPrimitive() ? json.get(key).getAsString() : "";
	}

	/** One line per message id, newest last, with its outcome summary — what `history` prints. */
	public static List<Line> lines() {
		Map<String, Line> byId = new LinkedHashMap<>();
		for (Row row : all()) {
			Line line = byId.get(row.messageId());
			if (line == null) {
				line = new Line(row);
				byId.put(row.messageId(), line);
			}
			line.rows.add(row);
		}
		return new ArrayList<>(byId.values());
	}

	/** A line and what happened to it, for the history view and the trace. */
	public static final class Line {
		private final String messageId;
		private final long tick;
		private final String speakerName;
		private final String speakerAgent;
		private final String text;
		private final String transport;
		private final List<Row> rows = new ArrayList<>();

		Line(Row first) {
			this.messageId = first.messageId();
			this.tick = first.tick();
			this.speakerName = first.speakerName();
			this.speakerAgent = first.speakerAgent();
			this.text = first.text();
			this.transport = first.transport();
			this.rows.add(first);
		}

		public String messageId() {
			return messageId;
		}

		public long tick() {
			return tick;
		}

		public String speakerName() {
			return speakerName;
		}

		public String speakerAgent() {
			return speakerAgent;
		}

		public String text() {
			return text;
		}

		public String transport() {
			return transport;
		}

		public List<Row> rows() {
			return List.copyOf(rows);
		}

		/** A line belongs to a mind when its own agent spoke it, or when that mind was a recipient. */
		public boolean involves(String agentId) {
			if (agentId == null || agentId.isBlank()) {
				return true;
			}
			for (Row row : rows) {
				if (agentId.equals(row.speakerAgent()) || agentId.equals(row.recipientId())) {
					return true;
				}
			}
			return false;
		}

		/** One outcome per recipient, for the one-line history view. */
		public String outcomes() {
			StringBuilder out = new StringBuilder();
			for (Row row : rows) {
				String who = row.recipientName().isEmpty() ? row.recipientId() : row.recipientName();
				if (out.length() > 0) {
					out.append(", ");
				}
				out.append(who.isEmpty() ? "nobody" : who).append('=').append(row.outcome());
			}
			return out.toString();
		}
	}
}

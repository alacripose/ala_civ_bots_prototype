package dev.ensouled.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import dev.ensouled.axis.AxisProof;

/**
 * §47.2: one semantic message, one provenance envelope.
 *
 * <p>Speech, an observation and a commitment used to arrive as three hand-rolled shapes: a spoken
 * claim, a bag of counts and coordinates with no sentence in it, and a decision row whose utterance
 * was {@code harvest_tree@12,64,-8}. A reader had to know three formats to read one being, and the
 * being's own lane carried machine tokens where the world's prose belongs — so anything consuming
 * that lane received the tokens as if they were language.
 *
 * <p>This is the one shape they travel in now, carrying the fields §47.2 requires: the message and
 * conversation identity, one authoritative sender, named recipients, channel and spatial scope, the
 * speech act, the utterance, the content hash, the tick window, the delivery state and the transport
 * kind. Everything that is not language — counts, coordinates, ladder tokens, proofs — goes in
 * {@code payload}, which is typed data and is never presented to a reader as a sentence.
 *
 * <p>The utterance is checked against the same baseline that decides whether a being may speak
 * ({@link ProseLogic}). A line the corpus cannot read as a claim is refused here rather than
 * streamed. A lane of half-tokens is exactly the flood this exists to stop.
 */
public final class MessageEnvelope {
	public static final String SCHEMA = "ensouled.message/1";
	/** The transport this row arrived by. A bridge is never presented as native player chat. */
	public static final String TRANSPORT_PLAYER_CHAT = "PLAYER_CHAT_PACKET";
	public static final String TRANSPORT_SELF = "SELF_STREAM";
	public static final String TRANSPORT_BRIDGE = "SERVER_BRIDGE";

	private MessageEnvelope() {
	}

	/** True when this world's baseline reads the line as a claim of a kind it knows. */
	public static boolean isProse(String line) {
		return line != null && ProseLogic.baseline().classify(line).isPresent();
	}

	/**
	 * A message about one being's own state, addressed to itself: the observation and decision lanes
	 * use this, so what a being's stream hands it is always a line it could have said out loud.
	 */
	public static JsonObject self(String agent, String conversationId, String speechAct, String utterance,
			long tick, String dimension, Vec3i where, JsonObject payload) {
		JsonArray recipients = new JsonArray();
		recipients.add(agent);
		return of(agent, agent, conversationId, "", recipients, "SELF", "SELF", speechAct, utterance,
				tick, tick, TRANSPORT_SELF, dimension, where, payload, new JsonArray());
	}

	/**
	 * One envelope. The utterance must classify, so a caller cannot stream a token and call it prose;
	 * {@code content_hash} covers the utterance alone, so a reader can check the words without
	 * trusting the rest of the row.
	 */
	public static JsonObject of(String sender, String senderName, String conversationId,
			String correlationId, JsonArray recipients, String channel, String scope, String speechAct,
			String utterance, long createdTick, long expiryTick, String transportKind, String dimension,
			Vec3i where, JsonObject payload, JsonArray links) {
		if (sender == null || sender.isBlank()) {
			throw new IllegalArgumentException("envelope-sender: an envelope names who sent it");
		}
		if (conversationId == null || conversationId.isBlank()) {
			throw new IllegalArgumentException("envelope-conversation: an envelope belongs to one conversation");
		}
		if (speechAct == null || speechAct.isBlank()) {
			throw new IllegalArgumentException("envelope-speech-act: an envelope declares what it does");
		}
		if (!isProse(utterance)) {
			throw new IllegalArgumentException("envelope-prose: \"" + utterance
					+ "\" is not a claim this world's baseline can read");
		}
		String hash = "sha256:" + AxisProof.digest(AxisProof.utf8(utterance));
		JsonObject envelope = new JsonObject();
		envelope.addProperty("schema", SCHEMA);
		envelope.addProperty("message_id", conversationId + ":" + hash.substring(7, 23));
		envelope.addProperty("conversation_id", conversationId);
		envelope.addProperty("correlation_id", correlationId == null ? "" : correlationId);
		envelope.addProperty("sender", sender);
		envelope.addProperty("sender_name", senderName == null ? sender : senderName);
		envelope.add("recipients", recipients == null ? new JsonArray() : recipients);
		envelope.addProperty("channel", channel == null ? "SELF" : channel);
		envelope.addProperty("scope", scope == null ? "SELF" : scope);
		envelope.addProperty("speech_act", speechAct);
		envelope.addProperty("utterance", utterance);
		envelope.addProperty("content_hash", hash);
		envelope.addProperty("created_tick", createdTick);
		envelope.addProperty("expiry_tick", expiryTick);
		envelope.addProperty("delivery_state", "SELF_HELD");
		envelope.addProperty("transport_kind", transportKind == null ? TRANSPORT_SELF : transportKind);
		envelope.addProperty("dimension", dimension == null ? "minecraft:overworld" : dimension);
		envelope.addProperty("position", where == null ? "" : where.toString());
		envelope.add("links", links == null ? new JsonArray() : links);
		envelope.add("payload", payload == null ? new JsonObject() : payload);
		return envelope;
	}

	/** The utterance this envelope carries. Present only when the row is an envelope at all. */
	public static String utterance(JsonObject envelope) {
		if (envelope == null || !envelope.has("utterance") || !envelope.get("utterance").isJsonPrimitive()) {
			return "";
		}
		return envelope.get("utterance").getAsString();
	}

	/** The typed payload; machine data lives here and nowhere else. */
	public static JsonObject payload(JsonObject envelope) {
		if (envelope != null && envelope.has("payload") && envelope.get("payload").isJsonObject()) {
			return envelope.getAsJsonObject("payload");
		}
		return new JsonObject();
	}
}

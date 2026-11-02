package dev.ensouled.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import dev.ensouled.surface.SurfaceStream;

/**
 * The handoff, as each being tells it on its own stream (§47.2, §47.4).
 *
 * <p>A request, an acceptance, a hand-over and a receipt were previously only rows in the being's
 * evidence list — which is a bounded ring, so after a few hundred ticks the exchange had rotated
 * out and nothing on the being's own record said it had ever happened. The stream ledger is the
 * durable half, so every step of the exchange is written there as one envelope: prose an operator
 * can read, with the transfer id, the item, the count and the counterparty in the typed payload.
 *
 * <p>Both halves of a transfer are written by the being that performed them: the giver records that
 * it accepted and handed over at a place, the receiver records that it asked and that it received.
 * Neither writes the other's half, so a claim about the exchange is never made by the party that
 * would benefit from it.
 */
public final class SocialStream {
	private SocialStream() {
	}

	/** The asker's own record: it asked this peer for this item, under this transfer id. */
	public static void requested(String agent, String peerId, String item, int count, String transferId,
			long tick) {
		emit(agent, "REQUEST", ProseLines.require(agent, agent + " requires " + count + " " + item
						+ " from " + peerId + " under transfer " + transferId + "."),
				payload(peerId, item, count, transferId, tick));
	}

	/** The giver's own record: it decided it could honour this request. */
	public static void accepted(String agent, String peerId, String item, int count, String transferId,
			long tick) {
		emit(agent, "ACCEPT", ProseLines.record(agent, agent + " undertakes to hand " + count + " " + item
						+ " to " + peerId + " under transfer " + transferId + "."),
				payload(peerId, item, count, transferId, tick));
	}

	/** The giver's own record: the stack left its hands, at a place, for a count it observed. */
	public static void handed(String agent, String peerId, String item, int count, Vec3i at,
			String transferId, long tick) {
		emit(agent, "COMMIT", ProseLines.execute(agent, agent + " hands " + count + " " + item + " to "
						+ peerId + " at " + (at == null ? "an unrecorded position" : at.toString()) + "."),
				payload(peerId, item, count, transferId, tick));
	}

	/** The receiver's own record: its inventory grew by this much, from this peer. */
	public static void received(String agent, String peerId, String item, int count, String transferId,
			long tick) {
		emit(agent, "REPORT", ProseLines.record(agent, agent + " holds " + count + " more " + item
						+ " than before, handed over by " + peerId + " under transfer " + transferId + "."),
				payload(peerId, item, count, transferId, tick));
	}

	/** The giver's own record: it will not supply this, for a reason the asker can act on. */
	public static void declined(String agent, String peerId, String item, String reason, String transferId,
			long tick) {
		emit(agent, "DECLINE", ProseLines.forbid(agent, agent + " may not hand " + item + " to " + peerId
						+ " under transfer " + transferId + ", because " + plain(reason) + "."),
				payload(peerId, item, 0, transferId, tick));
	}

	private static JsonObject payload(String peerId, String item, int count, String transferId, long tick) {
		JsonObject payload = new JsonObject();
		payload.addProperty("transfer_id", transferId == null ? "" : transferId);
		payload.addProperty("peer", peerId == null ? "" : peerId);
		payload.addProperty("item", item == null ? "" : item);
		payload.addProperty("count", count);
		payload.addProperty("tick", tick);
		payload.addProperty("agent_channel", "MESSAGE_BUS");
		return payload;
	}

	private static void emit(String agent, String speechAct, String utterance, JsonObject payload) {
		SurfaceStream stream = SurfaceStream.get();
		if (stream == null || agent == null || agent.isBlank()) {
			return;
		}
		try {
			JsonObject envelope = MessageEnvelope.self(agent, "social:" + agent + ":"
					+ payload.get("transfer_id").getAsString(), speechAct, utterance,
					payload.get("tick").getAsLong(), null, null, payload);
			stream.social(agent, "social:" + speechAct.toLowerCase(), envelope);
		} catch (RuntimeException refused) {
			dev.ensouled.EnsouledMod.LOG.warn("[ensouled] {} social row was not written: {}", agent,
					refused.getMessage());
		}
	}

	/** A reason is a code in the payload; in the sentence it has to read as words. */
	private static String plain(String reason) {
		if (reason == null || reason.isBlank()) {
			return "no reason was stated";
		}
		return reason.replace('[', '(').replace(']', ')').replaceAll("\\s+", " ").trim();
	}

	/** Kept so a caller can build the same address the emitter uses. */
	static JsonArray recipients(String... ids) {
		JsonArray out = new JsonArray();
		for (String id : ids) {
			if (id != null && !id.isBlank()) {
				out.add(id);
			}
		}
		return out;
	}
}

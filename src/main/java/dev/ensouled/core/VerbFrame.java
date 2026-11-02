package dev.ensouled.core;

import com.google.gson.JsonArray;
import dev.ensouled.axis.AxisProof;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.List;

/**
 * One parsed unit of meaning: who, did or wants what, to which thing, how many, to whom.
 *
 * <p>A frame is not a chat line and not a skill id. It is the shape a being reasons about: the same
 * frame can be spoken, planned, delegated or wished about. {@link State} separates three different
 * facts that an older design collapsed into "does not know":
 *
 * <ul>
 *   <li>{@link State#ATTESTED} — the corpus gives this verb a claim shape, so it can be asserted.
 *   <li>{@link State#NAMED} — the being's vocabulary names it, but no claim shape exists for it. It
 *       can be meant and wished about; it cannot be stated as a claim.
 *   <li>{@link State#UNKNOWN} — a word the vocabulary does not attest. The frame is still recorded,
 *       because a word somebody said is evidence whether or not this world can understand it.
 * </ul>
 *
 * <p>{@code residue} holds the words the parser could not place. It is never dropped: a frame that
 * quietly discards half a sentence reads as understanding it.
 */
public record VerbFrame(String frameId, long tick, String source, String actor, String verb,
		State verbState, String objectKind, Integer objectCount, String recipient, String modality,
		String claimShape, List<String> residue, String utterance, int start, int end) {

	public enum State {
		ATTESTED, NAMED, UNKNOWN
	}

	public static final String SCHEMA = "ensouled.frame/1";

	public JsonObject json() {
		JsonObject row = new JsonObject();
		row.addProperty("schema", SCHEMA);
		row.addProperty("frame_id", frameId);
		row.addProperty("tick", tick);
		row.addProperty("source", source);
		row.addProperty("actor", actor);
		row.addProperty("verb", verb);
		row.addProperty("verb_state", verbState.name().toLowerCase(java.util.Locale.ROOT));
		row.addProperty("object_kind", objectKind);
		if (objectCount == null) {
			row.add("object_count", com.google.gson.JsonNull.INSTANCE);
		} else {
			row.addProperty("object_count", objectCount);
		}
		row.addProperty("recipient", recipient);
		row.addProperty("modality", modality);
		row.addProperty("claim_shape", claimShape);
		JsonArray residue = new JsonArray();
		for (String word : this.residue) {
			residue.add(new JsonPrimitive(word));
		}
		row.add("residue", residue);
		row.addProperty("utterance", utterance);
		row.addProperty("evidence", "sha256:" + AxisProof.digest(AxisProof.utf8(utterance)));
		row.addProperty("span_start", start);
		row.addProperty("span_end", end);
		return row;
	}

	/** True when the frame names something to do and a vocabulary word for it. */
	public boolean actionable() {
		return verbState != State.UNKNOWN && !verb.isEmpty() && !objectKind.isEmpty();
	}

	/** The frame in one readable line, for a stream or a ledger note. */
	public String describe() {
		StringBuilder out = new StringBuilder();
		out.append(actor.isEmpty() ? "someone" : actor).append(' ').append(verb);
		if (!objectKind.isEmpty()) {
			out.append(' ').append(objectKind);
		}
		if (objectCount != null) {
			out.append(" x").append(objectCount);
		}
		if (!recipient.isEmpty()) {
			out.append(" -> ").append(recipient);
		}
		out.append(" [").append(verbState.name().toLowerCase(java.util.Locale.ROOT));
		if (!claimShape.isEmpty()) {
			out.append(", ").append(claimShape);
		}
		if (!residue.isEmpty()) {
			out.append(", residue ").append(residue.size());
		}
		return out.append(']').toString();
	}
}

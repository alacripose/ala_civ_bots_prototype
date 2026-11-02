package dev.ensouled.axis;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import dev.ensouled.core.MessageEnvelope;
import dev.ensouled.core.ProseLines;
import dev.ensouled.core.Vec3i;

/**
 * One decision, as a message a reader can check.
 *
 * <p>A decision row is not a sentence about what a being chose; it is the choice, its qualified
 * ladder token, the refusal code when it was refused, and the ticking proof that says which bytes,
 * which ticks and which world the reading was made from. The artifact is the decision itself, so a
 * reader can re-hash {@code artifact_bytes} and get the digest the proof names.
 *
 * <p>The row is a §47.2 envelope ({@link MessageEnvelope}): the utterance is prose a being could
 * have said out loud, and the token, the code and the proof are typed data in {@code payload}. The
 * older shape put {@code harvest_tree@12,64,-8} in the utterance slot, which is how a lane of
 * machine tokens came to be read as language.
 *
 * <p>{@code proof.ticket} names the {@link dev.ensouled.work.Ticket} that authorised the decision
 * and {@code payload.cell} is the cell it spent on that ticket. A decision made with no ticket
 * open falls back to its intention id, and says so by carrying {@code -1} as the cell: it is not
 * dressed up as ticketed work.
 */
public final class DecisionPackage {
	public static final String FORMAT = "civilian.decision/1";

	private DecisionPackage() {
	}

	/**
	 * Build the package for one candidate's gate result. {@code where} and {@code dimension} are the
	 * body's observed position, so the world digest is a fact about the world at that tick rather
	 * than a value the decision supplied about itself.
	 */
	public static JsonObject of(String agent, long tick, String skill, String subject,
			AxisGate.Decision decision, String intentionId, long startTick, Vec3i where, String dimension) {
		return of(agent, tick, skill, subject, decision, intentionId, startTick, where, dimension, null);
	}

	/** The same row, carrying the establishment rung the selection actually ranked on. */
	public static JsonObject of(String agent, long tick, String skill, String subject,
			AxisGate.Decision decision, String intentionId, long startTick, Vec3i where, String dimension,
			String establishment) {
		return of(agent, tick, skill, subject, decision, intentionId, startTick, where, dimension,
				establishment, null, -1);
	}

	/** The row with the ticket that authorised it and the cell it spent ({@code -1} when none). */
	public static JsonObject of(String agent, long tick, String skill, String subject,
			AxisGate.Decision decision, String intentionId, long startTick, Vec3i where, String dimension,
			String establishment, String ticketId, int cell) {
		String artifact = FORMAT + "|" + agent + "|" + tick + "|" + subject + "|" + decision.token()
				+ "|" + decision.code();
		byte[] bytes = AxisProof.utf8(artifact);
		String digest = "sha256:" + AxisProof.digest(bytes);
		String world = (dimension == null ? "minecraft:overworld" : dimension) + "@"
				+ (where == null ? "unknown" : where.x() + "," + where.y() + "," + where.z());
		String worldDigest = "sha256:" + AxisProof.digest(AxisProof.utf8(world));
		// The action is named by the corpus's own verb, not by the skill's machine id: the verb
		// library is induced from the corpus's sentences, so `harvest_tree` is spoken as
		// "harvest tree" and a skill the corpus has no verb for reports `unattested`.
		String action = dev.ensouled.core.VerbLibrary.corpus().phrase(skill);
		if (action == null || action.isBlank()) {
			action = String.valueOf(skill);
		}
		String verb = dev.ensouled.core.VerbLibrary.corpus().spokenVerb(skill);
		String verbBasis = dev.ensouled.core.VerbLibrary.corpus().basis(skill);
		String ticket = ticketId == null || ticketId.isBlank()
				? (intentionId == null ? "" : intentionId) : ticketId;

		JsonObject proof = new JsonObject();
		proof.addProperty("artifact", artifact);
		proof.addProperty("artifact_digest", digest);
		proof.addProperty("artifact_bytes", artifact);
		proof.addProperty("origin", AxisVocabulary.Kind.TICKET_EVIDENCE.qualify("C"));
		JsonArray span = new JsonArray();
		span.add(Math.min(startTick, tick));
		span.add(tick);
		proof.add("tick_span", span);
		proof.addProperty("world", world);
		proof.addProperty("world_digest", worldDigest);
		proof.addProperty("presenter", agent);
		proof.addProperty("ticket", ticket);
		proof.addProperty("checked", decision.checked());

		JsonObject payload = new JsonObject();
		payload.addProperty("format", FORMAT);
		payload.addProperty("agent", agent);
		payload.addProperty("tick", tick);
		payload.addProperty("skill", skill);
		payload.addProperty("subject", subject);
		payload.addProperty("verb", verb);
		payload.addProperty("verb_basis", verbBasis);
		payload.addProperty("admitted", decision.admitted());
		payload.addProperty("token", decision.token());
		payload.addProperty("establishment",
				AxisVocabulary.Kind.ESTABLISHMENT.qualify(establishment == null ? "unchecked" : establishment));
		payload.addProperty("code", decision.code());
		payload.addProperty("why", decision.why());
		payload.addProperty("witness", decision.witness() == null ? "" : decision.witness());
		payload.addProperty("intention", intentionId == null ? "" : intentionId);
		payload.addProperty("ticket", ticket);
		payload.addProperty("cell", cell);
		payload.add("proof", proof);

		String utterance = decision.admitted()
				? ProseLines.execute(agent, agent + " begins " + action + " at " + where(where) + ".")
				: ProseLines.forbid(agent, agent + " may not begin " + action + " because "
						+ decision.why() + ".");
		return MessageEnvelope.self(agent, "decision:" + agent, decision.admitted() ? "COMMIT" : "DECLINE",
				utterance, tick, dimension, where, payload);
	}

	private static String where(Vec3i at) {
		return at == null ? "an unrecorded position" : at.x() + "," + at.y() + "," + at.z();
	}
}

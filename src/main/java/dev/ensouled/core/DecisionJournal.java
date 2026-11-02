package dev.ensouled.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;

import dev.ensouled.axis.AxisGate;
import dev.ensouled.persist.MindJournal;

/**
 * The durable record of a decision: written ahead of the action it justifies (§49.3), closed when
 * its outcome is known, and reconciled at boot when a process died between the two.
 *
 * <p>Nothing here decides anything. A decision that cannot be written is not durable, and a decision
 * that is not durable does not start — that is the only rule this class enforces, and it enforces it
 * in one place rather than at each caller.
 */
public final class DecisionJournal {

	private final AgentIdentity identity;
	private final MindLedger ledger;
	private boolean recoveryChecked;

	DecisionJournal(AgentIdentity identity, MindLedger ledger) {
		this.identity = identity;
		this.ledger = ledger;
	}

	/**
	 * The decision, in the shape §47.2 requires of every semantic message: one identity, one
	 * authoritative sender, a scope, a speech act and a content hash, plus the axis token and the
	 * proof the selection was admitted by. Written before the intention starts executing.
	 */
	boolean writeAhead(Intention intention, WorldView view, long tick, AxisGate.Decision verdict,
			String establishment, ActionEnvelope envelope) {
		MindJournal journal = MindJournal.active();
		if (journal == null) {
			dev.ensouled.EnsouledMod.LOG.error("[ensouled] {} has no journal: the decision cannot be made durable",
					identity.displayName());
			return false;
		}
		String txn = "decision:" + identity.agentId() + ":" + intention.id();
		String subject = intention.skillId() + "@" + intention.target();
		// The write-ahead record is a §47.2 envelope too: what is durable is the sentence the being
		// would have said and the typed facts behind it, not a token in the utterance slot.
		JsonObject payload = new JsonObject();
		payload.addProperty("skill", intention.skillId());
		payload.addProperty("target", String.valueOf(intention.target()));
		payload.addProperty("subject", subject);
		payload.addProperty("axis", "PROCEDURAL_GATE");
		payload.addProperty("axis_token", verdict == null ? "-" : verdict.token());
		payload.addProperty("axis_proof", verdict == null ? "-" : verdict.basis());
		payload.addProperty("establishment",
				dev.ensouled.axis.AxisVocabulary.Kind.ESTABLISHMENT.qualify(establishment));
		payload.addProperty("action_hash", envelope == null ? "-" : envelope.actionHash());
		JsonObject message;
		try {
			message = MessageEnvelope.self(identity.displayName(), intention.id(), "COMMIT",
					ProseLines.execute(identity.displayName(), identity.displayName() + " begins "
							+ intention.skillId() + " at " + view.bodyPos().x() + "," + view.bodyPos().y()
							+ "," + view.bodyPos().z() + "."),
					tick, view.dimensionId(), view.bodyPos(), payload);
		} catch (IllegalArgumentException notProse) {
			// A decision that cannot be written as a §47.2 message is not durable, so it does not
			// start: the refusal is named here rather than thrown out of the decision cycle.
			dev.ensouled.EnsouledMod.LOG.error("[ensouled] {} decision {} has no prose form: {}",
					identity.displayName(), txn, notProse.getMessage());
			return false;
		}
		Map<String, String> fields = new LinkedHashMap<>();
		fields.put("message_id", message.get("message_id").getAsString());
		fields.put("conversation_id", message.get("conversation_id").getAsString());
		fields.put("correlation_id", message.get("correlation_id").getAsString());
		fields.put("sender", identity.agentId());
		fields.put("sender_name", message.get("sender_name").getAsString());
		fields.put("recipients", identity.displayName());
		fields.put("channel", message.get("channel").getAsString());
		fields.put("scope", message.get("scope").getAsString());
		fields.put("speech_act", message.get("speech_act").getAsString());
		fields.put("utterance", message.get("utterance").getAsString());
		fields.put("content_hash", message.get("content_hash").getAsString());
		fields.put("delivery_state", message.get("delivery_state").getAsString());
		fields.put("transport_kind", message.get("transport_kind").getAsString());
		fields.put("tick", Long.toString(tick));
		fields.put("dimension", view.dimensionId());
		fields.put("position", view.bodyPos().toString());
		fields.put("skill", intention.skillId());
		fields.put("target", String.valueOf(intention.target()));
		fields.put("axis", "PROCEDURAL_GATE");
		fields.put("axis_token", payload.get("axis_token").getAsString());
		fields.put("axis_proof", payload.get("axis_proof").getAsString());
		fields.put("establishment", payload.get("establishment").getAsString());
		fields.put("action_hash", payload.get("action_hash").getAsString());
		fields.put("payload", payload.toString());
		try {
			journal.append(MindJournal.Kind.DECISION, identity.agentId(), txn, fields);
			return true;
		} catch (RuntimeException notDurable) {
			dev.ensouled.EnsouledMod.LOG.error("[ensouled] {} decision {} is not durable: {}",
					identity.displayName(), txn, notDurable.getMessage());
			return false;
		}
	}

	/** Closes a decision that was written ahead, once its outcome is known. */
	void close(String status, Intention intention, long tick) {
		MindJournal journal = MindJournal.active();
		if (journal == null || intention == null) {
			return;
		}
		String txn = "decision:" + identity.agentId() + ":" + intention.id();
		MindJournal.Entry decision = journal.byTransaction(txn);
		if (decision == null || journal.recorded("outcome:" + txn)) {
			return;
		}
		try {
			journal.closeDecision(decision, status, Map.of("at_tick", Long.toString(tick)));
		} catch (RuntimeException notDurable) {
			dev.ensouled.EnsouledMod.LOG.warn("[ensouled] {} outcome for {} could not be written: {}",
					identity.displayName(), txn, notDurable.getMessage());
		}
	}

	/**
	 * §49.1/§49.3 boot: a decision that was written ahead with no outcome on disk is the process that
	 * stopped mid-action. It is named on the being's own stream and closed as recovered, and the
	 * being carries on deciding from its durable state rather than from a blank mind.
	 */
	void recoverOnce(long tick) {
		if (recoveryChecked) {
			return;
		}
		recoveryChecked = true;
		MindJournal journal = MindJournal.active();
		if (journal == null) {
			return;
		}
		// A process can only have been inside one decision when it stopped, so at most one pending row
		// is the crash. It is the newest. Anything older finished before the crash and lacked only its
		// closing row; announcing those as "interrupted" would be a false statement about what the
		// crash caught, and a restart that reports hundreds of finished wander steps is noise a reader
		// has to filter to find the one that matters. They are closed, silently, as superseded.
		List<MindJournal.Entry> pending = journal.pendingDecisions(identity.agentId());
		for (int i = 0; i < pending.size(); i++) {
			MindJournal.Entry entry = pending.get(i);
			boolean inFlight = i == pending.size() - 1;
			if (!inFlight) {
				journal.closeDecision(entry, "superseded", Map.of("closed_tick", Long.toString(tick)));
				continue;
			}
			String what = entry.field("utterance", entry.txn());
			String why = "written ahead at tick " + entry.field("tick", "?")
					+ ", no outcome on disk, recovered and continued";
			ledger.note(tick, identity.agentId(), "", EvidenceKind.DECISION, entry.txn(), "recovered",
					what + " — " + why);
			StreamNotices.recovered(identity.displayName(), what, why);
			dev.ensouled.EnsouledMod.LOG.info("[ensouled] {} recovered interrupted decision #{}: {}",
					identity.displayName(), entry.seq(), what);
			journal.closeDecision(entry, "recovered", Map.of("recovered_tick", Long.toString(tick)));
		}
	}
}

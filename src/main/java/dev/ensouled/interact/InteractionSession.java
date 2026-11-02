package dev.ensouled.interact;

import java.util.List;

import dev.ensouled.core.AuthorityGateway;
import dev.ensouled.core.EvidenceKind;
import dev.ensouled.core.GateDecision;
import dev.ensouled.core.WorldView;

/**
 * One being's access to world objects for the current tick: every operation passes the authority
 * gateway first, and both the verdict and the server result are recorded. Skills never touch a
 * container directly — they speak to this session, so permission and evidence cannot be skipped by
 * calling a lower layer.
 */
public final class InteractionSession {
	public interface Recorder {
		void note(EvidenceKind kind, String outcome, String detail);
	}

	private final InteractionHands hands;
	private final AuthorityGateway authority;
	private final WorldView view;
	private final Recorder recorder;

	public InteractionSession(InteractionHands hands, AuthorityGateway authority, WorldView view,
			Recorder recorder) {
		this.hands = hands;
		this.authority = authority;
		this.view = view;
		this.recorder = recorder;
	}

	/** Perceived objects that support at least one world-object operation, nearest first. */
	public List<InteractionTarget> targets(int radius) {
		return hands.targets(radius);
	}

	public InteractionOutcome perform(InteractionTarget target, InteractionRequest request) {
		GateDecision decision = authority.authorizeInteraction(request, target, view);
		if (!decision.permitted()) {
			recorder.note(EvidenceKind.GATE, decision.reasonCode(),
					request.describe() + " on " + target.objectId() + ": " + decision.detail());
			return InteractionOutcome.refused(decision.reasonCode(), decision.detail());
		}
		InteractionOutcome outcome = hands.perform(target, request);
		recorder.note(EvidenceKind.INTERACTION, outcome.code(),
				target.objectId() + " " + request.describe() + " -> " + outcome.status() + " "
						+ outcome.detail() + " " + outcome.facts());
		return outcome;
	}
}

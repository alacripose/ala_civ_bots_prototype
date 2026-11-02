package dev.ensouled.mind;

import java.util.List;

import dev.ensouled.interact.InteractionHands;
import dev.ensouled.interact.InteractionOutcome;
import dev.ensouled.interact.InteractionRequest;
import dev.ensouled.interact.InteractionTarget;

/**
 * The mind outlives any particular entity instance; this proxy lets a reloaded body re-supply its
 * world-object hands without rebuilding the mind, and refuses typed when no body is attached.
 */
public final class BodyHandsProxy implements InteractionHands {
	private InteractionHands delegate;

	public void attach(InteractionHands hands) {
		delegate = hands;
	}

	public void detach() {
		delegate = null;
	}

	public boolean attached() {
		return delegate != null;
	}

	@Override
	public List<InteractionTarget> targets(int radius) {
		return delegate == null ? List.of() : delegate.targets(radius);
	}

	@Override
	public InteractionOutcome perform(InteractionTarget target, InteractionRequest request) {
		if (delegate == null) {
			return InteractionOutcome.refused("no_body", "no body is attached to this mind");
		}
		return delegate.perform(target, request);
	}
}

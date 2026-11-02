package dev.ensouled.interact;

import java.util.List;
import java.util.Set;

/**
 * The body's hands for world objects: what it can perceive, and the server-authoritative operation on
 * a perceived object. An implementation may only move items through the real inventory/container API
 * under reach and slot rules; it must never expose "set this inventory" as a capability.
 */
public interface InteractionHands {
	/** Perceived supported objects within the radius, nearest first. */
	List<InteractionTarget> targets(int radius);

	InteractionOutcome perform(InteractionTarget target, InteractionRequest request);

	/** A body with no world-object capability: perceives nothing and refuses everything. */
	static InteractionHands none() {
		return None.INSTANCE;
	}

	final class None implements InteractionHands {
		private static final None INSTANCE = new None();

		private None() {
		}

		@Override
		public List<InteractionTarget> targets(int radius) {
			return List.of();
		}

		@Override
		public InteractionOutcome perform(InteractionTarget target, InteractionRequest request) {
			return InteractionOutcome.refused("unsupported_object",
					"this body kind has no world-object interaction path");
		}
	}

	/** Capabilities a chest exposes, declared once so perception, gate and adapter agree. */
	static Set<String> chestCapabilities() {
		return Set.of(InteractionRequest.INSPECT, InteractionRequest.WITHDRAW, InteractionRequest.DEPOSIT);
	}
}

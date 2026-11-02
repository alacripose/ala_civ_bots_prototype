package dev.ensouled.core;

import java.util.Optional;

/**
 * Fast reflexes (§5.4). This layer may only choose from a closed, tested list, and it can never
 * grant a capability, widen a scope or bypass the gate. It only decides that the body must move
 * away from an immediate physical hazard.
 */
public final class ReflexLayer {
	public record ReflexAction(String code, String reason, MotorCommand command, int priority) {
	}

	public Optional<ReflexAction> check(NeedsState needs, WorldView view) {
		Vec3i body = view.bodyPos();
		if (needs.onFire()) {
			return Optional.of(new ReflexAction("on_fire", "body is burning",
					new MotorCommand.MoveAwayFrom(body, 4, 60), 1000));
		}
		if (needs.submerged() && needs.air() < 120) {
			return Optional.of(new ReflexAction("drowning", "air supply is low",
					new MotorCommand.MoveTo(body.offset(0, 2, 0), 1.0, 60), 900));
		}
		for (EntityStimulus e : view.entities(8)) {
			if (e.hostileEngaged() && e.visible()) {
				return Optional.of(new ReflexAction("hostile_adjacent", "hostile " + e.entityTypeId() + " within 8 blocks",
						new MotorCommand.MoveAwayFrom(e.pos(), 10, 80), 950));
			}
		}
		return Optional.empty();
	}
}

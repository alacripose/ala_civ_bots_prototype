package dev.ensouled.core;

/**
 * An entity this agent perceived. Health of others is deliberately absent: it is not a
 * player-perceptible field, so the mind must not receive it (§6.1).
 */
public record EntityStimulus(
		String entityId,
		String entityTypeId,
		Vec3i pos,
		double distance,
		boolean visible,
		boolean hostileEngaged) {
}

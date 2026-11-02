package dev.ensouled.core;

import java.util.Optional;

/**
 * Which perceived player entities are ordinary humans rather than agent bodies (HUM-01). The
 * division is the agent registry's, exactly like {@link Peers}: a session bound to a mind is an
 * agent, anything else that is perceived as a player is a human.
 */
public final class Humans {
	public record Human(String entityUuid, String name, Vec3i pos, double distance) {
	}

	private Humans() {
	}

	/** Nearest perceived human this being can address, or empty when nobody is there to hear it. */
	public static Optional<Human> nearest(WorldView view, SocialSeam social, int radius) {
		Optional<HumanChannel> channel = social.humans();
		if (channel.isEmpty()) {
			return Optional.empty();
		}
		Human nearest = null;
		for (EntityStimulus entity : view.entities(radius)) {
			if (!entity.visible() || !"minecraft:player".equals(entity.entityTypeId())) {
				continue;
			}
			if (social.agentIdForEntity(entity.entityId()).isPresent()) {
				continue;
			}
			Optional<String> name = channel.get().displayName(entity.entityId());
			if (name.isEmpty()) {
				continue;
			}
			if (nearest == null || entity.distance() < nearest.distance()) {
				nearest = new Human(entity.entityId(), name.get(), entity.pos(), entity.distance());
			}
		}
		return Optional.ofNullable(nearest);
	}
}

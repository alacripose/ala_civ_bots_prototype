package dev.ensouled.core;

import java.util.Optional;

/** Which perceived player entities are other agents, and where they stand (Addendum C §C.7.1). */
public final class Peers {
	public record Peer(String agentId, Vec3i pos, double distance) {
	}

	private Peers() {
	}

	/** Nearest perceived agent body other than this being's own. */
	public static Optional<Peer> nearest(WorldView view, SocialSeam social, String selfAgentId, int radius) {
		Peer nearest = null;
		for (EntityStimulus entity : view.entities(radius)) {
			if (!entity.visible()) {
				continue;
			}
			Optional<String> agentId = social.agentIdForEntity(entity.entityId());
			if (agentId.isEmpty() || agentId.get().equals(selfAgentId)) {
				continue;
			}
			if (nearest == null || entity.distance() < nearest.distance()) {
				nearest = new Peer(agentId.get(), entity.pos(), entity.distance());
			}
		}
		return Optional.ofNullable(nearest);
	}

	public static Optional<Peer> agent(WorldView view, SocialSeam social, String agentId, int radius) {
		for (EntityStimulus entity : view.entities(radius)) {
			Optional<String> perceived = social.agentIdForEntity(entity.entityId());
			if (perceived.isPresent() && perceived.get().equals(agentId)) {
				return Optional.of(new Peer(agentId, entity.pos(), entity.distance()));
			}
		}
		return Optional.empty();
	}
}

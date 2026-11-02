package dev.ensouled.participant;

import java.util.Optional;

import dev.ensouled.mind.AgentManager;
import dev.ensouled.mind.ExternalBrains;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * The one place a connected session is classified: agent identity comes from the agent registry and
 * its body binding, never from an entity subclass. A session bound to a mind is an agent; anything
 * else is an ordinary human participant. Nothing here disconnects, spectates or restricts a human.
 */
public final class ParticipantRegistry {
	private static final ParticipantRegistry INSTANCE = new ParticipantRegistry();

	private ParticipantRegistry() {
	}

	public static ParticipantRegistry get() {
		return INSTANCE;
	}

	public ParticipantRole roleOf(ServerPlayerEntity player) {
		if (ExternalBrains.isExternal(player.getGameProfile().getName())) {
			return ParticipantRole.AGENT;
		}
		return agentIdOf(player).isPresent() ? ParticipantRole.AGENT : ParticipantRole.PARTICIPANT;
	}

	/** The registered mind that owns this session's body, or empty for a human session. */
	public Optional<String> agentIdOf(ServerPlayerEntity player) {
		AgentManager manager = AgentManager.get();
		return manager == null ? Optional.empty() : manager.agentIdForPlayer(player);
	}
}

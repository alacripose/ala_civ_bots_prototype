package dev.ensouled.participant;

import dev.ensouled.EnsouledMod;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Records how a connected session is classified, in production code and only through a server-side
 * event (correction §3, §5). A human joining is never disconnected, spectated or restricted for
 * being human; the classification exists so identity is decided by the registry, not by a subclass.
 */
public final class ParticipantHooks {
	private ParticipantHooks() {
	}

	public static void register() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayerEntity player = handler.player;
			EnsouledMod.LOG.info("[ensouled] session {} joined as {}",
					player.getGameProfile().getName(), ParticipantRegistry.get().roleOf(player));
		});
	}
}

package dev.ensouled;

import dev.ensouled.command.EnsouledCommands;
import dev.ensouled.mind.AgentManager;
import dev.ensouled.participant.ParticipantHooks;
import dev.ensouled.surface.SurfaceStream;

import java.nio.file.Path;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public final class EnsouledServer implements DedicatedServerModInitializer {
	@Override
	public void onInitializeServer() {
		// Entity types and attributes are registered once in the common initializer.
		EnsouledCommands.register();
		// Connected sessions are classified by the agent registry, and a human is never refused for
		// being human (correction §2, §5).
		ParticipantHooks.register();
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			AgentManager.onServerStarted(server);
			// The toast stream: mounted, receipted, served per agent, with the ledger already written.
			SurfaceStream.start(Path.of("").toAbsolutePath());
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			SurfaceStream.stop();
			AgentManager.onServerStopping(server);
		});
		ServerTickEvents.END_SERVER_TICK.register(AgentManager::onServerTick);
		// A networked agent client replaces its in-process fake body: one identity, one body.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			if (AgentManager.get() != null) {
				AgentManager.get().bindNetworkedPlayer(handler.player);
			}
		});
		// Ordinary chat is perception, not a command channel: every being that can hear the line is
		// handed it, and each one decides for itself what, if anything, to do about it (CHAT-01).
		ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
			if (AgentManager.get() != null) {
				AgentManager.get().hear(sender, message.getContent().getString());
			}
		});
		EnsouledMod.LOG.info("[ensouled] dedicated server init");
	}
}

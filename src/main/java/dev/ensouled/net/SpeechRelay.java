package dev.ensouled.net;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * One line of agent speech on its way to the agent's own client (G.9). The server sends the text;
 * the client sends it through its ordinary chat-send path, so the line reaches other players as
 * normal player chat under the agent's identity.
 */
public final class SpeechRelay {
	public static final Identifier CHANNEL = new Identifier("ensouled", "speech");

	private SpeechRelay() {
	}

	public static void send(ServerPlayerEntity client, String text) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeString(text, 256);
		ServerPlayNetworking.send(client, CHANNEL, buf);
	}
}

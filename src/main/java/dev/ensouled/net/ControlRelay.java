package dev.ensouled.net;

import dev.ensouled.EnsouledMod;
import dev.ensouled.surface.SurfaceStream;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client control channel: the server asks the headless client to produce ordinary player input.
 *
 * A control action is bounded by the declared action set, never by the confidence of the thing that
 * wants it. A program opens a ticket before its first packet; the ticket carries the presenter's
 * name and its ledger row is committed before the client is asked to move. An action outside the
 * set, or a packet with no ticket, is refused before it runs — the same door as `proof-no-presenter`,
 * on the side that acts rather than the side that claims. The client checks both fields and refuses
 * a packet that arrives without them, so the stream can never become a second authority that moves
 * a body on its own.
 */
public final class ControlRelay {
	public static final Identifier CHANNEL = new Identifier("ensouled", "control");

	/** The declared set. Widening it is its own decision with its own row; it is not widened here. */
	public static final List<String> ACTION_SET = List.of("forward", "look", "jump", "attack", "use", "select", "drop");

	private static final Map<UUID, String> TICKETS = new ConcurrentHashMap<>();

	private ControlRelay() {
	}

	/** Open a program's ticket. Returns null when the ticket could not be recorded: nothing moves. */
	public static String open(ServerPlayerEntity client, String program) {
		String presenter = client.getGameProfile().getName();
		String ticket = "ctl:" + UUID.randomUUID();
		SurfaceStream stream = SurfaceStream.get();
		if (stream != null) {
			try {
				stream.control(presenter, program, ticket);
			} catch (RuntimeException unrecorded) {
				EnsouledMod.LOG.warn("[ensouled] control program {} for {} was not recorded: {} — nothing moves",
						program, presenter, unrecorded.getMessage());
				return null;
			}
		}
		TICKETS.put(client.getUuid(), ticket);
		return ticket;
	}

	public static void close(ServerPlayerEntity client) {
		TICKETS.remove(client.getUuid());
	}

	public static void send(ServerPlayerEntity client, String action, float a, float b) {
		String presenter = client.getGameProfile().getName();
		if (!ACTION_SET.contains(action)) {
			refuse(presenter, action, "control-outside-action-set");
			return;
		}
		String ticket = TICKETS.get(client.getUuid());
		if (ticket == null) {
			refuse(presenter, action, "control-no-ticket");
			return;
		}
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeString(action, 32);
		buf.writeFloat(a);
		buf.writeFloat(b);
		buf.writeString(ticket, 64);
		buf.writeString(presenter, 32);
		ServerPlayNetworking.send(client, CHANNEL, buf);
	}

	private static void refuse(String presenter, String action, String code) {
		EnsouledMod.LOG.warn("[ensouled] control refused {} for {}: {}", action, presenter, code);
		SurfaceStream stream = SurfaceStream.get();
		if (stream != null) {
			try {
				stream.refuse(presenter, "control:" + action, code, "the action never reached the client");
			} catch (RuntimeException untracked) {
				EnsouledMod.LOG.warn("[ensouled] control refusal {} was not recorded: {}", code, untracked.getMessage());
			}
		}
	}
}

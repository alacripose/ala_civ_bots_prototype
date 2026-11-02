package dev.ensouled.entity;

import java.nio.file.Files;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import dev.ensouled.EnsouledMod;
import dev.ensouled.body.FakeClientConnection;
import dev.ensouled.core.Vec3i;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.GameMode;

/**
 * A real {@link ServerPlayerEntity} driven in-process: the primary citizen body of Addendum C.
 * Movement and interaction go through the ordinary player entity, interaction manager and world
 * rules; only the client side is absent.
 */
public final class FakePlayerBody extends ServerPlayerEntity {
	private FakePlayerBody(MinecraftServer server, ServerWorld world, GameProfile profile) {
		super(server, world, profile);
	}

	/**
	 * Materializes an agent-owned profile as a server player. The profile UUID is the agent's
	 * reserved identity; it is never a human account's UUID. Existing player data (inventory,
	 * position, effects) is loaded by the vanilla join path and left intact.
	 */
	public static FakePlayerBody materialize(MinecraftServer server, ServerWorld world, UUID profileId,
			String name, Vec3i pos) {
		ServerPlayerEntity online = server.getPlayerManager().getPlayer(profileId);
		if (online instanceof FakePlayerBody existing) {
			return existing;
		}
		boolean hadData = Files.exists(
				server.getSavePath(WorldSavePath.PLAYERDATA).resolve(profileId + ".dat"));

		GameProfile profile = new GameProfile(profileId, name);
		FakeClientConnection connection = new FakeClientConnection();
		FakePlayerBody player = new FakePlayerBody(server, world, profile);
		connection.setPacketListener(new ServerPlayNetworkHandler(server, connection, player));

		server.getPlayerManager().onPlayerConnect(connection, player);
		player.changeGameMode(GameMode.SURVIVAL);
		if (!hadData) {
			player.refreshPositionAndAngles(pos.x() + 0.5, pos.y(), pos.z() + 0.5, 0.0f, 0.0f);
			player.setHealth(20.0f);
			player.getHungerManager().setFoodLevel(20);
		}
		EnsouledMod.LOG.info("[ensouled] materialized fake player {} ({}) at {} in {} (restoredData={})",
				name, profileId, player.getBlockPos(), world.getRegistryKey().getValue(), hadData);
		return player;
	}

	/**
	 * Runs the shared player physics step that a real client would execute before reporting its
	 * position. Vanilla {@link ServerPlayerEntity} trusts client positions and never calls
	 * {@code tickMovement} on the server, so a client-less body must step it: motor intent (yaw,
	 * forward/strafe, jump) is the only input, while collision, gravity, step-up and fall damage stay
	 * the world's answer (Addendum C §C.4.2).
	 */
	/** The same slot-selection packet a client sends when the player scrolls the hotbar. */
	public void selectHotbarSlot(int slot) {
		networkHandler.onUpdateSelectedSlot(new UpdateSelectedSlotC2SPacket(slot));
	}

	/**
	 * The same server-side interaction the block-use packet resolves to, returning vanilla's own
	 * verdict so the motor can report a refusal instead of guessing. The packet path itself expects a
	 * live client session and silently dropped these interactions for a client-less body (verified on a
	 * booted server); the reach and target checks that path adds are applied by the motor.
	 */
	public ActionResult interactBlock(BlockHitResult hit) {
		return interactionManager.interactBlock(this, getWorld(), getMainHandStack(), Hand.MAIN_HAND, hit);
	}

	public void tickBodyPhysics() {
		if (!isAlive() || isRemoved()) {
			return;
		}
		this.tickMovement();
	}

	public UUID profileId() {
		return getGameProfile().getId();
	}
}

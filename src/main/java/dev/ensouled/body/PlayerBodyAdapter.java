package dev.ensouled.body;

import java.util.UUID;

import dev.ensouled.core.Motor;
import dev.ensouled.core.Vec3i;
import dev.ensouled.core.WorldView;
import dev.ensouled.entity.FakePlayerBody;
import dev.ensouled.interact.InteractionHands;
import dev.ensouled.interact.PlayerInteractionHands;
import net.minecraft.server.world.ServerWorld;

/** Wraps the primary fake-player body. */
public final class PlayerBodyAdapter implements BodyAdapter {
	private final FakePlayerBody player;

	public PlayerBodyAdapter(FakePlayerBody player) {
		this.player = player;
	}

	public FakePlayerBody playerBody() {
		return player;
	}

	public UUID profileId() {
		return player.profileId();
	}

	@Override
	public String agentId() {
		return "agent:" + player.profileId();
	}

	@Override
	public String displayName() {
		return player.getGameProfile().getName();
	}

	@Override
	public String kind() {
		return "player";
	}

	@Override
	public int entityId() {
		return player.getId();
	}

	@Override
	public boolean alive() {
		return player.isAlive() && !player.isRemoved() && !player.isDisconnected();
	}

	@Override
	public net.minecraft.entity.Entity entity() {
		return player;
	}

	@Override
	public ServerWorld world() {
		return (ServerWorld) player.getWorld();
	}

	@Override
	public Vec3i position() {
		return BlockSight.toVec(player.getBlockPos());
	}

	@Override
	public Motor createMotor() {
		return new PlayerMotor(player);
	}

	@Override
	public WorldView createView() {
		return new PlayerWorldView(player);
	}

	/** A client-less player is not ticked by vanilla, so its body physics run here. */
	@Override
	public void tickBody() {
		player.tickBodyPhysics();
	}

	@Override
	public InteractionHands interactions() {
		return new PlayerInteractionHands(player);
	}
}

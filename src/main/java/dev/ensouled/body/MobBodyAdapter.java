package dev.ensouled.body;

import dev.ensouled.core.Motor;
import dev.ensouled.core.Vec3i;
import dev.ensouled.core.WorldView;
import dev.ensouled.entity.AgentBodyEntity;
import net.minecraft.server.world.ServerWorld;

/** Wraps the custom civilian mob body used before fake players existed. */
public final class MobBodyAdapter implements BodyAdapter {
	private final AgentBodyEntity body;

	public MobBodyAdapter(AgentBodyEntity body) {
		this.body = body;
	}

	public AgentBodyEntity mob() {
		return body;
	}

	@Override
	public String agentId() {
		return body.getAgentId();
	}

	@Override
	public String displayName() {
		return body.getName().getString();
	}

	@Override
	public String kind() {
		return "mob";
	}

	@Override
	public int entityId() {
		return body.getId();
	}

	@Override
	public boolean alive() {
		return body.isAlive();
	}

	@Override
	public net.minecraft.entity.Entity entity() {
		return body;
	}

	@Override
	public ServerWorld world() {
		return (ServerWorld) body.getWorld();
	}

	@Override
	public Vec3i position() {
		return BlockSight.toVec(body.getBlockPos());
	}

	@Override
	public Motor createMotor() {
		return new MinecraftMotor(body);
	}

	@Override
	public WorldView createView() {
		return new MinecraftWorldView(body);
	}
}

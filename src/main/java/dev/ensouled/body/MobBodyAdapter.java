package dev.ensouled.body;

import dev.ensouled.core.Motor;
import dev.ensouled.core.Vec3i;
import dev.ensouled.core.WorldView;
import dev.ensouled.entity.AgentBodyEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

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

	/** A mob has no chat client; its line goes out through the server's ordinary chat format. */
	@Override
	public boolean say(String text) {
		if (!alive() || text == null || text.isBlank() || !(body.getWorld() instanceof ServerWorld world)) {
			return false;
		}
		MinecraftServer server = world.getServer();
		if (server == null) {
			return false;
		}
		server.getPlayerManager().broadcast(
				Text.translatable("chat.type.text", body.getDisplayName(), Text.literal(text)), false);
		return true;
	}
}

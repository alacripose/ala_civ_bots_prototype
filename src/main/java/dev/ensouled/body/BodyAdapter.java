package dev.ensouled.body;

import dev.ensouled.core.Motor;
import dev.ensouled.core.Vec3i;
import dev.ensouled.core.WorldView;
import dev.ensouled.interact.InteractionHands;
import net.minecraft.server.world.ServerWorld;

/**
 * One physical body. The mind is body-agnostic (Addendum C §C.3): a mob body and a fake-player body
 * implement the same seam, with type-specific motor and perception semantics behind it.
 */
public interface BodyAdapter {
	String agentId();

	String displayName();

	String kind();

	int entityId();

	boolean alive();

	net.minecraft.entity.Entity entity();

	ServerWorld world();

	Vec3i position();

	Motor createMotor();

	WorldView createView();

	/**
	 * Says one line through this body's ordinary player-chat transport. A body with no connected
	 * client cannot speak and returns false; a server-side system line is not a substitute.
	 */
	default boolean say(String text) {
		return false;
	}

	/**
	 * Advances this body's own simulation for one tick, for body kinds vanilla does not tick for us.
	 * A mob needs nothing here: the entity ticks itself while it is in the world.
	 */
	default void tickBody() {
	}

	/** World-object hands (chests and later other objects). A body kind without a path refuses. */
	default InteractionHands interactions() {
		return InteractionHands.none();
	}
}

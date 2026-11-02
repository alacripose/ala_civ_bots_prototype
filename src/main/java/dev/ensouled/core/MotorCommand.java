package dev.ensouled.core;

/**
 * The closed Phase 1 action vocabulary (§10.3): the same primitives a player produces with
 * keyboard/mouse, expressed as bounded input frames. No command teleports, injects items or
 * edits blocks directly; the body adapter must perform them through game mechanics.
 */
public sealed interface MotorCommand {
	int timeoutTicks();

	String describe();

	record MoveTo(Vec3i target, double radius, int timeoutTicks) implements MotorCommand {
		@Override
		public String describe() {
			return "move_to(" + target + ", r=" + radius + ")";
		}
	}

	record LookAt(Vec3i target, int timeoutTicks) implements MotorCommand {
		@Override
		public String describe() {
			return "look_at(" + target + ")";
		}
	}

	record MineBlock(Vec3i target, String expectedBlockId, int timeoutTicks) implements MotorCommand {
		@Override
		public String describe() {
			return "mine_block(" + target + ", expect=" + expectedBlockId + ")";
		}
	}

	record Jump(int timeoutTicks) implements MotorCommand {
		@Override
		public String describe() {
			return "jump()";
		}
	}

	/**
	 * Right-click the selected item against a block face, the way a player places a block: the target
	 * is the block being clicked and the face is the side the body is looking at.
	 */
	record Use(Vec3i target, String expectedBlockId, int timeoutTicks) implements MotorCommand {
		@Override
		public String describe() {
			return "use(" + target + ", expect=" + expectedBlockId + ")";
		}
	}

	/** Select a hotbar slot whose stack matches the item; the held item must actually change. */
	record Equip(String itemId, int timeoutTicks) implements MotorCommand {
		@Override
		public String describe() {
			return "equip(" + itemId + ")";
		}
	}

	/** Drop a held stack through the player's own drop path, so a real item entity enters the world. */
	record DropItem(String itemId, int count, int timeoutTicks) implements MotorCommand {
		@Override
		public String describe() {
			return "drop_item(" + itemId + " x" + count + ")";
		}
	}

	record CollectDrops(int radius, int timeoutTicks) implements MotorCommand {
		@Override
		public String describe() {
			return "collect_drops(r=" + radius + ")";
		}
	}

	record Eat(String itemId, int timeoutTicks) implements MotorCommand {
		@Override
		public String describe() {
			return "eat(" + itemId + ")";
		}
	}

	record MoveAwayFrom(Vec3i from, int distance, int timeoutTicks) implements MotorCommand {
		@Override
		public String describe() {
			return "move_away_from(" + from + ", d=" + distance + ")";
		}
	}

	record Wait(int ticks) implements MotorCommand {
		@Override
		public int timeoutTicks() {
			return ticks;
		}

		@Override
		public String describe() {
			return "wait(" + ticks + ")";
		}
	}
}

package dev.ensouled;

import java.util.Map;

import dev.ensouled.core.Motor;
import dev.ensouled.core.MotorCommand;
import dev.ensouled.core.MotorResult;
import dev.ensouled.core.NeedsState;
import dev.ensouled.core.Vec3i;

/** Simulates the physical outcome of each motor primitive. */
final class FakeMotor implements Motor {
	final FakeWorld world;
	boolean removeBlocks = true;
	boolean giveDrops = true;

	String lastCommand = "";
	int commandsStarted;

	private MotorCommand active;
	private int elapsed;
	private MotorResult result = MotorResult.success("idle", "no active program", Map.of());

	FakeMotor(FakeWorld world) {
		this.world = world;
	}

	@Override
	public void start(MotorCommand command) {
		active = command;
		elapsed = 0;
		commandsStarted++;
		lastCommand = command.describe();
		result = MotorResult.running(command.describe());
	}

	@Override
	public MotorResult poll() {
		if (active == null) {
			return result;
		}
		elapsed++;
		MotorResult step;
		if (active instanceof MotorCommand.MoveTo move) {
			if (elapsed >= 2) {
				world.body = move.target();
				step = MotorResult.success("arrived", "arrived", Map.of());
			} else {
				step = MotorResult.running("moving");
			}
		} else if (active instanceof MotorCommand.LookAt) {
			step = elapsed >= 1 ? MotorResult.success("oriented", "oriented", Map.of()) : MotorResult.running("looking");
		} else if (active instanceof MotorCommand.MineBlock mine) {
			step = pollMine(mine);
		} else if (active instanceof MotorCommand.DropItem drop) {
			int count = world.items.getOrDefault(drop.itemId(), 0);
			if (count < drop.count()) {
				step = MotorResult.failed("not_held", "none");
			} else {
				world.items.put(drop.itemId(), count - drop.count());
				step = MotorResult.success("dropped", "dropped", Map.of("count", Integer.toString(drop.count())));
			}
		} else if (active instanceof MotorCommand.CollectDrops) {
			step = MotorResult.success("collected", "collected drops", Map.of());
		} else if (active instanceof MotorCommand.Eat eat) {
			int count = world.items.getOrDefault(eat.itemId(), 0);
			if (count <= 0) {
				step = MotorResult.failed("no_food_item", "none");
			} else {
				world.items.put(eat.itemId(), count - 1);
				world.needs = new NeedsState(Math.min(20, world.needs.food() + 6), world.needs.health(),
						world.needs.air(), false, false, 0.0);
				step = MotorResult.success("ate", "ate", Map.of());
			}
		} else if (active instanceof MotorCommand.MoveAwayFrom) {
			step = MotorResult.success("escaped", "moved away", Map.of());
		} else {
			step = MotorResult.success("waited", "waited", Map.of());
		}
		if (step.terminal()) {
			active = null;
			result = step;
		}
		return result;
	}

	private MotorResult pollMine(MotorCommand.MineBlock mine) {
		if (elapsed < 2) {
			return MotorResult.running("mining");
		}
		String present = world.blocks.get(mine.target());
		if (present == null) {
			return MotorResult.success("already_gone", "air", Map.of("block_removed", "true"));
		}
		if (!present.equals(mine.expectedBlockId())) {
			return MotorResult.failed("block_changed", "expected " + mine.expectedBlockId());
		}
		if (!removeBlocks) {
			// A dishonest body: acknowledges success but mutates nothing.
			return MotorResult.success("block_removed", "claimed", Map.of("block_removed", "true"));
		}
		world.blocks.remove(mine.target());
		if (giveDrops) {
			world.items.merge("minecraft:oak_log", 1, Integer::sum);
		}
		return MotorResult.success("block_removed", "broke " + present, Map.of("block_removed", "true"));
	}

	@Override
	public void cancel() {
		if (active != null) {
			active = null;
			result = MotorResult.cancelled("cancelled");
		}
	}

	@Override
	public boolean busy() {
		return active != null;
	}

	Vec3i bodyPos() {
		return world.body;
	}
}

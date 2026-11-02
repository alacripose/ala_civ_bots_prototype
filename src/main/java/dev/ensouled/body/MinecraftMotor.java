package dev.ensouled.body;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.ensouled.EnsouledMod;
import dev.ensouled.core.Motor;
import dev.ensouled.core.MotorCommand;
import dev.ensouled.core.MotorResult;
import dev.ensouled.core.Vec3i;
import dev.ensouled.entity.AgentBodyEntity;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Performs one bounded motor program at a time through ordinary game actions: navigation for
 * movement, look control for attention, vanilla block-breaking math for mining, touch pickup for
 * items, and inventory consumption for eating. It never teleports, injects items or edits blocks
 * outside a completed break (§10.2, §32 "Physical agent").
 */
public final class MinecraftMotor implements Motor {
	public static final double REACH = 4.5;
	public static final double REACH_SQUARED = REACH * REACH;

	private final AgentBodyEntity body;

	private MotorCommand active;
	private int elapsed;
	private double breakProgress;
	private int walkedItemCount;
	private boolean walkedToDrop;
	private MotorResult result = MotorResult.success("idle", "no active program", Map.of());

	public MinecraftMotor(AgentBodyEntity body) {
		this.body = body;
	}

	@Override
	public void start(MotorCommand command) {
		if (active != null) {
			throw new IllegalStateException("motor already executing " + active.describe());
		}
		active = command;
		elapsed = 0;
		breakProgress = 0.0;
		walkedItemCount = 0;
		walkedToDrop = false;
		result = MotorResult.running(command.describe());
		if (command instanceof MotorCommand.MoveTo move) {
			beginMove(move);
		}
	}

	private void beginMove(MotorCommand.MoveTo move) {
		Vec3i target = move.target();
		boolean started = body.getNavigation().startMovingTo(target.x() + 0.5, target.y(), target.z() + 0.5, 1.0);
		if (!started) {
			result = MotorResult.failed("no_path", "no path to " + target);
			active = null;
		}
	}

	@Override
	public MotorResult poll() {
		if (active == null) {
			return result;
		}
		elapsed++;
		MotorResult step;
		if (active instanceof MotorCommand.MoveTo move) {
			step = pollMove(move);
		} else if (active instanceof MotorCommand.LookAt look) {
			step = pollLook(look);
		} else if (active instanceof MotorCommand.MineBlock mine) {
			step = pollMine(mine);
		} else if (active instanceof MotorCommand.CollectDrops collect) {
			step = pollCollect(collect);
		} else if (active instanceof MotorCommand.Eat eat) {
			step = pollEat(eat);
		} else if (active instanceof MotorCommand.MoveAwayFrom away) {
			step = pollAway(away);
		} else if (active instanceof MotorCommand.Wait wait) {
			step = elapsed >= wait.ticks()
					? MotorResult.success("waited", "waited " + elapsed + " ticks", Map.of())
					: MotorResult.running("waiting");
		} else {
			step = MotorResult.failed("unknown_command", String.valueOf(active));
		}
		if (step.terminal()) {
			active = null;
			result = step;
		}
		return result;
	}

	private MotorResult pollMove(MotorCommand.MoveTo move) {
		Vec3i target = move.target();
		double distance = body.getPos().distanceTo(new Vec3d(target.x() + 0.5, target.y(), target.z() + 0.5));
		if (distance <= Math.max(0.6, move.radius())) {
			body.getNavigation().stop();
			return MotorResult.success("arrived", "arrived within " + String.format("%.2f", distance), Map.of("distance", String.format("%.2f", distance)));
		}
		if (elapsed > move.timeoutTicks()) {
			body.getNavigation().stop();
			return MotorResult.failed("timeout", "movement exceeded " + move.timeoutTicks() + " ticks");
		}
		if (body.getNavigation().isIdle() && elapsed > 10) {
			double now = body.getPos().distanceTo(new Vec3d(target.x() + 0.5, target.y(), target.z() + 0.5));
			if (now > Math.max(0.6, move.radius())) {
				return MotorResult.failed("unreachable", "navigation stopped " + String.format("%.2f", now) + " blocks short");
			}
		}
		return MotorResult.running("moving");
	}

	private MotorResult pollLook(MotorCommand.LookAt look) {
		Vec3d eye = body.getEyePos();
		Vec3d center = Vec3d.ofCenter(new BlockPos(look.target().x(), look.target().y(), look.target().z()));
		double dx = center.x - eye.x;
		double dy = center.y - eye.y;
		double dz = center.z - eye.z;
		double horizontal = Math.sqrt(dx * dx + dz * dz);
		float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
		float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));
		body.setYaw(yaw);
		body.setHeadYaw(yaw);
		body.setPitch(pitch);
		if (elapsed >= 2) {
			return MotorResult.success("oriented", "yaw=" + String.format("%.1f", yaw) + " pitch=" + String.format("%.1f", pitch), Map.of());
		}
		return MotorResult.running("orienting");
	}

	private MotorResult pollMine(MotorCommand.MineBlock mine) {
		World world = body.getWorld();
		BlockPos pos = new BlockPos(mine.target().x(), mine.target().y(), mine.target().z());
		BlockState state = world.getBlockState(pos);
		String blockId = Registries.BLOCK.getId(state.getBlock()).toString();

		if (blockId.equals("minecraft:air")) {
			return MotorResult.success("already_gone", "target is already air", Map.of("block_removed", "true"));
		}
		if (!blockId.equals(mine.expectedBlockId())) {
			return MotorResult.failed("block_changed", "expected " + mine.expectedBlockId() + " but found " + blockId);
		}
		double eyeDistanceSq = body.getEyePos().squaredDistanceTo(Vec3d.ofCenter(pos));
		if (eyeDistanceSq > REACH_SQUARED) {
			return MotorResult.failed("out_of_reach", "target " + String.format("%.2f", Math.sqrt(eyeDistanceSq)) + " blocks from reach");
		}
		float hardness = state.getHardness(world, pos);
		if (hardness < 0.0f) {
			return MotorResult.failed("unbreakable", blockId + " cannot be broken");
		}
		ItemStack tool = body.getStackInHand(Hand.MAIN_HAND);
		float speed = tool.getMiningSpeedMultiplier(state);
		boolean canHarvest = !state.isToolRequired() || tool.isSuitableFor(state);
		breakProgress += speed / hardness / (canHarvest ? 30.0 : 100.0);
		if (breakProgress < 1.0) {
			if (elapsed > mine.timeoutTicks()) {
				return MotorResult.failed("timeout", "mining exceeded " + mine.timeoutTicks() + " ticks");
			}
			return MotorResult.running("mining " + String.format("%.2f", breakProgress));
		}
		boolean removed = world.breakBlock(pos, true, body, 3);
		world.playSound(null, pos, SoundEvents.BLOCK_WOOD_BREAK, net.minecraft.sound.SoundCategory.BLOCKS, 0.8f, 0.9f);
		Map<String, String> facts = new LinkedHashMap<>();
		facts.put("block_removed", Boolean.toString(removed));
		facts.put("block_id", blockId);
		return removed
				? MotorResult.success("block_removed", "broke " + blockId + " at " + mine.target(), facts)
				: MotorResult.failed("break_failed", "world refused to break " + blockId);
	}

	private MotorResult pollCollect(MotorCommand.CollectDrops collect) {
		World world = body.getWorld();
		Box area = body.getBoundingBox().expand(collect.radius());
		List<ItemEntity> items = world.getEntitiesByClass(ItemEntity.class, area, e -> !e.getStack().isEmpty());
		if (items.isEmpty()) {
			Map<String, String> facts = new LinkedHashMap<>();
			facts.put("collected", Integer.toString(walkedItemCount));
			return MotorResult.success("collected", "no drops remain nearby", facts);
		}
		ItemEntity nearest = items.get(0);
		for (ItemEntity item : items) {
			if (item.squaredDistanceTo(body) < nearest.squaredDistanceTo(body)) {
				nearest = item;
			}
		}
		if (elapsed > collect.timeoutTicks()) {
			return MotorResult.failed("drops_unreachable", items.size() + " drop(s) still on the ground");
		}
		if (!walkedToDrop) {
			body.getNavigation().stop();
			walkedToDrop = true;
		}
		// Direct movement control: walk onto the drop the way a player walks over it.
		body.getMoveControl().moveTo(nearest.getX(), nearest.getY(), nearest.getZ(), 1.2);
		body.getLookControl().lookAt(nearest);
		if (elapsed % 20 == 0) {
			EnsouledMod.LOG.info("[ensouled] collect: items={} nearest={} body={} dist={}", items.size(),
					nearest.getBlockPos(), body.getBlockPos(),
					String.format("%.2f", Math.sqrt(nearest.squaredDistanceTo(body))));
		}
		return MotorResult.running("walking to drop");
	}

	private MotorResult pollEat(MotorCommand.Eat eat) {
		int foodBefore = body.getFood();
		int countBefore = body.countItem(eat.itemId());
		if (countBefore <= 0) {
			return MotorResult.failed("no_food_item", "inventory has no " + eat.itemId());
		}
		if (!body.consumeItem(eat.itemId(), 1)) {
			return MotorResult.failed("consume_failed", "could not consume " + eat.itemId());
		}
		body.setFood(Math.min(20, foodBefore + 6));
		body.getWorld().playSound(null, body.getBlockPos(), SoundEvents.ENTITY_PLAYER_BURP,
				net.minecraft.sound.SoundCategory.PLAYERS, 0.5f, 1.0f);
		Map<String, String> facts = new LinkedHashMap<>();
		facts.put("food_before", Integer.toString(foodBefore));
		facts.put("food_after", Integer.toString(body.getFood()));
		facts.put("item_consumed", eat.itemId());
		return MotorResult.success("ate", "consumed " + eat.itemId(), facts);
	}

	private MotorResult pollAway(MotorCommand.MoveAwayFrom away) {
		Vec3i from = away.from();
		Vec3d fromCenter = new Vec3d(from.x() + 0.5, from.y(), from.z() + 0.5);
		Vec3d awayVector = body.getPos().subtract(fromCenter);
		if (awayVector.lengthSquared() < 0.01) {
			awayVector = new Vec3d(1.0, 0.0, 0.0);
		}
		Vec3d direction = awayVector.normalize().multiply(Math.max(4, away.distance()));
		double targetX = body.getX() + direction.x;
		double targetZ = body.getZ() + direction.z;
		if (body.getNavigation().isIdle()) {
			body.getNavigation().startMovingTo(targetX, body.getY(), targetZ, 1.25);
		}
		if (body.getPos().distanceTo(fromCenter) >= away.distance() || elapsed > away.timeoutTicks()) {
			body.getNavigation().stop();
			return MotorResult.success("escaped", "moved away from hazard", Map.of());
		}
		return MotorResult.running("escaping");
	}

	@Override
	public void cancel() {
		if (active != null) {
			active = null;
			body.getNavigation().stop();
			result = MotorResult.cancelled("cancelled by controller");
		}
	}

	@Override
	public boolean busy() {
		return active != null;
	}

	public AgentBodyEntity body() {
		return body;
	}
}

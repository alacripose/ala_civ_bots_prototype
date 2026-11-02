package dev.ensouled.body;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.ensouled.EnsouledMod;
import dev.ensouled.core.Motor;
import dev.ensouled.core.MotorCommand;
import dev.ensouled.core.MotorResult;
import dev.ensouled.core.Vec3i;
import dev.ensouled.entity.FakePlayerBody;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Player CUA motor (Addendum C §C.4.1). It produces player-shaped input (yaw, forward/strafe,
 * jump) and player action packets (block-breaking start/abort) resolved by the real
 * {@link FakePlayerBody} and its vanilla interaction manager. No teleporting, no direct block or
 * inventory mutation.
 */
public final class PlayerMotor implements Motor {
	public static final double REACH = 4.5;
	private static final double REACH_SQUARED = REACH * REACH;

	private final FakePlayerBody player;

	private MotorCommand active;
	private int elapsed;
	private int breakingSequence;
	private int breakTicks;
	private boolean breakingStarted;
	private double jumpStartY;
	private int noProgressTicks;
	private int collected;
	private Vec3d lastPos = Vec3d.ZERO;
	private MotorResult result = MotorResult.success("idle", "no active program", Map.of());

	public PlayerMotor(FakePlayerBody player) {
		this.player = player;
	}

	@Override
	public void start(MotorCommand command) {
		if (active != null) {
			throw new IllegalStateException("motor already executing " + active.describe());
		}
		active = command;
		elapsed = 0;
		breakingStarted = false;
		noProgressTicks = 0;
		collected = 0;
		lastPos = player.getPos();
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
			step = pollMove(move.target(), move.radius(), move.timeoutTicks());
		} else if (active instanceof MotorCommand.MoveAwayFrom away) {
			step = pollAway(away);
		} else if (active instanceof MotorCommand.LookAt look) {
			step = pollLook(look);
		} else if (active instanceof MotorCommand.Jump jump) {
			step = pollJump(jump.timeoutTicks());
		} else if (active instanceof MotorCommand.Equip equip) {
			step = pollEquip(equip);
		} else if (active instanceof MotorCommand.Use use) {
			step = pollUse(use);
		} else if (active instanceof MotorCommand.MineBlock mine) {
			step = pollMine(mine);
		} else if (active instanceof MotorCommand.DropItem drop) {
			step = pollDrop(drop);
		} else if (active instanceof MotorCommand.CollectDrops collect) {
			step = pollCollect(collect);
		} else if (active instanceof MotorCommand.Eat eat) {
			step = pollEat(eat);
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

	private MotorResult pollMove(Vec3i target, double radius, int timeoutTicks) {
		Vec3d destination = new Vec3d(target.x() + 0.5, target.y(), target.z() + 0.5);
		double distance = player.getPos().distanceTo(destination);
		if (distance <= Math.max(0.7, radius)) {
			stopInput();
			return MotorResult.success("arrived", "arrived within " + String.format("%.2f", distance),
					Map.of("distance", String.format("%.2f", distance)));
		}
		if (elapsed > timeoutTicks) {
			stopInput();
			return MotorResult.failed("timeout", "movement exceeded " + timeoutTicks + " ticks");
		}
		steer(destination);
		if (elapsed % 20 == 0) {
			double moved = player.getPos().distanceTo(lastPos);
			lastPos = player.getPos();
			if (moved < 0.15) {
				noProgressTicks += 20;
				player.setJumping(true);
			} else {
				noProgressTicks = 0;
			}
			if (noProgressTicks >= 60) {
				stopInput();
				return MotorResult.failed("blocked", "no progress toward " + target + " at " + player.getBlockPos());
			}
		}
		return MotorResult.running("walking");
	}

	private void steer(Vec3d destination) {
		double dx = destination.x - player.getX();
		double dz = destination.z - player.getZ();
		float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
		player.setYaw(yaw);
		player.setHeadYaw(yaw);
		player.setPitch(0.0f);
		player.forwardSpeed = 1.0f;
		player.sidewaysSpeed = 0.0f;
	}

	private void stopInput() {
		player.forwardSpeed = 0.0f;
		player.sidewaysSpeed = 0.0f;
		player.setJumping(false);
	}

	private MotorResult pollLook(MotorCommand.LookAt look) {
		Vec3d center = Vec3d.ofCenter(new BlockPos(look.target().x(), look.target().y(), look.target().z()));
		Vec3d eye = player.getEyePos();
		double dx = center.x - eye.x;
		double dy = center.y - eye.y;
		double dz = center.z - eye.z;
		float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
		float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
		player.setYaw(yaw);
		player.setHeadYaw(yaw);
		player.setPitch(pitch);
		if (elapsed >= 2) {
			return MotorResult.success("oriented", "yaw=" + String.format("%.1f", yaw), Map.of());
		}
		return MotorResult.running("orienting");
	}

	private MotorResult pollJump(int timeoutTicks) {
		if (elapsed == 1) {
			jumpStartY = player.getY();
		}
		double risen = player.getY() - jumpStartY;
		if (risen > 0.2) {
			player.setJumping(false);
			return MotorResult.success("jumped", "Y rose " + String.format("%.2f", risen),
					Map.of("y_before", String.format("%.2f", jumpStartY),
							"y_after", String.format("%.2f", player.getY())));
		}
		if (elapsed > timeoutTicks) {
			player.setJumping(false);
			return MotorResult.failed("jump_failed", "Y stayed at " + String.format("%.2f", player.getY())
					+ " (onGround=" + player.isOnGround() + ")");
		}
		// Same jump flag a client sends while the space bar is held; vanilla only lifts off from ground.
		player.setJumping(true);
		return MotorResult.running("jumping");
	}

	private MotorResult pollEquip(MotorCommand.Equip equip) {
		String held = Registries.ITEM.getId(player.getMainHandStack().getItem()).toString();
		if (elapsed > 1 && held.equals(equip.itemId())) {
			return MotorResult.success("equipped", "holding " + held,
					Map.of("slot", Integer.toString(player.getInventory().selectedSlot), "item", held));
		}
		if (elapsed == 1) {
			int slot = hotbarSlot(equip.itemId());
			if (slot < 0) {
				return MotorResult.failed("not_in_hotbar", "no " + equip.itemId() + " in hotbar slots 0-8");
			}
			// Vanilla packet path, which clamps to the hotbar and rejects an unchanged slot itself.
			player.selectHotbarSlot(slot);
			return MotorResult.running("equipping");
		}
		return MotorResult.running("equipping");
	}

	private MotorResult pollUse(MotorCommand.Use use) {
		World world = player.getWorld();
		BlockPos clicked = new BlockPos(use.target().x(), use.target().y(), use.target().z());
		Vec3d center = Vec3d.ofCenter(clicked);
		Direction face = Direction.getFacing(player.getEyePos().x - center.x,
				player.getEyePos().y - center.y, player.getEyePos().z - center.z);
		BlockPos placed = clicked.offset(face);
		String found = Registries.BLOCK.getId(world.getBlockState(placed).getBlock()).toString();

		if (found.equals(use.expectedBlockId())) {
			return MotorResult.success("placed", use.expectedBlockId() + " at " + placed,
					Map.of("placed_at", placed.toString(), "held", heldId()));
		}
		if (elapsed == 1) {
			ItemStack held = player.getMainHandStack();
			if (held.isEmpty()) {
				return MotorResult.failed("empty_hand", "nothing selected to use");
			}
			double reachSq = player.getEyePos().squaredDistanceTo(center);
			if (reachSq > REACH_SQUARED) {
				return MotorResult.failed("out_of_reach", "target " + String.format("%.2f", Math.sqrt(reachSq))
						+ " blocks away");
			}
			Vec3d hitVec = center.add(Vec3d.of(face.getVector()).multiply(0.45));
			aimAt(hitVec);
			// Vanilla's own interaction resolution (block use, then the selected item's use-on-block).
			ActionResult applied = player.interactBlock(new BlockHitResult(hitVec, face, clicked, false));
			if (!applied.isAccepted()) {
				return MotorResult.failed("use_refused", "vanilla interaction returned " + applied
						+ " for " + heldId() + " on " + clicked + " face " + face);
			}
			return MotorResult.running("using");
		}
		if (elapsed > use.timeoutTicks()) {
			return MotorResult.failed("use_failed", "expected " + use.expectedBlockId() + " at " + placed
					+ " but found " + found + " (held " + heldId() + ")");
		}
		return MotorResult.running("using");
	}

	private String heldId() {
		return Registries.ITEM.getId(player.getMainHandStack().getItem()).toString();
	}

	private int hotbarSlot(String itemId) {
		for (int slot = 0; slot < 9; slot++) {
			ItemStack stack = player.getInventory().getStack(slot);
			if (!stack.isEmpty() && Registries.ITEM.getId(stack.getItem()).toString().equals(itemId)) {
				return slot;
			}
		}
		return -1;
	}

	private void aimAt(Vec3d point) {
		Vec3d eye = player.getEyePos();
		double dx = point.x - eye.x;
		double dy = point.y - eye.y;
		double dz = point.z - eye.z;
		float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
		player.setYaw(yaw);
		player.setHeadYaw(yaw);
		player.setPitch((float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz))));
	}

	private MotorResult pollMine(MotorCommand.MineBlock mine) {
		World world = player.getWorld();
		BlockPos pos = new BlockPos(mine.target().x(), mine.target().y(), mine.target().z());
		BlockState state = world.getBlockState(pos);
		String blockId = Registries.BLOCK.getId(state.getBlock()).toString();

		if (blockId.equals("minecraft:air")) {
			return breakingStarted
					? MotorResult.success("block_removed", "broke " + mine.expectedBlockId() + " at " + pos,
							Map.of("block_id", mine.expectedBlockId(), "block_removed", "true"))
					: MotorResult.success("already_gone", "target is already air", Map.of("block_removed", "true"));
		}
		if (!blockId.equals(mine.expectedBlockId())) {
			return MotorResult.failed("block_changed", "expected " + mine.expectedBlockId() + " but found " + blockId);
		}
		double eyeDistanceSq = player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(pos));
		if (eyeDistanceSq > REACH_SQUARED) {
			return MotorResult.failed("out_of_reach", "target " + String.format("%.2f", Math.sqrt(eyeDistanceSq)) + " blocks away");
		}
		// Face the block the way a player must before mining.
		Vec3d center = Vec3d.ofCenter(pos);
		Vec3d eye = player.getEyePos();
		player.setYaw((float) (Math.toDegrees(Math.atan2(center.z - eye.z, center.x - eye.x)) - 90.0));
		player.setPitch((float) -Math.toDegrees(Math.atan2(center.y - eye.y,
				Math.hypot(center.x - eye.x, center.z - eye.z))));

		if (elapsed == 1) {
			// Same code path as an incoming player packet: vanilla records the mining target, tells
			// nearby clients that progress started, and expects the matching stop action later.
			player.interactionManager.processBlockBreakingAction(pos,
					PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, Direction.UP, breakingSequence++, world.getHeight());
			breakingStarted = true;
			float delta = state.calcBlockBreakingDelta(player, world, pos);
			// The server accepts the stop once elapsed mining time covers the block's hardness.
			breakTicks = Math.max(2, (int) Math.ceil(0.7 / Math.max(delta, 1.0e-4f)));
			return MotorResult.running("mining");
		}
		if (state.getHardness(world, pos) < 0.0f) {
			abortBreak(pos);
			return MotorResult.failed("unbreakable", blockId + " cannot be broken");
		}
		if (elapsed > mine.timeoutTicks()) {
			abortBreak(pos);
			return MotorResult.failed("timeout", "mining exceeded " + mine.timeoutTicks() + " ticks");
		}
		if (elapsed > breakTicks) {
			// A vanilla client stops the action when its local timer says the block is done; the server
			// then re-checks the elapsed time and applies the real break, drop and tool wear.
			player.interactionManager.processBlockBreakingAction(pos,
					PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, Direction.UP, breakingSequence++, world.getHeight());
			return MotorResult.running("finishing break");
		}
		if (elapsed % 20 == 0) {
			EnsouledMod.LOG.info("[ensouled] player mining {} progress tick {} of {}", blockId, elapsed, breakTicks);
		}
		return MotorResult.running("mining");
	}

	private void abortBreak(BlockPos pos) {
		player.interactionManager.processBlockBreakingAction(pos,
				PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, Direction.UP, breakingSequence++, player.getWorld().getHeight());
	}

	/**
	 * The drop key, not an inventory edit: split the held stack and hand it to the player's own drop
	 * path, which creates a real item entity owned by this body with the vanilla pickup delay.
	 */
	private MotorResult pollDrop(MotorCommand.DropItem drop) {
		int before = countItem(drop.itemId());
		if (before < drop.count()) {
			return MotorResult.failed("not_held", "inventory holds " + before + " " + drop.itemId()
					+ ", need " + drop.count());
		}
		int slot = stackSlot(drop.itemId());
		if (slot < 0) {
			return MotorResult.failed("not_held", "no " + drop.itemId() + " stack in the inventory");
		}
		ItemStack dropped = player.getInventory().getStack(slot).split(drop.count());
		ItemEntity entity = player.dropItem(dropped, false);
		int after = countItem(drop.itemId());
		Map<String, String> facts = new LinkedHashMap<>();
		facts.put("item", drop.itemId());
		facts.put("count", Integer.toString(drop.count()));
		facts.put("inventory_before", Integer.toString(before));
		facts.put("inventory_after", Integer.toString(after));
		if (entity == null || after != before - drop.count()) {
			return MotorResult.failed("drop_refused", "the world did not accept the dropped stack"
					+ " (before=" + before + " after=" + after + ")");
		}
		facts.put("stack_at", entity.getBlockPos().toString());
		return MotorResult.success("dropped", "dropped " + drop.count() + " " + drop.itemId(), facts);
	}

	private int countItem(String itemId) {
		int total = 0;
		for (int i = 0; i < player.getInventory().size(); i++) {
			ItemStack stack = player.getInventory().getStack(i);
			if (!stack.isEmpty() && Registries.ITEM.getId(stack.getItem()).toString().equals(itemId)) {
				total += stack.getCount();
			}
		}
		return total;
	}

	private int stackSlot(String itemId) {
		for (int i = 0; i < player.getInventory().size(); i++) {
			ItemStack stack = player.getInventory().getStack(i);
			if (!stack.isEmpty() && Registries.ITEM.getId(stack.getItem()).toString().equals(itemId)) {
				return i;
			}
		}
		return -1;
	}

	private MotorResult pollCollect(MotorCommand.CollectDrops collect) {
		World world = player.getWorld();
		Box area = player.getBoundingBox().expand(collect.radius());
		List<ItemEntity> items = world.getEntitiesByClass(ItemEntity.class, area, e -> !e.getStack().isEmpty());
		if (items.isEmpty()) {
			Map<String, String> facts = new LinkedHashMap<>();
			facts.put("collected", Integer.toString(collected));
			return MotorResult.success("collected", "no drops remain nearby", facts);
		}
		if (elapsed > collect.timeoutTicks()) {
			return MotorResult.failed("drops_unreachable", items.size() + " drop(s) still on the ground");
		}
		ItemEntity nearest = items.get(0);
		for (ItemEntity item : items) {
			if (item.squaredDistanceTo(player) < nearest.squaredDistanceTo(player)) {
				nearest = item;
			}
		}
		collected = items.size();
		steer(new Vec3d(nearest.getX(), nearest.getY(), nearest.getZ()));
		return MotorResult.running("walking to drop");
	}

	private MotorResult pollEat(MotorCommand.Eat eat) {
		int foodBefore = player.getHungerManager().getFoodLevel();
		ItemStack food = ItemStack.EMPTY;
		for (int i = 0; i < player.getInventory().size(); i++) {
			ItemStack stack = player.getInventory().getStack(i);
			if (!stack.isEmpty() && Registries.ITEM.getId(stack.getItem()).toString().equals(eat.itemId())) {
				food = stack;
				break;
			}
		}
		if (food.isEmpty()) {
			return MotorResult.failed("no_food_item", "inventory has no " + eat.itemId());
		}
		player.getHungerManager().eat(food.getItem(), food);
		food.decrement(1);
		player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_PLAYER_BURP,
				net.minecraft.sound.SoundCategory.PLAYERS, 0.5f, 1.0f);
		Map<String, String> facts = new LinkedHashMap<>();
		facts.put("food_before", Integer.toString(foodBefore));
		facts.put("food_after", Integer.toString(player.getHungerManager().getFoodLevel()));
		facts.put("item_consumed", eat.itemId());
		return MotorResult.success("ate", "consumed " + eat.itemId(), facts);
	}

	private MotorResult pollAway(MotorCommand.MoveAwayFrom away) {
		Vec3d from = new Vec3d(away.from().x() + 0.5, away.from().y(), away.from().z() + 0.5);
		Vec3d awayVector = player.getPos().subtract(from);
		if (awayVector.lengthSquared() < 0.01) {
			awayVector = new Vec3d(1.0, 0.0, 0.0);
		}
		Vec3d destination = player.getPos().add(awayVector.normalize().multiply(Math.max(4, away.distance())));
		if (player.getPos().distanceTo(from) >= away.distance() || elapsed > away.timeoutTicks()) {
			stopInput();
			return MotorResult.success("escaped", "moved away from hazard", Map.of());
		}
		steer(destination);
		return MotorResult.running("escaping");
	}

	@Override
	public void cancel() {
		if (active != null) {
			stopInput();
			active = null;
			result = MotorResult.cancelled("cancelled by controller");
		}
	}

	@Override
	public boolean busy() {
		return active != null;
	}

	public FakePlayerBody player() {
		return player;
	}
}

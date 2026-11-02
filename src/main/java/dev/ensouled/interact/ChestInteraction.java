package dev.ensouled.interact;

import java.util.Map;

import dev.ensouled.body.PlayerMotor;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Headless, server-authoritative chest transfer (Addendum C §C.5.2).
 *
 * <p>A body may have no client (legacy in-process fake player) or may have one that does not yet drive
 * container screens, so this adapter performs the transfer against the real {@link ChestBlockEntity}
 * inventory with the rules a closed interaction still owes the server: the object must still be the
 * chest that was perceived, the actor must be within player reach, the owner policy must allow it,
 * item counts are moved through the inventory API, and the before/after counts must agree before
 * anything is reported as done. When the
 * actor's inventory (or the chest) cannot take the whole stack, the already-extracted part is put
 * back and the operation is refused for that reason, so no item is ever conjured or destroyed.
 *
 * <p><b>Recorded divergence from a connected client:</b> a human client's transfer travels through
 * {@code ScreenHandler} slot clicks, which also synchronise the open screen and fire the menu/screen
 * hooks other mods listen to. This adapter calls the container inventory directly and therefore
 * skips screen synchronisation and screen-level hooks. It performs the same reach, slot and count
 * checks, and records that difference rather than presenting itself as a real click.
 */
public final class ChestInteraction {
	public static final String KIND = "chest";
	public static final String BLOCK_ID = "minecraft:chest";
	private static final int CONTENT_SUMMARY_LIMIT = 6;

	private ChestInteraction() {
	}

	public static InteractionOutcome operate(ServerPlayerEntity player, InteractionTarget target,
			InteractionRequest request, OwnershipBoundary boundary) {
		BlockPos pos = new BlockPos(target.pos().x(), target.pos().y(), target.pos().z());
		String blockId = Registries.BLOCK.getId(player.getWorld().getBlockState(pos).getBlock()).toString();
		if (!BLOCK_ID.equals(blockId)) {
			return InteractionOutcome.refused("unsupported_object",
					"expected " + BLOCK_ID + " at " + pos + " but found " + blockId);
		}
		BlockEntity entity = player.getWorld().getBlockEntity(pos);
		if (!(entity instanceof ChestBlockEntity chest)) {
			return InteractionOutcome.refused("container_missing", "no chest inventory at " + pos);
		}
		double eyeDistance = Math.sqrt(player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(pos)));
		if (eyeDistance > PlayerMotor.REACH) {
			return InteractionOutcome.refused("out_of_reach", String.format(
					"%.2f blocks from the chest, reach is %.2f", eyeDistance, PlayerMotor.REACH));
		}
		if (!boundary.allows(target.objectId(), target)) {
			return InteractionOutcome.refused("protected_target", "owner policy denies " + target.objectId());
		}
		if (!target.supports(request.operation())) {
			return InteractionOutcome.refused("unsupported_operation",
					target.kind() + " does not support " + request.operation());
		}
		return switch (request.operation()) {
			case InteractionRequest.INSPECT -> inspect(chest, target, request);
			case InteractionRequest.WITHDRAW -> withdraw(player, chest, target, request);
			case InteractionRequest.DEPOSIT -> deposit(player, chest, target, request);
			default -> InteractionOutcome.refused("unsupported_operation",
					"unknown operation " + request.operation());
		};
	}

	private static InteractionOutcome inspect(ChestBlockEntity chest, InteractionTarget target,
			InteractionRequest request) {
		Map<String, String> facts = InteractionOutcome.facts(
				"object", target.objectId(),
				"container_size", chest.size(),
				"transfer_id", request.transferId());
		int listed = 0;
		for (int slot = 0; slot < chest.size() && listed < CONTENT_SUMMARY_LIMIT; slot++) {
			ItemStack stack = chest.getStack(slot);
			if (stack.isEmpty()) {
				continue;
			}
			String itemId = Registries.ITEM.getId(stack.getItem()).toString();
			facts.merge(itemId, Integer.toString(stack.getCount()),
					(left, right) -> Integer.toString(Integer.parseInt(left) + Integer.parseInt(right)));
			facts.put("slot" + listed, itemId + "x" + stack.getCount());
			listed++;
		}
		return InteractionOutcome.performed("inspected", "read " + listed + " stack(s) from " + target.objectId(),
				facts);
	}

	private static InteractionOutcome withdraw(ServerPlayerEntity player, ChestBlockEntity chest,
			InteractionTarget target, InteractionRequest request) {
		int want = request.count();
		if (want <= 0) {
			return InteractionOutcome.refused("bad_request", "withdraw needs a positive count");
		}
		int chestBefore = count(chest, request.itemId());
		if (chestBefore <= 0) {
			return InteractionOutcome.refused("item_not_in_container",
					target.objectId() + " holds no " + request.itemId());
		}
		int moved = Math.min(want, chestBefore);
		int inventoryBefore = count(player.getInventory(), request.itemId());
		ItemStack taken = extract(chest, request.itemId(), moved);
		player.getInventory().insertStack(taken);
		int leftover = taken.getCount();
		if (leftover > 0) {
			int returned = insert(chest, taken);
			if (returned > 0) {
				return InteractionOutcome.failed("rollback_failed",
						"took " + moved + " but could not return " + returned,
						InteractionOutcome.facts("object", target.objectId(), "transfer_id", request.transferId()));
			}
			return InteractionOutcome.refused("inventory_full",
					"the body could take " + (moved - leftover) + " of " + moved + "; nothing was kept");
		}
		int chestAfter = count(chest, request.itemId());
		int inventoryAfter = count(player.getInventory(), request.itemId());
		Map<String, String> facts = InteractionOutcome.facts(
				"object", target.objectId(),
				"operation", InteractionRequest.WITHDRAW,
				"item", request.itemId(),
				"requested", want,
				"moved", moved,
				"container_before", chestBefore,
				"container_after", chestAfter,
				"inventory_before", inventoryBefore,
				"inventory_after", inventoryAfter,
				"transfer_id", request.transferId());
		if (chestBefore - chestAfter != moved || inventoryAfter - inventoryBefore != moved) {
			return InteractionOutcome.failed("verification_failed",
					"container " + chestBefore + "->" + chestAfter + ", inventory " + inventoryBefore + "->"
							+ inventoryAfter + " for a move of " + moved,
					facts);
		}
		return InteractionOutcome.performed("withdrawn",
				"withdrew " + moved + " " + request.itemId() + " from " + target.objectId(), facts);
	}

	private static InteractionOutcome deposit(ServerPlayerEntity player, ChestBlockEntity chest,
			InteractionTarget target, InteractionRequest request) {
		int want = request.count();
		if (want <= 0) {
			return InteractionOutcome.refused("bad_request", "deposit needs a positive count");
		}
		int inventoryBefore = count(player.getInventory(), request.itemId());
		if (inventoryBefore <= 0) {
			return InteractionOutcome.refused("item_not_in_inventory",
					"the body holds no " + request.itemId());
		}
		int moved = Math.min(want, inventoryBefore);
		int chestBefore = count(chest, request.itemId());
		ItemStack taken = extract(player.getInventory(), request.itemId(), moved);
		int leftover = insert(chest, taken);
		if (leftover > 0) {
			player.getInventory().insertStack(taken);
			return InteractionOutcome.refused("container_full",
					"the chest could take " + (moved - leftover) + " of " + moved + "; nothing was kept");
		}
		int chestAfter = count(chest, request.itemId());
		int inventoryAfter = count(player.getInventory(), request.itemId());
		Map<String, String> facts = InteractionOutcome.facts(
				"object", target.objectId(),
				"operation", InteractionRequest.DEPOSIT,
				"item", request.itemId(),
				"requested", want,
				"moved", moved,
				"container_before", chestBefore,
				"container_after", chestAfter,
				"inventory_before", inventoryBefore,
				"inventory_after", inventoryAfter,
				"transfer_id", request.transferId());
		if (chestAfter - chestBefore != moved || inventoryBefore - inventoryAfter != moved) {
			return InteractionOutcome.failed("verification_failed",
					"container " + chestBefore + "->" + chestAfter + ", inventory " + inventoryBefore + "->"
							+ inventoryAfter + " for a move of " + moved,
					facts);
		}
		return InteractionOutcome.performed("deposited",
				"deposited " + moved + " " + request.itemId() + " into " + target.objectId(), facts);
	}

	public static int count(Inventory inventory, String itemId) {
		int total = 0;
		for (int slot = 0; slot < inventory.size(); slot++) {
			ItemStack stack = inventory.getStack(slot);
			if (!stack.isEmpty() && Registries.ITEM.getId(stack.getItem()).toString().equals(itemId)) {
				total += stack.getCount();
			}
		}
		return total;
	}

	private static ItemStack extract(Inventory inventory, String itemId, int want) {
		ItemStack out = ItemStack.EMPTY;
		int remaining = want;
		for (int slot = 0; slot < inventory.size() && remaining > 0; slot++) {
			ItemStack stack = inventory.getStack(slot);
			if (stack.isEmpty() || !Registries.ITEM.getId(stack.getItem()).toString().equals(itemId)) {
				continue;
			}
			ItemStack piece = stack.split(Math.min(remaining, stack.getCount()));
			remaining -= piece.getCount();
			inventory.setStack(slot, stack);
			if (out.isEmpty()) {
				out = piece;
			} else {
				out.setCount(out.getCount() + piece.getCount());
			}
		}
		return out;
	}

	/** Returns how many items could not be inserted. */
	private static int insert(Inventory inventory, ItemStack stack) {
		int remaining = stack.getCount();
		for (int slot = 0; slot < inventory.size() && remaining > 0; slot++) {
			ItemStack existing = inventory.getStack(slot);
			if (existing.isEmpty() || !ItemStack.canCombine(existing, stack)) {
				continue;
			}
			int space = Math.min(existing.getMaxCount(), inventory.getMaxCountPerStack()) - existing.getCount();
			int moved = Math.min(space, remaining);
			if (moved <= 0) {
				continue;
			}
			existing.setCount(existing.getCount() + moved);
			remaining -= moved;
			inventory.setStack(slot, existing);
		}
		for (int slot = 0; slot < inventory.size() && remaining > 0; slot++) {
			if (!inventory.getStack(slot).isEmpty() || !inventory.isValid(slot, stack)) {
				continue;
			}
			int moved = Math.min(inventory.getMaxCountPerStack(), remaining);
			ItemStack placed = stack.copy();
			placed.setCount(moved);
			inventory.setStack(slot, placed);
			remaining -= moved;
		}
		stack.setCount(remaining);
		return remaining;
	}
}

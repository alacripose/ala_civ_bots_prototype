package dev.ensouled.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

/**
 * The Phase 1 virtual-player body (§10.2 B): a persistent entity with real collision, movement,
 * pathfinding, item pickup by touch, an inventory and hunger state. It deliberately has no
 * privileged operations: every world change it makes happens through block breaking or placement
 * performed by the motor layer under normal game rules.
 */
public class AgentBodyEntity extends PathAwareEntity {
	public static final int INVENTORY_SIZE = 36;

	private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
	private String agentId = "";
	private int food = 20;

	public AgentBodyEntity(EntityType<? extends AgentBodyEntity> entityType, World world) {
		super(entityType, world);
		this.setPersistent();
		this.setCanPickUpLoot(false);
	}

	public String getAgentId() {
		return agentId;
	}

	public void setAgentId(String agentId) {
		this.agentId = agentId;
	}

	public int getFood() {
		return food;
	}

	public void setFood(int food) {
		this.food = Math.max(0, Math.min(20, food));
	}

	public DefaultedList<ItemStack> getAgentInventory() {
		return inventory;
	}

	@Override
	public void tick() {
		super.tick();
		if (getWorld().isClient()) {
			return;
		}
		tickHunger();
		pickUpTouchedItems();
	}

	private void tickHunger() {
		long time = getWorld().getTime();
		if (time % 400 == 0 && food > 0) {
			food--;
		}
		if (food == 0 && time % 80 == 0) {
			DamageSource source = getDamageSources().starve();
			damage(source, 1.0f);
		}
	}

	/** Vanilla player rule: an item is collected when it touches the body. */
	private void pickUpTouchedItems() {
		Box pickupBox = getBoundingBox().expand(0.6);
		for (ItemEntity item : getWorld().getEntitiesByClass(ItemEntity.class, pickupBox, e -> !e.getStack().isEmpty())) {
			ItemStack stack = item.getStack();
			ItemStack remainder = insertStack(stack);
			if (remainder.isEmpty()) {
				item.discard();
				playSound(SoundEvents.ENTITY_ITEM_PICKUP, 0.2f, 1.4f);
				dev.ensouled.EnsouledMod.LOG.info("[ensouled] {} picked up {} x{}", agentId, stack.getItem(), stack.getCount());
			} else {
				item.setStack(remainder);
			}
		}
	}

	public ItemStack insertStack(ItemStack stack) {
		ItemStack remaining = stack.copy();
		for (int i = 0; i < inventory.size() && !remaining.isEmpty(); i++) {
			ItemStack slot = inventory.get(i);
			if (slot.isEmpty()) {
				inventory.set(i, remaining.copy());
				remaining = ItemStack.EMPTY;
				break;
			}
			if (ItemStack.canCombine(slot, remaining)) {
				int space = slot.getMaxCount() - slot.getCount();
				int moved = Math.min(space, remaining.getCount());
				if (moved > 0) {
					slot.increment(moved);
					remaining.decrement(moved);
				}
			}
		}
		return remaining;
	}

	public int countItem(String itemId) {
		int total = 0;
		for (ItemStack stack : inventory) {
			if (!stack.isEmpty()) {
				String id = net.minecraft.registry.Registries.ITEM.getId(stack.getItem()).toString();
				if (id.equals(itemId)) {
					total += stack.getCount();
				}
			}
		}
		return total;
	}

	public boolean consumeItem(String itemId, int count) {
		int remaining = count;
		for (int i = 0; i < inventory.size() && remaining > 0; i++) {
			ItemStack stack = inventory.get(i);
			if (stack.isEmpty()) {
				continue;
			}
			String id = net.minecraft.registry.Registries.ITEM.getId(stack.getItem()).toString();
			if (!id.equals(itemId)) {
				continue;
			}
			int taken = Math.min(remaining, stack.getCount());
			stack.decrement(taken);
			remaining -= taken;
			if (stack.isEmpty()) {
				inventory.set(i, ItemStack.EMPTY);
			}
		}
		return remaining == 0;
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putString("AgentId", agentId);
		nbt.putInt("Food", food);
		NbtCompound inventoryNbt = new NbtCompound();
		Inventories.writeNbt(inventoryNbt, inventory);
		nbt.put("Inventory", inventoryNbt);
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		agentId = nbt.getString("AgentId");
		food = nbt.contains("Food") ? nbt.getInt("Food") : 20;
		inventory.clear();
		Inventories.readNbt(nbt.getCompound("Inventory"), inventory);
	}

	@Override
	public boolean isPersistent() {
		return true;
	}

	@Override
	public boolean canImmediatelyDespawn(double distanceSquared) {
		return false;
	}
}

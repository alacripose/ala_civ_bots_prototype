package dev.ensouled.body;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import dev.ensouled.core.DroppedItem;
import dev.ensouled.core.EntityStimulus;
import dev.ensouled.core.InventorySlot;
import dev.ensouled.core.NeedsState;
import dev.ensouled.core.Vec3i;
import dev.ensouled.core.VisibleBlock;
import dev.ensouled.core.WorldView;
import dev.ensouled.entity.AgentBodyEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * The sense budget of one mob body (§6.1). Blocks and entities outside the sight radius or without a
 * clear line of sight are not returned, and other agents' inventories and hidden health are never
 * exposed.
 */
public final class MinecraftWorldView implements WorldView {
	public static final int SIGHT_RADIUS = 16;

	private final AgentBodyEntity body;

	public MinecraftWorldView(AgentBodyEntity body) {
		this.body = body;
	}

	@Override
	public long tick() {
		return body.getWorld().getTime();
	}

	@Override
	public Vec3i bodyPos() {
		return BlockSight.toVec(body.getBlockPos());
	}

	@Override
	public String dimensionId() {
		return body.getWorld().getRegistryKey().getValue().toString();
	}

	@Override
	public NeedsState needs() {
		return new NeedsState(body.getFood(), (int) body.getHealth(), body.getAir(),
				body.isOnFire(), body.isSubmergedInWater(), body.fallDistance);
	}

	@Override
	public List<VisibleBlock> visibleBlocks(int radius) {
		return BlockSight.visibleBlocks(body.getWorld(), body, Math.min(radius, SIGHT_RADIUS), this::canReach);
	}

	@Override
	public Optional<VisibleBlock> blockAt(Vec3i pos) {
		return BlockSight.blockAt(body.getWorld(), body, pos, SIGHT_RADIUS, this::canReach);
	}

	@Override
	public boolean canReach(Vec3i pos) {
		World world = body.getWorld();
		BlockPos blockPos = new BlockPos(pos.x(), pos.y(), pos.z());
		if (body.getEyePos().squaredDistanceTo(Vec3d.ofCenter(blockPos)) <= MinecraftMotor.REACH_SQUARED
				&& BlockSight.hasLineOfSight(world, body, blockPos)) {
			return true;
		}
		return BlockSight.nearestStandingNear(world, bodyPos(), pos, 3).isPresent();
	}

	@Override
	public boolean isStandable(Vec3i pos) {
		return BlockSight.isStandable(body.getWorld(), new BlockPos(pos.x(), pos.y(), pos.z()));
	}

	@Override
	public Optional<Vec3i> nearestStandingNear(Vec3i target, int radius) {
		return BlockSight.nearestStandingNear(body.getWorld(), bodyPos(), target, radius);
	}

	@Override
	public List<InventorySlot> inventory() {
		List<InventorySlot> out = new ArrayList<>();
		List<ItemStack> stacks = body.getAgentInventory();
		for (int i = 0; i < stacks.size(); i++) {
			ItemStack stack = stacks.get(i);
			if (!stack.isEmpty()) {
				out.add(new InventorySlot(i, Registries.ITEM.getId(stack.getItem()).toString(), stack.getCount()));
			}
		}
		return out;
	}

	@Override
	public int countItem(String itemId) {
		return body.countItem(itemId);
	}

	@Override
	public List<EntityStimulus> entities(int radius) {
		World world = body.getWorld();
		Box box = body.getBoundingBox().expand(radius);
		List<LivingEntity> living = world.getEntitiesByClass(LivingEntity.class, box, e -> e != body && e.isAlive());
		List<EntityStimulus> out = new ArrayList<>();
		for (LivingEntity entity : living) {
			BlockPos pos = entity.getBlockPos();
			out.add(new EntityStimulus(entity.getUuidAsString(),
					Registries.ENTITY_TYPE.getId(entity.getType()).toString(),
					new Vec3i(pos.getX(), pos.getY(), pos.getZ()),
					Math.sqrt(body.squaredDistanceTo(entity)),
					body.canSee(entity),
					entity instanceof HostileEntity));
		}
		return out;
	}

	@Override
	public List<DroppedItem> droppedItems(int radius) {
		World world = body.getWorld();
		Box box = body.getBoundingBox().expand(radius);
		List<ItemEntity> items = world.getEntitiesByClass(ItemEntity.class, box,
				item -> !item.getStack().isEmpty());
		List<DroppedItem> out = new ArrayList<>();
		for (ItemEntity item : items) {
			BlockPos pos = item.getBlockPos();
			double distance = Math.sqrt(body.squaredDistanceTo(item));
			if (distance > radius) {
				continue;
			}
			out.add(new DroppedItem(item.getUuidAsString(),
					Registries.ITEM.getId(item.getStack().getItem()).toString(),
					item.getStack().getCount(), new Vec3i(pos.getX(), pos.getY(), pos.getZ()), distance));
		}
		out.sort(java.util.Comparator.comparingDouble(DroppedItem::distance));
		return out;
	}

	@Override
	public Vec3i perceptionAnchor() {
		return bodyPos();
	}
}

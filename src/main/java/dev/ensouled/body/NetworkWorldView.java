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
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** Player-body perception under the same sight budget as every other body. */
public final class NetworkWorldView implements WorldView {
	public static final int SIGHT_RADIUS = 16;

	private final ServerPlayerEntity player;

	public NetworkWorldView(ServerPlayerEntity player) {
		this.player = player;
	}

	@Override
	public long tick() {
		return player.getWorld().getTime();
	}

	@Override
	public Vec3i bodyPos() {
		return BlockSight.toVec(player.getBlockPos());
	}

	@Override
	public String dimensionId() {
		return player.getWorld().getRegistryKey().getValue().toString();
	}

	@Override
	public NeedsState needs() {
		return new NeedsState(player.getHungerManager().getFoodLevel(), (int) player.getHealth(),
				player.getAir(), player.isOnFire(), player.isSubmergedInWater(), player.fallDistance);
	}

	@Override
	public List<VisibleBlock> visibleBlocks(int radius) {
		return BlockSight.visibleBlocks(player.getWorld(), player, Math.min(radius, SIGHT_RADIUS), this::canReach);
	}

	@Override
	public Optional<VisibleBlock> blockAt(Vec3i pos) {
		return BlockSight.blockAt(player.getWorld(), player, pos, SIGHT_RADIUS, this::canReach);
	}

	@Override
	public boolean canReach(Vec3i pos) {
		World world = player.getWorld();
		BlockPos blockPos = new BlockPos(pos.x(), pos.y(), pos.z());
		if (player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(blockPos)) <= PlayerMotor.REACH * PlayerMotor.REACH
				&& BlockSight.hasLineOfSight(world, player, blockPos)) {
			return true;
		}
		return BlockSight.nearestStandingNear(world, bodyPos(), pos, 3).isPresent();
	}

	@Override
	public boolean isStandable(Vec3i pos) {
		return BlockSight.isStandable(player.getWorld(), new BlockPos(pos.x(), pos.y(), pos.z()));
	}

	@Override
	public Optional<Vec3i> nearestStandingNear(Vec3i target, int radius) {
		return BlockSight.nearestStandingNear(player.getWorld(), bodyPos(), target, radius);
	}

	@Override
	public List<InventorySlot> inventory() {
		List<InventorySlot> out = new ArrayList<>();
		for (int i = 0; i < player.getInventory().size(); i++) {
			ItemStack stack = player.getInventory().getStack(i);
			if (!stack.isEmpty()) {
				out.add(new InventorySlot(i, Registries.ITEM.getId(stack.getItem()).toString(), stack.getCount()));
			}
		}
		return out;
	}

	@Override
	public int countItem(String itemId) {
		int total = 0;
		for (int i = 0; i < player.getInventory().size(); i++) {
			ItemStack stack = player.getInventory().getStack(i);
			if (!stack.isEmpty() && Registries.ITEM.getId(stack.getItem()).toString().equals(itemId)) {
				total += stack.getCount();
			}
		}
		return total;
	}

	@Override
	public List<EntityStimulus> entities(int radius) {
		World world = player.getWorld();
		Box box = player.getBoundingBox().expand(radius);
		List<LivingEntity> living = world.getEntitiesByClass(LivingEntity.class, box, e -> e != player && e.isAlive());
		List<EntityStimulus> out = new ArrayList<>();
		for (LivingEntity entity : living) {
			BlockPos pos = entity.getBlockPos();
			out.add(new EntityStimulus(entity.getUuidAsString(),
					Registries.ENTITY_TYPE.getId(entity.getType()).toString(),
					new Vec3i(pos.getX(), pos.getY(), pos.getZ()),
					Math.sqrt(player.squaredDistanceTo(entity)),
					player.canSee(entity),
					entity instanceof HostileEntity));
		}
		return out;
	}

	@Override
	public List<DroppedItem> droppedItems(int radius) {
		World world = player.getWorld();
		Box box = player.getBoundingBox().expand(radius);
		List<ItemEntity> items = world.getEntitiesByClass(ItemEntity.class, box,
				item -> !item.getStack().isEmpty());
		List<DroppedItem> out = new ArrayList<>();
		for (ItemEntity item : items) {
			BlockPos pos = item.getBlockPos();
			double distance = Math.sqrt(player.squaredDistanceTo(item));
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

	/** Block-state spot check used only by the motor/tests, mirrors the mob body's view. */
	public BlockState blockState(Vec3i pos) {
		return player.getWorld().getBlockState(new BlockPos(pos.x(), pos.y(), pos.z()));
	}
}

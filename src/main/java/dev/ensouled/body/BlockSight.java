package dev.ensouled.body;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import dev.ensouled.core.Vec3i;
import dev.ensouled.core.VisibleBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

/**
 * Physical sight rules shared by every body type: limited radius, a collider raycast, and real
 * standability. A body may only perceive what a player could stand and look at.
 */
public final class BlockSight {
	private BlockSight() {
	}

	public static boolean hasLineOfSight(World world, Entity viewer, BlockPos pos) {
		Vec3d start = viewer.getEyePos();
		Vec3d end = Vec3d.ofCenter(pos);
		BlockHitResult hit = world.raycast(new RaycastContext(start, end,
				RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, viewer));
		return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(pos);
	}

	public static boolean isStandable(World world, BlockPos pos) {
		BlockState feet = world.getBlockState(pos);
		BlockState head = world.getBlockState(pos.up());
		BlockState floor = world.getBlockState(pos.down());
		boolean feetFree = feet.getCollisionShape(world, pos).isEmpty();
		boolean headFree = head.getCollisionShape(world, pos.up()).isEmpty();
		boolean floorSolid = !floor.getCollisionShape(world, pos.down()).isEmpty();
		return feetFree && headFree && floorSolid;
	}

	public static Optional<Vec3i> nearestStandingNear(World world, Vec3i bodyCenter, Vec3i target, int radius) {
		Vec3i best = null;
		double bestDistance = Double.MAX_VALUE;
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dy = -2; dy <= 2; dy++) {
				for (int dz = -radius; dz <= radius; dz++) {
					Vec3i candidate = target.offset(dx, dy, dz);
					if (candidate.distanceTo(target) > radius) {
						continue;
					}
					if (!isStandable(world, new BlockPos(candidate.x(), candidate.y(), candidate.z()))) {
						continue;
					}
					double distance = candidate.distanceTo(bodyCenter);
					if (distance < bestDistance) {
						bestDistance = distance;
						best = candidate;
					}
				}
			}
		}
		return Optional.ofNullable(best);
	}

	/** Blocks this close are perceived whatever they are; beyond it, only what matters is raycast. */
	public static final int DETAIL_RADIUS = 8;
	/** Sight is a budget, not a census: one perception pass raycasts at most this many blocks. */
	public static final int MAX_VISIBLE = 256;

	/**
	 * What this body can actually see: every non-air block within sight that it has a clear line of
	 * sight to. Ground close by is included, so a being looking around sees the place it stands in;
	 * logs are raycast at the full radius because they are what it can act on.
	 *
	 * <p>This used to be {@code visibleLogs}: the "view" was a log finder, which is why a being in a
	 * clearing reported that it perceived nothing. It still cannot see through a wall — the raycast
	 * is a collider ray from the eye, and a nearer block hides what is behind it.
	 */
	public static List<VisibleBlock> visibleBlocks(World world, Entity viewer, int radius,
			Predicate<Vec3i> reachable) {
		Vec3i center = toVec(viewer.getBlockPos());
		int span = Math.max(1, radius);
		List<BlockPos> logs = new ArrayList<>();
		List<BlockPos> nearby = new ArrayList<>();
		for (int dx = -span; dx <= span; dx++) {
			for (int dy = -span; dy <= span; dy++) {
				for (int dz = -span; dz <= span; dz++) {
					BlockPos pos = new BlockPos(center.x() + dx, center.y() + dy, center.z() + dz);
					BlockState state = world.getBlockState(pos);
					if (state.isAir()) {
						continue;
					}
					double distance = Math.sqrt(viewer.squaredDistanceTo(Vec3d.ofCenter(pos)));
					if (distance > span) {
						continue;
					}
					boolean log = state.isIn(BlockTags.LOGS);
					if (!log && distance > DETAIL_RADIUS) {
						continue;
					}
					if (log) {
						logs.add(pos);
					} else {
						nearby.add(pos);
					}
				}
			}
		}
		// Logs are the resource this world acts on. Raycasting the ground first exhausted the
		// sight budget before a nearby trunk was reached, so the Axis planner saw only one
		// candidate and could not decide to harvest. Examine logs first, then the close ground.
		Comparator<BlockPos> nearest = Comparator.comparingDouble(
				pos -> viewer.squaredDistanceTo(Vec3d.ofCenter(pos)));
		logs.sort(nearest);
		nearby.sort(nearest);
		List<VisibleBlock> out = new ArrayList<>();
		int raycasts = 0;
		for (BlockPos pos : logs) {
			if (raycasts++ >= MAX_VISIBLE) {
				break;
			}
			addIfVisible(world, viewer, pos, reachable, out);
		}
		for (BlockPos pos : nearby) {
			if (raycasts++ >= MAX_VISIBLE) {
				break;
			}
			addIfVisible(world, viewer, pos, reachable, out);
		}
		out.sort(Comparator.comparingDouble(VisibleBlock::distance));
		return out;
	}

	private static void addIfVisible(World world, Entity viewer, BlockPos pos,
			Predicate<Vec3i> reachable, List<VisibleBlock> out) {
		if (!hasLineOfSight(world, viewer, pos)) {
			return;
		}
		BlockState state = world.getBlockState(pos);
		Vec3i vec = toVec(pos);
		out.add(new VisibleBlock(vec, Registries.BLOCK.getId(state.getBlock()).toString(),
				Math.sqrt(viewer.squaredDistanceTo(Vec3d.ofCenter(pos))), true,
				state.isIn(BlockTags.LOGS) && reachable.test(vec)));
	}

	/** Natural-log stimuli within the radius that have a clear sight line. */
	public static List<VisibleBlock> visibleLogs(World world, Entity viewer, int radius, Predicate<Vec3i> reachable) {
		List<VisibleBlock> out = new ArrayList<>();
		for (VisibleBlock block : visibleBlocks(world, viewer, radius, reachable)) {
			if (isLog(world, block.pos())) {
				out.add(block);
			}
		}
		return out;
	}

	private static boolean isLog(World world, Vec3i pos) {
		return world.getBlockState(new BlockPos(pos.x(), pos.y(), pos.z())).isIn(BlockTags.LOGS);
	}

	public static Optional<VisibleBlock> blockAt(World world, Entity viewer, Vec3i pos, int maxDistance,
			Predicate<Vec3i> reachable) {
		BlockPos blockPos = new BlockPos(pos.x(), pos.y(), pos.z());
		double distance = Math.sqrt(viewer.squaredDistanceTo(Vec3d.ofCenter(blockPos)));
		if (distance > maxDistance || !hasLineOfSight(world, viewer, blockPos)) {
			return Optional.empty();
		}
		BlockState state = world.getBlockState(blockPos);
		return Optional.of(new VisibleBlock(pos, Registries.BLOCK.getId(state.getBlock()).toString(),
				distance, true, reachable.test(pos)));
	}

	public static Vec3i toVec(BlockPos pos) {
		return new Vec3i(pos.getX(), pos.getY(), pos.getZ());
	}
}

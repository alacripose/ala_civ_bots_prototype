package dev.ensouled.interact;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import dev.ensouled.body.BlockSight;
import dev.ensouled.body.PlayerMotor;
import dev.ensouled.core.Vec3i;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * World-object perception and operation for a player body, whether that body is the in-process legacy
 * fake player or the server player of a connected client: both are ordinary {@link ServerPlayerEntity}
 * instances with the same reach, inventory and container rules. Perception is the same physical rule
 * as everything else: bounded radius, a collider raycast, and a real distance. The adapter resolves
 * the object type again at operation time, so a chest that was replaced between perception and use is
 * refused rather than acted on.
 */
public final class PlayerInteractionHands implements InteractionHands {
	public static final int MAX_SCAN_RADIUS = 8;

	private final ServerPlayerEntity player;
	private final OwnershipBoundary boundary;

	public PlayerInteractionHands(ServerPlayerEntity player) {
		this(player, OwnershipBoundary.NONE);
	}

	public PlayerInteractionHands(ServerPlayerEntity player, OwnershipBoundary boundary) {
		this.player = player;
		this.boundary = boundary;
	}

	@Override
	public List<InteractionTarget> targets(int radius) {
		int scan = Math.max(1, Math.min(radius, MAX_SCAN_RADIUS));
		Vec3i center = BlockSight.toVec(player.getBlockPos());
		List<InteractionTarget> out = new ArrayList<>();
		for (int dx = -scan; dx <= scan; dx++) {
			for (int dy = -scan; dy <= scan; dy++) {
				for (int dz = -scan; dz <= scan; dz++) {
					BlockPos pos = new BlockPos(center.x() + dx, center.y() + dy, center.z() + dz);
					if (!(player.getWorld().getBlockEntity(pos) instanceof ChestBlockEntity)) {
						continue;
					}
					if (!BlockSight.hasLineOfSight(player.getWorld(), player, pos)) {
						continue;
					}
					double distance = Math.sqrt(player.getEyePos()
							.squaredDistanceTo(Vec3d.ofCenter(pos)));
					if (distance > scan) {
						continue;
					}
					Vec3i vec = BlockSight.toVec(pos);
					out.add(new InteractionTarget(InteractionTarget.idOf(ChestInteraction.KIND, vec),
							ChestInteraction.KIND, ChestInteraction.BLOCK_ID, vec, distance,
							distance <= PlayerMotor.REACH, InteractionHands.chestCapabilities()));
				}
			}
		}
		out.sort(Comparator.comparingDouble(InteractionTarget::distance));
		return out;
	}

	@Override
	public InteractionOutcome perform(InteractionTarget target, InteractionRequest request) {
		if (!ChestInteraction.KIND.equals(target.kind())) {
			return InteractionOutcome.refused("unsupported_object",
					"this body has no interaction path for " + target.kind());
		}
		return ChestInteraction.operate(player, target, request, boundary);
	}

	public ServerPlayerEntity player() {
		return player;
	}
}

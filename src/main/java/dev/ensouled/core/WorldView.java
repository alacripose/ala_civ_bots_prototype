package dev.ensouled.core;

import java.util.List;
import java.util.Optional;

/**
 * What the agent can perceive and verify, and nothing more (§6.1, §32 "Local knowledge").
 * Implementations must not expose remote chunk contents, other inventories, world seed or
 * command permissions.
 */
public interface WorldView {
	long tick();

	Vec3i bodyPos();

	String dimensionId();

	NeedsState needs();

	/** Blocks this body can perceive within its sight radius, nearest first. */
	List<VisibleBlock> visibleBlocks(int radius);

	/** A perceived block at exactly this position, if it is currently perceptible. */
	Optional<VisibleBlock> blockAt(Vec3i pos);

	boolean canReach(Vec3i pos);

	boolean isStandable(Vec3i pos);

	/** Nearest standing position adjacent to the target that exists and has a path attempt. */
	Optional<Vec3i> nearestStandingNear(Vec3i target, int radius);

	List<InventorySlot> inventory();

	int countItem(String itemId);

	List<EntityStimulus> entities(int radius);

	/**
	 * Item entities this body can see within the radius, nearest first. A body kind whose senses do
	 * not reach loose items reports none; that is an absence of perception, not a claim that no item
	 * exists.
	 */
	default List<DroppedItem> droppedItems(int radius) {
		return List.of();
	}

	Vec3i perceptionAnchor();
}

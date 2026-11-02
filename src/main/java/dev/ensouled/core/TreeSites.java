package dev.ensouled.core;

import java.util.Optional;

/**
 * A tree is remembered as one site: its lowest connected trunk block. Without this, a restarted
 * being could "discover" the upper logs of a tree it already felled and treat them as new resource.
 */
public final class TreeSites {
	private static final int MAX_DESCENT = 4;

	private TreeSites() {
	}

	public static Vec3i base(Vec3i pos, WorldView view) {
		Vec3i cursor = pos;
		for (int i = 0; i < MAX_DESCENT; i++) {
			Vec3i below = cursor.offset(0, -1, 0);
			Optional<VisibleBlock> block = view.blockAt(below);
			if (block.isEmpty() || !BlockIds.isNaturalLog(block.get().blockId())) {
				break;
			}
			cursor = below;
		}
		return cursor;
	}
}

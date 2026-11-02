package dev.ensouled.core;

/**
 * Phase 1 mechanics catalog predicates (§6.3). Only declared vanilla 1.20.1 ids are treated as
 * known; anything else is unknown rather than guessed.
 */
public final class BlockIds {
	private BlockIds() {
	}

	public static boolean isNaturalLog(String blockId) {
		if (blockId == null) {
			return false;
		}
		return blockId.endsWith("_log") || blockId.equals("minecraft:log") || blockId.equals("minecraft:log2");
	}

	public static boolean isEdible(String itemId) {
		if (itemId == null) {
			return false;
		}
		return switch (itemId) {
			case "minecraft:bread", "minecraft:apple", "minecraft:cooked_beef", "minecraft:cooked_porkchop",
					"minecraft:cooked_mutton", "minecraft:cooked_chicken", "minecraft:baked_potato",
					"minecraft:golden_apple", "minecraft:carrot", "minecraft:melon_slice" -> true;
			default -> false;
		};
	}

	public static String siteKey(String dimensionId, Vec3i pos) {
		return dimensionId + "@" + pos;
	}
}

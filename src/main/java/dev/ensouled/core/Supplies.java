package dev.ensouled.core;

import java.util.List;
import java.util.Locale;

/**
 * What a being is carrying, by kind rather than by one item id.
 *
 * <p>A body that holds four oak planks holds wood. Asking its inventory for
 * {@code minecraft:oak_log} and getting zero is a true answer to a different question, and a being
 * that answers the wrong question out loud claims to lack a thing it is holding. So the kinds live
 * here, once: a log, a wood block, a stem, a hyphae and a plank are all wood, and every speaker
 * reads the same rule instead of each embedding an item id.
 *
 * <p>The inventory is the body's own, so this is perception of itself, not knowledge of the world.
 */
public final class Supplies {
	/** The suffix families that count as wood; the prefix is a wood species. */
	private static final List<String> WOOD_SUFFIXES = List.of("_log", "_wood", "_stem", "_hyphae", "_planks");

	/** The item asked for when nothing wood is held, so a sentence always has a name to use. */
	public static final String FALLBACK_WOOD = "minecraft:oak_log";

	private Supplies() {
	}

	/** How many wood items this body is carrying, across every species and wood shape. */
	public static int wood(WorldView view) {
		int total = 0;
		for (InventorySlot slot : view.inventory()) {
			if (isWood(slot.itemId())) {
				total += slot.count();
			}
		}
		return total;
	}

	/**
	 * The wood item this body holds most of, so speech can name the thing actually in its hands.
	 * With no wood held this answers {@link #FALLBACK_WOOD}: the sentence then names the thing it is
	 * short of, which is the one case where naming an absent item is the honest report.
	 */
	public static String woodItem(WorldView view) {
		String best = FALLBACK_WOOD;
		int bestCount = 0;
		for (InventorySlot slot : view.inventory()) {
			if (isWood(slot.itemId()) && slot.count() > bestCount) {
				best = slot.itemId();
				bestCount = slot.count();
			}
		}
		return best;
	}

	public static boolean isWood(String itemId) {
		if (itemId == null) {
			return false;
		}
		String path = itemId.toLowerCase(Locale.ROOT);
		int colon = path.indexOf(':');
		if (colon >= 0) {
			path = path.substring(colon + 1);
		}
		for (String suffix : WOOD_SUFFIXES) {
			if (path.endsWith(suffix)) {
				return true;
			}
		}
		return false;
	}
}

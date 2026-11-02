package dev.ensouled.work;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import dev.ensouled.core.BlockIds;
import dev.ensouled.core.InventorySlot;
import dev.ensouled.core.WorldView;

/**
 * What a commitment owes, in the world's own terms: a kind of thing and how many of it.
 *
 * <p>A deliverable is checkable from the body's own inventory, which is the only thing a being knows
 * without asking anybody. That is deliberate: a commitment whose completion could only be judged by
 * the person who asked for it would be an instruction, not a commitment.
 *
 * <p>The names are a declared table, not a guess. "wood", "logs" and "oak logs" all name
 * {@link Kind#LOGS} because this world harvests logs; a word outside the table is not silently
 * mapped to something similar, it makes the wish ambiguous so the being can ask.
 */
public record Deliverable(Kind kind, int count) {

	/** The kinds of thing this world can be asked for, and how each is recognised in an inventory. */
	public enum Kind {
		LOGS("log", "logs", "wood", "oak", "spruce", "birch"),
		PLANKS("plank", "planks"),
		BREAD("bread", "bread"),
		APPLE("apple", "apples"),
		DIRT("dirt", "dirt"),
		STONE("stone", "stone", "cobblestone", "rock", "rocks");

		private final String singular;
		private final String plural;
		private final List<String> nouns;

		Kind(String singular, String plural, String... aliases) {
			this.singular = singular;
			this.plural = plural;
			List<String> words = new java.util.ArrayList<>();
			words.add(singular);
			words.add(plural);
			words.addAll(List.of(aliases));
			this.nouns = List.copyOf(words);
		}

		public List<String> nouns() {
			return nouns;
		}

		/** The world's word for one of them, and for more than one: "log" and "logs". */
		public String noun(boolean many) {
			return many ? plural : singular;
		}

		/** Whether this slot of the body's own inventory counts towards this kind. */
		public boolean matches(String itemId) {
			if (itemId == null) {
				return false;
			}
			return switch (this) {
				case LOGS -> BlockIds.isNaturalLog(itemId);
				case PLANKS -> itemId.endsWith("_planks");
				case BREAD -> itemId.equals("minecraft:bread");
				case APPLE -> itemId.equals("minecraft:apple");
				case DIRT -> itemId.equals("minecraft:dirt");
				case STONE -> itemId.equals("minecraft:stone") || itemId.equals("minecraft:cobblestone");
			};
		}
	}

	private static final Map<String, Kind> ALIASES = buildAliases();

	private static Map<String, Kind> buildAliases() {
		Map<String, Kind> aliases = new java.util.LinkedHashMap<>();
		for (Kind kind : Kind.values()) {
			for (String noun : kind.nouns()) {
				aliases.put(noun, kind);
			}
		}
		return Map.copyOf(aliases);
	}

	/**
	 * Read a deliverable out of an utterance, or nothing when it does not name both a thing this world
	 * knows and how many. Nothing is a legitimate answer: the caller asks a question instead of
	 * inventing the missing half.
	 */
	public static Optional<Deliverable> parse(String text) {
		if (text == null || text.isBlank()) {
			return Optional.empty();
		}
		Kind kind = null;
		int count = 0;
		for (String token : text.toLowerCase(Locale.ROOT).split("[^a-z0-9']+")) {
			if (token.isEmpty()) {
				continue;
			}
			if (token.chars().allMatch(Character::isDigit)) {
				if (count == 0) {
					try {
						count = Integer.parseInt(token);
					} catch (NumberFormatException tooLarge) {
						return Optional.empty();
					}
				}
				continue;
			}
			if (kind == null) {
				kind = ALIASES.get(token);
			}
		}
		if (kind == null || count < 1) {
			return Optional.empty();
		}
		return Optional.of(new Deliverable(kind, count));
	}

	/**
	 * Whether the words ask for construction rather than gathering. A build wish spends stock the
	 * being already carries, so it is not answered `already-held`: the count is what it must put
	 * into the world, not what it must collect.
	 */
	public static boolean builds(String text) {
		if (text == null) {
			return false;
		}
		for (String token : text.toLowerCase(Locale.ROOT).split("[^a-z0-9']+")) {
			if (token.equals("build") || token.equals("builds") || token.equals("place")
					|| token.equals("raise") || token.equals("wall")) {
				return true;
			}
		}
		return false;
	}

	/**
	 * The kind an utterance names, whether or not it says how many. This is what lets a being ask about
	 * the missing half instead of about the whole sentence.
	 */
	public static Optional<Kind> named(String text) {
		if (text == null) {
			return Optional.empty();
		}
		for (String token : text.toLowerCase(Locale.ROOT).split("[^a-z0-9']+")) {
			Kind kind = ALIASES.get(token);
			if (kind != null) {
				return Optional.of(kind);
			}
		}
		return Optional.empty();
	}

	/** How many of this kind the body is carrying, counted from its own inventory. */
	public int held(WorldView view) {
		if (view == null) {
			return 0;
		}
		int total = 0;
		for (InventorySlot slot : view.inventory()) {
			if (slot.count() > 0 && kind.matches(slot.itemId())) {
				total += slot.count();
			}
		}
		return total;
	}

	/** The checkable condition itself. */
	public boolean met(WorldView view) {
		return held(view) >= count;
	}

	/** The world's word for what was asked for, singular or plural. */
	public String words() {
		return kind.noun(count != 1);
	}

	/** What the condition says, for a record a reader can check against an inventory. */
	public String condition() {
		return kind.name().toLowerCase(Locale.ROOT) + " held >= " + count;
	}
}

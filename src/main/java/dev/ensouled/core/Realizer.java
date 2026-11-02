package dev.ensouled.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Composes one spoken sentence. The being supplies what it means and the order it means it in; the
 * words come from two places and nowhere else: the prose-logic corpus ({@link Lexicon}, through
 * {@link #word}) and the world's own names and the being's own numbers ({@link #value}).
 *
 * <p>A phrase with no attested word for one of its meanings cannot become a sentence: it reports the
 * meaning as unsayable instead of substituting a word this project invented. That is what keeps the
 * corpus the baseline of what may be said rather than a decoration on handmade lines.
 */
public final class Realizer {
	private final List<String> tokens = new ArrayList<>();
	private final List<String> unsayable = new ArrayList<>();

	/** Append the corpus's own word for this meaning; the most-used attested candidate wins. */
	public Realizer word(String... candidates) {
		Lexicon lexicon = Lexicon.corpus();
		var chosen = lexicon.prefer(candidates);
		if (chosen.isEmpty()) {
			unsayable.add(String.join("/", candidates));
			return this;
		}
		tokens.add(chosen.get());
		return this;
	}

	/** Append a datum the being owns: a world name, a number, another person's name. */
	public Realizer value(String literal) {
		String stripped = literal == null ? "" : literal.strip();
		if (!stripped.isEmpty()) {
			tokens.add(stripped);
		}
		return this;
	}

	public Realizer value(int number) {
		return value(Integer.toString(number));
	}

	public boolean sayable() {
		return unsayable.isEmpty() && !tokens.isEmpty();
	}

	/** Meanings the corpus has no word for; empty when the sentence can be spoken. */
	public List<String> unsayable() {
		return List.copyOf(unsayable);
	}

	/**
	 * The sentence as spoken: first letter capitalised, a full stop at the end. Punctuation and case
	 * are presentation, so they are applied here rather than counted as vocabulary.
	 */
	public String sentence() {
		if (tokens.isEmpty()) {
			return "";
		}
		String joined = String.join(" ", tokens);
		String capitalised = joined.substring(0, 1).toUpperCase(Locale.ROOT) + joined.substring(1);
		return capitalised.endsWith(".") || capitalised.endsWith("?") || capitalised.endsWith("!")
				? capitalised
				: capitalised + ".";
	}

	/**
	 * The same words asked rather than stated. A being that cannot read a wish asks about the half it
	 * is missing, and a question is not an assertion, so it carries a question mark.
	 */
	public String question() {
		String spoken = sentence();
		if (spoken.endsWith(".")) {
			spoken = spoken.substring(0, spoken.length() - 1);
		}
		return spoken.endsWith("?") ? spoken : spoken + "?";
	}

	/** The words of the composed sentence, for the evidence trail and tests. */
	public List<String> tokens() {
		return List.copyOf(tokens);
	}

	/**
	 * A world name as words: {@code oak_log} is the world's name for what it is, spaced so it can be
	 * read aloud. The name is the world's, not a synonym written here.
	 */
	/**
	 * A world name in the number it is being counted in. The plural is the corpus's word when it
	 * attests one ({@code logs}), and a name that is already plural keeps its own spelling: the
	 * world calls the item {@code spruce_planks}, and {@code plankss} is not a word anything says.
	 */
	public static String many(String name, int count) {
		if (name == null || name.isBlank() || count == 1) {
			return name == null ? "" : name;
		}
		String plural = name + "s";
		if (Lexicon.corpus().attests(plural)) {
			return plural;
		}
		return name.endsWith("s") ? name : plural;
	}

	public static String worldName(String itemId) {
		String path = itemId.contains(":") ? itemId.substring(itemId.indexOf(':') + 1) : itemId;
		return path.replace('_', ' ').strip();
	}
}

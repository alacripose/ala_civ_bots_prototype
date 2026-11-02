package dev.ensouled.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Where a resident's name comes from.
 *
 * <p>A being's name is not a string this project picks: it comes out of the same prose-logic corpus
 * the being speaks from. Each candidate is put to {@link Lexicon#corpus()}, the corpus answers with
 * its own word and how often it says it, and that word — capitalised — becomes the name. A candidate
 * the corpus never attests produces no name at all, which is the same rule the speech library lives
 * by: a concept the corpus cannot name is not sayable, so it is not nameable either.
 *
 * <p>This is the hook the dynamic verb library needs: the vocabulary that decides what a being can
 * say is the vocabulary that decides who it is.
 */
public final class ResidentNames {
	/** The candidate words, in the order names are handed out. */
	private static final List<String> CANDIDATES = List.of("ash", "rest", "bramble", "reed", "thorn");

	private ResidentNames() {
	}

	/** A name and the corpus word it was made from, with the corpus's own count as provenance. */
	public record Resident(String name, String word, int count) {
		@Override
		public String toString() {
			return name + " <- " + word + " (" + count + ")";
		}
	}

	/** The resident roster the corpus supports, in order. Never invented, never padded. */
	public static List<Resident> defaults() {
		List<Resident> names = new ArrayList<>();
		for (String candidate : CANDIDATES) {
			int count = Lexicon.corpus().count(candidate);
			if (count <= 0) {
				continue;
			}
			names.add(new Resident(capitalise(candidate), candidate, count));
		}
		return names;
	}

	private static String capitalise(String word) {
		return word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1);
	}
}

package dev.ensouled.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The action verbs the corpus actually uses, induced from where it puts them.
 *
 * <p>This is not a synonym list written here. {@code tools/prose_compile.py} reads every quoted
 * sentence in the prose-logic claims and counts a word as a verb only when the corpus places it in
 * a verb position: after a modal or auxiliary ("you must <em>bring</em> the wood"), or at the start
 * of an imperative ("<em>Gather</em> the logs"). The compiled table is
 * {@code prose_verbs.tsv}, so a capability does not carry hand-written English meanings and an
 * action is not spoken by its machine id.
 *
 * <p>The corpus is the training set. A word it never uses in either position is not a verb here,
 * and a skill whose name holds no attested verb is reported as {@code unattested} rather than
 * being given a word this project made up.
 */
public final class VerbLibrary {
	private static final VerbLibrary CORPUS = load();
	private static final String UNATTESTED = "unattested";

	private final Map<String, Integer> verbs;
	private final String source;

	private VerbLibrary(Map<String, Integer> verbs, String source) {
		this.verbs = verbs;
		this.source = source;
	}

	public static VerbLibrary corpus() {
		return CORPUS;
	}

	public String source() {
		return source;
	}

	public int size() {
		return verbs.size();
	}

	/** How often the corpus puts this word in a verb position; zero means it never does. */
	public int count(String word) {
		return verbs.getOrDefault(normalize(word), 0);
	}

	public boolean isVerb(String word) {
		return count(word) > 0;
	}

	/**
	 * The verb for a skill or action id: {@code harvest_tree} answers {@code harvest}. The id's own
	 * words are the candidates and the corpus's count is the only weighting, so the library adds no
	 * vocabulary of its own. An id holding no attested verb answers empty.
	 */
	public Optional<String> verbFor(String skillId) {
		String best = null;
		int bestCount = 0;
		for (String word : parts(skillId)) {
			int count = count(word);
			if (count > bestCount) {
				best = word;
				bestCount = count;
			}
		}
		return Optional.ofNullable(best);
	}

	/**
	 * The action as words a reader could hear: the attested verb first, then the id's remaining
	 * words. {@code harvest_tree} answers {@code harvest tree}; an id with no attested verb keeps
	 * its words in order so the record still names what happened.
	 */
	public String phrase(String skillId) {
		List<String> words = parts(skillId);
		if (words.isEmpty()) {
			return "";
		}
		Optional<String> verb = verbFor(skillId);
		if (verb.isEmpty()) {
			return String.join(" ", words);
		}
		List<String> ordered = new ArrayList<>();
		ordered.add(verb.get());
		for (String word : words) {
			if (!word.equals(verb.get())) {
				ordered.add(word);
			}
		}
		return String.join(" ", ordered);
	}

	/** {@code lexicon:<count>} when the action is named by an attested verb, else {@code unattested}. */
	public String basis(String skillId) {
		Optional<String> verb = verbFor(skillId);
		return verb.map(word -> "lexicon:" + count(word)).orElse(UNATTESTED);
	}

	/** Which verb the phrase was spoken with, or empty when the corpus attests none of its words. */
	public String spokenVerb(String skillId) {
		return verbFor(skillId).orElse("");
	}

	private static List<String> parts(String skillId) {
		List<String> out = new ArrayList<>();
		if (skillId == null) {
			return out;
		}
		for (String part : skillId.toLowerCase(Locale.ROOT).split("[^a-z]+")) {
			if (!part.isBlank()) {
				out.add(part);
			}
		}
		return out;
	}

	private static String normalize(String word) {
		return word == null ? "" : word.toLowerCase(Locale.ROOT).strip();
	}

	private static VerbLibrary load() {
		Map<String, Integer> parsed = new HashMap<>();
		String source = "resources/ensouled/prose_verbs.tsv";
		try (InputStream in = VerbLibrary.class.getResourceAsStream("/ensouled/prose_verbs.tsv")) {
			if (in == null) {
				return new VerbLibrary(parsed, source + " (missing)");
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			String row;
			while ((row = reader.readLine()) != null) {
				String line = row.strip();
				if (line.isEmpty() || line.startsWith("#")) {
					continue;
				}
				String[] columns = line.split("\t");
				if (columns.length < 2) {
					continue;
				}
				try {
					parsed.put(columns[0], Integer.parseInt(columns[1].strip()));
				} catch (NumberFormatException ignored) {
					// A malformed row is skipped rather than guessed at.
				}
			}
		} catch (IOException e) {
			return new VerbLibrary(parsed, source + " (unreadable)");
		}
		return new VerbLibrary(parsed, source);
	}
}

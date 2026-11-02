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
 * The words the prose-logic corpus actually attests. This is not a dictionary written here: it is
 * the vocabulary of the quotes in {@code agent_god_files/prose_logic}, compiled by
 * {@code tools/prose_compile.py} into {@code prose_lexicon.tsv}. A being may only speak words that
 * appear in it, plus the world's own names for what it perceives and its own numbers.
 *
 * <p>When a being needs a word for something it means, it states the candidates and this class
 * answers with the one the corpus uses most. That is the whole of the choice: no word is invented
 * here, and a concept the corpus never names is not sayable.
 */
public final class Lexicon {
	private static final Lexicon CORPUS = load();

	private final Map<String, Integer> words;
	private final String source;

	private Lexicon(Map<String, Integer> words, String source) {
		this.words = words;
		this.source = source;
	}

	public static Lexicon corpus() {
		return CORPUS;
	}

	public String source() {
		return source;
	}

	public int size() {
		return words.size();
	}

	/** How often the corpus says this word; zero means it never does. */
	public int count(String word) {
		return words.getOrDefault(normalize(word), 0);
	}

	public boolean attests(String word) {
		return count(word) > 0;
	}

	/**
	 * The corpus's own word for a meaning: among the candidates given, the one it says most often.
	 * Empty when the corpus attests none of them, which makes that meaning unsayable rather than
	 * silently substituting a word this project made up.
	 */
	public Optional<String> prefer(String... candidates) {
		String best = null;
		int bestCount = 0;
		for (String candidate : candidates) {
			int count = count(candidate);
			if (count > bestCount) {
				best = normalize(candidate);
				bestCount = count;
			}
		}
		return Optional.ofNullable(best);
	}

	/** Every word in the input the corpus does not attest, in order. */
	public List<String> unattested(List<String> tokens) {
		List<String> missing = new ArrayList<>();
		for (String token : tokens) {
			if (!attests(token)) {
				missing.add(token);
			}
		}
		return missing;
	}

	private static String normalize(String word) {
		return word.toLowerCase(Locale.ROOT).strip();
	}

	private static Lexicon load() {
		Map<String, Integer> parsed = new HashMap<>();
		String source = "resources/ensouled/prose_lexicon.tsv";
		try (InputStream in = Lexicon.class.getResourceAsStream("/ensouled/prose_lexicon.tsv")) {
			if (in == null) {
				return new Lexicon(parsed, source + " (missing)");
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			String row;
			while ((row = reader.readLine()) != null) {
				String line = row.strip();
				if (line.isEmpty() || line.startsWith("#")) {
					continue;
				}
				int tab = line.indexOf('\t');
				if (tab <= 0) {
					continue;
				}
				try {
					parsed.put(line.substring(0, tab), Integer.parseInt(line.substring(tab + 1).strip()));
				} catch (NumberFormatException ignored) {
					// A malformed row is skipped rather than guessed at.
				}
			}
		} catch (IOException e) {
			return new Lexicon(parsed, source + " (unreadable)");
		}
		return new Lexicon(parsed, source);
	}
}

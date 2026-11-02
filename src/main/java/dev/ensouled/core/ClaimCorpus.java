package dev.ensouled.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The claims this world has ingested from the prose-logic corpus, and the only source of speech.
 *
 * <p>There is no speech-act menu here: no GREET, WARN or ANSWER. The corpus already states
 * 121,501 claims in its own words, each with a verb the baseline assigns a kind and a modality.
 * A being that hears a line selects among those claims by the words it shares with what was said,
 * weighted by how rare the word is in the corpus, and speaks the claim it selected. The utterance
 * is therefore the corpus's, not a sentence this project invented, and the selection is a reading
 * of the heard line rather than a table of intended replies.
 *
 * <p>The table is compiled by {@code tools/prose_compile.py} from
 * {@code agent_god_files/prose_logic/store/claim.jsonl}. A claim is admissible only when its verb
 * is in {@link ProseLogic}'s baseline; the kind and modality come from the baseline, never from the
 * row. Common words are not indexed: a query matches on the words that actually distinguish one
 * claim from another, so "the" cannot decide what a being says.
 */
public final class ClaimCorpus {
	/** A word this common says nothing about which claim is relevant; it is not indexed. */
	private static final int MAX_POSTINGS = 2000;
	/** Shortest word worth matching; the corpus has no meaningful one- or two-letter content words. */
	private static final int MIN_WORD_LENGTH = 3;

	private static final Pattern WORD = Pattern.compile("[a-z]+(?:'[a-z]+)?");

	private static final ClaimCorpus CORPUS = load();

	/** One claim that shares words with what was heard, and the words it shares. */
	public record Hit(Claim claim, double score, List<String> shared) {
	}

	private final List<Claim> claims;
	private final Map<String, int[]> index;
	private final String source;

	private ClaimCorpus(List<Claim> claims, Map<String, int[]> index, String source) {
		this.claims = claims;
		this.index = index;
		this.source = source;
	}

	public static ClaimCorpus corpus() {
		return CORPUS;
	}

	public String source() {
		return source;
	}

	public int size() {
		return claims.size();
	}

	/**
	 * The claims that read what was heard, best first. A hit exists only when the line shares at
	 * least one indexed word with the claim; when nothing is shared there is nothing to say, and the
	 * caller records silence rather than inventing a reply.
	 */
	public List<Hit> relevant(String text, int limit) {
		List<String> words = words(text);
		if (words.isEmpty() || claims.isEmpty() || limit <= 0) {
			return List.of();
		}
		Map<Integer, Double> scores = new HashMap<>();
		Map<Integer, Set<String>> shared = new HashMap<>();
		double total = claims.size() + 1.0;
		for (String word : words) {
			int[] postings = index.get(word);
			if (postings == null || postings.length == 0) {
				continue;
			}
			// A word only a few claims use is evidence; a word a third of the corpus uses is not.
			double idf = Math.log(total / (postings.length + 1.0));
			for (int claim : postings) {
				scores.merge(claim, idf, Double::sum);
				shared.computeIfAbsent(claim, key -> new LinkedHashSet<>()).add(word);
			}
		}
		return scores.entrySet().stream()
				.sorted((a, b) -> {
					int byScore = Double.compare(b.getValue(), a.getValue());
					return byScore != 0 ? byScore : Integer.compare(a.getKey(), b.getKey());
				})
				.limit(limit)
				.map(entry -> new Hit(claims.get(entry.getKey()), entry.getValue(),
						List.copyOf(shared.get(entry.getKey()))))
				.toList();
	}

	/** Distinct content words in an utterance, in the order they were said. */
	private static List<String> words(String text) {
		if (text == null || text.isBlank()) {
			return List.of();
		}
		Set<String> distinct = new LinkedHashSet<>();
		Matcher matcher = WORD.matcher(text.toLowerCase(Locale.ROOT));
		while (matcher.find()) {
			String word = matcher.group();
			if (word.length() >= MIN_WORD_LENGTH) {
				distinct.add(word);
			}
		}
		return List.copyOf(distinct);
	}

	private static ClaimCorpus load() {
		String source = "resources/ensouled/prose_claims.tsv";
		List<Claim> claims = new ArrayList<>();
		try (InputStream in = ClaimCorpus.class.getResourceAsStream("/ensouled/prose_claims.tsv")) {
			if (in == null) {
				return new ClaimCorpus(List.of(), Map.of(), source + " (missing)");
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			String row;
			while ((row = reader.readLine()) != null) {
				String line = row.strip();
				if (line.isEmpty() || line.startsWith("#")) {
					continue;
				}
				String[] parts = line.split("\t", 3);
				if (parts.length < 3) {
					continue;
				}
				Optional<ProseLogic.Pair> pair = ProseLogic.baseline().pair(parts[0]);
				if (pair.isEmpty()) {
					// A verb the baseline does not hold is not a claim this world can state.
					continue;
				}
				claims.add(new Claim(parts[1], pair.get().verb(), pair.get().kind(), pair.get().modality(),
						parts[2], Claim.Support.DIRECT));
			}
		} catch (IOException e) {
			return new ClaimCorpus(List.of(), Map.of(), source + " (unreadable)");
		}

		// The inverted index is built once, over the words that distinguish claims. A word used by
		// more than MAX_POSTINGS claims is dropped: it would match almost everything and decide
		// nothing.
		Map<String, List<Integer>> postings = new HashMap<>();
		for (int claim = 0; claim < claims.size(); claim++) {
			for (String word : words(claims.get(claim).quote())) {
				postings.computeIfAbsent(word, key -> new ArrayList<>()).add(claim);
			}
		}
		Map<String, int[]> index = new HashMap<>();
		for (Map.Entry<String, List<Integer>> entry : postings.entrySet()) {
			if (entry.getValue().size() > MAX_POSTINGS) {
				continue;
			}
			int[] rows = new int[entry.getValue().size()];
			for (int i = 0; i < rows.length; i++) {
				rows[i] = entry.getValue().get(i);
			}
			index.put(entry.getKey(), rows);
		}
		return new ClaimCorpus(List.copyOf(claims), Map.copyOf(index), source);
	}
}

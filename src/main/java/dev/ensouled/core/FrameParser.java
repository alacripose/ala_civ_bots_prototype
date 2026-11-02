package dev.ensouled.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns one utterance into a {@link VerbFrame}: who said it, what verb it means, on what thing, how
 * many, to whom, and which of the corpus's claim shapes it is stated in.
 *
 * <p>The parser does not own a vocabulary. Two compiled tables decide everything it says:
 * {@link ProseLogic} supplies the claim shapes and {@link Lexicon} supplies the words. The mapping
 * from a sentence's wording to a claim shape is {@code prose_cues.tsv}, compiled from the corpus's
 * own quotes by {@code tools/prose_compile.py}; a sentence is not assigned a shape by a table of
 * hand-written phrases, and when no shape has enough evidence behind it the frame says so instead
 * of guessing.
 *
 * <p>A verb is derivable at the capability boundary it was read at, not from a master list (the
 * design record's Q292: retire the verb enumeration, derive actions from capabilities). So the
 * frame carries what the parse actually established and nothing it assumed.
 */
public final class FrameParser {
	/** Grammar, not vocabulary: these are the closed classes every parser needs. */
	private static final List<String> DETERMINERS = List.of("the", "a", "an", "my", "your", "his", "her",
			"their", "our", "some", "any", "this", "that", "these", "those", "each", "every", "more");
	private static final List<String> PREPOSITIONS = List.of("to", "for", "with", "from", "of", "in", "on",
			"at", "by", "into", "out", "about");
	private static final List<String> PRONOUNS = List.of("i", "you", "he", "she", "we", "they", "it", "me",
			"him", "them", "us");
	private static final List<String> MUST_AUX = List.of("must", "shall", "will", "should", "ought", "gotta");
	private static final List<String> MAY_AUX = List.of("can", "may", "could", "might", "would", "do", "does",
			"did", "please");
	/** Closed-class grammar: words that can never fill the verb slot. */
	private static final List<String> SUBORDINATORS = List.of("if", "when", "whenever", "while", "whilst",
			"because", "though", "although", "unless", "since", "whereas", "that");
	private static final List<String> INDEFINITE = List.of("someone", "somebody", "something", "anyone",
			"anybody", "anything", "everyone", "everybody", "everything", "nobody", "nothing");
	private static final Pattern TOKEN = Pattern.compile("[a-z]+(?:'[a-z]+)?|\\d+");
	private static final Map<String, Map<String, Double>> CUES = loadCues();
	/**
	 * How a claim shape is decided. Each matched cue contributes its compiled lift, because the lift
	 * is the evidence the corpus compiled: a word this shape says twenty-seven times more often than
	 * the corpus overall is not the same claim marker as one it says nearly as often as everyone
	 * else. Counting matched words and discarding the lift made two ordinary sentences score within
	 * one word of each other and the frame say nothing, on text that does have a shape.
	 *
	 * <p>A shape is still not guessed: it needs at least two cue words, whose combined lift clears
	 * {@link #MIN_SHAPE_EVIDENCE}, and it must beat the runner-up by {@link #SHAPE_MARGIN_RATIO}.
	 */
	private static final double MIN_SHAPE_EVIDENCE = 12.0;
	private static final double SHAPE_MARGIN_RATIO = 1.5;

	private FrameParser() {
	}

	/**
	 * Parse one utterance. {@code index} disambiguates frames produced in the same tick.
	 */
	public static VerbFrame parse(long tick, String source, String actor, String utterance, int index) {
		String text = utterance == null ? "" : utterance;
		List<String> tokens = tokens(text);
		List<String> residue = new ArrayList<>(tokens);
		String frameId = "frame:" + tick + ":" + index;
		if (tokens.isEmpty()) {
			return new VerbFrame(frameId, tick, source, actor, "", VerbFrame.State.UNKNOWN, "",
					null, "", "", "", residue, text, 0, text.length());
		}

		String subject = PRONOUNS.contains(tokens.get(0)) ? tokens.get(0) : "";
		if (!subject.isEmpty()) {
			residue.remove(0);
		}

		// Modality is a property of the sentence, read before the verb is chosen: "will not ask"
		// and "may not begin" differ only there.
		String modality = "";
		for (String token : tokens) {
			if (MUST_AUX.contains(token)) {
				modality = "must";
				break;
			}
			if (MAY_AUX.contains(token)) {
				modality = "may";
				break;
			}
		}
		if (tokens.contains("not")) {
			modality = modality.isEmpty() ? "may" : modality;
		}

		// English puts the verb first among the words that are not grammar. The position decides,
		// not the vocabulary: searching for any attested word made "I smelt iron ore" take "iron" as
		// its verb, because "smelt" is unattested. The verb slot is filled by position and then
		// judged by attestation, which is the only order that can report an unknown word as unknown.
		int verbAt = -1;
		String verb = "";
		for (int i = 0; i < tokens.size(); i++) {
			String token = tokens.get(i);
			if (DETERMINERS.contains(token) || PREPOSITIONS.contains(token) || PRONOUNS.contains(token)
					|| token.chars().allMatch(Character::isDigit) || MUST_AUX.contains(token)
					|| MAY_AUX.contains(token) || "not".equals(token) || "yes".equals(token)
					|| "no".equals(token) || SUBORDINATORS.contains(token)
					|| INDEFINITE.contains(token)) {
				continue;
			}
			verb = token;
			verbAt = i;
			break;
		}

		VerbFrame.State state;
		if (verb.isEmpty()) {
			state = VerbFrame.State.UNKNOWN;
		} else if (ProseLogic.baseline().pair(verb.toUpperCase(Locale.ROOT)).isPresent()) {
			state = VerbFrame.State.ATTESTED;
		} else if (Lexicon.corpus().attests(verb)) {
			state = VerbFrame.State.NAMED;
		} else {
			state = VerbFrame.State.UNKNOWN;
		}

		// The object runs from after the verb to the end of the clause. A conjunction or comma ends
		// it: the rest is a second claim, and it belongs in the residue rather than being folded
		// into this one.
		Integer count = null;
		List<String> objectWords = new ArrayList<>();
		if (verbAt >= 0) {
			int stop = tokens.size();
			for (int i = verbAt + 1; i < tokens.size(); i++) {
				if ("and".equals(tokens.get(i)) || ",".equals(tokens.get(i))) {
					stop = i;
					break;
				}
			}
			for (int i = verbAt + 1; i < stop; i++) {
				String token = tokens.get(i);
				if (token.chars().allMatch(Character::isDigit) && count == null) {
					try {
						count = Integer.parseInt(token);
					} catch (NumberFormatException ignored) {
						// A number too large to hold is not a quantity anyone can carry.
					}
					continue;
				}
				if (DETERMINERS.contains(token) || PREPOSITIONS.contains(token)) {
					continue;
				}
				objectWords.add(token);
			}
			residue.remove(String.valueOf(verb));
			if (count != null) {
				residue.remove(String.valueOf(count));
			}
			for (String word : objectWords) {
				residue.remove(word);
			}
		}

		String objectKind = String.join(" ", objectWords);
		String recipient = recipient(tokens, subject);
		residue.remove(recipient);
		String shape = claimShape(tokens).orElse("");

		return new VerbFrame(frameId, tick, source, actor, verb, state, objectKind, count, recipient,
				modality, shape, residue, text, 0, text.length());
	}

	/** Who the frame is addressed to: an explicit pronoun or the word after "to"/"for". */
	private static String recipient(List<String> tokens, String subject) {
		for (int i = 0; i < tokens.size() - 1; i++) {
			if (("to".equals(tokens.get(i)) || "for".equals(tokens.get(i)))
					&& PRONOUNS.contains(tokens.get(i + 1))) {
				return tokens.get(i + 1);
			}
		}
		if (tokens.contains("me")) {
			return "me";
		}
		if ("you".equals(subject)) {
			return "you";
		}
		// "do you have ..." / "can you bring ..." address the person in the second position: the
		// auxiliary opens the question and the pronoun is who it is put to.
		if (tokens.size() > 1 && (MAY_AUX.contains(tokens.get(0)) || MUST_AUX.contains(tokens.get(0)))
				&& PRONOUNS.contains(tokens.get(1))) {
			return tokens.get(1);
		}
		return "";
	}

	/**
	 * The claim shape this sentence is stated in, scored against the cues the corpus compiles for
	 * each shape. Empty when no shape stands out: an unclaimed sentence is not forced into a claim
	 * just because a claim exists.
	 */
	public static Optional<String> claimShape(List<String> tokens) {
		if (CUES.isEmpty()) {
			return Optional.empty();
		}
		Map<String, Double> evidence = new LinkedHashMap<>();
		Map<String, Integer> matched = new LinkedHashMap<>();
		for (String token : tokens) {
			for (Map.Entry<String, Map<String, Double>> entry : CUES.entrySet()) {
				Double lift = entry.getValue().get(token);
				if (lift != null) {
					evidence.merge(entry.getKey(), lift, Double::sum);
					matched.merge(entry.getKey(), 1, Integer::sum);
				}
			}
		}
		if (evidence.isEmpty()) {
			return Optional.empty();
		}
		List<Map.Entry<String, Double>> ranked = new ArrayList<>(evidence.entrySet());
		ranked.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
		String best = ranked.get(0).getKey();
		// Two cue words, not one: a sentence that merely mentions "when" has not been stated as a
		// conditional, and this is the check that keeps the shape off such a sentence.
		if (matched.get(best) < 2 || evidence.get(best) < MIN_SHAPE_EVIDENCE) {
			return Optional.empty();
		}
		double second = ranked.size() > 1 ? ranked.get(1).getValue() : 0.0;
		if (second > 0 && evidence.get(best) < second * SHAPE_MARGIN_RATIO) {
			return Optional.empty();
		}
		return Optional.of(best);
	}

	private static List<String> tokens(String utterance) {
		List<String> tokens = new ArrayList<>();
		Matcher matcher = TOKEN.matcher(utterance == null ? "" : utterance.toLowerCase(Locale.ROOT));
		while (matcher.find()) {
			tokens.add(matcher.group());
		}
		return tokens;
	}

	/**
	 * Why this frame has the shape it has: the cue words that carried it, or {@code none}. A reader can
	 * check the basis against {@code prose_cues.tsv} without re-running the parse.
	 */
	public static String shapeBasis(String utterance, String shape) {
		if (shape == null || shape.isEmpty()) {
			return "none";
		}
		Map<String, Double> cues = CUES.getOrDefault(shape, Map.of());
		List<String> matched = new ArrayList<>();
		for (String token : tokens(utterance)) {
			if (cues.containsKey(token) && !matched.contains(token)) {
				matched.add(token);
			}
		}
		return matched.isEmpty() ? "none" : "cues:" + String.join(",", matched);
	}

	/** Why this frame has the verb it has: a claim shape, a lexicon word with its count, or nothing. */
	public static String verbBasis(String verb) {
		if (verb == null || verb.isEmpty()) {
			return "none";
		}
		if (ProseLogic.baseline().pair(verb.toUpperCase(Locale.ROOT)).isPresent()) {
			return "baseline";
		}
		int count = Lexicon.corpus().count(verb);
		return count > 0 ? "lexicon:" + count : "unattested";
	}

	/**
	 * The compiled cue table: shape to word to the lift the corpus measured for that pair. The lift is
	 * the third column of {@code prose_cues.tsv}; a word's lift is what the parser scores with.
	 */
	private static Map<String, Map<String, Double>> loadCues() {
		Map<String, Map<String, Double>> cues = new LinkedHashMap<>();
		try (java.io.InputStream in = FrameParser.class.getResourceAsStream("/ensouled/prose_cues.tsv")) {
			if (in == null) {
				return cues;
			}
			java.io.BufferedReader reader = new java.io.BufferedReader(
					new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
			String row;
			while ((row = reader.readLine()) != null) {
				String line = row.strip();
				if (line.isEmpty() || line.startsWith("#")) {
					continue;
				}
				String[] parts = line.split("\t");
				if (parts.length < 2) {
					continue;
				}
				double lift = 1.0;
				if (parts.length > 2) {
					try {
						lift = Double.parseDouble(parts[2]);
					} catch (NumberFormatException unmeasured) {
						// A row without a readable lift still marks the shape; it just carries the
						// weakest evidence a cue can carry.
						lift = 1.0;
					}
				}
				cues.computeIfAbsent(parts[0], key -> new LinkedHashMap<>()).put(parts[1], lift);
			}
		} catch (java.io.IOException unreadable) {
			return cues;
		}
		return cues;
	}
}

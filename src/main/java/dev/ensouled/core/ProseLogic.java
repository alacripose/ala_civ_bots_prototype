package dev.ensouled.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The prose-logic baseline this world classifies speech with. It is not this project's opinion: it
 * is loaded from the generated claim corpus of {@code agent_god_files/prose_logic}, where each
 * claim's rule is {@code <ref>: VERB [args]} and every verb belongs to exactly one atom kind and one
 * modality. That table is the whole of what a sentence may be composed of here.
 *
 * <p>A line is admissible only if it parses into that form and its verb is in the baseline with the
 * kind and modality the corpus gives it. Nothing is composed by this class; composition belongs to
 * the being. This is the baseline that decides whether what it composed can exist as a claim, and a
 * line that does not classify is refused by name rather than spoken.
 */
public final class ProseLogic {
	/** One baseline verb and the kind and modality the corpus assigns it. */
	public record Pair(String verb, String kind, String modality, int occurrences) {
	}

	private static final Pattern RULE = Pattern.compile("^\\s*(\\S[^:]*):\\s*([A-Z_]+)\\s*\\[(.*)\\]\\s*$");
	private static final ProseLogic BASELINE = load();

	private final Map<String, Pair> pairs;
	private final String source;

	private ProseLogic(Map<String, Pair> pairs, String source) {
		this.pairs = pairs;
		this.source = source;
	}

	public static ProseLogic baseline() {
		return BASELINE;
	}

	public String source() {
		return source;
	}

	public List<Pair> verbs() {
		return List.copyOf(pairs.values());
	}

	public Optional<Pair> pair(String verb) {
		return Optional.ofNullable(pairs.get(verb));
	}

	/**
	 * Classify one composed line. Present only when the line has the corpus rule form, its verb is a
	 * baseline verb, and the kind and modality come from the baseline rather than from the line.
	 */
	public Optional<Claim> classify(String line) {
		if (line == null) {
			return Optional.empty();
		}
		Matcher matcher = RULE.matcher(line);
		if (!matcher.matches()) {
			return Optional.empty();
		}
		Pair pair = pairs.get(matcher.group(2));
		if (pair == null) {
			return Optional.empty();
		}
		return Optional.of(new Claim(matcher.group(1), pair.verb(), pair.kind(), pair.modality(),
				matcher.group(3).trim(), Claim.Support.DIRECT));
	}

	private static ProseLogic load() {
		Map<String, Pair> parsed = new LinkedHashMap<>();
		String source = "resources/ensouled/prose_baseline.tsv";
		try (InputStream in = ProseLogic.class.getResourceAsStream("/ensouled/prose_baseline.tsv")) {
			if (in == null) {
				return new ProseLogic(parsed, source + " (missing)");
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			String row;
			while ((row = reader.readLine()) != null) {
				String line = row.strip();
				if (line.isEmpty() || line.startsWith("#")) {
					continue;
				}
				String[] parts = line.split("\t");
				if (parts.length < 4) {
					continue;
				}
				parsed.put(parts[0], new Pair(parts[0], parts[1], parts[2], Integer.parseInt(parts[3])));
			}
		} catch (IOException | NumberFormatException e) {
			return new ProseLogic(parsed, source + " (unreadable)");
		}
		return new ProseLogic(parsed, source);
	}
}

package dev.ensouled.core;

/**
 * The only place a being's lane composes a sentence.
 *
 * <p>Every verb here is one the prose baseline already holds ({@code RECORD}, {@code EXECUTE},
 * {@code FORBID}, ...), so a line written through this class classifies by construction, and a
 * caller cannot put a machine token in the utterance slot: {@code harvest_tree@12,64,-8} is not a
 * verb, so {@link ProseLogic#classify} refuses it and the line is never streamed. That is the whole
 * repair — the hand-rolled row shapes were the flood, and this is the one gate they now pass.
 */
public final class ProseLines {
	/** A fact the being holds, spoken in the corpus's rule form. */
	public static String record(String speaker, String sentence) {
		return compose(speaker, "RECORD", sentence);
	}

	/** A procedure the being is about to carry out. */
	public static String execute(String speaker, String sentence) {
		return compose(speaker, "EXECUTE", sentence);
	}

	/** A prohibition the being has run into. */
	public static String forbid(String speaker, String sentence) {
		return compose(speaker, "FORBID", sentence);
	}

	/** A requirement the being is stating or asking about. */
	public static String require(String speaker, String sentence) {
		return compose(speaker, "REQUIRE", sentence);
	}

	/** A condition the being can read: the situation, before the act. */
	public static String when(String speaker, String sentence) {
		return compose(speaker, "WHEN", sentence);
	}

	/**
	 * One line in the corpus's own rule form, {@code ref: VERB [sentence]}, refused by name when
	 * the baseline cannot read it as a claim.
	 */
	public static String compose(String speaker, String verb, String sentence) {
		if (speaker == null || speaker.isBlank()) {
			throw new IllegalArgumentException("prose-no-speaker: a line names who said it");
		}
		if (sentence == null || sentence.isBlank()) {
			throw new IllegalArgumentException("prose-empty: a line carries a sentence");
		}
		if (sentence.indexOf('[') >= 0 || sentence.indexOf(']') >= 0) {
			throw new IllegalArgumentException("prose-brackets: \"" + sentence
					+ "\" would end the clause early");
		}
		String line = label(speaker) + ": " + verb + " [" + sentence + "]";
		if (!MessageEnvelope.isProse(line)) {
			throw new IllegalArgumentException("prose-unreadable: \"" + line
					+ "\" is not a claim this world's baseline can read");
		}
		return line;
	}

	/**
	 * The spoken label for a sender. It is a name snapshot, never the identity key, and it cannot
	 * carry the colon that separates a reference from its verb — a synthetic id like
	 * {@code agent:ash} would otherwise make every line it speaks unreadable.
	 */
	private static String label(String speaker) {
		String cleaned = speaker.replaceAll("[^A-Za-z0-9 _.-]", " ").replaceAll("\\s+", " ").trim();
		return cleaned.isEmpty() ? "an unnamed being" : cleaned;
	}

	private ProseLines() {
	}
}

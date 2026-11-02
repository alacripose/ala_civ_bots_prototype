package dev.ensouled.core;

/**
 * One claim a being can state, in the prose-logic rule form the corpus uses: {@code <ref>: VERB
 * [args]}. The verb, the atom kind and the modality all come from {@link ProseLogic}'s baseline, so
 * a claim cannot invent a kind of sentence that the corpus does not already have.
 *
 * <p>{@link Support#DIRECT} is the only support a mechanical derivation can reach: the quote is what
 * the body observed, in the words the corpus attests. A claim with no quote from observation is not
 * spoken.
 */
public record Claim(String ref, String verb, String kind, String modality, String quote,
		Support support) {
	/** Computed, never asserted: direct means the quote is the observation itself. */
	public enum Support {
		DIRECT
	}

	/**
	 * The spoken form: the observed situation in English. The quote is what the being composed out of
	 * the corpus vocabulary, so this is what the person in the world reads.
	 */
	public String spoken() {
		return quote;
	}

	/** The corpus rule shape, kept for the evidence trail rather than for chat. */
	public String rule() {
		return ref + ": " + verb + " [" + quote + "]";
	}

	/** The full record, for the evidence trail rather than for chat. */
	public String describe() {
		return rule() + " kind=" + kind + " modality=" + modality + " support="
				+ support.name().toLowerCase(java.util.Locale.ROOT);
	}
}

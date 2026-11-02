package dev.ensouled.work;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * The two things a wish can be before it is work: a request for the being's own words, or a question
 * about what it is already doing. Everything else is read as a wish to take something on, which is
 * {@link WorkLedger#offer}'s business and not this record's.
 *
 * <p>These are English tables, and they belong beside the work a wish becomes rather than inside the
 * mind. What the being then does with the reading stays its own decision.
 */
public record WishReading(Shape shape) {

	public enum Shape {
		/** Phrased as a request for the being's words: refused, not negotiated. */
		DICTATION,
		/** Asking what the being is doing or what it owes. */
		QUESTION_ABOUT_WORK,
		/** Anything else: offered to the ledger as a wish naming a thing. */
		OTHER
	}

	private static final Pattern DICTATION =
			Pattern.compile("\\b(say|tell|reply|answer|announce|shout|write|repeat)\\b");
	private static final String[] OPENERS = { "what", "where", "how", "why", "who", "when", "which" };

	public static WishReading of(String text) {
		String lower = text == null ? "" : text.toLowerCase(Locale.ROOT);
		if (DICTATION.matcher(lower).find()) {
			return new WishReading(Shape.DICTATION);
		}
		if (lower.indexOf('?') >= 0 || startsWithOpener(lower)) {
			return new WishReading(Shape.QUESTION_ABOUT_WORK);
		}
		return new WishReading(Shape.OTHER);
	}

	private static boolean startsWithOpener(String lower) {
		for (String opener : OPENERS) {
			if (lower.startsWith(opener)) {
				return true;
			}
		}
		return false;
	}
}

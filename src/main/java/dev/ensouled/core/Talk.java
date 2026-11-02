package dev.ensouled.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * What this being could say. It composes sentences out of the prose-logic corpus's own vocabulary
 * ({@link Lexicon}), states them as corpus claims ({@code <ref>: VERB [sentence]}) so
 * {@link ProseLogic} can admit or refuse them, and lets the being pick which one it means. Keeping
 * quiet is one of the picks.
 *
 * <p>Nothing here is a canned line: each candidate is built from this body's own numbers and the
 * world's own names for what it holds, arranged by the being, with every word drawn from the
 * corpus. A sentence whose meanings the corpus cannot name is unsayable and is reported as such.
 */
public final class Talk {
	public static final String KEEP_QUIET = "keep_quiet";

	/** What the being perceived about itself and the person it is answering. */
	public record Observation(String speakerName, String itemId, int held, int needed, int food,
			boolean hungry) {
	}

	/** One composed sentence, its weight, and the corpus verb it is stated as. */
	public record Line(String verb, int utilityMilli, String sentence, List<String> unsayable) {
		public boolean sayable() {
			return unsayable.isEmpty() && !sentence.isEmpty();
		}
	}

	private Talk() {
	}

	public static List<Line> candidates(String ref, Hearing.Heard heard, Affect affect,
			Observation obs, boolean resting, boolean requestJustRefused) {
		List<Line> out = new ArrayList<>();
		if (!heard.addressedToMe() && !requestJustRefused) {
			out.add(new Line(KEEP_QUIET, 0, "", List.of()));
			return out;
		}
		String item = Realizer.worldName(obs.itemId());
		String itemWords = Realizer.many(item, obs.held());
		int urgency = affect.radius() / 100;
		int record = 420 + urgency;
		int when = obs.needed() > 0 ? 380 + urgency : 0;
		int forbid = requestJustRefused ? 900 : 0;
		int require = obs.hungry() ? 520 + urgency : 0;
		// Rest values silence, but not over a person who used this being's name: being addressed ends
		// a rest, exactly as an incoming message does, so the being answers before it settles again.
		int quiet = resting && !heard.addressedToMe() ? 700 : 140;
		if (affect.family() == Affect.Family.SAD) {
			quiet += 200;
		}
		if (affect.family() == Affect.Family.SCARED) {
			// An alarmed being states the condition it needs met before it makes conversation.
			when += 200;
		}

		if (require > 0) {
			// Obligation: what the body needs, said plainly.
			out.add(line("REQUIRE", require, new Realizer()
					.word("i")
					.word("need", "want", "lack")
					.word("food", "bread")));
		}
		if (forbid > 0) {
			// Prohibition: it will not ask this person again for a while.
			out.add(line("FORBID", forbid, new Realizer()
					.word("i")
					.word("will", "shall")
					.word("not")
					.word("ask")
					.word("you")
					.word("for")
					.value(item)
					.word("again")));
		}
		// Fact: what it actually holds, verified by its own inventory.
		out.add(line("RECORD", record, new Realizer()
				.word("i")
				.word("have", "hold", "keep")
				.value(obs.held())
				.value(itemWords)
				.word("and")
				.value(obs.food())
				.word("food")));
		if (when > 0) {
			// Condition: what would settle the requirement.
			out.add(line("WHEN", when, new Realizer()
					.word("if")
					.word("you")
					.word("bring", "give")
					.word("me")
					.value(obs.needed())
					.word("more")
					.value(Realizer.many(item, obs.needed()))
					.word("i")
					.word("will", "shall")
					.word("have", "hold")
					.word("enough")));
		}
		out.add(new Line(KEEP_QUIET, quiet, "", List.of()));
		return out;
	}

	private static Line line(String verb, int utility, Realizer realizer) {
		return new Line(verb, utility, realizer.sentence(), realizer.unsayable());
	}

	/**
	 * What a being says when it takes work on. The number and the thing are its own reading of what it
	 * owes; the words are the corpus's. Empty when the corpus cannot say it, which leaves the being
	 * quiet rather than inventing a phrase.
	 */
	public static String owing(int count, String words) {
		Realizer realizer = new Realizer()
				.word("i")
				.word("will", "shall")
				.word("bring", "get", "have")
				.value(count)
				.value(words);
		return realizer.sayable() ? realizer.sentence() : "";
	}

	/** The question a being asks when a request named a thing but not how many of it. */
	public static String askHowMany(String words) {
		Realizer realizer = new Realizer().word("how").word("many");
		if (words != null && !words.isBlank()) {
			realizer.value(words);
		}
		return realizer.sayable() ? realizer.question() : "";
	}

	/** The question a being asks when it cannot tell what it is being asked for at all. */
	public static String askWhat() {
		Realizer realizer = new Realizer()
				.word("what")
				.word("do", "would")
				.word("you")
				.word("want", "ask")
				.word("me")
				.word("to")
				.word("bring", "get");
		return realizer.sayable() ? realizer.question() : "";
	}

	/** Highest weight first; ties by verb, so the same situation composes the same order. */
	public static List<Line> ordered(List<Line> candidates) {
		return candidates.stream()
				.sorted(Comparator.comparingInt(Line::utilityMilli).reversed().thenComparing(Line::verb))
				.toList();
	}
}

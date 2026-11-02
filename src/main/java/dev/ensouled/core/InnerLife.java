package dev.ensouled.core;

import java.util.List;
import java.util.function.Consumer;

/**
 * What a being does when it is not deciding: it thinks out loud, and at rest it works its memories
 * over as dreams.
 *
 * <p>This is the mind's off-path, and it names no intention, moves no body and changes nothing in the
 * world — which is exactly why it was worth taking out of {@link AgentMind}: the decision chain stays
 * readable, and everything that only makes noise lives here. All three intervals are this class's
 * own; the one thing it borrows is the mind's speech pacing, so a murmur cannot tread on a sentence
 * the being has just spoken to somebody.
 */
public final class InnerLife {
	/** How often a being mutters to itself. */
	private static final int MURMUR_INTERVAL_TICKS = 700;
	/** How often a resting being draws two memories together. */
	private static final int DREAM_INTERVAL_TICKS = 70;

	private final AgentIdentity identity;
	private final MindLedger ledger;
	private final Consumer<String> speech;
	private long lastMurmurTick = Long.MIN_VALUE / 2;
	private long lastDreamTick = Long.MIN_VALUE / 2;
	private String lastMurmurText = "";

	/** @param speech what the being does with a finished line: it says it */
	public InnerLife(AgentIdentity identity, MindLedger ledger, Consumer<String> speech) {
		this.identity = identity;
		this.ledger = ledger;
		this.speech = speech;
	}

	/**
	 * One tick of the off-path. A resting being dreams; a being with nothing to say to anybody, and
	 * nothing said recently, murmurs.
	 */
	public void tick(WorldView view, long now, String intentionId, boolean resting, long lastSpokenTick) {
		if (resting) {
			dream(view, now, intentionId);
			return;
		}
		if (now - lastMurmurTick >= MURMUR_INTERVAL_TICKS
				&& now - lastSpokenTick >= Voice.MIN_SPEECH_INTERVAL_TICKS * 2) {
			murmur(view, now);
		}
	}

	/** Thinking out loud: the same corpus vocabulary, aimed at nobody in particular. */
	private void murmur(WorldView view, long now) {
		lastMurmurTick = now;
		Realizer sentence = new Realizer();
		int food = view.needs().food();
		// Wood is a kind, not an item id: a body holding planks or a birch log is not short of wood,
		// so the shortage arm below is only reachable when nothing wood is carried at all.
		int wood = Supplies.wood(view);
		String woodItem = Supplies.woodItem(view);
		String woodWords = Realizer.worldName(woodItem) + (wood == 1 ? "" : "s");
		int choice = Math.floorMod(identity.agentId().hashCode() + (int) now, 100);
		if (view.needs().hungry() || choice < 30) {
			sentence.word("i").word("need", "want", "lack").word("food", "bread");
		} else if (choice < 55) {
			// Onomatopoeia is vocabulary too, and it is attested like everything else.
			sentence.word("hm", "hmm", "ah", "oh", "well");
		} else if (wood > 0) {
			// What it is actually carrying comes before what it might want: a being that reports a
			// shortage while its hands are full is reading the wrong question off its own inventory.
			sentence.word("i").word("have", "hold", "keep").value(wood).value(woodWords)
					.word("and").value(food).word("food");
		} else if (choice < 80) {
			sentence.word("i").word("have", "hold", "keep").value(0).value("wood")
					.word("and").value(food).word("food");
		} else {
			sentence.word("i").word("need", "want", "lack").word("wood");
		}
		if (!sentence.sayable()) {
			return;
		}
		String line = sentence.sentence();
		if (line.equals(lastMurmurText)) {
			// The same true sentence twice in a row is a recording, not thinking out loud.
			return;
		}
		lastMurmurText = line;
		speech.accept(line);
	}

	/**
	 * A rest is where memory is worked over. Two things the being remembers are drawn wide and put
	 * together, and the result is kept as a dream. It is reprocessing, not a new perception.
	 */
	private void dream(WorldView view, long now, String intentionId) {
		if (now - lastDreamTick < DREAM_INTERVAL_TICKS) {
			return;
		}
		lastDreamTick = now;
		List<Episode> remembered = ledger.memory().all();
		if (remembered.size() < 2) {
			return;
		}
		int first = Math.floorMod(identity.agentId().hashCode() * 31 + (int) now, remembered.size());
		int second = Math.floorMod(first * 7 + 3, remembered.size());
		if (second == first) {
			second = (first + 1) % remembered.size();
		}
		String and = Lexicon.corpus().prefer("and", "with").orElse("and");
		Episode a = remembered.get(first);
		Episode b = remembered.get(second);
		String dream = a.summary() + " " + and + " " + b.summary();
		ledger.remember(now, EpisodeKind.DREAM, dream, view.bodyPos(), "dream_reprocessed",
				a.episodeId() + "+" + b.episodeId());
		ledger.note(now, identity.agentId(), intentionId, EvidenceKind.LEARN, "", "dreamt", dream);
	}
}

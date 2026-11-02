package dev.ensouled.work;

import dev.ensouled.core.AgentIdentity;
import dev.ensouled.core.EvidenceKind;
import dev.ensouled.core.MindLedger;
import dev.ensouled.core.Voice;
import dev.ensouled.core.StreamNotices;
import dev.ensouled.core.Talk;
import dev.ensouled.core.Wishes;
import dev.ensouled.core.WorldView;

/**
 * A wish put to one being, and what it makes of it.
 *
 * <p>A wish is not dictation. When it asks for a sentence, the being refuses on the record: words are
 * its own, and a line chosen by an operator and emitted as this being would be the operator talking.
 * A wish that names something is taken as a consideration — noted in its own trail, with its own
 * answer on its stream — and what it then does about it stays the being's decision. A wish that is
 * ambiguous is clarified, never guessed (addendum E.4).
 */
public final class WishDesk {

	/** A being answers at most one wish at a time, so a queue of them cannot become its voice. */
	private static final int WISH_INTERVAL_TICKS = 100;

	private final AgentIdentity identity;
	private final MindLedger ledger;
	private final Voice voice;
	private long lastWishTick;

	public WishDesk(AgentIdentity identity, MindLedger ledger, Voice voice) {
		this.identity = identity;
		this.ledger = ledger;
		this.voice = voice;
	}

	public void tick(WorldView view, long tick, String intentionId) {
		if (tick - lastWishTick < WISH_INTERVAL_TICKS) {
			return;
		}
		Wishes.Wish wish = Wishes.take(identity.agentId());
		if (wish == null) {
			return;
		}
		lastWishTick = tick;
		WishReading reading = WishReading.of(wish.text());
		if (reading.shape() == WishReading.Shape.DICTATION) {
			ledger.note(tick, identity.agentId(), intentionId, EvidenceKind.SOCIAL, "", "wish_refused",
					wish.id() + ": " + wish.text() + " — it asked for my words");
			StreamNotices.refuse(identity.agentId(), "wish " + wish.id(), "wish-dictation",
					"words are mine to choose; a wish may ask for a thing, not for a sentence");
			return;
		}
		WorkLedger work = WorkLedger.get();
		if (work == null) {
			// Without a world there is nothing a being can owe, so the wish stays what it was: a
			// thing it has in mind and nothing more.
			StreamNotices.noted(identity.agentId(), "wish " + wish.id(), "wish-noted",
					"I have it in mind; what I do about it is mine");
			return;
		}
		// A question about what it is doing is answered from what it owes. When there is nothing to
		// answer from, the wish is read as a request again rather than dropped.
		if (reading.shape() == WishReading.Shape.QUESTION_ABOUT_WORK && answerFromWork(work, wish, tick,
				intentionId)) {
			return;
		}
		WorkLedger.Answer answer = work.offer(identity.agentId(), identity.displayName(), wish.id(),
				wish.text(), tick, view);
		ledger.note(tick, identity.agentId(), intentionId, EvidenceKind.SOCIAL, "",
				answer.opened() ? "wish_taken_up" : "wish_" + answer.code(),
				wish.id() + ": " + wish.text() + " — " + answer.why());
		if (answer.opened()) {
			// What it took on is said in its own words, so the person who asked hears the work
			// exist rather than inferring it from a silent ledger row.
			String promised = Talk.owing(answer.count(), answer.words());
			if (!promised.isEmpty()) {
				voice.say(promised, tick);
			}
			return;
		}
		// Ambiguous is not understood: the being asks about the half it is missing, or says it
		// cannot tell (addendum E.4).
		String ask = answer.words().isBlank() ? Talk.askWhat() : Talk.askHowMany(answer.words());
		if (!ask.isEmpty()) {
			voice.say(ask, tick);
			StreamNotices.noted(identity.agentId(), "wish " + wish.id(), "wish-asked", ask);
			return;
		}
		StreamNotices.refuse(identity.agentId(), "wish " + wish.id(), "wish-unsupported",
				"I cannot tell what you are asking me to bring, and I will not invent it");
	}

	/**
	 * "What is the plan?" already has an answer and it is in the work record, so it is spoken from
	 * there. False when there is no open commitment to answer from, which sends the wish down the
	 * ordinary path instead of leaving it unanswered.
	 */
	private boolean answerFromWork(WorkLedger work, Wishes.Wish wish, long tick, String intentionId) {
		java.util.Optional<Commitment> owed = work.openCommitment(identity.agentId());
		if (owed.isEmpty()) {
			return false;
		}
		String answer = Talk.owing(owed.get().deliverable().count(), owed.get().deliverable().words());
		if (answer.isEmpty()) {
			return false;
		}
		ledger.note(tick, identity.agentId(), intentionId, EvidenceKind.SOCIAL, "", "wish_answered",
				wish.id() + ": " + wish.text() + " — answered from " + owed.get().commitmentId());
		voice.say(answer, tick);
		return true;
	}
}

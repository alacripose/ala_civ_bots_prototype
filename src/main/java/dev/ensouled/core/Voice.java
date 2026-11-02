package dev.ensouled.core;

import java.util.List;
import java.util.Optional;

/**
 * What this being says out loud, and the one rule for choosing it: the line is a claim the corpus
 * states, selected from the claims this world has ingested, so nothing here is a sentence invented
 * for a situation and nothing here is a menu of speech acts.
 *
 * <p>The heard line is the query. {@link ClaimCorpus} answers with the claims that share its rare
 * words, and the best one is spoken if {@link ProseLogic} still admits it. Silence is a result too:
 * a line the corpus has no claim about is recorded as such. {@link Speech} is the channel that
 * carries a chosen line; this is the being choosing it.
 */
public final class Voice {

	/** Shortest gap between two spoken lines, so an answer is an answer and not a loop. */
	public static final int MIN_SPEECH_INTERVAL_TICKS = 40;
	/** How many ingested claims are read as candidates for one answer. */
	public static final int MAX_REPLY_CANDIDATES = 8;

	/** This tick, as far as choosing a sentence is concerned. */
	public record Occasion(long tick, Affect affect, boolean resting, String intentionId) {
	}

	private final AgentIdentity identity;
	private final MindLedger ledger;
	private final SocialSeam social;
	private long lastSpokenTick = Long.MIN_VALUE / 2;

	Voice(AgentIdentity identity, MindLedger ledger, SocialSeam social) {
		this.identity = identity;
		this.ledger = ledger;
		this.social = social;
	}

	public long lastSpokenTick() {
		return lastSpokenTick;
	}

	/** Say a line the being has already chosen; the row records whether a client carried it. */
	public boolean say(String text, long tick) {
		boolean spoken = social.speech().say(identity.agentId(), text);
		ledger.note(tick, identity.agentId(), "", EvidenceKind.SOCIAL, "",
				spoken ? "said" : "speech_unavailable", text);
		if (spoken) {
			lastSpokenTick = tick;
		}
		return spoken;
	}

	/**
	 * Decide what, if anything, to say back. The being is not answering a keyword: it collects the
	 * lines it could say from its own state and picks one, and keeping quiet is one of the picks.
	 */
	public void answer(Hearing.Heard heard, boolean requestJustRefused, WorldView view, Occasion occasion) {
		if (occasion.tick() - lastSpokenTick < MIN_SPEECH_INTERVAL_TICKS) {
			// One answer per breath: a person is not a stream of prompts, and a being that repeats
			// itself every tick is not talking, it is looping.
			return;
		}
		// The whole ingested corpus is the candidate set. The heard line is the query: a claim earns
		// its place by the rare words it shares with what was said, so there is no reply category to
		// choose and nothing to fall back to when the being has no claim about the matter.
		List<ClaimCorpus.Hit> hits = ClaimCorpus.corpus().relevant(heard.text(), MAX_REPLY_CANDIDATES);
		if (hits.isEmpty()) {
			ledger.note(occasion.tick(), identity.agentId(), "", EvidenceKind.SOCIAL, "", "kept_quiet",
					"no ingested claim shares a word with \"" + heard.text() + "\"");
			return;
		}
		ClaimCorpus.Hit hit = hits.get(0);
		// The selected claim still passes the baseline: its verb, kind and modality are the
		// corpus's, and a row the baseline cannot read is never spoken.
		Optional<Claim> admitted = ProseLogic.baseline().classify(hit.claim().rule());
		if (admitted.isEmpty()) {
			ledger.note(occasion.tick(), identity.agentId(), "", EvidenceKind.SOCIAL, "", "unclassified_prose",
					"the baseline does not accept the ingested claim \"" + hit.claim().rule() + "\"");
			return;
		}
		ledger.note(occasion.tick(), identity.agentId(), occasion.intentionId(), EvidenceKind.DECISION,
				"", "chose_claim", admitted.get().describe() + " score=" + Math.round(hit.score() * 100)
						+ "/100 overlap=" + hit.shared() + " of " + hits.size() + " ingested claim(s) for "
						+ heard.speakerName() + " (affect " + occasion.affect().tertiary() + ")");
		say(admitted.get().spoken(), occasion.tick());
	}
}

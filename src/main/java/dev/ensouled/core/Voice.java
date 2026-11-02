package dev.ensouled.core;

import java.util.List;
import java.util.Optional;

/**
 * What this being says out loud, and the one rule for choosing it: a sentence exists only if the
 * corpus accepts it, so an unclassifiable line is kept quiet and recorded rather than spoken.
 *
 * <p>The mind decides; this class only turns a decision into speech and keeps the being from
 * answering twice in one breath. Silence is a choice here too — {@link Talk#KEEP_QUIET} outweighs
 * every composable claim. {@link Speech} is the channel that carries a chosen line; this is the
 * being choosing it.
 */
public final class Voice {

	/** Shortest gap between two spoken lines, so an answer is an answer and not a loop. */
	public static final int MIN_SPEECH_INTERVAL_TICKS = 40;

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
		// The claim is about wood this body is carrying, counted by kind: asking for a single item id
		// made a being holding planks announce that it held none.
		String heldItem = Supplies.woodItem(view);
		int held = Supplies.wood(view);
		int needed = Math.max(0, dev.ensouled.skills.RequestFromHumanRoutine.QUANTITY - held);
		// Every number spoken here is this body's own observation, so the spoken claim cannot exceed
		// what it actually holds or feels.
		Talk.Observation observation = new Talk.Observation(heard.speakerName(), heldItem, held,
				needed, view.needs().food(), view.needs().hungry());
		String ref = identity.displayName() + "@" + occasion.tick();
		List<Talk.Line> options = Talk.ordered(Talk.candidates(ref, heard, occasion.affect(),
				observation, occasion.resting(), requestJustRefused));
		// The prose-logic baseline is the classifier: a composed line exists only if the corpus's
		// verbs, kinds and modalities accept it. Nothing here is spoken on the being's word alone.
		for (Talk.Line option : options) {
			if (Talk.KEEP_QUIET.equals(option.verb())) {
				ledger.note(occasion.tick(), identity.agentId(), "", EvidenceKind.SOCIAL, "", "kept_quiet",
						"silence outweighed every composable claim (affect " + occasion.affect().tertiary()
								+ ")");
				return;
			}
			if (!option.sayable()) {
				ledger.note(occasion.tick(), identity.agentId(), "", EvidenceKind.SOCIAL, "", "unsayable",
						"the corpus has no word for " + option.unsayable());
				continue;
			}
			Optional<Claim> claim = ProseLogic.baseline().classify(
					ref + ": " + option.verb() + " [" + option.sentence() + "]");
			if (claim.isEmpty()) {
				ledger.note(occasion.tick(), identity.agentId(), "", EvidenceKind.SOCIAL, "",
						"unclassified_prose",
						"the baseline does not accept \"" + option.sentence() + "\" as a claim of this corpus");
				continue;
			}
			ledger.note(occasion.tick(), identity.agentId(), occasion.intentionId(), EvidenceKind.DECISION,
					"", "chose_claim", claim.get().describe() + " utility=" + option.utilityMilli() + " of "
							+ options.size() + " candidate(s) for " + heard.speakerName() + " (affect "
							+ occasion.affect().tertiary() + ")");
			say(claim.get().spoken(), occasion.tick());
			return;
		}
	}
}

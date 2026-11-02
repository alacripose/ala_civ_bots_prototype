package dev.ensouled.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.JsonObject;

import dev.ensouled.axis.AxisGate;
import dev.ensouled.core.ReflexLayer.ReflexAction;
import dev.ensouled.interact.InteractionHands;
import dev.ensouled.interact.InteractionOutcome;
import dev.ensouled.interact.InteractionRequest;
import dev.ensouled.interact.InteractionSession;
import dev.ensouled.interact.InteractionTarget;
import dev.ensouled.surface.SurfaceStream;
import dev.ensouled.work.WishDesk;

/**
 * One persistent being's decision cycle: sense → update → reflex → select → gate → act → verify.
 * Holds identity, beliefs, needs, the single dominant intention and the gate; everything it
 * remembers or can cite lives in its {@link MindLedger}, and its save format in
 * {@link MindPersistence}. No language model participates in any decision this class makes
 * (§1.1, §5).
 */
public final class AgentMind implements SkillContext.Listener {
	public static final int SENSE_INTERVAL_TICKS = 5;
	public static final int INTENTION_TTL_TICKS = 1200;
	/** Weariness is the being's own, 0..FATIGUE_MAX: accrued awake, relieved by choosing rest. */
	public static final int FATIGUE_MAX = 1000;
	/** Above this, sitting down is something the body wants rather than an absence of options. */
	public static final int FATIGUE_REST_WANTED = 550;
	/** What one deliberate rest takes off the body. */
	public static final int FATIGUE_REST_RELIEF = 700;
	/** How often a being publishes what it can see, so the world is on record as it changes. */
	public static final int OBSERVATION_INTERVAL_TICKS = 200;
	/** A decision this close is a contest, and a contest is worth remembering. */
	public static final int CONTEST_MARGIN_MILLI = 250;
	/** Shortest gap between two remembered contests, so deliberation does not flood memory. */
	public static final int CONTEST_QUIET_TICKS = 200;

	private final AgentIdentity identity;
	private final AuthorityGateway gate;
	private final Motor motor;
	private final SkillRegistry skills;
	/** This being's axis cells: every commitment read against the work behind it. */
	private final AxisGate.Cells axis = new AxisGate.Cells();
	/** The verdict the last selected candidate was admitted by; written ahead with the decision. */
	private AxisGate.Decision lastVerdict;
	/** The rung each candidate's selection stood on this cycle; selection sorts by it first. */
	private java.util.Map<String, Integer> lastReadingRanked = new java.util.LinkedHashMap<>();
	/** The rung the committed candidate stood on, so the record says what the choice rested on. */
	private String lastReading = "unchecked";
	private final SkillRuntime runtime;
	private final SocialSeam social;
	private final InteractionHands hands;
	private final MindLedger ledger = new MindLedger();
	/** Murmuring and dreaming: the off-path, and the only thing that needed its own owner. */
	private final InnerLife innerLife;
	/** What the being says out loud, and how it is chosen. */
	private final Voice voice;
	/** Wishes put to the being, and what it makes of them. */
	private final WishDesk wishes;
	/** The durable record of the decision in flight: written before it acts, closed after. */
	private final DecisionJournal journal;

	private final BeliefStore beliefs = new BeliefStore();
	private final Arbiter arbiter = new Arbiter();
	private final ReflexLayer reflexes = new ReflexLayer();
	private final CandidateGenerator generator = new CandidateGenerator();

	private NeedsState needs = NeedsState.healthy();
	private Intention current;
	private SkillRoutine activeRoutine;
	private ActionEnvelope currentEnvelope;
	private long tick;
	private long intentionSequence;
	private String lastDimensionId = "minecraft:overworld";
	private String lastReflexCode = "";
	private boolean reflexEngaged;
	private int decisionCount;
	private int lastDecisionChoices;
	/** Exertion, in the body's own terms. It is a need, so it can be unmet, and the being decides. */
	private int fatigue;
	private long lastThoughtTick = Long.MIN_VALUE / 2;
	private long lastObservationTick = Long.MIN_VALUE / 2;
	private String lastObservationDigest = "genesis";
	private long thoughtSequence;
	/** The being's affect on the feeling wheel: state it carries, not narration it performs. */
	private Affect affect = Affect.initial();
	/** Situational inputs to the next affect update, set by what happened since the last one. */
	private boolean lastActionSucceeded;
	private boolean lastActionFailed;
	private boolean requestRefusedHeard;
	private boolean spokenToHeard;

	public AgentMind(AgentIdentity identity, AuthorityGateway gate, Motor motor, SkillRegistry skills) {
		this(identity, gate, motor, skills, SocialSeam.isolated());
	}

	public AgentMind(AgentIdentity identity, AuthorityGateway gate, Motor motor, SkillRegistry skills,
			SocialSeam social) {
		this(identity, gate, motor, skills, social, InteractionHands.none());
	}

	public AgentMind(AgentIdentity identity, AuthorityGateway gate, Motor motor, SkillRegistry skills,
			SocialSeam social, InteractionHands hands) {
		this.identity = identity.validate();
		this.gate = gate;
		this.motor = motor;
		this.skills = skills;
		this.social = social;
		this.hands = hands;
		this.runtime = new SkillRuntime(motor);
		this.voice = new Voice(this.identity, ledger, social);
		this.wishes = new WishDesk(this.identity, ledger, voice);
		this.journal = new DecisionJournal(this.identity, ledger);
		this.innerLife = new InnerLife(this.identity, ledger, this::say);
	}

	// ---------------------------------------------------------------- decision cycle

	/**
	 * One fixed-tick slice of the cycle. Motor programs advance every tick; perception and new
	 * selections are bounded per slice.
	 */
	public void tick(WorldView view) {
		this.tick = view.tick();
		// Before anything is sensed or decided: a decision the process died inside is recovered
		// from the journal, named on this being's own stream, and closed — once.
		journal.recoverOnce(tick);
		this.needs = view.needs();
		this.lastDimensionId = view.dimensionId();
		accrueFatigue();
		publishAxis();

		if (tick % SENSE_INTERVAL_TICKS == 0) {
			sense(view);
		}

		hear(view);
		// What the work this being owes makes of what it just perceived. It owns the commitments;
		// the mind only reports what the body can see and hold, and is told what a step is worth.
		dev.ensouled.work.WorkLedger work = dev.ensouled.work.WorkLedger.get();
		if (work != null) {
			work.observe(identity.agentId(), identity.displayName(), view, tick);
		}
		updateAffect(view);
		reflex(view);
		if (reflexEngaged) {
			return;
		}
		innerLife(view);

		if (current != null && current.state().active() && current.expired(tick)) {
			interruptCurrent("expired_intention");
		}
		// Rest is a decision, not deafness: being addressed ends it, so a resting being answers its
		// own incoming speech on the next selection instead of sleeping through it.
		if (current != null && current.state().active()
				&& Phase1GatePolicy.SKILL_REST.equals(current.skillId())
				&& social.bus().peek(identity.agentId(), tick).isPresent()) {
			interruptCurrent("addressed_while_resting");
		}
		if (current != null && current.state().terminal()) {
			journal.close("completed", current, tick);
			release();
		}

		if (current == null && !selectIntention(view)) {
			return;
		}

		switch (current.state()) {
			case ACTIVATED -> commitThroughGate(view);
			case COMMITTED -> startExecuting(view);
			case EXECUTING -> executeStep(view);
			default -> {
				release();
			}
		}
	}

	/**
	 * Answer what this body just heard. The occasion is this tick's own state, so the line the being
	 * chooses is about what it is holding, feeling and doing now.
	 */
	private void respond(Hearing.Heard heard, boolean requestJustRefused, WorldView view) {
		boolean resting = current != null && Phase1GatePolicy.SKILL_REST.equals(current.skillId());
		voice.answer(heard, requestJustRefused, view, new Voice.Occasion(tick, affect, resting,
				current == null ? "" : current.id()));
	}

	private void sense(WorldView view) {
		java.util.List<VisibleBlock> seen = view.visibleBlocks(CandidateGenerator.SIGHT_RADIUS);
		if (tick - lastObservationTick >= OBSERVATION_INTERVAL_TICKS) {
			lastObservationTick = tick;
			publishObservation(view, seen);
		}
		for (VisibleBlock block : seen) {
			if (!BlockIds.isNaturalLog(block.blockId())) {
				continue;
			}
			String site = BlockIds.siteKey(lastDimensionId, TreeSites.base(block.pos(), view));
			boolean collected = isSiteAlreadyCollected(block.pos(), view);
			beliefs.ingest(new Percept(PerceptSource.VISION, tick, CandidateGenerator.SIGHT_RADIUS,
					"block", site, collected ? "harvested_by_self" : "present",
					block.blockId(), 1000, "obs:" + tick + ":" + block.pos()));
		}
		for (EntityStimulus entity : view.entities(8)) {
			if (entity.visible() && entity.hostileEngaged()) {
				beliefs.ingest(new Percept(PerceptSource.VISION, tick, 8, "entity", entity.entityId(),
						"hostile_present", entity.entityTypeId(), 1000,
						"obs:" + tick + ":" + entity.entityId()));
			}
		}
	}

	/**
	 * Ordinary chat this body could hear (CHAT-01). A heard line is evidence and an opening, never an
	 * order: the mind records it, lets the routine that owns that exchange answer for it, and
	 * otherwise reads it itself. A line that names this being is answered in its own chat, through
	 * its own connected client, so the person reads a player line rather than a system notice.
	 */
	private void hear(WorldView view) {
		List<Hearing.Heard> lines = social.hearing().take(identity.agentId(), tick);
		if (!lines.isEmpty()) {
			dev.ensouled.EnsouledMod.LOG.info("[ensouled] {} perceived {} spoken line(s)",
					identity.displayName(), lines.size());
		}
		for (Hearing.Heard heard : lines) {
			ledger.note(tick, identity.agentId(), current == null ? "" : current.id(), EvidenceKind.SOCIAL,
					"", "heard", heard.speakerName() + " (" + heard.speakerId() + "): " + heard.text());
			ledger.remember(tick, EpisodeKind.SOCIAL,
					"heard " + heard.speakerName() + ": " + heard.text(), view.bodyPos(), "heard",
					"ev:" + ledger.evidence().sequence());
			boolean refused = false;
			if (current != null && current.state() == IntentionState.EXECUTING && activeRoutine != null) {
				refused = activeRoutine.hear(skillContext(view), heard.speakerId(),
						heard.speakerName(), heard.text());
			}
			if (refused) {
				requestRefusedHeard = true;
			}
			if (heard.addressedToMe()) {
				spokenToHeard = true;
			}
			if (heard.addressedToMe() || refused) {
				respond(heard, refused, view);
			}
		}
	}

	private SkillContext skillContext(WorldView view) {
		return new SkillContext(view, current, runtime, this, social, identity.agentId(),
				identity.displayName(), interactionsWith(view));
	}

	/** Re-read where this being stands on the wheel, from what its body is going through now. */
	private void updateAffect(WorldView view) {
		boolean resting = current != null && Phase1GatePolicy.SKILL_REST.equals(current.skillId());
		boolean peerPresent = Peers.nearest(view, social, identity.agentId(), 12).isPresent();
		Affect.Situation situation = new Affect.Situation(needs.inDanger(), needs.starving(),
				needs.hungry(), current != null && current.state() == IntentionState.EXECUTING, resting,
				spokenToHeard, requestRefusedHeard, lastActionFailed, lastActionSucceeded, peerPresent);
		Affect before = affect;
		affect = affect.react(situation, tick);
		if (affect.family() != before.family()) {
			ledger.note(tick, identity.agentId(), current == null ? "" : current.id(), EvidenceKind.AFFECT,
					"", "changed", before.path() + " -> " + affect.path() + " radius "
							+ affect.radius() + " drift " + affect.drift());
		}
		lastActionSucceeded = false;
		lastActionFailed = false;
		requestRefusedHeard = false;
		spokenToHeard = false;
	}

	// ---------------------------------------------------------------- inner life

	/**
	 * A being is not a silent process between decisions. Alone with itself it says things out loud; a
	 * close call is a contest and becomes a cell of memory; a resting body works its memories over as
	 * dreams. None of this changes the world, all of it is the being's own.
	 */
	private void innerLife(WorldView view) {
		String intentionId = current == null ? "" : current.id();
		wishes.tick(view, tick, intentionId);
		boolean resting = current != null && Phase1GatePolicy.SKILL_REST.equals(current.skillId())
				&& current.state() == IntentionState.EXECUTING;
		innerLife.tick(view, tick, intentionId, resting, voice.lastSpokenTick());
	}

	/** Say a line the being has already chosen. */
	public boolean say(String text) {
		return voice.say(text, tick);
	}

	private void reflex(WorldView view) {
		Optional<ReflexAction> maybe = reflexes.check(needs, view);
		if (maybe.isPresent()) {
			ReflexAction action = maybe.get();
			if (!action.code().equals(lastReflexCode)) {
				interruptCurrent("reflex:" + action.code());
				ledger.note(tick, identity.agentId(), "", EvidenceKind.REFLEX, "",
						"engaged", action.code() + ": " + action.reason());
				ledger.remember(tick, EpisodeKind.REFLEX_INTERRUPT,
						"reflex " + action.code() + ": " + action.reason(), view.bodyPos(),
						"self_performed", "ev:" + ledger.evidence().sequence());
				lastReflexCode = action.code();
			}
			if (!runtime.awaiting()) {
				runtime.issue(action.command());
			}
			runtime.poll();
			reflexEngaged = true;
			return;
		}
		if (reflexEngaged) {
			ledger.note(tick, identity.agentId(), "", EvidenceKind.REFLEX, "", "cleared", lastReflexCode);
			lastReflexCode = "";
		}
		reflexEngaged = false;
	}

	/**
	 * One choice per decision cycle, from the candidates this world actually allows. A candidate whose
	 * declared preconditions are missing is skipped rather than ending the decision: otherwise a
	 * blocked request would starve every candidate below it — including choosing to rest — and the
	 * being would look idle while its own tree still had something it could do.
	 */
	private boolean selectIntention(WorldView view) {
		List<Candidate> candidates = generator.generate(view, this);
		lastDecisionChoices = candidates.size();
		reasonAboutContest(view, candidates);
		// The reading is taken before anything is ranked. A selection that does not stand leaves the
		// contest with its code on record, and a weaker reading ranks below a stronger one — so the
		// ladder decides which action is taken instead of describing the action already taken.
		Map<String, AxisGate.Reading> readings = new java.util.LinkedHashMap<>();
		Map<String, Integer> ranks = new java.util.LinkedHashMap<>();
		List<Candidate> standing = new ArrayList<>();
		for (Candidate candidate : candidates) {
			AxisGate.Reading reading = readCandidate(view, candidate);
			readings.put(candidate.candidateId(), reading);
			ranks.put(candidate.candidateId(), reading.rank());
			if (reading.admitted()) {
				standing.add(candidate);
				continue;
			}
			ledger.note(tick, identity.agentId(), "", EvidenceKind.GATE, candidate.skillId(), "refused",
					reading.decision().code() + ": " + reading.decision().why());
			ledger.remember(tick, EpisodeKind.ACTION_REFUSED,
					"axis refused " + candidate.skillId() + ": " + reading.decision().code(),
					candidate.target(), "axis", "ev:" + ledger.evidence().sequence());
			StreamNotices.refuse(identity.displayName(), candidate.skillId(),
					reading.decision().code(), reading.decision().why());
		}
		lastReadingRanked = ranks;
		List<Candidate> remaining = new ArrayList<>(standing);
		while (!remaining.isEmpty()) {
			Optional<Candidate> choice = arbiter.select(remaining, null, ranks);
			if (choice.isEmpty()) {
				return false;
			}
			Candidate candidate = choice.get();
			remaining.remove(candidate);
			SkillRoutine routine = skills.newRoutine(candidate.skillId());
			Intention intention = new Intention("intent:" + (++intentionSequence), candidate.skillId(),
					candidate.target(), candidate.targetBlockId(), tick, tick + INTENTION_TTL_TICKS,
					Phase1GatePolicy.SKILL_HARVEST.equals(candidate.skillId()) ? 1 : 0);
			SkillContext ctx = new SkillContext(view, intention, runtime, this, social,
					identity.agentId(), identity.displayName(), interactionsWith(view));
			List<String> missing = routine.unmetPreconditions(ctx);
			if (!missing.isEmpty()) {
				// This candidate is not available now, but the being is not out of options: record the
				// refusal against the candidate and let the next one try.
				ledger.note(tick, identity.agentId(), intention.id(), EvidenceKind.GATE,
						intention.id(), "blocked", candidate.skillId() + " preconditions missing: " + missing);
				ledger.remember(tick, EpisodeKind.ACTION_REFUSED,
						"refused " + candidate.skillId() + ": " + missing, candidate.target(),
						"self_observed", "ev:" + ledger.evidence().sequence());
				ledger.practiced(candidate.skillId(), tick, false, "gate:" + candidate.candidateId());
				StreamNotices.refuse(identity.displayName(), candidate.skillId(), "preconditions",
						String.join(", ", missing));
				continue;
			}
			AxisGate.Reading reading = readings.get(candidate.candidateId());
			AxisGate.Decision verdict = reading.decision();
			// The reading was taken before selection; the candidate that won on that reading is the one
			// committed to here, so this is the reading the ledger and the stream carry.
			axis.record(verdict, dev.ensouled.axis.AxisVocabulary.Kind.PROCEDURAL_GATE, tick);
			publishDecision(view, candidate, intention, reading);
			ledger.note(tick, identity.agentId(), intention.id(), EvidenceKind.DECISION,
					intention.id(), "selected", candidate.skillId() + " at " + reading.establishment()
							+ " utility=" + candidate.utilityMilli() + " of " + candidates.size()
							+ " candidate(s) at " + standing.size() + " standing");
			lastVerdict = verdict;
			lastReading = reading.establishment();
			current = intention;
			activeRoutine = routine;
			currentEnvelope = null;
			return true;
		}
		return false;
	}

	/** The observation lane: what this being could see, on its own stream and nowhere else. */
	private void publishObservation(WorldView view, java.util.List<VisibleBlock> seen) {
		SurfaceStream stream = SurfaceStream.get();
		if (stream == null) {
			return;
		}
		try {
			JsonObject pkg = ObservationPackage.of(identity.displayName(), view, seen, lastObservationDigest);
			stream.observation(identity.displayName(), pkg);
			// The row is written by this point. Reading a digest that is not there used to throw after
			// the write and report the observation as lost, which was the opposite of what happened.
			com.google.gson.JsonElement digest = pkg.get("digest");
			lastObservationDigest = digest != null && digest.isJsonPrimitive() ? digest.getAsString() : "";
		} catch (RuntimeException rejected) {
			dev.ensouled.EnsouledMod.LOG.warn("[ensouled] observation for {} was not written: {}",
					identity.displayName(), rejected.getMessage());
		}
	}

	/**
	 * Read one candidate before anything is ranked: the gate's own checks, and where this being's
	 * evidence puts the selection on the establishment ladder.
	 *
	 * <p>The rung is a fact about the world this being has already recorded, never a value the
	 * candidate supplied about itself: an action with recorded successes behind it is {@code active},
	 * an action whose record is against it is {@code stale}, and an action with nothing on record is
	 * {@code unchecked} — a state, not a pass. That is what makes the reading decide the choice: two
	 * candidates in the same world with the same utility are separated by the rung, and a different
	 * rung picks a different action.
	 */
	private AxisGate.Reading readCandidate(WorldView view, Candidate candidate) {
		boolean perceived = isPerceived(view, candidate.target());
		boolean takenBefore = !candidate.targetBlockId().isBlank()
				&& isSiteAlreadyCollected(candidate.target(), view);
		String establishment = AxisReadings.establishment(perceived, takenBefore);
		String basis = AxisReadings.basis(candidate.skillId(), candidate.target(), perceived, takenBefore);
		AxisGate.Selection selection = new AxisGate.Selection(
				candidate.needRelief() > 0 ? "need-relief" : "commitment", candidate.utilityMilli(),
				candidate.skillId(), true, true, 1, -1);
		// The witness is this being's own evidence sequence: the rung cites the record it was read
		// from, and a rung with no record would be unchecked rather than passed.
		String witness = "ev:" + ledger.evidence().sequence();
		AxisGate.Reading reading = AxisGate.read(candidate.skillId() + "@" + candidate.target(),
				selection, witness, establishment);
		ledger.note(tick, identity.agentId(), "", EvidenceKind.GATE, candidate.candidateId(), "ranked",
				reading.establishment() + ": " + basis);
		return reading;
	}

	/**
	 * Is the thing this selection would act on actually there to be seen? Its own body always is; a
	 * block counts only with a clear line of sight; another being counts when it is at that position.
	 */
	private boolean isPerceived(WorldView view, Vec3i target) {
		if (target == null) {
			return false;
		}
		if (target.equals(view.bodyPos())) {
			return true;
		}
		if (view.blockAt(target).filter(VisibleBlock::lineOfSight).isPresent()) {
			return true;
		}
		for (EntityStimulus entity : view.entities(8)) {
			if (entity.visible() && entity.pos().equals(target)) {
				return true;
			}
		}
		return false;
	}

	/** One decision, streamed to this being's feed and nowhere else. */
	private void publishDecision(WorldView view, Candidate candidate, Intention intention,
			AxisGate.Reading reading) {
		SurfaceStream stream = SurfaceStream.get();
		if (stream == null) {
			return;
		}
		String subject = candidate.skillId() + "@" + candidate.target();
		try {
			// The ticket authorises this decision, and the decision spends one of its declared
			// cells. When no ticket covers the step, the row says so with cell -1 rather than
			// borrowing an authorisation the being does not have.
			String ticketId = "";
			int cell = -1;
			dev.ensouled.work.WorkLedger work = dev.ensouled.work.WorkLedger.get();
			if (work != null) {
				java.util.Optional<dev.ensouled.work.Ticket> active = work.activeTicket(identity.agentId());
				if (active.isPresent()) {
					cell = work.cell(identity.agentId(), intention.id(), candidate.skillId(), tick);
					if (cell >= 0) {
						ticketId = active.get().ticketId();
					}
				}
			}
			stream.decision(identity.displayName(), dev.ensouled.axis.DecisionPackage.of(
					identity.displayName(), tick, candidate.skillId(), subject, reading.decision(),
					intention.id(), intention.startedTick(), view.bodyPos(), view.dimensionId(),
					reading.establishment(), ticketId, cell));
		} catch (RuntimeException rejected) {
			dev.ensouled.EnsouledMod.LOG.warn("[ensouled] decision package for {} was not written: {}",
					identity.displayName(), rejected.getMessage());
		}
	}

	/**
	 * Close the axis cells when the horizon arrives and publish the package to this being's stream.
	 * Thousands of decisions travel as one chained record, not as thousands of rows.
	 */
	private void publishAxis() {
		long evidence = ledger.evidence().sequence();
		if (!axis.due(tick, evidence)) {
			return;
		}
		JsonObject pkg = axis.close(tick, evidence, identity.displayName());
		SurfaceStream stream = SurfaceStream.get();
		if (stream != null) {
			stream.axisPackage(identity.displayName(), pkg);
		}
		dev.ensouled.EnsouledMod.LOG.info(
				"[ensouled] axis cells {} decisions={} admitted={} refused={} unchecked={} absent={} tokens={} digest={}",
				identity.displayName(), pkg.get("decisions").getAsInt(), pkg.get("admitted").getAsInt(),
				pkg.get("refused").getAsInt(), pkg.get("unchecked").getAsInt(),
				pkg.get("absent").getAsInt(), pkg.get("tokens"), pkg.get("digest").getAsString());
	}

	/**
	 * When two things are nearly equally worth doing, the being notices the contest and keeps it: a
	 * close call is a cell of memory, not a detail that evaporates in the arbiter's sort.
	 */
	private void reasonAboutContest(WorldView view, List<Candidate> candidates) {
		if (candidates.size() < 2 || tick - lastThoughtTick < CONTEST_QUIET_TICKS) {
			return;
		}
		List<Candidate> ordered = new ArrayList<>(candidates);
		ordered.sort((a, b) -> Integer.compare(b.utilityMilli(), a.utilityMilli()));
		Candidate best = ordered.get(0);
		Candidate rival = ordered.get(1);
		int margin = best.utilityMilli() - rival.utilityMilli();
		if (margin >= CONTEST_MARGIN_MILLI) {
			return;
		}
		lastThoughtTick = tick;
		String and = Lexicon.corpus().prefer("and", "with").orElse("and");
		ledger.remember(tick, EpisodeKind.THOUGHT, "weighed " + best.skillId() + " " + and + " "
				+ rival.skillId() + " (" + margin + " apart)", view.bodyPos(), "self_reasoned",
				"th:" + (++thoughtSequence));
	}

	private void commitThroughGate(WorldView view) {
		ActionEnvelope envelope = envelopeFor(current);
		GateDecision decision = gate.decide(envelope, view);
		ledger.note(tick, identity.agentId(), current.id(), EvidenceKind.GATE, envelope.actionHash(),
				decision.verdict().name().toLowerCase(), decision.reasonCode() + ": " + decision.detail());

		if (decision.permitted()) {
			currentEnvelope = envelope;
			current.commit();
			return;
		}
		if (decision.verdict() == GateDecision.Verdict.REFUSE) {
			current.block(decision.reasonCode());
			ledger.remember(tick, EpisodeKind.ACTION_REFUSED,
					"gate refused " + current.skillId() + ": " + decision.reasonCode(), current.target(),
					"gate", "ev:" + ledger.evidence().sequence());
			StreamNotices.refuse(identity.displayName(), current.skillId(), decision.reasonCode(), decision.detail());
			release();
		}
		// HOLD: the intention stays ACTIVATED and is retried on a later tick until it expires.
	}

	private void startExecuting(WorldView view) {
		// §49.3: the decision reaches the journal before the action it justifies exists. An action
		// that starts without a durable record is an action nobody can recover, so it does not start.
		if (!journal.writeAhead(current, view, tick, lastVerdict, lastReading, currentEnvelope)) {
			current.block("decision-not-durable");
			ledger.note(tick, identity.agentId(), current.id(), EvidenceKind.GATE, "", "refused",
					"decision-not-durable: the journal could not take the decision, so no action began");
			StreamNotices.refuse(identity.displayName(), current.skillId(), "decision-not-durable",
					"the write-ahead record failed, so the action was not started");
			journal.close("refused:decision-not-durable", current, tick);
			release();
			return;
		}
		current.startExecution();
		runtime.reset();
		decisionCount++;
		activeRoutine.begin(new SkillContext(view, current, runtime, this, social, identity.agentId(),
				identity.displayName(), interactionsWith(view)));
	}

	private void executeStep(WorldView view) {
		runtime.poll();
		MotorResult finished = runtime.takeJustFinished();
		if (finished != null) {
			ledger.note(tick, identity.agentId(), current == null ? "" : current.id(), EvidenceKind.MOTOR,
					"", finished.status().name().toLowerCase(),
					finished.code() + ": " + finished.detail() + " " + finished.facts());
		}
		if (current == null || current.state() != IntentionState.EXECUTING) {
			return;
		}
		activeRoutine.tick(new SkillContext(view, current, runtime, this, social, identity.agentId(),
				identity.displayName(), interactionsWith(view)));
	}

	@Override
	public void motorIssued(MotorCommand command) {
		ledger.note(tick, identity.agentId(), current == null ? "" : current.id(), EvidenceKind.MOTOR,
				"", "issued", command.describe());
	}

	private InteractionSession interactionsWith(WorldView view) {
		return new InteractionSession(hands, gate, view, this::note);
	}

	/**
	 * World objects this body can perceive as interaction candidates, nearest first. Perception only:
	 * contents and every operation still pass the authority gateway through the session.
	 */
	public List<InteractionTarget> perceivedObjects(int radius) {
		return hands.targets(radius);
	}

	/**
	 * How much of an item one perceived object holds, read through the same gate as any interaction.
	 * Candidate generation uses this so a requirement is grounded in what the being can actually see.
	 * It is perception, not an act, so it is not entered in the evidence trail; a refused or failed
	 * read answers the sentinel {@link #UNKNOWN_COUNT} rather than a guess.
	 */
	public int storedCount(WorldView view, InteractionTarget target, String itemId) {
		InteractionOutcome outcome = new InteractionSession(hands, gate, view, (kind, result, detail) -> {
		}).perform(target, InteractionRequest.inspect(itemId, "perception"));
		return outcome.performed() ? outcome.intFact(itemId, 0) : UNKNOWN_COUNT;
	}

	/** A container read that could not be completed; never treated as an amount of zero. */
	public static final int UNKNOWN_COUNT = -1;

	@Override
	public void recordContainerMove(String transferId, Vec3i where, String operation, String itemId,
			int count) {
		ledger.recordContainerMove(tick, identity.agentId(), where, operation, transferId, itemId, count);
	}

	@Override
	public void note(EvidenceKind kind, String outcome, String detail) {
		ledger.note(tick, identity.agentId(), current == null ? "" : current.id(), kind, "", outcome,
				detail);
	}

	private ActionEnvelope envelopeFor(Intention intention) {
		return new ActionEnvelope(ActionEnvelope.SCHEMA, identity.agentId(), intention.id(),
				intention.skillId(), intention.target(), intention.targetBlockId(), lastDimensionId,
				tick, intention.expiresTick(), intention.budgetCost());
	}

	// ---------------------------------------------------------------- skill callbacks

	@Override
	public void completed(String detail, Map<String, String> observedFacts) {
		Intention intention = current;
		if (intention == null || intention.state() != IntentionState.EXECUTING) {
			return;
		}
		String hash = currentEnvelope == null ? "" : currentEnvelope.actionHash();
		intention.beginVerify();
		ledger.note(tick, identity.agentId(), intention.id(), EvidenceKind.VERIFY, hash,
				"satisfied", detail + " " + observedFacts);
		intention.satisfy();

		if (Phase1GatePolicy.SKILL_HARVEST.equals(intention.skillId())) {
			ledger.recordCollection(tick, identity.agentId(), lastDimensionId, intention.target(),
					intention.targetBlockId());
			gate.spend(currentEnvelope, 1);
		}
		if (Phase1GatePolicy.SKILL_REST.equals(intention.skillId())) {
			// The rest did what it was chosen for: the body is less tired than it was.
			int before = fatigue;
			fatigue = Math.max(0, fatigue - FATIGUE_REST_RELIEF);
			ledger.note(tick, identity.agentId(), intention.id(), EvidenceKind.LEARN, hash,
					"rested", "fatigue " + before + " -> " + fatigue);
		}
		if (Phase1GatePolicy.SKILL_REQUEST_FOOD.equals(intention.skillId())) {
			// The recipient records the physical truth: its own inventory grew, once per transfer id.
			ledger.recordTransfer(tick, identity.agentId(), intention.target(),
					observedFacts.getOrDefault("transfer_id", ""),
					observedFacts.getOrDefault("item", ""),
					Integer.parseInt(observedFacts.getOrDefault("count", "0")));
		}
		if (Phase1GatePolicy.SKILL_REQUEST_SUPPLIES.equals(intention.skillId())
				|| Phase1GatePolicy.SKILL_REQUEST_FROM_HUMAN.equals(intention.skillId())) {
			// Same rule for a requested resource, from a peer or a human: the receiving mind cites its
			// own verified delta, never the fact that it was promised something.
			ledger.recordTransfer(tick, identity.agentId(), intention.target(),
					observedFacts.getOrDefault("transfer_id", ""),
					observedFacts.getOrDefault("item", ""),
					Integer.parseInt(observedFacts.getOrDefault("received", "0")));
		}

		lastActionSucceeded = true;
		ledger.practiced(intention.skillId(), tick, true, "intent:" + intention.id());
		dev.ensouled.work.WorkLedger work = dev.ensouled.work.WorkLedger.get();
		if (work != null) {
			work.discovered(identity.agentId(), identity.displayName(), intention.skillId(), tick,
					true, "intent:" + intention.id());
		}
		ledger.note(tick, identity.agentId(), intention.id(), EvidenceKind.LEARN, hash,
				"reinforced", intention.skillId() + " successes="
						+ ledger.stats(intention.skillId()).successes() + " failures="
						+ ledger.stats(intention.skillId()).failures());

		// The decision is over, so its row is closed here rather than only at the next boot. Without
		// this, every decision that finished normally was still "open" on disk: a restart then
		// reported hundreds of completed wander steps as interrupted, and the one decision that
		// really was in flight when the process died could not be told apart from them.
		journal.close("completed", intention, tick);
		release();
		runtime.reset();
	}

	@Override
	public void failed(String code, String detail) {
		Intention intention = current;
		if (intention == null || intention.state().terminal()) {
			return;
		}
		String hash = currentEnvelope == null ? "" : currentEnvelope.actionHash();
		intention.fail(code);
		// The failing code alone is not a diagnosis; the detail says where it stopped.
		dev.ensouled.EnsouledMod.LOG.info("[ensouled] {} failed {}: {} at {}", identity.displayName(),
				intention.skillId(), code, detail);
		ledger.note(tick, identity.agentId(), intention.id(), EvidenceKind.VERIFY, hash, "failed",
				code + ": " + detail);
		ledger.remember(tick, EpisodeKind.ACTION_FAILED,
				intention.skillId() + " failed: " + code, intention.target(), "self_performed",
				"ev:" + ledger.evidence().sequence());
		lastActionFailed = true;
		ledger.practiced(intention.skillId(), tick, false, "intent:" + intention.id());
		dev.ensouled.work.WorkLedger work = dev.ensouled.work.WorkLedger.get();
		if (work != null) {
			work.discovered(identity.agentId(), identity.displayName(), intention.skillId(), tick,
					false, "intent:" + intention.id());
		}
		ledger.note(tick, identity.agentId(), intention.id(), EvidenceKind.LEARN, hash,
				"failure_recorded", intention.skillId() + " code=" + code);

		// Same rule as a completed decision: a failure is an outcome, and an outcome that is not on
		// disk leaves the decision looking like the one a crash caught mid-action.
		journal.close("failed:" + code, intention, tick);
		release();
		runtime.cancel();
	}

	/** The decision this being is carrying is over: what it was doing, how, and under what record. */
	private void release() {
		current = null;
		activeRoutine = null;
		currentEnvelope = null;
	}

	private void interruptCurrent(String reason) {
		if (current != null && current.state().active()) {
			current.interrupt(reason);
			ledger.note(tick, identity.agentId(), current.id(), EvidenceKind.DECISION, "",
					"interrupted", reason);
			journal.close("interrupted:" + reason, current, tick);
		}
		runtime.cancel();
		release();
	}

	// ---------------------------------------------------------------- queries

	public boolean isSiteAlreadyCollected(Vec3i pos, WorldView view) {
		return ledger.wasCollectedAt(lastDimensionId, TreeSites.base(pos, view), pos,
				MindLedger.SAME_SITE_RADIUS);
	}

	public AgentIdentity identity() {
		return identity;
	}

	public NeedsState needs() {
		return needs;
	}

	public Affect affect() {
		return affect;
	}

	/** Affect arrives with the restored mind: a being wakes up still feeling its own day. */
	void adoptRestoredAffect(Affect restored) {
		this.affect = restored;
	}

	public BeliefStore beliefs() {
		return beliefs;
	}

	public MindLedger ledger() {
		return ledger;
	}

	public SocialSeam social() {
		return social;
	}

	public Optional<Intention> currentIntention() {
		return Optional.ofNullable(current);
	}

	public AuthorityGateway gate() {
		return gate;
	}

	public long tick() {
		return tick;
	}

	public long intentionSequence() {
		return intentionSequence;
	}

	public String dimensionId() {
		return lastDimensionId;
	}

	public int decisionCount() {
		return decisionCount;
	}

	/** How tired this body is, 0..{@link #FATIGUE_MAX}. */
	public int fatigue() {
		return fatigue;
	}

	/** Whether the body's own weariness is enough that rest is wanted, not merely available. */
	public boolean needsRest() {
		return fatigue >= FATIGUE_REST_WANTED;
	}

	/** How much the body wants to be up and doing: the inverse of its weariness. */
	public int restlessness() {
		return FATIGUE_MAX - fatigue;
	}

	/**
	 * Weariness accrues with being awake: effort costs more than standing about, and choosing rest
	 * pays some of it back while the rest is still happening.
	 */
	private void accrueFatigue() {
		if (current != null && Phase1GatePolicy.SKILL_REST.equals(current.skillId())) {
			fatigue = Math.max(0, fatigue - 4);
			return;
		}
		boolean working = current != null && current.state() == IntentionState.EXECUTING;
		int increment = working ? 2 : (tick % 2 == 0 ? 1 : 0);
		fatigue = Math.min(FATIGUE_MAX, fatigue + increment);
	}

	public int lastDecisionChoices() {
		return lastDecisionChoices;
	}

	public boolean reflexEngaged() {
		return reflexEngaged;
	}

	public String describe() {
		StringBuilder sb = new StringBuilder();
		sb.append(identity.displayName()).append(" [").append(identity.agentId()).append("]")
				.append(" food=").append(needs.food()).append(" health=").append(needs.health())
				.append(" fatigue=").append(fatigue).append(" tick=").append(tick).append('\n');
		sb.append("  intentions=").append((long) intentionSequence)
				.append(" decisions=").append(decisionCount)
				.append(" harvests=").append(ledger.harvestCount())
				.append(" budget=").append(gate.budgetRemaining()).append('\n');
		sb.append("  current=").append(current == null ? "idle" : current.toString()).append('\n');
		sb.append("  affect=").append(affect.describe()).append('\n');
		sb.append("  beliefs=").append(beliefs.size()).append(" episodes=").append(ledger.memory().size())
				.append(" evidence=").append(ledger.evidence().size())
				.append(" events=").append(ledger.events().size());
		return sb.toString();
	}

	/** Restored state arrives whole; the decision cycle reads it, it never rebuilds it. */
	void adoptRestored(long restoredTick, long restoredIntentionSequence, String dimension,
			int restoredDecisions) {
		this.tick = restoredTick;
		this.intentionSequence = restoredIntentionSequence;
		this.lastDimensionId = dimension;
		this.decisionCount = restoredDecisions;
	}

	/** A being wakes up as tired as it went to sleep. */
	void adoptRestoredFatigue(int restoredFatigue) {
		this.fatigue = Math.max(0, Math.min(FATIGUE_MAX, restoredFatigue));
	}
}

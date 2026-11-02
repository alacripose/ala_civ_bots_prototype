package dev.ensouled.core;

import java.util.ArrayList;
import java.util.List;

import dev.ensouled.interact.InteractionHands;

/**
 * Turns a mind into a portable state tree and back. The mind's own decisions stay in
 * {@link AgentMind}; this owner knows the wire layout, so a change to what is saved never touches
 * the decision cycle.
 */
public final class MindPersistence {
	/**
	 * Layout of the mind's own saved state. It is written into every snapshot, so a build that meets
	 * a newer mind marks that life INTEGRITY_BLOCKED instead of reinterpreting its memory (§49.1).
	 */
	public static final int FORMAT = 1;

	private MindPersistence() {
	}

	public static StateNode save(AgentMind mind) {
		MindLedger ledger = mind.ledger();
		AgentIdentity identity = mind.identity();
		StateNode root = new StateNode();
		root.putInt("mind_format", FORMAT);
		root.putString("agent_id", identity.agentId());
		root.putString("display_name", identity.displayName());
		root.putString("gender_schema", identity.genderSchemaRef());
		root.putString("gender_value", identity.genderValue());
		root.putLong("created_epoch", identity.createdEpochMilli());
		root.putLong("birth_tick", identity.birthTick());
		root.putString("lineage", identity.lineageNote());
		root.putString("dimension", mind.dimensionId());
		root.putLong("saved_tick", mind.tick());
		root.putLong("intention_sequence", mind.intentionSequence());
		root.putLong("episode_sequence", ledger.episodeSequence());
		root.putLong("evidence_sequence", ledger.evidence().sequence());
		root.putLong("event_sequence", ledger.events().sequence());
		root.putInt("harvest_count", ledger.harvestCount());
		root.putInt("decision_count", mind.decisionCount());
		root.putInt("budget", mind.gate().budgetRemaining());
		root.putString("affect_family", mind.affect().family().name());
		root.putString("affect_secondary", mind.affect().secondary());
		root.putString("affect_tertiary", mind.affect().tertiary());
		root.putInt("affect_radius", mind.affect().radius());
		root.putInt("affect_drift", mind.affect().drift());
		root.putInt("fatigue", mind.fatigue());

		List<StateNode> beliefNodes = new ArrayList<>();
		for (Belief b : mind.beliefs().snapshot()) {
			beliefNodes.add(new StateNode()
					.putString("k", b.key()).putString("v", b.value())
					.putInt("c", b.confidenceMilli()).putString("s", b.source().name())
					.putLong("t", b.observedTick()).putString("e", b.evidenceRef()));
		}
		root.putNodeList("beliefs", beliefNodes);

		List<StateNode> episodeNodes = new ArrayList<>();
		for (Episode e : ledger.memory().all()) {
			episodeNodes.add(new StateNode()
					.putString("id", e.episodeId()).putLong("t", e.tick())
					.putString("kind", e.kind().name()).putString("summary", e.summary())
					.putInt("x", e.where().x()).putInt("y", e.where().y()).putInt("z", e.where().z())
					.putString("prov", e.provenance()).putString("ev", e.evidenceRef()));
		}
		root.putNodeList("episodes", episodeNodes);

		List<StateNode> evidenceNodes = new ArrayList<>();
		for (EvidenceRecord r : ledger.evidence().all()) {
			evidenceNodes.add(new StateNode()
					.putString("id", r.recordId()).putLong("t", r.tick())
					.putString("agent", r.agentId()).putString("intent", r.intentionId())
					.putString("kind", r.kind().name()).putString("hash", r.actionHash())
					.putString("outcome", r.outcome()).putString("detail", r.detail()));
		}
		root.putNodeList("evidence", evidenceNodes);

		List<StateNode> eventNodes = new ArrayList<>();
		for (CanonicalEvent e : ledger.events().all()) {
			eventNodes.add(new StateNode()
					.putString("id", e.eventId()).putString("type", e.type()).putLong("t", e.tick())
					.putString("agent", e.agentId()).putInt("x", e.where().x())
					.putInt("y", e.where().y()).putInt("z", e.where().z())
					.putString("subject", e.subjectId()).putString("detail", e.detail()));
		}
		root.putNodeList("events", eventNodes);

		List<StateNode> practiceNodes = new ArrayList<>();
		for (MindLedger.Practice p : ledger.practice()) {
			practiceNodes.add(new StateNode().putLong("t", p.tick())
					.putString("skill", p.skillId()).putString("verb", p.verb())
					.putString("ok", Boolean.toString(p.success()))
					.putString("e", p.evidence()));
		}
		root.putNodeList("practice", practiceNodes);


		mind.currentIntention()
				.filter(intention -> intention.state().active())
				.ifPresent(intention -> root.putNode("in_flight", new StateNode()
						.putString("intention", intention.id()).putString("skill", intention.skillId())
						.putInt("x", intention.target().x()).putInt("y", intention.target().y())
						.putInt("z", intention.target().z()).putString("state", intention.state().name())));
		return root;
	}

	public static AgentMind restore(StateNode root, AuthorityGateway gate, Motor motor, SkillRegistry skills) {
		return restore(root, gate, motor, skills, SocialSeam.isolated());
	}

	public static AgentMind restore(StateNode root, AuthorityGateway gate, Motor motor, SkillRegistry skills,
			SocialSeam social) {
		return restore(root, gate, motor, skills, social, InteractionHands.none());
	}

	public static AgentMind restore(StateNode root, AuthorityGateway gate, Motor motor, SkillRegistry skills,
			SocialSeam social, InteractionHands hands) {
		AgentIdentity identity = new AgentIdentity(
				root.string("agent_id", "agent:unknown"),
				root.string("display_name", "unknown"),
				root.string("gender_schema", ""),
				root.string("gender_value", ""),
				root.lng("created_epoch", 0L),
				root.lng("birth_tick", 0L),
				root.string("lineage", ""));
		AgentMind mind = new AgentMind(identity, gate, motor, skills, social, hands);
		MindLedger ledger = mind.ledger();

		mind.adoptRestored(root.lng("saved_tick", 0L), root.lng("intention_sequence", 0L),
				root.string("dimension", "minecraft:overworld"), root.integer("decision_count", 0));
		gate.restoreBudget(root.integer("budget", gate.budgetRemaining()));
		mind.adoptRestoredAffect(Affect.restore(root.string("affect_family", ""),
				root.string("affect_secondary", ""), root.string("affect_tertiary", ""),
				root.integer("affect_radius", Affect.CALM), root.integer("affect_drift", 0),
				mind.tick()));
		ledger.restoreHarvestCount(root.integer("harvest_count", 0));
		mind.adoptRestoredFatigue(root.integer("fatigue", 0));

		List<Belief> beliefList = new ArrayList<>();
		for (StateNode n : root.nodes("beliefs")) {
			beliefList.add(new Belief(n.string("k", ""), n.string("v", ""), n.integer("c", 0),
					parseSource(n.string("s", "")), n.lng("t", 0L), n.string("e", "")));
		}
		mind.beliefs().restore(beliefList);

		List<Episode> episodeList = new ArrayList<>();
		for (StateNode n : root.nodes("episodes")) {
			episodeList.add(new Episode(n.string("id", ""), n.lng("t", 0L),
					parseKind(n.string("kind", "")), n.string("summary", ""),
					new Vec3i(n.integer("x", 0), n.integer("y", 0), n.integer("z", 0)),
					n.string("prov", ""), n.string("ev", "")));
		}
		ledger.restoreEpisodes(episodeList, root.lng("episode_sequence", 0L));

		List<EvidenceRecord> evidenceList = new ArrayList<>();
		for (StateNode n : root.nodes("evidence")) {
			evidenceList.add(new EvidenceRecord(n.string("id", ""), n.lng("t", 0L),
					n.string("agent", ""), n.string("intent", ""),
					parseEvidenceKind(n.string("kind", "")), n.string("hash", ""),
					n.string("outcome", ""), n.string("detail", "")));
		}
		ledger.restoreEvidence(evidenceList, root.lng("evidence_sequence", 0L));

		List<CanonicalEvent> eventList = new ArrayList<>();
		for (StateNode n : root.nodes("events")) {
			eventList.add(new CanonicalEvent(n.string("id", ""), n.string("type", ""), n.lng("t", 0L),
					n.string("agent", ""),
					new Vec3i(n.integer("x", 0), n.integer("y", 0), n.integer("z", 0)),
					n.string("subject", ""), n.string("detail", "")));
		}
		ledger.restoreEvents(eventList, root.lng("event_sequence", 0L));

		List<MindLedger.Practice> practiced = new ArrayList<>();
		for (StateNode n : root.nodes("practice")) {
			practiced.add(new MindLedger.Practice(n.lng("t", 0L), n.string("skill", ""),
					n.string("verb", ""), Boolean.parseBoolean(n.string("ok", "false")),
					n.string("e", "")));
		}
		if (practiced.isEmpty()) {
			// A save written before the practice log carries the derived counters only. Import them
			// once as practice rows with a named provenance, so the log stays the one record and the
			// being keeps the verbs it already learned when it logs back in.
			long importedAt = root.lng("saved_tick", 0L);
			for (StateNode n : root.nodes("stats")) {
				practiced.addAll(migratedPractice(n.string("skill", ""), n.integer("successes", 0),
						n.integer("failures", 0), importedAt));
			}
		}
		ledger.restorePractice(practiced);

		StateNode inFlight = root.node("in_flight");
		if (inFlight.keys().contains("intention")) {
			Vec3i target = new Vec3i(inFlight.integer("x", 0), inFlight.integer("y", 0),
					inFlight.integer("z", 0));
			ledger.remember(mind.tick(), EpisodeKind.ACTION_FAILED,
					"interrupted at restart: " + inFlight.string("skill", "") + " at " + target,
					target, "self_observed", "ce:restart");
			ledger.note(mind.tick(), identity.agentId(), inFlight.string("intention", ""),
					EvidenceKind.DECISION, "", "interrupted", "process restart");
		}
		return mind;
	}

	/** Counters from a pre-log save become practice rows, labelled by where they came from. */
	private static List<MindLedger.Practice> migratedPractice(String skillId, int successes, int failures,
			long tick) {
		List<MindLedger.Practice> rows = new ArrayList<>();
		String verb = MindLedger.learnedKey(skillId);
		for (int i = 0; i < successes; i++) {
			rows.add(new MindLedger.Practice(tick, skillId, verb, true, "migrated:stats"));
		}
		for (int i = 0; i < failures; i++) {
			rows.add(new MindLedger.Practice(tick, skillId, verb, false, "migrated:stats"));
		}
		return rows;
	}

	private static PerceptSource parseSource(String value) {
		try {
			return PerceptSource.valueOf(value);
		} catch (IllegalArgumentException e) {
			return PerceptSource.MEMORY;
		}
	}

	private static EpisodeKind parseKind(String value) {
		try {
			return EpisodeKind.valueOf(value);
		} catch (IllegalArgumentException e) {
			return EpisodeKind.OTHER;
		}
	}

	private static EvidenceKind parseEvidenceKind(String value) {
		try {
			return EvidenceKind.valueOf(value);
		} catch (IllegalArgumentException e) {
			return EvidenceKind.DECISION;
		}
	}
}

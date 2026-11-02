package dev.ensouled;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.ensouled.core.ActionEnvelope;
import dev.ensouled.core.AgentIdentity;
import dev.ensouled.core.AgentMind;
import dev.ensouled.core.Candidate;
import dev.ensouled.core.CanonicalEvent;
import dev.ensouled.core.EpisodeKind;
import dev.ensouled.core.GateDecision;
import dev.ensouled.core.MindPersistence;
import dev.ensouled.core.NeedsState;
import dev.ensouled.core.Phase1GatePolicy;
import dev.ensouled.core.SkillRegistry;
import dev.ensouled.core.StateNode;
import dev.ensouled.core.Vec3i;
import dev.ensouled.core.Arbiter;

class Phase1AcceptanceTest {
	private static final String AGENT_ID = "agent:test";
	private dev.ensouled.persist.MindJournal journal;

	@BeforeEach
	void openJournal() throws java.io.IOException {
		journal = TestJournal.install("phase1");
	}

	@AfterEach
	void closeJournal() {
		TestJournal.uninstall(journal);
	}
	private static final Vec3i TREE = new Vec3i(5, 0, 0);

	private static AgentMind mind(FakeWorld world, FakeMotor motor, Phase1GatePolicy gate) {
		AgentIdentity identity = new AgentIdentity(AGENT_ID, "Ash", "", "unspecified", 0L, 0L, "test");
		return new AgentMind(identity, gate, motor, SkillRegistry.phase1());
	}

	private static void run(AgentMind mind, FakeWorld world, int ticks) {
		for (int i = 0; i < ticks; i++) {
			world.tick++;
			mind.tick(world);
		}
	}

	private static FakeWorld treeWorld() {
		FakeWorld world = new FakeWorld();
		world.blocks.put(TREE, "minecraft:oak_log");
		world.blocks.put(TREE.above(), "minecraft:oak_log");
		return world;
	}

	private static long siteEvents(AgentMind mind) {
		String site = "minecraft:overworld@" + TREE;
		return mind.ledger().events().all().stream()
				.filter(e -> e.type().equals(CanonicalEvent.RESOURCE_COLLECTED) && e.subjectId().equals(site))
				.count();
	}

	@Test
	void harvestsThroughBodyMechanicsAndRecordsVerifiedEvent() {
		FakeWorld world = treeWorld();
		FakeMotor motor = new FakeMotor(world);
		AgentMind mind = mind(world, motor, new Phase1GatePolicy(AGENT_ID, new Vec3i(0, 0, 0), 64, 64));

		run(mind, world, 120);

		assertEquals(1, siteEvents(mind), "exactly one canonical collection for the site");
		assertTrue(world.blocks.get(TREE) == null, "the log was actually removed from the world");
		assertTrue(world.items.getOrDefault("minecraft:oak_log", 0) >= 1, "a real drop reached the inventory");
		assertTrue(mind.ledger().memory().recent(32).stream()
						.anyMatch(e -> e.kind() == EpisodeKind.RESOURCE_COLLECTED && e.where().equals(TREE)),
				"the episode is remembered with its location");
		assertEquals(1, mind.ledger().stats("harvest_tree").successes());
		assertTrue(mind.ledger().evidence().all().stream()
				.anyMatch(r -> r.kind() == dev.ensouled.core.EvidenceKind.GATE && r.outcome().equals("permit")),
				"the action passed a recorded gate decision");
	}

	@Test
	void acknowledgementWithoutWorldChangeIsRejected() {
		FakeWorld world = treeWorld();
		FakeMotor motor = new FakeMotor(world);
		motor.removeBlocks = false;
		AgentMind mind = mind(world, motor, new Phase1GatePolicy(AGENT_ID, new Vec3i(0, 0, 0), 64, 64));

		run(mind, world, 120);

		assertEquals(0, mind.ledger().events().count(CanonicalEvent.RESOURCE_COLLECTED));
		assertTrue(mind.ledger().evidence().all().stream()
				.anyMatch(r -> r.kind() == dev.ensouled.core.EvidenceKind.VERIFY
						&& r.outcome().equals("failed") && r.detail().contains("postcondition_failed")));
		assertEquals(0, mind.ledger().stats("harvest_tree").successes());
	}

	@Test
	void missingDropAfterMiningFailsTheInventoryPostcondition() {
		FakeWorld world = treeWorld();
		FakeMotor motor = new FakeMotor(world);
		motor.giveDrops = false;
		AgentMind mind = mind(world, motor, new Phase1GatePolicy(AGENT_ID, new Vec3i(0, 0, 0), 64, 64));

		run(mind, world, 160);

		assertEquals(0, mind.ledger().events().count(CanonicalEvent.RESOURCE_COLLECTED));
		assertTrue(mind.ledger().evidence().all().stream()
				.anyMatch(r -> r.detail().contains("no_inventory_delta")));
	}

	@Test
	void gateRefusesOutOfScopeActionsRegardlessOfUtility() {
		FakeWorld world = treeWorld();
		Phase1GatePolicy gate = new Phase1GatePolicy(AGENT_ID, new Vec3i(0, 0, 0), 4, 8);

		ActionEnvelope tooFar = envelope("harvest_tree", TREE, "minecraft:oak_log", 1);
		GateDecision far = gate.decide(tooFar, world);
		assertEquals(GateDecision.Verdict.REFUSE, far.verdict());
		assertEquals("outside_home_radius", far.reasonCode());

		ActionEnvelope stone = envelope("harvest_tree", new Vec3i(1, 0, 0), "minecraft:stone", 1);
		world.blocks.put(new Vec3i(1, 0, 0), "minecraft:stone");
		GateDecision notHarvestable = gate.decide(stone, world);
		assertEquals(GateDecision.Verdict.REFUSE, notHarvestable.verdict());
		assertEquals("not_harvestable", notHarvestable.reasonCode());

		ActionEnvelope unpermitted = envelope("dig_tunnel", new Vec3i(1, 0, 0), "minecraft:dirt", 0);
		assertEquals("skill_not_permitted", gate.decide(unpermitted, world).reasonCode());

		ActionEnvelope unknownAgent = new ActionEnvelope(ActionEnvelope.SCHEMA, "agent:other",
				"intent:1", "harvest_tree", TREE, "minecraft:oak_log", "minecraft:overworld", 1, 100, 1);
		assertEquals("identity_mismatch", gate.decide(unknownAgent, world).reasonCode());

		ActionEnvelope expired = new ActionEnvelope(ActionEnvelope.SCHEMA, AGENT_ID, "intent:2",
				"harvest_tree", TREE, "minecraft:oak_log", "minecraft:overworld", 200, 100, 1);
		assertEquals("expired", gate.decide(expired, world).reasonCode());
	}

	@Test
	void budgetExhaustionRefusesFurtherHarvests() {
		FakeWorld world = treeWorld();
		Phase1GatePolicy gate = new Phase1GatePolicy(AGENT_ID, new Vec3i(0, 0, 0), 64, 0);
		assertEquals("budget_exhausted",
				gate.decide(envelope("harvest_tree", TREE, "minecraft:oak_log", 1), world).reasonCode());
	}

	@Test
	void restartRestoresIdentityHistoryAndDoesNotDuplicateTheEvent() {
		FakeWorld world = treeWorld();
		FakeMotor motor = new FakeMotor(world);
		Phase1GatePolicy gate = new Phase1GatePolicy(AGENT_ID, new Vec3i(0, 0, 0), 64, 64);
		AgentMind mind = mind(world, motor, gate);
		run(mind, world, 120);
		assertEquals(1, siteEvents(mind));
		int logsBeforeRestart = world.items.getOrDefault("minecraft:oak_log", 0);

		StateNode saved = MindPersistence.save(mind);

		// Restart: new process, new motor, same world save.
		Phase1GatePolicy restoredGate = new Phase1GatePolicy(AGENT_ID, new Vec3i(0, 0, 0), 64, 64);
		AgentMind restored = MindPersistence.restore(saved, restoredGate, new FakeMotor(world), SkillRegistry.phase1());

		assertEquals(AGENT_ID, restored.identity().agentId(), "identity survives the process");
		assertTrue(restored.ledger().memory().hasCollectedAt(TREE), "autobiographical memory survives");
		assertTrue(restored.ledger().events().exists(CanonicalEvent.RESOURCE_COLLECTED, "minecraft:overworld@" + TREE));

		// The same tree is re-presented (whole column); the being must not re-harvest it, even when
		// the lower trunk is occluded from its raycast.
		world.blocks.put(TREE, "minecraft:oak_log");
		world.blocks.put(TREE.above(), "minecraft:oak_log");
		world.occludeLowerLogs = true;
		run(restored, world, 80);

		assertEquals(1, siteEvents(restored), "no duplicate canonical event for the same site");
		assertEquals(logsBeforeRestart, world.items.getOrDefault("minecraft:oak_log", 0),
				"no second collection of the remembered site");
	}

	@Test
	void reflexPreemptsAnActiveIntention() {
		FakeWorld world = treeWorld();
		FakeMotor motor = new FakeMotor(world);
		AgentMind mind = mind(world, motor, new Phase1GatePolicy(AGENT_ID, new Vec3i(0, 0, 0), 64, 64));
		run(mind, world, 5);

		world.needs = new NeedsState(20, 20, 300, true, false, 0.0);
		run(mind, world, 1);

		assertTrue(mind.reflexEngaged(), "the reflex layer took the body");
		assertTrue(motor.lastCommand.startsWith("move_away_from"), "the body got the hazard escape command");
		assertTrue(mind.currentIntention().isEmpty(), "the ordinary intention was interrupted");
	}

	@Test
	void arbiterKeepsTheIncumbentUnlessChallengerWinsClearly() {
		Arbiter arbiter = new Arbiter();
		Candidate incumbent = new Candidate("a", "harvest_tree", TREE, "minecraft:oak_log",
				0, 0, 500, 0, 0, 0, 0, 0, 0, 1, 1);
		Candidate slight = new Candidate("b", "harvest_tree", new Vec3i(6, 0, 0), "minecraft:oak_log",
				0, 0, 600, 0, 0, 0, 0, 0, 0, 1, 1);
		dev.ensouled.core.Intention live = new dev.ensouled.core.Intention("intent:live", "harvest_tree", TREE,
				"minecraft:oak_log", 0L, 1000L, 1);
		Optional<Candidate> chosen = arbiter.select(java.util.List.of(slight, incumbent), live);
		assertEquals(incumbent.candidateId(), chosen.orElseThrow().candidateId());

		Candidate decisive = new Candidate("c", "harvest_tree", new Vec3i(7, 0, 0), "minecraft:oak_log",
				0, 0, 1400, 0, 0, 0, 0, 0, 0, 1, 1);
		assertEquals("c", arbiter.select(java.util.List.of(decisive), null).orElseThrow().candidateId());
	}

	@Test
	void sensesAreLimitedToThePerceptionRadius() {
		FakeWorld world = new FakeWorld();
		Vec3i far = new Vec3i(40, 0, 0);
		world.blocks.put(far, "minecraft:oak_log");
		FakeMotor motor = new FakeMotor(world);
		AgentMind mind = mind(world, motor, new Phase1GatePolicy(AGENT_ID, new Vec3i(0, 0, 0), 64, 64));

		run(mind, world, 60);

		assertTrue(mind.beliefs().snapshot().stream()
				.noneMatch(b -> b.key().contains(far.toString())), "unseen blocks never enter belief");
		assertEquals(0, mind.ledger().events().count(CanonicalEvent.RESOURCE_COLLECTED));
		assertFalse(world.blocks.isEmpty(), "the unseen block still exists in the world");
	}

	@Test
	void eatingRequiresAFoodItemAndAVerifiedHungerDelta() {
		FakeWorld world = new FakeWorld();
		world.needs = new NeedsState(8, 20, 300, false, false, 0.0);
		world.items.put("minecraft:bread", 1);
		FakeMotor motor = new FakeMotor(world);
		AgentMind mind = mind(world, motor, new Phase1GatePolicy(AGENT_ID, new Vec3i(0, 0, 0), 64, 64));

		run(mind, world, 40);

		assertEquals(0, world.items.getOrDefault("minecraft:bread", 0), "the item was consumed");
		assertTrue(world.needs.food() > 8, "hunger actually changed");
		assertEquals(1, mind.ledger().stats("eat_if_hungry").successes());
	}

	private static ActionEnvelope envelope(String skill, Vec3i target, String blockId, int cost) {
		return new ActionEnvelope(ActionEnvelope.SCHEMA, AGENT_ID, "intent:test", skill, target, blockId,
				"minecraft:overworld", 1, 1000, cost);
	}
}

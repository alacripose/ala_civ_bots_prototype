package dev.ensouled;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.ensouled.core.AgentIdentity;
import dev.ensouled.core.AgentMind;
import dev.ensouled.core.MindPersistence;
import dev.ensouled.core.NeedsState;
import dev.ensouled.core.Phase1GatePolicy;
import dev.ensouled.core.SkillRegistry;
import dev.ensouled.core.Vec3i;

/**
 * The never-stateless baseline: a being with nothing more urgent still chooses something. Resting is
 * a chosen intention with its own evidence, and it is asked for by weariness — a body that is not
 * tired walks instead of sitting down, which is the contract `CandidateGenerator` states. These two
 * checks assert the decision, not the resting, so they keep their point either way.
 */
public class RestDecisionTest {
	private dev.ensouled.persist.MindJournal journal;

	@BeforeEach
	void openJournal() throws java.io.IOException {
		journal = TestJournal.install("rest");
	}

	@AfterEach
	void closeJournal() {
		TestJournal.uninstall(journal);
	}


	private static AgentMind mind(FakeWorld world) {
		AgentIdentity identity = new AgentIdentity("agent:rest-test", "Resty", "", "unspecified", 0L, 0L, "test");
		return new AgentMind(identity, new Phase1GatePolicy("agent:rest-test", new Vec3i(0, 0, 0), 64, 64),
				new FakeMotor(world), SkillRegistry.phase1());
	}

	@Test
	void aBodyWithNothingToDoActsInsteadOfIdling() {
		FakeWorld world = new FakeWorld();
		world.body = new Vec3i(0, 0, 0);
		world.needs = NeedsState.healthy();
		AgentMind mind = mind(world);

		world.tick++;
		mind.tick(world);

		assertTrue(mind.lastDecisionChoices() >= 1, "a body with nothing to do is still offered work");
		assertTrue(mind.currentIntention().isPresent(), "the being chose something instead of being idle");

		for (int i = 0; i < 120; i++) {
			world.tick++;
			mind.tick(world);
		}
		assertTrue(mind.ledger().evidence().all().stream()
				.anyMatch(e -> e.outcome().equals("selected")),
				"the choice itself is recorded as evidence");
	}

	@Test
	void aRestoredBeingStillDecides() {
		FakeWorld world = new FakeWorld();
		world.body = new Vec3i(0, 0, 0);
		world.needs = NeedsState.healthy();
		AgentMind original = mind(world);
		world.tick++;
		original.tick(world);

		AgentMind revived = MindPersistence.restore(MindPersistence.save(original),
				new Phase1GatePolicy("agent:rest-test", new Vec3i(0, 0, 0), 64, 64),
				new FakeMotor(world), SkillRegistry.phase1(),
				dev.ensouled.core.SocialSeam.isolated(),
				new ReasoningHandsStub());
		for (int i = 0; i < 130; i++) {
			world.tick++;
			revived.tick(world);
		}
		assertTrue(revived.ledger().evidence().all().stream()
				.anyMatch(e -> e.outcome().equals("selected")),
				"a restored life keeps deciding after the restart");
	}

	/** A restored mind is handed the same no-object hands the isolated constructor uses. */
	private static final class ReasoningHandsStub implements dev.ensouled.interact.InteractionHands {
		@Override
		public java.util.List<dev.ensouled.interact.InteractionTarget> targets(int radius) {
			return java.util.List.of();
		}

		@Override
		public dev.ensouled.interact.InteractionOutcome perform(
				dev.ensouled.interact.InteractionTarget target,
				dev.ensouled.interact.InteractionRequest request) {
			return dev.ensouled.interact.InteractionOutcome.refused("unsupported_object", "test hands");
		}
	}

	@Test
	void aHungryBodyDoesNotRestInsteadOfSettlingItsNeed() {
		FakeWorld world = new FakeWorld();
		world.body = new Vec3i(0, 0, 0);
		world.needs = new NeedsState(8, 20, 300, false, false, 0.0);
		AgentMind mind = mind(world);

		world.tick++;
		mind.tick(world);

		assertTrue(mind.currentIntention().map(i -> !"rest".equals(i.skillId())).orElse(true),
				"a hungry being's need outranks rest");
	}
}

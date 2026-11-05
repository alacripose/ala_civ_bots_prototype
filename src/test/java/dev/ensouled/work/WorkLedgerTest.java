package dev.ensouled.work;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import dev.ensouled.core.EntityStimulus;
import dev.ensouled.core.InventorySlot;
import dev.ensouled.core.NeedsState;
import dev.ensouled.core.VerbFrame;
import dev.ensouled.core.Vec3i;
import dev.ensouled.core.VisibleBlock;
import dev.ensouled.core.WorldView;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The long-horizon spine on its own: what a wish becomes, what perception does to it, and what a plan
 * replacement may and may not touch. These are the conditions the live server later showed; the test
 * pins them so a change to the rules has to be deliberate.
 */
final class WorkLedgerTest {

	@Test
	void aWishThatNamesAThingAndANumberBecomesWork() {
		Optional<Deliverable> parsed = Deliverable.parse("bring me 4 oak logs");
		assertTrue(parsed.isPresent());
		assertEquals(Deliverable.Kind.LOGS, parsed.get().kind());
		assertEquals(4, parsed.get().count());
	}

	@Test
	void aWishThatNamesOnlyOneHalfIsNotReadAsTheOther() {
		// "some wood" names the thing and not the number: the being must ask, not assume a number.
		assertTrue(Deliverable.parse("bring me some wood").isEmpty());
		assertEquals(Optional.of(Deliverable.Kind.LOGS), Deliverable.named("bring me some wood"));
		assertTrue(Deliverable.parse("bring me 3 moonrocks").isEmpty());
		assertEquals(Optional.empty(), Deliverable.named("bring me 3 moonrocks"));
	}

	@Test
	void aWishForSomethingTheWorldCannotSupplyIsHeldAsAnAspiration(@TempDir Path dir) {
		WorkLedger work = WorkLedger.at(dir.resolve("work.json"));
		// The parser can read this wish, but no learned or visible target supplies moonrock. The
		// frame is supplied directly because a unit test has no live WorldState to record one.
		VerbFrame frame = new VerbFrame("frame:aspiration", 900L, "wish", "Nia", "bring",
				VerbFrame.State.NAMED, "moonrock", 3, "", "", "", List.of(),
				"bring me 3 moonrocks", 0, 18);
		WorkLedger.Answer answer = work.offer("agent:9", "Nia", "wish:9", "bring me 3 moonrocks",
				900L, new Stub(900L), frame);
		assertEquals("wish-aspiration", answer.code(), answer.why());
		assertFalse(answer.opened(), "an aspiration is not a commitment");
		assertTrue(work.commitmentsOf("agent:9").isEmpty(),
				"a wish with no target must not open work nobody can do");
	}

	@Test
	void aDecisionSpendsTheWishCellAndIsNotRebilledAsDiscovery(@TempDir Path dir) {
		WorkLedger work = WorkLedger.at(dir.resolve("work.json"));
		Stub view = new Stub(700L);
		work.offer("agent:9", "Nia", "wish:9", "bring me 4 oak logs", 700L, view);

		Ticket before = work.activeTicket("agent:9").orElseThrow();
		assertEquals(0, before.usedCells());
		assertEquals(0, work.cell("agent:9", "intent:7", "harvest_tree", 710L));
		Ticket spent = work.activeTicket("agent:9").orElseThrow();
		assertEquals(1, spent.usedCells());
		assertEquals(List.of("intent:7"), spent.evidence());

		view.slots.add(new InventorySlot(0, "minecraft:oak_log", 4));
		work.observe("agent:9", "Nia", view, 720L);
		work.discovered("agent:9", "Nia", "harvest_tree", 730L, true, "intent:7");
		assertTrue(work.discoveriesOf("agent:9").isEmpty(),
				"the decision is already on the wish ticket, not a fresh discovery");
	}

	@Test
	void aCommitmentAdvancesItsStagesOnlyOnWhatTheBodyCanSeeAndHold(@TempDir Path dir) {
		WorkLedger work = WorkLedger.at(dir.resolve("work.json"));
		Stub view = new Stub(100L);
		WorkLedger.Answer answer = work.offer("agent:1", "Ash", "wish:1", "bring me 4 oak logs",
				100L, view);
		assertTrue(answer.opened(), answer.why());
		assertEquals(4, answer.count());

		// Nothing in sight and nothing in hand: no stage has been reached.
		work.observe("agent:1", "Ash", view, 120L);
		assertEquals(0, stage(work, "agent:1"));

		// One log in sight: SURVEY. It has not been taken yet, so CLAIM is not claimed.
		view.sight.add(new VisibleBlock(new Vec3i(1, 64, 1), "minecraft:oak_log", 0.8, true, true));
		work.observe("agent:1", "Ash", view, 140L);
		assertEquals(1, stage(work, "agent:1"));

		// One log in the inventory: CLAIM, and not a stage further.
		view.slots.add(new InventorySlot(0, "minecraft:oak_log", 1));
		work.observe("agent:1", "Ash", view, 160L);
		assertEquals(2, stage(work, "agent:1"));

		// All four in hand: the condition is met, the window is met, and the run reaches ESTABLISH.
		view.slots.clear();
		view.slots.add(new InventorySlot(0, "minecraft:oak_log", 4));
		work.observe("agent:1", "Ash", view, 180L);
		assertEquals(3, stage(work, "agent:1"));
		assertTrue(work.describe().contains("SATISFIED"), work.describe());
		assertTrue(work.describe().contains("held >= 4"), work.describe());
	}

	@Test
	void aRunReachesOneStageAtATimeSoNoneIsSkipped(@TempDir Path dir) {
		WorkLedger work = WorkLedger.at(dir.resolve("work.json"));
		Stub view = new Stub(500L);
		// It already carries one of the thing it owes, so CLAIM's condition holds from the first
		// tick. That must not carry it past SURVEY: a stage with no row is a stage never reached.
		view.slots.add(new InventorySlot(0, "minecraft:oak_log", 1));
		work.offer("agent:7", "Cass", "wish:7", "bring me 4 oak logs", 500L, view);
		work.observe("agent:7", "Cass", view, 520L);
		assertEquals(0, stage(work, "agent:7"), "holding one is not a survey");

		view.sight.add(new VisibleBlock(new Vec3i(3, 64, 3), "minecraft:oak_log", 0.9, true, true));
		work.observe("agent:7", "Cass", view, 540L);
		assertEquals(1, stage(work, "agent:7"), "the log in sight is the survey");

		work.observe("agent:7", "Cass", view, 560L);
		assertEquals(2, stage(work, "agent:7"), "the log in hand is the claim, on the next step");
	}

	@Test
	void aPlanIsReplacedButTheCommitmentOutlivesIt(@TempDir Path dir) {
		WorkLedger work = WorkLedger.at(dir.resolve("work.json"));
		Stub view = new Stub(1000L);
		work.offer("agent:2", "Bram", "wish:2", "bring me 4 oak logs", 1000L, view);
		assertTrue(work.describe().contains("step=harvest_tree"), work.describe());

		// Nothing of that kind in sight for the patience window: the step changes, the debt does not.
		work.observe("agent:2", "Bram", view, 1000L + WorkLedger.STEP_PATIENCE_TICKS + 20);
		assertTrue(work.describe().contains("step=wander"), work.describe());
		assertTrue(work.describe().contains("nothing of that kind in sight"), work.describe());
		assertTrue(work.describe().contains("owes 4 logs"), work.describe());

		// In sight again: back to the step that serves it.
		view.sight.add(new VisibleBlock(new Vec3i(2, 64, 2), "minecraft:oak_log", 0.9, true, true));
		work.observe("agent:2", "Bram", view, 1000L + WorkLedger.STEP_PATIENCE_TICKS + 60);
		assertTrue(work.describe().contains("step=harvest_tree"), work.describe());
	}

	@Test
	void anExpiredWindowSaysSoAndNothingPretendsItWasMet(@TempDir Path dir) {
		WorkLedger work = WorkLedger.at(dir.resolve("work.json"));
		Stub view = new Stub(0L);
		work.offer("agent:3", "Resty", "wish:3", "bring me 2 bread", 0L, view);
		work.observe("agent:3", "Resty", view, WorkLedger.WINDOW_TICKS + 1);
		assertTrue(work.describe().contains("EXPIRED"), work.describe());
		assertTrue(work.describe().contains("window closed"), work.describe());
	}

	@Test
	void aStepTheBeingOwesIsWorthMoreThanOneItDoesNot(@TempDir Path dir) {
		WorkLedger work = WorkLedger.at(dir.resolve("work.json"));
		work.offer("agent:4", "Ham", "wish:4", "bring me 4 oak logs", 0L, new Stub(0L));
		assertEquals(WorkLedger.PULL, work.pull("agent:4", "harvest_tree"));
		assertEquals(0, work.pull("agent:4", "wander"));
		assertEquals(0, work.pull("agent:5", "harvest_tree"));
	}

	@Test
	void aCommitmentSurvivesTheLedgerBeingReopened(@TempDir Path dir) {
		Path file = dir.resolve("work.json");
		WorkLedger work = WorkLedger.at(file);
		work.offer("agent:6", "Jeff", "wish:6", "bring me 4 oak logs", 500L, new Stub(500L));
		// A restart: the file is the only thing that survives.
		WorkLedger reopened = WorkLedger.at(file);
		List<Commitment> owed = reopened.commitmentsOf("agent:6");
		assertEquals(1, owed.size());
		assertEquals(4, owed.get(0).deliverable().count());
		assertTrue(owed.get(0).open(), "a commitment is still owed after the process that made it ends");
	}

	@Test
	void aFailedFirstPracticeIsReplacedByTheDiscoveryThatSucceeds(@TempDir Path dir) {
		WorkLedger work = WorkLedger.at(dir.resolve("work.json"));
		work.discovered("agent:8", "Ivy", "harvest_tree", 10L, false, "gate:cand:1");
		assertEquals(Ticket.State.REFUSED,
				work.discoveriesOf("agent:8").get("harvest_tree").state());

		work.discovered("agent:8", "Ivy", "harvest_tree", 20L, true, "intent:2");
		Ticket discovered = work.discoveriesOf("agent:8").get("harvest_tree");
		assertEquals(Ticket.State.HELD, discovered.state(),
				"the later success is the witnessed unlock, not the refused first try");
		assertEquals(20L, discovered.closedTick());
		assertTrue(discovered.witness().contains("held 1/1"), discovered.witness());
	}

	private static int stage(WorkLedger work, String agentId) {
		List<Commitment> owed = work.commitmentsOf(agentId);
		return owed.isEmpty() ? -1 : owed.get(owed.size() - 1).stageReached();
	}

	/** The smallest world a commitment can be judged in: what is in sight and what is in hand. */
	private static final class Stub implements WorldView {
		private final long tick;
		private final List<VisibleBlock> sight = new ArrayList<>();
		private final List<InventorySlot> slots = new ArrayList<>();

		Stub(long tick) {
			this.tick = tick;
		}

		@Override
		public long tick() {
			return tick;
		}

		@Override
		public Vec3i bodyPos() {
			return new Vec3i(0, 64, 0);
		}

		@Override
		public String dimensionId() {
			return "minecraft:overworld";
		}

		@Override
		public NeedsState needs() {
			return NeedsState.healthy();
		}

		@Override
		public List<VisibleBlock> visibleBlocks(int radius) {
			return List.copyOf(sight);
		}

		@Override
		public Optional<VisibleBlock> blockAt(Vec3i pos) {
			return sight.stream().filter(block -> block.pos().equals(pos)).findFirst();
		}

		@Override
		public boolean canReach(Vec3i pos) {
			return true;
		}

		@Override
		public boolean isStandable(Vec3i pos) {
			return true;
		}

		@Override
		public Optional<Vec3i> nearestStandingNear(Vec3i target, int radius) {
			return Optional.of(target);
		}

		@Override
		public List<InventorySlot> inventory() {
			return List.copyOf(slots);
		}

		@Override
		public int countItem(String itemId) {
			int total = 0;
			for (InventorySlot slot : slots) {
				if (slot.itemId().equals(itemId)) {
					total += slot.count();
				}
			}
			return total;
		}

		@Override
		public List<EntityStimulus> entities(int radius) {
			return List.of();
		}

		@Override
		public Vec3i perceptionAnchor() {
			return bodyPos();
		}
	}
}

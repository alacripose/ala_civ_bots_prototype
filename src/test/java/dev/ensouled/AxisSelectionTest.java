package dev.ensouled;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

import dev.ensouled.axis.AxisGate;
import dev.ensouled.axis.AxisVocabulary;
import dev.ensouled.axis.DecisionPackage;
import dev.ensouled.core.Arbiter;
import dev.ensouled.core.AxisReadings;
import dev.ensouled.core.Candidate;
import dev.ensouled.core.MessageEnvelope;
import dev.ensouled.core.ObservationPackage;
import dev.ensouled.core.ProseLines;
import dev.ensouled.core.ProseLogic;
import dev.ensouled.core.Vec3i;

/**
 * The two claims this pass has to hold up, as checks rather than as prose.
 *
 * <p>First: the axis reading is part of how a candidate is chosen. The counterfactual is the whole
 * test — the same candidates, the same utilities, the same world, and only the record behind each
 * action changed. If the chosen action is the same either way, the reading is decoration; the
 * assertions below fail when it is.
 *
 * <p>Second: the lane a being reads carries language. A row whose utterance is a machine token is
 * refused by the same baseline that decides whether a being may speak.
 */
class AxisSelectionTest {

	/** One candidate whose utility is exactly the need relief it offers, so the arithmetic is visible. */
	private static Candidate candidate(String skill, int utility) {
		return new Candidate(skill + "#1", skill, new Vec3i(4, 0, 0), "minecraft:oak_log", utility, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0L);
	}

	private static Map<String, Integer> ranks(boolean gatherPerceived, boolean gatherTaken,
			boolean restPerceived, boolean restTaken) {
		Map<String, Integer> ranks = new LinkedHashMap<>();
		ranks.put("gather_wood#1", AxisGate.rank(
				AxisReadings.establishment(gatherPerceived, gatherTaken)));
		ranks.put("rest#1", AxisGate.rank(AxisReadings.establishment(restPerceived, restTaken)));
		return ranks;
	}

	@Test
	void theSameWorldWithTwoRecordsChoosesTwoDifferentActions() {
		Candidate gather = candidate("gather_wood", 900);
		Candidate rest = candidate("rest", 700);
		List<Candidate> candidates = List.of(gather, rest);
		Arbiter arbiter = new Arbiter();

		// Reading one: the tree is in sight, so gathering stands on an observation.
		String first = arbiter.select(candidates, null, ranks(true, false, false, false)).orElseThrow()
				.skillId();
		assertEquals("gather_wood", first,
				"the selection that stands on something in sight is chosen over an unchecked one");

		// Reading two: the same candidates and utilities, and the reading swapped — memory says the
		// site was already taken, while the other target is in front of the body.
		String second = arbiter.select(candidates, null, ranks(false, true, true, false)).orElseThrow()
				.skillId();
		assertEquals("rest", second,
				"a stale site must hand the body to the selection that still stands");
		assertNotEquals(first, second,
				"the reading must change the decision, not annotate it after the fact");
	}

	@Test
	void withEqualReadingsUtilityStillDecides() {
		Candidate gather = candidate("gather_wood", 900);
		Candidate rest = candidate("rest", 700);
		String chosen = new Arbiter().select(List.of(gather, rest), null,
				ranks(false, false, false, false)).orElseThrow().skillId();
		assertEquals("gather_wood", chosen, "no record either way is a tie, and utility breaks it");
	}

	@Test
	void aReadingWithNothingInSightIsUncheckedAndNotAPass() {
		assertEquals(AxisReadings.UNCHECKED, AxisReadings.establishment(false, false));
		assertEquals(AxisReadings.ACTIVE, AxisReadings.establishment(true, false));
		assertEquals(AxisReadings.STALE, AxisReadings.establishment(true, true));
		assertTrue(AxisGate.rank(AxisReadings.ACTIVE) > AxisGate.rank(AxisReadings.STALE));
		assertTrue(AxisGate.rank(AxisReadings.STALE) > AxisGate.rank(AxisReadings.UNCHECKED));
		assertTrue(AxisGate.rank(AxisReadings.UNCHECKED) > AxisGate.rank(AxisReadings.REFUSED));
	}

	@Test
	void aDecisionRowSpeaksProseAndKeepsItsTokenInThePayload() {
		AxisGate.Selection selection = new AxisGate.Selection("need-relief", 900, "harvest_tree", true,
				true, 1, -1);
		AxisGate.Reading reading = AxisGate.read("harvest_tree@4,0,0", selection, "ev:41", "active");
		JsonObject pkg = DecisionPackage.of("Ash", 120L, "harvest_tree", "harvest_tree@4,0,0",
				reading.decision(), "intent:7", 100L, new Vec3i(4, 0, 0), "minecraft:overworld",
				reading.establishment());

		String utterance = MessageEnvelope.utterance(pkg);
		assertTrue(ProseLogic.baseline().classify(utterance).isPresent(),
				"the lane must hand a reader a claim, not a row: " + utterance);
		assertFalse(utterance.contains("@"), "a token is not an utterance: " + utterance);

		JsonObject payload = MessageEnvelope.payload(pkg);
		assertEquals("procedural_gate PASS",
				AxisVocabulary.check(AxisVocabulary.Kind.PROCEDURAL_GATE, payload.get("token").getAsString()) == null
						? payload.get("token").getAsString()
						: "unqualified");
		assertEquals("establishment active", payload.get("establishment").getAsString());
		assertTrue(payload.has("proof"), "the ticking proof travels with the typed payload");
	}

	@Test
	void aTokenInTheUtteranceSlotIsRefusedByName() {
		assertThrows(IllegalArgumentException.class, () -> MessageEnvelope.self("Ash", "obs:Ash",
				"REPORT", "Ash: harvest_tree@4,0,0", 1L, null, null, new JsonObject()));
		assertThrows(IllegalArgumentException.class,
				() -> ProseLines.compose("Ash", "HARVEST", "a tree at 4,0,0"));
	}

	@Test
	void anObservationCarriesItsCountsAsDataAndItsLineAsProse() {
		FakeWorld world = new FakeWorld();
		Vec3i tree = new Vec3i(2, 0, 0);
		world.blocks.put(tree, "minecraft:oak_log");
		world.blocks.put(tree.above(), "minecraft:oak_log");
		List<dev.ensouled.core.VisibleBlock> seen = List.of(
				new dev.ensouled.core.VisibleBlock(tree, "minecraft:oak_log", 2.0, true, true),
				new dev.ensouled.core.VisibleBlock(tree.above(), "minecraft:oak_log", 2.5, true, true));

		JsonObject pkg = ObservationPackage.of("Ash", world, seen, "genesis");
		String utterance = MessageEnvelope.utterance(pkg);
		assertTrue(ProseLogic.baseline().classify(utterance).isPresent(), utterance);
		assertFalse(utterance.contains("\"counts\""), "JSON in the utterance slot is the flood: " + utterance);

		JsonObject payload = MessageEnvelope.payload(pkg);
		assertEquals(2, payload.getAsJsonObject("counts").get("blocks").getAsInt());
		assertEquals(2, payload.getAsJsonObject("counts").get("logs").getAsInt());
		assertTrue(payload.get("digest").getAsString().startsWith("sha256:"));
	}
}

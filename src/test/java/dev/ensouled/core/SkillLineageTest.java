package dev.ensouled.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

class SkillLineageTest {
	@Test
	void oneVerbIsOneNodeWithItsFirstPracticeAndTicket() {
		MindLedger ledger = new MindLedger();
		ledger.practiced("harvest_tree", 10L, true, "intent:1");
		ledger.practiced("harvest_tree", 20L, true, "intent:2");
		ledger.practiced("harvest_tree", 30L, false, "target_occluded");

		SkillLineage lineage = SkillLineage.of(ledger.practice(), Map.of("harvest_tree",
				new SkillLineage.Discovery("ticket:1", "HELD", "held 1/1 at tick 10")), 3);

		assertEquals(3, lineage.sessions());
		assertEquals(1, lineage.nodes().size(), "two routines speaking one verb are one ability");
		SkillLineage.Node node = lineage.nodes().get(0);
		assertEquals("harvest", node.verb());
		assertTrue(node.basis().startsWith("lexicon:"), node.basis());
		assertEquals("harvest_tree", node.firstSkill());
		assertEquals(10L, node.firstTick());
		assertEquals(30L, node.lastTick());
		assertEquals(2, node.successes());
		assertEquals(1, node.failures());
		assertEquals(266, node.habitStrength());
		assertTrue(node.witnessed());
		assertEquals("ticket:1", node.discovery().ticketId());
		assertEquals("HELD", node.discovery().state());
	}

	@Test
	void aVerbTheCorpusDoesNotAttestKeepsItsOwnName() {
		MindLedger ledger = new MindLedger();
		ledger.practiced("unheardof_thing", 5L, true, "intent:1");

		SkillLineage lineage = SkillLineage.of(ledger.practice(), Map.of(), 1);

		SkillLineage.Node node = lineage.nodes().get(0);
		assertEquals("unheardof_thing", node.verb());
		assertEquals("unattested", node.basis());
		assertFalse(node.witnessed());
	}

	@Test
	void noPracticeMeansNoNodeRatherThanAZeroRow() {
		SkillLineage lineage = SkillLineage.of(new MindLedger().practice(), Map.of(), 0);

		assertTrue(lineage.isEmpty());
		assertTrue(lineage.lines().isEmpty());
	}
}

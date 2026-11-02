package dev.ensouled.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The practice log is the record; the counters are a replay of it, keyed by the learned verb. */
class PracticeLogTest {

	@Test
	void countersAreAFunctionOfThePracticeRows() {
		MindLedger first = new MindLedger();
		first.practiced("harvest_tree", 10L, true, "intent:1");
		first.practiced("harvest_tree", 20L, true, "intent:2");
		first.practiced("harvest_tree", 30L, false, "target_occluded");

		MindLedger restored = new MindLedger();
		restored.restorePractice(first.practice());

		assertEquals(2, restored.stats("harvest_tree").successes());
		assertEquals(1, restored.stats("harvest_tree").failures());
		assertEquals(2, restored.statsBySkill().get("harvest").successes(),
				"the learned unit is the corpus verb, not the routine id");
	}

	@Test
	void aReplayReplacesRatherThanAddsToTheCache() {
		MindLedger ledger = new MindLedger();
		ledger.practiced("wander", 1L, true, "intent:1");
		ledger.restorePractice(List.of(new MindLedger.Practice(5L, "rest", "rest", false, "intent:9")));

		assertEquals(0, ledger.stats("wander").successes(), "the replayed log is the whole record");
		assertEquals(1, ledger.stats("rest").failures());
		assertEquals(1, ledger.practice().size());
	}

	@Test
	void aStoredVerbOutlivesItsRoutineName() {
		MindLedger ledger = new MindLedger();
		ledger.restorePractice(List.of(new MindLedger.Practice(7L, "old_skill_gone", "harvest", true, "intent:3")));

		assertTrue(ledger.statsBySkill().containsKey("harvest"));
		assertEquals(1, ledger.stats("harvest_tree").successes(),
				"a routine that speaks harvest reads the skill the log recorded");
	}
}

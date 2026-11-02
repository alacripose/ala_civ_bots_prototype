package dev.ensouled.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Speech is not composed from a speech-act menu: it is selected from the claims the world ingested
 * from the prose-logic corpus, and the words of the heard line decide which claim answers.
 */
class ClaimSpeechTest {
	private final ClaimCorpus corpus = ClaimCorpus.corpus();

	@Test
	void theCorpusWasIngested() {
		assertTrue(corpus.size() > 1000, "ingested claims: " + corpus.size() + " from " + corpus.source());
		assertEquals("resources/ensouled/prose_claims.tsv", corpus.source());
	}

	@Test
	void everyIngestedClaimCarriesABaselineVerb() {
		List<ClaimCorpus.Hit> hits = corpus.relevant("wood and stone and water", 8);
		assertFalse(hits.isEmpty(), "the corpus states claims about the things a body handles");
		for (ClaimCorpus.Hit hit : hits) {
			assertTrue(ProseLogic.baseline().pair(hit.claim().verb()).isPresent(),
					hit.claim().verb() + " is a baseline verb");
		}
	}

	@Test
	void theHeardLineDecidesWhichClaimAnswers() {
		ClaimCorpus.Hit top = corpus.relevant("bring the wood to the fire", 1).get(0);
		assertFalse(top.shared().isEmpty());
		String spoken = top.claim().spoken().toLowerCase(java.util.Locale.ROOT);
		for (String word : top.shared()) {
			assertTrue(spoken.contains(word), "the answer shares \"" + word + "\": " + spoken);
		}
		// The selected row is a corpus claim, admitted by the same baseline every other line passes.
		assertTrue(ProseLogic.baseline().classify(top.claim().rule()).isPresent());
	}

	@Test
	void aLineWithNothingInCommonHasNothingToSay() {
		assertTrue(corpus.relevant("zzzqqq xxyyzz", 8).isEmpty());
	}
}

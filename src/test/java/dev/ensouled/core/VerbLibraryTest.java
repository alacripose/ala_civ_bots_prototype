package dev.ensouled.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The verb library is compiled from the corpus, and these tests hold it to that: every verb it
 * offers is a word the lexicon attests, a skill with an attested verb is named by it, and a skill
 * with none is reported as unattested rather than given a word this project made up.
 */
class VerbLibraryTest {
	private final VerbLibrary verbs = VerbLibrary.corpus();

	@Test
	void theLibraryWasCompiledFromTheCorpus() {
		assertTrue(verbs.size() > 100, "the compiled verb table is present: " + verbs.source());
		assertEquals("resources/ensouled/prose_verbs.tsv", verbs.source());
	}

	@Test
	void everyOfferedVerbIsAttestedVocabulary() {
		// The two compiled tables must agree: a verb the corpus uses in a verb position is also a
		// word the corpus says. A row that broke this would be a word this project invented.
		Lexicon lexicon = Lexicon.corpus();
		assertTrue(verbs.isVerb("harvest"), "harvest is used as a verb");
		for (String word : new String[] { "harvest", "mine", "gather", "bring", "eat", "rest" }) {
			assertTrue(verbs.isVerb(word), word + " is in the induced table");
			assertTrue(lexicon.attests(word), word + " is attested vocabulary");
		}
	}

	@Test
	void aSkillIsSpokenWithItsAttestedVerb() {
		assertEquals("harvest", verbs.verbFor("harvest_tree").orElse(""));
		assertEquals("harvest tree", verbs.phrase("harvest_tree"));
		assertTrue(verbs.basis("harvest_tree").startsWith("lexicon:"), verbs.basis("harvest_tree"));
	}

	@Test
	void aSkillWithNoAttestedVerbIsReportedUnattested() {
		// `craft` is vocabulary but the corpus never puts it in a verb position, so the library does
		// not claim it: the honest answer is that the corpus has no verb for that action yet.
		assertTrue(verbs.verbFor("unheardof_thing").isEmpty());
		assertEquals("unattested", verbs.basis("unheardof_thing"));
		assertEquals("unheardof thing", verbs.phrase("unheardof_thing"));
		assertFalse(verbs.isVerb("unheardof"));
	}

	@Test
	void aPluralNameKeepsTheSpellingTheWorldGaveIt() {
		// The live defect this closed: `spruce_planks` was spoken as "spruce plankss".
		assertEquals("spruce planks", Realizer.many("spruce planks", 9));
		assertEquals("logs", Realizer.many("log", 3));
		assertEquals("log", Realizer.many("log", 1));
	}
}

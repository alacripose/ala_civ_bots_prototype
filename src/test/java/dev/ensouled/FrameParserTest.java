package dev.ensouled;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.ensouled.core.FrameParser;
import dev.ensouled.core.VerbFrame;
import org.junit.jupiter.api.Test;

/**
 * The parser is judged on lines the world actually carried, not on invented ones: these utterances
 * are taken from {@code run/world/ensouled/chat/transcript.jsonl} and from the being's own speech.
 */
final class FrameParserTest {
	@Test
	void aSelfReportKeepsItsQuantityAndItsUnparsedTail() {
		VerbFrame frame = FrameParser.parse(29135L, "speech", "Ash", "I have 4 oak planks and 20 food.", 0);
		// The actor is who the frame is attributed to, not the pronoun in the sentence.
		assertEquals("Ash", frame.actor());
		assertEquals("have", frame.verb());
		assertEquals(VerbFrame.State.NAMED, frame.verbState());
		assertEquals("oak planks", frame.objectKind());
		assertEquals(4, frame.objectCount());
		// The second claim is not silently folded into the first, and not silently dropped either.
		assertTrue(frame.residue().contains("food"), frame.residue().toString());
		assertTrue(frame.residue().contains("and"), frame.residue().toString());
	}

	@Test
	void aQuestionIsAddressedToThePersonItNames() {
		VerbFrame frame = FrameParser.parse(25353L, "speech", "Bramble", "do you have 8 oak logs?", 1);
		assertEquals("have", frame.verb());
		assertEquals(8, frame.objectCount());
		assertEquals("oak logs", frame.objectKind());
		assertEquals("you", frame.recipient());
		assertEquals("may", frame.modality());
	}

	@Test
	void aNeedIsAFrameWithoutAQuantity() {
		VerbFrame frame = FrameParser.parse(25353L, "speech", "Ash", "I need wood.", 2);
		assertEquals("need", frame.verb());
		assertEquals("wood", frame.objectKind());
		// Unstated is not zero: nothing was said about how much.
		assertNull(frame.objectCount());
		assertTrue(frame.residue().isEmpty(), frame.residue().toString());
	}

	@Test
	void aVerbTheWorldCannotPerformIsNamedRatherThanRefused() {
		VerbFrame frame = FrameParser.parse(25353L, "wish", "Ash", "I press the cogwheel.", 3);
		assertEquals("press", frame.verb());
		assertEquals("cogwheel", frame.objectKind());
		// Attested vocabulary with no claim shape: it can be meant, not asserted.
		assertEquals(VerbFrame.State.NAMED, frame.verbState());
		assertTrue(frame.actionable());
	}

	@Test
	void aWishWithTwoConditionalCuesIsStatedInWhen() {
		// A live wish: "ensouled wish Ash if someone comes while the sun is low, wake the others".
		// The corpus says "if" and "someone" far more often in WHEN claims than anywhere else, and
		// that measured lift is what decides the shape.
		String text = "if someone comes while the sun is low, wake the others";
		VerbFrame frame = FrameParser.parse(99000L, "wish", "Rcon", text, 0);
		assertEquals("WHEN", frame.claimShape());
		assertEquals("cues:if,someone", FrameParser.shapeBasis(text, frame.claimShape()));
		// A subordinator opens the clause; it is grammar and never the verb.
		assertEquals("comes", frame.verb());
	}

	@Test
	void aShapeMustOutweighTheOtherShapesItAlsoCues() {
		// Same live wish shape, different wording: REQUIRE cues (you, must, leave) outweigh FORBID
		// (not, you) by more than the margin, so the frame states REQUIRE rather than nothing.
		String text = "you must not leave the base when the night comes";
		VerbFrame frame = FrameParser.parse(99001L, "wish", "Rcon", text, 0);
		assertEquals("REQUIRE", frame.claimShape());
		assertEquals("must", frame.modality());
	}

	@Test
	void oneCueWordIsNotAShape() {
		// "when" on its own is not enough: the sentence mentions a condition, it is not stated as
		// one. Unclaimed is a reading, not a failure to read.
		VerbFrame frame = FrameParser.parse(99002L, "chat", "alacripose", "I need wood when it is cold.", 0);
		assertEquals("", frame.claimShape());
		assertEquals("none", FrameParser.shapeBasis("I need wood when it is cold.", frame.claimShape()));
	}

	@Test
	void anOrdinarySelfReportCarriesNoClaimShape() {
		// Corpus-voiced speech is the intended language, and most of it is not a claim in any of
		// the corpus's shapes. The frame says so instead of assigning one.
		VerbFrame frame = FrameParser.parse(99003L, "speech", "Ash", "I have 0 oak logs and 20 food.", 0);
		assertEquals("", frame.claimShape());
	}

	@Test
	void aWordOutsideTheVocabularyIsStillRecorded() {
		// "smelt" used to stand here as the unattested word. It is attested now: the mod source trees
		// were ingested, and they say it. The contract was never that one particular word is missing;
		// it is that a word nothing attests is recorded and named rather than dropped.
		VerbFrame frame = FrameParser.parse(25353L, "wish", "Ash", "I flange the cogwheel.", 4);
		assertEquals("flange", frame.verb());
		assertEquals(VerbFrame.State.UNKNOWN, frame.verbState());
		assertFalse(frame.actionable());
		// The frame exists and is attributable: an unknown word is evidence, not nothing.
		assertTrue(frame.json().get("evidence").getAsString().startsWith("sha256:"));
		assertEquals("ensouled.frame/1", frame.json().get("schema").getAsString());
	}
}

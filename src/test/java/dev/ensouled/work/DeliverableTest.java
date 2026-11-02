package dev.ensouled.work;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A build wish and a gather wish name the same kind and count, and ask for opposite work. */
class DeliverableTest {

	@Test
	void buildWordsChooseConstruction() {
		assertTrue(Deliverable.builds("build 4 dirt here"));
		assertTrue(Deliverable.builds("place 2 planks"));
		assertTrue(Deliverable.builds("raise a wall of 6 dirt"));
	}

	@Test
	void aGatherWishIsNotABuildWish() {
		assertFalse(Deliverable.builds("bring me 4 dirt"));
		assertFalse(Deliverable.builds("please gather 3 oak logs for me"));
		assertFalse(Deliverable.builds(null));
	}

	@Test
	void theSameWordsStillParseToTheSameDeliverable() {
		assertEquals(new Deliverable(Deliverable.Kind.DIRT, 4),
				Deliverable.parse("build 4 dirt here").orElseThrow());
		assertEquals(new Deliverable(Deliverable.Kind.PLANKS, 2),
				Deliverable.parse("place 2 planks").orElseThrow());
	}
}

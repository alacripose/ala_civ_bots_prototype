package dev.ensouled.work;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The ticket's own rules: declared cells are bounded, states are named, the digest covers the claim. */
class TicketTest {

	private Ticket opened() {
		return Ticket.opened("ticket:1", "commit:1", "world:1", "agent:1", "Ash", "wish:wish:1",
				"harvest", "lexicon:4", 2, 100L, 700L);
	}

	@Test
	void aCellIsSpentOnceAndTheBudgetIsABound() {
		Ticket ticket = opened().withCell("intent:1").withCell("intent:2");
		assertEquals(2, ticket.usedCells());
		assertFalse(ticket.cellAvailable(), "a spent budget is not available");
		assertEquals(List.of("intent:1", "intent:2"), ticket.evidence());
	}

	@Test
	void closingOnObservedEvidenceNamesHeld() {
		Ticket held = opened().withCell("intent:1").closed(Ticket.State.HELD, 220L, "held 9/9",
				"held 9/9 at tick 220");
		assertEquals(Ticket.State.HELD, held.state());
		assertFalse(held.open());
		assertEquals("held 9/9 at tick 220", held.witness());
		assertEquals(220L, held.closedTick());
	}

	@Test
	void absentAndRefusedAreDifferentVerdicts() {
		assertEquals(Ticket.State.ABSENT, opened().closed(Ticket.State.ABSENT, 700L, "window closed", "-").state());
		assertEquals(Ticket.State.REFUSED, opened().closed(Ticket.State.REFUSED, 400L, "superseded", "-").state());
	}

	@Test
	void theDigestChangesWhenTheClaimChanges() {
		Ticket plain = opened();
		Ticket spent = plain.withCell("intent:1");
		Ticket held = spent.closed(Ticket.State.HELD, 300L, "held 1/9", "held 1/9 at tick 300");
		assertNotEquals(plain.digest(), spent.digest(), "a spent cell is part of the claim");
		assertNotEquals(spent.digest(), held.digest(), "a verdict is part of the claim");
		assertTrue(held.digest().matches("^sha256:[0-9a-f]{64}$"));
	}

	@Test
	void jsonCarriesTheSameFactsTheRecordHolds() {
		Ticket held = opened().withCell("intent:1").closed(Ticket.State.HELD, 300L, "held 1/9",
				"held 1/9 at tick 300");
		var json = held.json();
		assertEquals(Ticket.FORMAT, json.get("format").getAsString());
		assertEquals("HELD", json.get("state").getAsString());
		assertEquals(2, json.getAsJsonObject("cells").get("declared").getAsInt());
		assertEquals(1, json.getAsJsonObject("cells").get("used").getAsInt());
		assertEquals(held.digest(), json.get("digest").getAsString());
		assertEquals(held.artifact(), json.get("artifact").getAsString());
		assertEquals(1, json.getAsJsonArray("evidence").size());
	}
}

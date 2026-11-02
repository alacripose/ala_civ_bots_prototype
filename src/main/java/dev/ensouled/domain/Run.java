package dev.ensouled.domain;

/**
 * A bounded operation inside a world — a survey, a founding expedition, a supply campaign (§A4.1,
 * §A7). A run ends without ending the world; its stages are recorded one transition at a time so a
 * resumed run can say what it had already achieved.
 *
 * <p>The stage list is the one §A4.1 names for a founding run, plus the two states every run needs:
 * {@link Stage#CONSIDERED} when the being has taken the operation up, and {@link Stage#ABANDONED} when
 * it has put it down. Stages are recorded, not enforced by a state machine here: a being's own
 * decision moves a run, and this record is the evidence of where it got to.
 */
public record Run(String runId, String worldId, String agentId, String purpose, Stage stage,
		long openedTick, long closedTick) {

	public enum Stage {
		/** Taken up by the being; nothing done yet. */
		CONSIDERED,
		/** What the place offers has been looked at. */
		SURVEY,
		/** A site or resource is claimed. */
		CLAIM,
		/** The minimum for the operation stands. */
		ESTABLISH,
		/** The operation survives its first test. */
		SURVIVE,
		/** At least one repeatable supply or source exists. */
		BOOTSTRAP,
		/** Connected to a base, or deliberately isolated. */
		CONNECT,
		/** Registered and saved. */
		COMMIT,
		/** Put down without completing. */
		ABANDONED
	}

	public Run advance(Stage next, long tick) {
		boolean closing = next == Stage.COMMIT || next == Stage.ABANDONED;
		return new Run(runId, worldId, agentId, purpose, next, openedTick, closing ? tick : closedTick);
	}

	public boolean closed() {
		return closedTick > 0;
	}
}

package dev.ensouled.work;

/**
 * Something a being owes the world, and when it stops owing it.
 *
 * <p>A commitment outlives the plan that serves it: a plan that fails is replaced, and the
 * commitment is still there to be served by the next one. It also outlives the tick it was made in,
 * which is the whole point — the being carries it across decisions, ticks and (through
 * {@link WorkLedger}'s file) across a restart, until the deliverable is in hand or its window has
 * closed.
 *
 * <p>{@code stageReached} is the run stage this commitment has already advanced to. It is kept here
 * rather than read back from the run so that a stage is advanced once and only once, even if the run
 * record is written by somebody else.
 */
public record Commitment(String commitmentId, String worldId, String agentId, String name,
		Deliverable deliverable, String source, long openedTick, long dueTick, String runId,
		State state, long closedTick, String closedWhy, int stageReached) {

	public enum State {
		/** Owed and workable. */
		OPEN,
		/** The deliverable is in hand. */
		SATISFIED,
		/** The window closed before the deliverable was in hand. */
		EXPIRED,
		/** The being put it down; nothing is owed any more. */
		ABANDONED
	}

	public boolean open() {
		return state == State.OPEN;
	}

	public Commitment closed(State next, long tick, String why) {
		return new Commitment(commitmentId, worldId, agentId, name, deliverable, source, openedTick,
				dueTick, runId, next, tick, why, stageReached);
	}

	public Commitment staged(int stageOrdinal) {
		return new Commitment(commitmentId, worldId, agentId, name, deliverable, source, openedTick,
				dueTick, runId, state, closedTick, closedWhy, stageOrdinal);
	}

	public Commitment withRun(String id) {
		return new Commitment(commitmentId, worldId, agentId, name, deliverable, source, openedTick,
				dueTick, id, state, closedTick, closedWhy, stageReached);
	}
}

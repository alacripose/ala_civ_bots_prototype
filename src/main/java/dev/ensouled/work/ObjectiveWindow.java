package dev.ensouled.work;

/**
 * The stretch of world time during which a commitment can still be met, and what became of it.
 *
 * <p>A window is not a timer that punishes: it is the record that a course of action stops being
 * available. An expiry writes a row of its own — a window that closes quietly is a being that
 * silently stopped owing something, which is the failure mode this type exists to prevent.
 */
public record ObjectiveWindow(String windowId, String commitmentId, long opensTick, long closesTick,
		String condition, State state, long closedTick) {

	public enum State {
		OPEN, MET, EXPIRED
	}

	public boolean open() {
		return state == State.OPEN;
	}

	public ObjectiveWindow closed(State next, long tick) {
		return new ObjectiveWindow(windowId, commitmentId, opensTick, closesTick, condition, next, tick);
	}
}

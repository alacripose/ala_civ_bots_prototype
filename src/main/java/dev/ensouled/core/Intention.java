package dev.ensouled.core;

import java.util.Objects;

/**
 * The one dominant intention a body may hold (§5.3). Transition legality is enforced here so
 * two subsystems can never both drive the hands.
 */
public final class Intention {
	private final String id;
	private final String skillId;
	private final Vec3i target;
	private final String targetBlockId;
	private final long startedTick;
	private final long expiresTick;
	private final int budgetCost;

	private IntentionState state = IntentionState.ACTIVATED;
	private String terminalReason = "";

	public Intention(String id, String skillId, Vec3i target, String targetBlockId,
			long startedTick, long expiresTick, int budgetCost) {
		this.id = Objects.requireNonNull(id);
		this.skillId = Objects.requireNonNull(skillId);
		this.target = Objects.requireNonNull(target);
		this.targetBlockId = targetBlockId == null ? "" : targetBlockId;
		this.startedTick = startedTick;
		this.expiresTick = expiresTick;
		this.budgetCost = budgetCost;
	}

	public String id() {
		return id;
	}

	public String skillId() {
		return skillId;
	}

	public Vec3i target() {
		return target;
	}

	public String targetBlockId() {
		return targetBlockId;
	}

	public long startedTick() {
		return startedTick;
	}

	public long expiresTick() {
		return expiresTick;
	}

	public int budgetCost() {
		return budgetCost;
	}

	public IntentionState state() {
		return state;
	}

	public String terminalReason() {
		return terminalReason;
	}

	public boolean expired(long nowTick) {
		return nowTick > expiresTick;
	}

	public void commit() {
		transition(IntentionState.ACTIVATED, IntentionState.COMMITTED);
	}

	public void startExecution() {
		transition(IntentionState.COMMITTED, IntentionState.EXECUTING);
	}

	public void beginVerify() {
		transition(IntentionState.EXECUTING, IntentionState.VERIFYING);
	}

	public void satisfy() {
		transition(IntentionState.VERIFYING, IntentionState.SATISFIED);
	}

	public void fail(String reason) {
		if (state.terminal()) throw new IllegalStateException("already terminal: " + state);
		state = IntentionState.FAILED;
		terminalReason = reason == null ? "" : reason;
	}

	public void interrupt(String reason) {
		if (state.terminal()) throw new IllegalStateException("already terminal: " + state);
		state = IntentionState.INTERRUPTED;
		terminalReason = reason == null ? "" : reason;
	}

	public void block(String reason) {
		if (state.terminal()) throw new IllegalStateException("already terminal: " + state);
		state = IntentionState.BLOCKED;
		terminalReason = reason == null ? "" : reason;
	}

	private void transition(IntentionState from, IntentionState to) {
		if (state != from) {
			throw new IllegalStateException("illegal intention transition " + state + " -> " + to + " (expected " + from + ")");
		}
		state = to;
	}

	@Override
	public String toString() {
		return "intention[" + id + " " + skillId + "@" + target + " " + state + "]";
	}
}

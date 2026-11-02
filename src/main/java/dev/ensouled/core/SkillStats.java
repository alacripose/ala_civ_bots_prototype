package dev.ensouled.core;

/**
 * Success/failure statistics learned from verified outcomes (§5.8 outcome reinforcement).
 *
 * <p>This is a cache over {@link MindLedger}'s practice rows, not a record of its own: it is built
 * by replaying those rows and is never saved or restored directly.
 */
public final class SkillStats {
	private int successes;
	private int failures;

	public void recordSuccess() {
		successes++;
	}

	public void recordFailure() {
		failures++;
	}

	public int successes() {
		return successes;
	}

	public int failures() {
		return failures;
	}

	/** Habit strength in thousandths, derived only from observed results. */
	public int habitStrength() {
		int total = successes + failures;
		if (total == 0) {
			return 0;
		}
		return Math.min(400, (successes * 400) / total);
	}
}

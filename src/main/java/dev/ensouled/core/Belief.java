package dev.ensouled.core;

/** A reconciled belief: the agent's current stance on one fact, with its evidence. */
public record Belief(
		String key,
		String value,
		int confidenceMilli,
		PerceptSource source,
		long observedTick,
		String evidenceRef) {

	public boolean isFresh(long nowTick, int maxAgeTicks) {
		return nowTick - observedTick <= maxAgeTicks;
	}
}

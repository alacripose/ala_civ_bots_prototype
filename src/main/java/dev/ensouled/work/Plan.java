package dev.ensouled.work;

import java.util.List;

/**
 * The steps a being intends to satisfy a commitment with, and the step it is on.
 *
 * <p>A plan is disposable in a way a commitment is not: when its step stops being workable the whole
 * plan is replaced and the reason is kept, so a reader can see that the being changed its mind about
 * how, not about what. Steps are skill ids — things this body can actually do — so a plan cannot name
 * an intention the body has no routine for.
 */
public record Plan(String planId, String commitmentId, List<String> steps, int current,
		long createdTick, long replacedTick, String replacedWhy) {

	public String step() {
		return current >= 0 && current < steps.size() ? steps.get(current) : "";
	}

	/** The same commitment served by a different plan; the old plan is not edited, it is replaced. */
	public Plan replacedBy(List<String> next, long tick, String why) {
		return new Plan(planId, commitmentId, List.copyOf(next), 0, createdTick, tick, why);
	}
}

package dev.ensouled.core;

import java.util.List;

/**
 * Executable knowledge (§5.7). Implementations are typed state machines, not prose prompts, and
 * are created fresh per intention so no two bodies share routine state.
 */
public interface SkillRoutine {
	String skillId();

	/** Declared preconditions that the perceived world does not currently satisfy. */
	List<String> unmetPreconditions(SkillContext ctx);

	void begin(SkillContext ctx);

	/** Advance at most one bounded motor program per call. */
	void tick(SkillContext ctx);

	/**
	 * A line of ordinary chat this being heard while this routine was executing. A routine answers
	 * only for the exchange it owns; returning false means the line was not its business, and the
	 * mind reads it on its own. A routine never acts on a heard line without verifying the world.
	 */
	default boolean hear(SkillContext ctx, String speakerId, String speakerName, String text) {
		return false;
	}
}

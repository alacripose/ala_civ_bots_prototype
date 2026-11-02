package dev.ensouled.skills;

import java.util.List;
import java.util.Map;

import dev.ensouled.core.Phase1GatePolicy;
import dev.ensouled.core.SkillContext;
import dev.ensouled.core.SkillRoutine;

/**
 * rest@1: choosing to stay put. Standing still is a decision the being makes, never the absence of
 * one: a body with nothing more urgent to do selects a bounded rest inside its own position,
 * records that it chose rest, and then competes for its next intention like any other action.
 *
 * <p>A hungry being with food eats first; a hungry being with nothing to eat may still rest, or an
 * exhausted body would walk until it dies. Rest is never the default: the candidate only exists
 * when the body's own fatigue asks for it.
 */
public final class RestRoutine implements SkillRoutine {
	/**
	 * How long one deliberate rest lasts. Short by design: a rest is one decision among many, and
	 * ending it returns the being to its own selection on the next tick.
	 */
	public static final int REST_TICKS = 100;

	private int rested;

	@Override
	public String skillId() {
		return Phase1GatePolicy.SKILL_REST;
	}

	@Override
	public List<String> unmetPreconditions(SkillContext ctx) {
		// Rest is a survival decision, not a reward for having no needs. A hungry body that holds
		// food still eats first, because eating outranks resting in the candidate utility; a hungry
		// body with nothing to eat must be allowed to conserve what it has. Refusing rest on hunger
		// made an exhausted, starving body wander until it died (rest 4/268 on the live mob).
		return List.of();
	}

	@Override
	public void begin(SkillContext ctx) {
		rested = 0;
	}

	@Override
	public void tick(SkillContext ctx) {
		rested++;
		if (rested >= REST_TICKS) {
			ctx.complete("rested " + rested + " ticks at " + ctx.view().bodyPos(),
					Map.of("rest_ticks", Integer.toString(rested)));
		}
	}
}

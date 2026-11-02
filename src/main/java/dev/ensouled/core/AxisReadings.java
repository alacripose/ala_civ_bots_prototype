package dev.ensouled.core;

/**
 * Where one candidate's selection stands on the axis establishment ladder, read from what this
 * being can see and remember about the thing it would act on.
 *
 * <p>This is a reading, not a score, and the rungs are the crate's: {@code active}, {@code stale},
 * {@code unchecked}, {@code refused} — strongest first. It answers "does this selection stand on
 * something the world shows me now?", never "have I been good at this lately". That distinction is
 * the difference between a ladder and a habit: a rung derived from a skill's success count lets one
 * successful wander outrank every unevaluated piece of work forever, which is a being that walks in
 * circles. A rung derived from the target cannot: a tree in sight is {@code active} whether or not
 * any tree was felled before.
 *
 * <ul>
 *   <li>{@code active} — the thing it would act on is perceived now: a block in sight, a being at
 *       that position, or its own body.</li>
 *   <li>{@code stale} — this being's memory says the site was already taken, so the plan behind the
 *       selection is out of date.</li>
 *   <li>{@code unchecked} — nothing is perceived there: no evidence either way, which is a state
 *       and never a pass.</li>
 *   <li>{@code refused} — the gate refused the selection; it cannot be chosen at all.</li>
 * </ul>
 *
 * <p>The selection {@link Arbiter} sorts by this rung before utility, so a different reading picks a
 * different action rather than annotating the action already chosen.
 */
public final class AxisReadings {
	public static final String ACTIVE = "active";
	public static final String STALE = "stale";
	public static final String UNCHECKED = "unchecked";
	public static final String REFUSED = "refused";

	private AxisReadings() {
	}

	/**
	 * The rung for one selection. {@code perceived} is what the body can see now; {@code takenBefore}
	 * is what its own memory says about the site.
	 */
	public static String establishment(boolean perceived, boolean takenBefore) {
		if (takenBefore) {
			return STALE;
		}
		return perceived ? ACTIVE : UNCHECKED;
	}

	/** Why the rung is what it is, in the being's own terms. */
	public static String basis(String skillId, Vec3i target, boolean perceived, boolean takenBefore) {
		String where = target == null ? "no target" : target.toString();
		if (takenBefore) {
			return "this being's memory says " + where + " was already taken, so " + skillId
					+ " rests on an out-of-date plan";
		}
		if (perceived) {
			return where + " is in sight now, so " + skillId + " stands on an observation";
		}
		return "nothing is perceived at " + where + ", so " + skillId + " is unchecked";
	}
}

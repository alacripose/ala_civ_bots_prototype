package dev.ensouled.core;

/**
 * Body-reported physiological state. These fields are readable from the body/HUD only
 * (proprioception), so the mind may not infer them for other agents.
 */
public record NeedsState(int food, int health, int air, boolean onFire, boolean submerged, double fallDistance) {
	public static final int MAX_FOOD = 20;
	public static final int MAX_HEALTH = 20;

	public static NeedsState healthy() {
		return new NeedsState(MAX_FOOD, MAX_HEALTH, 300, false, false, 0.0);
	}

	public boolean hungry() {
		return food <= 14;
	}

	public boolean starving() {
		return food <= 6;
	}

	public boolean inDanger() {
		return onFire || submerged && air < 120 || health <= 6;
	}
}

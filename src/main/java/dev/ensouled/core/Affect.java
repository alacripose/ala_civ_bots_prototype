package dev.ensouled.core;

import java.util.Locale;

/**
 * This being's affect, read on the feeling wheel: one primary family, the precise secondary and
 * tertiary state inside it, a radius from resolved (0) to extreme (65535), and the bearer's signed
 * spectral drift. The wheel's vocabulary is the ontology — this class only decides which place on
 * the wheel this body is standing on because of what is actually happening to it.
 *
 * <p>Affect is state, not script. It is chosen from the body's own situation, it is saved with the
 * mind so a being wakes up still feeling the way its last day left it, and it is an input to
 * decisions (whether to answer, how urgently, whether to rest). It never grants authority: a
 * frightened being verifies more, it does not get permission it lacked.
 */
public record Affect(Family family, String secondary, String tertiary, int radius, int drift,
		long updatedTick) {
	public static final int RESOLVED = 0;
	public static final int CALM = 13107;
	public static final int PRESENT = 32767;
	public static final int STRONG = 49151;
	public static final int EXTREME = 65535;

	/** The six families, with the real wavelength each one is anchored at. */
	public enum Family {
		MAD("Mad", 0xFF2300, 645, "red"),
		SCARED("Scared", 0xFF7A00, 600, "orange"),
		JOYFUL("Joyful", 0xFFD500, 575, "yellow"),
		POWERFUL("Powerful", 0x39C46A, 545, "green"),
		PEACEFUL("Peaceful", 0x2E7BE0, 480, "blue"),
		SAD("Sad", 0x8B5CD6, 420, "purple");

		private final String label;
		private final int rgb;
		private final int wavelengthNm;
		private final String band;

		Family(String label, int rgb, int wavelengthNm, String band) {
			this.label = label;
			this.rgb = rgb;
			this.wavelengthNm = wavelengthNm;
			this.band = band;
		}

		public String label() {
			return label;
		}

		public int rgb() {
			return rgb;
		}

		public int wavelengthNm() {
			return wavelengthNm;
		}

		public String band() {
			return band;
		}
	}

	/**
	 * What is happening to this body right now. Deliberately about the world, not about words: the
	 * same situation yields the same place on the wheel, and no utterance can set it directly.
	 */
	public record Situation(boolean inDanger, boolean starving, boolean hungry, boolean acting,
			boolean resting, boolean spokenTo, boolean requestRefused, boolean actionFailed,
			boolean actionSucceeded, boolean peerPresent) {
	}

	public static Affect initial() {
		return new Affect(Family.PEACEFUL, "Content", "settled", CALM, 0, 0L);
	}

	public Affect at(long tick) {
		return new Affect(family, secondary, tertiary, radius, drift, tick);
	}

	/** A saved reading comes back whole, or the being starts from a settled centre. */
	public static Affect restore(String family, String secondary, String tertiary, int radius, int drift,
			long tick) {
		for (Family candidate : Family.values()) {
			if (candidate.name().equals(family)) {
				return new Affect(candidate, secondary, tertiary,
						Math.max(RESOLVED, Math.min(EXTREME, radius)), drift, tick);
			}
		}
		return initial();
	}

	public String color() {
		return String.format(Locale.ROOT, "#%06X", family.rgb());
	}

	/** The canonical path, preserved even when a playful alias is used elsewhere. */
	public String path() {
		return family.label() + " → " + secondary + " → " + tertiary;
	}

	public String describe() {
		return tertiary + " (" + path() + ", " + color() + ", radius " + radius + "/" + EXTREME
				+ ", drift " + drift + ", " + family.band() + ")";
	}

	/**
	 * The next place on the wheel, from the dominant action-relevant feeling. Precedence is the
	 * being's own: danger before hunger, a refusal before an idle moment. The radius eases toward its
	 * target instead of jumping, so one bad tick does not erase a settled day.
	 */
	public Affect react(Situation s, long tick) {
		Family next = family;
		String nextSecondary = secondary;
		String nextTertiary = tertiary;
		int target = radius;
		if (s.inDanger()) {
			next = Family.SCARED;
			nextSecondary = "Frightened";
			nextTertiary = "alarmed";
			target = EXTREME;
		} else if (s.starving()) {
			next = Family.SCARED;
			nextSecondary = "Helpless";
			nextTertiary = "powerless";
			target = STRONG;
		} else if (s.actionFailed()) {
			next = Family.MAD;
			nextSecondary = "Frustrated";
			nextTertiary = "blocked";
			target = PRESENT;
		} else if (s.requestRefused()) {
			next = Family.SAD;
			nextSecondary = "Disappointed";
			nextTertiary = "let-down";
			target = PRESENT;
		} else if (s.actionSucceeded()) {
			next = Family.POWERFUL;
			nextSecondary = "Satisfied";
			nextTertiary = "nailed-it";
			target = PRESENT;
		} else if (s.hungry()) {
			next = Family.SCARED;
			nextSecondary = "Anxious";
			nextTertiary = "worried";
			target = CALM;
		} else if (s.spokenTo()) {
			next = Family.PEACEFUL;
			nextSecondary = "Receptive";
			nextTertiary = "listening";
			target = CALM;
		} else if (s.resting()) {
			next = Family.PEACEFUL;
			nextSecondary = "Relaxed";
			nextTertiary = "unwound";
			target = CALM;
		} else if (s.acting()) {
			next = Family.POWERFUL;
			nextSecondary = "Confident";
			nextTertiary = "ready";
			target = PRESENT;
		} else if (s.peerPresent()) {
			next = Family.PEACEFUL;
			nextSecondary = "Content";
			nextTertiary = "settled";
			target = CALM;
		} else {
			next = Family.PEACEFUL;
			nextSecondary = "Content";
			nextTertiary = "settled";
			target = CALM;
		}
		int eased = next == family
				? radius + (target - radius) / 2
				: target / 2 + radius / 4;
		return new Affect(next, nextSecondary, nextTertiary, Math.max(RESOLVED, Math.min(EXTREME, eased)),
				driftFor(eased), tick);
	}

	/** A calm reading never leaves its family band; an escalated one walks out of the visible. */
	private static int driftFor(int radius) {
		if (radius >= EXTREME - 1) {
			return -4;
		}
		if (radius >= STRONG) {
			return -2;
		}
		if (radius >= PRESENT) {
			return -1;
		}
		return 0;
	}
}

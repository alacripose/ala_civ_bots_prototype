package dev.ensouled.core;

/**
 * Where a fact came from. These are distinct evidence kinds on purpose: "I saw it",
 * "I heard it", "someone told me" and "I remember it" are not the same claim (§6.2).
 */
public enum PerceptSource {
	VISION,
	SOUND,
	HUD,
	PROPRIOCEPTION,
	TOUCH,
	SOCIAL,
	MEMORY
}

package dev.ensouled.core;

public enum EpisodeKind {
	RESOURCE_COLLECTED,
	SITE_DEPLETED,
	ROUTE_BLOCKED,
	ACTION_REFUSED,
	ACTION_FAILED,
	REFLEX_INTERRUPT,
	THREAT,
	SOCIAL,
	/** An inner event: a contested choice, a murmured thought, a dream. A cell of memory, not an act. */
	THOUGHT,
	/** Memory recombined while the body rests. */
	DREAM,
	OTHER
}

package dev.ensouled.core;

/**
 * V4 §49.1: an unavailable state is not an empty state. Every life is observed in exactly one of
 * these conditions, and only {@link #OPERATIONAL} may take new consequential actions.
 *
 * <p>The two recovery conditions are the document's own names; {@code ABSENT} exists because §16
 * requires an absent record to be distinguished from a corrupt or unsupported one, and a life with
 * no durable record yet is neither.
 */
public enum ContinuityState {
	/** No durable record has ever been written for this life (a fresh world, or a person not yet written). */
	ABSENT,
	/** A record exists, was read and verified; the being may act. */
	OPERATIONAL,
	/** A record exists but the being stopped in the middle of something; it must reconcile before acting. */
	RECOVERING,
	/** The record could not be read or verified, or its schema is newer than this build. Actions are refused. */
	INTEGRITY_BLOCKED;
}

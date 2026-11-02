package dev.ensouled.domain;

/**
 * A request put to the world, carrying the revision it expects and a key that makes a repeat a no-op
 * (§A6.4). A {@code Frame} is meaning; a Command is a Frame the world is asked to act on, plus these
 * two facts that make the asking safe to retry after a dropped acknowledgement.
 *
 * <p>{@code expectedRevision} is a plain equality check against {@link WorldIdentity#revision()}. A
 * mismatch is refused rather than rebased: a command that could not see a world change has no basis
 * for deciding what its own change means.
 */
public record Command(String commandId, String worldId, long expectedRevision, String idempotencyKey,
		String type, String actor, String payload) {

	/** The journal identity of this command: the key is what deduplicates, never the id. */
	public String dedupeKey() {
		return worldId + "|" + idempotencyKey;
	}
}

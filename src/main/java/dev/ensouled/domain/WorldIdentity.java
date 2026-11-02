package dev.ensouled.domain;

/**
 * A world's identity: the durable container whose clock, seed and revision outlive every session
 * (§A5.1, §A5.2). Created once per save and never re-minted; a missing identity is a new world, not a
 * new identity for an old one.
 *
 * <p>The revision is the number a {@link Command} names when it says what state it expects. It moves
 * only when an accepted command changes the world, so a retry can be told apart from a new act.
 */
public record WorldIdentity(String worldId, long seed, int schemaVersion, long revision, long openedTick) {
	public static final int SCHEMA = 1;

	public WorldIdentity advance() {
		return new WorldIdentity(worldId, seed, schemaVersion, revision + 1, openedTick);
	}
}

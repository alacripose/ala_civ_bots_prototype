package dev.ensouled.domain;

import dev.ensouled.core.Vec3i;

/**
 * A persistent settlement inside a world: a claim, who may act in it, and the upkeep that keeps it
 * standing (§A4.2–§A4.4). A base instance is this record; a blueprint would be a separate type and is
 * not built yet (§A4.3).
 *
 * <p>The claim radius is the authority the existing gateway already enforces for a being's home —
 * this record gives that authority an identity, so a plan can name the base it acts in instead of a
 * coordinate in whatever world is loaded.
 */
public record Homebase(String baseId, String worldId, String name, String ownerAgentId, Vec3i centre,
		int claimRadius, int upkeepBuffer, long foundedTick) {
}

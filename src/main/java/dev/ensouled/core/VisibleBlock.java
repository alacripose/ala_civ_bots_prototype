package dev.ensouled.core;

/**
 * A block this agent could actually perceive. Visibility and reachability are decided by the
 * body adapter using real sight lines and reach, not by a global block query.
 */
public record VisibleBlock(Vec3i pos, String blockId, double distance, boolean lineOfSight, boolean reachable) {
}

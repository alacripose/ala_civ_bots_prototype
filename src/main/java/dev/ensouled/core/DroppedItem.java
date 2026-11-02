package dev.ensouled.core;

/**
 * An item entity this body can actually see lying in the world. Owner and pickup delay are
 * deliberately absent: a player sees an item on the ground, not who threw it or when it will be
 * collectable, so a mind must not receive those fields as perception.
 */
public record DroppedItem(String entityId, String itemId, int count, Vec3i pos, double distance) {
}

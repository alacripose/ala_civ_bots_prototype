package dev.ensouled.interact;

import java.util.Set;

import dev.ensouled.core.Vec3i;

/**
 * One world object as this body perceives it: identity, what it is, where, how far, whether the body
 * can physically reach it, and which operations the object itself supports.
 *
 * <p>Capabilities are a statement about the object, not about permission: the authority gateway
 * decides whether this being may use any of them.
 */
public record InteractionTarget(
		String objectId,
		String kind,
		String blockId,
		Vec3i pos,
		double distance,
		boolean reachable,
		Set<String> capabilities) {

	public InteractionTarget {
		capabilities = Set.copyOf(capabilities);
	}

	public boolean supports(String operation) {
		return capabilities.contains(operation);
	}

	public static String idOf(String kind, Vec3i pos) {
		return kind + "@" + pos;
	}
}

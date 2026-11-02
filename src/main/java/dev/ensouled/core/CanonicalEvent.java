package dev.ensouled.core;

/**
 * A canonical event derived from verified world facts. Story and memory may cite these; they
 * may never invent or rewrite them (§18 "Story truth").
 */
public record CanonicalEvent(
		String eventId,
		String type,
		long tick,
		String agentId,
		Vec3i where,
		String subjectId,
		String detail) {
	public static final String RESOURCE_COLLECTED = "resource.collected";
	public static final String SITE_DEPLETED = "site.depleted";
	/** An item actually moved from one agent's inventory into another's, keyed by transfer id. */
	public static final String ITEM_TRANSFERRED = "item.transferred";
	/** An item actually left a world container into this being's inventory (or entered it). */
	public static final String CONTAINER_WITHDRAWN = "container.withdrawn";
	public static final String CONTAINER_DEPOSITED = "container.deposited";
}

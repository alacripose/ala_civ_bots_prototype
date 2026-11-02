package dev.ensouled.core;

/**
 * Identity outlives the process (§7.1). The gender field is carried as an opaque value plus
 * the schema reference that defines it, because the canonical gender ontology is an open
 * Phase 0 decision (§19.5); the engine preserves the property without inventing semantics.
 */
public record AgentIdentity(
		String agentId,
		String displayName,
		String genderSchemaRef,
		String genderValue,
		long createdEpochMilli,
		long birthTick,
		String lineageNote) {

	public AgentIdentity validate() {
		if (agentId == null || agentId.isBlank()) throw new IllegalArgumentException("agentId");
		if (displayName == null || displayName.isBlank()) throw new IllegalArgumentException("displayName");
		return this;
	}
}

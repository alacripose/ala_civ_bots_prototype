package dev.ensouled.core;

/**
 * One inspectable step of the decision cycle: what was chosen, what was gated, what the body
 * actually did, and what the world showed afterwards.
 */
public record EvidenceRecord(
		String recordId,
		long tick,
		String agentId,
		String intentionId,
		EvidenceKind kind,
		String actionHash,
		String outcome,
		String detail) {
}

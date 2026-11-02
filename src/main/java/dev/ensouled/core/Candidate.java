package dev.ensouled.core;

/**
 * A proposed activation with every utility term explicit and inspectable (§5.5). Values are
 * thousandths; the sum is comparable across candidates and is NOT an authorization.
 */
public record Candidate(
		String candidateId,
		String skillId,
		Vec3i target,
		String targetBlockId,
		int needRelief,
		int commitmentValue,
		int expectedProgress,
		int habitStrength,
		int novelty,
		int physicalRisk,
		int resourceCost,
		int travelCost,
		int uncertaintyPenalty,
		int priorityClass,
		long firstSeenTick) {

	public int utilityMilli() {
		return needRelief + commitmentValue + expectedProgress + habitStrength + novelty
				- physicalRisk - resourceCost - travelCost - uncertaintyPenalty;
	}

	public boolean sameTargetAs(Candidate other) {
		return other != null && skillId.equals(other.skillId) && target.equals(other.target);
	}
}

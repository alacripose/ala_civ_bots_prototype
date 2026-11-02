package dev.ensouled.core;

/**
 * One interpreted local observation (§6.2). Confidence is measured uncertainty in
 * thousandths (1000 = fully observed under a declared sense), never free-form prose.
 */
public record Percept(
		PerceptSource source,
		long tick,
		int localityBlocks,
		String subjectType,
		String subjectId,
		String predicate,
		String value,
		int confidenceMilli,
		String evidenceRef) {

	public String key() {
		return subjectType + "|" + subjectId + "|" + predicate;
	}

	public Percept validate() {
		if (source == null) throw new IllegalArgumentException("source");
		if (subjectId == null || subjectId.isBlank()) throw new IllegalArgumentException("subjectId");
		if (predicate == null || predicate.isBlank()) throw new IllegalArgumentException("predicate");
		if (confidenceMilli < 0 || confidenceMilli > 1000) throw new IllegalArgumentException("confidenceMilli");
		if (evidenceRef == null || evidenceRef.isBlank()) throw new IllegalArgumentException("evidenceRef");
		return this;
	}
}

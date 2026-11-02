package dev.ensouled.core;

/**
 * One autobiographical episode. Provenance distinguishes what the agent did from what it
 * was told or inferred, and the evidence reference points at the canonical record.
 */
public record Episode(
		String episodeId,
		long tick,
		EpisodeKind kind,
		String summary,
		Vec3i where,
		String provenance,
		String evidenceRef) {
}

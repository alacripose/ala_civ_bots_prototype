package dev.ensouled.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The single decision owner (§2.2, §5.3). Producers submit candidates; this class picks one.
 *
 * <p>The first key is the <b>axis reading</b>, not utility: a candidate whose selection stands on a
 * stronger rung of the establishment ladder is chosen ahead of one that merely scores higher. That
 * is what makes the reading part of how the being decides — a weaker reading on the better-scoring
 * action changes which action is taken, rather than being written on the action already chosen.
 *
 * <p>Hysteresis stops a minor new stimulus from stealing a committed intention, and tie-breaking is
 * stable: reading, continuity, priority class, then action identity — never hash iteration order.
 */
public final class Arbiter {
	/** A challenger must beat the incumbent by this much to cause a switch. */
	public static final int SWITCH_MARGIN_MILLI = 200;

	public Optional<Candidate> select(List<Candidate> candidates, Intention current) {
		return select(candidates, current, Map.of());
	}

	/**
	 * Select among candidates whose axis readings were already taken. {@code readingRank} is the
	 * rank of each candidate's rung; a candidate with no entry is read as the weakest standing rung.
	 */
	public Optional<Candidate> select(List<Candidate> candidates, Intention current,
			Map<String, Integer> readingRank) {
		List<Candidate> ordered = new ArrayList<>(candidates);
		ordered.sort(Comparator
				.comparingInt((Candidate c) -> readingRank.getOrDefault(c.candidateId(), 0)).reversed()
				.thenComparing(Comparator.comparingInt(Candidate::utilityMilli).reversed())
				.thenComparing(Comparator.comparingInt(Candidate::priorityClass).reversed())
				.thenComparing(Candidate::skillId)
				.thenComparing(c -> c.target().x())
				.thenComparing(c -> c.target().y())
				.thenComparing(c -> c.target().z()));

		if (ordered.isEmpty()) {
			return Optional.empty();
		}
		Candidate best = ordered.get(0);
		if (current != null && current.state().active()) {
			Candidate incumbent = null;
			for (Candidate c : ordered) {
				if (c.skillId().equals(current.skillId()) && c.target().equals(current.target())) {
					incumbent = c;
					break;
				}
			}
			if (incumbent != null) {
				int incumbentRank = readingRank.getOrDefault(incumbent.candidateId(), 0);
				int bestRank = readingRank.getOrDefault(best.candidateId(), 0);
				// A standing commitment is kept while its reading is at least as strong and the
				// challenger does not win clearly; a stronger reading does take the body, because the
				// ladder is the reason to switch.
				if (incumbentRank > bestRank) {
					return Optional.of(incumbent);
				}
				if (incumbentRank == bestRank
						&& best.utilityMilli() - incumbent.utilityMilli() < SWITCH_MARGIN_MILLI) {
					return Optional.of(incumbent);
				}
			}
		}
		return Optional.of(best);
	}
}

package dev.ensouled.domain;

import java.util.List;

/**
 * A short simulated segment inside a world: a conversation, a wave, one act of work (§A7). It is not
 * persisted — §A5.2 excludes the transient, and a persisted encounter would be a second record of
 * something the frames already carry.
 *
 * <p>An encounter is what makes a frame attributable to a stretch of time. Two frames an hour apart
 * are not the same exchange, and a reader should not have to infer that from tick numbers.
 */
public record Encounter(String encounterId, String worldId, List<String> participants, long openedTick) {
	public boolean includes(String actor) {
		return participants.contains(actor);
	}

	public Encounter with(String actor) {
		if (includes(actor)) {
			return this;
		}
		List<String> next = new java.util.ArrayList<>(participants);
		next.add(actor);
		return new Encounter(encounterId, worldId, List.copyOf(next), openedTick);
	}
}

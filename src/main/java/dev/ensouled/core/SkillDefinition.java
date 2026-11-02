package dev.ensouled.core;

import java.util.List;

/**
 * Executable knowledge has a typed contract (§5.7). The declarative strings are documentation
 * and conformance metadata; the executable transition is the typed {@link SkillRoutine}.
 */
public record SkillDefinition(
		String id,
		int revision,
		List<String> preconditions,
		List<String> postconditions,
		List<String> failureModes,
		String gateScope) {

	public String key() {
		return id + "@" + revision;
	}
}

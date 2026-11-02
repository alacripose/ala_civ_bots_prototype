package dev.ensouled.interact;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * What the server actually did, separated from what was asked: status, typed code, and the observed
 * before/after counts used to verify the postcondition. A refusal is a first-class result — never a
 * silent success and never converted into a different path that happens to work.
 */
public record InteractionOutcome(Status status, String code, String detail, Map<String, String> facts) {
	public enum Status {
		PERFORMED,
		REFUSED,
		FAILED
	}

	public InteractionOutcome {
		facts = Map.copyOf(facts);
	}

	public static InteractionOutcome performed(String code, String detail, Map<String, String> facts) {
		return new InteractionOutcome(Status.PERFORMED, code, detail, facts);
	}

	public static InteractionOutcome refused(String code, String detail) {
		return new InteractionOutcome(Status.REFUSED, code, detail, Map.of());
	}

	public static InteractionOutcome failed(String code, String detail, Map<String, String> facts) {
		return new InteractionOutcome(Status.FAILED, code, detail, facts);
	}

	public boolean performed() {
		return status == Status.PERFORMED;
	}

	public String fact(String key, String fallback) {
		return facts.getOrDefault(key, fallback);
	}

	public int intFact(String key, int fallback) {
		try {
			return Integer.parseInt(facts.getOrDefault(key, Integer.toString(fallback)));
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	public static Map<String, String> facts(Object... pairs) {
		Map<String, String> out = new LinkedHashMap<>();
		for (int i = 0; i + 1 < pairs.length; i += 2) {
			out.put(String.valueOf(pairs[i]), String.valueOf(pairs[i + 1]));
		}
		return out;
	}
}

package dev.ensouled.core;

import java.util.Map;

/**
 * Outcome reported back from the body. The {@code facts} map carries observed world state
 * (e.g. {@code block_removed=true}, {@code inventory_log_delta=1}) so the mind can verify the
 * postcondition instead of trusting the acknowledgement (§18 "Action verification").
 */
public record MotorResult(MotorStatus status, String code, String detail, Map<String, String> facts) {
	public static MotorResult running(String detail) {
		return new MotorResult(MotorStatus.RUNNING, "running", detail, Map.of());
	}

	public static MotorResult success(String code, String detail, Map<String, String> facts) {
		return new MotorResult(MotorStatus.SUCCESS, code, detail, Map.copyOf(facts));
	}

	public static MotorResult failed(String code, String detail) {
		return new MotorResult(MotorStatus.FAILED, code, detail, Map.of());
	}

	public static MotorResult cancelled(String detail) {
		return new MotorResult(MotorStatus.CANCELLED, "cancelled", detail, Map.of());
	}

	public boolean terminal() {
		return status != MotorStatus.RUNNING;
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
}

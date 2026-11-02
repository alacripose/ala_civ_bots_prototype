package dev.ensouled.core;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * The typed scope presented to the gate (§10.3). Utility never decides authorization: this
 * envelope is evaluated by predicates after selection.
 */
public record ActionEnvelope(
		String schema,
		String agentId,
		String intentionId,
		String skillId,
		Vec3i target,
		String targetBlockId,
		String dimensionId,
		long issuedTick,
		long expiresTick,
		int budgetCost) {

	public static final String SCHEMA = "ensouled.action/1";

	public String canonical() {
		return String.join("|", schema, agentId, intentionId, skillId,
				target.toString(), targetBlockId, dimensionId,
				Long.toString(issuedTick), Long.toString(expiresTick), Integer.toString(budgetCost));
	}

	/** Stable action hash so refusals and permits are auditable and replayable. */
	public String actionHash() {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(canonical().getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < 12; i++) {
				sb.append(String.format("%02x", hash[i]));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 unavailable", e);
		}
	}
}

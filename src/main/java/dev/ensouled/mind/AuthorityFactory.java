package dev.ensouled.mind;

import dev.ensouled.core.AuthorityGateway;
import dev.ensouled.core.Phase1GatePolicy;
import dev.ensouled.core.Vec3i;

/**
 * The one place the authority implementation is chosen.
 *
 * <p>Today that is {@link Phase1GatePolicy}, a documented <b>provisional</b> stand-in for the
 * project's canonical Axis enforcement point, which is not present in this workspace
 * (docs/AUTHORITY.md, docs/PHASE0_CONTRACTS.md #6). Every being is created through this factory, so
 * replacing the authority is one edit here rather than a hunt through call sites — and nothing else
 * in the codebase may construct an authority.
 */
public final class AuthorityFactory {
	private AuthorityFactory() {
	}

	public static AuthorityGateway provisional(String agentId, Vec3i homeAnchor, int homeRadius,
			int harvestBudget) {
		return new Phase1GatePolicy(agentId, homeAnchor, homeRadius, harvestBudget);
	}
}

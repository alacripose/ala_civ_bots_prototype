package dev.ensouled.interact;

/**
 * The ownership/protection boundary every world object must pass (plan §10.5, Addendum C §C.7).
 *
 * <p>No claim or protection system exists in this build, so the only wired provider is
 * {@link #NONE}. It is deliberately an explicit hook rather than an implicit "no restrictions": when
 * a claim/protection source is integrated it answers here, and the adapter turns a denial into the
 * typed refusal {@code protected_target} instead of performing the operation.
 */
public interface OwnershipBoundary {
	boolean allows(String agentId, InteractionTarget target);

	OwnershipBoundary NONE = (agentId, target) -> true;
}

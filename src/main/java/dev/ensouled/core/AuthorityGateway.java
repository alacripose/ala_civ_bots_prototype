package dev.ensouled.core;

import dev.ensouled.interact.InteractionRequest;
import dev.ensouled.interact.InteractionTarget;

/**
 * The single authority seam every world-modifying act must cross: a selected intention, and every
 * world-object interaction a skill performs on its behalf (Addendum C §C.16, plan §11.7).
 *
 * <p>The project's canonical Axis enforcement point is not present in this workspace, so
 * {@link Phase1GatePolicy} is wired here as a <b>provisional</b> implementation. That is a stand-in,
 * not a promotion: the production integration replaces it at {@code mind.AuthorityFactory} and every
 * caller keeps talking to this interface, so no capability grows a private permission path.
 *
 * <p>A successful action proves capability, never permission. This interface is where permission is
 * decided, with typed verdicts and stable reason codes; nothing may perform a world change after a
 * refusal, and a refusal is never converted into a different path that happens to work.
 */
public interface AuthorityGateway {
	/** May this being start this intention at all? */
	GateDecision decide(ActionEnvelope envelope, WorldView view);

	/**
	 * May this being use this perceived world object in this way? Capabilities reported by the
	 * object (inspect/withdraw/deposit) are never authorization for them.
	 */
	GateDecision authorizeInteraction(InteractionRequest request, InteractionTarget target, WorldView view);

	/** Spend a granted budget unit after the action was actually performed and verified. */
	void spend(ActionEnvelope envelope, int units);

	int budgetRemaining();

	void restoreBudget(int budget);

	/**
	 * Where this being's home is, for scopes that are bounded by it. A being that materializes
	 * somewhere else reconciles its home to where it actually is, rather than acting inside a
	 * coordinate from a world it no longer stands in.
	 */
	default void reanchor(Vec3i home) {
	}
}

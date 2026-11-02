package dev.ensouled.core;

import java.util.LinkedHashSet;
import java.util.Set;

import dev.ensouled.interact.InteractionRequest;
import dev.ensouled.interact.InteractionTarget;

/**
 * Conservative pre-authorization for the Phase 1 slice (§11.7 open decision, recorded in
 * docs/PHASE0_CONTRACTS.md as a provisional ruling): the being may gather natural logs inside a
 * bounded home radius and eat its own food. Everything else is refused, including any target
 * the body cannot currently reach or perceive.
 */
public final class Phase1GatePolicy implements ProceduralGate {
	public static final String SKILL_HARVEST = "harvest_tree";
	public static final String SKILL_EAT = "eat_if_hungry";
	public static final String SKILL_SAFETY = "safety_move";
	/** Ask a peer for food, and answer such a request. Both spend no harvest budget. */
	public static final String SKILL_REQUEST_FOOD = "request_food";
	public static final String SKILL_GIVE_FOOD = "give_food";
	/** State a stock requirement to a peer, and supply one from storage. No harvest budget. */
	public static final String SKILL_REQUEST_SUPPLIES = "request_supplies";
	public static final String SKILL_SUPPLY_FROM_CHEST = "supply_from_chest";
	/**
	 * Address a perceived human, then take and stow what that human actually drops (HUM-01). No
	 * harvest budget: the being moves only what a human chose to give it, through the same
	 * interaction contract as any other container deposit.
	 */
	public static final String SKILL_REQUEST_FROM_HUMAN = "request_from_human";
	/** Choosing to stay put (rest) is a declared skill, not the idle state of a mind without one. */
	public static final String SKILL_REST = "rest";
	/** Being up and about in the neighbourhood, chosen when the body is not tired. */
	public static final String SKILL_WANDER = "wander";
	/** Putting one block from the body's own inventory back into the world, inside home. */
	public static final String SKILL_PLACE = "place_block";
	/** Most items one interaction may move; keeps a single request from emptying a container. */
	public static final int INTERACTION_BATCH_LIMIT = 8;

	private final String agentId;
	private Vec3i homeAnchor;
	private final int homeRadius;
	private final Set<String> harvestable = new LinkedHashSet<>();
	private int harvestBudgetRemaining;

	public Phase1GatePolicy(String agentId, Vec3i homeAnchor, int homeRadius, int harvestBudget) {
		this.agentId = agentId;
		this.homeAnchor = homeAnchor;
		this.homeRadius = homeRadius;
		this.harvestBudgetRemaining = harvestBudget;
		harvestable.add("minecraft:oak_log");
		harvestable.add("minecraft:birch_log");
		harvestable.add("minecraft:spruce_log");
		harvestable.add("minecraft:jungle_log");
		harvestable.add("minecraft:acacia_log");
		harvestable.add("minecraft:dark_oak_log");
		harvestable.add("minecraft:mangrove_log");
		harvestable.add("minecraft:cherry_log");
	}

	@Override
	public GateDecision decide(ActionEnvelope envelope, WorldView view) {
		String hash = envelope.actionHash();
		if (!agentId.equals(envelope.agentId())) {
			return GateDecision.refuse("identity_mismatch", "envelope belongs to another being", hash);
		}
		if (envelope.issuedTick() > envelope.expiresTick()) {
			return GateDecision.refuse("expired", "intention expired before execution", hash);
		}
		String skill = envelope.skillId();
		if (SKILL_HARVEST.equals(skill)) {
			if (harvestBudgetRemaining <= 0) {
				return GateDecision.refuse("budget_exhausted", "harvest budget for this being is spent", hash);
			}
			if (!harvestable.contains(envelope.targetBlockId())) {
				return GateDecision.refuse("not_harvestable", "target is not a declared natural log: " + envelope.targetBlockId(), hash);
			}
			if (!view.blockAt(envelope.target()).isPresent()) {
				return GateDecision.refuse("not_perceived", "target is not currently perceptible", hash);
			}
			if (!view.canReach(envelope.target())) {
				return GateDecision.refuse("not_reachable", "target is outside body reach", hash);
			}
			int distance = (int) Math.round(homeAnchor.distanceTo(envelope.target()));
			if (distance > homeRadius) {
				return GateDecision.refuse("outside_home_radius", "target " + distance + " blocks from home anchor", hash);
			}
			return GateDecision.permit(hash);
		}
		if (SKILL_EAT.equals(skill)) {
			if (envelope.budgetCost() != 0) {
				return GateDecision.refuse("bad_scope", "eating spends no harvest budget", hash);
			}
			return GateDecision.permit(hash);
		}
		if (SKILL_SAFETY.equals(skill)) {
			return GateDecision.permit(hash);
		}
		if (SKILL_REST.equals(skill)) {
			if (envelope.budgetCost() != 0) {
				return GateDecision.refuse("bad_scope", "resting spends no harvest budget", hash);
			}
			return GateDecision.permit(hash);
		}
		if (SKILL_WANDER.equals(skill)) {
			if (envelope.budgetCost() != 0) {
				return GateDecision.refuse("bad_scope", "walking about spends no harvest budget", hash);
			}
			return GateDecision.permit(hash);
		}
		if (SKILL_REQUEST_FOOD.equals(skill) || SKILL_GIVE_FOOD.equals(skill)) {
			if (envelope.budgetCost() != 0) {
				return GateDecision.refuse("bad_scope", "a food handoff spends no harvest budget", hash);
			}
			return GateDecision.permit(hash);
		}
		if (SKILL_REQUEST_SUPPLIES.equals(skill) || SKILL_SUPPLY_FROM_CHEST.equals(skill)
				|| SKILL_REQUEST_FROM_HUMAN.equals(skill)) {
			if (envelope.budgetCost() != 0) {
				return GateDecision.refuse("bad_scope", "a supply exchange spends no harvest budget", hash);
			}
			return GateDecision.permit(hash);
		}
		if (SKILL_PLACE.equals(skill)) {
			if (envelope.budgetCost() != 0) {
				return GateDecision.refuse("bad_scope", "placing a block spends no harvest budget", hash);
			}
			if (view.blockAt(envelope.target()).isPresent()) {
				return GateDecision.refuse("site_occupied", "a block already stands there", hash);
			}
			if (!view.canReach(envelope.target())) {
				return GateDecision.refuse("not_reachable", "site is outside body reach", hash);
			}
			int siteDistance = (int) Math.round(homeAnchor.distanceTo(envelope.target()));
			if (siteDistance > homeRadius) {
				return GateDecision.refuse("outside_home_radius", "site " + siteDistance + " blocks from home anchor", hash);
			}
			return GateDecision.permit(hash);
		}
		return GateDecision.refuse("skill_not_permitted", "no pre-authorization for skill " + skill, hash);
	}

	/**
	 * Provisional world-object scope: a being may read, take from, or put into a chest inside its home
	 * radius, if it can actually reach it, in bounded amounts. Anything else is refused with a reason.
	 * This is the temporary Phase-1 ruling recorded in docs/AUTHORITY.md, not the canonical Axis.
	 */
	@Override
	public GateDecision authorizeInteraction(InteractionRequest request, InteractionTarget target,
			WorldView view) {
		String hash = "interaction:" + request.operation() + ":" + target.objectId();
		if (!target.reachable()) {
			return GateDecision.refuse("not_reachable", target.objectId() + " is outside body reach", hash);
		}
		if (!target.supports(request.operation())) {
			return GateDecision.refuse("unsupported_operation",
					"object capabilities are " + target.capabilities(), hash);
		}
		int distance = (int) Math.round(homeAnchor.distanceTo(target.pos()));
		if (distance > homeRadius) {
			return GateDecision.refuse("outside_home_radius",
					"object " + distance + " blocks from home anchor", hash);
		}
		if (request.count() > INTERACTION_BATCH_LIMIT) {
			return GateDecision.refuse("bad_scope",
					"a single transfer is limited to " + INTERACTION_BATCH_LIMIT + " items", hash);
		}
		return GateDecision.permit(hash);
	}

	@Override
	public void spend(ActionEnvelope envelope, int units) {
		if (SKILL_HARVEST.equals(envelope.skillId())) {
			harvestBudgetRemaining = Math.max(0, harvestBudgetRemaining - units);
		}
	}

	public Vec3i homeAnchor() {
		return homeAnchor;
	}

	@Override
	public void reanchor(Vec3i home) {
		if (home != null) {
			this.homeAnchor = home;
		}
	}

	public int homeRadius() {
		return homeRadius;
	}

	@Override
	public int budgetRemaining() {
		return harvestBudgetRemaining;
	}

	@Override
	public void restoreBudget(int budget) {
		this.harvestBudgetRemaining = Math.max(0, budget);
	}
}

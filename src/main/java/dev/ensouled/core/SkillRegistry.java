package dev.ensouled.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import dev.ensouled.skills.EatIfHungryRoutine;
import dev.ensouled.skills.GiveFoodRoutine;
import dev.ensouled.skills.HarvestTreeRoutine;
import dev.ensouled.skills.PlaceBlockRoutine;
import dev.ensouled.skills.RequestFoodRoutine;
import dev.ensouled.skills.RequestFromHumanRoutine;
import dev.ensouled.skills.RequestSuppliesRoutine;
import dev.ensouled.skills.RestRoutine;
import dev.ensouled.skills.SupplyFromChestRoutine;
import dev.ensouled.skills.WanderRoutine;

/** Versioned registry of the Phase 1 executable skills. */
public final class SkillRegistry {
	private final Map<String, SkillDefinition> definitions = new LinkedHashMap<>();
	private final Map<String, Supplier<SkillRoutine>> factories = new LinkedHashMap<>();

	public static SkillRegistry phase1() {
		SkillRegistry registry = new SkillRegistry();
		registry.register(new SkillDefinition(
						"harvest_tree", 1,
						List.of("target_is_natural_log", "target_perceived", "target_reachable"),
						List.of("target_block_removed", "drop_collected", "inventory_delta_observed"),
						List.of("target_occluded", "path_invalid", "tool_broken", "block_protected", "action_timeout"),
						"gather.natural_logs.home_radius"),
				HarvestTreeRoutine::new);
		registry.register(new SkillDefinition(
						PlaceBlockRoutine.ID, 1,
						List.of("holding_a_buildable_block", "site_in_reach", "site_empty"),
						List.of("block_placed", "inventory_delta_observed"),
						List.of("nothing_to_place", "no_site_in_reach", "site_not_reachable",
								"not_in_hotbar", "use_failed", "postcondition_failed",
								"no_inventory_delta", "path_invalid"),
						"build.place_one_block"),
				PlaceBlockRoutine::new);
		registry.register(new SkillDefinition(
						"eat_if_hungry", 1,
						List.of("food_low", "food_item_on_hand"),
						List.of("food_value_consumed", "hunger_observed"),
						List.of("no_food_item", "not_hungry", "eat_timeout"),
						"self.sustain"),
				EatIfHungryRoutine::new);
		registry.register(new SkillDefinition(
						Phase1GatePolicy.SKILL_REQUEST_FOOD, 1,
						List.of("hungry", "no_food_on_hand", "peer_in_sight"),
						List.of("peer_accepted", "delivery_observed", "inventory_delta_observed"),
						List.of("no_peer", "peer_declined", "request_timeout", "delivery_timeout"),
						"social.ask_for_food"),
				RequestFoodRoutine::new);
		registry.register(new SkillDefinition(
						Phase1GatePolicy.SKILL_GIVE_FOOD, 1,
						List.of("pending_request"),
						List.of("requester_answered", "item_dropped", "inventory_delta_observed"),
						List.of("item_not_held", "requester_not_in_sight", "requester_unreachable", "drop_failed"),
						"social.share_food"),
				GiveFoodRoutine::new);
		registry.register(new SkillDefinition(
						Phase1GatePolicy.SKILL_REQUEST_SUPPLIES, 1,
						List.of("stock_deficit", "storage_in_sight", "peer_in_sight"),
						List.of("peer_accepted", "delivery_observed", "container_delta_verified"),
						List.of("no_peer", "peer_declined", "request_timeout", "delivery_timeout",
								"storage_lost", "stow_unverified"),
						"social.ask_for_supplies"),
				RequestSuppliesRoutine::new);
		registry.register(new SkillDefinition(
						Phase1GatePolicy.SKILL_SUPPLY_FROM_CHEST, 1,
						List.of("pending_request", "storage_in_sight", "stock_available"),
						List.of("requester_answered", "container_delta_verified", "item_dropped",
								"inventory_delta_observed"),
						List.of("no_storage_in_sight", "stock_unavailable", "not_reachable",
								"requester_not_in_sight", "requester_unreachable", "drop_failed"),
						"social.supply_from_storage"),
				SupplyFromChestRoutine::new);
		registry.register(new SkillDefinition(
						Phase1GatePolicy.SKILL_REQUEST_FROM_HUMAN, 1,
						List.of("stock_deficit", "storage_in_sight", "human_in_sight"),
						List.of("human_addressed", "human_supply_observed", "container_delta_verified"),
						List.of("human_in_sight", "human_silent", "human_left", "drop_unreachable",
								"pickup_unverified", "storage_lost", "stow_unverified"),
						"social.ask_a_human"),
				RequestFromHumanRoutine::new);
		registry.register(new SkillDefinition(
						Phase1GatePolicy.SKILL_REST, 1,
						List.of("needs_settled"),
						List.of("rest_interval_observed", "fatigue_relieved"),
						List.of("need_arose", "rest_interrupted"),
						"self.rest"),
				RestRoutine::new);
		registry.register(new SkillDefinition(
						Phase1GatePolicy.SKILL_WANDER, 1,
						List.of("open_ground_in_sight"),
						List.of("body_moved", "arrival_observed"),
						List.of("path_invalid", "no_progress", "action_timeout"),
						"self.be_up_and_about"),
				WanderRoutine::new);
		return registry;
	}

	public void register(SkillDefinition definition, Supplier<SkillRoutine> factory) {
		SkillRoutine probe = factory.get();
		if (!definition.id().equals(probe.skillId())) {
			throw new IllegalArgumentException("definition/routine id mismatch: " + definition.id() + " vs " + probe.skillId());
		}
		definitions.put(definition.key(), definition);
		factories.put(definition.id(), factory);
	}

	public Optional<SkillDefinition> definition(String key) {
		return Optional.ofNullable(definitions.get(key));
	}

	/** Fresh routine per intention: no shared mutable state between bodies. */
	public SkillRoutine newRoutine(String skillId) {
		Supplier<SkillRoutine> factory = factories.get(skillId);
		if (factory == null) {
			throw new IllegalArgumentException("unknown skill: " + skillId);
		}
		return factory.get();
	}

	public boolean has(String skillId) {
		return factories.containsKey(skillId);
	}

	public List<SkillDefinition> definitions() {
		return List.copyOf(definitions.values());
	}
}

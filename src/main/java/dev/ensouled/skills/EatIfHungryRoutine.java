package dev.ensouled.skills;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.ensouled.core.BlockIds;
import dev.ensouled.core.InventorySlot;
import dev.ensouled.core.MotorCommand;
import dev.ensouled.core.SkillContext;
import dev.ensouled.core.SkillRoutine;

/** eat_if_hungry@1 — consume a real food item from the body inventory and verify the hunger delta. */
public final class EatIfHungryRoutine implements SkillRoutine {
	private String itemId = "";
	private int foodBefore;
	private int countBefore;

	@Override
	public String skillId() {
		return "eat_if_hungry";
	}

	@Override
	public List<String> unmetPreconditions(SkillContext ctx) {
		List<String> missing = new ArrayList<>();
		if (!ctx.view().needs().hungry()) {
			missing.add("food_low");
		}
		if (findFood(ctx).isEmpty()) {
			missing.add("food_item_on_hand");
		}
		return missing;
	}

	@Override
	public void begin(SkillContext ctx) {
		itemId = findFood(ctx);
		foodBefore = ctx.view().needs().food();
		countBefore = ctx.countItem(itemId);
	}

	@Override
	public void tick(SkillContext ctx) {
		if (ctx.step() == 0) {
			if (!ctx.idle()) {
				return;
			}
			if (ctx.issued()) {
				if (ctx.lastFailed()) {
					ctx.fail("eat_failed", ctx.last().code() + ": " + ctx.last().detail());
					return;
				}
				ctx.advance();
				return;
			}
			ctx.issue(new MotorCommand.Eat(itemId, 80));
			return;
		}

		int foodAfter = ctx.view().needs().food();
		int countAfter = ctx.countItem(itemId);
		Map<String, String> facts = new LinkedHashMap<>();
		facts.put("food_before", Integer.toString(foodBefore));
		facts.put("food_after", Integer.toString(foodAfter));
		facts.put("item_before", Integer.toString(countBefore));
		facts.put("item_after", Integer.toString(countAfter));

		if (foodAfter <= foodBefore && countAfter >= countBefore) {
			ctx.fail("no_hunger_delta", "no food value was consumed");
			return;
		}
		ctx.complete("ate " + itemId, facts);
	}

	private static String findFood(SkillContext ctx) {
		for (InventorySlot slot : ctx.view().inventory()) {
			if (slot.count() > 0 && BlockIds.isEdible(slot.itemId())) {
				return slot.itemId();
			}
		}
		return "";
	}
}

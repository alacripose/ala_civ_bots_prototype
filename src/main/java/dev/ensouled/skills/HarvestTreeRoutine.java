package dev.ensouled.skills;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import dev.ensouled.core.BlockIds;
import dev.ensouled.core.MotorCommand;
import dev.ensouled.core.SkillContext;
import dev.ensouled.core.SkillRoutine;
import dev.ensouled.core.Vec3i;
import dev.ensouled.core.VisibleBlock;

/**
 * harvest_tree@1 — approach a perceived natural log, look at it, mine the reachable column with
 * real block-breaking mechanics, pick up the drops by touch, then verify the block removed and the
 * inventory delta before claiming success.
 */
public final class HarvestTreeRoutine implements SkillRoutine {
	private static final int MAX_COLUMN = 3;

	private Vec3i cursor;
	private int mined;
	private int startItems;

	@Override
	public String skillId() {
		return "harvest_tree";
	}

	@Override
	public List<String> unmetPreconditions(SkillContext ctx) {
		List<String> missing = new ArrayList<>();
		Optional<VisibleBlock> target = ctx.view().blockAt(ctx.intention().target());
		if (target.isEmpty()) {
			missing.add("target_perceived");
			return missing;
		}
		if (!BlockIds.isNaturalLog(target.get().blockId())) {
			missing.add("target_is_natural_log");
		}
		if (!target.get().reachable()) {
			missing.add("target_reachable");
		}
		return missing;
	}

	@Override
	public void begin(SkillContext ctx) {
		cursor = ctx.intention().target();
		mined = 0;
		startItems = ctx.totalItems();
	}

	@Override
	public void tick(SkillContext ctx) {
		switch (ctx.step()) {
			case 0 -> approach(ctx);
			case 1 -> orient(ctx);
			case 2 -> mineColumn(ctx);
			case 3 -> collect(ctx);
			default -> verify(ctx);
		}
	}

	private void approach(SkillContext ctx) {
		if (!ctx.idle()) {
			return;
		}
		if (ctx.issued()) {
			if (ctx.lastFailed()) {
				ctx.fail("path_invalid", ctx.last().code() + ": " + ctx.last().detail());
				return;
			}
			ctx.advance();
			return;
		}
		Optional<Vec3i> standing = ctx.view().nearestStandingNear(ctx.intention().target(), 3);
		if (standing.isEmpty()) {
			ctx.fail("path_invalid", "no adjacent standing position");
			return;
		}
		ctx.issue(new MotorCommand.MoveTo(standing.get(), 1.5, 400));
	}

	private void orient(SkillContext ctx) {
		if (!ctx.idle()) {
			return;
		}
		if (ctx.issued()) {
			// Looking is best-effort: a failed look does not invalidate a reachable target.
			ctx.advance();
			return;
		}
		ctx.issue(new MotorCommand.LookAt(cursor, 40));
	}

	private void mineColumn(SkillContext ctx) {
		if (!ctx.idle()) {
			return;
		}
		if (ctx.issued()) {
			if (ctx.lastFailed()) {
				ctx.fail("mining_failed", ctx.last().code() + ": " + ctx.last().detail());
				return;
			}
			mined++;
			cursor = cursor.above();
			ctx.clearResult();
			if (mined >= MAX_COLUMN) {
				ctx.advance();
			}
			return;
		}
		Optional<VisibleBlock> next = ctx.view().blockAt(cursor);
		if (next.isEmpty() || !BlockIds.isNaturalLog(next.get().blockId()) || !next.get().reachable()) {
			ctx.advance();
			return;
		}
		ctx.issue(new MotorCommand.MineBlock(cursor, next.get().blockId(), 240));
	}

	private void collect(SkillContext ctx) {
		if (!ctx.idle()) {
			return;
		}
		if (ctx.issued()) {
			// Drops that remain on the ground are caught by the verification step.
			ctx.advance();
			return;
		}
		ctx.issue(new MotorCommand.CollectDrops(4, 120));
	}

	private void verify(SkillContext ctx) {
		int delta = ctx.totalItems() - startItems;
		Optional<VisibleBlock> original = ctx.view().blockAt(ctx.intention().target());
		boolean removed = original.isEmpty() || !BlockIds.isNaturalLog(original.get().blockId());

		Map<String, String> facts = new LinkedHashMap<>();
		facts.put("logs_mined", Integer.toString(mined));
		facts.put("inventory_delta", Integer.toString(delta));
		facts.put("target_removed", Boolean.toString(removed));

		if (mined <= 0) {
			ctx.fail("nothing_mined", "no log was actually removed from the world");
			return;
		}
		if (!removed) {
			ctx.fail("postcondition_failed", "target block is still present after mining");
			return;
		}
		if (delta <= 0) {
			ctx.fail("no_inventory_delta", "logs were removed but no drop reached the inventory");
			return;
		}
		ctx.complete("harvested " + mined + " log(s) at " + ctx.intention().target(), facts);
	}
}

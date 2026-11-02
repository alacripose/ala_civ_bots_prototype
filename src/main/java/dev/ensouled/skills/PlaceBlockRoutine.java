package dev.ensouled.skills;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import dev.ensouled.core.InventorySlot;
import dev.ensouled.core.MotorCommand;
import dev.ensouled.core.SkillContext;
import dev.ensouled.core.SkillRoutine;
import dev.ensouled.core.Vec3i;
import dev.ensouled.core.VisibleBlock;
import dev.ensouled.work.Deliverable;
import dev.ensouled.work.WorkLedger;

/**
 * place_block@1 — put one block from the body's own inventory into the world beside it, through the
 * same player input a person uses: equip the stack, right-click a visible face, and then check that
 * a block is standing where there was none and that the stack actually shrank.
 *
 * <p>This is the construction half of a build wish. {@code harvest_tree} takes material out of the
 * world; this puts material back, which is what makes a settlement possible rather than a pile of
 * drops. The site is chosen from what the body can see and reach, never supplied by a client, and the
 * gate refuses a site outside the home radius or one that is already occupied.
 *
 * <p>Placement is recorded through {@link WorkLedger#recordPlacement} only after the world shows the
 * block: the routine reports what it observed, and the ticket closes on that observation.
 */
public final class PlaceBlockRoutine implements SkillRoutine {
	public static final String ID = "place_block";
	/** How far from the body a site is looked for. Reach is 4.5 blocks; this stays inside it. */
	public static final int SITE_RADIUS = 3;

	private Vec3i site;
	private String itemId = "";
	private int startCount;

	@Override
	public String skillId() {
		return ID;
	}

	@Override
	public List<String> unmetPreconditions(SkillContext ctx) {
		List<String> missing = new ArrayList<>();
		String item = itemFor(ctx);
		if (item.isEmpty()) {
			missing.add("nothing_to_place");
		}
		Optional<Vec3i> found = siteFor(ctx);
		if (found.isEmpty()) {
			missing.add("no_site_in_reach");
		} else if (!ctx.view().canReach(found.get())) {
			missing.add("site_not_reachable");
		}
		return missing;
	}

	@Override
	public void begin(SkillContext ctx) {
		site = siteFor(ctx).orElse(ctx.intention().target());
		itemId = ctx.intention().targetBlockId().isBlank() ? itemFor(ctx) : ctx.intention().targetBlockId();
		startCount = countHeld(ctx, itemId);
	}

	@Override
	public void tick(SkillContext ctx) {
		switch (ctx.step()) {
			case 0 -> approach(ctx);
			case 1 -> equip(ctx);
			case 2 -> place(ctx);
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
		Optional<Vec3i> standing = ctx.view().nearestStandingNear(site, 2);
		if (standing.isEmpty()) {
			ctx.fail("path_invalid", "no standing position beside " + site);
			return;
		}
		ctx.issue(new MotorCommand.MoveTo(standing.get(), 1.5, 400));
	}

	private void equip(SkillContext ctx) {
		if (!ctx.idle()) {
			return;
		}
		if (ctx.issued()) {
			// The hotbar may not carry the stack; the vanilla path refuses rather than reaching into
			// the inventory, so a failure here is a named refusal, not a workaround.
			if (ctx.lastFailed()) {
				ctx.fail("not_in_hotbar", ctx.last().code() + ": " + ctx.last().detail());
				return;
			}
			ctx.advance();
			return;
		}
		ctx.issue(new MotorCommand.Equip(itemId, 40));
	}

	private void place(SkillContext ctx) {
		if (!ctx.idle()) {
			return;
		}
		if (ctx.issued()) {
			if (ctx.lastFailed()) {
				ctx.fail("use_failed", ctx.last().code() + ": " + ctx.last().detail());
				return;
			}
			ctx.advance();
			return;
		}
		Vec3i support = new Vec3i(site.x(), site.y() - 1, site.z());
		Optional<VisibleBlock> face = ctx.view().blockAt(support);
		if (face.isEmpty()) {
			ctx.advance();
			return;
		}
		// Clicking the block under the site, looking down at it, is how a person places the block
		// above it: the adapter resolves the face from the eyes, exactly as the client would.
		ctx.issue(new MotorCommand.LookAt(site, 40));
		ctx.issue(new MotorCommand.Use(support, face.get().blockId(), 60));
	}

	private void verify(SkillContext ctx) {
		if (!ctx.idle()) {
			return;
		}
		Optional<VisibleBlock> placed = ctx.view().blockAt(site);
		int count = countHeld(ctx, itemId);
		boolean standing = placed.isPresent() && !"minecraft:air".equals(placed.get().blockId())
				&& !"minecraft:cave_air".equals(placed.get().blockId());
		Map<String, String> facts = new LinkedHashMap<>();
		facts.put("site", site.toString());
		facts.put("observed_block", placed.map(VisibleBlock::blockId).orElse("none"));
		facts.put("inventory_before", Integer.toString(startCount));
		facts.put("inventory_after", Integer.toString(count));
		if (!standing) {
			ctx.fail("postcondition_failed", "no block stands at " + site + " after placing");
			return;
		}
		if (count >= startCount) {
			ctx.fail("no_inventory_delta", "a block appeared but the stack did not shrink");
			return;
		}
		WorkLedger.recordPlacement(ctx.agentId(), ctx.view().tick(), site, placed.get().blockId());
		ctx.complete("placed " + placed.get().blockId() + " at " + site, facts);
	}

	/** The item this body would place: the first stack that matches what the open ticket owes. */
	private static String itemFor(SkillContext ctx) {
		return WorkLedger.itemForBuild(ctx.agentId(), ctx.view()).orElse("");
	}

	private static int countHeld(SkillContext ctx, String itemId) {
		if (itemId == null || itemId.isBlank()) {
			return 0;
		}
		int total = 0;
		for (InventorySlot slot : ctx.view().inventory()) {
			if (itemId.equals(slot.itemId())) {
				total += slot.count();
			}
		}
		return total;
	}

	private static Optional<Vec3i> siteFor(SkillContext ctx) {
		return siteNear(ctx.view(), ctx.view().bodyPos());
	}

	/** A reachable empty cell beside the body with something solid under it, nearest first. */
	public static Optional<Vec3i> siteNear(dev.ensouled.core.WorldView view, Vec3i body) {
		Vec3i best = null;
		double bestDistance = Double.MAX_VALUE;
		for (int dy = -1; dy <= 1; dy++) {
			for (int dx = -SITE_RADIUS; dx <= SITE_RADIUS; dx++) {
				for (int dz = -SITE_RADIUS; dz <= SITE_RADIUS; dz++) {
					Vec3i candidate = new Vec3i(body.x() + dx, body.y() + dy, body.z() + dz);
					if (candidate.equals(body)) {
						continue;
					}
					if (view.blockAt(candidate).isPresent()) {
						continue;
					}
					if (view.blockAt(new Vec3i(candidate.x(), candidate.y() - 1, candidate.z())).isEmpty()) {
						continue;
					}
					if (!view.canReach(candidate)) {
						continue;
					}
					double distance = body.distanceTo(candidate);
					if (distance < bestDistance) {
						best = candidate;
						bestDistance = distance;
					}
				}
			}
		}
		return Optional.ofNullable(best);
	}
}

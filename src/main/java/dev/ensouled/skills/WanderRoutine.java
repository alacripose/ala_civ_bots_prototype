package dev.ensouled.skills;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import dev.ensouled.core.SkillContext;
import dev.ensouled.core.SkillRoutine;
import dev.ensouled.core.Vec3i;
import dev.ensouled.core.WorldView;

/**
 * wander@1 — being up and about in its own neighbourhood. A living being with no unmet need does
 * not stand still by default: it walks somewhere it can see, looks around, and then decides again.
 * Resting is a separate decision made when the body actually wants it, never the fallback when
 * nothing else is on offer.
 *
 * <p>The destination is picked from ground the body can already perceive; the walk is an ordinary
 * move command, and the outcome is verified from where the body actually ended up.
 */
public final class WanderRoutine implements SkillRoutine {
	public static final String SKILL = "wander";
	/** Ordered offsets tried around the body, so a blocked direction is not the only direction. */
	private static final int[][] STEPS = { { 1, 0 }, { 0, 1 }, { -1, 0 }, { 0, -1 },
			{ 1, 1 }, { -1, 1 }, { 1, -1 }, { -1, -1 } };
	/** How far the body actually has to move for the walk to count as having happened. */
	private static final double MOVED_ENOUGH = 1.5;

	private Vec3i start;
	private Vec3i destination;
	private boolean moved;
	/** The motor reported the destination reached, whatever the straight-line distance says. */
	private boolean arrived;

	@Override
	public String skillId() {
		return SKILL;
	}

	/**
	 * Where this body could walk to now: a standing position it can perceive and reach, away from
	 * where it already stands. The direction rotates with the clock so a being does not pace the
	 * same two blocks forever.
	 */
	public static Optional<Vec3i> destination(WorldView view, String agentId, long tick) {
		Vec3i body = view.bodyPos();
		int offset = Math.floorMod((agentId + "#" + (tick / 200)).hashCode(), STEPS.length);
		for (int i = 0; i < STEPS.length; i++) {
			int[] step = STEPS[(offset + i) % STEPS.length];
			int distance = 4 + (i % 3) * 2;
			Vec3i roughly = new Vec3i(body.x() + step[0] * distance, body.y(),
					body.z() + step[1] * distance);
			Optional<Vec3i> standing = view.nearestStandingNear(roughly, 3);
			if (standing.isEmpty() || standing.get().equals(body)) {
				continue;
			}
			// A step of one or two blocks is a shuffle, not a walk: the destination has to be far
			// enough that arriving means the body actually went somewhere.
			if (standing.get().distanceTo(body) < 3.0) {
				continue;
			}
			if (!view.isStandable(standing.get()) || !view.canReach(standing.get())) {
				continue;
			}
			return standing;
		}
		return Optional.empty();
	}

	@Override
	public List<String> unmetPreconditions(SkillContext ctx) {
		Optional<Vec3i> open = destination(ctx.view(), ctx.agentId(), ctx.view().tick());
		if (open.isEmpty()) {
			return List.of("open_ground_in_sight");
		}
		return List.of();
	}

	@Override
	public void begin(SkillContext ctx) {
		this.start = ctx.view().bodyPos();
		// Walk to the place the arbiter weighed and the gate permitted, not a place recomputed now:
		// a recomputed direction can land on the body's own position, which is not a walk.
		Vec3i chosen = ctx.intention().target();
		this.destination = chosen.equals(start)
				? destination(ctx.view(), ctx.agentId(), ctx.view().tick()).orElse(chosen)
				: chosen;
		this.moved = false;
		this.arrived = false;
	}

	@Override
	public void tick(SkillContext ctx) {
		if (!ctx.idle()) {
			return;
		}
		if (!moved) {
			if (ctx.issued()) {
				if (ctx.lastFailed()) {
					ctx.fail("path_invalid", ctx.last().code() + ": " + ctx.last().detail());
					return;
				}
				moved = true;
				// The motor already judged the walk: it stops inside its own radius. A walk that
				// arrived at 1.4 blocks is an arrival, not a body that failed to move.
				arrived = true;
				ctx.clearResult();
				return;
			}
			ctx.issue(new dev.ensouled.core.MotorCommand.MoveTo(destination, 1.5, 300));
			return;
		}
		double walked = ctx.view().bodyPos().distanceTo(start);
		Map<String, String> facts = new LinkedHashMap<>();
		facts.put("walked", Integer.toString((int) Math.round(walked)));
		facts.put("from", start.toString());
		facts.put("to", ctx.view().bodyPos().toString());
		if (walked < MOVED_ENOUGH && !arrived) {
			ctx.fail("no_progress", "body did not actually move from " + start);
			return;
		}
		ctx.complete("walked " + Math.round(walked) + " blocks to " + ctx.view().bodyPos(), facts);
	}
}

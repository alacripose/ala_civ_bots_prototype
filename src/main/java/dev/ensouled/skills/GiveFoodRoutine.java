package dev.ensouled.skills;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import dev.ensouled.core.AgentMessage;
import dev.ensouled.core.BlockIds;
import dev.ensouled.core.EvidenceKind;
import dev.ensouled.core.MessageBus;
import dev.ensouled.core.MotorCommand;
import dev.ensouled.core.Peers;
import dev.ensouled.core.Phase1GatePolicy;
import dev.ensouled.core.SkillContext;
import dev.ensouled.core.SkillRoutine;
import dev.ensouled.core.SocialStream;
import dev.ensouled.core.Vec3i;

/**
 * give_food@1 — a being with a pending request decides for itself whether it can honour it. If it
 * holds the item and can see the requester it accepts, walks over, drops the real stack through the
 * player drop path, and verifies its own inventory went down. Otherwise it declines with a reason,
 * and the requester learns a refusal rather than a delivery.
 */
public final class GiveFoodRoutine implements SkillRoutine {
	private static final int REQUESTER_RADIUS = 16;
	private static final int DELIVERY_DISTANCE = 3;

	private AgentMessage request;
	private int startCount;

	@Override
	public String skillId() {
		return Phase1GatePolicy.SKILL_GIVE_FOOD;
	}

	@Override
	public List<String> unmetPreconditions(SkillContext ctx) {
		List<String> missing = new ArrayList<>();
		Optional<AgentMessage> pending = ctx.social().bus().peek(ctx.agentId(), ctx.view().tick());
		if (pending.isEmpty() || !AgentMessage.REQUEST.equals(pending.get().speechAct())) {
			missing.add("pending_request");
		} else if (!BlockIds.isEdible(pending.get().itemId())) {
			// This skill shares food from the hands; a resource request is supply_from_chest's.
			missing.add("not_a_food_request");
		}
		return missing;
	}

	@Override
	public void begin(SkillContext ctx) {
		Optional<AgentMessage> taken = ctx.social().bus().take(ctx.agentId(), ctx.view().tick());
		if (taken.isEmpty()) {
			ctx.fail("no_request", "the request was gone before it could be read");
			return;
		}
		request = taken.get();
		startCount = ctx.countItem(request.itemId());
	}

	@Override
	public void tick(SkillContext ctx) {
		if (request == null) {
			ctx.fail("no_request", "routine began without a request");
			return;
		}
		switch (ctx.step()) {
			case 0 -> assess(ctx);
			case 1 -> approach(ctx);
			case 2 -> drop(ctx);
			case 3 -> verify(ctx);
			default -> ctx.fail("handoff_state", "unexpected step " + ctx.step());
		}
	}

	private void assess(SkillContext ctx) {
		if (startCount < request.count()) {
			decline(ctx, "item_not_held");
			ctx.fail("item_not_held", "holds " + startCount + " " + request.itemId()
					+ ", request needs " + request.count());
			return;
		}
		if (Peers.agent(ctx.view(), ctx.social(), request.senderId(), REQUESTER_RADIUS).isEmpty()) {
			decline(ctx, "requester_not_in_sight");
			ctx.fail("requester_not_in_sight", "cannot see " + request.senderId());
			return;
		}
		ctx.social().bus().accept(ctx.agentId(), request.senderId(), request, ctx.view().tick());
		ctx.note(EvidenceKind.SOCIAL, "accepted",
				"accepted " + request.messageId() + " from " + request.senderId());
		SocialStream.accepted(ctx.agentId(), request.senderId(), request.itemId(), request.count(),
				request.messageId(), ctx.view().tick());
		ctx.advance();
	}

	private void approach(SkillContext ctx) {
		if (!ctx.idle()) {
			return;
		}
		if (ctx.issued()) {
			if (ctx.lastFailed()) {
				decline(ctx, "requester_unreachable");
				ctx.fail("requester_unreachable", ctx.last().code() + ": " + ctx.last().detail());
				return;
			}
			ctx.advance();
			return;
		}
		Optional<Peers.Peer> requester = Peers.agent(ctx.view(), ctx.social(), request.senderId(),
				REQUESTER_RADIUS);
		if (requester.isEmpty()) {
			decline(ctx, "requester_lost");
			ctx.fail("requester_lost", "lost sight of " + request.senderId() + " while approaching");
			return;
		}
		Peers.Peer peer = requester.get();
		if (peer.distance() <= DELIVERY_DISTANCE) {
			ctx.advance();
			return;
		}
		ctx.issue(new MotorCommand.MoveTo(peer.pos(), 2.5, 240));
	}

	private void drop(SkillContext ctx) {
		if (!ctx.idle()) {
			return;
		}
		if (ctx.issued()) {
			if (ctx.lastFailed()) {
				decline(ctx, "drop_failed");
				ctx.fail("drop_failed", ctx.last().code() + ": " + ctx.last().detail());
				return;
			}
			ctx.advance();
			return;
		}
		ctx.issue(new MotorCommand.DropItem(request.itemId(), request.count(), 40));
	}

	private void verify(SkillContext ctx) {
		int now = ctx.countItem(request.itemId());
		int dropped = startCount - now;
		Map<String, String> facts = new LinkedHashMap<>();
		facts.put("transfer_id", request.messageId());
		facts.put("item", request.itemId());
		facts.put("count", Integer.toString(dropped));
		facts.put("inventory_before", Integer.toString(startCount));
		facts.put("inventory_after", Integer.toString(now));
		facts.put("requester", request.senderId());
		if (dropped < request.count()) {
			ctx.fail("drop_unverified", "inventory went " + startCount + "->" + now
					+ ", expected to lose " + request.count());
			return;
		}
		Vec3i here = ctx.view().bodyPos();
		SocialStream.handed(ctx.agentId(), request.senderId(), request.itemId(), dropped, here,
				request.messageId(), ctx.view().tick());
		ctx.complete("handed " + dropped + " " + request.itemId() + " to " + request.senderId()
				+ " at " + here, facts);
	}

	private void decline(SkillContext ctx, String reason) {
		ctx.social().bus().decline(ctx.agentId(), request.senderId(), request, reason,
				ctx.view().tick());
		ctx.note(EvidenceKind.SOCIAL, "declined", reason + " for " + request.messageId());
		SocialStream.declined(ctx.agentId(), request.senderId(), request.itemId(), reason,
				request.messageId(), ctx.view().tick());
	}
}

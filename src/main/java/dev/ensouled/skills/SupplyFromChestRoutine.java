package dev.ensouled.skills;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import dev.ensouled.core.AgentMessage;
import dev.ensouled.core.BlockIds;
import dev.ensouled.core.EvidenceKind;
import dev.ensouled.core.MotorCommand;
import dev.ensouled.core.Peers;
import dev.ensouled.core.Phase1GatePolicy;
import dev.ensouled.core.SkillContext;
import dev.ensouled.core.SkillRoutine;
import dev.ensouled.interact.InteractionOutcome;
import dev.ensouled.interact.InteractionRequest;
import dev.ensouled.interact.InteractionTarget;

/**
 * supply_from_chest@1 — the supplier side of a resource transfer. A pending request for a resource
 * this being does not hold is answered from storage: it perceives a chest, reads its contents through
 * the gated inspect operation, accepts only if that chest can cover the request, withdraws through the
 * real container path, walks to the requester and drops the stack, then verifies its own inventory
 * delta. A refusal (no storage, short stock, unreachable chest, full inventory) is answered with a
 * typed decline, never with a different path that happens to work.
 *
 * <p>Food requests belong to {@link GiveFoodRoutine}, which answers from what the being carries; this
 * skill is the storage-backed resource supply, so the two never compete for the same message.
 */
public final class SupplyFromChestRoutine implements SkillRoutine {
	/** How far this body can look for the chest it will draw from. */
	public static final int STORAGE_RADIUS = 6;

	private static final int REQUESTER_RADIUS = 16;
	private static final int DELIVERY_DISTANCE = 3;

	private AgentMessage request;
	private InteractionTarget chest;
	private InteractionOutcome withdrawOutcome;
	private int startCount;

	@Override
	public String skillId() {
		return Phase1GatePolicy.SKILL_SUPPLY_FROM_CHEST;
	}

	@Override
	public List<String> unmetPreconditions(SkillContext ctx) {
		List<String> missing = new ArrayList<>();
		Optional<AgentMessage> pending = ctx.social().bus().peek(ctx.agentId(), ctx.view().tick());
		if (pending.isEmpty() || !AgentMessage.REQUEST.equals(pending.get().speechAct())) {
			missing.add("pending_request");
			return missing;
		}
		AgentMessage asked = pending.get();
		if (BlockIds.isEdible(asked.itemId())) {
			// A food request is answered from the hands by give_food, not from a chest.
			missing.add("not_a_resource_request");
		}
		if (ctx.countItem(asked.itemId()) >= asked.count()) {
			// Already carrying it: this being needs no chest to answer.
			missing.add("item_already_held");
		}
		if (storage(ctx).isEmpty()) {
			missing.add("storage_in_sight");
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
			case 1 -> withdraw(ctx);
			case 2 -> approach(ctx);
			case 3 -> drop(ctx);
			case 4 -> verify(ctx);
			default -> ctx.fail("supply_state", "unexpected step " + ctx.step());
		}
	}

	private void assess(SkillContext ctx) {
		Optional<InteractionTarget> found = storage(ctx);
		if (found.isEmpty()) {
			decline(ctx, "no_storage_in_sight");
			ctx.fail("no_storage_in_sight", "no chest supporting a withdraw is perceived");
			return;
		}
		chest = found.get();
		if (startCount >= request.count()) {
			ctx.advance();
			return;
		}
		// Permitted perception of the contents: the inspect operation is gated like any other.
		InteractionOutcome inspected = ctx.interactions().perform(chest,
				InteractionRequest.inspect(request.itemId(), request.messageId()));
		if (!inspected.performed()) {
			decline(ctx, inspected.code());
			ctx.fail(inspected.code(), inspected.detail());
			return;
		}
		int available = inspected.intFact(request.itemId(), 0);
		if (available < request.count()) {
			decline(ctx, "stock_unavailable");
			ctx.fail("stock_unavailable", chest.objectId() + " holds " + available + " "
					+ request.itemId() + ", request needs " + request.count());
			return;
		}
		if (Peers.agent(ctx.view(), ctx.social(), request.senderId(), REQUESTER_RADIUS).isEmpty()) {
			decline(ctx, "requester_not_in_sight");
			ctx.fail("requester_not_in_sight", "cannot see " + request.senderId());
			return;
		}
		ctx.social().bus().accept(ctx.agentId(), request.senderId(), request, ctx.view().tick());
		ctx.note(EvidenceKind.SOCIAL, "accepted",
				"accepted " + request.messageId() + " from " + request.senderId() + " with "
						+ available + " " + request.itemId() + " in " + chest.objectId());
		ctx.advance();
	}

	private void withdraw(SkillContext ctx) {
		int needed = request.count() - ctx.countItem(request.itemId());
		if (needed <= 0) {
			ctx.advance();
			return;
		}
		InteractionOutcome outcome = ctx.interactions().perform(chest,
				InteractionRequest.withdraw(request.itemId(), needed, request.messageId()));
		if (!outcome.performed()) {
			ctx.fail(outcome.code(), outcome.detail());
			return;
		}
		withdrawOutcome = outcome;
		int moved = outcome.intFact("moved", 0);
		if (moved != needed) {
			ctx.fail("withdraw_short", "needed " + needed + " but moved " + moved);
			return;
		}
		ctx.recordContainerMove(request.messageId(), chest.pos(), InteractionRequest.WITHDRAW,
				request.itemId(), moved);
		ctx.advance();
	}

	private void approach(SkillContext ctx) {
		if (!ctx.idle()) {
			return;
		}
		if (ctx.issued()) {
			if (ctx.lastFailed()) {
				ctx.fail("requester_unreachable", ctx.last().code() + ": " + ctx.last().detail());
				return;
			}
			ctx.clearResult();
			ctx.advance();
			return;
		}
		Optional<Peers.Peer> requester = Peers.agent(ctx.view(), ctx.social(), request.senderId(),
				REQUESTER_RADIUS);
		if (requester.isEmpty()) {
			ctx.fail("requester_lost", "lost sight of " + request.senderId() + " while approaching");
			return;
		}
		if (requester.get().distance() <= DELIVERY_DISTANCE) {
			ctx.advance();
			return;
		}
		ctx.issue(new MotorCommand.MoveTo(requester.get().pos(), 2.5, 240));
	}

	private void drop(SkillContext ctx) {
		if (!ctx.idle()) {
			return;
		}
		if (ctx.issued()) {
			if (ctx.lastFailed()) {
				ctx.fail("drop_failed", ctx.last().code() + ": " + ctx.last().detail());
				return;
			}
			ctx.clearResult();
			ctx.advance();
			return;
		}
		ctx.issue(new MotorCommand.DropItem(request.itemId(), request.count(), 40));
	}

	private void verify(SkillContext ctx) {
		int now = ctx.countItem(request.itemId());
		int dropped = startCount + request.count() - now;
		int containerBefore = withdrawOutcome.intFact("container_before", -1);
		int containerAfter = withdrawOutcome.intFact("container_after", -1);
		Map<String, String> facts = new LinkedHashMap<>();
		facts.put("transfer_id", request.messageId());
		facts.put("item", request.itemId());
		facts.put("dropped", Integer.toString(dropped));
		facts.put("container", chest.objectId());
		facts.put("container_before", Integer.toString(containerBefore));
		facts.put("container_after", Integer.toString(containerAfter));
		facts.put("inventory_before", Integer.toString(startCount));
		facts.put("inventory_after", Integer.toString(now));
		facts.put("requester", request.senderId());
		if (dropped != request.count() || containerBefore - containerAfter != request.count()) {
			ctx.fail("handover_unverified", "dropped " + dropped + " of " + request.count()
					+ "; container " + containerBefore + "->" + containerAfter
					+ "; inventory " + startCount + "->" + now);
			return;
		}
		ctx.complete("supplied " + dropped + " " + request.itemId() + " from " + chest.objectId()
				+ " to " + request.senderId(), facts);
	}

	private void decline(SkillContext ctx, String reason) {
		ctx.social().bus().decline(ctx.agentId(), request.senderId(), request, reason,
				ctx.view().tick());
		ctx.note(EvidenceKind.SOCIAL, "declined", reason + " for " + request.messageId());
	}

	/** The nearest perceived chest this body could draw an item from. */
	private Optional<InteractionTarget> storage(SkillContext ctx) {
		return ctx.interactions().targets(STORAGE_RADIUS).stream()
				.filter(target -> target.supports(InteractionRequest.WITHDRAW))
				.findFirst();
	}
}

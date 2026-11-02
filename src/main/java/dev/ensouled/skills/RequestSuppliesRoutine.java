package dev.ensouled.skills;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import dev.ensouled.core.AgentMessage;
import dev.ensouled.core.EvidenceKind;
import dev.ensouled.core.MessageBus;
import dev.ensouled.core.MotorCommand;
import dev.ensouled.core.Peers;
import dev.ensouled.core.Phase1GatePolicy;
import dev.ensouled.core.SkillContext;
import dev.ensouled.core.SkillRoutine;
import dev.ensouled.interact.InteractionOutcome;
import dev.ensouled.interact.InteractionRequest;
import dev.ensouled.interact.InteractionTarget;

/**
 * request_supplies@1 — a being with a stock requirement states it to a perceived peer, waits for
 * that peer's own answer, takes the item the peer actually drops, then stows it in its own storage
 * chest through the interaction contract. Every step is verified against this being's own world
 * state: nothing is believed because a peer said so, and the deposit is recorded once per transfer
 * id from the container and inventory deltas the adapter observed.
 *
 * <p>The requirement is grounded in perception, not in a hidden goal: the body holds less than the
 * declared stock and can see a storage chest that does not hold it either. It cannot see the peer's
 * chest, so the peer's stock is unknown to it — the request is speech, and the answer is that peer's
 * decision.
 */
public final class RequestSuppliesRoutine implements SkillRoutine {
	public static final String ITEM = "minecraft:oak_log";
	public static final int QUANTITY = 2;
	/** Repeated asking of the same peer inside this window is suppressed. */
	public static final int ASK_INTERVAL_TICKS = 1200;
	/** How far this body can look for a storage chest. */
	public static final int STORAGE_RADIUS = 6;

	private static final int REPLY_WAIT_TICKS = 600;
	private static final int DELIVERY_WAIT_TICKS = 900;
	private static final int PEER_RADIUS = 12;

	private String peerId = "";
	private String transferId = "";
	private long openedTick;
	private long acceptedTick;
	private int startCount;
	private InteractionTarget storage;
	private InteractionOutcome stowOutcome;

	@Override
	public String skillId() {
		return Phase1GatePolicy.SKILL_REQUEST_SUPPLIES;
	}

	@Override
	public List<String> unmetPreconditions(SkillContext ctx) {
		List<String> missing = new ArrayList<>();
		if (ctx.countItem(ITEM) >= QUANTITY) {
			missing.add("already_supplied");
		}
		Optional<InteractionTarget> chest = storage(ctx);
		if (chest.isEmpty()) {
			missing.add("storage_in_sight");
		} else {
			// The deficit must be real: the being reads its own chest through the gate before asking.
			InteractionOutcome stocked = ctx.interactions().perform(chest.get(),
					InteractionRequest.inspect(ITEM, "precondition"));
			if (stocked.performed() && stocked.intFact(ITEM, 0) >= QUANTITY) {
				missing.add("storage_already_stocked");
			}
		}
		Optional<Peers.Peer> peer = Peers.nearest(ctx.view(), ctx.social(), ctx.agentId(), PEER_RADIUS);
		if (peer.isEmpty()) {
			missing.add("peer_in_sight");
		} else if (ctx.social().bus().requestedWithin(ctx.agentId(), peer.get().agentId(),
				ctx.view().tick(), ASK_INTERVAL_TICKS)) {
			missing.add("already_asked");
		}
		return missing;
	}

	@Override
	public void begin(SkillContext ctx) {
		Optional<Peers.Peer> peer = Peers.nearest(ctx.view(), ctx.social(), ctx.agentId(), PEER_RADIUS);
		Optional<InteractionTarget> chest = storage(ctx);
		if (peer.isEmpty() || chest.isEmpty()) {
			ctx.fail("requirement_unbound", "peer or storage chest is no longer perceived");
			return;
		}
		peerId = peer.get().agentId();
		storage = chest.get();
		openedTick = ctx.view().tick();
		startCount = ctx.countItem(ITEM);
		AgentMessage request = ctx.social().bus().request(ctx.agentId(), peerId, ITEM, QUANTITY,
				ctx.intention().id(), openedTick);
		transferId = request.messageId();
		ctx.note(EvidenceKind.SOCIAL, "requested",
				"asked " + peerId + " for " + QUANTITY + " " + ITEM + " (" + transferId + ")");
	}

	@Override
	public void tick(SkillContext ctx) {		switch (ctx.step()) {
			case 0 -> awaitReply(ctx);
			case 1 -> awaitDelivery(ctx);
			case 2 -> approachStorage(ctx);
			case 3 -> stow(ctx);
			case 4 -> verifyStow(ctx);
			default -> ctx.fail("supply_state", "unexpected step " + ctx.step());
		}
	}

	private void awaitReply(SkillContext ctx) {
		MessageBus bus = ctx.social().bus();
		long now = ctx.view().tick();
		Optional<AgentMessage> declined = bus.find(ctx.agentId(), AgentMessage.DECLINE, transferId, now);
		if (declined.isPresent()) {
			ctx.fail("peer_declined", declined.get().senderId() + ": " + declined.get().reason());
			return;
		}
		Optional<AgentMessage> accepted = bus.find(ctx.agentId(), AgentMessage.ACCEPT, transferId, now);
		if (accepted.isPresent()) {
			acceptedTick = now;
			ctx.note(EvidenceKind.SOCIAL, "accepted",
					accepted.get().senderId() + " accepted " + transferId);
			ctx.advance();
			return;
		}
		if (now - openedTick > REPLY_WAIT_TICKS) {
			ctx.fail("request_timeout", "no answer from " + peerId + " for " + transferId);
		}
	}

	private void awaitDelivery(SkillContext ctx) {
		long now = ctx.view().tick();
		int held = ctx.countItem(ITEM);
		if (held >= startCount + QUANTITY) {
			ctx.advance();
			return;
		}
		if (now - acceptedTick > DELIVERY_WAIT_TICKS) {
			ctx.fail("delivery_timeout", peerId + " accepted " + transferId
					+ " but nothing arrived; inventory " + startCount + "->" + held);
			return;
		}
		if (!ctx.idle()) {
			return;
		}
		if (ctx.issued()) {
			// The finished pickup attempt is consumed here; a new one starts on the next slice.
			ctx.clearResult();
		}
		// Only chase something this body can actually see; re-issuing a sweep over empty ground just
		// walks the being away from the chest it will need afterwards.
		boolean dropInSight = ctx.view().droppedItems(8).stream()
				.anyMatch(item -> item.itemId().equals(ITEM));
		if (!dropInSight) {
			return;
		}
		// Walk to whatever the peer actually dropped; pickup stays a real player touch.
		ctx.issue(new MotorCommand.CollectDrops(6, 120));
	}

	/** Walk back to the chest the requirement was about; a container is only usable within reach. */
	private void approachStorage(SkillContext ctx) {
		Optional<InteractionTarget> chest = sameObject(ctx, storage);
		if (chest.isPresent()) {
			storage = chest.get();
			if (storage.reachable()) {
				ctx.advance();
				return;
			}
		} else if (ctx.view().bodyPos().distanceTo(storage.pos()) <= 2.5) {
			ctx.fail("storage_lost", "the chest this requirement was about is no longer perceived: "
					+ storage.objectId() + " at " + storage.pos());
			return;
		}
		// The pickup can carry the body out of the chest's perception radius. Walk back to the exact
		// object this requirement was about; never substitute whatever container is nearest now.
		if (!ctx.idle()) {
			return;
		}
		if (ctx.issued()) {
			if (ctx.lastFailed()) {
				ctx.fail("storage_unreachable", ctx.last().code() + ": " + ctx.last().detail());
				return;
			}
			ctx.clearResult();
		}
		ctx.issue(new MotorCommand.MoveTo(storage.pos(), 2.0, 240));
	}

	private void stow(SkillContext ctx) {
		int held = ctx.countItem(ITEM);
		if (held <= 0) {
			ctx.fail("nothing_to_stow", "the received items are no longer in the inventory");
			return;
		}
		// The stock goes into the chest this requirement was about, never into whichever container is
		// nearest now: after the handover the peer's own chest is closer, and silently stowing there
		// would leave the requirement unsatisfied while looking successful.
		Optional<InteractionTarget> chest = sameObject(ctx, storage);
		if (chest.isEmpty()) {
			ctx.fail("storage_lost", "the chest this requirement was about is no longer perceived: "
					+ storage.objectId());
			return;
		}
		storage = chest.get();
		InteractionOutcome outcome = ctx.interactions().perform(storage,
				InteractionRequest.deposit(ITEM, Math.min(held, QUANTITY), transferId));
		if (!outcome.performed()) {
			ctx.fail(outcome.code(), outcome.detail());
			return;
		}
		stowOutcome = outcome;
		ctx.advance();
	}

	private void verifyStow(SkillContext ctx) {
		int moved = stowOutcome.intFact("moved", 0);
		int containerBefore = stowOutcome.intFact("container_before", -1);
		int containerAfter = stowOutcome.intFact("container_after", -1);
		int held = ctx.countItem(ITEM);
		Map<String, String> facts = new LinkedHashMap<>();
		facts.put("transfer_id", transferId);
		facts.put("item", ITEM);
		facts.put("received", Integer.toString(moved));
		facts.put("peer", peerId);
		facts.put("container", storage.objectId());
		facts.put("container_before", Integer.toString(containerBefore));
		facts.put("container_after", Integer.toString(containerAfter));
		facts.put("inventory_after", Integer.toString(held));
		if (moved <= 0 || containerAfter - containerBefore != moved || held != startCount) {
			ctx.fail("stow_unverified", "container " + containerBefore + "->" + containerAfter
					+ " moved=" + moved + " inventory=" + startCount + "->" + held);
			return;
		}
		ctx.recordContainerMove(transferId, storage.pos(), InteractionRequest.DEPOSIT, ITEM, moved);
		ctx.complete("received " + moved + " " + ITEM + " from " + peerId + " and stowed it in "
				+ storage.objectId(), facts);
	}

	private static Optional<InteractionTarget> sameObject(SkillContext ctx, InteractionTarget wanted) {
		return ctx.interactions().targets(STORAGE_RADIUS).stream()
				.filter(target -> target.objectId().equals(wanted.objectId()))
				.filter(target -> target.supports(InteractionRequest.DEPOSIT))
				.findFirst();
	}

	/** The nearest perceived chest this body could put an item into. */
	private Optional<InteractionTarget> storage(SkillContext ctx) {
		return ctx.interactions().targets(STORAGE_RADIUS).stream()
				.filter(target -> target.supports(InteractionRequest.DEPOSIT))
				.findFirst();
	}
}

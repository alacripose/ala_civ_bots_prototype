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
import dev.ensouled.core.SocialStream;

/**
 * request_food@1 — a hungry being with nothing to eat asks a perceived agent peer for bread, waits
 * for that peer's own answer, then takes the item the peer actually drops and verifies its own
 * inventory delta. A decline or silence is a typed failure, never a delivery.
 */
public final class RequestFoodRoutine implements SkillRoutine {
	public static final String ITEM = "minecraft:bread";
	public static final int QUANTITY = 1;
	/** Repeated asking of the same peer inside this window is suppressed. */
	public static final int ASK_INTERVAL_TICKS = 1200;

	private static final int REPLY_WAIT_TICKS = 400;
	private static final int DELIVERY_WAIT_TICKS = 600;
	private static final int PEER_RADIUS = 12;

	private String peerId = "";
	private String transferId = "";
	private long openedTick;
	private long acceptedTick;
	private int startCount;

	@Override
	public String skillId() {
		return Phase1GatePolicy.SKILL_REQUEST_FOOD;
	}

	@Override
	public List<String> unmetPreconditions(SkillContext ctx) {
		List<String> missing = new ArrayList<>();
		if (!ctx.view().needs().hungry()) {
			missing.add("not_hungry");
		}
		if (ctx.countItem(ITEM) >= QUANTITY) {
			missing.add("already_supplied");
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
		if (peer.isEmpty()) {
			ctx.fail("peer_lost", "no agent peer is visible");
			return;
		}
		peerId = peer.get().agentId();
		openedTick = ctx.view().tick();
		startCount = ctx.countItem(ITEM);
		AgentMessage request = ctx.social().bus().request(ctx.agentId(), peerId, ITEM, QUANTITY,
				ctx.intention().id(), openedTick);
		transferId = request.messageId();
		ctx.note(EvidenceKind.SOCIAL, "requested",
				"asked " + peerId + " for " + QUANTITY + " " + ITEM + " (" + transferId + ")");
		// The ask is on this being's own stream, not only in the bounded evidence ring: after a few
		// hundred ticks the ring has rotated and the exchange would have no record at all.
		SocialStream.requested(ctx.agentId(), peerId, ITEM, QUANTITY, transferId, openedTick);
	}

	@Override
	public void tick(SkillContext ctx) {
		switch (ctx.step()) {
			case 0 -> awaitReply(ctx);
			case 1 -> awaitDelivery(ctx);
			default -> ctx.fail("handoff_state", "unexpected step " + ctx.step());
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
			SocialStream.accepted(ctx.agentId(), accepted.get().senderId(), ITEM, QUANTITY, transferId,
					now);
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
			Map<String, String> facts = new LinkedHashMap<>();
			facts.put("transfer_id", transferId);
			facts.put("item", ITEM);
			facts.put("count", Integer.toString(held - startCount));
			facts.put("inventory_before", Integer.toString(startCount));
			facts.put("inventory_after", Integer.toString(held));
			SocialStream.received(ctx.agentId(), peerId, ITEM, held - startCount, transferId, now);
			ctx.complete("received " + (held - startCount) + " " + ITEM + " from " + peerId, facts);
			return;
		}
		if (now - acceptedTick > DELIVERY_WAIT_TICKS) {
			ctx.fail("delivery_timeout", peerId + " accepted " + transferId
					+ " but nothing arrived; inventory " + startCount + "->" + held);
			return;
		}
		if (ctx.idle()) {
			// Walk to whatever the peer actually dropped; pickup stays a real player touch.
			ctx.issue(new MotorCommand.CollectDrops(6, 120));
		}
	}
}

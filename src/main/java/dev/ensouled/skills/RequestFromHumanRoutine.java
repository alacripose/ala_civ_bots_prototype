package dev.ensouled.skills;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import dev.ensouled.core.AgentMessage;
import dev.ensouled.core.DroppedItem;
import dev.ensouled.core.EvidenceKind;
import dev.ensouled.core.HumanChannel;
import dev.ensouled.core.Humans;
import dev.ensouled.core.MotorCommand;
import dev.ensouled.core.Phase1GatePolicy;
import dev.ensouled.core.SkillContext;
import dev.ensouled.core.SkillRoutine;
import dev.ensouled.interact.InteractionOutcome;
import dev.ensouled.interact.InteractionRequest;
import dev.ensouled.interact.InteractionTarget;

/**
 * request_from_human@1 — a being with a stock requirement addresses a human it can see in ordinary
 * chat, then treats the human's own physical reply as the answer: the human drops the requested
 * items, the being perceives them on the ground, walks over, takes them and stows them in its own
 * chest through the interaction contract.
 *
 * <p>The human's side is not scripted anywhere in production code. The server only hands the
 * utterance to that session's chat; whether the human answers, ignores it or drops something else is
 * the human's decision, and the being verifies what actually appeared in the world rather than what
 * it asked for. An unanswered request is a typed failure ({@code human_silent}), not a retry loop.
 *
 * <p>A peer agent that can hear the same requirement is asked first, over the typed bus; this skill
 * is the human-facing fallback for a being with no peer in sight.
 */
public final class RequestFromHumanRoutine implements SkillRoutine {
	public static final String ITEM = "minecraft:oak_log";
	public static final int QUANTITY = 2;
	/** Conversation distance: how far this being can address one human and see the reply. */
	public static final int HUMAN_RADIUS = 8;
	public static final int ASK_INTERVAL_TICKS = 1200;
	/**
	 * How long a person's own "no" keeps this being from asking them again. The point is not to
	 * punish the person, it is that an answered request is finished: the being returns to its own
	 * life instead of turning the conversation into a retry loop.
	 */
	public static final int DECLINE_QUIET_TICKS = 4800;
	/** Words a person uses to refuse; matched as whole words so "north" is not heard as "no". */
	private static final java.util.Set<String> REFUSALS = java.util.Set.of("no", "nope", "none",
			"nah", "negative", "dont", "wont", "cant", "cannot", "never", "refuse", "stop");

	private static final int REPLY_WAIT_TICKS = 900;
	/** How far from the body a chest must be for a stated condition to be meetable. */
	public static final int STORAGE_RADIUS = 6;
	private static final int PICKUP_RADIUS = 4;
	private static final int MAX_COLLECT_ATTEMPTS = 3;

	private Humans.Human human;
	private String transferId = "";
	private long askedTick;
	private int startCount;
	private InteractionTarget storage;
	private DroppedItem drop;
	private int collectAttempts;
	private InteractionOutcome stowOutcome;

	@Override
	public String skillId() {
		return Phase1GatePolicy.SKILL_REQUEST_FROM_HUMAN;
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
			InteractionOutcome stocked = ctx.interactions().perform(chest.get(),
					InteractionRequest.inspect(ITEM, "precondition"));
			if (stocked.performed() && stocked.intFact(ITEM, 0) >= QUANTITY) {
				missing.add("storage_already_stocked");
			}
		}
		Optional<Humans.Human> nearby = Humans.nearest(ctx.view(), ctx.social(), HUMAN_RADIUS);
		if (nearby.isEmpty()) {
			missing.add("human_in_sight");
		} else if (ctx.social().bus().requestedWithin(ctx.agentId(), "human:" + nearby.get().entityUuid(),
				ctx.view().tick(), ASK_INTERVAL_TICKS)) {
			missing.add("already_asked");
		} else if (ctx.social().bus().declinedWithin(ctx.agentId(),
				"human:" + nearby.get().entityUuid(), ctx.view().tick(), DECLINE_QUIET_TICKS)) {
			// An answered request stays answered: the person already said no in their own words.
			missing.add("human_declined_recently");
		}
		return missing;
	}

	@Override
	public void begin(SkillContext ctx) {
		Optional<Humans.Human> nearby = Humans.nearest(ctx.view(), ctx.social(), HUMAN_RADIUS);
		Optional<InteractionTarget> chest = storage(ctx);
		Optional<HumanChannel> channel = ctx.social().humans();
		if (nearby.isEmpty() || chest.isEmpty() || channel.isEmpty()) {
			ctx.fail("human_unavailable", "the human, the storage chest or the human channel is gone");
			return;
		}
		human = nearby.get();
		storage = chest.get();
		askedTick = ctx.view().tick();
		startCount = ctx.countItem(ITEM);
		// The utterance is recorded on the same bus as agent speech, addressed to a human session
		// that has no inbox: it is an audit record and a rate limit, never a message a mind reads.
		AgentMessage request = ctx.social().bus().request(ctx.agentId(), "human:" + human.entityUuid(),
				ITEM, QUANTITY, ctx.intention().id(), askedTick);
		transferId = request.messageId();
		String text = ctx.agentName() + " needs " + QUANTITY + " oak_log (transfer " + transferId + "). "
				+ human.name() + ", drop " + QUANTITY + " oak_log beside me and I will stow them.";
		boolean spoken = channel.get().tell(human.entityUuid(), text);
		ctx.note(EvidenceKind.SOCIAL, spoken ? "told_human" : "human_unreachable",
				human.name() + " (" + human.entityUuid() + ") asked for " + QUANTITY + " " + ITEM
						+ " as " + transferId + ": " + text);
		if (!spoken) {
			ctx.fail("human_unreachable", human.name() + " is not connected to receive " + transferId);
		}
	}

	@Override
	public void tick(SkillContext ctx) {
		switch (ctx.step()) {
			case 0 -> awaitSupply(ctx);
			case 1 -> approachDrop(ctx);
			case 2 -> collectDrop(ctx);
			case 3 -> approachStorage(ctx);
			case 4 -> stow(ctx);
			default -> ctx.fail("human_supply_state", "unexpected step " + ctx.step());
		}
	}

	/**
	 * The answer can be words, not only a drop. When the person this being asked refuses in ordinary
	 * chat, the request is over: the refusal is recorded with its provenance, the exchange ends with
	 * a typed failure, and the being says so back instead of waiting out the silence or asking again.
	 */
	@Override
	public boolean hear(SkillContext ctx, String speakerId, String speakerName, String text) {
		if (human == null || !human.entityUuid().equals(speakerId)) {
			return false;
		}
		if (ctx.step() != 0 || !refusal(text)) {
			return false;
		}
		ctx.social().bus().recordHumanAnswer("human:" + speakerId, ctx.agentId(), ITEM,
				speakerName + " said: " + text, ctx.view().tick(), DECLINE_QUIET_TICKS);
		ctx.note(EvidenceKind.SOCIAL, "human_declined",
				speakerName + " answered " + transferId + ": " + text);
		ctx.fail("human_declined", speakerName + " said \"" + text + "\" to " + transferId);
		return true;
	}

	private static boolean refusal(String text) {
		for (String word : text.toLowerCase(java.util.Locale.ROOT).split("[^a-z']+")) {
			if (REFUSALS.contains(word.replace("'", ""))) {
				return true;
			}
		}
		return false;
	}

	/** Wait for something the human actually dropped; nothing is believed because it was asked. */
	private void awaitSupply(SkillContext ctx) {
		// A real player often presses Q once per item, so the reply may arrive as several stacks.
		// What matters is the total the human put on the ground, not one tidy stack.
		List<DroppedItem> supply = ctx.view().droppedItems(HUMAN_RADIUS).stream()
				.filter(item -> item.itemId().equals(ITEM))
				.toList();
		int available = supply.stream().mapToInt(DroppedItem::count).sum();
		if (available >= QUANTITY) {
			drop = supply.get(0);
			ctx.note(EvidenceKind.SOCIAL, "human_supplied",
					human.name() + " left " + available + " " + ITEM + " at " + drop.pos()
							+ " for " + transferId);
			ctx.advance();
			return;
		}
		if (Humans.nearest(ctx.view(), ctx.social(), HUMAN_RADIUS).isEmpty()) {
			ctx.fail("human_left", human.name() + " left the area before answering " + transferId);
			return;
		}
		if (ctx.view().tick() - askedTick > REPLY_WAIT_TICKS) {
			ctx.fail("human_silent", human.name() + " did not drop " + QUANTITY + " " + ITEM
					+ " within " + REPLY_WAIT_TICKS + " ticks of " + transferId
					+ "; inventory holds " + ctx.countItem(ITEM));
		}
	}

	/** Walk to the physical reply; an item is only collectable when the body is actually on it. */
	private void approachDrop(SkillContext ctx) {
		Optional<DroppedItem> current = sameDrop(ctx);
		if (current.isEmpty()) {
			// It may have been taken by the body already (pickup happens on contact), or removed.
			ctx.advance();
			return;
		}
		if (current.get().distance() <= 1.6) {
			drop = current.get();
			ctx.advance();
			return;
		}
		if (!ctx.idle()) {
			return;
		}
		if (ctx.issued()) {
			if (ctx.lastFailed()) {
				ctx.fail("drop_unreachable", ctx.last().code() + ": " + ctx.last().detail()
						+ " (drop at " + drop.pos() + ", distance " + fmt(drop.distance()) + ")");
				return;
			}
			ctx.clearResult();
		}
		drop = current.get();
		ctx.issue(new MotorCommand.MoveTo(drop.pos(), 1.0, 160));
	}

	private void collectDrop(SkillContext ctx) {
		if (ctx.countItem(ITEM) >= startCount + QUANTITY) {
			ctx.advance();
			return;
		}
		if (!ctx.idle()) {
			return;
		}
		if (ctx.issued()) {
			if (ctx.lastFailed()) {
				ctx.fail("pickup_failed", ctx.last().code() + ": " + ctx.last().detail()
						+ " (transfer " + transferId + ", drop " + describeDrop(ctx) + ")");
				return;
			}
			ctx.clearResult();
		}
		if (collectAttempts >= MAX_COLLECT_ATTEMPTS) {
			ctx.fail("pickup_unverified", "took " + (ctx.countItem(ITEM) - startCount) + " of " + QUANTITY
					+ " for " + transferId + "; " + describeDrop(ctx));
			return;
		}
		Optional<DroppedItem> remaining = ctx.view().droppedItems(HUMAN_RADIUS).stream()
				.filter(item -> item.itemId().equals(ITEM))
				.findFirst();
		if (remaining.isEmpty()) {
			ctx.fail("pickup_unverified", "took " + (ctx.countItem(ITEM) - startCount) + " of " + QUANTITY
					+ " for " + transferId + "; no " + ITEM + " remains on the ground");
			return;
		}
		drop = remaining.get();
		if (drop.distance() > PICKUP_RADIUS - 1) {
			// A second stack may have landed outside one pickup sweep; walk to it first.
			ctx.issue(new MotorCommand.MoveTo(drop.pos(), 1.5, 120));
			return;
		}
		collectAttempts++;
		ctx.issue(new MotorCommand.CollectDrops(PICKUP_RADIUS, 120));
	}

	/** Walk back to the chest this requirement was about; reach is part of the contract. */
	private void approachStorage(SkillContext ctx) {
		Optional<InteractionTarget> chest = sameObject(ctx, storage);
		if (chest.isPresent()) {
			storage = chest.get();
			if (storage.reachable()) {
				ctx.advance();
				return;
			}
		} else if (ctx.view().bodyPos().distanceTo(storage.pos()) <= 2.5) {
			ctx.fail("storage_lost", "the chest for " + transferId + " is no longer perceived: "
					+ storage.objectId() + " at " + storage.pos());
			return;
		}
		// Picking the drop up can carry the body past the chest's perception radius; walk back to the
		// exact object this requirement named instead of failing or grabbing the nearest container.
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
			ctx.fail("nothing_to_stow", "the items the human supplied are no longer held");
			return;
		}
		Optional<InteractionTarget> chest = sameObject(ctx, storage);
		if (chest.isEmpty()) {
			ctx.fail("storage_lost", "the chest for " + transferId + " is no longer perceived: "
					+ storage.objectId());
			return;
		}
		storage = chest.get();
		// Stow everything the human actually gave, so the body's own count returns to where it started
		// even when the human dropped more than the stated quantity.
		InteractionOutcome outcome = ctx.interactions().perform(storage,
				InteractionRequest.deposit(ITEM, held, transferId));
		if (!outcome.performed()) {
			ctx.fail(outcome.code(), outcome.detail());
			return;
		}
		stowOutcome = outcome;
		int moved = outcome.intFact("moved", 0);
		int containerBefore = outcome.intFact("container_before", -1);
		int containerAfter = outcome.intFact("container_after", -1);
		int after = ctx.countItem(ITEM);
		Map<String, String> facts = new LinkedHashMap<>();
		facts.put("transfer_id", transferId);
		facts.put("item", ITEM);
		facts.put("received", Integer.toString(moved));
		facts.put("human", human.name());
		facts.put("human_uuid", human.entityUuid());
		facts.put("container", storage.objectId());
		facts.put("container_before", Integer.toString(containerBefore));
		facts.put("container_after", Integer.toString(containerAfter));
		facts.put("inventory_before", Integer.toString(startCount));
		facts.put("inventory_after", Integer.toString(after));
		if (moved < QUANTITY || containerAfter - containerBefore != moved || after != startCount) {
			ctx.fail("stow_unverified", "container " + containerBefore + "->" + containerAfter
					+ " moved=" + moved + " inventory=" + startCount + "->" + after);
			return;
		}
		ctx.recordContainerMove(transferId, storage.pos(), InteractionRequest.DEPOSIT, ITEM, moved);
		confirm(ctx, moved);
		ctx.complete("received " + moved + " " + ITEM + " from " + human.name() + " and stowed it in "
				+ storage.objectId(), facts);
	}

	/** Best effort: the physical transfer is already verified, so a lost session is evidence only. */
	private void confirm(SkillContext ctx, int moved) {
		Optional<HumanChannel> channel = ctx.social().humans();
		if (channel.isEmpty()) {
			return;
		}
		String text = ctx.agentName() + " received " + moved + " oak_log from " + human.name()
				+ " and stowed them in my chest (transfer " + transferId + ").";
		boolean told = channel.get().tell(human.entityUuid(), text);
		ctx.note(EvidenceKind.SOCIAL, told ? "confirmed_to_human" : "confirmation_undelivered",
				"transfer " + transferId + ": " + text);
	}

	private Optional<DroppedItem> sameDrop(SkillContext ctx) {
		if (drop == null) {
			return Optional.empty();
		}
		return ctx.view().droppedItems(HUMAN_RADIUS).stream()
				.filter(item -> item.entityId().equals(drop.entityId()))
				.filter(item -> item.itemId().equals(ITEM))
				.findFirst();
	}

	private String describeDrop(SkillContext ctx) {
		Optional<DroppedItem> remaining = ctx.view().droppedItems(HUMAN_RADIUS).stream()
				.filter(item -> item.itemId().equals(ITEM))
				.findFirst();
		if (remaining.isEmpty()) {
			return "no " + ITEM + " is on the ground any more";
		}
		DroppedItem item = remaining.get();
		return "drop still at " + item.pos() + " (" + item.count() + " " + ITEM + ", "
				+ fmt(item.distance()) + " blocks away)";
	}

	private static Optional<InteractionTarget> sameObject(SkillContext ctx, InteractionTarget wanted) {
		return ctx.interactions().targets(STORAGE_RADIUS).stream()
				.filter(target -> target.objectId().equals(wanted.objectId()))
				.filter(target -> target.supports(InteractionRequest.DEPOSIT))
				.findFirst();
	}

	private Optional<InteractionTarget> storage(SkillContext ctx) {
		return ctx.interactions().targets(STORAGE_RADIUS).stream()
				.filter(target -> target.supports(InteractionRequest.DEPOSIT))
				.findFirst();
	}

	private static String fmt(double value) {
		return String.format("%.2f", value);
	}
}

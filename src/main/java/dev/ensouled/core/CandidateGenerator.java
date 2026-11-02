package dev.ensouled.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import dev.ensouled.interact.InteractionRequest;
import dev.ensouled.interact.InteractionTarget;
import dev.ensouled.skills.RequestFoodRoutine;
import dev.ensouled.skills.RequestFromHumanRoutine;
import dev.ensouled.skills.RequestSuppliesRoutine;
import dev.ensouled.skills.WanderRoutine;
import dev.ensouled.work.WorkLedger;

/**
 * Candidates are retrieved from declared schema + perceived world state, not generated as
 * unrestricted goals (§5.2). Every candidate is something the being can see and could do now.
 */
public final class CandidateGenerator {
	public static final int SIGHT_RADIUS = 16;
	private static final int MAX_HUNGER_RELIEF = 900;

	public List<Candidate> generate(WorldView view, AgentMind mind) {
		List<Candidate> out = new ArrayList<>();
		long now = view.tick();
		Vec3i body = view.bodyPos();
		String actor = mind.identity().agentId();

		java.util.Set<Vec3i> seenSites = new java.util.HashSet<>();
		for (VisibleBlock block : view.visibleBlocks(SIGHT_RADIUS)) {
			if (!BlockIds.isNaturalLog(block.blockId()) || !block.lineOfSight()) {
				continue;
			}
			Vec3i site = TreeSites.base(block.pos(), view);
			// One tree is one site: remember it by its trunk base, not per log block.
			if (!seenSites.add(site)) {
				continue;
			}
			if (mind.isSiteAlreadyCollected(site, view)) {
				continue;
			}
			int distance = (int) Math.round(body.distanceTo(site));
			int travel = distance * 45;
			boolean reachable = view.canReach(site);
			int reachPenalty = reachable ? 0 : 400;
			out.add(new Candidate(
					"cand:harvest:" + site,
					Phase1GatePolicy.SKILL_HARVEST,
					site,
					block.blockId(),
					0,
					// A commitment the being holds makes the step that serves it worth more than
					// habit or curiosity: this is the only place the long-horizon work reaches the
					// cycle, and it reaches it as utility, not as an order.
					work(actor, Phase1GatePolicy.SKILL_HARVEST),
					650,
					mind.ledger().stats(Phase1GatePolicy.SKILL_HARVEST).habitStrength(),
					120,
					40,
					0,
					travel,
					reachPenalty,
					1,
					now));
		}

		if (view.needs().hungry() && hasEdible(view)) {
			out.add(new Candidate(
					"cand:eat:" + body,
					Phase1GatePolicy.SKILL_EAT,
					body,
					"",
					900 - view.needs().food() * 30,
					0,
					300,
					mind.ledger().stats(Phase1GatePolicy.SKILL_EAT).habitStrength(),
					0,
					0,
					100,
					0,
					0,
					3,
					now));
		}

		String selfId = mind.identity().agentId();
		// A hungry being with nothing to eat may ask a peer it can see. It cannot know what the peer
		// holds: the request is speech, and the peer decides from its own inventory.
		if (view.needs().hungry() && !hasEdible(view)) {
			Optional<Peers.Peer> peer = Peers.nearest(view, mind.social(), selfId, 12);
			if (peer.isPresent() && !mind.social().bus().requestedWithin(selfId, peer.get().agentId(),
					now, RequestFoodRoutine.ASK_INTERVAL_TICKS)) {
				out.add(new Candidate(
						"cand:ask:" + peer.get().agentId(),
						Phase1GatePolicy.SKILL_REQUEST_FOOD,
						peer.get().pos(),
						RequestFoodRoutine.ITEM,
						MAX_HUNGER_RELIEF - view.needs().food() * 30,
						0,
						300,
						mind.ledger().stats(Phase1GatePolicy.SKILL_REQUEST_FOOD).habitStrength(),
						100,
						0,
						0,
						(int) Math.round(peer.get().distance()) * 20,
						0,
						2,
						now));
			}
		}

		// A body that can see its own storage chest may find it short of the stock it keeps there. The
		// deficit is perceptible (its own chest, its own inventory); the peer's stock is not, so asking
		// is speech and the answer is that peer's own decision.
		List<InteractionTarget> objects = mind.perceivedObjects(RequestSuppliesRoutine.STORAGE_RADIUS);
		Optional<InteractionTarget> storage = objects.stream()
				.filter(target -> target.supports(InteractionRequest.DEPOSIT))
				.findFirst();
		Optional<Peers.Peer> peer = Peers.nearest(view, mind.social(), selfId, 12);
		boolean stockDeficit = storage.isPresent()
				&& view.countItem(RequestSuppliesRoutine.ITEM) < RequestSuppliesRoutine.QUANTITY
				&& mind.storedCount(view, storage.get(), RequestSuppliesRoutine.ITEM) < RequestSuppliesRoutine.QUANTITY;
		if (stockDeficit && peer.isPresent() && !mind.social().bus().requestedWithin(selfId,
				peer.get().agentId(), now, RequestSuppliesRoutine.ASK_INTERVAL_TICKS)) {
			out.add(new Candidate(
					"cand:supplies:" + storage.get().objectId(),
					Phase1GatePolicy.SKILL_REQUEST_SUPPLIES,
					storage.get().pos(),
					storage.get().blockId(),
					500,
					0,
					200,
					mind.ledger().stats(Phase1GatePolicy.SKILL_REQUEST_SUPPLIES).habitStrength(),
					60,
					0,
					0,
					(int) Math.round(storage.get().distance()) * 10,
					0,
					2,
					now));
		}
		// The same requirement may also be stated to a human present in the world. A peer is preferred
		// by utility, but a human is a legitimate recipient: the request is speech, and nothing about
		// what the human holds is known to this being.
		if (stockDeficit) {
			Optional<Humans.Human> human = Humans.nearest(view, mind.social(),
					RequestFromHumanRoutine.HUMAN_RADIUS);
			if (human.isPresent() && !mind.social().bus().requestedWithin(selfId,
					"human:" + human.get().entityUuid(), now, RequestFromHumanRoutine.ASK_INTERVAL_TICKS)) {
				out.add(new Candidate(
						"cand:human:" + human.get().entityUuid(),
						Phase1GatePolicy.SKILL_REQUEST_FROM_HUMAN,
						storage.get().pos(),
						storage.get().blockId(),
						450,
						0,
						200,
						mind.ledger().stats(Phase1GatePolicy.SKILL_REQUEST_FROM_HUMAN).habitStrength(),
						40,
						0,
						0,
						(int) Math.round(human.get().distance()) * 10,
						0,
						2,
						now));
			}
		}

		// A pending request is answered by the recipient's own decision, not by the sender's claim.
		Optional<AgentMessage> request = mind.social().bus().peek(selfId, now);
		if (request.isPresent() && AgentMessage.REQUEST.equals(request.get().speechAct())) {
			Optional<Peers.Peer> requester = Peers.agent(view, mind.social(), request.get().senderId(), 16);
			if (BlockIds.isEdible(request.get().itemId())) {
				out.add(new Candidate(
						"cand:give:" + request.get().messageId(),
						Phase1GatePolicy.SKILL_GIVE_FOOD,
						requester.map(Peers.Peer::pos).orElse(body),
						request.get().itemId(),
						700,
						0,
						200,
						mind.ledger().stats(Phase1GatePolicy.SKILL_GIVE_FOOD).habitStrength(),
						0,
						0,
						100,
						requester.map(p -> (int) Math.round(p.distance()) * 20).orElse(0),
						0,
						2,
						now));
			} else if (view.countItem(request.get().itemId()) < request.get().count()) {
				// A resource request this body does not carry is answered from storage, if it has some.
				Optional<InteractionTarget> source = objects.stream()
						.filter(target -> target.supports(InteractionRequest.WITHDRAW))
						.findFirst();
				if (source.isPresent()) {
					out.add(new Candidate(
							"cand:supply:" + request.get().messageId(),
							Phase1GatePolicy.SKILL_SUPPLY_FROM_CHEST,
							source.get().pos(),
							source.get().blockId(),
							700,
							0,
							200,
							mind.ledger().stats(Phase1GatePolicy.SKILL_SUPPLY_FROM_CHEST).habitStrength(),
							0,
							0,
							100,
							(int) Math.round(source.get().distance()) * 10,
							0,
							2,
							now));
				}
			}
		}
		// A build wish is served by putting material back, not by gathering more: the candidate is
		// the site, and the item is whatever the open ticket owes and the body actually carries.
		if (work(actor, Phase1GatePolicy.SKILL_PLACE) > 0) {
			Optional<Vec3i> site = dev.ensouled.skills.PlaceBlockRoutine.siteNear(view, body);
			Optional<String> placing = dev.ensouled.work.WorkLedger.itemForBuild(actor, view);
			if (site.isPresent() && placing.isPresent()) {
				out.add(new Candidate(
						"cand:build:" + site.get(),
						Phase1GatePolicy.SKILL_PLACE,
						site.get(),
						placing.get(),
						0,
						work(actor, Phase1GatePolicy.SKILL_PLACE),
						700,
						mind.ledger().stats(Phase1GatePolicy.SKILL_PLACE).habitStrength(),
						120,
						0,
						0,
						(int) Math.round(body.distanceTo(site.get())) * 40,
						0,
						1,
						now));
			}
		}
		// Being up and about is what a body does when it is not tired: the world is a place to walk
		// in, not a place to stand in until something urgent happens. This is always available, so a
		// being with no unmet need still has something of its own to choose.
		Optional<Vec3i> somewhere = WanderRoutine.destination(view, selfId, now);
		if (somewhere.isPresent()) {
			out.add(new Candidate(
					"cand:wander:" + somewhere.get(),
					Phase1GatePolicy.SKILL_WANDER,
					somewhere.get(),
					"",
					// A body's restlessness is a pull, not a claim on the cycle: capped, so a being that has
					// wandered a while still prefers a reachable tree to one more leg of the same walk.
					140 + Math.min(120, mind.restlessness() / 4),
					work(actor, Phase1GatePolicy.SKILL_WANDER),
					80,
					mind.ledger().stats(Phase1GatePolicy.SKILL_WANDER).habitStrength(),
					60,
					0,
					0,
					0,
					0,
					1,
					now));
		}
		// Rest is a decision the body's own weariness asks for, not the state it falls into when
		// nothing else exists. A body that is not tired has no rest candidate at all.
		if (mind.needsRest()) {
			out.add(new Candidate(
					"cand:rest:" + body,
					Phase1GatePolicy.SKILL_REST,
					body,
					"",
					// The need scales the whole way and outranks the pull of habit: a body at its limit
					// wants rest more than one more log or one more walk, while a body just past the
					// threshold only mildly prefers sitting down.
					400 + (mind.fatigue() - AgentMind.FATIGUE_REST_WANTED),
					0,
					// Resting is progress on the need it answers, not a pause in progress.
					120,
					// Rest is chosen for the need it answers, never out of habit: a long life that has
					// rested often must not make resting this being's default.
					0,
					0,
					0,
					0,
					0,
					0,
					0,
					now));
		}
		return out;
	}

	/** What the work this being owes adds to a step; zero when nothing it holds is served by it. */
	private static int work(String agentId, String skillId) {
		WorkLedger work = WorkLedger.get();
		return work == null ? 0 : work.pull(agentId, skillId);
	}

	public static boolean hasEdible(WorldView view) {
		for (InventorySlot slot : view.inventory()) {
			if (BlockIds.isEdible(slot.itemId()) && slot.count() > 0) {
				return true;
			}
		}
		return false;
	}
}

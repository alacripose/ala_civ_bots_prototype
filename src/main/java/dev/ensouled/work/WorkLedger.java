package dev.ensouled.work;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dev.ensouled.EnsouledMod;
import dev.ensouled.core.Phase1GatePolicy;
import dev.ensouled.core.StreamNotices;
import dev.ensouled.core.Vec3i;
import dev.ensouled.core.VisibleBlock;
import dev.ensouled.core.WorldView;
import dev.ensouled.domain.Run;
import dev.ensouled.domain.WorldState;
import dev.ensouled.persist.MindJournal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * The one owner of long-horizon work: a being's commitments, the plans that serve them, the windows
 * they run inside, and the run stages they advance.
 *
 * <p>This concern used to have no owner at all. A wish was noted and forgotten, a run was opened and
 * never advanced, and the only thing that outlived a single action was the mind's current intention
 * — so a resident could not hold anything across ticks, and nothing in the world's record could show
 * that work had progressed. It is deliberately outside {@code AgentMind} (which decides one action at
 * a time) and {@code AgentManager} (which owns lives and bodies): a mind reports what it perceives,
 * this decides what that means for what it owes.
 *
 * <p>Data flows one way. A wish arrives through {@link #offer}; perception arrives through
 * {@link #observe} once per tick from whichever mind owns the body; selection asks {@link #pull} what
 * a step is worth. Nothing here decides an action, moves a body or speaks on its own: it keeps the
 * books and writes the rows.
 *
 * <p>State lives in {@code world/ensouled/work.json} — the world's own directory, beside its identity
 * and its journal — because a commitment is world-lifetime, not encounter-lifetime.
 */
public final class WorkLedger {
	public static final String FILE = "work.json";
	/** How long a commitment stays workable, in world ticks (five minutes of world time). */
	public static final int WINDOW_TICKS = 6000;
	/** How long a step may go without the thing it needs in sight before the plan is replaced. */
	public static final int STEP_PATIENCE_TICKS = 600;
	/** What a commitment adds to a candidate that serves its current step. */
	public static final int PULL = 420;
	/** Bound on the record: closed commitments are history, not state. */
	private static final int MAX_CLOSED_KEPT = 64;
	private static final String STEP_WANDER = Phase1GatePolicy.SKILL_WANDER;
	private static final String STEP_HARVEST = Phase1GatePolicy.SKILL_HARVEST;
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	/**
	 * What a being made of a wish: whether it took the work on, what it says back, and why.
	 * {@code say} is the being's own sentence, composed by the caller from the corpus — this class
	 * does not speak.
	 */
	public record Answer(String code, String why, int count, String words) {
		public boolean opened() {
			return "work-opened".equals(code);
		}

		static Answer of(String code, String why) {
			return new Answer(code, why, 0, "");
		}
	}

	private static WorkLedger current;

	private final Path file;
	private final Map<String, Commitment> commitments = new LinkedHashMap<>();
	private final Map<String, Plan> plans = new LinkedHashMap<>();
	private final Map<String, ObjectiveWindow> windows = new LinkedHashMap<>();
	private final Map<String, Ticket> tickets = new LinkedHashMap<>();
	/** Blocks this being has been observed placing, per commitment: build work's own evidence. */
	private final Map<String, Integer> placedCounts = new LinkedHashMap<>();
	/** When the deliverable was last actually in sight, per commitment: a transient work heuristic. */
	private final Map<String, Long> lastSighted = new LinkedHashMap<>();

	private WorkLedger(Path file) {
		this.file = file;
		load();
	}

	/** The ledger for the world that is open, or null when no world is. */
	public static WorkLedger get() {
		WorldState world = WorldState.get();
		if (world == null) {
			return null;
		}
		Path file = world.directory().resolve(FILE);
		if (current == null || !current.file.equals(file)) {
			current = new WorkLedger(file);
		}
		return current;
	}

	/** A ledger over an explicit file: the server uses {@link #get}, a test uses this. */
	static WorkLedger at(Path file) {
		WorkLedger ledger = new WorkLedger(file);
		current = ledger;
		return ledger;
	}

	// ------------------------------------------------------------------ what a being owes

	/**
	 * Take up a wish, or say why it cannot be taken up. A wish that names a thing and a number becomes
	 * work; a wish that names only one of them is answered with a question rather than an invented
	 * reading (addendum E.4: unsupported or ambiguous requests are clarified or refused, never
	 * guessed).
	 */
	public Answer offer(String agentId, String agentName, String wishId, String text, long tick,
			WorldView view) {
		Optional<Deliverable> parsed = Deliverable.parse(text);
		if (parsed.isEmpty()) {
			record(agentName, agentId, null, "work-unclear", Map.of("wish", wishId, "said", text));
			// The named half, if one was named: the being asks about what is missing rather than
			// about the whole sentence.
			String words = Deliverable.named(text).map(kind -> kind.noun(true)).orElse("");
			return new Answer("wish-unclear", "the words did not name both a thing and a number", 0,
					words);
		}
		Deliverable deliverable = parsed.get();
		// A build wish is not answered "already held": the stock it names is what must be put
		// into the world, so holding it is the precondition, not the completion.
		boolean building = Deliverable.builds(text);
		if (!building && deliverable.met(view)) {
			record(agentName, agentId, null, "work-already-held",
					Map.of("wish", wishId, "held", Integer.toString(deliverable.held(view)),
							"count", Integer.toString(deliverable.count())));
			return new Answer("work-already-held", "already carried " + deliverable.held(view) + " of "
					+ deliverable.count(), deliverable.count(), deliverable.words());
		}
		for (Commitment open : openFor(agentId)) {
			if (open.deliverable().equals(deliverable)) {
				record(agentName, agentId, open, "work-already-held",
						Map.of("wish", wishId, "held", Integer.toString(deliverable.held(view))));
				return new Answer("work-already-held", "already owed " + deliverable.words(),
						deliverable.count(), deliverable.words());
			}
			// One commitment at a time: the new wish supersedes the old, and the old one says so.
			close(open, Commitment.State.ABANDONED, tick, "superseded by " + wishId, 0, "",
					agentName);
		}
		String worldId = WorldState.get() == null ? "" : WorldState.get().world().worldId();
		String runId = "";
		WorldState world = WorldState.get();
		if (world != null) {
			runId = world.openRun(agentId, text, tick).runId();
		}
		Commitment commitment = new Commitment("commit:" + UUID.randomUUID(), worldId, agentId,
				agentName, deliverable, "wish:" + wishId, tick, tick + WINDOW_TICKS, runId,
				Commitment.State.OPEN, 0L, "", Run.Stage.CONSIDERED.ordinal());
		commitments.put(commitment.commitmentId(), commitment);
		String step = building ? Phase1GatePolicy.SKILL_PLACE : STEP_HARVEST;
		Plan plan = new Plan("plan:" + UUID.randomUUID(), commitment.commitmentId(),
				List.of(step), 0, tick, 0L, "");
		plans.put(plan.planId(), plan);
		ObjectiveWindow window = new ObjectiveWindow("window:" + UUID.randomUUID(),
				commitment.commitmentId(), tick, commitment.dueTick(), deliverable.condition(),
				ObjectiveWindow.State.OPEN, 0L);
		windows.put(window.windowId(), window);
		Ticket ticket = Ticket.opened("ticket:" + UUID.randomUUID(), commitment.commitmentId(),
				worldId, agentId, agentName, "wish:" + wishId,
				dev.ensouled.core.VerbLibrary.corpus().spokenVerb(step),
				dev.ensouled.core.VerbLibrary.corpus().basis(step), deliverable.count(),
				tick, commitment.dueTick());
		tickets.put(ticket.ticketId(), ticket);
		record(agentName, agentId, commitment, "work-opened",
				Map.of("wish", wishId, "said", text, "run", runId,
						"count", Integer.toString(deliverable.count()),
						"due", Long.toString(commitment.dueTick())));
		record(agentName, agentId, commitment, "ticket-opened",
				Map.of("ticket", ticket.ticketId(), "cells", "0/" + ticket.declaredCells(),
						"verb", ticket.verb()));
		publish(ticket);
		save();
		return new Answer("work-opened", commitment.commitmentId(), deliverable.count(),
				deliverable.words());
	}

	// ------------------------------------------------------------------ what perception changes

	/**
	 * Apply this tick's perception to the work this being owes. Called by the mind that owns the body,
	 * once per tick; everything it decides is checkable from what was passed in, and every change is
	 * written before the next tick can overwrite it.
	 */
	public void observe(String agentId, String agentName, WorldView view, long tick) {
		if (view == null) {
			return;
		}
		boolean changed = false;
		for (Commitment commitment : openFor(agentId)) {
			int held = commitment.deliverable().held(view);
			if (held >= commitment.deliverable().count()) {
				changed |= satisfy(commitment, agentName, tick, held);
				continue;
			}
			if (tick >= commitment.dueTick()) {
				changed |= expire(commitment, agentName, tick, held);
				continue;
			}
			changed |= advanceStage(commitment, agentName, view, tick, held);
			changed |= replan(commitment, agentName, view, tick);
		}
		if (changed) {
			save();
		}
	}

	/** What a plan step is worth to this being right now, from the work it owes. */
	public int pull(String agentId, String skillId) {
		for (Commitment commitment : openFor(agentId)) {
			Plan plan = planFor(commitment);
			if (plan != null && plan.step().equals(skillId)) {
				return PULL;
			}
		}
		return 0;
	}

	// ------------------------------------------------------------------ the rules, in one place

	/**
	 * Advance the commitment's run on conditions a reader can check from the same world: seeing one of
	 * the things it owes (SURVEY), holding the first one (CLAIM), holding all of them (ESTABLISH, by
	 * {@link #satisfy}). A stage is only ever advanced forward, and only once.
	 */
	private boolean advanceStage(Commitment commitment, String agentName, WorldView view, long tick,
			int held) {
		boolean sighted = seesAny(view, commitment.deliverable());
		if (sighted) {
			lastSighted.put(commitment.commitmentId(), tick);
		}
		// One stage per step, and only the one after the stage last reached. A being that already
		// holds one of the thing it owes satisfies CLAIM's condition on its first tick, and jumping
		// straight to it left SURVEY with no row of its own: a run that reached a stage it never
		// recorded. Each transition now has its own condition and its own row, in order.
		Run.Stage next = Run.Stage.values()[Math.min(commitment.stageReached() + 1,
				Run.Stage.values().length - 1)];
		Run.Stage target = null;
		String condition = "";
		if (next == Run.Stage.SURVEY && sighted) {
			target = Run.Stage.SURVEY;
			condition = commitment.deliverable().kind().name().toLowerCase(java.util.Locale.ROOT)
					+ " in sight";
		} else if (next == Run.Stage.CLAIM && held >= 1) {
			target = Run.Stage.CLAIM;
			condition = "held " + held + "/" + commitment.deliverable().count();
		}
		if (target == null) {
			return false;
		}
		advanceRun(commitment, target, tick, condition);
		commitments.put(commitment.commitmentId(), commitment.staged(target.ordinal()));
		record(agentName, commitment.agentId(), commitment, "run-" + target.name().toLowerCase(
				java.util.Locale.ROOT),
				Map.of("stage", target.name(), "condition", condition,
						"held", Integer.toString(held)));
		return true;
	}

	/**
	 * Replace a plan whose step has stopped being workable, without touching the commitment. The
	 * being goes to look with a different step and says why, so the change of method is on the record
	 * beside the thing it still owes.
	 */
	private boolean replan(Commitment commitment, String agentName, WorldView view, long tick) {
		Plan plan = planFor(commitment);
		if (plan == null) {
			return false;
		}
		boolean sighted = seesAny(view, commitment.deliverable());
		long since = lastSighted.getOrDefault(commitment.commitmentId(),
				plan.replacedTick() > 0 ? plan.replacedTick() : plan.createdTick());
		if (sighted && STEP_WANDER.equals(plan.step())) {
			plans.put(plan.planId(), plan.replacedBy(List.of(STEP_HARVEST), tick,
					"the thing owed is in sight again"));
			record(agentName, commitment.agentId(), commitment, "plan-replaced",
					Map.of("step", STEP_HARVEST, "why", "in sight again"));
			return true;
		}
		if (!sighted && STEP_HARVEST.equals(plan.step()) && tick - since >= STEP_PATIENCE_TICKS) {
			plans.put(plan.planId(), plan.replacedBy(List.of(STEP_WANDER), tick,
					"nothing of that kind in sight for " + STEP_PATIENCE_TICKS + " ticks"));
			record(agentName, commitment.agentId(), commitment, "plan-replaced",
					Map.of("step", STEP_WANDER, "why", "nothing in sight"));
			return true;
		}
		return false;
	}

	private boolean satisfy(Commitment commitment, String agentName, long tick, int held) {
		close(commitment, Commitment.State.SATISFIED, tick, "held " + held + "/"
				+ commitment.deliverable().count(), held,
				commitment.deliverable().condition(), agentName);
		if (commitment.stageReached() < Run.Stage.ESTABLISH.ordinal()) {
			advanceRun(commitment, Run.Stage.ESTABLISH, tick, commitment.deliverable().condition());
			commitments.put(commitment.commitmentId(),
					commitments.get(commitment.commitmentId()).staged(Run.Stage.ESTABLISH.ordinal()));
		}
		record(agentName, commitment.agentId(), commitment, "work-satisfied",
				Map.of("held", Integer.toString(held),
						"count", Integer.toString(commitment.deliverable().count()),
						"condition", commitment.deliverable().condition()));
		return true;
	}

	private boolean expire(Commitment commitment, String agentName, long tick, int held) {
		close(commitment, Commitment.State.EXPIRED, tick,
				"window closed at " + commitment.dueTick() + " with " + held + "/"
						+ commitment.deliverable().count(), held, "window closed", agentName);
		if (commitment.stageReached() < Run.Stage.ABANDONED.ordinal()) {
			advanceRun(commitment, Run.Stage.ABANDONED, tick, "window closed");
			commitments.put(commitment.commitmentId(),
					commitments.get(commitment.commitmentId()).staged(Run.Stage.ABANDONED.ordinal()));
		}
		return true;
	}

	private void close(Commitment commitment, Commitment.State state, long tick, String why, int held,
			String condition, String agentName) {
		commitments.put(commitment.commitmentId(), commitment.closed(state, tick, why));
		closeTicket(commitment, state, tick, why, held, condition);
		for (ObjectiveWindow window : windows.values()) {
			if (window.commitmentId().equals(commitment.commitmentId()) && window.open()) {
				windows.put(window.windowId(), window.closed(
						state == Commitment.State.SATISFIED ? ObjectiveWindow.State.MET
								: ObjectiveWindow.State.EXPIRED, tick));
				record(agentName, commitment.agentId(), commitment,
						state == Commitment.State.SATISFIED ? "window-met" : "window-expired",
						Map.of("condition", condition, "held", Integer.toString(held)));
			}
		}
	}

	private void advanceRun(Commitment commitment, Run.Stage stage, long tick, String condition) {
		WorldState world = WorldState.get();
		if (world == null || commitment.runId().isBlank()) {
			return;
		}
		world.advanceRun(commitment.runId(), stage, tick);
	}

	private static boolean seesAny(WorldView view, Deliverable deliverable) {
		for (VisibleBlock block : view.visibleBlocks(DevView.RADIUS)) {
			if (deliverable.kind().matches(block.blockId()) && block.lineOfSight()) {
				return true;
			}
		}
		return false;
	}

	/** The sight radius work is judged on: the same one the being decides with. */
	private static final class DevView {
		static final int RADIUS = 16;
	}

	/** The commitment this being is carrying, if any. A reader: a question about work does not open it. */
	public Optional<Commitment> openCommitment(String agentId) {
		List<Commitment> open = openFor(agentId);
		return open.isEmpty() ? Optional.empty() : Optional.of(open.get(open.size() - 1));
	}

	/** The ticket this being is working under, if one is open. A reader: it changes nothing. */
	public Optional<Ticket> activeTicket(String agentId) {
		for (Commitment commitment : openFor(agentId)) {
			Ticket ticket = ticketFor(commitment.commitmentId());
			if (ticket != null && ticket.open()) {
				return Optional.of(ticket);
			}
		}
		return Optional.empty();
	}

	/**
	 * Spend one declared cell on a decision that serves the open ticket's step. Returns the cell
	 * index, or -1 when the budget is spent or the decision is not this ticket's work. A decision
	 * the ticket did not authorise is not charged to it.
	 */
	public int cell(String agentId, String decisionId, String skillId, long tick) {
		Optional<Commitment> commitment = openCommitment(agentId);
		if (commitment.isEmpty()) {
			return -1;
		}
		Ticket ticket = ticketFor(commitment.get().commitmentId());
		if (ticket == null || !ticket.open()) {
			return -1;
		}
		Plan plan = planFor(commitment.get());
		if (plan == null || skillId == null || !plan.step().equals(skillId)) {
			return -1;
		}
		if (!ticket.cellAvailable()) {
			record(commitment.get().name(), agentId, commitment.get(), "ticket-cell-refused",
					Map.of("ticket", ticket.ticketId(),
							"cells", ticket.usedCells() + "/" + ticket.declaredCells(),
							"why", "the declared cell budget is spent"));
			StreamNotices.refuse(commitment.get().name(), ticket.ticketId(), "ticket-cell-budget",
					"the declared cell budget is spent");
			publish(ticket);
			return -1;
		}
		Ticket next = ticket.withCell(decisionId);
		tickets.put(next.ticketId(), next);
		record(commitment.get().name(), agentId, commitment.get(), "ticket-cell",
				Map.of("ticket", next.ticketId(), "cell", Integer.toString(next.usedCells()),
						"decision", decisionId == null ? "" : decisionId));
		publish(next);
		save();
		return next.usedCells() - 1;
	}


	/**
	 * Record one block that the body observed itself put into the world. The count is the evidence:
	 * a build commitment is satisfied by blocks standing in the world, never by the being saying it
	 * built them. Called by the routine after it has seen the block and the stack shrink.
	 */
	public static void recordPlacement(String agentId, long tick, Vec3i at, String blockId) {
		WorkLedger ledger = get();
		if (ledger != null) {
			ledger.placed(agentId, tick, at, blockId);
		}
	}

	/**
	 * The first time this being practises a verb outside any wish, that verb gets its own one-cell
	 * ticket. Discovery is read from work the being actually did, not from a list: a verb with no
	 * practice has no standing ticket, and a verb already discovered is not re-opened on every step.
	 */
	public void discovered(String agentId, String agentName, String skillId, long tick,
			boolean success, String evidence) {
		java.util.Optional<Commitment> open = openCommitment(agentId);
		if (open.isPresent()) {
			Plan plan = planFor(open.get());
			if (plan != null && skillId.equals(plan.step())) {
				// The wish's ticket already authorises and counts this exact work; a different
				// verb practised alongside the wish is still this being's own discovery.
				return;
			}
		}
		String purpose = "discover:" + skillId;
		for (Ticket ticket : tickets.values()) {
			if (ticket.agentId().equals(agentId) && ticket.purpose().equals(purpose)) {
				return;
			}
		}
		String worldId = WorldState.get() == null ? "" : WorldState.get().world().worldId();
		String witness = success ? "held 1/1 at tick " + tick
				: "refused 0/1: " + (evidence == null ? "" : evidence);
		Ticket ticket = Ticket.opened("ticket:" + UUID.randomUUID(), "", worldId, agentId,
				agentName, purpose, dev.ensouled.core.VerbLibrary.corpus().spokenVerb(skillId),
				dev.ensouled.core.VerbLibrary.corpus().basis(skillId), 1, tick, tick + WINDOW_TICKS)
				.withCell(evidence)
				.closed(success ? Ticket.State.HELD : Ticket.State.REFUSED, tick,
						success ? "first practice" : "practice refused", witness);
		tickets.put(ticket.ticketId(), ticket);
		publish(ticket);
		save();
	}

	private void placed(String agentId, long tick, Vec3i at, String blockId) {
		Optional<Commitment> open = openCommitment(agentId);
		if (open.isEmpty()) {
			return;
		}
		Commitment commitment = open.get();
		Plan plan = planFor(commitment);
		if (plan == null || !Phase1GatePolicy.SKILL_PLACE.equals(plan.step())) {
			return;
		}
		int count = placedCounts.getOrDefault(commitment.commitmentId(), 0) + 1;
		placedCounts.put(commitment.commitmentId(), count);
		record(commitment.name(), agentId, commitment, "build-placed",
				Map.of("block", blockId == null ? "" : blockId, "at", String.valueOf(at),
						"placed", count + "/" + commitment.deliverable().count()));
		if (count >= commitment.deliverable().count()) {
			commitments.put(commitment.commitmentId(),
					commitment.closed(Commitment.State.SATISFIED, tick, "placed " + count));
			closeTicketOnPlacement(commitment, tick, count);
		}
		save();
	}

	/** HELD, with the number of blocks seen standing, not the number the being claims. */
	private void closeTicketOnPlacement(Commitment commitment, long tick, int count) {
		for (Ticket ticket : List.copyOf(tickets.values())) {
			if (!ticket.commitmentId().equals(commitment.commitmentId()) || !ticket.open()) {
				continue;
			}
			Ticket closed = ticket.closed(Ticket.State.HELD, tick, "placed " + count,
					"placed " + count + "/" + commitment.deliverable().count() + " at tick " + tick);
			tickets.put(closed.ticketId(), closed);
			record(commitment.name(), commitment.agentId(), commitment, "ticket-closed",
					Map.of("ticket", closed.ticketId(), "state", "HELD",
							"cells", closed.usedCells() + "/" + closed.declaredCells(),
							"witness", closed.witness()));
			publish(closed);
		}
	}

	/** The item this body would place for the work it owes, if it carries one. */
	public static Optional<String> itemForBuild(String agentId, WorldView view) {
		WorkLedger ledger = get();
		if (ledger == null || view == null) {
			return Optional.empty();
		}
		Optional<Commitment> open = ledger.openCommitment(agentId);
		if (open.isEmpty()) {
			return Optional.empty();
		}
		Plan plan = ledger.planFor(open.get());
		if (plan == null || !Phase1GatePolicy.SKILL_PLACE.equals(plan.step())) {
			return Optional.empty();
		}
		Deliverable.Kind kind = open.get().deliverable().kind();
		for (dev.ensouled.core.InventorySlot slot : view.inventory()) {
			if (slot.count() > 0 && kind.matches(slot.itemId())) {
				return Optional.of(slot.itemId());
			}
		}
		return Optional.empty();
	}

	private Ticket ticketFor(String commitmentId) {
		for (Ticket ticket : tickets.values()) {
			if (ticket.commitmentId().equals(commitmentId)) {
				return ticket;
			}
		}
		return null;
	}

	/** A ticket closes on the commitment's verdict, and on nothing the being says about itself. */
	private void closeTicket(Commitment commitment, Commitment.State state, long tick, String why,
			int held, String condition) {
		Ticket.State next = switch (state) {
			case SATISFIED -> Ticket.State.HELD;
			case EXPIRED -> Ticket.State.ABSENT;
			case ABANDONED -> Ticket.State.REFUSED;
			case OPEN -> Ticket.State.OPEN;
		};
		if (next == Ticket.State.OPEN) {
			return;
		}
		for (Ticket ticket : List.copyOf(tickets.values())) {
			if (!ticket.commitmentId().equals(commitment.commitmentId()) || !ticket.open()) {
				continue;
			}
			String witness = next == Ticket.State.HELD
					? "held " + held + "/" + commitment.deliverable().count() + " at tick " + tick
					: condition + " at tick " + tick;
			Ticket closed = ticket.closed(next, tick,
					next == Ticket.State.HELD
							? "held " + held + "/" + commitment.deliverable().count() : why,
					witness);
			tickets.put(closed.ticketId(), closed);
			record(commitment.name(), commitment.agentId(), commitment, "ticket-closed",
					Map.of("ticket", closed.ticketId(), "state", closed.state().name(),
							"cells", closed.usedCells() + "/" + closed.declaredCells(),
							"witness", closed.witness()));
			publish(closed);
		}
	}

	private static void publish(Ticket ticket) {
		dev.ensouled.surface.SurfaceStream stream = dev.ensouled.surface.SurfaceStream.get();
		if (stream == null) {
			return;
		}
		try {
			stream.ticket(ticket.presenter(), ticket.json());
		} catch (RuntimeException rejected) {
			EnsouledMod.LOG.warn("[ensouled] ticket {} was not streamed: {}", ticket.ticketId(),
					rejected.getMessage());
		}
	}

	/**
	 * The discovery tickets this being opened, keyed by the skill that first practised the verb.
	 * A reader: it is the evidence side of the skill lineage and changes nothing.
	 */
	public Map<String, Ticket> discoveriesOf(String agentId) {
		Map<String, Ticket> out = new LinkedHashMap<>();
		for (Ticket ticket : tickets.values()) {
			if (!ticket.agentId().equals(agentId) || !ticket.purpose().startsWith("discover:")) {
				continue;
			}
			out.putIfAbsent(ticket.purpose().substring("discover:".length()), ticket);
		}
		return out;
	}

	/** Everything this being has owed, closed ones included. A reader; it changes nothing. */
	public List<Commitment> commitmentsOf(String agentId) {
		List<Commitment> out = new ArrayList<>();
		for (Commitment commitment : commitments.values()) {
			if (commitment.agentId().equals(agentId)) {
				out.add(commitment);
			}
		}
		return out;
	}

	private List<Commitment> openFor(String agentId) {
		List<Commitment> out = new ArrayList<>();
		for (Commitment commitment : commitments.values()) {
			if (commitment.agentId().equals(agentId) && commitment.open()) {
				out.add(commitment);
			}
		}
		return out;
	}

	private Plan planFor(Commitment commitment) {
		for (Plan plan : plans.values()) {
			if (plan.commitmentId().equals(commitment.commitmentId())) {
				return plan;
			}
		}
		return null;
	}

	// ------------------------------------------------------------------ the record

	private void record(String agentName, String agentId, Commitment commitment, String code,
			Map<String, String> fields) {
		String subject = commitment == null ? "work" : "work " + commitment.commitmentId();
		Map<String, String> row = new LinkedHashMap<>(fields);
		if (commitment != null) {
			row.put("commitment", commitment.commitmentId());
			row.put("run", commitment.runId());
		}
		MindJournal journal = MindJournal.active();
		if (journal != null) {
			try {
				journal.append(MindJournal.Kind.WORK, agentId,
						code + ":" + (commitment == null ? "-" : commitment.commitmentId()), row);
			} catch (RuntimeException failed) {
				EnsouledMod.LOG.warn("[ensouled] work row {} was not journaled: {}", code,
						failed.getMessage());
			}
		}
		StreamNotices.noted(agentName == null ? agentId : agentName, subject, code,
				fields.getOrDefault("condition", fields.getOrDefault("why", "")));
		EnsouledMod.LOG.info("[ensouled] work {} for {}: {}", code, agentName, row);
	}

	public String describe() {
		StringBuilder out = new StringBuilder("work ").append(file);
		int open = 0;
		for (Commitment commitment : commitments.values()) {
			if (commitment.open()) {
				open++;
			}
		}
		out.append(" commitments=").append(commitments.size()).append(" open=").append(open)
				.append(" plans=").append(plans.size()).append(" windows=").append(windows.size());
		for (Commitment commitment : commitments.values()) {
			Plan plan = planFor(commitment);
			out.append("\n  ").append(commitment.commitmentId()).append(' ')
					.append(commitment.name()).append(" owes ")
					.append(commitment.deliverable().count()).append(' ')
					.append(commitment.deliverable().words()).append(" by tick ")
					.append(commitment.dueTick()).append(" — ").append(commitment.state());
			if (plan != null) {
				out.append(" step=").append(plan.step());
				if (!plan.replacedWhy().isEmpty()) {
					out.append(" (replaced: ").append(plan.replacedWhy()).append(')');
				}
			}
			if (!commitment.runId().isBlank()) {
				out.append(" run=").append(commitment.runId());
			}
			for (ObjectiveWindow window : windows.values()) {
				if (window.commitmentId().equals(commitment.commitmentId())) {
					// The condition is printed with the window: a reader can check it against the
					// body's inventory without being told what the check was.
					out.append(" window=").append(window.state()).append(" (")
							.append(window.condition()).append(", closes ")
							.append(window.closesTick()).append(')');
				}
			}
			if (!commitment.closedWhy().isEmpty()) {
				out.append(" why=").append(commitment.closedWhy());
			}
		}
		for (Ticket ticket : tickets.values()) {
			out.append("\n  ticket ").append(ticket.ticketId()).append(' ').append(ticket.state())
					.append(" cells=").append(ticket.usedCells()).append('/')
					.append(ticket.declaredCells()).append(" witness=").append(ticket.witness());
		}
		return out.toString();
	}

	// ------------------------------------------------------------------ persistence

	private synchronized void save() {
		prune();
		try {
			Files.createDirectories(file.getParent());
			JsonObject root = new JsonObject();
			JsonArray rows = new JsonArray();
			for (Commitment commitment : commitments.values()) {
				JsonObject row = new JsonObject();
				row.addProperty("commitment_id", commitment.commitmentId());
				row.addProperty("world_id", commitment.worldId());
				row.addProperty("agent_id", commitment.agentId());
				row.addProperty("name", commitment.name());
				row.addProperty("kind", commitment.deliverable().kind().name());
				row.addProperty("count", commitment.deliverable().count());
				row.addProperty("source", commitment.source());
				row.addProperty("opened_tick", commitment.openedTick());
				row.addProperty("due_tick", commitment.dueTick());
				row.addProperty("run_id", commitment.runId());
				row.addProperty("state", commitment.state().name());
				row.addProperty("closed_tick", commitment.closedTick());
				row.addProperty("closed_why", commitment.closedWhy());
				row.addProperty("stage_reached", commitment.stageReached());
				rows.add(row);
			}
			root.add("commitments", rows);
			JsonArray planRows = new JsonArray();
			for (Plan plan : plans.values()) {
				JsonObject row = new JsonObject();
				row.addProperty("plan_id", plan.planId());
				row.addProperty("commitment_id", plan.commitmentId());
				JsonArray steps = new JsonArray();
				plan.steps().forEach(steps::add);
				row.add("steps", steps);
				row.addProperty("current", plan.current());
				row.addProperty("created_tick", plan.createdTick());
				row.addProperty("replaced_tick", plan.replacedTick());
				row.addProperty("replaced_why", plan.replacedWhy());
				planRows.add(row);
			}
			root.add("plans", planRows);
			JsonArray windowRows = new JsonArray();
			for (ObjectiveWindow window : windows.values()) {
				JsonObject row = new JsonObject();
				row.addProperty("window_id", window.windowId());
				row.addProperty("commitment_id", window.commitmentId());
				row.addProperty("opens_tick", window.opensTick());
				row.addProperty("closes_tick", window.closesTick());
				row.addProperty("condition", window.condition());
				row.addProperty("state", window.state().name());
				row.addProperty("closed_tick", window.closedTick());
				windowRows.add(row);
			}
			root.add("windows", windowRows);
			JsonArray ticketRows = new JsonArray();
			for (Ticket ticket : tickets.values()) {
				JsonObject row = new JsonObject();
				row.addProperty("ticket_id", ticket.ticketId());
				row.addProperty("commitment_id", ticket.commitmentId());
				row.addProperty("world_id", ticket.worldId());
				row.addProperty("agent_id", ticket.agentId());
				row.addProperty("presenter", ticket.presenter());
				row.addProperty("purpose", ticket.purpose());
				row.addProperty("verb", ticket.verb());
				row.addProperty("verb_basis", ticket.verbBasis());
				row.addProperty("declared_cells", ticket.declaredCells());
				row.addProperty("used_cells", ticket.usedCells());
				row.addProperty("opened_tick", ticket.openedTick());
				row.addProperty("closes_tick", ticket.closesTick());
				row.addProperty("state", ticket.state().name());
				row.addProperty("closed_tick", ticket.closedTick());
				row.addProperty("closed_why", ticket.closedWhy());
				row.addProperty("witness", ticket.witness());
				JsonArray spent = new JsonArray();
				ticket.evidence().forEach(spent::add);
				row.add("evidence", spent);
				ticketRows.add(row);
			}
			root.add("tickets", ticketRows);
			JsonObject buildRows = new JsonObject();
			for (Map.Entry<String, Integer> row : placedCounts.entrySet()) {
				buildRows.addProperty(row.getKey(), row.getValue());
			}
			root.add("builds", buildRows);
			Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
		} catch (IOException failure) {
			EnsouledMod.LOG.error("[ensouled] work could not be written to {}: {}", file,
					failure.getMessage());
		}
	}

	private void prune() {
		List<String> closed = new ArrayList<>();
		for (Commitment commitment : commitments.values()) {
			if (!commitment.open()) {
				closed.add(commitment.commitmentId());
			}
		}
		while (closed.size() > MAX_CLOSED_KEPT) {
			String oldest = closed.remove(0);
			commitments.remove(oldest);
			plans.values().removeIf(plan -> plan.commitmentId().equals(oldest));
			windows.values().removeIf(window -> window.commitmentId().equals(oldest));
			tickets.values().removeIf(ticket -> ticket.commitmentId().equals(oldest));
		}
	}

	private void load() {
		if (!Files.isRegularFile(file)) {
			return;
		}
		try {
			JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8))
					.getAsJsonObject();
			if (root.has("commitments")) {
				for (var element : root.getAsJsonArray("commitments")) {
					JsonObject row = element.getAsJsonObject();
					Deliverable deliverable = new Deliverable(
							Deliverable.Kind.valueOf(row.get("kind").getAsString()),
							row.get("count").getAsInt());
					Commitment commitment = new Commitment(row.get("commitment_id").getAsString(),
							row.get("world_id").getAsString(), row.get("agent_id").getAsString(),
							row.get("name").getAsString(), deliverable,
							row.get("source").getAsString(), row.get("opened_tick").getAsLong(),
							row.get("due_tick").getAsLong(), row.get("run_id").getAsString(),
							Commitment.State.valueOf(row.get("state").getAsString()),
							row.get("closed_tick").getAsLong(), row.get("closed_why").getAsString(),
							row.get("stage_reached").getAsInt());
					commitments.put(commitment.commitmentId(), commitment);
				}
			}
			if (root.has("plans")) {
				for (var element : root.getAsJsonArray("plans")) {
					JsonObject row = element.getAsJsonObject();
					List<String> steps = new ArrayList<>();
					for (var step : row.getAsJsonArray("steps")) {
						steps.add(step.getAsString());
					}
					Plan plan = new Plan(row.get("plan_id").getAsString(),
							row.get("commitment_id").getAsString(), steps,
							row.get("current").getAsInt(), row.get("created_tick").getAsLong(),
							row.get("replaced_tick").getAsLong(),
							row.get("replaced_why").getAsString());
					plans.put(plan.planId(), plan);
				}
			}
			if (root.has("windows")) {
				for (var element : root.getAsJsonArray("windows")) {
					JsonObject row = element.getAsJsonObject();
					ObjectiveWindow window = new ObjectiveWindow(row.get("window_id").getAsString(),
							row.get("commitment_id").getAsString(), row.get("opens_tick").getAsLong(),
							row.get("closes_tick").getAsLong(), row.get("condition").getAsString(),
							ObjectiveWindow.State.valueOf(row.get("state").getAsString()),
							row.get("closed_tick").getAsLong());
					windows.put(window.windowId(), window);
				}
			}
			if (root.has("tickets")) {
				for (var element : root.getAsJsonArray("tickets")) {
					JsonObject row = element.getAsJsonObject();
					List<String> evidence = new ArrayList<>();
					for (var spent : row.getAsJsonArray("evidence")) {
						evidence.add(spent.getAsString());
					}
					Ticket ticket = new Ticket(row.get("ticket_id").getAsString(),
							row.get("commitment_id").getAsString(), row.get("world_id").getAsString(),
							row.get("agent_id").getAsString(), row.get("presenter").getAsString(),
							row.get("purpose").getAsString(), row.get("verb").getAsString(),
							row.get("verb_basis").getAsString(), row.get("declared_cells").getAsInt(),
							row.get("used_cells").getAsInt(), row.get("opened_tick").getAsLong(),
							row.get("closes_tick").getAsLong(),
							Ticket.State.valueOf(row.get("state").getAsString()),
							row.get("closed_tick").getAsLong(), row.get("closed_why").getAsString(),
							row.get("witness").getAsString(), List.copyOf(evidence));
					tickets.put(ticket.ticketId(), ticket);
				}
			}
			if (root.has("builds")) {
				for (Map.Entry<String, com.google.gson.JsonElement> row
						: root.getAsJsonObject("builds").entrySet()) {
					placedCounts.put(row.getKey(), row.getValue().getAsInt());
				}
			}
		} catch (IOException | RuntimeException unreadable) {
			EnsouledMod.LOG.warn("[ensouled] {} is unreadable ({}); commitments start empty", file,
					unreadable.getMessage());
		}
	}
}

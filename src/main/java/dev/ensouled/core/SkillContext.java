package dev.ensouled.core;

import java.util.List;
import java.util.Map;

import dev.ensouled.interact.InteractionSession;

/** Narrow view of the mind + body that a skill routine may use. */
public final class SkillContext {
	public interface Listener {
		void completed(String detail, Map<String, String> observedFacts);

		void failed(String code, String detail);

		void motorIssued(MotorCommand command);

		/** Record something the routine itself learned, e.g. a social exchange it took part in. */
		void note(EvidenceKind kind, String outcome, String detail);

		/** A verified container movement, recorded once per transfer id and operation. */
		void recordContainerMove(String transferId, Vec3i where, String operation, String itemId, int count);
	}

	private final WorldView view;
	private final Intention intention;
	private final SkillRuntime runtime;
	private final Listener listener;
	private final SocialSeam social;
	private final String agentId;
	private final String agentName;
	private final InteractionSession interactions;

	public SkillContext(WorldView view, Intention intention, SkillRuntime runtime, Listener listener,
			SocialSeam social, String agentId, String agentName, InteractionSession interactions) {
		this.view = view;
		this.intention = intention;
		this.runtime = runtime;
		this.listener = listener;
		this.social = social;
		this.agentId = agentId;
		this.agentName = agentName;
		this.interactions = interactions;
	}

	public WorldView view() {
		return view;
	}

	public Intention intention() {
		return intention;
	}

	public int step() {
		return runtime.step();
	}

	public void advance() {
		runtime.advance();
	}

	public boolean idle() {
		return !runtime.awaiting();
	}

	/** A command was issued in this step; its result may be consumed once idle. */
	public boolean issued() {
		return runtime.issuedThisStep();
	}

	public void clearResult() {
		runtime.clearResult();
	}

	public boolean lastSucceeded() {
		return runtime.lastSucceeded();
	}

	public boolean lastFailed() {
		return runtime.lastFailed();
	}

	public MotorResult last() {
		return runtime.last();
	}

	public void issue(MotorCommand command) {
		if (runtime.issue(command)) {
			listener.motorIssued(command);
		}
	}

	public int totalItems() {
		int total = 0;
		for (InventorySlot slot : view.inventory()) {
			total += slot.count();
		}
		return total;
	}

	public int countItem(String itemId) {
		return view.countItem(itemId);
	}

	public List<VisibleBlock> visibleBlocks(int radius) {
		return view.visibleBlocks(radius);
	}

	public void complete(String detail, Map<String, String> observedFacts) {
		listener.completed(detail, observedFacts);
	}

	public void fail(String code, String detail) {
		listener.failed(code, detail);
	}

	public void note(EvidenceKind kind, String outcome, String detail) {
		listener.note(kind, outcome, detail);
	}

	public SocialSeam social() {
		return social;
	}

	public String agentId() {
		return agentId;
	}

	/** The name this being speaks under; an utterance must be attributable to it. */
	public String agentName() {
		return agentName;
	}

	/** World objects this body can perceive, and the gated way to operate them. */
	public InteractionSession interactions() {
		return interactions;
	}

	public void recordContainerMove(String transferId, Vec3i where, String operation, String itemId,
			int count) {
		listener.recordContainerMove(transferId, where, operation, itemId, count);
	}
}

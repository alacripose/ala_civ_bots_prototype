package dev.ensouled.persist;

import java.util.ArrayList;
import java.util.List;

import dev.ensouled.core.AgentIdentity;
import dev.ensouled.core.ContinuityState;
import dev.ensouled.core.NeedsState;
import dev.ensouled.core.StateNode;
import dev.ensouled.core.Vec3i;

/**
 * V4 §49.2: the durable record that is about a person rather than about one conversation with the
 * world. It carries what this build can truthfully observe — genesis provenance, the history of
 * identities and bodies bound to this life, observed needs with the provider that reported them,
 * the configuration the mind was running under, and the reconciliation metadata for the last time
 * it was seen.
 *
 * <p>Domains the document lists that this build has no source for (relationships, contracts,
 * test participation, death) are not invented here; they are absent, and {@link #unrecorded()}
 * names them so a reader can tell absence from emptiness.
 */
public final class AgentContinuity {
	public static final int SCHEMA = 1;
	/** Who reported the need observations last time: the vanilla player's own HUD values. */
	public static final String NEEDS_PROVIDER = "minecraft-1.20.1/player-native";

	/** §49.2 "identity/profile binding history": one life may wear several bodies over time. */
	public record Binding(String bodyKind, String profileId, String entityId, long firstTick, long lastTick,
			int count) {
	}

	private static final List<String> UNRECORDED = List.of(
			"relationships", "conversations", "commitments", "contracts", "test_participation",
			"suspension_or_death");

	private final List<Binding> bindings = new ArrayList<>();
	private long createdEpoch;
	private long birthTick;
	private String lineage = "";
	private int boots;
	private ContinuityState state = ContinuityState.ABSENT;
	private String condition = "";
	private String lastBodyKind = "";
	private String lastProfile = "";
	private String lastDimension = "";
	private Vec3i lastPosition = new Vec3i(0, 0, 0);
	private long lastSessionEnd;
	private int food = -1;
	private int health = -1;
	private int air = -1;
	private long needsTick = -1;
	private String gatePolicy = "";
	private String skills = "";
	private long observedGeneration;

	private AgentContinuity() {
	}

	public static AgentContinuity of(AgentIdentity identity) {
		AgentContinuity continuity = new AgentContinuity();
		continuity.createdEpoch = identity.createdEpochMilli();
		continuity.birthTick = identity.birthTick();
		continuity.lineage = identity.lineageNote();
		return continuity;
	}

	/**
	 * Reads a continuity record written by a previous run. A pre-continuity snapshot has none, so
	 * genesis falls back to the identity fields the mind itself carries; that is a migration, not an
	 * invented genesis.
	 */
	public static AgentContinuity fromState(StateNode continuity, StateNode mind) {
		AgentContinuity facts = new AgentContinuity();
		facts.createdEpoch = continuity.lng("created_epoch", mind.lng("created_epoch", 0L));
		facts.birthTick = continuity.lng("birth_tick", mind.lng("birth_tick", 0L));
		facts.lineage = continuity.string("lineage", mind.string("lineage", ""));
		facts.boots = continuity.integer("boots", 0);
		facts.state = parseState(continuity.string("state", "ABSENT"));
		facts.condition = continuity.string("condition", "");
		facts.lastBodyKind = continuity.string("last_body_kind", "");
		facts.lastProfile = continuity.string("last_profile", "");
		facts.lastDimension = continuity.string("last_dimension", mind.string("dimension", ""));
		facts.lastPosition = new Vec3i(continuity.integer("last_x", 0), continuity.integer("last_y", 0),
				continuity.integer("last_z", 0));
		facts.lastSessionEnd = continuity.lng("last_session_end", 0L);
		facts.food = continuity.integer("needs_food", -1);
		facts.health = continuity.integer("needs_health", -1);
		facts.air = continuity.integer("needs_air", -1);
		facts.needsTick = continuity.lng("needs_tick", -1L);
		facts.gatePolicy = continuity.string("gate_policy", "");
		facts.skills = continuity.string("skills", "");
		facts.observedGeneration = continuity.lng("observed_generation", 0L);
		for (StateNode node : continuity.nodes("bindings")) {
			facts.bindings.add(new Binding(node.string("body_kind", ""), node.string("profile", ""),
					node.string("entity", ""), node.lng("first_tick", 0L), node.lng("last_tick", 0L),
					node.integer("count", 0)));
		}
		return facts;
	}

	public void booted() {
		boots++;
	}

	public void bound(String bodyKind, String profileId, String entityId, long tick) {
		lastBodyKind = bodyKind == null ? "" : bodyKind;
		lastProfile = profileId == null ? "" : profileId;
		String key = profileId != null && !profileId.isEmpty() ? profileId : (entityId == null ? "" : entityId);
		for (int i = 0; i < bindings.size(); i++) {
			Binding existing = bindings.get(i);
			String existingKey = !existing.profileId().isEmpty() ? existing.profileId() : existing.entityId();
			if (existing.bodyKind().equals(lastBodyKind) && existingKey.equals(key)) {
				bindings.set(i, new Binding(existing.bodyKind(), existing.profileId(), existing.entityId(),
						existing.firstTick(), tick, existing.count() + 1));
				return;
			}
		}
		bindings.add(new Binding(lastBodyKind, lastProfile, entityId == null ? "" : entityId, tick, tick, 1));
	}

	public void sessionEnd(long epochMilli) {
		lastSessionEnd = epochMilli;
	}

	public void where(Vec3i position, String dimension) {
		if (position != null) {
			lastPosition = position;
		}
		if (dimension != null && !dimension.isEmpty()) {
			lastDimension = dimension;
		}
	}

	public void observe(NeedsState needs, long tick) {
		if (needs == null) {
			return;
		}
		food = needs.food();
		health = needs.health();
		air = needs.air();
		needsTick = tick;
	}

	public void configure(String gate, String skills) {
		gatePolicy = gate == null ? "" : gate;
		this.skills = skills == null ? "" : skills;
	}

	public void observed(ContinuityState state, String condition, long generation) {
		this.state = state;
		this.condition = condition == null ? "" : condition;
		this.observedGeneration = generation;
	}

	public ContinuityState state() {
		return state;
	}

	public String condition() {
		return condition;
	}

	public int boots() {
		return boots;
	}

	public String lastBodyKind() {
		return lastBodyKind;
	}

	public Vec3i lastPosition() {
		return lastPosition;
	}

	public long lastSessionEnd() {
		return lastSessionEnd;
	}

	public List<Binding> bindings() {
		return List.copyOf(bindings);
	}

	public List<String> unrecorded() {
		return UNRECORDED;
	}

	public long observedGeneration() {
		return observedGeneration;
	}

	public StateNode toState() {
		StateNode node = new StateNode();
		node.putInt("schema", SCHEMA);
		node.putLong("created_epoch", createdEpoch);
		node.putLong("birth_tick", birthTick);
		node.putString("lineage", lineage);
		node.putInt("boots", boots);
		node.putString("state", state.name());
		node.putString("condition", condition);
		node.putString("last_body_kind", lastBodyKind);
		node.putString("last_profile", lastProfile);
		node.putString("last_dimension", lastDimension);
		node.putInt("last_x", lastPosition.x());
		node.putInt("last_y", lastPosition.y());
		node.putInt("last_z", lastPosition.z());
		node.putLong("last_session_end", lastSessionEnd);
		node.putInt("needs_food", food);
		node.putInt("needs_health", health);
		node.putInt("needs_air", air);
		node.putLong("needs_tick", needsTick);
		node.putString("needs_provider", NEEDS_PROVIDER);
		node.putString("gate_policy", gatePolicy);
		node.putString("skills", skills);
		node.putLong("observed_generation", observedGeneration);
		List<StateNode> bindingNodes = new ArrayList<>();
		for (Binding binding : bindings) {
			bindingNodes.add(new StateNode()
					.putString("body_kind", binding.bodyKind())
					.putString("profile", binding.profileId())
					.putString("entity", binding.entityId())
					.putLong("first_tick", binding.firstTick())
					.putLong("last_tick", binding.lastTick())
					.putInt("count", binding.count()));
		}
		node.putNodeList("bindings", bindingNodes);
		node.putStringList("unrecorded", UNRECORDED);
		return node;
	}

	private static ContinuityState parseState(String value) {
		try {
			return ContinuityState.valueOf(value);
		} catch (IllegalArgumentException e) {
			return ContinuityState.ABSENT;
		}
	}
}

package dev.ensouled.mind;

import java.util.UUID;

import dev.ensouled.body.BodyAdapter;
import dev.ensouled.body.PlayerBodyAdapter;
import dev.ensouled.core.AgentIdentity;
import dev.ensouled.core.AgentMind;
import dev.ensouled.core.AuthorityGateway;
import dev.ensouled.core.EpisodeKind;
import dev.ensouled.core.MindPersistence;
import dev.ensouled.core.SkillRegistry;
import dev.ensouled.core.SocialSeam;
import dev.ensouled.core.StateNode;
import dev.ensouled.core.Vec3i;
import dev.ensouled.core.WorldView;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

/** Binds one persistent mind to at most one live body, of any supported body kind. */
public final class AgentRecord {
	private final String agentId;
	private final String name;
	private final String bodyKind;
	private final UUID profileId;
	private Vec3i home;
	private final AuthorityGateway gate;
	private final BodyMotorProxy motorProxy;
	private final BodyHandsProxy hands;
	private final AgentMind mind;

	private BodyAdapter body;
	private WorldView view;

	private AgentRecord(String agentId, String name, String bodyKind, UUID profileId, Vec3i home,
			AuthorityGateway gate, BodyMotorProxy motorProxy, BodyHandsProxy hands, AgentMind mind) {
		this.agentId = agentId;
		this.name = name;
		this.bodyKind = bodyKind;
		this.profileId = profileId;
		this.home = home;
		this.gate = gate;
		this.motorProxy = motorProxy;
		this.hands = hands;
		this.mind = mind;
	}

	public static AgentRecord create(BodyAdapter body, SkillRegistry skills, AgentIdentity identity,
			AuthorityGateway gate, SocialSeam social, Vec3i home) {
		BodyMotorProxy proxy = new BodyMotorProxy();
		BodyHandsProxy hands = new BodyHandsProxy();
		AgentRecord record = new AgentRecord(identity.agentId(), identity.displayName(), body.kind(),
				profileIdOf(body), home, gate, proxy, hands,
				new AgentMind(identity, gate, proxy, skills, social, hands));
		record.attach(body);
		return record;
	}

	public static AgentRecord restored(StateNode root, SkillRegistry skills, AuthorityGateway gate,
			String name, Vec3i home, String bodyKind, UUID profileId, SocialSeam social) {
		BodyMotorProxy proxy = new BodyMotorProxy();
		BodyHandsProxy hands = new BodyHandsProxy();
		AgentMind mind = MindPersistence.restore(root, gate, proxy, skills, social, hands);
		return new AgentRecord(mind.identity().agentId(), name, bodyKind, profileId, home, gate, proxy,
				hands, mind);
	}

	private static UUID profileIdOf(BodyAdapter body) {
		return body instanceof PlayerBodyAdapter player ? player.profileId() : null;
	}

	public String agentId() {
		return agentId;
	}

	public String name() {
		return name;
	}

	public String bodyKind() {
		return bodyKind;
	}

	public UUID profileId() {
		return profileId;
	}

	public Vec3i home() {
		return home;
	}

	public AgentMind mind() {
		return mind;
	}

	public BodyAdapter body() {
		return body;
	}

	/** The perception view the mind is deciding from this tick, or null while unbound. */
	public WorldView view() {
		return view;
	}

	public AuthorityGateway gate() {
		return gate;
	}

	public BodyMotorProxy motorProxy() {
		return motorProxy;
	}

	public void attach(BodyAdapter adapter) {
		if (adapter == null) {
			return;
		}
		// The entity itself, not its id: a reconnecting client is a new player entity whose id was
		// recycled, so an id comparison keeps the retired session attached and the being silent.
		if (body != null && body.entity() != null && body.entity() == adapter.entity()
				&& body.kind().equals(adapter.kind())) {
			return;
		}
		this.motorProxy.attach(adapter.createMotor());
		this.hands.attach(adapter.interactions());
		this.body = adapter;
		this.view = adapter.createView();
		// The being lives where its body actually is. A saved home coordinate belongs to the land it
		// was recorded in; on arrival somewhere else it is reconciled to this body and recorded,
		// rather than bounding its actions by a place it can no longer stand.
		Vec3i at = adapter.position();
		if (!at.equals(home)) {
			gate.reanchor(at);
			mind.ledger().remember(mind.tick(), EpisodeKind.OTHER,
					"materialized at " + at + "; home was " + home, at, "self_observed",
					"ev:home-reanchored");
			this.home = at;
		}
	}

	/** Body-kind simulation step, before the mind acts on last tick's intent. */
	public void tickBody() {
		if (body != null && body.alive()) {
			body.tickBody();
		}
	}

	public void tick(MinecraftServer server) {
		if (body == null || !body.alive()) {
			body = null;
			view = null;
			motorProxy.detach();
			hands.detach();
			return;
		}
		if (view == null) {
			view = body.createView();
		}
		mind.tick(view);
	}

	/** Item count in the current body inventory (used by the acceptance harness). */
	public int countItem(String itemId) {
		return view == null ? 0 : view.countItem(itemId);
	}

	public Vec3i position() {
		return body == null ? home : body.position();
	}

	public String dimension() {
		return body == null ? "minecraft:overworld" : body.world().getRegistryKey().getValue().toString();
	}

	/** Server world containing this body, or the overworld as a fallback. */
	public ServerWorld world(MinecraftServer server) {
		return body == null ? server.getOverworld() : body.world();
	}
}

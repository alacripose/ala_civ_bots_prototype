package dev.ensouled.mind;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import dev.ensouled.EnsouledMod;
import dev.ensouled.body.BodyAdapter;
import dev.ensouled.body.MobBodyAdapter;
import dev.ensouled.body.NetworkPlayerBodyAdapter;
import dev.ensouled.body.PlayerBodyAdapter;
import dev.ensouled.core.AgentIdentity;
import dev.ensouled.domain.WorldState;
import dev.ensouled.core.ChatTranscript;
import dev.ensouled.core.ContinuityState;
import dev.ensouled.core.EpisodeKind;
import dev.ensouled.core.Hearing;
import dev.ensouled.core.HumanChannel;
import dev.ensouled.core.Speech;
import dev.ensouled.core.MessageBus;
import dev.ensouled.core.AuthorityGateway;
import dev.ensouled.core.SkillRegistry;
import dev.ensouled.core.SocialSeam;
import dev.ensouled.core.Vec3i;
import dev.ensouled.entity.AgentBodyEntity;
import dev.ensouled.entity.EnsouledEntities;
import dev.ensouled.entity.FakePlayerBody;
import dev.ensouled.persist.AgentContinuity;
import dev.ensouled.persist.MindJournal;
import dev.ensouled.persist.MindStorage;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

/**
 * Owns the mapping from persistent minds to bodies. Every tick each bound body advances exactly one
 * mind; no other subsystem may drive a body's hands (§2.2). Fake-player bodies are re-materialized
 * from their reserved profile UUID after a restart (Addendum C §C.8).
 */
public final class AgentManager implements SocialSeam {
	public static final int HOME_RADIUS = 64;
	public static final int HARVEST_BUDGET = 64;
	private static final int AUTOSAVE_INTERVAL_TICKS = 6000;

	private static AgentManager instance;

	private final MinecraftServer server;
	private final SkillRegistry skills = SkillRegistry.phase1();
	/** Everything that survives a restart: the journal, the snapshots, the waiting lives. */
	private final ContinuityLedger lives;
	/** Every being that has a body on this server, by agent id. */
	private final Map<String, AgentRecord> records = new LinkedHashMap<>();
	/** Speech: the bus, ordinary chat in and out, and which entity is which mind. */
	private final SpeechRoutes speech;
	/** Which minds held a live body last tick: a body change or loss is a persist point. */
	private final java.util.Set<String> embodied = new java.util.HashSet<>();
	private int ticksSinceSave;
	/**
	 * Legacy in-process fake bodies (Addendum C). Off by default: production embodiment is the
	 * ordinary player session of a connected headless client (V4 §44.3), and a persisted mind waits
	 * for its own client instead of being stood up as a fake player. Tests that need the old behaviour
	 * ask for it explicitly with {@code ENSOULED_FAKE_BODIES=1}.
	 */
	private final boolean fakeBodies = "1".equals(System.getenv("ENSOULED_FAKE_BODIES"));
	/**
	 * The resident roster (§48.2). A session may only be minted into a being when its name is on this
	 * list. A person's session is never a resident: minting one onto an operator's client puts a mind
	 * on their avatar, which is the system taking the controls from a human (correction §2, §5).
	 */
	private static final java.util.Set<String> RESIDENTS = residentRoster();
	private boolean materializationPending = true;

	private AgentManager(MinecraftServer server) {
		this.server = server;
		this.lives = new ContinuityLedger(server, skills);
		// The world's durable identity is opened before any mind can be bound to it, so a binding has a
		// world to belong to and a command has a revision to be checked against (§A5.1, §A6.4).
		WorldState.open(server);
		EnsouledMod.LOG.info("[ensouled] external brains: {}", ExternalBrains.names());
		this.speech = new SpeechRoutes(new SpeechRoutes.Minds() {
			@Override
			public AgentRecord record(String agentId) {
				return records.get(agentId);
			}

			@Override
			public String nameOf(String agentId) {
				return AgentManager.this.nameOf(agentId);
			}

			@Override
			public Optional<String> agentIdForPlayer(ServerPlayerEntity player) {
				return AgentManager.this.agentIdForPlayer(player);
			}

			@Override
			public List<AgentRecord> records() {
				return List.copyOf(records.values());
			}

			@Override
			public MinecraftServer server() {
				return server;
			}
		});
	}

	private String nameOf(String agentId) {
		AgentRecord record = records.get(agentId);
		return record == null ? agentId : record.name();
	}

	public static void onServerStarted(MinecraftServer server) {
		instance = new AgentManager(server);
		// The conversation record lives with the world it happened in, so a restart reads the same
		// transcript rather than a fresh one (§47.4).
		ChatTranscript.open(MindStorage.directoryFor(server).toPath());
		instance.lives.recover();
	}

	/**
	 * §49.1/§49.3 boot: read the journal, read the newest complete snapshot, and say out loud which
	 * condition every life came back in. Nothing here mints a mind to fill a gap.
	 */

	public static void onServerStopping(MinecraftServer server) {
		if (instance != null) {
			instance.saveAll();
			MindJournal.uninstall(instance.lives.journal());
			instance.lives.journal().close();
			instance = null;
		}
	}

	public static void onServerTick(MinecraftServer server) {
		if (instance != null) {
			instance.tick();
		}
	}

	public static AgentManager get() {
		return instance;
	}

	private void tick() {
		if (materializationPending) {
			if (fakeBodies) {
				materializePersistedPlayers();
			}
			materializationPending = false;
		}
		scanBodies();
		resolveClientBodies();
		// Bodies settle before the mind acts: last tick's motor intent becomes the world's answer,
		// exactly as a real client's input would.
		for (AgentRecord record : List.copyOf(records.values())) {
			record.tickBody();
		}
		for (AgentRecord record : List.copyOf(records.values())) {
			record.tick(server);
		}
		// A body arriving or leaving is exactly the moment a life must not be lost: persist here, so
		// a disconnect, a replaced body or a crash after this tick still finds the same being.
		if (embodimentChanged()) {
			saveAll();
		}
		// §49.3: a canonical event is a fact the being may cite, so it is made durable before the
		// action counts as complete — not up to five minutes later. Only changes trigger a write.
		if (canonicalEventsChanged()) {
			saveAll();
		}
		if (++ticksSinceSave >= AUTOSAVE_INTERVAL_TICKS) {
			saveAll();
			ticksSinceSave = 0;
		}
	}

	/**
	 * A resident's session is its body, and the session can be bound before it is ready to be ticked.
	 * When that happens the record's own step (which correctly refuses to act through a dead body)
	 * drops the body, and nothing ever binds one again: the mind's tick stops advancing while the
	 * world continues, so the being looks present in the player list and stopped in its own record.
	 * Body lifetime belongs here, beside {@link #scanBodies}, and it is resolved from the server's
	 * own player list every tick rather than once at join.
	 */
	private void resolveClientBodies() {
		if (server == null) {
			return;
		}
		for (AgentRecord record : List.copyOf(records.values())) {
			BodyAdapter current = record.body();
			// Mob bodies are the entity scan's business, and an unloaded life waits for its own
			// arrival: only a player-session body is resolved from the player list.
			if (current != null && !"player".equals(current.kind())) {
				continue;
			}
			if (!RESIDENTS.contains(record.name().toUpperCase(java.util.Locale.ROOT))) {
				continue;
			}
			ServerPlayerEntity session = server.getPlayerManager().getPlayer(record.name());
			if (session == null) {
				continue;
			}
			if (current != null && current.entity() == session) {
				continue;
			}
			record.attach(new NetworkPlayerBodyAdapter(session));
			EnsouledMod.LOG.info("[ensouled] rebound {} onto its session {} at tick {}",
					record.agentId(), record.name(), server.getTicks());
		}
	}

	/** True when any embodied mind has appended a canonical event since the last snapshot. */
	private boolean canonicalEventsChanged() {
		for (AgentRecord record : records.values()) {
			Long saved = lives.savedEventSequence(record.agentId());
			if (saved == null || saved.longValue() != record.mind().ledger().events().sequence()) {
				return true;
			}
		}
		return false;
	}

	private void scanBodies() {
		for (ServerWorld world : server.getWorlds()) {
			for (Entity entity : world.iterateEntities()) {
				if (entity instanceof AgentBodyEntity mob && !mob.getAgentId().isEmpty()) {
					bind(new MobBodyAdapter(mob));
				}
			}
		}
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			if (player instanceof FakePlayerBody fake
					&& !ExternalBrains.isExternal(fake.getGameProfile().getName())) {
				bind(new PlayerBodyAdapter(fake));
			}
		}
	}

	private void bind(BodyAdapter adapter) {
		AgentRecord existing = records.get(adapter.agentId());
		if (existing != null) {
			existing.attach(adapter);
			return;
		}
		MindStorage.LoadedMind loaded = lives.unboundByAgentId(adapter.agentId());
		if (loaded != null && loaded.state() == ContinuityState.INTEGRITY_BLOCKED) {
			// §49.1: an unreadable record is not an empty one. The body binds to nobody and the life
			// stays exactly where it is on disk until a human reconciles it.
			EnsouledMod.LOG.error("[ensouled] refusing {} body for {}: record is INTEGRITY_BLOCKED ({})",
					adapter.kind(), adapter.agentId(), loaded.condition());
			return;
		}
		if (loaded == null && !lives.canMint()) {
			EnsouledMod.LOG.error("[ensouled] refusing to mint a mind for {}: {}", adapter.agentId(),
					store() == null ? "no store" : store().describe());
			return;
		}
		AgentRecord record;
		AgentContinuity facts;
		String entityId = adapter.entity() == null ? "" : adapter.entity().getUuidAsString();
		if (loaded != null) {
			lives.takeUnboundByAgentId(loaded.agentId());
			// The being wakes where its body actually is. A saved home coordinate belongs to the land
			// it was recorded in; when the body materializes elsewhere (a replaced world, a moved
			// spawn), that coordinate is not where it lives now, so the home is re-anchored to the
			// body and the move is recorded rather than silently inherited.
			Vec3i home = adapter.position();
			AuthorityGateway gate = AuthorityFactory.provisional(loaded.agentId(), home, HOME_RADIUS, HARVEST_BUDGET);
			// Write-ahead (§49.3): the binding is on disk before the mind may act through this body.
			if (!lives.commit(MindJournal.Kind.BIND, loaded.agentId(),
					"bind:" + loaded.agentId() + ":" + adapter.kind() + ":"
							+ (loaded.profileId().isEmpty() ? entityId : loaded.profileId()),
					lives.bindFields(adapter.kind(), loaded.profileId(), entityId))) {
				lives.hold(loaded);
				return;
			}
			record = AgentRecord.restored(loaded.root(), skills, gate, loaded.name(), home,
					loaded.bodyKind(), parseUuid(loaded.profileId()), this);
			facts = lives.continuityFor(loaded.agentId(),
					() -> AgentContinuity.fromState(loaded.continuity(), loaded.root()));
			facts.bound(adapter.kind(), loaded.profileId(), entityId, server.getTicks());
			if (!home.equals(loaded.home())) {
				record.mind().ledger().remember(record.mind().tick(), EpisodeKind.OTHER,
						"woke at " + home + "; home was " + loaded.home() + " in the land before",
						home, "self_observed", "ev:home-reanchored");
			}
			record.attach(adapter);
			EnsouledMod.LOG.info("[ensouled] restored mind {} ({}) into {} body (state={})",
					record.agentId(), record.name(), adapter.kind(), loaded.state());
		} else {
			Vec3i home = adapter.position();
			AuthorityGateway gate = AuthorityFactory.provisional(adapter.agentId(), home, HOME_RADIUS, HARVEST_BUDGET);
			AgentIdentity identity = newIdentity(adapter.agentId(), adapter.displayName(), server.getTicks());
			if (!lives.commit(MindJournal.Kind.GENESIS, identity.agentId(), "genesis:" + identity.agentId(),
					lives.genesisFields(identity, adapter.kind(), "", entityId))) {
				return;
			}
			record = AgentRecord.create(adapter, skills, identity, gate, this, home);
			facts = lives.continuityFor(identity.agentId(), () -> AgentContinuity.of(identity));
			facts.bound(adapter.kind(), "", entityId, server.getTicks());
			EnsouledMod.LOG.info("[ensouled] new mind {} ({}) bound to {} body",
					record.agentId(), record.name(), adapter.kind());
		}
		records.put(record.agentId(), record);
		saveAll();
		if (loaded != null && loaded.state() == ContinuityState.RECOVERING) {
			// Reconciliation is reported only once the memory of it is durable: the snapshot carrying
			// the interrupted action has landed, so the transition cannot be lost.
			lives.commit(MindJournal.Kind.RECONCILE, record.agentId(), "",
					Map.of("condition", loaded.condition(), "tick", Long.toString(server.getTicks())));
			facts.observed(ContinuityState.OPERATIONAL, "reconciled: " + loaded.condition(),
					lives.storageGeneration());
			EnsouledMod.LOG.info("[ensouled] continuity {} RECOVERING -> OPERATIONAL ({})",
					record.name(), loaded.condition());
		}
	}

	/** True when any mind gained or lost its body since the last check. */
	private boolean embodimentChanged() {
		boolean changed = false;
		for (AgentRecord record : records.values()) {
			boolean live = record.body() != null && record.body().alive();
			boolean known = embodied.contains(record.agentId());
			if (live && !known) {
				embodied.add(record.agentId());
				changed = true;
			} else if (!live && known) {
				embodied.remove(record.agentId());
				// A body can be lost (disconnect, death, removal). The life and its memory stay; losing
				// the body is a transition that must be readable after a crash, not an inference.
				lives.commit(MindJournal.Kind.UNBIND, record.agentId(),
						"unbind:" + record.agentId() + ":" + server.getTicks(),
						Map.of("body_kind", record.bodyKind(), "tick", Long.toString(server.getTicks())));
				AgentContinuity facts = lives.continuityOf(record.agentId());
				if (facts != null) {
					facts.sessionEnd(System.currentTimeMillis());
				}
				changed = true;
			}
		}
		return changed;
	}

	/** Spawn the secondary mob body (kept for the earlier verified slice and tests). */
	public AgentRecord spawnMob(ServerWorld world, Vec3i pos, String name) {
		if (!lives.canMint()) {
			EnsouledMod.LOG.error("[ensouled] refusing to spawn {}: {}", name, store() == null ? "no store" : store().describe());
			return null;
		}
		String agentId = "agent:" + UUID.randomUUID();
		AgentBodyEntity body = new AgentBodyEntity(EnsouledEntities.AGENT, world);
		body.setAgentId(agentId);
		body.setCustomName(Text.literal(name));
		body.setCustomNameVisible(true);
		body.refreshPositionAndAngles(pos.x() + 0.5, pos.y(), pos.z() + 0.5, world.getRandom().nextFloat() * 360.0f, 0.0f);
		if (!world.spawnEntity(body)) {
			EnsouledMod.LOG.warn("[ensouled] failed to spawn mob body at {}", pos);
		}
		MobBodyAdapter adapter = new MobBodyAdapter(body);
		return registerNew(adapter, name, pos, newIdentity(adapter.agentId(), name, server.getTicks()));
	}

	/** Spawn a primary fake-player body with an agent-owned profile. */
	public AgentRecord spawnPlayer(ServerWorld world, Vec3i pos, String name) {
		if (!lives.canMint()) {
			EnsouledMod.LOG.error("[ensouled] refusing to spawn {}: {}", name, store() == null ? "no store" : store().describe());
			return null;
		}
		UUID profileId = UUID.randomUUID();
		FakePlayerBody body = FakePlayerBody.materialize(server, world, profileId, name, pos);
		PlayerBodyAdapter adapter = new PlayerBodyAdapter(body);
		return registerNew(adapter, name, pos, newIdentity(adapter.agentId(), name, server.getTicks()));
	}

	private AgentRecord registerNew(BodyAdapter adapter, String name, Vec3i pos, AgentIdentity identity) {
		AuthorityGateway gate = AuthorityFactory.provisional(adapter.agentId(), pos, HOME_RADIUS, HARVEST_BUDGET);
		String entityId = adapter.entity() == null ? "" : adapter.entity().getUuidAsString();
		String profile = adapter instanceof PlayerBodyAdapter player && player.profileId() != null
				? player.profileId().toString()
				: adapter instanceof NetworkPlayerBodyAdapter network
						? network.player().getUuidAsString() : "";
		if (!lives.commit(MindJournal.Kind.GENESIS, identity.agentId(), "genesis:" + identity.agentId(),
				lives.genesisFields(identity, adapter.kind(), profile, entityId))) {
			return null;
		}
		AgentRecord record = AgentRecord.create(adapter, skills, identity, gate, this, pos);
		AgentContinuity facts = lives.continuityFor(identity.agentId(), () -> AgentContinuity.of(identity));
		facts.bound(adapter.kind(), profile, entityId, server.getTicks());
		records.put(record.agentId(), record);
		claimHome(record.agentId(), record.name(), pos);
		saveAll();
		return record;
	}

	private void materializePersistedPlayers() {
		for (MindStorage.LoadedMind mind : List.copyOf(lives.unbound())) {
			if (ExternalBrains.isExternal(mind.name())) {
				continue;
			}
			if (mind.state() == ContinuityState.INTEGRITY_BLOCKED) {
				continue;
			}
			if (!"player".equals(mind.bodyKind()) || mind.profileId().isEmpty()) {
				continue;
			}
			UUID profileId = parseUuid(mind.profileId());
			if (profileId == null || server.getPlayerManager().getPlayer(profileId) != null) {
				continue;
			}
			FakePlayerBody.materialize(server, server.getOverworld(), profileId, mind.name(), mind.home());
		}
	}

	/**
	 * The names that may become beings on connect. Unset, the roster is the corpus's own answer
	 * ({@link dev.ensouled.core.ResidentNames}): a name comes from the vocabulary the being speaks
	 * from, not from a string written into this class. `ENSOULED_RESIDENTS` overrides it for a run.
	 */
	private static java.util.Set<String> residentRoster() {
		String configured = System.getenv("ENSOULED_RESIDENTS");
		java.util.Set<String> names = new java.util.HashSet<>();
		if (configured == null || configured.isBlank()) {
			for (dev.ensouled.core.ResidentNames.Resident resident
					: dev.ensouled.core.ResidentNames.defaults()) {
				names.add(resident.name());
			}
		} else {
			for (String name : configured.split(",")) {
				if (!name.isBlank()) {
					names.add(name.trim());
				}
			}
		}
		return names.stream().map(name -> name.toUpperCase(java.util.Locale.ROOT))
				.collect(java.util.stream.Collectors.toUnmodifiableSet());
	}

	private static UUID parseUuid(String value) {
		if (value == null || value.isEmpty()) {
			return null;
		}
		try {
			return UUID.fromString(value);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private static AgentIdentity newIdentity(String agentId, String name, long tick) {
		return new AgentIdentity(agentId, name, "", "unspecified", System.currentTimeMillis(), tick,
				"phase1-spawn");
	}

	public Collection<AgentRecord> records() {
		return List.copyOf(records.values());
	}

	public Optional<AgentRecord> byName(String name) {
		for (AgentRecord record : records.values()) {
			if (record.name().equalsIgnoreCase(name)) {
				return Optional.of(record);
			}
		}
		return Optional.empty();
	}

	/**
	 * Registry truth for a connected session: which mind's body binding this player is. Empty for a
	 * human, and for a bot whose registry entry has not been bound yet — never inferred from a class
	 * (Addendum D §D.1, correction §2, §6).
	 */
	public Optional<String> agentIdForPlayer(ServerPlayerEntity player) {
		for (AgentRecord record : records.values()) {
			if (record.body() != null && record.body().entity() == player) {
				return Optional.of(record.agentId());
			}
		}
		return Optional.empty();
	}

	/**
	 * Removes one mind and its body from this server. Used by the attached acceptance harness to
	 * clean up only the fixture bodies its own previous run recorded, never a developer's citizens.
	 */
	public boolean remove(String agentId) {
		AgentRecord record = records.remove(agentId);
		if (record == null) {
			return false;
		}
		BodyAdapter body = record.body();
		if (body != null && body.entity() != null) {
			Entity entity = body.entity();
			if (entity instanceof ServerPlayerEntity player) {
				server.getPlayerManager().remove(player);
			}
			entity.discard();
		}
		EnsouledMod.LOG.info("[ensouled] removed mind {} ({}) and its body", agentId, record.name());
		return true;
	}

	/**
	 * A networked client with an agent's name is that agent's body of record: retire the in-process
	 * fake body so exactly one Ash exists, while the mind keeps its identity and memory.
	 */
	public void bindNetworkedPlayer(ServerPlayerEntity player) {
		String name = player.getGameProfile().getName();
		if (ExternalBrains.isExternal(name)) {
			// The mind stays here and selects through AXIS; the external client is the motor body.
			EnsouledMod.LOG.info("[ensouled] external-brain resident {}: AXIS decides, its own client executes the motor frames", name);
		}
		if (!RESIDENTS.contains(name.toUpperCase(java.util.Locale.ROOT))) {
			// A person joined. Their avatar stays theirs. This gate is checked before restoring as
			// well as before minting, because a mind already in the store under a person's name — a
			// life written by an earlier bug, or copied in from another world — would otherwise be
			// rebound on every join and act, and speak, through a human being's own session.
			EnsouledMod.LOG.info("[ensouled] session {} is a person, not a resident: no mind restores, mints or binds",
					name);
			return;
		}
		AgentRecord record = byName(name).orElse(null);
		MindStorage.LoadedMind restored = null;
		if (record == null) {
			// A persisted life whose own client arrives before any in-process body exists: the session
			// is the body and the mind is the one already carrying this name. No second being is minted,
			// and no blank mind replaces the one that lived.
			MindStorage.LoadedMind loaded = lives.takeUnbound(name).orElse(null);
			if (loaded != null && loaded.state() == ContinuityState.INTEGRITY_BLOCKED) {
				lives.hold(loaded);
				EnsouledMod.LOG.error("[ensouled] refusing client {}: its record is INTEGRITY_BLOCKED ({})",
						name, loaded.condition());
				return;
			}
			if (loaded == null) {
				if (!lives.canMint()) {
					EnsouledMod.LOG.error("[ensouled] refusing to mint a mind for client {}: {}", name,
							store() == null ? "no store" : store().describe());
					return;
				}
				// §48.2 genesis bootstrap: a session with no mind of its record, in a store that will take
				// it, is a resident arriving for the first time. Its session is its body; the identity is
				// minted once, governed and journalled before it acts. Without this a new world stays
				// silent however many clients join, which is not an empty population, it is a missing one.
				Vec3i first = new Vec3i(player.getBlockX(), player.getBlockY(), player.getBlockZ());
				NetworkPlayerBodyAdapter session = new NetworkPlayerBodyAdapter(player);
				AgentRecord minted = registerNew(session, name, first,
						newIdentity(session.agentId(), name, server.getTicks()));
				if (minted == null) {
					EnsouledMod.LOG.error("[ensouled] first-time client {} was not made durable; not acting",
							name);
					return;
				}
				EnsouledMod.LOG.info("[ensouled] minted {} for first-time client {} at {}", minted.agentId(),
						name, first);
				return;
			}
			restored = loaded;
			net.minecraft.util.math.BlockPos at = player.getBlockPos();
			Vec3i home = new Vec3i(at.getX(), at.getY(), at.getZ());
			AuthorityGateway gate = AuthorityFactory.provisional(loaded.agentId(), home, HOME_RADIUS,
					HARVEST_BUDGET);
			record = AgentRecord.restored(loaded.root(), skills, gate, loaded.name(), home, "player",
					parseUuid(loaded.profileId()), this);
			if (!home.equals(loaded.home())) {
				record.mind().ledger().remember(record.mind().tick(), EpisodeKind.OTHER,
						"woke at " + home + "; home was " + loaded.home() + " in the land before",
						home, "self_observed", "ev:home-reanchored");
			}
		records.put(record.agentId(), record);
		claimHome(record.agentId(), record.name(), home);
		EnsouledMod.LOG.info("[ensouled] restored persisted mind {} ({}) onto its client {}",
				record.agentId(), record.name(), name);
		}
		BodyAdapter current = record.body();
		if (current != null && current.entity() == player) {
			return;
		}
		// The in-process body of the same identity retires; the mind and its memory stay put. This
		// also covers a reconnect: the stale adapter is replaced by the session that is here now.
		if (current != null && current.entity() instanceof FakePlayerBody fake && !fake.isRemoved()) {
			server.getPlayerManager().remove(fake);
			fake.discard();
		}
		String entityId = player.getUuidAsString();
		String profile = record.profileId() == null ? "" : record.profileId().toString();
		// Write-ahead (§49.3): the session binding is durable before the mind acts through it.
		if (!lives.commit(MindJournal.Kind.BIND, record.agentId(),
				"bind:" + record.agentId() + ":player:" + (profile.isEmpty() ? entityId : profile),
				lives.bindFields("player", profile, entityId))) {
			return;
		}
		final AgentIdentity identity = record.mind().identity();
		AgentContinuity facts = lives.continuityFor(record.agentId(), () -> AgentContinuity.of(identity));
		facts.bound("player", profile, entityId, server.getTicks());
		record.attach(new NetworkPlayerBodyAdapter(player));
		saveAll();
		if (restored != null && restored.state() == ContinuityState.RECOVERING) {
			lives.commit(MindJournal.Kind.RECONCILE, record.agentId(), "",
					Map.of("condition", restored.condition(), "tick", Long.toString(server.getTicks())));
			facts.observed(ContinuityState.OPERATIONAL, "reconciled: " + restored.condition(),
					lives.storageGeneration());
			EnsouledMod.LOG.info("[ensouled] continuity {} RECOVERING -> OPERATIONAL ({})",
					record.name(), restored.condition());
		}
		EnsouledMod.LOG.info("[ensouled] agent {} now runs as its network client {}; fake body retired body={} alive={}",
				record.agentId(), name,
				record.body() == null ? "null" : record.body().getClass().getSimpleName(),
				record.body() != null && record.body().alive());
	}

	/**
	 * Give a being's home an identity the first time it is bound, so a plan can name the base it acts
	 * in instead of a coordinate that belongs to whatever world happens to be loaded (§A4.2–§A4.4).
	 */
	private void claimHome(String agentId, String name, Vec3i home) {
		WorldState world = WorldState.get();
		if (world != null) {
			world.claimHomebase(agentId, name, home, HOME_RADIUS, HARVEST_BUDGET, server.getTicks());
		}
	}

	/** The persisted life with this name that has no body yet, removed from the unbound set. */

	public Optional<AgentRecord> byAgentId(String agentId) {
		return Optional.ofNullable(records.get(agentId));
	}


	/** §49.3: a transition this returned false for did not reach the disk and must not be reported. */

	/** Only a store that loaded may be filled with new lives; a damaged one preserves what exists. */


	/** The identity fields a journal genesis carries, read from a snapshot that predates the journal. */


	/** Which executable knowledge this mind was running under, for the durable configuration record. */

	public MindStorage.Store store() {
		return lives.store();
	}

	public MindJournal journal() {
		return lives.journal();
	}

	public AgentContinuity continuityOf(String agentId) {
		return lives.continuityOf(agentId);
	}

	public long storageGeneration() {
		return lives.storageGeneration();
	}

	/** Every snapshot point: the journal, the lives that are waiting, and the lives at work. */
	public void saveAll() {
		lives.saveAll(List.copyOf(records.values()), server.getTicks());
	}

	@Override
	public MessageBus bus() {
		return speech.bus();
	}

	@Override
	public Optional<HumanChannel> humans() {
		return speech.humans();
	}

	@Override
	public Hearing hearing() {
		return speech.hearing();
	}

	@Override
	public Speech speech() {
		return speech.speech();
	}

	@Override
	public Optional<String> agentIdForEntity(String entityUuid) {
		return speech.agentIdForEntity(entityUuid);
	}

	/** Ordinary player chat, handed to every being that could hear it (CHAT-01). */
	public void hear(ServerPlayerEntity speaker, String text) {
		speech.hear(speaker, text);
	}

	public MinecraftServer server() {
		return server;
	}
}

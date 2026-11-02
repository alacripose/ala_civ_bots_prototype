package dev.ensouled.mind;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.server.MinecraftServer;

import dev.ensouled.EnsouledMod;
import dev.ensouled.core.AgentIdentity;
import dev.ensouled.core.ContinuityState;
import dev.ensouled.core.MindPersistence;
import dev.ensouled.core.SkillRegistry;
import dev.ensouled.core.StateNode;
import dev.ensouled.persist.AgentContinuity;
import dev.ensouled.persist.MindJournal;
import dev.ensouled.persist.MindStorage;

/**
 * What survives a restart: the append-only journal, the snapshots, and the condition every life came
 * back in (§49.1). One owner, because a life that is written by two of these is a life that can be
 * half-written.
 *
 * <p>This class also keeps the lives that are waiting: a persisted mind whose client has not arrived
 * yet is held here, not forgotten, and is handed over when the session carrying its name connects.
 * Binding a body to a life is {@link AgentManager}'s business; being durable is this one's.
 */
final class ContinuityLedger {

	private final MinecraftServer server;
	private final SkillRegistry skills;
	/** §49.3: the durable store and its append-only transition journal. */
	private final MindStorage storage;
	private final MindJournal journal;
	private MindStorage.Store store;
	/** §49.2: what is durable about each life, held per AgentId rather than per body. */
	private final Map<String, AgentContinuity> continuity = new LinkedHashMap<>();
	/** Event sequence reflected by the last snapshot, so new canonical events are made durable promptly. */
	private final Map<String, Long> savedEventSequence = new HashMap<>();
	/** Lives read from the store that have no body yet. */
	private final Map<String, MindStorage.LoadedMind> unbound = new LinkedHashMap<>();

	ContinuityLedger(MinecraftServer server, SkillRegistry skills) {
		this.server = server;
		this.skills = skills;
		this.storage = new MindStorage(MindStorage.directoryFor(server));
		this.journal = new MindJournal(storage.journalFile());
		// The being's decision path has no handle of its own: it reaches the same journal the
		// manager opened, so a decision is durable before its action exists (§49.3).
		MindJournal.install(journal);
	}

	MindJournal journal() {
		return journal;
	}

	MindStorage.Store store() {
		return store;
	}

	AgentContinuity continuityOf(String agentId) {
		return continuity.get(agentId);
	}

	AgentContinuity continuityFor(String agentId, java.util.function.Supplier<AgentContinuity> create) {
		return continuity.computeIfAbsent(agentId, id -> create.get());
	}

	long storageGeneration() {
		return storage.generation();
	}

	/** The event sequence the last snapshot already carries for this life. */
	Long savedEventSequence(String agentId) {
		return savedEventSequence.get(agentId);
	}

	MindStorage.LoadedMind unboundByAgentId(String agentId) {
		return unbound.get(agentId);
	}

	void recover() {
		store = storage.load(journal);
		boolean minting = canMint();
		EnsouledMod.LOG.info("[ensouled] continuity store: {}", store.describe());
		if (!minting) {
			EnsouledMod.LOG.error("[ensouled] store is {} ({}); no new mind will be minted until it is reconciled",
					store.outcome(), store.detail());
		}
		for (MindStorage.LoadedMind mind : store.minds()) {
			AgentContinuity facts = AgentContinuity.fromState(mind.continuity(), mind.root());
			facts.booted();
			facts.observed(mind.state(), mind.condition(), store.generation());
			continuity.put(mind.agentId(), facts);
			unbound.put(mind.agentId(), mind);
			if (!journal.recorded("genesis:" + mind.agentId())) {
				// A life older than this journal: its identity is adopted from the snapshot it was read
				// from, so the journal can still name it if that snapshot is later unreadable.
				Map<String, String> genesis = genesisFields(mind.root());
				if (mind.name() != null && !mind.name().isEmpty()) {
					// A blocked record has no readable state root; the name still comes from the store,
					// and the journal must not learn "unknown" in place of a person.
					genesis.put("name", mind.name());
				}
				genesis.put("body_kind", mind.bodyKind());
				genesis.put("profile", mind.profileId());
				genesis.put("dimension", mind.dimension());
				genesis.put("adopted_from_snapshot", "true");
				commit(MindJournal.Kind.GENESIS, mind.agentId(), "genesis:" + mind.agentId(), genesis);
			}
			if (mind.state() == ContinuityState.INTEGRITY_BLOCKED) {
				commit(MindJournal.Kind.RECOVERY, mind.agentId(),
						"blocked:" + mind.agentId() + ":" + store.generation(),
						Map.of("state", mind.state().name(), "condition", mind.condition()));
			}
			EnsouledMod.LOG.info("[ensouled] continuity {} [{}] state={} gen={} boots={} body={} condition={}",
					mind.name(), mind.agentId(), mind.state(), mind.generation(), facts.boots(),
					mind.bodyKind().isEmpty() ? "none" : mind.bodyKind(),
					mind.condition().isEmpty() ? "-" : mind.condition());
		}
		EnsouledMod.LOG.info("[ensouled] recovery complete: {} life(s) known, {} awaiting bodies, minting={}",
				store.minds().size(), unbound.size(), minting);
	}

	void saveAll(java.util.Collection<AgentRecord> records, long tick) {
		if (store != null && !store.writable()) {
			// §49.3: a failed load must not replace the last valid state on disk.
			EnsouledMod.LOG.warn("[ensouled] not writing a snapshot: the store did not load ({})",
					store.describe());
			return;
		}
		// Lives without a body are still lives: their last written state and continuity are carried
		// forward, so a snapshot taken while they wait does not erase them. A blocked record is not
		// rewritten at all; the store keeps its entry verbatim.
		Map<String, MindStorage.AgentSnapshot> snapshots = new LinkedHashMap<>();
		String skillFingerprint = skillFingerprint();
		for (MindStorage.LoadedMind mind : unbound.values()) {
			if (mind.state() == ContinuityState.INTEGRITY_BLOCKED) {
				continue;
			}
			AgentContinuity facts = continuity.computeIfAbsent(mind.agentId(),
					id -> AgentContinuity.fromState(mind.continuity(), mind.root()));
			snapshots.put(mind.agentId(), new MindStorage.AgentSnapshot(mind.agentId(), mind.name(), mind.home(),
					mind.dimension(), mind.bodyKind(), mind.profileId(), mind.root(), facts.toState()));
		}
		for (AgentRecord record : records) {
			final AgentIdentity identity = record.mind().identity();
			AgentContinuity facts = continuity.computeIfAbsent(record.agentId(),
					id -> AgentContinuity.of(identity));
			facts.observe(record.mind().needs(), record.mind().tick());
			facts.where(record.position(), record.dimension());
			facts.configure(record.gate().getClass().getSimpleName(), skillFingerprint);
			snapshots.put(record.agentId(), new MindStorage.AgentSnapshot(record.agentId(), record.name(),
					record.home(), record.dimension(), record.bodyKind(),
					record.profileId() == null ? "" : record.profileId().toString(),
					MindPersistence.save(record.mind()), facts.toState()));
			savedEventSequence.put(record.agentId(), record.mind().ledger().events().sequence());
		}
		if (snapshots.isEmpty()) {
			return;
		}
		storage.save(snapshots.values(), journal, tick);
	}

	boolean commit(MindJournal.Kind kind, String agentId, String txn, Map<String, String> fields) {
		try {
			journal.append(kind, agentId, txn, fields);
			return true;
		} catch (IllegalStateException e) {
			EnsouledMod.LOG.error("[ensouled] {} for {} was not made durable; refusing the transition",
					kind, agentId, e);
			return false;
		}
	}

	boolean canMint() {
		return store == null || store.writable();
	}

	Map<String, String> bindFields(String bodyKind, String profile, String entityId) {
		Map<String, String> fields = new LinkedHashMap<>();
		fields.put("body_kind", bodyKind);
		fields.put("profile", profile == null ? "" : profile);
		fields.put("entity", entityId == null ? "" : entityId);
		fields.put("tick", Long.toString(server.getTicks()));
		return fields;
	}

	Map<String, String> genesisFields(StateNode root) {
		Map<String, String> fields = new LinkedHashMap<>();
		fields.put("name", root.string("display_name", root.string("agent_id", "unknown")));
		fields.put("created_epoch", Long.toString(root.lng("created_epoch", 0L)));
		fields.put("birth_tick", Long.toString(root.lng("birth_tick", 0L)));
		fields.put("lineage", root.string("lineage", ""));
		fields.put("gender_schema", root.string("gender_schema", ""));
		fields.put("gender_value", root.string("gender_value", ""));
		return fields;
	}

	Map<String, String> genesisFields(AgentIdentity identity, String bodyKind, String profile,
			String entityId) {
		Map<String, String> fields = new LinkedHashMap<>();
		fields.put("name", identity.displayName());
		fields.put("created_epoch", Long.toString(identity.createdEpochMilli()));
		fields.put("birth_tick", Long.toString(identity.birthTick()));
		fields.put("lineage", identity.lineageNote());
		fields.put("gender_schema", identity.genderSchemaRef());
		fields.put("gender_value", identity.genderValue());
		fields.put("body_kind", bodyKind);
		fields.put("profile", profile == null ? "" : profile);
		fields.put("entity", entityId == null ? "" : entityId);
		fields.put("dimension", "minecraft:overworld");
		return fields;
	}

	String skillFingerprint() {
		StringBuilder sb = new StringBuilder();
		for (var definition : skills.definitions()) {
			if (sb.length() > 0) {
				sb.append(',');
			}
			sb.append(definition.key());
		}
		return sb.toString();
	}

	/** This life stops waiting: it is about to have a body again. */
	MindStorage.LoadedMind takeUnboundByAgentId(String agentId) {
		return unbound.remove(agentId);
	}

	/** The persisted life with this name that has no body yet; it stops waiting here. */
	java.util.Optional<MindStorage.LoadedMind> takeUnbound(String name) {
		for (MindStorage.LoadedMind mind : List.copyOf(unbound.values())) {
			if (mind.name().equalsIgnoreCase(name)) {
				unbound.remove(mind.agentId());
				return java.util.Optional.of(mind);
			}
		}
		return java.util.Optional.empty();
	}

	/** A life whose session arrived but could not be reconciled: it keeps waiting, on the record. */
	void hold(MindStorage.LoadedMind mind) {
		unbound.put(mind.agentId(), mind);
	}

	java.util.Collection<MindStorage.LoadedMind> unbound() {
		return unbound.values();
	}
}

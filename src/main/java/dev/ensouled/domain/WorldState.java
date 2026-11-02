package dev.ensouled.domain;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dev.ensouled.EnsouledMod;
import dev.ensouled.core.Vec3i;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The single owner of a world's durable facts: its identity, the revision commands are checked
 * against, the homebases that exist and the runs that have been opened (§A5.1–§A5.3, §A6.4).
 *
 * <p>Nothing else writes these. A command is accepted here or refused here, and the acceptance is
 * what moves the revision; a run is opened here and advanced here. Everything transient — encounters,
 * frames, routes — is owned elsewhere and is not stored here (§A5.2).
 *
 * <p>Idempotency is the dedupe key, not the command id: a client that retries after a dropped
 * acknowledgement sends the same key and gets {@link Result#REPLAYED} without a second change. §A6.4
 * names exactly that failure — the same base founded twice, the same packet spent twice.
 */
public final class WorldState {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final int APPLIED_KEYS_KEPT = 512;
	private static volatile WorldState current;

	private final Path file;
	private final Path directory;
	private WorldIdentity world;
	private final Map<String, Homebase> homebases = new LinkedHashMap<>();
	private final Map<String, Run> runs = new LinkedHashMap<>();
	private final Set<String> appliedKeys = new LinkedHashSet<>();

	private WorldState(Path file, WorldIdentity world) {
		this.file = file;
		this.directory = file.getParent();
		this.world = world;
	}

	/** The directory this world's own files live in: the one place its identity is written. */
	public Path directory() {
		return directory;
	}

	public enum Result {
		APPLIED, REPLAYED, STALE, WRONG_WORLD
	}

	public static WorldState open(MinecraftServer server) {
		Path root = server.getSavePath(net.minecraft.util.WorldSavePath.ROOT).resolve("ensouled");
		Path file = root.resolve("domain.json");
		WorldState state = load(file);
		if (state == null) {
			state = new WorldState(file, new WorldIdentity("world:" + UUID.randomUUID(),
					server.getOverworld().getSeed(), WorldIdentity.SCHEMA, 0L, server.getTicks()));
			state.save();
			EnsouledMod.LOG.info("[ensouled] opened world {} (schema {}, seed {})", state.world.worldId(),
					state.world.schemaVersion(), state.world.seed());
		} else {
			EnsouledMod.LOG.info("[ensouled] resumed world {} at revision {} ({} homebase(s), {} run(s))",
					state.world.worldId(), state.world.revision(), state.homebases.size(), state.runs.size());
		}
		current = state;
		return state;
	}

	public static WorldState get() {
		return current;
	}

	/** A world over an explicit file: the server uses {@link #open}, a test uses this. */
	static WorldState at(Path file, WorldIdentity identity) {
		WorldState loaded = load(file);
		if (loaded != null) {
			current = loaded;
			return loaded;
		}
		WorldState state = new WorldState(file, identity);
		state.save();
		current = state;
		return state;
	}

	public WorldIdentity world() {
		return world;
	}

	public long revision() {
		return world.revision();
	}

	public List<Homebase> homebases() {
		return List.copyOf(homebases.values());
	}

	public List<Run> runs() {
		return List.copyOf(runs.values());
	}

	/**
	 * Accept a command against the revision and key it carries. A repeat is a no-op, a stale revision
	 * is refused, and only an applied command moves the revision.
	 */
	public synchronized Result accept(Command command, long tick) {
		if (command == null) {
			return Result.STALE;
		}
		if (!world.worldId().equals(command.worldId())) {
			return Result.WRONG_WORLD;
		}
		if (appliedKeys.contains(command.dedupeKey())) {
			return Result.REPLAYED;
		}
		if (command.expectedRevision() != world.revision()) {
			return Result.STALE;
		}
		appliedKeys.add(command.dedupeKey());
		while (appliedKeys.size() > APPLIED_KEYS_KEPT) {
			String oldest = appliedKeys.iterator().next();
			appliedKeys.remove(oldest);
		}
		world = world.advance();
		save();
		EnsouledMod.LOG.info("[ensouled] command {} {} applied as {} at revision {} (tick {})",
				command.type(), command.commandId(), command.idempotencyKey(), world.revision(), tick);
		return Result.APPLIED;
	}

	/**
	 * Whether this key has already been applied, without applying anything. A caller that must do
	 * side-effecting work before it can build a command uses this to ask first, so a duplicate is a
	 * no-op rather than a revision moved for something nobody received.
	 */
	public synchronized boolean hasApplied(String idempotencyKey) {
		return appliedKeys.contains(world.worldId() + "|" + idempotencyKey);
	}

	/** Register the home a being claims. One homebase per agent, re-claimed in place if it moves. */
	public synchronized Homebase claimHomebase(String agentId, String name, Vec3i centre, int claimRadius,
			int upkeepBuffer, long tick) {
		for (Homebase existing : homebases.values()) {
			if (existing.ownerAgentId().equals(agentId)) {
				Homebase moved = new Homebase(existing.baseId(), world.worldId(), name, agentId, centre,
						claimRadius, upkeepBuffer, existing.foundedTick());
				homebases.put(moved.baseId(), moved);
				save();
				return moved;
			}
		}
		Homebase base = new Homebase("base:" + UUID.randomUUID(), world.worldId(), name, agentId, centre,
				claimRadius, upkeepBuffer, tick);
		homebases.put(base.baseId(), base);
		save();
		return base;
	}

	public synchronized Run openRun(String agentId, String purpose, long tick) {
		Run run = new Run("run:" + UUID.randomUUID(), world.worldId(), agentId, purpose, Run.Stage.CONSIDERED,
				tick, 0L);
		runs.put(run.runId(), run);
		save();
		EnsouledMod.LOG.info("[ensouled] run {} opened for {}: {}", run.runId(), agentId, purpose);
		return run;
	}

	public synchronized Optional<Run> advanceRun(String runId, Run.Stage stage, long tick) {
		Run run = runs.get(runId);
		if (run == null) {
			return Optional.empty();
		}
		Run next = run.advance(stage, tick);
		runs.put(runId, next);
		save();
		return Optional.of(next);
	}

	/** The homebase this agent owns, if any. */
	public Optional<Homebase> homebaseOf(String agentId) {
		for (Homebase base : homebases.values()) {
			if (base.ownerAgentId().equals(agentId)) {
				return Optional.of(base);
			}
		}
		return Optional.empty();
	}

	public String describe() {
		StringBuilder out = new StringBuilder("world ").append(world.worldId())
				.append(" rev=").append(world.revision())
				.append(" schema=").append(world.schemaVersion())
				.append(" bases=").append(homebases.size())
				.append(" runs=").append(runs.size());
		for (Homebase base : homebases.values()) {
			out.append("\n  ").append(base.baseId()).append(" ").append(base.name())
					.append(" by ").append(base.ownerAgentId()).append(" at ").append(base.centre());
		}
		for (Run run : runs.values()) {
			out.append("\n  ").append(run.runId()).append(" ").append(run.stage())
					.append(" ").append(run.purpose()).append(run.closed() ? " (closed)" : "");
		}
		return out.toString();
	}

	// ---------------------------------------------------------------- persistence (§A5.2)

	private synchronized void save() {
		try {
			Files.createDirectories(file.getParent());
			JsonObject root = new JsonObject();
			JsonObject identity = new JsonObject();
			identity.addProperty("world_id", world.worldId());
			identity.addProperty("seed", world.seed());
			identity.addProperty("schema_version", world.schemaVersion());
			identity.addProperty("revision", world.revision());
			identity.addProperty("opened_tick", world.openedTick());
			root.add("world", identity);

			com.google.gson.JsonArray bases = new com.google.gson.JsonArray();
			for (Homebase base : homebases.values()) {
				JsonObject row = new JsonObject();
				row.addProperty("base_id", base.baseId());
				row.addProperty("name", base.name());
				row.addProperty("owner_agent_id", base.ownerAgentId());
				row.addProperty("x", base.centre().x());
				row.addProperty("y", base.centre().y());
				row.addProperty("z", base.centre().z());
				row.addProperty("claim_radius", base.claimRadius());
				row.addProperty("upkeep_buffer", base.upkeepBuffer());
				row.addProperty("founded_tick", base.foundedTick());
				bases.add(row);
			}
			root.add("homebases", bases);

			com.google.gson.JsonArray runRows = new com.google.gson.JsonArray();
			for (Run run : runs.values()) {
				JsonObject row = new JsonObject();
				row.addProperty("run_id", run.runId());
				row.addProperty("agent_id", run.agentId());
				row.addProperty("purpose", run.purpose());
				row.addProperty("stage", run.stage().name());
				row.addProperty("opened_tick", run.openedTick());
				row.addProperty("closed_tick", run.closedTick());
				runRows.add(row);
			}
			root.add("runs", runRows);

			com.google.gson.JsonArray keys = new com.google.gson.JsonArray();
			for (String key : appliedKeys) {
				keys.add(key);
			}
			root.add("applied_commands", keys);

			Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
		} catch (IOException failure) {
			EnsouledMod.LOG.error("[ensouled] could not write {}: {}", file, failure.getMessage());
		}
	}

	private static WorldState load(Path file) {
		if (!Files.isRegularFile(file)) {
			return null;
		}
		try {
			JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8))
					.getAsJsonObject();
			JsonObject identity = root.getAsJsonObject("world");
			WorldIdentity world = new WorldIdentity(identity.get("world_id").getAsString(),
					identity.get("seed").getAsLong(), identity.get("schema_version").getAsInt(),
					identity.get("revision").getAsLong(), identity.get("opened_tick").getAsLong());
			WorldState state = new WorldState(file, world);
			if (root.has("homebases")) {
				for (var element : root.getAsJsonArray("homebases")) {
					JsonObject row = element.getAsJsonObject();
					Homebase base = new Homebase(row.get("base_id").getAsString(), world.worldId(),
							row.get("name").getAsString(), row.get("owner_agent_id").getAsString(),
							new Vec3i(row.get("x").getAsInt(), row.get("y").getAsInt(), row.get("z").getAsInt()),
							row.get("claim_radius").getAsInt(), row.get("upkeep_buffer").getAsInt(),
							row.get("founded_tick").getAsLong());
					state.homebases.put(base.baseId(), base);
				}
			}
			if (root.has("runs")) {
				for (var element : root.getAsJsonArray("runs")) {
					JsonObject row = element.getAsJsonObject();
					Run run = new Run(row.get("run_id").getAsString(), world.worldId(),
							row.get("agent_id").getAsString(), row.get("purpose").getAsString(),
							Run.Stage.valueOf(row.get("stage").getAsString()),
							row.get("opened_tick").getAsLong(), row.get("closed_tick").getAsLong());
					state.runs.put(run.runId(), run);
				}
			}
			if (root.has("applied_commands")) {
				for (var element : root.getAsJsonArray("applied_commands")) {
					state.appliedKeys.add(element.getAsString());
				}
			}
			return state;
		} catch (IOException | RuntimeException unreadable) {
			EnsouledMod.LOG.error("[ensouled] {} is unreadable ({}); a new world will be minted",
					file, unreadable.getMessage());
			return null;
		}
	}
}

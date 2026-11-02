package dev.ensouled.persist;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import dev.ensouled.EnsouledMod;
import dev.ensouled.core.ContinuityState;
import dev.ensouled.core.MindPersistence;
import dev.ensouled.core.StateNode;
import dev.ensouled.core.Vec3i;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

/**
 * V4 §49.1–§49.3: the durable store behind a person, not behind a process.
 *
 * <p>Every save is a whole new generation written to {@code minds.dat.tmp}, forced to disk, and
 * atomically moved over {@code minds.dat}, with the previous generation kept as
 * {@code minds.dat.prev}. A process killed at any point therefore leaves a complete snapshot —
 * the generation being written or the one before it — and never a half-written file. Load reports
 * the four §16 conditions explicitly ({@code ABSENT}, {@code LOADED}, {@code CORRUPT},
 * {@code UNSUPPORTED}) and never turns an unreadable record into a blank mind; the agent is marked
 * {@link ContinuityState#INTEGRITY_BLOCKED} and its artifacts are left untouched.
 *
 * <p>{@link MindJournal} carries the transitions. This class only commits snapshots; it refuses to
 * write while the store it loaded is not itself readable, so a failed load cannot erase the last
 * valid state.
 */
public final class MindStorage {
	/** NBT layout of the container. Additive fields keep old snapshots readable. */
	public static final int FORMAT = 2;
	/** Layout of the continuity record inside each agent entry. */
	public static final int SCHEMA = 1;

	public enum Outcome {
		/** No snapshot file exists. */
		ABSENT,
		/** A snapshot was read and every entry decoded. */
		LOADED,
		/** A snapshot exists but could not be read as NBT. */
		CORRUPT,
		/** A snapshot exists, in a container format this build does not read. */
		UNSUPPORTED
	}

	public record AgentSnapshot(String agentId, String name, Vec3i home, String dimension,
			String bodyKind, String profileId, StateNode mind, StateNode continuity) {
	}

	public record LoadedMind(String agentId, String name, Vec3i home, String dimension, String bodyKind,
			String profileId, StateNode root, StateNode continuity, ContinuityState state, String condition,
			long generation) {
	}

	public record Store(Outcome outcome, String detail, long generation, String commitId, long journalSeq,
			boolean usedPrevious, boolean orphanedTemporary, List<LoadedMind> minds) {

		/** §49.3: an unreadable store must not be overwritten by the next save. */
		public boolean writable() {
			return outcome == Outcome.LOADED || outcome == Outcome.ABSENT;
		}

		public String describe() {
			StringBuilder sb = new StringBuilder("outcome=").append(outcome)
					.append(" loaded_gen=").append(generation)
					.append(" loaded_seq=").append(journalSeq)
					.append(" snapshot=")
					.append(outcome == Outcome.ABSENT ? "none" : (usedPrevious ? "previous" : "current"))
					.append(" minds=").append(minds.size());
			if (detail != null && !detail.isEmpty()) {
				sb.append(" (").append(detail).append(')');
			}
			return sb.toString();
		}
	}

	private record Attempt(Outcome outcome, NbtCompound root, String detail) {
	}

	private final File directory;
	private long generation;
	private boolean writable = true;
	/**
	 * Entries read from disk, carried forward on every save. A life this process is not holding —
	 * because its body has not arrived, or because its own entry is blocked — keeps its record
	 * instead of being silently dropped by the next snapshot.
	 */
	private NbtCompound loadedAgents = new NbtCompound();

	public MindStorage(File directory) {
		this.directory = directory;
	}

	public static File directoryFor(MinecraftServer server) {
		Path worldRoot = server.getSavePath(WorldSavePath.ROOT);
		return worldRoot.resolve("ensouled").toFile();
	}

	public File directory() {
		return directory;
	}

	public File currentFile() {
		return new File(directory, "minds.dat");
	}

	public File previousFile() {
		return new File(directory, "minds.dat.prev");
	}

	public File temporaryFile() {
		return new File(directory, "minds.dat.tmp");
	}

	public File journalFile() {
		return new File(directory, "journal.log");
	}

	public long generation() {
		return generation;
	}

	public boolean writable() {
		return writable;
	}

	public Store load(MindJournal journal) {
		File current = currentFile();
		File previous = previousFile();
		boolean orphaned = quarantineOrphanTemporary();

		if (!current.exists() && !previous.exists()) {
			generation = 0;
			List<LoadedMind> pending = genesisOnlyMinds(journal);
			return new Store(Outcome.ABSENT, "no snapshot on disk", 0, "", seqOf(journal), false, orphaned,
					pending);
		}

		Attempt attempt = read(current);
		boolean usedPrevious = false;
		if (!attempt.outcome().equals(Outcome.LOADED) && previous.exists()) {
			Attempt fallback = read(previous);
			if (fallback.outcome().equals(Outcome.LOADED)) {
				usedPrevious = true;
				EnsouledMod.LOG.warn("[ensouled] {} was {} ({}); recovered generation from {}",
						current.getName(), attempt.outcome(), attempt.detail(), previous.getName());
				attempt = fallback;
			}
		}

		if (!attempt.outcome().equals(Outcome.LOADED)) {
			generation = 0;
			writable = false;
			// §49.1: a life whose record cannot be read is not a life with no memory. Its identity
			// still comes from the journal, and it may not act until a human reconciles the store.
			List<LoadedMind> blocked = blockedFrom(journal,
					"store " + attempt.outcome() + ": " + attempt.detail());
			return new Store(attempt.outcome(), attempt.detail(), 0, "", seqOf(journal), usedPrevious, orphaned,
					blocked);
		}

		NbtCompound root = attempt.root();
		generation = root.getLong("Generation");
		String commitId = root.getString("CommitId");
		long journalSeq = root.getLong("JournalSeq");
		List<LoadedMind> minds = new ArrayList<>();
		Set<String> seen = new LinkedHashSet<>();
		NbtCompound agents = root.getCompound("Agents");
		loadedAgents = agents.copy();
		for (String agentId : agents.getKeys()) {
			seen.add(agentId);
			minds.add(decode(agentId, agents.getCompound(agentId)));
		}
		for (LoadedMind genesis : genesisOnlyMinds(journal)) {
			if (!seen.contains(genesis.agentId())) {
				minds.add(genesis);
			}
		}
		writable = true;
		return new Store(Outcome.LOADED, attempt.detail(), generation, commitId, journalSeq, usedPrevious,
				orphaned, minds);
	}

	/**
	 * Writes a new generation and returns its number, or -1 when nothing was written. The caller
	 * journals the commit only after this returns, so a crash leaves an older but complete store.
	 */
	public long save(Collection<AgentSnapshot> snapshots, MindJournal journal, long tick) {
		if (!writable) {
			EnsouledMod.LOG.error("[ensouled] refusing to write {}: the store did not load; artifacts are kept",
					currentFile());
			return -1;
		}
		if (snapshots.isEmpty()) {
			// Nothing to replace. Rotating generations for an empty set could put an empty file where
			// a life was, so the snapshot is simply not taken.
			return -1;
		}
		long next = generation + 1;
		String commitId = UUID.randomUUID().toString();
		NbtCompound root = new NbtCompound();
		root.putInt("Format", FORMAT);
		root.putInt("Schema", SCHEMA);
		root.putLong("Generation", next);
		root.putString("CommitId", commitId);
		root.putLong("JournalSeq", seqOf(journal));
		root.putLong("WrittenTick", tick);
		root.putLong("WrittenAt", System.currentTimeMillis());
		// Start from what is already durable, then replace only the entries this process holds state
		// for. A blocked or not-yet-reconnected life keeps the exact record it had.
		NbtCompound agents = loadedAgents.copy();
		for (AgentSnapshot snapshot : snapshots) {
			NbtCompound entry = NbtStateCodec.toNbt(snapshot.mind());
			entry.putString("Name", snapshot.name());
			entry.putString("Dimension", snapshot.dimension());
			entry.putString("BodyKind", snapshot.bodyKind());
			entry.putString("Profile", snapshot.profileId());
			NbtCompound home = new NbtCompound();
			home.putInt("x", snapshot.home().x());
			home.putInt("y", snapshot.home().y());
			home.putInt("z", snapshot.home().z());
			entry.put("Home", home);
			entry.put("Continuity", NbtStateCodec.toNbt(snapshot.continuity()));
			agents.put(snapshot.agentId(), entry);
		}
		root.put("Agents", agents);

		File temporary = temporaryFile();
		File current = currentFile();
		try {
			Files.createDirectories(directory.toPath());
			// 1. A complete new generation, forced to disk while it still has a private name.
			try (OutputStream out = Files.newOutputStream(temporary.toPath(),
					StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
				NbtIo.writeCompressed(root, out);
			}
			try (FileChannel channel = FileChannel.open(temporary.toPath(), StandardOpenOption.WRITE)) {
				channel.force(true);
			}
			// 2. The generation that was serving becomes the fallback; the new one takes its place
			//    with one atomic rename. A kill between the two leaves .prev readable, which is why
			//    load falls back to it.
			if (current.exists()) {
				Files.move(current.toPath(), previousFile().toPath(), StandardCopyOption.REPLACE_EXISTING);
			}
			Files.move(temporary.toPath(), current.toPath(), StandardCopyOption.REPLACE_EXISTING,
					StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException e) {
			EnsouledMod.LOG.error("[ensouled] snapshot generation {} failed; previous generation is untouched",
					next, e);
			return -1;
		}
		generation = next;
		loadedAgents = agents;
		if (journal != null) {
			try {
				journal.append(MindJournal.Kind.SNAPSHOT, "", "snapshot:" + next, Map.of(
						"generation", Long.toString(next),
						"commit", commitId,
						"minds", Integer.toString(snapshots.size()),
						"tick", Long.toString(tick)));
			} catch (IllegalStateException e) {
				// The file is already durable; only the bookkeeping entry failed. The next boot can
				// still read it, and the journal names the generation it is behind on.
				EnsouledMod.LOG.error("[ensouled] snapshot gen={} is durable but its journal entry failed",
						next, e);
			}
		}
		EnsouledMod.LOG.info("[ensouled] snapshot gen={} commit={} minds={} -> {}", next,
				commitId.substring(0, 8), snapshots.size(), current);
		return next;
	}

	// ---------------------------------------------------------------- reading

	private Attempt read(File file) {
		if (!file.exists()) {
			return new Attempt(Outcome.ABSENT, null, file.getName() + " is absent");
		}
		try {
			NbtCompound root;
			try (var in = Files.newInputStream(file.toPath())) {
				root = NbtIo.readCompressed(in);
			}
			int format = root.getInt("Format");
			if (format != FORMAT) {
				return new Attempt(Outcome.UNSUPPORTED, null,
						file.getName() + " format " + format + ", this build reads " + FORMAT);
			}
			return new Attempt(Outcome.LOADED, root, file.getName());
		} catch (IOException | RuntimeException e) {
			return new Attempt(Outcome.CORRUPT, null, file.getName() + " could not be read: " + e);
		}
	}

	private LoadedMind decode(String agentId, NbtCompound entry) {
		String name = entry.getString("Name");
		NbtCompound homeNbt = entry.getCompound("Home");
		Vec3i home = new Vec3i(homeNbt.getInt("x"), homeNbt.getInt("y"), homeNbt.getInt("z"));
		String dimension = entry.getString("Dimension");
		String bodyKind = entry.getString("BodyKind");
		String profileId = entry.getString("Profile");
		int mindFormat = entry.getInt("mind_format");
		if (mindFormat > MindPersistence.FORMAT) {
			return new LoadedMind(agentId, name, home, dimension, bodyKind, profileId, new StateNode(),
					new StateNode(), ContinuityState.INTEGRITY_BLOCKED,
					"mind format " + mindFormat + " is newer than this build reads (" + MindPersistence.FORMAT
							+ "); the snapshot is kept and not reinterpreted",
					0);
		}
		try {
			StateNode root = NbtStateCodec.fromNbt(entry);
			StateNode continuity = NbtStateCodec.fromNbt(entry.getCompound("Continuity"));
			String interruption = interruptionIn(root);
			ContinuityState state = interruption.isEmpty() ? ContinuityState.OPERATIONAL
					: ContinuityState.RECOVERING;
			return new LoadedMind(agentId, name, home, dimension, bodyKind, profileId, root, continuity, state,
					interruption, 0);
		} catch (RuntimeException e) {
			return new LoadedMind(agentId, name, home, dimension, bodyKind, profileId, new StateNode(),
					new StateNode(), ContinuityState.INTEGRITY_BLOCKED,
					"entry could not be decoded: " + e, 0);
		}
	}

	/** §16: a stop mid-action is indeterminate until the world is observed, never a failure or a retry. */
	private static String interruptionIn(StateNode root) {
		StateNode inFlight = root.node("in_flight");
		if (!inFlight.keys().contains("intention")) {
			return "";
		}
		Vec3i target = new Vec3i(inFlight.integer("x", 0), inFlight.integer("y", 0), inFlight.integer("z", 0));
		return "interrupted " + inFlight.string("skill", "?") + " at " + target
				+ "; outcome indeterminate until observed";
	}

	/**
	 * A life whose genesis is in the journal but whose snapshot never landed. The identity is real
	 * and must not be re-minted; its memory is whatever the record says (nothing), so it recovers.
	 */
	private List<LoadedMind> genesisOnlyMinds(MindJournal journal) {
		List<LoadedMind> minds = new ArrayList<>();
		if (journal == null) {
			return minds;
		}
		Set<String> seen = new LinkedHashSet<>();
		for (MindJournal.Entry entry : journal.entries()) {
			if (entry.kind() != MindJournal.Kind.GENESIS || !seen.add(entry.agentId())) {
				continue;
			}
			Vec3i home = new Vec3i(0, 0, 0);
			String dimension = entry.field("dimension", "minecraft:overworld");
			StateNode root = new StateNode()
					.putString("agent_id", entry.agentId())
					.putString("display_name", entry.field("name", entry.agentId()))
					.putString("gender_schema", entry.field("gender_schema", ""))
					.putString("gender_value", entry.field("gender_value", ""))
					.putLong("created_epoch", entry.lng("created_epoch", entry.atMilli()))
					.putLong("birth_tick", entry.lng("birth_tick", 0L))
					.putString("lineage", entry.field("lineage", ""))
					.putString("dimension", dimension);
			minds.add(new LoadedMind(entry.agentId(), entry.field("name", entry.agentId()), home, dimension,
					entry.field("body_kind", ""), entry.field("profile", ""), root, new StateNode(),
					ContinuityState.RECOVERING,
					"genesis is recorded but no snapshot was ever committed; the identity survives, the memory is empty",
					0));
		}
		return minds;
	}

	/**
	 * A save that died while writing leaves a temporary file that was never moved into place. It is
	 * not a generation; keep it as evidence and start the next save from a clean name.
	 */
	private boolean quarantineOrphanTemporary() {
		File temporary = temporaryFile();
		if (!temporary.exists()) {
			return false;
		}
		File evidence = new File(directory, "minds.dat.tmp.orphan-" + System.currentTimeMillis());
		try {
			Files.move(temporary.toPath(), evidence.toPath(), StandardCopyOption.REPLACE_EXISTING);
			EnsouledMod.LOG.warn("[ensouled] an uncommitted snapshot ({}) was left by a failed save; kept as {}",
					temporary.getName(), evidence.getName());
			return true;
		} catch (IOException e) {
			EnsouledMod.LOG.warn("[ensouled] could not move {} aside", temporary, e);
			return true;
		}
	}

	private static long seqOf(MindJournal journal) {
		return journal == null ? 0 : journal.lastSeq();
	}

	/** Every life the journal knows, for a store whose snapshot could not be read. */
	public static List<LoadedMind> blockedFrom(MindJournal journal, String reason) {
		List<LoadedMind> blocked = new ArrayList<>();
		Set<String> seen = new LinkedHashSet<>();
		if (journal == null) {
			return blocked;
		}
		for (MindJournal.Entry entry : journal.entries()) {
			if (entry.kind() != MindJournal.Kind.GENESIS || !seen.add(entry.agentId())) {
				continue;
			}
			blocked.add(new LoadedMind(entry.agentId(), entry.field("name", entry.agentId()),
					new Vec3i(0, 0, 0), entry.field("dimension", "minecraft:overworld"),
					entry.field("body_kind", ""), entry.field("profile", ""), new StateNode(), new StateNode(),
					ContinuityState.INTEGRITY_BLOCKED, reason, 0));
		}
		return blocked;
	}

}

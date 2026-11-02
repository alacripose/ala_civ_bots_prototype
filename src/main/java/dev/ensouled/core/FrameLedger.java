package dev.ensouled.core;

import com.google.gson.JsonObject;

import dev.ensouled.EnsouledMod;
import dev.ensouled.domain.Encounter;
import dev.ensouled.domain.WorldState;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Where a classified utterance becomes evidence. One row per frame, appended as it happens, stamped
 * with the world and the encounter it belongs to so a reader can tell two exchanges apart.
 *
 * <p>This is the caller the parser was missing. It classifies; it never speaks, decides or acts —
 * a frame is a reading of something somebody actually said.
 *
 * <p>An encounter is a short segment (§A7) and is deliberately not persisted: it is closed by silence
 * rather than by a write, so a crash loses the grouping and not the content.
 */
public final class FrameLedger {
	/** How long a conversation stays open without a line, in ticks. */
	private static final long ENCOUNTER_QUIET_TICKS = 400;
	/** Rows are bounded per world so a long session cannot fill the disk with readings. */
	private static final long MAX_ROWS = 20_000;

	private static final AtomicLong SEQUENCE = new AtomicLong();
	private static Encounter encounter;
	private static long lastTick = Long.MIN_VALUE;
	private static long rows;
	private static boolean full;

	private FrameLedger() {
	}

	/** Classify one utterance and record it. Returns the frame, or null when there is nothing to read. */
	public static VerbFrame record(String source, String actor, String text, long tick) {
		WorldState world = WorldState.get();
		if (world == null || text == null || text.isBlank()) {
			return null;
		}
		VerbFrame frame = FrameParser.parse(tick, source, actor, text, (int) SEQUENCE.incrementAndGet());
		if (full) {
			return frame;
		}
		Encounter open = encounter(tick, actor == null || actor.isBlank() ? "unknown" : actor);
		JsonObject row = frame.json();
		row.addProperty("world_id", world.world().worldId());
		row.addProperty("encounter_id", open.encounterId());
		row.addProperty("shape_basis", FrameParser.shapeBasis(text, frame.claimShape()));
		row.addProperty("verb_basis", FrameParser.verbBasis(frame.verb()));
		append(row);
		return frame;
	}

	/** The current encounter, opened on first use and replaced after a quiet stretch. */
	private static Encounter encounter(long tick, String actor) {
		if (encounter == null || tick - lastTick > ENCOUNTER_QUIET_TICKS) {
			WorldState world = WorldState.get();
			encounter = new Encounter("encounter:" + java.util.UUID.randomUUID(),
					world == null ? "" : world.world().worldId(), java.util.List.of(actor), tick);
		} else {
			encounter = encounter.with(actor);
		}
		lastTick = tick;
		return encounter;
	}

	/**
	 * Where the rows go. The world owns this path: a relative one resolves against whatever directory
	 * the server was started in, which put an earlier run's rows in {@code run/run/world/...} while the
	 * reader looked somewhere else.
	 */
	public static Path file() {
		WorldState world = WorldState.get();
		if (world == null) {
			return Path.of("frames.jsonl");
		}
		return world.directory().resolve("frames.jsonl");
	}

	public static String describe() {
		return "frames " + file() + " rows=" + rows + (full ? " (full: further frames are classified but not written)" : "");
	}

	private static synchronized void append(JsonObject row) {
		if (rows >= MAX_ROWS) {
			full = true;
			EnsouledMod.LOG.warn("[ensouled] frame ledger reached {} rows; classification continues unrecorded", MAX_ROWS);
			return;
		}
		try {
			Path path = file();
			Files.createDirectories(path.getParent());
			Files.writeString(path, row.toString() + "\n", StandardCharsets.UTF_8,
					StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			rows++;
		} catch (IOException failure) {
			EnsouledMod.LOG.warn("[ensouled] frame could not be recorded: {}", failure.getMessage());
		}
	}
}

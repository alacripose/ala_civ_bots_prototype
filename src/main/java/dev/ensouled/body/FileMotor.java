package dev.ensouled.body;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dev.ensouled.core.Motor;
import dev.ensouled.core.MotorCommand;
import dev.ensouled.core.MotorResult;
import dev.ensouled.core.MotorStatus;
import dev.ensouled.core.Vec3i;
import dev.ensouled.domain.WorldState;

import net.minecraft.server.network.ServerPlayerEntity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A motor whose body is an ordinary external client rather than a server-side fake player and
 * rather than the Fabric control channel.
 *
 * <p>The server keeps the mind, AXIS and the wish desk. This class writes one bounded motor frame at
 * a time to {@code world/ensouled/motor/<name>.commands.jsonl}; the external client performs the
 * frame through its ordinary login and player input, then appends an observed result to
 * {@code <name>.results.jsonl}. The server never reads an acknowledgement as truth: the mind still
 * verifies the postcondition from its own view of the player and inventory.
 */
public final class FileMotor implements Motor {
	private static final Gson GSON = new Gson();

	private final Path commands;
	private final Path results;
	private MotorCommand active;
	private String activeId;
	private int elapsed;
	private MotorResult last = MotorResult.cancelled("idle");
	private boolean writeFailed;

	public FileMotor(ServerPlayerEntity player) {
		String name = player.getGameProfile().getName();
		Path directory = directoryFor(player);
		this.commands = directory.resolve(name + ".commands.jsonl");
		this.results = directory.resolve(name + ".results.jsonl");
	}

	/**
	 * Writes one line the mind asked this body to say. The external client performs it through its
	 * own ordinary chat send. Speech is deliberately separate from the motor slot: a being may speak
	 * while walking, and a line must not wait behind a movement frame.
	 */
	public static boolean say(ServerPlayerEntity player, String text) {
		if (text == null || text.isBlank()) {
			return false;
		}
		Path directory = directoryFor(player);
		Path speech = directory.resolve(player.getGameProfile().getName() + ".speech.jsonl");
		JsonObject row = new JsonObject();
		row.addProperty("id", "speech:" + UUID.randomUUID());
		row.addProperty("kind", "Say");
		row.addProperty("text", text);
		try {
			Files.createDirectories(directory);
			Files.writeString(speech, GSON.toJson(row) + "\n", StandardCharsets.UTF_8,
					StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			return true;
		} catch (IOException failed) {
			return false;
		}
	}

	private static Path directoryFor(ServerPlayerEntity player) {
		return WorldState.get() == null
				? Path.of("run", "world", "ensouled", "motor")
				: WorldState.get().directory().resolve("motor");
	}

	@Override
	public void start(MotorCommand command) {
		if (active != null) {
			throw new IllegalStateException("motor already executing " + active.describe());
		}
		active = command;
		activeId = "motor:" + UUID.randomUUID();
		elapsed = 0;
		writeFailed = false;
		JsonObject row = new JsonObject();
		row.addProperty("id", activeId);
		row.addProperty("kind", kindOf(command));
		row.addProperty("timeoutTicks", command.timeoutTicks());
		if (command instanceof MotorCommand.MoveTo move) {
			row.add("target", vec(move.target()));
			row.addProperty("radius", move.radius());
		} else if (command instanceof MotorCommand.LookAt look) {
			row.add("target", vec(look.target()));
		} else if (command instanceof MotorCommand.MineBlock mine) {
			row.add("target", vec(mine.target()));
			row.addProperty("expectedBlockId", mine.expectedBlockId());
		} else if (command instanceof MotorCommand.Use use) {
			row.add("target", vec(use.target()));
			row.addProperty("expectedBlockId", use.expectedBlockId());
		} else if (command instanceof MotorCommand.Equip equip) {
			row.addProperty("itemId", equip.itemId());
		} else if (command instanceof MotorCommand.DropItem drop) {
			row.addProperty("itemId", drop.itemId());
			row.addProperty("count", drop.count());
		} else if (command instanceof MotorCommand.CollectDrops collect) {
			row.addProperty("radius", collect.radius());
		} else if (command instanceof MotorCommand.Eat eat) {
			row.addProperty("itemId", eat.itemId());
		} else if (command instanceof MotorCommand.MoveAwayFrom away) {
			row.add("from", vec(away.from()));
			row.addProperty("distance", away.distance());
		} else if (command instanceof MotorCommand.Wait wait) {
			row.addProperty("ticks", wait.ticks());
		}
		append(commands, row);
		last = writeFailed
				? MotorResult.failed("client_unavailable", "could not write a motor frame for " + commands)
				: MotorResult.running(command.describe());
		if (writeFailed) {
			active = null;
			activeId = null;
		}
	}

	@Override
	public MotorResult poll() {
		if (active == null) {
			return last;
		}
		JsonObject row = findResult();
		if (row != null) {
			last = parse(row);
			active = null;
			activeId = null;
			return last;
		}
		elapsed++;
		// The client owns the motor timeout; this is only the outer bound that keeps a dead client
		// from holding an intention forever. The skill's own timeout remains authoritative.
		if (elapsed > active.timeoutTicks() + 100) {
			cancelFrame();
			last = MotorResult.failed("client_timeout",
					"no observed result after " + elapsed + " ticks for " + active.describe());
			active = null;
			activeId = null;
			return last;
		}
		return MotorResult.running(active.describe());
	}

	@Override
	public void cancel() {
		if (active == null) {
			return;
		}
		cancelFrame();
		last = MotorResult.cancelled("cancelled by controller");
		active = null;
		activeId = null;
	}

	@Override
	public boolean busy() {
		return active != null;
	}

	private void cancelFrame() {
		if (activeId == null) {
			return;
		}
		JsonObject row = new JsonObject();
		row.addProperty("id", activeId);
		row.addProperty("kind", "Cancel");
		append(commands, row);
	}

	private JsonObject findResult() {
		if (!Files.isRegularFile(results)) {
			return null;
		}
		try {
			for (String line : Files.readAllLines(results, StandardCharsets.UTF_8)) {
				if (line.isBlank()) {
					continue;
				}
				JsonObject row = JsonParser.parseString(line).getAsJsonObject();
				if (row.has("id") && activeId.equals(row.get("id").getAsString())) {
					return row;
				}
			}
		} catch (IOException | RuntimeException unreadable) {
			// A partially written result is retried on the next poll; the file is append-only.
		}
		return null;
	}

	private MotorResult parse(JsonObject row) {
		String status = row.has("status") ? row.get("status").getAsString() : "FAILED";
		String code = row.has("code") ? row.get("code").getAsString() : "client_result";
		String detail = row.has("detail") ? row.get("detail").getAsString() : "";
		Map<String, String> facts = new LinkedHashMap<>();
		if (row.has("facts") && row.get("facts").isJsonObject()) {
			for (Map.Entry<String, JsonElement> entry : row.getAsJsonObject("facts").entrySet()) {
				if (entry.getValue().isJsonPrimitive()) {
					facts.put(entry.getKey(), entry.getValue().getAsString());
				}
			}
		}
		try {
			return new MotorResult(MotorStatus.valueOf(status), code, detail, Map.copyOf(facts));
		} catch (IllegalArgumentException unknown) {
			return MotorResult.failed("client_result_malformed", "unknown status " + status);
		}
	}

	private void append(Path file, JsonObject row) {
		try {
			Files.createDirectories(file.getParent());
			Files.writeString(file, GSON.toJson(row) + "\n", StandardCharsets.UTF_8,
					StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (IOException failed) {
			writeFailed = true;
		}
	}

	private static JsonObject vec(Vec3i point) {
		JsonObject row = new JsonObject();
		row.addProperty("x", point.x());
		row.addProperty("y", point.y());
		row.addProperty("z", point.z());
		return row;
	}

	private static String kindOf(MotorCommand command) {
		if (command instanceof MotorCommand.MoveTo) {
			return "MoveTo";
		}
		if (command instanceof MotorCommand.LookAt) {
			return "LookAt";
		}
		if (command instanceof MotorCommand.MineBlock) {
			return "MineBlock";
		}
		if (command instanceof MotorCommand.Jump) {
			return "Jump";
		}
		if (command instanceof MotorCommand.Use) {
			return "Use";
		}
		if (command instanceof MotorCommand.Equip) {
			return "Equip";
		}
		if (command instanceof MotorCommand.DropItem) {
			return "DropItem";
		}
		if (command instanceof MotorCommand.CollectDrops) {
			return "CollectDrops";
		}
		if (command instanceof MotorCommand.Eat) {
			return "Eat";
		}
		if (command instanceof MotorCommand.MoveAwayFrom) {
			return "MoveAwayFrom";
		}
		if (command instanceof MotorCommand.Wait) {
			return "Wait";
		}
		return "Unknown";
	}
}

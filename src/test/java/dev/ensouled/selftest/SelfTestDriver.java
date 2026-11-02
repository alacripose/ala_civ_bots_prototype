package dev.ensouled.selftest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.ensouled.EnsouledMod;
import dev.ensouled.core.AgentMessage;
import dev.ensouled.core.AgentMind;
import dev.ensouled.core.BlockIds;
import dev.ensouled.core.CanonicalEvent;
import dev.ensouled.core.EpisodeKind;
import dev.ensouled.core.EvidenceKind;
import dev.ensouled.core.EvidenceRecord;
import dev.ensouled.core.Motor;
import dev.ensouled.core.MotorCommand;
import dev.ensouled.core.MotorResult;
import dev.ensouled.core.MotorStatus;
import dev.ensouled.core.Vec3i;
import dev.ensouled.entity.AgentBodyEntity;
import dev.ensouled.entity.FakePlayerBody;
import dev.ensouled.harness.HarnessControl;
import dev.ensouled.interact.ChestInteraction;
import dev.ensouled.interact.InteractionOutcome;
import dev.ensouled.interact.InteractionRequest;
import dev.ensouled.interact.InteractionSession;
import dev.ensouled.interact.InteractionTarget;
import dev.ensouled.interact.PlayerInteractionHands;
import dev.ensouled.mind.AgentManager;
import dev.ensouled.mind.AgentRecord;
import dev.ensouled.skills.RequestSuppliesRoutine;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.Heightmap;

/**
 * Automated acceptance harness.
 *
 * <p>phase1 / player1: build a tree fixture, spawn one being (mob body or fake-player body), and
 * require a verified canonical collection event, a real inventory delta and a memory episode.
 * <p>phase2: restart the process and require the same identity and history, restored inventory, a
 * living body, and no second collection of the remembered tree.
 * <p>verbs: drive jump, equip and use directly through the body's motor on a booted server and
 * require observed effects — Y rises, the held item changes, a stone block breaks with the tool
 * taking durability, and a selected block is placed with the stack shrinking.
 * <p>handoff: two citizens settle one food transfer through a real request/accept exchange.
 * <p>chest: two citizens settle one storage-backed resource transfer — requirement stated, chest
 * contents read through the gated inspect, item withdrawn from the real chest, dropped, taken and
 * stowed in the recipient's own chest — with both container deltas and both inventories verified
 * under one transfer id, plus typed refusals for an unreachable object and an unsupported operation.
 */
public final class SelfTestDriver {
	public static final String TRIGGER_FILE = "ensouled-selftest.txt";
	public static final String MARKER_FILE = "ensouled-selftest-phase1.txt";
	public static final String RESULT_FILE = "ensouled-selftest-result.txt";
	/** Fixture ownership ledger: what the last attached run created, so it can clean up only that. */
	public static final String OWNERSHIP_FILE = "ensouled-harness-owners.txt";
	/** Attached fixture plot, kept away from spawn where a developer builds. */
	public static final int PLOT_OFFSET = 128;

	private static SelfTestDriver active;

	private final MinecraftServer server;
	private final String phase;
	private final String runId;
	private final boolean attached;
	private final boolean shutdownWhenDone;
	private final List<String> spawnedAgents = new ArrayList<>();
	private Vec3i plotBase;
	private int ticks;
	private int stage;
	private Vec3i site;
	private Vec3i agentSpawn;
	private String agentId = "";
	private String bodyKind = "mob";
	private String profileId = "";
	private int ticksSinceReplay;
	private long mineCommandsAtReplay;
	private int inventoryAtReplay;

	// Verb probe state (phase "verbs").
	private FakePlayerBody probePlayer;
	private AgentMind probeMind;
	private Motor probeMotor;
	private boolean probeAwaiting;
	private boolean probeIssued;
	private int probeStageTick;
	private double jumpStartY;
	private double jumpPeak;
	private double jumpRisen;
	private int toolDamageBefore;
	private int toolDamageAfter;
	private int dirtBefore;
	private Vec3i verbStone;
	private Vec3i verbFloor;

	// Handoff probe state (phase "handoff"): two citizens, one request/accept, one real item.
	private String requesterId = "";
	private String supplierId = "";
	private Vec3i supplierSpawn;
	private int requesterFoodBefore;
	/** Bus length when this run began: an attached run shares the bus with earlier phases. */
	private int busLogAtStart;

	// Chest supply state (phase "chest"): the same two citizens, one storage-backed transfer.
	private Vec3i chestSourcePos;
	private Vec3i chestSinkPos;
	private Vec3i chestFarPos;
	private int chestSourceBefore;
	private boolean chestRefusalsChecked;

	private SelfTestDriver(MinecraftServer server, String phase, String runId, boolean attached,
			boolean shutdownWhenDone) {
		this.server = server;
		this.phase = phase;
		this.runId = runId;
		this.attached = attached;
		this.shutdownWhenDone = shutdownWhenDone;
	}

	/** The companion-mod entrypoint for {@code /ensouled harness}: a phase inside the live server. */
	public static HarnessControl.Starter control() {
		return new HarnessControl.Starter() {
			@Override
			public String start(MinecraftServer server, String phase, boolean attached) {
				return startPhase(server, phase, attached);
			}

			@Override
			public boolean cancel(MinecraftServer server) {
				if (active == null || active.server != server) {
					return false;
				}
				active.finish(false, "cancelled by operator");
				return true;
			}

			@Override
			public String status() {
				return active == null ? null
						: "phase=" + active.phase + " run=" + active.runId + " t=" + active.ticks;
			}
		};
	}

	/** CI boot path: triggered by a file that the disposable CI script owns; stops on completion. */
	public static void start(MinecraftServer server, String phase) {
		startPhase(server, phase, false);
	}

	private static String startPhase(MinecraftServer server, String phase, boolean attached) {
		if (active != null) {
			EnsouledMod.LOG.warn("[ensouled] selftest already running");
			return null;
		}
		String runId = (attached ? "dev-" : "ci-") + Long.toString(System.currentTimeMillis(), 36);
		SelfTestDriver driver = new SelfTestDriver(server, phase, runId, attached, !attached);
		if (attached) {
			driver.prepareAttachedFixture();
		}
		active = driver;
		EnsouledMod.LOG.info("[ensouled] SELFTEST phase={} run={} mode={} starting", phase, runId,
				attached ? "attach" : "disposable-ci");
		return runId;
	}

	/**
	 * Attached runs live in the developer's own world: force-load one plot away from spawn, remove the
	 * bodies and loose items the previous attached run created (recorded by id), and never touch a
	 * chest, block or mind this harness did not create.
	 */
	private void prepareAttachedFixture() {
		ServerWorld world = server.getOverworld();
		BlockPos spawn = world.getSpawnPos();
		plotBase = new Vec3i(spawn.getX() + PLOT_OFFSET, spawn.getY(), spawn.getZ() + PLOT_OFFSET);
		for (int cx = (plotBase.x() - 24) >> 4; cx <= (plotBase.x() + 24) >> 4; cx++) {
			for (int cz = (plotBase.z() - 24) >> 4; cz <= (plotBase.z() + 24) >> 4; cz++) {
				world.setChunkForced(cx, cz, true);
			}
		}
		clearOwnedFixture(world);
		Box plot = Box.from(Vec3d.of(new BlockPos(plotBase.x(), plotBase.y(), plotBase.z()))).expand(48.0, 32.0, 48.0);
		for (ItemEntity item : world.getEntitiesByClass(ItemEntity.class, plot, e -> true)) {
			item.discard();
		}
		writeOwnership("plot=" + plotBase.x() + "," + plotBase.y() + "," + plotBase.z());
		EnsouledMod.LOG.info("[ensouled] SELFTEST attach plot={} (spawn offset {}) force-loaded",
				plotBase, PLOT_OFFSET);
	}

	private void clearOwnedFixture(ServerWorld world) {
		Path ledger = server.getRunDirectory().toPath().resolve(OWNERSHIP_FILE);
		if (!Files.exists(ledger)) {
			return;
		}
		try {
			for (String line : Files.readAllLines(ledger, StandardCharsets.UTF_8)) {
				if (line.startsWith("agent=")) {
					String agentId = line.substring("agent=".length()).trim();
					if (AgentManager.get() != null && AgentManager.get().remove(agentId)) {
						EnsouledMod.LOG.info("[ensouled] SELFTEST removed previous fixture body {}", agentId);
					}
				}
			}
			Files.delete(ledger);
		} catch (IOException e) {
			EnsouledMod.LOG.error("[ensouled] could not read harness ownership ledger", e);
		}
	}

	private void writeOwnership(String line) {
		Path ledger = server.getRunDirectory().toPath().resolve(OWNERSHIP_FILE);
		try {
			Files.writeString(ledger, "run=" + runId + "\n" + line + "\n", StandardCharsets.UTF_8,
					Files.exists(ledger) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE,
					StandardOpenOption.WRITE);
		} catch (IOException e) {
			EnsouledMod.LOG.error("[ensouled] could not write harness ownership ledger", e);
		}
	}

	private void claimAgent(String agentId) {
		spawnedAgents.add(agentId);
		if (attached) {
			writeOwnership("agent=" + agentId);
		}
	}

	public static void onServerStarted(MinecraftServer server) {
		Path trigger = server.getRunDirectory().toPath().resolve(TRIGGER_FILE);
		if (!Files.exists(trigger)) {
			return;
		}
		// Only the disposable CI script sets this. A leftover trigger file must never make an
		// interactive dev server run a phase and then stop itself.
		if (!"1".equals(System.getenv("ENSOULED_SELFTEST"))) {
			try {
				Files.delete(trigger);
			} catch (IOException ignored) {
			}
			EnsouledMod.LOG.warn("[ensouled] ignored selftest trigger {}: this server was not started"
					+ " by the disposable CI script (ENSOULED_SELFTEST=1)", TRIGGER_FILE);
			return;
		}
		try {
			String phase = Files.readString(trigger, StandardCharsets.UTF_8).trim();
			Files.delete(trigger);
			if (!phase.isEmpty()) {
				start(server, phase);
			}
		} catch (IOException e) {
			EnsouledMod.LOG.error("[ensouled] could not read selftest trigger", e);
		}
	}

	public static void onServerTick(MinecraftServer server) {
		if (active == null) {
			return;
		}
		if (active.server != server) {
			active = null;
			return;
		}
		active.tick();
	}

	private void tick() {
		ticks++;
		if (stage == 99) {
			return;
		}
		switch (phase) {
			case "phase1" -> tickHarvest("mob");
			case "player1" -> tickHarvest("player");
			case "phase2" -> tickRestart();
			case "verbs" -> tickVerbs();
			case "handoff" -> tickHandoff();
			case "chest" -> tickChest();
			default -> finish(false, "unknown phase '" + phase + "'");
		}
	}

	// ------------------------------------------------------------------ harvest boot

	private void tickHarvest(String kind) {
		if (stage == 0) {
			bodyKind = kind;
			setupFixture();
			AgentManager manager = AgentManager.get();
			AgentRecord record = kind.equals("player")
					? manager.spawnPlayer(server.getOverworld(), agentSpawn, "Ash")
					: manager.spawnMob(server.getOverworld(), agentSpawn, "Ash");
			agentId = record.agentId();
			claimAgent(agentId);
			if (record.profileId() != null) {
				profileId = record.profileId().toString();
			}
			writeMarker();
			EnsouledMod.LOG.info("[ensouled] SELFTEST fixture kind={} site={} spawn={} agent={}",
					bodyKind, site, agentSpawn, agentId);
			stage = 1;
			return;
		}
		Optional<AgentRecord> record = AgentManager.get() == null ? Optional.empty()
				: AgentManager.get().byAgentId(agentId);
		if (record.isEmpty()) {
			if (ticks > 200) {
				finish(false, "spawned body was never bound to a mind");
			}
			return;
		}
		AgentMind mind = record.get().mind();
		if (ticks % 200 == 0) {
			logProgress(record.get());
		}
		long events = siteEventCount(mind);
		int logs = record.get().countItem("minecraft:oak_log");
		boolean remembered = mind.ledger().memory().recent(64).stream()
				.anyMatch(e -> e.kind() == EpisodeKind.RESOURCE_COLLECTED && e.where().equals(site));
		if (events >= 1) {
			finish(events == 1 && logs >= 1 && remembered,
					"harvest events=" + events + " inventory_oak_log=" + logs + " remembered=" + remembered
							+ " body=" + bodyKind + " evidence=" + mind.ledger().evidence().size());
			return;
		}
		if (ticks > 3000) {
			logProgress(record.get());
			finish(false, "timeout: no canonical collection event within 3000 ticks; body=" + bodyKind
					+ " current=" + mind.currentIntention().map(Object::toString).orElse("idle"));
		}
	}

	private void setupFixture() {
		ServerWorld world = server.getOverworld();
		BlockPos spawn = fixtureBase();
		int groundY = world.getTopY(Heightmap.Type.MOTION_BLOCKING, spawn.getX() + 2, spawn.getZ());
		// Harness fixture only: a flat work area and one tree. These are not agent actions.
		for (int dx = -3; dx <= 9; dx++) {
			for (int dz = -3; dz <= 3; dz++) {
				BlockPos floor = new BlockPos(spawn.getX() + dx, groundY - 1, spawn.getZ() + dz);
				if (world.getBlockState(floor).isAir()) {
					world.setBlockState(floor, Blocks.DIRT.getDefaultState(), 3);
				}
				for (int dy = 0; dy < 4; dy++) {
					BlockPos clear = floor.up(1 + dy);
					if (!world.getBlockState(clear).isAir()) {
						world.setBlockState(clear, Blocks.AIR.getDefaultState(), 3);
					}
				}
			}
		}
		site = new Vec3i(spawn.getX() + 6, groundY, spawn.getZ());
		for (int i = 0; i < 3; i++) {
			world.setBlockState(new BlockPos(site.x(), site.y() + i, site.z()), Blocks.OAK_LOG.getDefaultState(), 3);
		}
		agentSpawn = new Vec3i(spawn.getX() + 1, groundY, spawn.getZ());
	}

	// ------------------------------------------------------------------ restart boot

	private void tickRestart() {
		if (stage == 0) {
			if (!readMarker()) {
				finish(false, "no phase1 marker; run phase1 or player1 first");
				return;
			}
			stage = 1;
			return;
		}
		AgentManager manager = AgentManager.get();
		if (manager == null) {
			return;
		}
		Optional<AgentRecord> record = manager.byAgentId(agentId);
		if (record.isEmpty()) {
			if (ticks > 600) {
				finish(false, "persisted mind " + agentId + " was not restored after restart");
			}
			return;
		}
		AgentMind mind = record.get().mind();
		if (stage == 1) {
			boolean identity = mind.identity().agentId().equals(agentId);
			boolean remembered = mind.ledger().memory().recent(64).stream()
					.anyMatch(e -> e.kind() == EpisodeKind.RESOURCE_COLLECTED && e.where().equals(site));
			boolean canonical = mind.ledger().events().exists(CanonicalEvent.RESOURCE_COLLECTED,
					BlockIds.siteKey("minecraft:overworld", site));
			inventoryAtReplay = record.get().countItem("minecraft:oak_log");
			EnsouledMod.LOG.info("[ensouled] SELFTEST continuity kind={} identity={} remembered={} canonical={} inventory={}",
					bodyKind, identity, remembered, canonical, inventoryAtReplay);
			if (!identity || !remembered || !canonical) {
				finish(false, "continuity lost: identity=" + identity + " remembered=" + remembered + " canonical=" + canonical);
				return;
			}
			relocateOutOfColumn(record.get());
			ServerWorld world = server.getOverworld();
			for (int i = 0; i < 3; i++) {
				world.setBlockState(new BlockPos(site.x(), site.y() + i, site.z()), Blocks.OAK_LOG.getDefaultState(), 3);
			}
			mineCommandsAtReplay = mineCommandsIssued(mind);
			stage = 2;
			return;
		}
		ticksSinceReplay++;
		long events = siteEventCount(mind);
		if (events > 1) {
			finish(false, "duplicate collection event for site " + site + ": count=" + events);
			return;
		}
		if (ticksSinceReplay > 600) {
			long newMineCommands = mineCommandsIssued(mind) - mineCommandsAtReplay;
			int inventoryNow = record.get().countItem("minecraft:oak_log");
			boolean alive = record.get().body() != null && record.get().body().alive();
			boolean noReHarvest = alive && newMineCommands == 0 && inventoryNow == inventoryAtReplay && events == 1;
			finish(noReHarvest, "restart continuity kind=" + bodyKind + " site=" + site + " events=" + events
					+ " alive=" + alive + " new_mine_commands=" + newMineCommands
					+ " inventory=" + inventoryAtReplay + "->" + inventoryNow
					+ " intention=" + mind.currentIntention().map(Object::toString).orElse("idle"));
		}
	}

	private void relocateOutOfColumn(AgentRecord record) {
		Entity entity = record.body() == null ? null : record.body().entity();
		if (entity == null) {
			return;
		}
		Box column = new Box(site.x(), site.y(), site.z(), site.x() + 1.0, site.y() + 3.0, site.z() + 1.0).expand(0.2);
		if (!entity.getBoundingBox().intersects(column)) {
			return;
		}
		entity.refreshPositionAndAngles(agentSpawn.x() + 0.5, agentSpawn.y(), agentSpawn.z() + 0.5, 0.0f, 0.0f);
		entity.setVelocity(0.0, 0.0, 0.0);
		entity.fallDistance = 0.0f;
		if (entity instanceof FakePlayerBody player) {
			player.forwardSpeed = 0.0f;
			player.sidewaysSpeed = 0.0f;
			player.setJumping(false);
		}
		if (entity instanceof AgentBodyEntity mob) {
			mob.getNavigation().stop();
		}
		EnsouledMod.LOG.info("[ensouled] SELFTEST relocated body out of fixture column");
	}

	// ------------------------------------------------------------------ verb probe

	private void tickVerbs() {
		if (stage == 0) {
			setupVerbFixture();
			AgentRecord record = AgentManager.get().spawnPlayer(server.getOverworld(), agentSpawn, "Ash");
			agentId = record.agentId();
			bodyKind = "player";
			probeMind = record.mind();
			probeMotor = record.motorProxy();
			if (record.body() != null && record.body().entity() instanceof FakePlayerBody body) {
				probePlayer = body;
				// Harness fixture, like the tree: the block and the tool are handed over so the probe can
				// exercise equip/use without first building a gathering loop. Not an agent action.
				body.getInventory().setStack(0, new ItemStack(Items.DIRT, 8));
				body.getInventory().setStack(1, new ItemStack(Items.IRON_PICKAXE));
			}
			EnsouledMod.LOG.info("[ensouled] SELFTEST verb fixture stone={} floor={} spawn={} agent={}",
					verbStone, verbFloor, agentSpawn, agentId);
			probeStageTick = ticks;
			stage = 1;
			return;
		}
		if (stage == 99) {
			return;
		}
		if (probePlayer == null || !probePlayer.isAlive()) {
			finish(false, "verb probe lost its body");
			return;
		}
		switch (stage) {
			case 1 -> probeJump();
			case 2 -> probeEquipTool();
			case 3 -> probeMineWithTool();
			case 4 -> probeEquipBlock();
			case 5 -> probePlace();
			default -> finish(false, "verb probe left in stage " + stage);
		}
	}

	private void probeJump() {
		if (!probeIssued) {
			if (ticks - probeStageTick < 20) {
				return; // let the body settle onto the floor before the first input
			}
			jumpStartY = probePlayer.getY();
			jumpPeak = jumpStartY;
			issueProbe(new MotorCommand.Jump(40));
			return;
		}
		jumpPeak = Math.max(jumpPeak, probePlayer.getY());
		MotorResult result = pollProbe();
		if (result == null) {
			return;
		}
		jumpRisen = jumpPeak - jumpStartY;
		boolean ok = result.status() == MotorStatus.SUCCESS && jumpRisen > 0.2;
		logProbe("jump", ok, result, "y=" + fmt(jumpStartY) + "->" + fmt(jumpPeak));
		if (!ok) {
			finish(false, "jump did not raise the body: y=" + fmt(jumpStartY) + "->" + fmt(jumpPeak)
					+ " result=" + result.code() + ": " + result.detail());
			return;
		}
		ItemStack tool = probePlayer.getInventory().getStack(1);
		if (tool.isEmpty()) {
			finish(false, "fixture lost the pickaxe before the tool probe");
			return;
		}
		toolDamageBefore = tool.getDamage();
		advanceProbe(2, new MotorCommand.Equip("minecraft:iron_pickaxe", 40));
	}

	private void probeEquipTool() {
		if (!probeIssued) {
			issueProbe(new MotorCommand.Equip("minecraft:iron_pickaxe", 40));
			return;
		}
		MotorResult result = pollProbe();
		if (result == null) {
			return;
		}
		String held = heldId();
		boolean ok = result.status() == MotorStatus.SUCCESS && held.equals("minecraft:iron_pickaxe");
		logProbe("equip", ok, result, "held=" + held + " slot=" + probePlayer.getInventory().selectedSlot);
		if (!ok) {
			finish(false, "equip did not change the held item: held=" + held
					+ " result=" + result.code() + ": " + result.detail());
			return;
		}
		advanceProbe(3, new MotorCommand.MineBlock(verbStone, "minecraft:stone", 240));
	}

	private void probeMineWithTool() {
		if (!probeIssued) {
			issueProbe(new MotorCommand.MineBlock(verbStone, "minecraft:stone", 240));
			return;
		}
		MotorResult result = pollProbe();
		if (result == null) {
			return;
		}
		toolDamageAfter = probePlayer.getInventory().getStack(1).getDamage();
		String block = blockId(verbStone);
		boolean removed = block.equals("minecraft:air");
		boolean wore = toolDamageAfter > toolDamageBefore;
		boolean ok = result.status() == MotorStatus.SUCCESS && removed && wore;
		logProbe("mine_with_tool", ok, result, "block=" + block + " pickaxe_damage="
				+ toolDamageBefore + "->" + toolDamageAfter);
		if (!ok) {
			finish(false, "mining with a tool failed: block_at_stone=" + block + " pickaxe_damage="
					+ toolDamageBefore + "->" + toolDamageAfter + " result=" + result.code());
			return;
		}
		advanceProbe(4, new MotorCommand.Equip("minecraft:dirt", 40));
	}

	private void probeEquipBlock() {
		if (!probeIssued) {
			issueProbe(new MotorCommand.Equip("minecraft:dirt", 40));
			return;
		}
		MotorResult result = pollProbe();
		if (result == null) {
			return;
		}
		String held = heldId();
		boolean ok = result.status() == MotorStatus.SUCCESS && held.equals("minecraft:dirt");
		logProbe("equip_block", ok, result, "held=" + held + " slot=" + probePlayer.getInventory().selectedSlot);
		if (!ok) {
			finish(false, "equip did not select the block: held=" + held);
			return;
		}
		dirtBefore = countItem("minecraft:dirt");
		advanceProbe(5, new MotorCommand.Use(verbFloor, "minecraft:dirt", 40));
	}

	private void probePlace() {
		if (!probeIssued) {
			issueProbe(new MotorCommand.Use(verbFloor, "minecraft:dirt", 40));
			return;
		}
		MotorResult result = pollProbe();
		if (result == null) {
			return;
		}
		String block = blockId(verbStone);
		int dirtAfter = countItem("minecraft:dirt");
		boolean placed = block.equals("minecraft:dirt");
		boolean spent = dirtAfter == dirtBefore - 1;
		int motorEvidence = probeMind.ledger().evidence().byKind(EvidenceKind.MOTOR).size();
		boolean recorded = motorEvidence >= 8;
		boolean ok = result.status() == MotorStatus.SUCCESS && placed && spent && recorded;
		logProbe("use", ok, result, "block=" + block + " dirt=" + dirtBefore + "->" + dirtAfter
				+ " motor_evidence=" + motorEvidence);
		finish(ok, "verbs body=player jump=+" + fmt(jumpRisen) + " equip=iron_pickaxe"
				+ " mine=stone_removed tool_durability=" + toolDamageBefore + "->" + toolDamageAfter
				+ " use=placed_at_" + verbStone + " dirt=" + dirtBefore + "->" + dirtAfter
				+ " motor_evidence=" + motorEvidence
				+ (ok ? "" : " result=" + result.code() + ": " + result.detail()
						+ " block_at_use=" + block));
	}

	// ------------------------------------------------------------------ two-citizen handoff

	/**
	 * Two fake players, one shared task: a hungry citizen with no food asks a peer it can see, the
	 * peer decides from its own inventory, answers, walks over and drops the real stack; the asker
	 * takes it and eats it. The transfer must be recorded exactly once, by the recipient.
	 */
	private void tickHandoff() {
		if (stage == 0) {
			setupHandoffFixture();
			AgentManager manager = AgentManager.get();
			// An attached run shares one bus with every earlier phase in this world; only the speech
			// this run causes may be counted as this run's exchange.
			busLogAtStart = manager.bus().log().size();
			AgentRecord requester = manager.spawnPlayer(server.getOverworld(), agentSpawn, "Ash");
			AgentRecord supplier = manager.spawnPlayer(server.getOverworld(), supplierSpawn, "Bram");
			requesterId = requester.agentId();
			supplierId = supplier.agentId();
			claimAgent(requesterId);
			claimAgent(supplierId);
			// Harness fixture, like the tree and the pickaxe: hunger and one loaf are handed over so the
			// exchange can be exercised without first simulating a whole day of work.
			if (requester.body() != null && requester.body().entity() instanceof FakePlayerBody body) {
				body.getHungerManager().setFoodLevel(6);
			}
			if (supplier.body() != null && supplier.body().entity() instanceof FakePlayerBody body) {
				body.getInventory().setStack(0, new ItemStack(Items.BREAD, 1));
			}
			requesterFoodBefore = requester.body() != null
					&& requester.body().entity() instanceof FakePlayerBody body
							? body.getHungerManager().getFoodLevel()
							: requester.mind().needs().food();
			EnsouledMod.LOG.info("[ensouled] SELFTEST handoff requester={} food={} supplier={} bread=1 spawns={}/{}",
					requesterId, requesterFoodBefore, supplierId, agentSpawn, supplierSpawn);
			stage = 1;
			return;
		}
		AgentManager manager = AgentManager.get();
		if (manager == null) {
			return;
		}
		Optional<AgentRecord> requester = manager.byAgentId(requesterId);
		Optional<AgentRecord> supplier = manager.byAgentId(supplierId);
		if (requester.isEmpty() || supplier.isEmpty()) {
			if (ticks > 300) {
				finish(false, "handoff bodies were never both bound to minds");
			}
			return;
		}
		AgentRecord asker = requester.get();
		AgentRecord giver = supplier.get();
		long transfers = asker.mind().ledger().events().all().stream()
				.filter(e -> e.type().equals(CanonicalEvent.ITEM_TRANSFERRED)).count();
		long supplierTransfers = giver.mind().ledger().events().all().stream()
				.filter(e -> e.type().equals(CanonicalEvent.ITEM_TRANSFERRED)).count();
		boolean asked = socialOutcome(asker.mind(), "requested");
		boolean answered = socialOutcome(giver.mind(), "accepted");
		boolean heard = socialOutcome(asker.mind(), "accepted");
		boolean declined = socialOutcome(asker.mind(), "declined") || socialOutcome(giver.mind(), "declined");
		int food = asker.mind().needs().food();
		int giverBread = giver.countItem("minecraft:bread");
		int askerBread = asker.countItem("minecraft:bread");
		long requests = manager.bus().log().stream().skip(busLogAtStart)
				.filter(m -> m.speechAct().equals(AgentMessage.REQUEST)).count();
		long accepts = manager.bus().log().stream().skip(busLogAtStart)
				.filter(m -> m.speechAct().equals(AgentMessage.ACCEPT)).count();

		if (ticks % 200 == 0) {
			EnsouledMod.LOG.info("[ensouled] SELFTEST handoff t={} requester food={} body={} bread={} cur={} | supplier bread={} cur={} | msgs={}",
					ticks, food, bodyFood(asker), askerBread, asker.mind().currentIntention().map(Object::toString).orElse("idle"),
					giverBread, giver.mind().currentIntention().map(Object::toString).orElse("idle"),
					manager.bus().log().size());
		}

		// Eating is proven by the mind's own verified eat skill and the empty stack, not by the food
		// number alone: a peaceful server nudges hunger on its own, so the number is context only.
		int eats = asker.mind().ledger().stats("eat_if_hungry").successes();
		boolean complete = transfers == 1 && supplierTransfers == 0 && asked && answered && heard
				&& !declined && eats == 1 && giverBread == 0 && askerBread == 0
				&& requests == 1 && accepts == 1;
		if (complete) {
			finish(true, "handoff citizens=2 request=msg:1 accept=msg:2 transfers=1 eats=" + eats
					+ " food=" + requesterFoodBefore + "->" + food
					+ " supplier_bread=1->" + giverBread + " asker_bread=" + askerBread);
			return;
		}
		if (ticks > 1800) {
			finish(false, "handoff did not complete: transfers=" + transfers
					+ " supplier_transfers=" + supplierTransfers + " asked=" + asked
					+ " answered=" + answered + " heard=" + heard + " declined=" + declined
					+ " requests=" + requests + " accepts=" + accepts
					+ " food=" + requesterFoodBefore + "->" + food
					+ " supplier_bread=" + giverBread + " asker_bread=" + askerBread
					+ " requester_current=" + asker.mind().currentIntention().map(Object::toString).orElse("idle")
					+ " supplier_current=" + giver.mind().currentIntention().map(Object::toString).orElse("idle"));
		}
	}

	private static int bodyFood(AgentRecord record) {
		return record.body() != null && record.body().entity() instanceof FakePlayerBody body
				? body.getHungerManager().getFoodLevel()
				: -1;
	}

	private static boolean socialOutcome(AgentMind mind, String outcome) {
		return mind.ledger().evidence().all().stream()
				.anyMatch(e -> e.kind() == EvidenceKind.SOCIAL && e.outcome().equals(outcome));
	}

	private void setupHandoffFixture() {
		ServerWorld world = server.getOverworld();
		BlockPos spawn = fixtureBase();
		int groundY = world.getTopY(Heightmap.Type.MOTION_BLOCKING, spawn.getX() + 2, spawn.getZ());
		for (int dx = -4; dx <= 8; dx++) {
			for (int dz = -4; dz <= 4; dz++) {
				BlockPos floor = new BlockPos(spawn.getX() + dx, groundY - 1, spawn.getZ() + dz);
				world.setBlockState(floor, Blocks.DIRT.getDefaultState(), 3);
				for (int dy = 0; dy < 4; dy++) {
					world.setBlockState(floor.up(1 + dy), Blocks.AIR.getDefaultState(), 3);
				}
			}
		}
		agentSpawn = new Vec3i(spawn.getX() + 1, groundY, spawn.getZ());
		supplierSpawn = new Vec3i(spawn.getX() + 4, groundY, spawn.getZ());
		// No logs and no loose food: the only reason to ask is the hunger need, and the only bread in
		// the world is the one the supplier was given.
	}

	// ------------------------------------------------------------------ storage-backed transfer

	/**
	 * Two citizens and three real chests. Ash keeps stock in the chest beside it and finds it empty,
	 * so it states the requirement to Bram. Bram answers from its own chest: it reads the contents
	 * through the gated inspect, withdraws two logs through the real container path, walks over and
	 * drops them. Ash takes them and stows them in its own chest. Nothing here is asserted from a
	 * message: both container deltas, both inventories and one canonical event per side are read back
	 * from the world under the transfer id the two minds agreed on. A chest beyond reach and an
	 * unsupported operation must both come back as typed refusals that change nothing.
	 */
	private void tickChest() {
		if (stage == 0) {
			setupChestFixture();
			AgentManager manager = AgentManager.get();
			AgentRecord requester = manager.spawnPlayer(server.getOverworld(), agentSpawn, "Ash");
			AgentRecord supplier = manager.spawnPlayer(server.getOverworld(), supplierSpawn, "Bram");
			requesterId = requester.agentId();
			supplierId = supplier.agentId();
			claimAgent(requesterId);
			claimAgent(supplierId);
			chestSourceBefore = chestCount(chestSourcePos, RequestSuppliesRoutine.ITEM);
			EnsouledMod.LOG.info("[ensouled] SELFTEST chest requester={} supplier={} source={}({}) sink={} far={}",
					requesterId, supplierId, chestSourcePos, chestSourceBefore, chestSinkPos, chestFarPos);
			stage = 1;
			return;
		}
		AgentManager manager = AgentManager.get();
		if (manager == null) {
			return;
		}
		Optional<AgentRecord> requester = manager.byAgentId(requesterId);
		Optional<AgentRecord> supplier = manager.byAgentId(supplierId);
		if (requester.isEmpty() || supplier.isEmpty()) {
			if (ticks > 300) {
				finish(false, "chest bodies were never both bound to minds");
			}
			return;
		}
		AgentRecord storer = requester.get();
		AgentRecord drawer = supplier.get();
		if (!chestRefusalsChecked) {
			chestRefusalsChecked = true;
			if (!probeRefusals(storer)) {
				return;
			}
		}

		String item = RequestSuppliesRoutine.ITEM;
		int source = chestCount(chestSourcePos, item);
		int sink = chestCount(chestSinkPos, item);
		int drawerInventory = drawer.countItem(item);
		int storerInventory = storer.countItem(item);
		List<CanonicalEvent> withdrawals = events(drawer.mind(), CanonicalEvent.CONTAINER_WITHDRAWN);
		List<CanonicalEvent> deposits = events(storer.mind(), CanonicalEvent.CONTAINER_DEPOSITED);
		List<CanonicalEvent> transfers = events(storer.mind(), CanonicalEvent.ITEM_TRANSFERRED);
		boolean asked = socialOutcome(storer.mind(), "requested");
		boolean answered = socialOutcome(drawer.mind(), "accepted");
		boolean heard = socialOutcome(storer.mind(), "accepted");
		boolean declined = socialOutcome(storer.mind(), "declined") || socialOutcome(drawer.mind(), "declined");
		String transferId = withdrawals.isEmpty() ? ""
				: withdrawals.get(0).subjectId().replace(":" + InteractionRequest.WITHDRAW, "");
		boolean oneTransfer = !transferId.isEmpty()
				&& deposits.size() == 1
				&& deposits.get(0).subjectId().equals(transferId + ":" + InteractionRequest.DEPOSIT)
				&& transfers.size() == 1
				&& transfers.get(0).subjectId().equals(transferId);
		boolean conserved = source + sink + drawerInventory + storerInventory == chestSourceBefore;
		int loose = looseStock(item);

		if (ticks % 200 == 0) {
			EnsouledMod.LOG.info("[ensouled] SELFTEST chest t={} source={} sink={} drawer_inv={} storer_inv={}"
					+ " withdraws={} deposits={} transfers={} loose={} source_block={} | storer={}@{} drawer={}@{}",
					ticks, source, sink, drawerInventory, storerInventory, withdrawals.size(),
					deposits.size(), transfers.size(), loose, blockId(chestSourcePos),
					storer.mind().currentIntention().map(Object::toString).orElse("idle"), storer.position(),
					drawer.mind().currentIntention().map(Object::toString).orElse("idle"), drawer.position());
		}

		if (ticks % 400 == 0) {
			EnsouledMod.LOG.info("[ensouled] SELFTEST chest trace storer:{}", evidenceTail(storer.mind(), 8));
			EnsouledMod.LOG.info("[ensouled] SELFTEST chest trace drawer:{}", evidenceTail(drawer.mind(), 8));
		}

		boolean complete = source == chestSourceBefore - RequestSuppliesRoutine.QUANTITY
				&& sink == RequestSuppliesRoutine.QUANTITY && drawerInventory == 0
				&& storerInventory == 0 && oneTransfer && asked && answered && heard && !declined
				&& conserved && loose == 0;
		if (complete) {
			finish(true, "chest citizens=2 transfer=" + transferId + " source=" + chestSourceBefore + "->" + source
					+ " sink=0->" + sink + " drawer_inventory=" + drawerInventory
					+ " storer_inventory=" + storerInventory + " withdrawals=1 deposits=1 transfers=1"
					+ " conserved=" + conserved + " refusals=not_reachable,unsupported_operation");
			return;
		}
		if (ticks > 2400) {
			finish(false, "chest transfer did not complete: source=" + chestSourceBefore + "->" + source
					+ " sink=" + sink + " drawer_inventory=" + drawerInventory
					+ " storer_inventory=" + storerInventory + " withdrawals=" + withdrawals.size()
					+ " deposits=" + deposits.size() + " transfers=" + transfers.size()
					+ " asked=" + asked + " answered=" + answered + " heard=" + heard
					+ " declined=" + declined + " conserved=" + conserved + " loose=" + loose
					+ " storer_current=" + storer.mind().currentIntention().map(Object::toString).orElse("idle")
					+ " drawer_current=" + drawer.mind().currentIntention().map(Object::toString).orElse("idle"));
		}
	}

	/**
	 * Two refusal boundaries through the production interaction path: an object outside the body's
	 * reach, and an operation the object does not declare. Both must be typed refusals, and neither
	 * may change the world.
	 */
	private boolean probeRefusals(AgentRecord storer) {
		InteractionSession session = new InteractionSession(storer.body().interactions(), storer.gate(),
				storer.body().createView(), (kind, outcome, detail) -> {
				});
		List<InteractionTarget> perceived = session.targets(PlayerInteractionHands.MAX_SCAN_RADIUS);
		InteractionTarget far = perceived.stream().filter(t -> t.pos().equals(chestFarPos)).findFirst()
				.orElse(null);
		InteractionTarget near = perceived.stream().filter(t -> t.pos().equals(chestSinkPos)).findFirst()
				.orElse(null);
		if (far == null || near == null) {
			finish(false, "chest refusals: the fixture chests were not perceived (far=" + far
					+ " near=" + near + ")");
			return false;
		}
		InteractionOutcome unreachable = session.perform(far,
				InteractionRequest.withdraw(RequestSuppliesRoutine.ITEM, 1, "probe:reach"));
		InteractionOutcome unsupported = session.perform(near,
				new InteractionRequest("smelt", RequestSuppliesRoutine.ITEM, 1, "probe:operation"));
		int sinkAfter = chestCount(chestSinkPos, RequestSuppliesRoutine.ITEM);
		boolean refusedReach = unreachable.status() == InteractionOutcome.Status.REFUSED
				&& "not_reachable".equals(unreachable.code());
		boolean refusedOperation = unsupported.status() == InteractionOutcome.Status.REFUSED
				&& "unsupported_operation".equals(unsupported.code());
		boolean untouched = sinkAfter == 0;
		EnsouledMod.LOG.info("[ensouled] SELFTEST chest refusals reach={}/{} operation={}/{} sink_after={}",
				unreachable.status(), unreachable.code(), unsupported.status(), unsupported.code(), sinkAfter);
		if (!refusedReach || !refusedOperation || !untouched) {
			finish(false, "chest refusals were not enforced: reach=" + unreachable.status() + "/"
					+ unreachable.code() + " operation=" + unsupported.status() + "/" + unsupported.code()
					+ " sink_after_probe=" + sinkAfter);
			return false;
		}
		return true;
	}

	private static BlockPos blockPos(Vec3i pos) {
		return new BlockPos(pos.x(), pos.y(), pos.z());
	}

	private void setupChestFixture() {
		ServerWorld world = server.getOverworld();
		BlockPos spawn = fixtureBase();
		int groundY = world.getTopY(Heightmap.Type.MOTION_BLOCKING, spawn.getX() + 2, spawn.getZ());
		for (int dx = -4; dx <= 8; dx++) {
			for (int dz = -8; dz <= 8; dz++) {
				BlockPos floor = new BlockPos(spawn.getX() + dx, groundY - 1, spawn.getZ() + dz);
				world.setBlockState(floor, Blocks.DIRT.getDefaultState(), 3);
				for (int dy = 0; dy < 4; dy++) {
					world.setBlockState(floor.up(1 + dy), Blocks.AIR.getDefaultState(), 3);
				}
			}
		}
		agentSpawn = new Vec3i(spawn.getX() + 1, groundY, spawn.getZ());
		supplierSpawn = new Vec3i(spawn.getX() + 4, groundY, spawn.getZ());
		// Each citizen owns the chest beside it, on opposite sides so neither can reach the other's.
		chestSinkPos = new Vec3i(agentSpawn.x(), groundY, agentSpawn.z() - 2);
		chestSourcePos = new Vec3i(supplierSpawn.x(), groundY, supplierSpawn.z() + 2);
		chestFarPos = new Vec3i(agentSpawn.x(), groundY, agentSpawn.z() - 6);
		placeChest(chestSourcePos);
		placeChest(chestSinkPos);
		placeChest(chestFarPos);
		// The supplier's chest stays stocked after the transfer, so it never becomes the being with a
		// deficit: exactly one mind has a requirement, and the workflow cannot mirror itself.
		fillChest(chestSourcePos, new ItemStack(Items.OAK_LOG, RequestSuppliesRoutine.QUANTITY * 2));
		// No logs in the world and no hunger on either side: the only reason to act is the empty
		// storage chest and the request it produces.
	}

	private void placeChest(Vec3i pos) {
		server.getOverworld().setBlockState(new BlockPos(pos.x(), pos.y(), pos.z()),
				Blocks.CHEST.getDefaultState(), 3);
	}

	private void fillChest(Vec3i pos, ItemStack stack) {
		BlockEntity entity = server.getOverworld().getBlockEntity(new BlockPos(pos.x(), pos.y(), pos.z()));
		if (entity instanceof Inventory inventory) {
			inventory.setStack(0, stack);
			inventory.markDirty();
		}
	}

	private int chestCount(Vec3i pos, String itemId) {
		BlockEntity entity = server.getOverworld().getBlockEntity(new BlockPos(pos.x(), pos.y(), pos.z()));
		return entity instanceof Inventory inventory ? ChestInteraction.count(inventory, itemId) : -1;
	}

	private int looseStock(String itemId) {
		ServerWorld world = server.getOverworld();
		Box area = new Box(agentSpawn.x() - 16, agentSpawn.y() - 6, agentSpawn.z() - 16,
				agentSpawn.x() + 16, agentSpawn.y() + 6, agentSpawn.z() + 16);
		return world.getEntitiesByClass(ItemEntity.class, area,
				item -> Registries.ITEM.getId(item.getStack().getItem()).toString().equals(itemId)).size();
	}

	private static String evidenceTail(AgentMind mind, int count) {
		StringBuilder sb = new StringBuilder();
		for (EvidenceRecord entry : mind.ledger().evidence().recent(count)) {
			sb.append(" | ").append(entry.tick()).append(' ').append(entry.kind()).append(' ')
					.append(entry.outcome()).append(' ').append(entry.detail());
		}
		return sb.toString();
	}

	private static List<CanonicalEvent> events(AgentMind mind, String type) {
		return mind.ledger().events().all().stream().filter(e -> e.type().equals(type)).toList();
	}

	private void advanceProbe(int nextStage, MotorCommand command) {
		stage = nextStage;
		probeIssued = false;
		probeStageTick = ticks;
		issueProbe(command);
	}

	private void issueProbe(MotorCommand command) {
		probeMotor.start(command);
		probeAwaiting = true;
		probeIssued = true;
		probeMind.ledger().evidence().append(server.getTicks(), agentId, "", EvidenceKind.MOTOR, "", "issued",
				command.describe());
		EnsouledMod.LOG.info("[ensouled] SELFTEST probe issued {}", command.describe());
	}

	private MotorResult pollProbe() {
		if (!probeAwaiting) {
			return null;
		}
		MotorResult result = probeMotor.poll();
		if (!result.terminal()) {
			return null;
		}
		probeAwaiting = false;
		probeMind.ledger().evidence().append(server.getTicks(), agentId, "", EvidenceKind.MOTOR, "",
				result.status().name().toLowerCase(), result.code() + ": " + result.detail() + " " + result.facts());
		return result;
	}

	private void logProbe(String label, boolean ok, MotorResult result, String observed) {
		EnsouledMod.LOG.info("[ensouled] SELFTEST probe {} {} {} result={}:{} {}", label,
				ok ? "PASS" : "FAIL", observed, result.code(), result.detail(), result.facts());
	}

	private String heldId() {
		return Registries.ITEM.getId(probePlayer.getMainHandStack().getItem()).toString();
	}

	private String blockId(Vec3i pos) {
		return Registries.BLOCK.getId(server.getOverworld()
				.getBlockState(new BlockPos(pos.x(), pos.y(), pos.z())).getBlock()).toString();
	}

	private int countItem(String itemId) {
		int total = 0;
		for (int slot = 0; slot < probePlayer.getInventory().size(); slot++) {
			ItemStack stack = probePlayer.getInventory().getStack(slot);
			if (!stack.isEmpty() && Registries.ITEM.getId(stack.getItem()).toString().equals(itemId)) {
				total += stack.getCount();
			}
		}
		return total;
	}

	private static String fmt(double value) {
		return String.format("%.2f", value);
	}

	/**
	 * Where this run builds its fixture. A disposable CI world uses spawn; an attached run uses its
	 * own force-loaded plot so a developer's build at spawn is never cleared or floored.
	 */
	private BlockPos fixtureBase() {
		ServerWorld world = server.getOverworld();
		if (!attached) {
			return world.getSpawnPos();
		}
		if (plotBase == null) {
			prepareAttachedFixture();
		}
		return new BlockPos(plotBase.x(), plotBase.y(), plotBase.z());
	}

	private void setupVerbFixture() {
		ServerWorld world = server.getOverworld();
		BlockPos spawn = fixtureBase();
		int groundY = world.getTopY(Heightmap.Type.MOTION_BLOCKING, spawn.getX() + 2, spawn.getZ());
		for (int dx = -3; dx <= 6; dx++) {
			for (int dz = -3; dz <= 3; dz++) {
				BlockPos floor = new BlockPos(spawn.getX() + dx, groundY - 1, spawn.getZ() + dz);
				world.setBlockState(floor, Blocks.DIRT.getDefaultState(), 3);
				for (int dy = 0; dy < 4; dy++) {
					world.setBlockState(floor.up(1 + dy), Blocks.AIR.getDefaultState(), 3);
				}
			}
		}
		agentSpawn = new Vec3i(spawn.getX() + 1, groundY, spawn.getZ());
		verbStone = new Vec3i(spawn.getX() + 3, groundY, spawn.getZ());
		verbFloor = new Vec3i(verbStone.x(), verbStone.y() - 1, verbStone.z());
		world.setBlockState(new BlockPos(verbStone.x(), verbStone.y(), verbStone.z()),
				Blocks.STONE.getDefaultState(), 3);
		// Deliberately no logs here: the mind has no harvest candidate, so the probe owns the motor alone.
	}

	// ------------------------------------------------------------------ shared

	private void logProgress(AgentRecord record) {
		AgentMind mind = record.mind();
		StringBuilder sb = new StringBuilder("progress ");
		sb.append(mind.describe().replace('\n', ' '));
		if (record.body() == null) {
			sb.append(" body=unbound");
		} else {
			sb.append(" body=").append(record.body().kind()).append('@').append(record.body().position())
					.append(" alive=").append(record.body().alive());
		}
		sb.append(" logs=").append(record.countItem("minecraft:oak_log"));
		for (var entry : mind.ledger().evidence().recent(6)) {
			sb.append(" | t=").append(entry.tick()).append(' ').append(entry.kind())
					.append(' ').append(entry.outcome()).append(' ').append(entry.detail());
		}
		EnsouledMod.LOG.info("[ensouled] SELFTEST {}", sb);
	}

	private long mineCommandsIssued(AgentMind mind) {
		return mind.ledger().evidence().all().stream()
				.filter(e -> e.kind() == EvidenceKind.MOTOR && e.outcome().equals("issued")
						&& e.detail().startsWith("mine_block"))
				.count();
	}

	private long siteEventCount(AgentMind mind) {
		String key = BlockIds.siteKey("minecraft:overworld", site);
		return mind.ledger().events().all().stream()
				.filter(e -> e.type().equals(CanonicalEvent.RESOURCE_COLLECTED) && e.subjectId().equals(key))
				.count();
	}

	private boolean readMarker() {
		Path marker = server.getRunDirectory().toPath().resolve(MARKER_FILE);
		if (!Files.exists(marker)) {
			return false;
		}
		try {
			for (String line : Files.readAllLines(marker, StandardCharsets.UTF_8)) {
				String[] parts = line.split("=", 2);
				if (parts.length != 2) {
					continue;
				}
				switch (parts[0].trim()) {
					case "agent" -> agentId = parts[1].trim();
					case "kind" -> bodyKind = parts[1].trim();
					case "profile" -> profileId = parts[1].trim();
					case "site" -> site = parseVec(parts[1]);
					case "spawn" -> agentSpawn = parseVec(parts[1]);
					default -> {
					}
				}
			}
			return site != null && !agentId.isEmpty();
		} catch (IOException | NumberFormatException e) {
			EnsouledMod.LOG.error("[ensouled] could not read selftest marker", e);
			return false;
		}
	}

	private static Vec3i parseVec(String value) {
		String[] xyz = value.trim().split(",");
		return new Vec3i(Integer.parseInt(xyz[0]), Integer.parseInt(xyz[1]), Integer.parseInt(xyz[2]));
	}

	private void writeMarker() {
		Path marker = server.getRunDirectory().toPath().resolve(MARKER_FILE);
		String body = "agent=" + agentId + "\nkind=" + bodyKind + "\nprofile=" + profileId
				+ "\nsite=" + site.x() + "," + site.y() + "," + site.z()
				+ "\nspawn=" + agentSpawn.x() + "," + agentSpawn.y() + "," + agentSpawn.z() + "\n";
		try {
			Files.writeString(marker, body, StandardCharsets.UTF_8);
		} catch (IOException e) {
			EnsouledMod.LOG.error("[ensouled] could not write selftest marker", e);
		}
	}

	private void finish(boolean pass, String detail) {
		stage = 99;
		String line = (pass ? "SELFTEST PASS" : "SELFTEST FAIL") + " phase=" + phase
				+ " run=" + runId + " " + detail;
		EnsouledMod.LOG.info("[ensouled] {}", line);
		Path result = server.getRunDirectory().toPath().resolve(RESULT_FILE);
		try {
			Files.writeString(result, line + "\n", StandardCharsets.UTF_8,
					Files.exists(result) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE,
					StandardOpenOption.WRITE);
		} catch (IOException e) {
			EnsouledMod.LOG.error("[ensouled] could not write selftest result", e);
		}
		if (AgentManager.get() != null) {
			AgentManager.get().saveAll();
		}
		if (attached) {
			releasePlot();
		}
		active = null;
		if (shutdownWhenDone) {
			server.stop(false);
		}
	}

	/** Drops the force-load tickets this run added; the world itself stays exactly as it is. */
	private void releasePlot() {
		if (plotBase == null) {
			return;
		}
		ServerWorld world = server.getOverworld();
		for (int cx = (plotBase.x() - 24) >> 4; cx <= (plotBase.x() + 24) >> 4; cx++) {
			for (int cz = (plotBase.z() - 24) >> 4; cz <= (plotBase.z() + 24) >> 4; cz++) {
				world.setChunkForced(cx, cz, false);
			}
		}
	}
}

package dev.ensouled.command;

import java.util.List;
import java.util.UUID;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import dev.ensouled.core.AgentMind;
import dev.ensouled.core.BlockIds;
import dev.ensouled.core.ChatTranscript;
import dev.ensouled.core.Episode;
import dev.ensouled.core.FrameLedger;
import dev.ensouled.domain.Command;
import dev.ensouled.domain.WorldState;
import dev.ensouled.core.EvidenceRecord;
import dev.ensouled.core.Peers;
import dev.ensouled.core.Vec3i;
import dev.ensouled.core.Wishes;
import dev.ensouled.harness.HarnessControl;
import dev.ensouled.interact.ChestInteraction;
import dev.ensouled.mind.AgentManager;
import dev.ensouled.mind.AgentRecord;
import dev.ensouled.persist.AgentContinuity;
import dev.ensouled.persist.MindJournal;
import dev.ensouled.persist.MindStorage;
import dev.ensouled.skills.RequestFromHumanRoutine;
import dev.ensouled.skills.RequestSuppliesRoutine;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** /ensouled — spawn a being and inspect its mind, memory, evidence trail and canonical events. */
public final class EnsouledCommands {
	private EnsouledCommands() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				dispatcher.register(CommandManager.literal("ensouled")
						// Anyone may ask to meet a being: /ensouled humanprobe is the human side of HUM-01
						// and must work for an ordinary player who joined over the network.
						.then(CommandManager.literal("humanprobe")
								.executes(EnsouledCommands::humanProbe))
						// The durable facts of this world: identity, revision, homebases, runs. Read-only.
						.then(CommandManager.literal("world")
								.executes(EnsouledCommands::world))
						// DEV-01: run an acceptance phase inside this server instead of booting a probe server
						// that owns and deletes run/world. The server, the world and every session stay up.
						.then(CommandManager.literal("harness")
								.requires(EnsouledCommands::operator)
								.then(CommandManager.literal("status")
										.executes(EnsouledCommands::harnessStatus))
								.then(CommandManager.literal("cancel")
										.executes(EnsouledCommands::harnessCancel))
								.then(CommandManager.argument("phase", StringArgumentType.word())
										.executes(EnsouledCommands::harnessStart)))
						.then(CommandManager.literal("spawn")
								.requires(EnsouledCommands::operator)
								.then(CommandManager.argument("name", StringArgumentType.word())
										.executes(EnsouledCommands::spawn)))
						.then(CommandManager.literal("spawnplayer")
								.requires(EnsouledCommands::operator)
								.then(CommandManager.argument("name", StringArgumentType.word())
										.executes(EnsouledCommands::spawnPlayer)))
						.then(CommandManager.literal("list")
								.requires(EnsouledCommands::operator)
								.executes(EnsouledCommands::list))
						.then(CommandManager.literal("inspect")
								.requires(EnsouledCommands::operator)
								.then(CommandManager.argument("name", StringArgumentType.word())
										.executes(EnsouledCommands::inspect)))
						.then(CommandManager.literal("memory")
								.requires(EnsouledCommands::operator)
								.then(CommandManager.argument("name", StringArgumentType.word())
										.executes(EnsouledCommands::memory)))
						.then(CommandManager.literal("evidence")
								.requires(EnsouledCommands::operator)
								.then(CommandManager.argument("name", StringArgumentType.word())
										.executes(EnsouledCommands::evidence)))
						.then(CommandManager.literal("continuity")
								.requires(EnsouledCommands::operator)
								.executes(EnsouledCommands::continuity)
								.then(CommandManager.argument("name", StringArgumentType.word())
										.executes(EnsouledCommands::continuityOf)))
						.then(CommandManager.literal("journal")
								.requires(EnsouledCommands::operator)
								.executes(context -> journal(context, 20))
								.then(CommandManager.argument("count", IntegerArgumentType.integer(1, 500))
										.executes(context -> journal(context,
												IntegerArgumentType.getInteger(context, "count")))))
						.then(CommandManager.literal("save")
								.requires(EnsouledCommands::operator)
								.executes(context -> {
									AgentManager manager = AgentManager.get();
									if (manager == null) {
										context.getSource().sendError(Text.literal("Agent manager is not running"));
										return 0;
									}
									long before = manager.storageGeneration();
									manager.saveAll();
									long after = manager.storageGeneration();
									if (after == before) {
										context.getSource().sendError(Text.literal("Snapshot refused: "
												+ manager.store().describe()));
										return 0;
									}
									context.getSource().sendFeedback(() -> Text.literal(
											"Persisted every mind as generation " + after), false);
									return 1;
								}))
						// §47.4: the conversation the world actually carried, and what happened to each
						// line of it. The transcript is the record; these commands only read it.
						.then(CommandManager.literal("chat")
								.requires(EnsouledCommands::operator)
								.then(CommandManager.literal("history")
										.executes(context -> chatHistory(context, "", ChatTranscript.DEFAULT_HISTORY))
										.then(CommandManager.argument("who", StringArgumentType.word())
												.executes(context -> chatHistory(context,
														StringArgumentType.getString(context, "who"),
														ChatTranscript.DEFAULT_HISTORY))
												.then(CommandManager.argument("limit", IntegerArgumentType.integer(1, 200))
														.executes(context -> chatHistory(context,
																StringArgumentType.getString(context, "who"),
																IntegerArgumentType.getInteger(context, "limit"))))))
								.then(CommandManager.literal("trace")
										.then(CommandManager.argument("id", StringArgumentType.word())
												.executes(context -> chatTrace(context,
														StringArgumentType.getString(context, "id")))))
								.then(CommandManager.literal("visibility")
										.then(CommandManager.argument("mode", StringArgumentType.word())
												.executes(context -> chatVisibility(context,
														StringArgumentType.getString(context, "mode"))))))
						// What the residents owe, which plan serves it, and where each run has got to. A
						// reader: it never opens, advances or expires anything.
						.then(CommandManager.literal("work")
								.requires(EnsouledCommands::operator)
								.executes(EnsouledCommands::work))
						// Who the world will accept as a resident, and the corpus word each name came from.
						.then(CommandManager.literal("residents")
								.requires(EnsouledCommands::operator)
								.executes(EnsouledCommands::residents))
						// An operator may put a wish to a being. It is never a way to make it talk.
						.then(CommandManager.literal("wish")
								.requires(EnsouledCommands::operator)
								.then(CommandManager.argument("name", StringArgumentType.word())
										.then(CommandManager.argument("text", StringArgumentType.greedyString())
												.executes(EnsouledCommands::wish))))));
	}

	private static int residents(CommandContext<ServerCommandSource> context) {
		StringBuilder sb = new StringBuilder("residents (names come from the prose corpus, not a list here):\n");
		for (dev.ensouled.core.ResidentNames.Resident resident
				: dev.ensouled.core.ResidentNames.defaults()) {
			sb.append("  ").append(resident).append('\n');
		}
		String text = sb.toString();
		context.getSource().sendFeedback(() -> Text.literal(text), false);
		return 1;
	}

	/** The long-horizon record: commitments, their plans, their windows and their runs. */
	private static int work(CommandContext<ServerCommandSource> context) {
		dev.ensouled.work.WorkLedger ledger = dev.ensouled.work.WorkLedger.get();
		if (ledger == null) {
			context.getSource().sendError(Text.literal("No world identity is open"));
			return 0;
		}
		context.getSource().sendFeedback(() -> Text.literal(ledger.describe()), false);
		return 1;
	}

	/** The menu of a wish, printed where the operator can read it back. */
	private static int wish(CommandContext<ServerCommandSource> context) {
		AgentManager manager = AgentManager.get();
		if (manager == null) {
			context.getSource().sendError(Text.literal("Agent manager is not running"));
			return 0;
		}
		WorldState state = WorldState.get();
		if (state == null) {
			context.getSource().sendError(Text.literal("This world has no identity; nothing may be commanded"));
			return 0;
		}
		String name = StringArgumentType.getString(context, "name");
		String text = StringArgumentType.getString(context, "text");
		long tick = context.getSource().getServer().getOverworld().getTime();
		// A wish is a command to the world, so it carries the revision it was decided against and a
		// key that makes a retry a no-op (§A6.4). The key is the wish itself: the same words to the
		// same being are the same request, not a second one.
		String key = "wish|" + name + "|" + text;
		// Ask before acting. A command that can have no effect must not move the revision, and a
		// previous pass proved how easy that is to get wrong: an accepted wish to nobody advanced the
		// world and then reported a retry for a wish that had never been put.
		if (state.hasApplied(key)) {
			context.getSource().sendFeedback(() -> Text.literal("That wish is already with " + name
					+ " (key " + key + "): a retry is not a second wish."), false);
			return 1;
		}
		if (manager.byName(name).isEmpty()) {
			context.getSource().sendError(Text.literal("No wish was put to " + name
					+ ": there is no such being here. Nothing was commanded and the world did not move."));
			return 0;
		}
		Wishes.Wish wish = Wishes.wish(name, text, tick);
		if (wish == null) {
			context.getSource().sendError(Text.literal("No wish was put to " + name
					+ ": the words were empty, or one was put to it a moment ago."
					+ " A wish is a toast on its own stream; it answers in its own words or refuses."));
			return 0;
		}
		Command command = new Command("cmd:" + UUID.randomUUID(), state.world().worldId(), state.revision(),
				key, "PutWish", context.getSource().getName(), text);
		WorldState.Result accepted = state.accept(command, tick);
		if (accepted != WorldState.Result.APPLIED) {
			context.getSource().sendError(Text.literal("Wish " + wish.id() + " was put to " + name
					+ " but its command was " + accepted + " (expected revision " + command.expectedRevision()
					+ ", world at " + state.revision() + "). It is not on the durable record."));
			return 0;
		}
		// A wish is an utterance put to a being, so it is read like any other: the frame records what
		// the operator actually asked, before the being can answer it. It is the second live caller of
		// the parser (chat is the first) and the only one an operator controls the wording of.
		FrameLedger.record("wish", context.getSource().getName(), text, tick);
		// The wish is only the asking. Whether a run exists is the being's answer: when it takes the
		// work up, the ledger opens the run at the first stage (§A4.1) and advances it as what it owes
		// comes in. Opening one here would record a run for a wish nobody accepted.
		final String wishId = wish.id();
		context.getSource().sendFeedback(() -> Text.literal("Wish " + wishId + " put to " + name
				+ " as a toast on its stream, command " + key + " at revision "
				+ state.revision() + ". Nothing is spoken for it: the wish may be refused."), false);
		return 1;
	}

	/** The world's durable facts: what a reader needs to check a command against (§A5.1, §A6.4). */
	private static int world(CommandContext<ServerCommandSource> context) {
		WorldState state = WorldState.get();
		if (state == null) {
			context.getSource().sendError(Text.literal("No world identity is open"));
			return 0;
		}
		context.getSource().sendFeedback(() -> Text.literal(state.describe()), false);
		context.getSource().sendFeedback(() -> Text.literal(FrameLedger.describe()), false);
		return 1;
	}

	private static int chatHistory(CommandContext<ServerCommandSource> context, String who, int limit) {
		List<ChatTranscript.Line> lines = ChatTranscript.lines();
		List<ChatTranscript.Line> shown = new java.util.ArrayList<>();
		for (ChatTranscript.Line line : lines) {
			if (line.involves(who)) {
				shown.add(line);
			}
		}
		int from = Math.max(0, shown.size() - limit);
		StringBuilder sb = new StringBuilder("chat transcript ").append(ChatTranscript.file())
				.append(": ").append(shown.size()).append(" line(s)")
				.append(who.isBlank() ? "" : " for " + who).append('\n');
		for (ChatTranscript.Line line : shown.subList(from, shown.size())) {
			sb.append("  t=").append(line.tick()).append(' ').append(line.messageId()).append(' ')
					.append(line.speakerName().isEmpty() ? line.speakerAgent() : line.speakerName())
					.append(" \"").append(line.text()).append("\" -> ").append(line.outcomes())
					.append(" transport=").append(line.transport()).append('\n');
		}
		String text = sb.toString();
		context.getSource().sendFeedback(() -> Text.literal(text), false);
		return 1;
	}

	private static int chatTrace(CommandContext<ServerCommandSource> context, String id) {
		List<ChatTranscript.Line> lines = ChatTranscript.lines();
		for (ChatTranscript.Line line : lines) {
			if (!line.messageId().equals(id)) {
				continue;
			}
			StringBuilder sb = new StringBuilder("chat ").append(id).append(" t=").append(line.tick())
					.append(" transport=").append(line.transport()).append('\n')
					.append("  \"").append(line.text()).append("\"\n");
			for (ChatTranscript.Row row : line.rows()) {
				sb.append("  ").append(row.recipientName().isEmpty() ? row.recipientId() : row.recipientName())
						.append(" ").append(row.outcome())
						.append(" named=").append(row.named())
						.append(" d=").append(row.distance())
						.append(" recipient=").append(row.recipientId()).append('\n');
			}
			String text = sb.toString();
			context.getSource().sendFeedback(() -> Text.literal(text), false);
			return 1;
		}
		context.getSource().sendError(Text.literal("No chat line '" + id + "' in " + ChatTranscript.file()));
		return 0;
	}

	private static int chatVisibility(CommandContext<ServerCommandSource> context, String mode) {
		ChatTranscript.Visibility next = ChatTranscript.Visibility.parse(mode);
		if (next == null) {
			context.getSource().sendError(Text.literal("Visibility is all or off: 'off' hides the operator's copies"
					+ " of a being's speech acts, not the conversation itself."));
			return 0;
		}
		ChatTranscript.setVisibility(next);
		context.getSource().sendFeedback(() -> Text.literal("Chat visibility " + next), false);
		return 1;
	}

	private static boolean operator(ServerCommandSource source) {
		return source.hasPermissionLevel(2);
	}

	/**
	 * V4 §49.1: which condition every life came back in, the generation it came from, and the
	 * transition journal that explains it. This is the operator's view of the durable record.
	 */
	private static int continuity(CommandContext<ServerCommandSource> context) {
		AgentManager manager = AgentManager.get();
		if (manager == null) {
			context.getSource().sendError(Text.literal("Agent manager is not running"));
			return 0;
		}
		StringBuilder sb = new StringBuilder("continuity store: ")
				.append(manager.store() == null ? "not loaded" : manager.store().describe())
				.append(" gen=").append(manager.storageGeneration())
				.append(" journal_entries=").append(manager.journal().size()).append('\n');
		for (AgentRecord record : manager.records()) {
			appendContinuity(sb, record.name(), record.agentId(), manager.continuityOf(record.agentId()),
					"live body=" + (record.body() == null ? "unbound" : record.bodyKind()));
		}
		if (manager.store() != null) {
			for (MindStorage.LoadedMind mind : manager.store().minds()) {
				if (manager.byAgentId(mind.agentId()).isPresent()) {
					continue;
				}
				sb.append("  ").append(mind.name()).append(" [").append(mind.agentId()).append("] state=")
						.append(mind.state())
						.append(" record_gen=").append(mind.generation())
						.append(" body=").append(mind.bodyKind().isEmpty() ? "none" : mind.bodyKind())
						.append(" condition=").append(mind.condition().isEmpty() ? "-" : mind.condition())
						.append('\n');
			}
		}
		String text = sb.toString();
		context.getSource().sendFeedback(() -> Text.literal(text), false);
		return 1;
	}

	private static int continuityOf(CommandContext<ServerCommandSource> context) {
		AgentRecord record = require(context);
		if (record == null) {
			return 0;
		}
		AgentManager manager = AgentManager.get();
		StringBuilder sb = new StringBuilder("continuity of ").append(record.name()).append(":\n");
		AgentContinuity facts = manager.continuityOf(record.agentId());
		appendContinuity(sb, record.name(), record.agentId(), facts, "live");
		if (facts != null) {
			sb.append("  bindings=").append(facts.bindings().size()).append('\n');
			for (AgentContinuity.Binding binding : facts.bindings()) {
				sb.append("    ").append(binding.bodyKind()).append(" profile=")
						.append(binding.profileId().isEmpty() ? "-" : binding.profileId())
						.append(" entity=").append(binding.entityId().isEmpty() ? "-" : binding.entityId())
						.append(" ticks=").append(binding.firstTick()).append("..").append(binding.lastTick())
						.append(" count=").append(binding.count()).append('\n');
			}
			sb.append("  last_session_end=").append(facts.lastSessionEnd() == 0 ? "never"
					: java.time.Instant.ofEpochMilli(facts.lastSessionEnd())).append('\n');
			sb.append("  unrecorded=").append(String.join(",", facts.unrecorded())).append('\n');
		}
		sb.append("  journal (last 8 of this life):\n");
		List<MindJournal.Entry> entries = manager.journal().entriesFor(record.agentId());
		for (MindJournal.Entry entry : entries.subList(Math.max(0, entries.size() - 8), entries.size())) {
			sb.append("    ").append(entry.describe()).append('\n');
		}
		String text = sb.toString();
		context.getSource().sendFeedback(() -> Text.literal(text), false);
		return 1;
	}

	private static void appendContinuity(StringBuilder sb, String name, String agentId,
			AgentContinuity facts, String where) {
		sb.append("  ").append(name).append(" [").append(agentId).append("] ");
		if (facts == null) {
			sb.append("state=ABSENT (no continuity record yet)").append('\n');
			return;
		}
		sb.append("state=").append(facts.state())
				.append(" boots=").append(facts.boots())
				.append(" bindings=").append(facts.bindings().size())
				.append(" last_body=").append(facts.lastBodyKind().isEmpty() ? "none" : facts.lastBodyKind())
				.append(" at=").append(facts.lastPosition())
				.append(" record_gen=").append(facts.observedGeneration())
				.append(' ').append(where)
				.append(" condition=").append(facts.condition().isEmpty() ? "-" : facts.condition())
				.append('\n');
	}

	private static int journal(CommandContext<ServerCommandSource> context, int count) {
		AgentManager manager = AgentManager.get();
		if (manager == null) {
			context.getSource().sendError(Text.literal("Agent manager is not running"));
			return 0;
		}
		List<MindJournal.Entry> entries = manager.journal().recent(count);
		StringBuilder sb = new StringBuilder("journal ").append(manager.journal().file().getName())
				.append(": last ").append(entries.size()).append(" of ")
				.append(manager.journal().size()).append('\n');
		for (MindJournal.Entry entry : entries) {
			sb.append("  ").append(entry.describe()).append('\n');
		}
		String text = sb.toString();
		context.getSource().sendFeedback(() -> Text.literal(text), false);
		return 1;
	}

	/** Starts a phase in this running server; nothing is stopped or reset when it finishes. */
	private static int harnessStart(CommandContext<ServerCommandSource> context) {
		ServerCommandSource source = context.getSource();
		String phase = StringArgumentType.getString(context, "phase");
		if (!HarnessControl.available()) {
			source.sendError(Text.literal("No acceptance harness on this server. Start it with"
					+ " scripts/devserver.sh (the harness is a companion mod on the gradle dev server)."));
			return 0;
		}
		var runId = HarnessControl.start(source.getServer(), phase, true);
		if (runId.isEmpty()) {
			source.sendError(Text.literal("Harness refused the phase (one is already running, or it could"
					+ " not start). " + HarnessControl.status().orElse("")));
			return 0;
		}
		source.sendFeedback(() -> Text.literal("harness run=" + runId.get() + " phase=" + phase
				+ " (attach mode: server, world and sessions stay up; result is logged and appended to"
				+ " run/ensouled-selftest-result.txt)"), false);
		return 1;
	}

	private static int harnessStatus(CommandContext<ServerCommandSource> context) {
		ServerCommandSource source = context.getSource();
		if (!HarnessControl.available()) {
			source.sendError(Text.literal("No acceptance harness on this server"));
			return 0;
		}
		source.sendFeedback(() -> Text.literal(HarnessControl.status()
				.map(s -> "harness running: " + s).orElse("harness idle")), false);
		return 1;
	}

	private static int harnessCancel(CommandContext<ServerCommandSource> context) {
		ServerCommandSource source = context.getSource();
		if (!HarnessControl.cancel(source.getServer())) {
			source.sendError(Text.literal("No harness phase is running"));
			return 0;
		}
		source.sendFeedback(() -> Text.literal("harness phase cancelled; the server stays up"), false);
		return 1;
	}

	/**
	 * The human side of HUM-01, runnable by any connected player on the dev server: place a chest
	 * beside them, put two logs in their own inventory, and introduce Ash. The agent then asks in
	 * ordinary chat, and only the player's own drop answers it — the server never drops for them.
	 */
	private static int humanProbe(com.mojang.brigadier.context.CommandContext<ServerCommandSource> context) {
		ServerCommandSource source = context.getSource();
		ServerPlayerEntity player = source.getPlayer();
		if (player == null) {
			source.sendError(Text.literal("Run /ensouled humanprobe as a player: the demo needs somebody to ask."));
			return 0;
		}
		if (AgentManager.get() == null) {
			source.sendError(Text.literal("Agent manager is not running"));
			return 0;
		}
		ServerWorld world = (ServerWorld) player.getWorld();
		BlockPos base = player.getBlockPos();
		BlockPos chestPos = null;
		Direction chestSide = null;
		boolean stocked = false;
		boolean placeChest = false;
		for (Direction side : Direction.Type.HORIZONTAL) {
			BlockPos candidate = base.offset(side);
			BlockEntity entity = world.getBlockEntity(candidate);
			if (entity instanceof Inventory inventory) {
				chestPos = candidate;
				chestSide = side;
				stocked = ChestInteraction.count(inventory, RequestFromHumanRoutine.ITEM) > 0;
				if (!stocked) {
					break;
				}
				continue;
			}
			if (world.getBlockState(candidate).isAir() && !world.getBlockState(candidate.down()).isAir()) {
				chestPos = candidate;
				chestSide = side;
				placeChest = true;
				stocked = false;
				break;
			}
		}
		if (chestPos == null) {
			source.sendError(Text.literal("No free block beside you for a chest; step into the open and try again."));
			return 0;
		}
		if (stocked) {
			source.sendError(Text.literal("Every container beside you already holds oak_log; empty one and try again."));
			return 0;
		}
		BlockPos ashPos = base.offset(chestSide, 2);
		// Harvest utility is 650 - distance*45, so a reachable trunk inside seven blocks outranks the
		// human request at 450. Refuse the setup instead of raising the score or letting the being
		// silently walk off to chop: this is the scenario's precondition, and the player can meet it.
		for (int dx = -7; dx <= 7; dx++) {
			for (int dy = -7; dy <= 7; dy++) {
				for (int dz = -7; dz <= 7; dz++) {
					BlockPos log = ashPos.add(dx, dy, dz);
					if (BlockIds.isNaturalLog(Registries.BLOCK.getId(world.getBlockState(log).getBlock()).toString())) {
						source.sendError(Text.literal("A natural log at " + log.getX() + "," + log.getY() + ","
								+ log.getZ() + " is within seven blocks of Ash's spot, so it would rather harvest."
								+ " Step into the open and run this again."));
						return 0;
					}
				}
			}
		}
		if (placeChest) {
			world.setBlockState(chestPos, Blocks.CHEST.getDefaultState(), 3);
		}

		final BlockPos chest = chestPos;
		player.getInventory().insertStack(new ItemStack(Items.OAK_LOG, RequestFromHumanRoutine.QUANTITY));
		int held = 0;
		for (int slot = 0; slot < player.getInventory().size(); slot++) {
			ItemStack stack = player.getInventory().getStack(slot);
			if (stack.isEmpty() || !Registries.ITEM.getId(stack.getItem()).toString()
					.equals(RequestFromHumanRoutine.ITEM)) {
				continue;
			}
			held += stack.getCount();
			if (slot < 9) {
				player.getInventory().selectedSlot = slot;
			}
		}
		if (held < RequestFromHumanRoutine.QUANTITY) {
			source.sendError(Text.literal("Your inventory is full; free a slot so you can hold the two logs."));
			return 0;
		}

		if (AgentManager.get().byName("Ash").isPresent()) {
			source.sendFeedback(() -> Text.literal("Ash is already here; it will ask when it can see you and its chest at "
					+ chest + " is short of oak_log."), false);
			return 1;
		}
		AgentRecord record = AgentManager.get().spawnPlayer(world,
				new Vec3i(ashPos.getX(), ashPos.getY(), ashPos.getZ()), "Ash");
		if (record == null) {
			source.sendError(Text.literal("Refused to introduce Ash: the continuity store is not writable ("
					+ AgentManager.get().store().describe() + ")"));
			return 0;
		}
		source.sendFeedback(() -> Text.literal("Ash [" + record.agentId() + "] will ask you in chat for "
				+ RequestFromHumanRoutine.QUANTITY + " oak_log. Press Q beside it to drop them; it will pick "
				+ "them up and stow them in " + chest + "."), false);
		return 1;
	}

	private static int spawn(com.mojang.brigadier.context.CommandContext<ServerCommandSource> context) {
		if (AgentManager.get() == null) {
			context.getSource().sendError(Text.literal("Agent manager is not running"));
			return 0;
		}
		String name = StringArgumentType.getString(context, "name");
		ServerCommandSource source = context.getSource();
		Vec3d position = source.getPosition();
		ServerWorld world = source.getWorld();
		Vec3i pos = new Vec3i(MathHelper.floor(position.x), MathHelper.floor(position.y), MathHelper.floor(position.z));
		AgentRecord record = AgentManager.get().spawnMob(world, pos, name);
		if (record == null) {
			source.sendError(Text.literal("Refused to spawn a being: the continuity store is not writable ("
					+ AgentManager.get().store().describe() + ")"));
			return 0;
		}
		source.sendFeedback(() -> Text.literal("Spawned mob body " + record.name() + " [" + record.agentId() + "] at " + pos), false);
		return 1;
	}

	private static int spawnPlayer(com.mojang.brigadier.context.CommandContext<ServerCommandSource> context) {
		if (AgentManager.get() == null) {
			context.getSource().sendError(Text.literal("Agent manager is not running"));
			return 0;
		}
		String name = StringArgumentType.getString(context, "name");
		ServerCommandSource source = context.getSource();
		Vec3d position = source.getPosition();
		ServerWorld world = source.getWorld();
		Vec3i pos = new Vec3i(MathHelper.floor(position.x), MathHelper.floor(position.y), MathHelper.floor(position.z));
		AgentRecord record = AgentManager.get().spawnPlayer(world, pos, name);
		if (record == null) {
			source.sendError(Text.literal("Refused to spawn a being: the continuity store is not writable ("
					+ AgentManager.get().store().describe() + ")"));
			return 0;
		}
		source.sendFeedback(() -> Text.literal("Spawned fake player " + record.name() + " [" + record.agentId()
				+ "] profile=" + record.profileId() + " at " + pos), false);
		return 1;
	}

	private static int list(com.mojang.brigadier.context.CommandContext<ServerCommandSource> context) {
		if (AgentManager.get() == null || AgentManager.get().records().isEmpty()) {
			context.getSource().sendFeedback(() -> Text.literal("No ensouled beings"), false);
			return 1;
		}
		StringBuilder sb = new StringBuilder("Ensouled beings:\n");
		for (AgentRecord record : AgentManager.get().records()) {
			AgentMind mind = record.mind();
			AgentContinuity facts = AgentManager.get().continuityOf(record.agentId());
			sb.append("  ").append(record.name()).append(" [").append(record.agentId()).append("] ")
					.append("state=").append(facts == null ? "ABSENT" : facts.state())
					.append(" gen=").append(facts == null ? 0 : facts.observedGeneration())
					.append(" kind=").append(record.bodyKind())
					.append(" body=").append(record.body() == null ? "unloaded" : "live")
					.append(" food=").append(mind.needs().food())
					.append(" harvests=").append(mind.ledger().harvestCount())
					.append(" current=").append(mind.currentIntention().map(Object::toString).orElse("idle"))
					.append('\n');
		}
		String text = sb.toString();
		context.getSource().sendFeedback(() -> Text.literal(text), false);
		return 1;
	}

	private static int inspect(com.mojang.brigadier.context.CommandContext<ServerCommandSource> context) {
		AgentRecord record = require(context);
		if (record == null) {
			return 0;
		}
		AgentMind mind = record.mind();
		StringBuilder sb = new StringBuilder(mind.describe()).append('\n');
		sb.append("  home=").append(record.home()).append(" gate_budget=").append(record.gate().budgetRemaining()).append('\n');
		// Why the being is idle: how many candidates its own generator produced on the last idle tick,
		// what its hands perceive, and whether a reflex owns the body instead.
		sb.append("  perceive: choices=").append(mind.lastDecisionChoices())
				.append(" objects=").append(mind.perceivedObjects(RequestSuppliesRoutine.STORAGE_RADIUS).size())
				.append(" peers=").append(record.view() == null ? "unbound"
						: Peers.nearest(record.view(), mind.social(), record.agentId(), 12)
								.map(peer -> peer.agentId() + "@" + String.format("%.1f", peer.distance()))
								.orElse("none"))
				.append(" reflex=").append(mind.reflexEngaged())
				.append(" pending=").append(mind.social().bus().pending(record.agentId()))
				.append(" head=").append(mind.social().bus().peek(record.agentId(), mind.tick())
						.map(message -> message.speechAct() + ":" + message.messageId())
						.orElse("-"))
				.append('\n');
		sb.append("  last_evidence:").append('\n');
		for (EvidenceRecord evidence : mind.ledger().evidence().recent(8)) {
			sb.append("    t=").append(evidence.tick()).append(' ').append(evidence.kind())
					.append(' ').append(evidence.outcome()).append(' ').append(evidence.detail()).append('\n');
		}
		String text = sb.toString();
		context.getSource().sendFeedback(() -> Text.literal(text), false);
		return 1;
	}

	private static int memory(com.mojang.brigadier.context.CommandContext<ServerCommandSource> context) {
		AgentRecord record = require(context);
		if (record == null) {
			return 0;
		}
		AgentMind mind = record.mind();
		StringBuilder sb = new StringBuilder("Memory of ").append(record.name()).append(":\n");
		for (Episode episode : mind.ledger().memory().recent(12)) {
			sb.append("  t=").append(episode.tick()).append(' ').append(episode.kind()).append(' ')
					.append(episode.summary()).append(" @").append(episode.where())
					.append(" (").append(episode.provenance()).append(")\n");
		}
		sb.append("  canonical events: ").append(mind.ledger().events().size()).append('\n');
		for (var event : mind.ledger().events().all()) {
			sb.append("    ").append(event.type()).append(' ').append(event.subjectId()).append(" t=").append(event.tick()).append('\n');
		}
		String text = sb.toString();
		context.getSource().sendFeedback(() -> Text.literal(text), false);
		return 1;
	}

	private static int evidence(com.mojang.brigadier.context.CommandContext<ServerCommandSource> context) {
		AgentRecord record = require(context);
		if (record == null) {
			return 0;
		}
		StringBuilder sb = new StringBuilder("Evidence trail of ").append(record.name()).append(":\n");
		for (EvidenceRecord entry : record.mind().ledger().evidence().recent(20)) {
			sb.append("  t=").append(entry.tick()).append(' ').append(entry.kind())
					.append(" hash=").append(entry.actionHash())
					.append(" -> ").append(entry.outcome()).append(' ').append(entry.detail()).append('\n');
		}
		String text = sb.toString();
		context.getSource().sendFeedback(() -> Text.literal(text), false);
		return 1;
	}

	private static AgentRecord require(com.mojang.brigadier.context.CommandContext<ServerCommandSource> context) {
		if (AgentManager.get() == null) {
			context.getSource().sendError(Text.literal("Agent manager is not running"));
			return null;
		}
		String name = StringArgumentType.getString(context, "name");
		var record = AgentManager.get().byName(name);
		if (record.isEmpty()) {
			context.getSource().sendError(Text.literal("No being named '" + name + "'"));
			return null;
		}
		return record.get();
	}
}

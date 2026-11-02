package dev.ensouled.body;

import java.util.List;
import java.util.Map;

import dev.ensouled.core.Motor;
import dev.ensouled.core.MotorCommand;
import dev.ensouled.core.MotorResult;
import dev.ensouled.core.Vec3i;
import dev.ensouled.core.WorldView;
import dev.ensouled.interact.InteractionHands;
import dev.ensouled.interact.PlayerInteractionHands;
import dev.ensouled.mind.ExternalBrains;
import dev.ensouled.net.ControlRelay;
import dev.ensouled.net.SpeechRelay;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;

/**
 * The connected player body of an existing agent (NET-01). The mind keeps its identity, memory and
 * record; perception reads the real player's replicated state, and actions are delegated to the
 * headless client over {@code ensouled:control}, which turns them into ordinary player input. Results
 * are verified against the server-authoritative player position, never from the acknowledgement.
 */
public final class NetworkPlayerBodyAdapter implements BodyAdapter {
	private static final int LOOK_POLLS = 3;

	private final ServerPlayerEntity player;

	public NetworkPlayerBodyAdapter(ServerPlayerEntity player) {
		this.player = player;
	}

	public ServerPlayerEntity player() {
		return player;
	}

	@Override
	public String agentId() {
		// The adapter is attached to an existing AgentRecord; the record's id stays authoritative.
		return "agent:" + player.getUuid();
	}

	@Override
	public String displayName() {
		return player.getGameProfile().getName();
	}

	@Override
	public String kind() {
		return "player";
	}

	@Override
	public int entityId() {
		return player.getId();
	}

	@Override
	public boolean alive() {
		return player.isAlive() && !player.isRemoved() && !player.isDisconnected();
	}

	@Override
	public Entity entity() {
		return player;
	}

	@Override
	public ServerWorld world() {
		return (ServerWorld) player.getWorld();
	}

	@Override
	public Vec3i position() {
		return BlockSight.toVec(player.getBlockPos());
	}

	@Override
	public Motor createMotor() {
		// An external resident's own process is the body. It has no Fabric control channel, so the
		// mind's frames go through the file bridge instead of the mod's key-state packets.
		if (ExternalBrains.isExternal(player.getGameProfile().getName())) {
			return new FileMotor(player);
		}
		return new ClientInputMotor(player);
	}

	@Override
	public WorldView createView() {
		return new NetworkWorldView(player);
	}

	@Override
	public boolean say(String text) {
		if (!alive() || text == null || text.isBlank()) {
			return false;
		}
		if (ExternalBrains.isExternal(player.getGameProfile().getName())) {
			// The Node body owns its ordinary chat send; the server only writes the line it decided
			// to say. The Node client calls bot.chat, which emits a normal player-chat packet.
			return FileMotor.say(player, text);
		}
		SpeechRelay.send(player, text);
		return true;
	}

	@Override
	public void tickBody() {
		// The client ticks this player and reports its own input; the server must not run physics for it.
	}

	@Override
	public InteractionHands interactions() {
		// Same container adapter a player body has always used: real reach, real chest inventory, real
		// before/after counts. It is a server-side container path (no client screen), which is the
		// documented divergence; screen parity through the client is a later capability.
		return new PlayerInteractionHands(player);
	}

	/** Motor program executed by the connected client, verified on the server. */
	private static final class ClientInputMotor implements Motor {
		private final ServerPlayerEntity player;
		private MotorCommand command;
		private int ticks;
		private boolean started;
		/** Ticks spent letting a hotbar selection reach the client before the action is sent. */
		private int settleTicks;
		private int itemBefore;
		private int foodBefore;
		private boolean actioned;

		ClientInputMotor(ServerPlayerEntity player) {
			this.player = player;
		}

		@Override
		public void start(MotorCommand command) {
			if (this.command != null) {
				return;
			}
			if (ControlRelay.open(player, command.getClass().getSimpleName()) == null) {
				// No ticket, no movement: an action that cannot be recorded must not run.
				return;
			}
			this.command = command;
			this.ticks = 0;
			this.started = false;
		}

		@Override
		public MotorResult poll() {
			if (command == null) {
				return MotorResult.cancelled("idle");
			}
			ticks++;
			if (command instanceof MotorCommand.MoveTo move) {
				return pollMove(move);
			}
			if (command instanceof MotorCommand.LookAt look) {
				return pollLook(look);
			}
			if (command instanceof MotorCommand.CollectDrops collect) {
				return pollCollect(collect);
			}
			if (command instanceof MotorCommand.Jump jump) {
				return pollJump(jump);
			}
			if (command instanceof MotorCommand.MineBlock mine) {
				return pollMine(mine);
			}
			if (command instanceof MotorCommand.DropItem drop) {
				return pollDrop(drop);
			}
			if (command instanceof MotorCommand.Equip equip) {
				return pollEquip(equip);
			}
			if (command instanceof MotorCommand.Eat eat) {
				return pollEat(eat);
			}
			if (command instanceof MotorCommand.Use use) {
				return pollUse(use);
			}
			if (command instanceof MotorCommand.MoveAwayFrom away) {
				Vec3i awayFrom = awayFrom(away);
				MotorResult step = walkTo(awayFrom, 1.5, away.timeoutTicks());
				return step.terminal() ? done(step) : step;
			}
			if (command instanceof MotorCommand.Wait wait) {
				return ticks >= wait.ticks()
						? done(MotorResult.success("waited", "waited " + ticks + " ticks", Map.of()))
						: MotorResult.running("waiting");
			}
			return done(MotorResult.failed("client_command_unimplemented",
					command.getClass().getSimpleName() + " has no client bridge yet"));
		}

		private MotorResult pollMove(MotorCommand.MoveTo move) {
			MotorResult step = walkTo(move.target(), move.radius(), move.timeoutTicks());
			// A finished walk must also end the program, or the motor stays busy and the routine can
			// never issue its next command.
			return step.terminal() ? done(step) : step;
		}

		/** Ticks of no progress before the walk is treated as blocked, and how many steps it may try. */
		private static final int STALL_TICKS = 30;
		private static final int MAX_HOPS = 3;
		private double lastDistance = Double.MAX_VALUE;
		private int stalled;
		private int hops;

		/**
		 * Walk toward a block position by holding the client's own forward key. Success is the server's
		 * view of where the player actually ended up, never the client's acknowledgement.
		 */
		private MotorResult walkTo(Vec3i target, double radius, int timeoutTicks) {
			if (!started) {
				started = true;
				face(target);
				ControlRelay.send(player, "forward", 1.0f, 0.0f);
				lastDistance = Double.MAX_VALUE;
			}
			Vec3i here = BlockSight.toVec(player.getBlockPos());
			if (here.distanceTo(target) <= radius) {
				ControlRelay.send(player, "forward", 0.0f, 0.0f);
				return MotorResult.success("arrived", "walked to " + here,
						Map.of("position", here.toString(), "distance", String.format("%.2f",
								here.distanceTo(target))));
			}
			double distance = here.distanceTo(target);
			// A walk that stops making progress is blocked, not slow: a one-block step, a fence or a
			// ledge stops forward input cold while the clock runs out. A person steps up; so does this,
			// and a walk that stays blocked says so by name instead of reporting a timeout.
			if (distance > lastDistance - 0.05) {
				stalled++;
			} else {
				stalled = 0;
			}
			lastDistance = distance;
			if (stalled >= STALL_TICKS) {
				stalled = 0;
				if (++hops > MAX_HOPS) {
					ControlRelay.send(player, "forward", 0.0f, 0.0f);
					return MotorResult.failed("movement_blocked",
							"blocked at " + here + " walking to " + target + " after " + MAX_HOPS + " step(s)");
				}
				face(target);
				ControlRelay.send(player, "jump", 1.0f, 0.0f);
			}
			if (ticks > timeoutTicks) {
				ControlRelay.send(player, "forward", 0.0f, 0.0f);
				return MotorResult.failed("movement_timeout",
						"stopped at " + here + " short of " + target);
			}
			face(target);
			return MotorResult.running("walking to " + target);
		}

		private Vec3i awayFrom(MotorCommand.MoveAwayFrom away) {
			double dx = player.getX() - (away.from().x() + 0.5);
			double dz = player.getZ() - (away.from().z() + 0.5);
			double length = Math.sqrt(dx * dx + dz * dz);
			if (length < 0.001) {
				dx = 1;
				dz = 0;
				length = 1;
			}
			return new Vec3i((int) Math.floor(player.getX() + dx / length * away.distance()),
					player.getBlockY(),
					(int) Math.floor(player.getZ() + dz / length * away.distance()));
		}

		/**
		 * Mine the block the way a person does: aim at it and hold the attack key. The client's own
		 * interaction manager decides when a swing lands and the server validates reach, tool, hardness
		 * and protection; this only reads back whether the block actually left the world.
		 */
		private MotorResult pollMine(MotorCommand.MineBlock mine) {
			BlockPos pos = new BlockPos(mine.target().x(), mine.target().y(), mine.target().z());
			String blockId = Registries.BLOCK.getId(player.getWorld().getBlockState(pos).getBlock()).toString();
			if (blockId.equals("minecraft:air")) {
				return done(MotorResult.success("block_removed", "target is now air",
						Map.of("block_removed", "true")));
			}
			if (!blockId.equals(mine.expectedBlockId())) {
				return done(MotorResult.failed("block_changed",
						"expected " + mine.expectedBlockId() + " but found " + blockId));
			}
			if (!started) {
				started = true;
				face(mine.target());
				ControlRelay.send(player, "attack", 1.0f, 0.0f);
			}
			// Keep the aim on the block while the swing is happening; a player's mouse would.
			face(mine.target());
			if (ticks > mine.timeoutTicks()) {
				return done(MotorResult.failed("mining_timeout",
						"block still " + blockId + " after " + ticks + " ticks of swinging"));
			}
			return MotorResult.running("mining " + mine.target());
		}

		/** Drop a held stack through the client's own drop: the item entity is the world's, not ours. */
		private MotorResult pollDrop(MotorCommand.DropItem drop) {
			if (!started) {
				int slot = selectedSlotWith(drop.itemId());
				if (slot < 0) {
					return done(MotorResult.failed("item_not_held",
							"the selected hotbar has no " + drop.itemId()));
				}
				started = true;
				itemBefore = player.getInventory().count(Registries.ITEM.get(new Identifier(drop.itemId())));
				ControlRelay.send(player, "select", slot, 0.0f);
				return MotorResult.running("selecting " + drop.itemId());
			}
			if (settleTicks++ < 2) {
				return MotorResult.running("selecting " + drop.itemId());
			}
			if (!actioned) {
				actioned = true;
				ControlRelay.send(player, "drop", 0.0f, drop.count() > 1 ? 1.0f : 0.0f);
				return MotorResult.running("dropping " + drop.itemId());
			}
			int now = player.getInventory().count(Registries.ITEM.get(new Identifier(drop.itemId())));
			if (now < itemBefore) {
				return done(MotorResult.success("dropped", "dropped " + (itemBefore - now) + " "
						+ drop.itemId(), Map.of("dropped", Integer.toString(itemBefore - now))));
			}
			if (ticks > drop.timeoutTicks()) {
				return done(MotorResult.failed("drop_not_observed",
						"the item count did not fall; the drop never reached the server"));
			}
			return MotorResult.running("dropping " + drop.itemId());
		}

		private MotorResult pollEquip(MotorCommand.Equip equip) {
			if (!started) {
				int slot = selectedSlotWith(equip.itemId());
				if (slot < 0) {
					return done(MotorResult.failed("item_not_held",
							"the selected hotbar has no " + equip.itemId()));
				}
				started = true;
				ControlRelay.send(player, "select", slot, 0.0f);
			}
			if (equip.itemId().equals(Registries.ITEM.getId(player.getMainHandStack().getItem()).toString())) {
				return done(MotorResult.success("equipped", "holding " + equip.itemId(), Map.of()));
			}
			if (ticks > equip.timeoutTicks()) {
				return done(MotorResult.failed("equip_not_observed", "the server never saw the held item change"));
			}
			return MotorResult.running("equipping " + equip.itemId());
		}

		/** Eat by holding right-click on food; success is the server's own hunger value rising. */
		private MotorResult pollEat(MotorCommand.Eat eat) {
			if (!started) {
				int slot = selectedSlotWith(eat.itemId());
				if (slot < 0) {
					return done(MotorResult.failed("no_food_item",
							"the selected hotbar has no " + eat.itemId()));
				}
				started = true;
				foodBefore = player.getHungerManager().getFoodLevel();
				itemBefore = player.getInventory().count(Registries.ITEM.get(new Identifier(eat.itemId())));
				ControlRelay.send(player, "select", slot, 0.0f);
				return MotorResult.running("selecting " + eat.itemId());
			}
			if (settleTicks++ < 2) {
				return MotorResult.running("selecting " + eat.itemId());
			}
			ControlRelay.send(player, "use", 1.0f, 0.0f);
			int foodAfter = player.getHungerManager().getFoodLevel();
			int itemsAfter = player.getInventory().count(Registries.ITEM.get(new Identifier(eat.itemId())));
			if (foodAfter > foodBefore || itemsAfter < itemBefore) {
				ControlRelay.send(player, "use", 0.0f, 0.0f);
				return done(MotorResult.success("ate", "food " + foodBefore + " -> " + foodAfter,
						Map.of("food_before", Integer.toString(foodBefore),
								"food_after", Integer.toString(foodAfter),
								"item_consumed", eat.itemId())));
			}
			if (ticks > eat.timeoutTicks()) {
				ControlRelay.send(player, "use", 0.0f, 0.0f);
				return done(MotorResult.failed("eat_timeout",
						"food stayed at " + foodAfter + "; nothing was consumed"));
			}
			return MotorResult.running("eating " + eat.itemId());
		}

		/**
		 * Right-click a block once. Success is the server's own view of the block changing, so a use that
		 * the server refused is a failure here rather than an assumed interaction.
		 */
		private MotorResult pollUse(MotorCommand.Use use) {
			BlockPos pos = new BlockPos(use.target().x(), use.target().y(), use.target().z());
			String before = Registries.BLOCK.getId(player.getWorld().getBlockState(pos).getBlock()).toString();
			if (!started) {
				started = true;
				face(use.target());
				ControlRelay.send(player, "use", 1.0f, 0.0f);
				return MotorResult.running("using " + use.target());
			}
			if (settleTicks++ < 3) {
				return MotorResult.running("using " + use.target());
			}
			ControlRelay.send(player, "use", 0.0f, 0.0f);
			String after = Registries.BLOCK.getId(player.getWorld().getBlockState(pos).getBlock()).toString();
			if (!before.equals(after)) {
				return done(MotorResult.success("used", "block changed " + before + " -> " + after,
						Map.of("block_changed", "true")));
			}
			if (ticks > use.timeoutTicks()) {
				return done(MotorResult.failed("use_not_observed", "the block did not change on the server"));
			}
			return MotorResult.running("using " + use.target());
		}

		/** A hotbar slot (0-8) holding this item, or -1 when the selected hotbar has none. */
		private int selectedSlotWith(String itemId) {
			for (int slot = 0; slot < 9; slot++) {
				var stack = player.getInventory().getStack(slot);
				if (!stack.isEmpty() && Registries.ITEM.getId(stack.getItem()).toString().equals(itemId)) {
					return slot;
				}
			}
			return -1;
		}

		private MotorResult pollLook(MotorCommand.LookAt look) {
			if (!started) {
				started = true;
				face(look.target());
			}
			if (ticks >= LOOK_POLLS) {
				float wanted = yawTo(look.target());
				float delta = Math.abs(MathHelper.wrapDegrees(player.getYaw() - wanted));
				return done(delta <= 8.0f
						? MotorResult.success("looked", "server saw yaw " + player.getYaw(),
								Map.of("yaw", String.format("%.1f", player.getYaw())))
						: MotorResult.failed("look_not_applied", "server yaw " + player.getYaw()
								+ " differs from " + wanted));
			}
			return MotorResult.running("turning");
		}

		private MotorResult pollJump(MotorCommand.Jump jump) {
			if (!started) {
				started = true;
				ControlRelay.send(player, "jump", 1.0f, 0.0f);
			}
			if (ticks >= 3 && !player.isOnGround()) {
				return done(MotorResult.success("jumped", "server saw the player leave the ground",
						Map.of("y", String.format("%.2f", player.getY()))));
			}
			if (ticks > jump.timeoutTicks()) {
				return done(MotorResult.failed("jump_not_observed", "server never saw the player airborne"));
			}
			return MotorResult.running("jumping");
		}

		/**
		 * Walk onto the nearest drop the way a player would: face it, hold forward, stop on top of it.
		 * The pickup itself is the server's own rule for a player standing over an item; this motor only
		 * supplies the movement, and reports success from the drop actually disappearing.
		 */
		private MotorResult pollCollect(MotorCommand.CollectDrops collect) {
			Box area = player.getBoundingBox().expand(collect.radius());
			List<ItemEntity> drops = player.getWorld().getEntitiesByClass(ItemEntity.class, area,
					item -> !item.getStack().isEmpty());
			if (drops.isEmpty()) {
				ControlRelay.send(player, "forward", 0.0f, 0.0f);
				return done(MotorResult.success("collected", "no drops remain nearby", Map.of()));
			}
			ItemEntity nearest = drops.get(0);
			for (ItemEntity drop : drops) {
				if (drop.squaredDistanceTo(player) < nearest.squaredDistanceTo(player)) {
					nearest = drop;
				}
			}
			if (nearest.squaredDistanceTo(player) <= 1.0) {
				// Standing on it: stop pressing and let the server's pickup rule run.
				ControlRelay.send(player, "forward", 0.0f, 0.0f);
				if (ticks > collect.timeoutTicks()) {
					return done(MotorResult.failed("pickup_not_observed",
							"stood over the drop but no pickup was observed"));
				}
				return MotorResult.running("standing over drop");
			}
			if (ticks > collect.timeoutTicks()) {
				ControlRelay.send(player, "forward", 0.0f, 0.0f);
				return done(MotorResult.failed("collect_timeout", "nearest drop still "
						+ String.format("%.2f", Math.sqrt(nearest.squaredDistanceTo(player))) + " blocks away"));
			}
			face(BlockSight.toVec(nearest.getBlockPos()));
			ControlRelay.send(player, "forward", 1.0f, 0.0f);
			return MotorResult.running("walking to drop");
		}

		/**
		 * Aim at the block itself, pitch included. A horizontal aim misses anything level with the eyes,
		 * and a player who is not looking at a block cannot break it however long they swing.
		 */
		private void face(Vec3i target) {
			double dx = target.x() + 0.5 - player.getX();
			double dy = target.y() + 0.5 - player.getEyeY();
			double dz = target.z() + 0.5 - player.getZ();
			double horizontal = Math.sqrt(dx * dx + dz * dz);
			float yaw = yawTo(target);
			float pitch = (float) (-MathHelper.atan2(dy, horizontal) * 180.0 / Math.PI);
			ControlRelay.send(player, "look", yaw, pitch);
		}

		private float yawTo(Vec3i target) {
			double dx = target.x() + 0.5 - player.getX();
			double dz = target.z() + 0.5 - player.getZ();
			return (float) (MathHelper.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
		}

		private MotorResult done(MotorResult result) {
			// No key stays held after the program ends: a client left pressing attack would keep
			// swinging at whatever it faces with no intention behind it.
			ControlRelay.send(player, "attack", 0.0f, 0.0f);
			ControlRelay.send(player, "use", 0.0f, 0.0f);
			ControlRelay.send(player, "forward", 0.0f, 0.0f);
			ControlRelay.close(player);
			command = null;
			return result;
		}

		@Override
		public void cancel() {
			if (command == null) {
				// Nothing was ever opened, so there is nothing to release and no action to refuse.
				return;
			}
			ControlRelay.send(player, "forward", 0.0f, 0.0f);
			ControlRelay.send(player, "attack", 0.0f, 0.0f);
			ControlRelay.send(player, "use", 0.0f, 0.0f);
			ControlRelay.close(player);
			command = null;
		}

		@Override
		public boolean busy() {
			return command != null;
		}
	}
}

package dev.ensouled.client;

import dev.ensouled.EnsouledMod;
import dev.ensouled.entity.AgentBodyEntity;
import dev.ensouled.net.ControlRelay;
import dev.ensouled.net.SpeechRelay;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import dev.ensouled.entity.EnsouledEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.Identifier;

/**
 * Renders the agent body as an ordinary humanoid so the being is visible in-world. Rendering is
 * presentation only; nothing here influences decisions.
 */
public final class EnsouledClient implements ClientModInitializer {
	private static final Identifier SKIN = new Identifier("textures/entity/player/wide/steve.png");

	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(EnsouledEntities.AGENT, context ->
				new MobEntityRenderer<AgentBodyEntity, PlayerEntityModel<AgentBodyEntity>>(context,
						new PlayerEntityModel<>(context.getPart(EntityModelLayers.PLAYER), false), 0.5f) {
					@Override
					public Identifier getTexture(AgentBodyEntity entity) {
						return SKIN;
					}
				});
		// Agent speech arrives as data; the client says it through its own ordinary chat-send path,
		// so it is broadcast by the server as that player's chat and reaches every connected player.
		ClientPlayNetworking.registerGlobalReceiver(SpeechRelay.CHANNEL,
				(client, handler, buf, responseSender) -> {
					String text = buf.readString(256);
					client.execute(() -> {
						if (client.getNetworkHandler() == null || client.player == null) {
							EnsouledMod.LOG.warn("[ensouled] speech dropped, no live session to say it on: {}", text);
							return;
						}
						client.getNetworkHandler().sendChatMessage(text);
						EnsouledMod.LOG.info("[ensouled] speech sent from this client: {}", text);
					});
				});
		// Control channel: the agent's motor is carried out by ordinary client input (key state and
		// rotation), which the client reports to the server like any player input.
		ClientPlayNetworking.registerGlobalReceiver(ControlRelay.CHANNEL,
				(client, handler, buf, responseSender) -> {
					String action = buf.readString(32);
					float a = buf.readFloat();
					float b = buf.readFloat();
					String ticket = buf.readString(64);
					String presenter = buf.readString(32);
					if (ticket.isBlank() || presenter.isBlank()) {
						// No ticket, no presenter, no movement: the stream is not an authority.
						EnsouledMod.LOG.warn("[ensouled] control refused {}: packet carries no ticket/presenter", action);
						return;
					}
					client.execute(() -> applyControl(client, action, a, b));
				});
		ClientTickEvents.END_CLIENT_TICK.register(EnsouledClient::applyHeldInput);
		// The served stream: this client's own feed, drawn on the game's own toast stack.
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> StreamClient.onJoin(client));
		EnsouledMod.LOG.info("[ensouled] client renderer, speech and control receivers registered");
	}

	private static int jumpTicks;
	/**
	 * Keys the being is holding. The client clears key state every tick from the real keyboard and
	 * mouse, neither of which a headless body has, so a press has to be re-applied for as long as the
	 * intention holds it. Without this a swing or a right-click lasts one tick and nothing happens.
	 */
	private static boolean attackHeld;
	private static boolean useHeld;
	/**
	 * Forward is held the same way. The client clears key state every tick from a keyboard this body
	 * does not have, so a walk that is pressed once is a walk that lasts one tick: the being then
	 * records `no_progress` while its routine was correct and the key was simply lifted.
	 */
	private static boolean forwardHeld;

	private static void applyControl(MinecraftClient client, String action, float a, float b) {
		switch (action) {
			case "forward" -> {
				forwardHeld = a > 0.5f;
				client.options.forwardKey.setPressed(forwardHeld);
			}
			case "look" -> {
				if (client.player != null) {
					client.player.setYaw(a);
					client.player.setPitch(b);
				}
			}
			case "jump" -> {
				client.options.jumpKey.setPressed(true);
				jumpTicks = 2;
			}
			// Attack is a held key: the client's own interaction manager decides when the swing lands,
			// exactly as it does for a person holding the mouse button over a block.
			case "attack" -> {
				attackHeld = a > 0.5f;
				client.options.attackKey.setPressed(attackHeld);
			}
			// Use is the same for placing, opening and eating: right-click held for as long as we ask.
			case "use" -> {
				useHeld = a > 0.5f;
				client.options.useKey.setPressed(useHeld);
			}
			case "select" -> {
				if (client.player != null) {
					int slot = Math.max(0, Math.min(8, (int) a));
					client.player.getInventory().selectedSlot = slot;
				}
			}
			// Dropping goes through the player's own drop path, so the stack becomes an item entity in
			// the world like anything a person throws down.
			case "drop" -> {
				if (client.player != null) {
					client.player.dropSelectedItem(b > 0.5f);
				}
			}
			default -> EnsouledMod.LOG.warn("[ensouled] unknown control action {}", action);
		}
	}

	private static void applyHeldInput(MinecraftClient client) {
		if (forwardHeld) {
			client.options.forwardKey.setPressed(true);
		}
		if (attackHeld) {
			client.options.attackKey.setPressed(true);
		}
		if (useHeld) {
			client.options.useKey.setPressed(true);
		}
		if (jumpTicks > 0 && --jumpTicks == 0) {
			client.options.jumpKey.setPressed(false);
		}
	}
}

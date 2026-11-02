package dev.ensouled.mind;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import dev.ensouled.core.AgentMessage;
import dev.ensouled.core.ChatTranscript;
import dev.ensouled.core.FrameLedger;
import dev.ensouled.core.Hearing;
import dev.ensouled.core.HumanChannel;
import dev.ensouled.core.MessageBus;
import dev.ensouled.core.Speech;
import dev.ensouled.core.SpeechInbox;
import dev.ensouled.core.Vec3i;
import dev.ensouled.EnsouledMod;
import dev.ensouled.entity.FakePlayerBody;
import dev.ensouled.net.SpeechRelay;

/**
 * Where a being's words meet the server: agent-to-agent speech acts are echoed as system lines,
 * ordinary chat is handed to the minds that could hear it, and a mind's own line leaves through its
 * connected client rather than a system message.
 *
 * <p>This is the whole of {@link dev.ensouled.core.SocialSeam} with a world behind it. Nothing here
 * decides anything: it carries speech, records that it carried it, and answers which entity is which
 * mind.
 */
final class SpeechRoutes implements dev.ensouled.core.SocialSeam {

	/** The registry questions speech needs, answered by the manager that owns them. */
	interface Minds {
		AgentRecord record(String agentId);

		String nameOf(String agentId);

		Optional<String> agentIdForPlayer(ServerPlayerEntity player);

		List<AgentRecord> records();

		MinecraftServer server();
	}

	/** How far ordinary speech carries, before a being's name is used. */
	static final int SPEECH_RADIUS = 32;

	private final Minds minds;
	/** One bus for every mind in the world: messages are per-recipient, never shared state. */
	private final MessageBus bus = new MessageBus();
	/** Ordinary chat waiting to be perceived; one world-wide inbox, shared like the message bus. */
	private final SpeechInbox heard = SpeechInbox.shared();
	/**
	 * Speech to ordinary humans (HUM-01). A session resolves here only when the registry does not
	 * bind it to a mind, so an agent body is never spoken to as a human and a human is never
	 * addressed as a peer.
	 */
	private final HumanChannel humanChannel = new HumanChannel() {
		@Override
		public Optional<String> displayName(String entityUuid) {
			return humanPlayer(entityUuid).map(player -> player.getGameProfile().getName());
		}

		@Override
		public boolean tell(String entityUuid, String text) {
			Optional<ServerPlayerEntity> player = humanPlayer(entityUuid);
			if (player.isEmpty()) {
				return false;
			}
			// Addendum F §F.6.3: a system message is not the bot's player chat. Tag it so nobody can
			// mistake this interim delivery for a connected client speaking.
			player.get().sendMessage(Text.literal("[ensouled] [System] " + text), false);
			return true;
		}
	};

	SpeechRoutes(Minds minds) {
		this.minds = minds;
		// Every speech act is also spoken aloud: a person standing in the world reads the exchange
		// in ordinary chat, through the server's own broadcast path, not a side channel.
		bus.onSpeech(this::announce);
	}

	@Override
	public MessageBus bus() {
		return bus;
	}

	@Override
	public Optional<HumanChannel> humans() {
		return Optional.of(humanChannel);
	}

	@Override
	public Hearing hearing() {
		return heard;
	}

	/**
	 * Ordinary player chat out of the being's own client (CHAT-01). A mind bound to an in-process body
	 * has no client and cannot speak as a player; that is reported as false, never faked with a system
	 * message, so the caller records `speech_unavailable` instead of believing it spoke.
	 */
	@Override
	public Speech speech() {
		return (agentId, text) -> {
			AgentRecord record = minds.record(agentId);
			if (record == null || record.body() == null || !record.body().alive()) {
				return false;
			}
			if (!record.body().say(text)) {
				EnsouledMod.LOG.info("[ensouled] speech unavailable: {} has no connected client ({})",
						record.name(), text);
				return false;
			}
			EnsouledMod.LOG.info("[ensouled] speech -> body {} ({}): {}", record.name(),
					record.body().kind(), text);
			return true;
		};
	}

	/**
	 * Diagnostic echo of an agent-to-agent speech act, shown to connected players in a form that
	 * cannot be mistaken for player chat. Addendum F §F.6.3: a server system/broadcast message is not
	 * a substitute for a bot sending ordinary player chat, so this line is explicitly tagged and the
	 * real capability stays with the connected-client milestone (`CHAT-02`). Messages addressed to a
	 * human session are skipped; that session already got the ask through {@link HumanChannel}, which
	 * is likewise an interim system message, not player speech.
	 */
	private void announce(AgentMessage message) {
		// Classify the act once, as evidence of what the being meant, before deciding how the line is
		// carried: the frame belongs to the utterance, not to whichever transport delivered it.
		FrameLedger.record("speech", minds.nameOf(message.senderId()), utterance(message),
				minds.server().getOverworld().getTime());
		if (message.recipientId().startsWith("human:")) {
			return;
		}
		// §47.4: operator visibility hides only the operator's copy. It never suppresses the line
		// itself, and it never changes who was a recipient.
		if (ChatTranscript.visibility() == ChatTranscript.Visibility.OFF) {
			return;
		}
		// Preferred path: the agent's own connected client says it, so players read ordinary chat.
		// The speaking session is the mind's body binding, never a name match: identity is the
		// registry's answer, not a display string.
		AgentRecord sender = minds.record(message.senderId());
		if (sender != null && sender.body() != null
				&& sender.body().entity() instanceof ServerPlayerEntity speaking
				&& !(speaking instanceof FakePlayerBody) && !speaking.isDisconnected()) {
			String said = utterance(message);
			SpeechRelay.send(speaking, said);
			EnsouledMod.LOG.info("[ensouled] speech -> client {}: {}",
					speaking.getGameProfile().getName(), said);
			return;
		}
		String item = message.itemId().substring(message.itemId().indexOf(':') + 1);
		// Marked [System] for the same reason the first fallback line is: this is the server
		// describing a message (addendum E.3.1), not the being speaking it.
		String line = "[ensouled] [System] " + minds.nameOf(message.senderId()) + " -> "
				+ minds.nameOf(message.recipientId()) + ": " + switch (message.speechAct()) {
					case AgentMessage.REQUEST -> "request " + message.count() + " " + item + " ("
							+ message.messageId() + ")";
					case AgentMessage.ACCEPT -> "accept " + message.count() + " " + item + " ("
							+ message.subject() + ")";
					case AgentMessage.DECLINE -> "decline " + item + " (" + message.reason() + ")";
					default -> message.speechAct();
				};
		minds.server().getPlayerManager().broadcast(Text.literal(line), false);
		EnsouledMod.LOG.info("[ensouled] chat {}", line);
	}

	private static String utterance(AgentMessage message) {
		String item = message.itemId().substring(message.itemId().indexOf(':') + 1);
		return switch (message.speechAct()) {
			case AgentMessage.REQUEST -> "do you have " + message.count() + " " + item + "?";
			case AgentMessage.ACCEPT -> "yes, " + message.count() + " " + item;
			case AgentMessage.DECLINE -> "no, " + message.reason();
			default -> message.speechAct();
		};
	}

	/**
	 * Ordinary player chat, handed to every being that could hear it (CHAT-01). A line reaches a mind
	 * only when the body is bound and the speaker is within earshot, or the line names the being; the
	 * mind then decides what, if anything, it means. Nothing here grants authority: a person's words
	 * are evidence in that mind's own trail, not a command.
	 */
	void hear(ServerPlayerEntity speaker, String text) {
		String line = text == null ? "" : text.strip();
		if (line.isEmpty()) {
			return;
		}
		MinecraftServer server = minds.server();
		String speakerId = speaker.getUuidAsString();
		String speakerName = speaker.getGameProfile().getName();
		Optional<String> speakerAgent = minds.agentIdForPlayer(speaker);
		Vec3i where = new Vec3i(speaker.getBlockX(), speaker.getBlockY(), speaker.getBlockZ());
		// World time, not server-lifetime ticks: a mind's clock is the world's, and a line stamped on
		// another base would look expired the moment it arrived.
		long now = server.getOverworld().getTime();
		// One id for one line, minted before any delivery decision, so every row about it shares it
		// and an operator can ask for its trace. §47.4/§47.5: the transcript is the durable half.
		String messageId = ChatTranscript.nextId(now);
		// An inbound line is a frame whether or not any being is close enough to hear it: what was
		// said is evidence about the speaker, and a parser that only ran on delivery would lose
		// exactly the lines nobody answered.
		FrameLedger.record("chat", speakerName, line, now);
		String speakerAgentId = speakerAgent.orElse("");
		List<AgentRecord> records = minds.records();
		int reached = 0;
		int traced = 0;
		for (AgentRecord record : records) {
			if (record.body() == null || !record.body().alive()) {
				continue;
			}
			double distance = record.body().position().distanceTo(where);
			if (speakerAgent.isPresent() && speakerAgent.get().equals(record.agentId())) {
				// The echo guard, on the record: a being never hears its own broadcast and so can never
				// answer itself in a loop.
				ChatTranscript.delivery(messageId, now, speakerId, speakerName, speakerAgentId, line,
						record.agentId(), record.name(), "self_excluded", false, distance);
				traced++;
				continue;
			}
			boolean named = mentions(line, record.name());
			if (!named && distance > SPEECH_RADIUS) {
				ChatTranscript.delivery(messageId, now, speakerId, speakerName, speakerAgentId, line,
						record.agentId(), record.name(), "out_of_earshot", false, distance);
				traced++;
				continue;
			}
			heard.deliver(record.agentId(), new Hearing.Heard(speakerId, speakerName, line, now, named));
			ChatTranscript.delivery(messageId, now, speakerId, speakerName, speakerAgentId, line,
					record.agentId(), record.name(), "delivered", named, distance);
			reached++;
			traced++;
		}
		if (traced == 0) {
			// A line in a world with no bound mind is still a line: it is in the transcript with the
			// reason nobody heard it, rather than vanished.
			ChatTranscript.delivery(messageId, now, speakerId, speakerName, speakerAgentId, line, "", "",
					"no_mind_bound", false, 0.0);
		}
		EnsouledMod.LOG.info("[ensouled] heard \"{}\" from {} ({}) by {} mind(s) of {} [{}]", line,
				speakerName, speakerId, reached, records.size(), messageId);
	}

	/** Did this line use that being's name as a word, rather than as part of another word? */
	private static boolean mentions(String line, String name) {
		for (String word : line.toLowerCase(java.util.Locale.ROOT).split("[^a-z0-9_]+")) {
			if (word.equals(name.toLowerCase(java.util.Locale.ROOT))) {
				return true;
			}
		}
		return false;
	}

	/** A connected player session that this registry does not bind to an agent mind. */
	private Optional<ServerPlayerEntity> humanPlayer(String entityUuid) {
		UUID id;
		try {
			id = UUID.fromString(entityUuid);
		} catch (IllegalArgumentException | NullPointerException e) {
			return Optional.empty();
		}
		ServerPlayerEntity player = minds.server().getPlayerManager().getPlayer(id);
		if (player == null || player.isDisconnected() || minds.agentIdForPlayer(player).isPresent()) {
			return Optional.empty();
		}
		return Optional.of(player);
	}

	/**
	 * Only registered agent bodies resolve here; a human player is never addressed as a peer.
	 *
	 * <p>A connected player's session UUID is not its {@code AgentId}: a bot that joined the server
	 * through a normal login carries the profile UUID of that login, while its mind keeps the id it
	 * was minted with. Resolve a live session through the body binding first, and only fall back to
	 * the id convention for body kinds the player list does not own.
	 */
	@Override
	public Optional<String> agentIdForEntity(String entityUuid) {
		UUID id;
		try {
			id = UUID.fromString(entityUuid);
		} catch (IllegalArgumentException | NullPointerException e) {
			return Optional.empty();
		}
		ServerPlayerEntity player = minds.server().getPlayerManager().getPlayer(id);
		if (player != null) {
			return minds.agentIdForPlayer(player);
		}
		String agentId = "agent:" + entityUuid;
		return minds.record(agentId) != null ? Optional.of(agentId) : Optional.empty();
	}
}

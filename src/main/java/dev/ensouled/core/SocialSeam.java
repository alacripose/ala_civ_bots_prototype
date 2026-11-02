package dev.ensouled.core;

import java.util.Optional;

/**
 * How one mind reaches other minds (Addendum C §C.6): the shared inboxes, and which perceived
 * player entity actually belongs to a registered agent. A human player is not an agent body, so it
 * never resolves here and must be interacted with as a participant, not addressed as a peer.
 */
public interface SocialSeam {
	MessageBus bus();

	Optional<String> agentIdForEntity(String entityUuid);

	/**
	 * The human-facing speech path, when this world has one (HUM-01). Empty for a being living
	 * alone: with no channel there is nobody to address, and no candidate may pretend otherwise.
	 */
	default Optional<HumanChannel> humans() {
		return Optional.empty();
	}

	/**
	 * Ordinary player chat as this being hears it (CHAT-01). A mind that can hear nothing still
	 * decides normally; hearing is perception, not a channel anyone may command it through.
	 */
	default Hearing hearing() {
		return Hearing.none();
	}

	/**
	 * Ordinary player chat as this being says it (CHAT-01), through its own connected client. A body
	 * with no client cannot speak as a player and reports that instead of faking a line.
	 */
	default Speech speech() {
		return Speech.none();
	}

	/** No peers, no mailbox traffic: the default for a being living alone. */
	static SocialSeam isolated() {
		return Isolated.INSTANCE;
	}

	final class Isolated implements SocialSeam {
		private static final Isolated INSTANCE = new Isolated();
		private final MessageBus bus = new MessageBus();

		private Isolated() {
		}

		@Override
		public MessageBus bus() {
			return bus;
		}

		@Override
		public Optional<String> agentIdForEntity(String entityUuid) {
			return Optional.empty();
		}
	}
}

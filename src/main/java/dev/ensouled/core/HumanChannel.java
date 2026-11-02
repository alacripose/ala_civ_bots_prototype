package dev.ensouled.core;

import java.util.Optional;

/**
 * The agent-to-human speech path (HUM-01). A mind may address one human session it has perceived;
 * whether the human acts on what it heard is the human's decision, never the server's.
 *
 * <p>This is deliberately narrower than the typed agent bus: an utterance is an offer of
 * information, not a fact, not a request the sender can enforce, and not authority over anything a
 * gate protects. A channel that cannot reach the session says so instead of pretending it spoke.
 */
public interface HumanChannel {
	/** Display name of a live human session, or empty when that session is not present. */
	Optional<String> displayName(String entityUuid);

	/** Hand one line of speech to that session's ordinary chat; false when it is not connected. */
	boolean tell(String entityUuid, String text);
}

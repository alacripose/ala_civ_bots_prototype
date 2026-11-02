package dev.ensouled.core;

/**
 * Ordinary player chat as a mind produces it (Addendum F §F.6.3, CHAT-01): the line is spoken by the
 * being's own connected client through the normal chat-send path, so other players read player chat
 * under the being's own name. There is no system-message fallback here: a body with no client cannot
 * speak as a player and says so, rather than pretending the line reached anyone.
 */
public interface Speech {
	/**
	 * @return true only when a connected client actually sent the line
	 */
	boolean say(String agentId, String text);

	/** A being with no connected client: silence, reported as unavailable by the caller. */
	static Speech none() {
		return (agentId, text) -> false;
	}
}

package dev.ensouled.participant;

/**
 * Who a connected player session is, decided by the server rather than by the client or by a Java
 * subclass (Addendum D §D.1, correction §2).
 *
 * <ul>
 *   <li>{@link #AGENT} — the session is bound to a registered mind in the agent registry; the mind
 *       owns that body. A bot connecting through an ordinary client login resolves here once its
 *       registry entry is bound, which is why identity is never {@code instanceof}-based.
 *   <li>{@link #PARTICIPANT} — an ordinary human player. Joining is never refused and the session is
 *       never disconnected, spectated or restricted by this classification.
 * </ul>
 */
public enum ParticipantRole {
	AGENT,
	PARTICIPANT;

	public boolean isAgent() {
		return this == AGENT;
	}
}

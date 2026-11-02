package dev.ensouled.core;

import java.util.List;

/**
 * Ordinary player chat as a mind perceives it (Addendum F §F.6.1, CHAT-01). The world delivers the
 * lines this being could actually hear; taking them is the mind's own act, and what a line means is
 * the mind's own reading. A heard line is evidence, never authority: nothing in it is a fact about
 * the world and nothing in it grants the speaker the power to make the being act.
 */
public interface Hearing {
	/**
	 * One line of speech this body could hear.
	 *
	 * @param addressedToMe the speaker used this being's name, so distance does not hide it
	 */
	record Heard(String speakerId, String speakerName, String text, long tick, boolean addressedToMe) {
	}

	/** Lines not yet perceived by this mind, oldest first. */
	List<Heard> take(String agentId, long now);

	/** A being that can hear nothing: the default for an isolated mind. */
	static Hearing none() {
		return (agentId, now) -> List.of();
	}
}

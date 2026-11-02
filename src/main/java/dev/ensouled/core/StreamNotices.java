package dev.ensouled.core;

import dev.ensouled.EnsouledMod;
import dev.ensouled.surface.SurfaceStream;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A refusal a being made, published to that being's own stream.
 *
 * The being is the actor, so the row lands in its own feed and nowhere else. A repeated refusal is
 * one notice: the same agent, subject and code inside a quiet window is the same fact seen again,
 * and a stream that repeats it twenty times a second is a stream nobody can read.
 */
public final class StreamNotices {
	private static final Map<String, Long> SEEN = new ConcurrentHashMap<>();
	private static final long QUIET_MILLIS = 30_000;

	private StreamNotices() {
	}

	/**
	 * A decision that was recovered from the journal: it is not a refusal, so it is written as a
	 * committed row on the being's own stream, once.
	 */
	public static void recovered(String agent, String subject, String why) {
		SurfaceStream stream = SurfaceStream.get();
		if (stream == null || agent == null || agent.isBlank()) {
			return;
		}
		String key = agent + "|" + subject + "|recovered";
		long now = System.currentTimeMillis();
		Long last = SEEN.get(key);
		if (last != null) {
			return;
		}
		SEEN.put(key, now);
		try {
			stream.record(agent, subject, "recovered-interrupt", why);
		} catch (RuntimeException rejected) {
			EnsouledMod.LOG.warn("[ensouled] recovery row for {} could not be recorded: {}", agent,
					rejected.getMessage());
		}
	}

	/** The being's own answer to something put to it that it did not refuse. */
	public static void noted(String agent, String subject, String code, String why) {
		SurfaceStream stream = SurfaceStream.get();
		if (stream == null || agent == null || agent.isBlank()) {
			return;
		}
		try {
			stream.record(agent, subject, code, why);
		} catch (RuntimeException rejected) {
			EnsouledMod.LOG.warn("[ensouled] note {} for {} could not be recorded: {}", code, agent,
					rejected.getMessage());
		}
	}

	public static void refuse(String agent, String subject, String code, String why) {
		SurfaceStream stream = SurfaceStream.get();
		if (stream == null || agent == null || agent.isBlank()) {
			return;
		}
		String key = agent + "|" + subject + "|" + code;
		long now = System.currentTimeMillis();
		Long last = SEEN.get(key);
		if (last != null && now - last < QUIET_MILLIS) {
			return;
		}
		SEEN.put(key, now);
		try {
			stream.refuse(agent, subject, code, why);
		} catch (RuntimeException rejected) {
			// A refusal that cannot be recorded is logged, never promoted to a silence.
			EnsouledMod.LOG.warn("[ensouled] refusal {} for {} could not be recorded: {}", code, agent,
					rejected.getMessage());
		}
	}
}

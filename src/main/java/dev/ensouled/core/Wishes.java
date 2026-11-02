package dev.ensouled.core;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dev.ensouled.EnsouledMod;
import dev.ensouled.mind.AgentManager;
import dev.ensouled.mind.AgentRecord;
import dev.ensouled.surface.SurfaceStream;

/**
 * An operator's wish, addressed to one being.
 *
 * <p>There is no command that makes a being speak: words are the being's own, and a line typed by
 * an operator and emitted as that being is not that being talking. What an operator may do is put a
 * wish to it. The wish is delivered on the being's own stream as a toast (§47.4), and the being
 * answers it in its own way: it takes the wish up if it already holds that goal itself, and refuses
 * it otherwise, on the record, with its own reason. A wish never produces speech of the operator's
 * wording — the refusal path exists precisely so that "say this for me" cannot happen.
 *
 * <p>Pending wishes are per-run and lost with the server. What survives is the trail: the toast row
 * and the being's answer are both rows on its stream, which is written as they happen.
 */
public final class Wishes {
	/** A wish this being has not answered yet. */
	public record Wish(String id, String agentId, String text, long tick) {
	}

	private static final int MAX_PENDING_PER_AGENT = 8;
	private static final Map<String, Deque<Wish>> PENDING = new ConcurrentHashMap<>();
	/** Operators ask; a being is not a text-to-speech engine on tap. */
	private static final long MIN_SPACING_MILLIS = 2_000;
	private static final Map<String, Long> LAST_WISH = new ConcurrentHashMap<>();
	private static long sequence;

	private Wishes() {
	}

	/** Put a wish to the being with this name. Returns the wish, or null when it was not accepted. */
	public static Wish wish(String agentName, String text, long tick) {
		AgentManager manager = AgentManager.get();
		if (manager == null) {
			return null;
		}
		var found = manager.byName(agentName);
		if (found.isEmpty()) {
			return null;
		}
		AgentRecord record = found.get();
		String line = text == null ? "" : text.strip();
		if (line.isEmpty()) {
			return null;
		}
		long now = System.currentTimeMillis();
		Long last = LAST_WISH.get(record.agentId());
		if (last != null && now - last < MIN_SPACING_MILLIS) {
			return null;
		}
		LAST_WISH.put(record.agentId(), now);
		Wish wish = new Wish("wish:" + (++sequence), record.agentId(), line, tick);
		Deque<Wish> queue = PENDING.computeIfAbsent(record.agentId(), ignored -> new ArrayDeque<>());
		synchronized (queue) {
			if (queue.size() >= MAX_PENDING_PER_AGENT) {
				queue.removeFirst();
			}
			queue.addLast(wish);
		}
		// The wish is a toast on that being's own stream, with its words kept as words: the operator
		// asked, and the being is the one who will answer.
		SurfaceStream stream = SurfaceStream.get();
		if (stream != null) {
			try {
				stream.record(record.agentId(), "wish " + wish.id(), "wish-put",
						"asked, not dictated: " + line);
			} catch (RuntimeException rejected) {
				EnsouledMod.LOG.warn("[ensouled] wish {} for {} is not on the stream: {}", wish.id(),
						record.name(), rejected.getMessage());
			}
		}
		EnsouledMod.LOG.info("[ensouled] wish {} put to {}: {}", wish.id(), record.name(), line);
		return wish;
	}

	/** The oldest unanswered wish for this being, or null. Taking one consumes it. */
	public static Wish take(String agentId) {
		Deque<Wish> queue = PENDING.get(agentId);
		if (queue == null) {
			return null;
		}
		synchronized (queue) {
			return queue.pollFirst();
		}
	}

	public static int pending(String agentId) {
		Deque<Wish> queue = PENDING.get(agentId);
		if (queue == null) {
			return 0;
		}
		synchronized (queue) {
			return queue.size();
		}
	}
}

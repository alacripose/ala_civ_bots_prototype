package dev.ensouled.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ordinary player chat waiting to be perceived, one world-wide inbox per being (CHAT-01). It is
 * shared the same way the message bus is shared: one world, one ear, whoever is asking. A being
 * takes only its own lines, and a line that is never taken expires instead of being delivered late
 * or to someone else.
 */
public final class SpeechInbox implements Hearing {
	/** How long a spoken line stays hearable; long enough for a tick, short enough to be speech. */
	public static final int TTL_TICKS = 200;

	private static final SpeechInbox SHARED = new SpeechInbox();

	private final Map<String, Deque<Hearing.Heard>> pending = new LinkedHashMap<>();

	public static SpeechInbox shared() {
		return SHARED;
	}

	private SpeechInbox() {
	}

	/** Offer a line to one being; the being still decides whether to perceive it. */
	public void deliver(String agentId, Hearing.Heard line) {
		synchronized (pending) {
			pending.computeIfAbsent(agentId, ignored -> new ArrayDeque<>()).addLast(line);
		}
	}

	public int waiting(String agentId) {
		synchronized (pending) {
			Deque<Hearing.Heard> inbox = pending.get(agentId);
			return inbox == null ? 0 : inbox.size();
		}
	}

	@Override
	public List<Hearing.Heard> take(String agentId, long now) {
		synchronized (pending) {
			Deque<Hearing.Heard> inbox = pending.get(agentId);
			if (inbox == null) {
				return List.of();
			}
			List<Hearing.Heard> out = new ArrayList<>();
			while (!inbox.isEmpty()) {
				Hearing.Heard next = inbox.removeFirst();
				if (now - next.tick() <= TTL_TICKS) {
					out.add(next);
				}
			}
			return out;
		}
	}
}

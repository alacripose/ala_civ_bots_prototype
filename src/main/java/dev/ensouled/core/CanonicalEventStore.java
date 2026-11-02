package dev.ensouled.core;

import java.util.ArrayList;
import java.util.List;

/** The authoritative fact log the mind may cite. Append-only and bounded. */
public final class CanonicalEventStore {
	public static final int MAX_EVENTS = 500;

	private final ArrayList<CanonicalEvent> events = new ArrayList<>();
	private long sequence;

	public CanonicalEvent append(String type, long tick, String agentId, Vec3i where, String subjectId, String detail) {
		CanonicalEvent event = new CanonicalEvent("ce:" + (++sequence), type, tick, agentId, where, subjectId, detail);
		events.add(event);
		while (events.size() > MAX_EVENTS) {
			events.remove(0);
		}
		return event;
	}

	/** Duplicate guard: has this exact site already produced this event type? */
	public boolean exists(String type, String subjectId) {
		for (CanonicalEvent e : events) {
			if (e.type().equals(type) && e.subjectId().equals(subjectId)) {
				return true;
			}
		}
		return false;
	}

	public int count(String type) {
		int n = 0;
		for (CanonicalEvent e : events) {
			if (e.type().equals(type)) n++;
		}
		return n;
	}

	public List<CanonicalEvent> all() {
		return new ArrayList<>(events);
	}

	public void restore(List<CanonicalEvent> restored, long sequence) {
		events.clear();
		events.addAll(restored);
		while (events.size() > MAX_EVENTS) {
			events.remove(0);
		}
		this.sequence = sequence;
	}

	public long sequence() {
		return sequence;
	}

	public int size() {
		return events.size();
	}
}

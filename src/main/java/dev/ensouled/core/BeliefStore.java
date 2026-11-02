package dev.ensouled.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bounded belief store. Facts are reconciled from perceptions, never injected wholesale
 * from server state (§32 "Local knowledge"). Older, weaker beliefs are evicted first.
 */
public final class BeliefStore {
	public static final int MAX_BELIEFS = 512;

	private final LinkedHashMap<String, Belief> beliefs = new LinkedHashMap<>();

	public void ingest(Percept percept) {
		Percept p = percept.validate();
		Belief existing = beliefs.get(p.key());
		// A weaker newer claim does not overwrite a stronger recent one; both are kept as
		// evidence in the log by the caller, but the store keeps the best-supported stance.
		if (existing != null && existing.confidenceMilli() > p.confidenceMilli()
				&& existing.isFresh(p.tick(), 200)) {
			return;
		}
		beliefs.put(p.key(), new Belief(p.key(), p.value(), p.confidenceMilli(), p.source(),
				p.tick(), p.evidenceRef()));
		evictIfNeeded();
	}

	private void evictIfNeeded() {
		while (beliefs.size() > MAX_BELIEFS) {
			Iterator<Map.Entry<String, Belief>> it = beliefs.entrySet().iterator();
			it.next();
			it.remove();
		}
	}

	public Belief get(String key) {
		return beliefs.get(key);
	}

	public String valueOf(String key) {
		Belief b = beliefs.get(key);
		return b == null ? null : b.value();
	}

	public boolean hasFresh(String key, long nowTick, int maxAgeTicks) {
		Belief b = beliefs.get(key);
		return b != null && b.isFresh(nowTick, maxAgeTicks);
	}

	public List<Belief> snapshot() {
		return new ArrayList<>(beliefs.values());
	}

	public void restore(List<Belief> restored) {
		beliefs.clear();
		for (Belief b : restored) {
			beliefs.put(b.key(), b);
		}
		evictIfNeeded();
	}

	public int size() {
		return beliefs.size();
	}
}

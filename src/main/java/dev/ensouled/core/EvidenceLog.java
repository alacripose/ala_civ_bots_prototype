package dev.ensouled.core;

import java.util.ArrayList;
import java.util.List;

/** Durable decision/action/verification trail used to audit behavior after a restart. */
public final class EvidenceLog {
	public static final int MAX_RECORDS = 1000;

	private final ArrayList<EvidenceRecord> records = new ArrayList<>();
	private long sequence;

	public EvidenceRecord append(long tick, String agentId, String intentionId, EvidenceKind kind,
			String actionHash, String outcome, String detail) {
		EvidenceRecord record = new EvidenceRecord("ev:" + (++sequence), tick, agentId, intentionId,
				kind, actionHash, outcome, detail);
		records.add(record);
		while (records.size() > MAX_RECORDS) {
			records.remove(0);
		}
		return record;
	}

	public List<EvidenceRecord> recent(int limit) {
		int from = Math.max(0, records.size() - limit);
		return new ArrayList<>(records.subList(from, records.size()));
	}

	public List<EvidenceRecord> all() {
		return new ArrayList<>(records);
	}

	public List<EvidenceRecord> byKind(EvidenceKind kind) {
		List<EvidenceRecord> out = new ArrayList<>();
		for (EvidenceRecord r : records) {
			if (r.kind() == kind) out.add(r);
		}
		return out;
	}

	public void restore(List<EvidenceRecord> restored, long sequence) {
		records.clear();
		records.addAll(restored);
		while (records.size() > MAX_RECORDS) {
			records.remove(0);
		}
		this.sequence = sequence;
	}

	public long sequence() {
		return sequence;
	}

	public int size() {
		return records.size();
	}
}

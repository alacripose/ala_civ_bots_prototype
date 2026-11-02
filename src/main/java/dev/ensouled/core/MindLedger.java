package dev.ensouled.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Everything one being remembers and can cite: episodic memory, the canonical event log, the
 * evidence trail and the practice log it has actually performed. The mind decides; the ledger
 * records and answers history questions, so neither class has to carry the other's bookkeeping
 * rules.
 */
public final class MindLedger {
	/** Radius within which a remembered collection covers the same physical tree. */
	public static final double SAME_SITE_RADIUS = 3.0;

	private final EpisodicMemory memory = new EpisodicMemory();
	private final CanonicalEventStore events = new CanonicalEventStore();
	private final EvidenceLog evidence = new EvidenceLog();
	/** Every verified practice of a skill, in order: the record a skill is a view over (Q572). */
	private final java.util.List<Practice> practice = new java.util.ArrayList<>();
	/**
	 * The derived view, memoised by verb: one verb is one learned skill however many routines speak
	 * it. Rebuilt from {@link #practice} on load rather than saved beside it, so no counter can
	 * disagree with the work it claims to summarise.
	 */
	private final Map<String, SkillStats> stats = new LinkedHashMap<>();

	private long episodeSequence;
	private int harvestCount;

	public EvidenceRecord note(long tick, String agentId, String intentionId, EvidenceKind kind,
			String actionHash, String outcome, String detail) {
		return evidence.append(tick, agentId, intentionId, kind, actionHash, outcome, detail);
	}

	public Episode remember(long tick, EpisodeKind kind, String summary, Vec3i where,
			String provenance, String evidenceRef) {
		Episode episode = new Episode("ep:" + (++episodeSequence), tick, kind, summary, where, provenance,
				evidenceRef);
		memory.add(episode);
		return episode;
	}

	/**
	 * Records a verified collection once per site. Returns false when the site already produced a
	 * canonical event, so repeating an action cannot inflate history.
	 */
	public boolean recordCollection(long tick, String agentId, String dimension, Vec3i target, String blockId) {
		String site = BlockIds.siteKey(dimension, target);
		if (events.exists(CanonicalEvent.RESOURCE_COLLECTED, site)) {
			return false;
		}
		events.append(CanonicalEvent.RESOURCE_COLLECTED, tick, agentId, target, site, blockId);
		harvestCount++;
		remember(tick, EpisodeKind.RESOURCE_COLLECTED, "harvested " + blockId + " at " + target, target,
				"self_performed", "ce:" + events.sequence());
		return true;
	}

	/**
	 * An item actually changed hands, keyed by the transfer id the two agents agreed on. Recorded by
	 * the recipient from its own inventory delta, so the same bread cannot be counted twice.
	 */
	public boolean recordTransfer(long tick, String agentId, Vec3i where, String transferId, String itemId,
			int count) {
		if (events.exists(CanonicalEvent.ITEM_TRANSFERRED, transferId)) {
			return false;
		}
		events.append(CanonicalEvent.ITEM_TRANSFERRED, tick, agentId, where, transferId,
				itemId + " x" + count);
		return true;
	}

	/**
	 * An item actually crossed between this being's inventory and a world container, recorded once per
	 * transfer id and operation so a retry cannot inflate the history.
	 */
	public boolean recordContainerMove(long tick, String agentId, Vec3i where, String operation,
			String transferId, String itemId, int count) {
		String type = "withdraw".equals(operation) ? CanonicalEvent.CONTAINER_WITHDRAWN
				: CanonicalEvent.CONTAINER_DEPOSITED;
		String subject = transferId + ":" + operation;
		if (events.exists(type, subject)) {
			return false;
		}
		events.append(type, tick, agentId, where, subject, itemId + " x" + count);
		return true;
	}

	/**
	 * A felled tree is remembered as a place, not only as its lowest block: the trunk base is often
	 * occluded from the body, so judging by exact coordinates alone would re-harvest the upper logs of
	 * a tree this being already took. {@code site} is the tree base used for the exact match;
	 * {@code pos} is what the body currently sees and anchors the proximity check.
	 */
	public boolean wasCollectedAt(String dimension, Vec3i site, Vec3i pos, double radius) {
		if (events.exists(CanonicalEvent.RESOURCE_COLLECTED, BlockIds.siteKey(dimension, site))
				|| memory.hasCollectedAt(site)) {
			return true;
		}
		String dimensionPrefix = dimension + "@";
		for (CanonicalEvent event : events.all()) {
			if (event.type().equals(CanonicalEvent.RESOURCE_COLLECTED)
					&& event.subjectId().startsWith(dimensionPrefix)
					&& event.where().distanceTo(pos) <= radius) {
				return true;
			}
		}
		for (Episode episode : memory.all()) {
			if (episode.kind() == EpisodeKind.RESOURCE_COLLECTED
					&& episode.where().distanceTo(pos) <= radius) {
				return true;
			}
		}
		return false;
	}

	/** One verified practice: which routine was used, which corpus verb it speaks. */
	public record Practice(long tick, String skillId, String verb, boolean success,
			String evidence) {
	}

	/**
	 * The verb is the learned unit (Q572): two routines that speak the same attested verb build
	 * one skill, and a routine the corpus gives no verb for is learned under its own id and
	 * reported as unattested rather than given an invented name.
	 */
	public static String learnedKey(String skillId) {
		String verb = VerbLibrary.corpus().spokenVerb(skillId);
		return verb == null || verb.isBlank() ? String.valueOf(skillId) : verb;
	}

	public SkillStats stats(String skillId) {
		return stats.computeIfAbsent(learnedKey(skillId), ignored -> new SkillStats());
	}

	/**
	 * Record one verified outcome. The counters move only because a practice row was appended; there
	 * is no path that changes a count without naming the work that produced it.
	 */
	public Practice practiced(String skillId, long tick, boolean success, String evidence) {
		Practice row = new Practice(tick, skillId, learnedKey(skillId), success,
				evidence == null ? "" : evidence);
		practice.add(row);
		apply(row);
		return row;
	}

	public java.util.List<Practice> practice() {
		return java.util.List.copyOf(practice);
	}

	/**
	 * Replay the record, not the counters. A save carries practice rows only, so the numbers a
	 * decision reads are always a function of the work that actually happened. The verb stored in
	 * the row wins over a fresh lookup: a row says what was learned when it was learned.
	 */
	public void restorePractice(java.util.List<Practice> rows) {
		practice.clear();
		stats.clear();
		for (Practice row : rows) {
			practice.add(row);
			apply(row);
		}
	}

	/** The learned skills, keyed by the verb the corpus attests: the civilian-doc view. */
	public Map<String, SkillStats> statsBySkill() {
		return Collections.unmodifiableMap(stats);
	}

	private void apply(Practice row) {
		String key = row.verb() == null || row.verb().isBlank() ? learnedKey(row.skillId()) : row.verb();
		SkillStats stats = this.stats.computeIfAbsent(key, ignored -> new SkillStats());
		if (row.success()) {
			stats.recordSuccess();
		} else {
			stats.recordFailure();
		}
	}

	public EpisodicMemory memory() {
		return memory;
	}

	public CanonicalEventStore events() {
		return events;
	}

	public EvidenceLog evidence() {
		return evidence;
	}

	public int harvestCount() {
		return harvestCount;
	}

	public long episodeSequence() {
		return episodeSequence;
	}

	public void restoreEpisodes(List<Episode> episodes, long sequence) {
		memory.restore(episodes);
		episodeSequence = sequence;
	}

	public void restoreEvents(List<CanonicalEvent> restored, long sequence) {
		events.restore(restored, sequence);
	}

	public void restoreEvidence(List<EvidenceRecord> restored, long sequence) {
		evidence.restore(restored, sequence);
	}

	public void restoreHarvestCount(int count) {
		harvestCount = count;
	}
}

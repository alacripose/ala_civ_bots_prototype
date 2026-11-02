package dev.ensouled.core;

import java.util.ArrayList;
import java.util.List;

/** Bounded personal history. Only what this being perceived or performed enters here. */
public final class EpisodicMemory {
	public static final int MAX_EPISODES = 256;

	private final ArrayList<Episode> episodes = new ArrayList<>();

	public void add(Episode episode) {
		episodes.add(episode);
		while (episodes.size() > MAX_EPISODES) {
			episodes.remove(0);
		}
	}

	public List<Episode> recent(int limit) {
		int from = Math.max(0, episodes.size() - limit);
		return new ArrayList<>(episodes.subList(from, episodes.size()));
	}

	public List<Episode> all() {
		return new ArrayList<>(episodes);
	}

	/** Has this being already recorded a completed collection at this block site? */
	public boolean hasCollectedAt(Vec3i where) {
		for (Episode e : episodes) {
			if (e.kind() == EpisodeKind.RESOURCE_COLLECTED && where.equals(e.where())) {
				return true;
			}
		}
		return false;
	}

	public void restore(List<Episode> restored) {
		episodes.clear();
		episodes.addAll(restored);
		while (episodes.size() > MAX_EPISODES) {
			episodes.remove(0);
		}
	}

	public int size() {
		return episodes.size();
	}
}

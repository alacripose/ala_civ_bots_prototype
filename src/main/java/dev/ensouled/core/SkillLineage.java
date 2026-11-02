package dev.ensouled.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The lineage of a being's abilities, read from the work that produced them.
 *
 * <p>This follows the design analysis's rule that research and unlocks are persistent state and
 * that a reward must have an intelligible provenance (§4.7, §5.3, §7.6). An ability here is not a
 * row in a skill table: it is a corpus-attested verb, unlocked by the first practice the being
 * actually performed, witnessed by the one-cell discovery ticket opened from that practice, and
 * carried forward by the practice log when the body is restored. A verb with no practice has no
 * node; a node with no ticket says so rather than inventing an unlock.
 *
 * <p>The view is derived on demand. Nothing here is saved beside the practice rows it summarises,
 * so lineage cannot disagree with the work it claims — the same rule as {@link SkillStats}.
 */
public final class SkillLineage {
	/** The part of a discovery ticket a lineage node cites, without making core depend on work. */
	public record Discovery(String ticketId, String state, String witness) {
		public static Discovery none() {
			return new Discovery("", "", "");
		}
	}

	/**
	 * One learned ability. {@code firstSkill} is the routine that first spoke this verb, so a
	 * reader can follow the node back to the exact practice row and ticket; {@code firstTick} is
	 * the unlock, {@code lastTick} the latest reinforcement.
	 */
	public record Node(String verb, String basis, String firstSkill, long firstTick, long lastTick,
			int successes, int failures, Discovery discovery) {
		public int habitStrength() {
			return SkillStats.habitStrength(successes, failures);
		}

		public boolean witnessed() {
			return discovery != null && !discovery.ticketId().isBlank();
		}
	}

	private final int sessions;
	private final List<Node> nodes;

	private SkillLineage(int sessions, List<Node> nodes) {
		this.sessions = sessions;
		this.nodes = nodes;
	}

	/**
	 * Build the lineage from practice rows, discovery tickets keyed by the skill id that opened
	 * them, and the number of sessions this life has been booted for. The stored verb in a row wins
	 * over a fresh lookup: a node says what was learned when it was learned.
	 */
	public static SkillLineage of(List<MindLedger.Practice> practice,
			Map<String, Discovery> discoveries, int sessions) {
		Map<String, Accumulator> byVerb = new LinkedHashMap<>();
		for (MindLedger.Practice row : practice) {
			String learned = row.verb();
			if (learned == null || learned.isBlank()) {
				learned = MindLedger.learnedKey(row.skillId());
			}
			String verb = learned;
			byVerb.computeIfAbsent(verb, ignored -> new Accumulator(verb, row.skillId(),
					row.tick())).add(row, discoveries);
		}
		List<Node> nodes = new ArrayList<>();
		for (Accumulator accumulator : byVerb.values()) {
			nodes.add(accumulator.node());
		}
		return new SkillLineage(Math.max(0, sessions), List.copyOf(nodes));
	}

	public int sessions() {
		return sessions;
	}

	public List<Node> nodes() {
		return nodes;
	}

	public boolean isEmpty() {
		return nodes.isEmpty();
	}

	/** What a reader is shown: one line per learned ability, and nothing when nothing was learned. */
	public List<String> lines() {
		List<String> out = new ArrayList<>();
		for (Node node : nodes) {
			Discovery discovery = node.discovery() == null ? Discovery.none() : node.discovery();
			out.add(node.verb() + " " + node.basis() + " first=" + node.firstTick()
					+ " last=" + node.lastTick() + " practice=" + node.successes() + "/"
					+ node.failures() + " habit=" + node.habitStrength()
					+ " ticket=" + (discovery.ticketId().isBlank() ? "-" : discovery.ticketId())
					+ (discovery.state().isBlank() ? "" : " " + discovery.state())
					+ (discovery.witness().isBlank() ? "" : " witness=" + discovery.witness()));
		}
		return out;
	}

	private static final class Accumulator {
		private final String verb;
		private final String firstSkill;
		private final long firstTick;
		private long lastTick;
		private int successes;
		private int failures;
		private Discovery discovery;

		private Accumulator(String verb, String firstSkill, long firstTick) {
			this.verb = verb;
			this.firstSkill = firstSkill;
			this.firstTick = firstTick;
			this.lastTick = firstTick;
		}

		private void add(MindLedger.Practice row, Map<String, Discovery> discoveries) {
			lastTick = Math.max(lastTick, row.tick());
			if (row.success()) {
				successes++;
			} else {
				failures++;
			}
			if (discovery == null && discoveries != null) {
				discovery = discoveries.get(row.skillId());
			}
		}

		private Node node() {
			return new Node(verb, VerbLibrary.corpus().basis(firstSkill), firstSkill, firstTick,
					lastTick, successes, failures, discovery == null ? Discovery.none() : discovery);
		}
	}
}

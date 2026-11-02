package dev.ensouled.axis;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.ensouled.surface.SurfaceDocument;

/**
 * The civilians' axis gate: the four checks the `axis` crate makes before a decision may become a
 * commitment, with the crate's refusal codes.
 *
 * <ul>
 *   <li><b>Selection</b> — a choice must cite a need, carry an estimate, name one atomic task, have
 *       targets, sit on a live plan, and spend inside its budget ({@code EmptyNeed},
 *       {@code MissingEstimate}, {@code NoAtomicTask}, {@code NoTargets}, {@code PlanNotLive},
 *       {@code BudgetExceeded}).</li>
 *   <li><b>Task</b> — a task needs a kind; make/gather needs a process; build needs planned
 *       material; and no task may complete by declaration ({@code CompletionByDeclaration}). An
 *       unclassified check command is refused rather than defaulted ({@code UnclassifiedCheck}).</li>
 *   <li><b>Reading</b> — every reading carries a <em>qualified</em> token from
 *       {@link AxisVocabulary}, a non-empty basis and a witness. A reading with no witness is
 *       {@code unchecked}: a state, never a pass. Absent work is an honest {@code absent}, also not
 *       a refusal.</li>
 *   <li><b>Arrangement</b> — panes must be non-empty and distinct; a refused write leaves the
 *       running arrangement untouched.</li>
 * </ul>
 *
 * <p>Thousands of decisions do not become thousands of rows. {@link Cells} accumulates one being's
 * readings and closes a package at a decision or tick boundary, chaining each package to the
 * previous one by digest; the package carries counts, the coverage table, the refusals and the
 * evidence sequence it was read at, so the stream carries a horizon of decisions as one record.
 */
public final class AxisGate {
	public static final String FORMAT = "civilian.axis-cells/1";
	/** Decisions in one package, and the tick horizon it closes at, whichever comes first. */
	public static final int DECISION_BATCH = 200;
	public static final int TICK_BATCH = 1200;

	/** A refusal carries the crate's code, so the ledger row and the toast say the same thing. */
	public record Refusal(String code, String why) {
	}

	/**
	 * A decision as the axis records it: the qualified token, the basis for it, and the witness a
	 * reader can open. {@code witness == null} is {@code unchecked}, and unchecked is not a pass.
	 */
	public record Decision(String subject, boolean admitted, String token, String basis,
			String witness, String code, String why) {

		public boolean checked() {
			return witness != null && !witness.isBlank();
		}

		/** A reading is only a reading if its token is qualified for the ladder it is filed under. */
		public Refusal tokenRefusal(AxisVocabulary.Kind kind) {
			return AxisVocabulary.check(kind, token);
		}
	}

	/**
	 * A selection as the crate validates it. {@code estimateMilli} is null when the selector
	 * supplied no estimate; {@code maximumCells} below zero means no budget was declared, and an
	 * undeclared budget cannot be exceeded.
	 */
	public record Selection(String need, Integer estimateMilli, String task, boolean hasTargets,
			boolean planLive, long requestedCells, long maximumCells) {

		public Refusal verify() {
			if (need == null || need.isBlank()) {
				return new Refusal("EmptyNeed", "the selection cites no need");
			}
			if (estimateMilli == null) {
				return new Refusal("MissingEstimate", "the selection carries no estimate");
			}
			if (task == null || task.isBlank()) {
				return new Refusal("NoAtomicTask", "the selection names no atomic task");
			}
			if (!hasTargets) {
				return new Refusal("NoTargets", "the selection has no targets");
			}
			if (!planLive) {
				return new Refusal("PlanNotLive", "the plan behind the selection is not live");
			}
			if (maximumCells >= 0 && requestedCells > maximumCells) {
				return new Refusal("BudgetExceeded",
						"asks for " + requestedCells + " of " + maximumCells + " cells");
			}
			return null;
		}
	}

	/** A selection that passed, or the refusal that stopped it. */
	public static Decision select(String subject, Selection selection, String witness) {
		Refusal refusal = selection.verify();
		if (refusal != null) {
			return new Decision(subject, false, AxisVocabulary.Kind.PROCEDURAL_GATE.qualify("FAIL"),
					refusal.why(), witness, refusal.code(), refusal.why());
		}
		String basis = "need, estimate, task, targets, live plan"
				+ (selection.maximumCells() >= 0
						? ", " + selection.requestedCells() + "/" + selection.maximumCells() + " cells"
						: ", no budget declared");
		return new Decision(subject, true, AxisVocabulary.Kind.PROCEDURAL_GATE.qualify("PASS"), basis,
				witness, "selection-held", basis);
	}

	/**
	 * Task validity. {@code declaredOnly} is true when the only completion on offer is the
	 * routine's own word: that is {@code CompletionByDeclaration}, and the crate refuses it. An
	 * empty kind is {@code NoAtomicTask}, never a default.
	 */
	public static Decision task(String subject, String kind, String process, long plannedMaterial,
			long requiredMaterial, boolean declaredOnly, String witness) {
		Refusal refusal = null;
		if (kind == null || kind.isBlank()) {
			refusal = new Refusal("NoAtomicTask", "the task has no kind");
		} else if (("make".equals(kind) || "gather".equals(kind))
				&& (process == null || process.isBlank())) {
			refusal = new Refusal("MissingProcess", kind + " needs a process");
		} else if ("build".equals(kind)) {
			if (plannedMaterial <= 0) {
				refusal = new Refusal("MissingMaterial", "build has no planned material");
			} else if (requiredMaterial > 0 && plannedMaterial < requiredMaterial) {
				refusal = new Refusal("MaterialShortfall",
						"planned " + plannedMaterial + " of " + requiredMaterial);
			}
		}
		if (refusal == null && declaredOnly) {
			refusal = new Refusal("CompletionByDeclaration",
					"the completion is a declaration, not an observation");
		}
		if (refusal != null) {
			return new Decision(subject, false, AxisVocabulary.Kind.PROCEDURAL_GATE.qualify("FAIL"),
					refusal.why(), witness, refusal.code(), refusal.why());
		}
		return new Decision(subject, true, AxisVocabulary.Kind.PROCEDURAL_GATE.qualify("PASS"),
				kind + " is well formed", witness, "task-held", kind + " is well formed");
	}

	/**
	 * The class of a check command, or a refusal when it does not place itself. A rendering word
	 * wins over the verb, and {@code cargo test} is CPU whatever the test is called.
	 */
	public static String classify(String check) {
		if (check == null || check.isBlank()) {
			return null;
		}
		String trimmed = check.trim();
		for (String word : new String[] { "docs/baselines", "capture", "frame-cost", "ladder-view",
				"screenshot" }) {
			if (trimmed.contains(word)) {
				return "rendering";
			}
		}
		String[] words = trimmed.split("\\s+");
		String verb = words[0];
		if ("cargo".equals(verb) || verb.startsWith("cargo-")) {
			String subcommand = words.length > 1 ? words[1] : "";
			if ("run".equals(subcommand)) {
				return trimmed.contains("shploingus") ? "rendering" : "cpu";
			}
			return "cpu";
		}
		if ("rustc".equals(verb) || "git".equals(verb) || verb.startsWith("./gradlew")) {
			return "cpu";
		}
		return null;
	}

	public enum Ladder {
		HELD,
		ABSENT,
		UNCHECKED
	}

	/**
	 * Read a witness. No witness is {@code UNCHECKED} — a state, never a pass. A witness that cannot
	 * be parsed is {@code UnreadableLadder}; no work on record is an honest {@code ABSENT}.
	 */
	public static Ladder ladder(String witness, boolean workOnRecord) {
		if (witness == null || witness.isBlank()) {
			return Ladder.UNCHECKED;
		}
		if (witness.startsWith("!")) {
			return Ladder.UNCHECKED;
		}
		return workOnRecord ? Ladder.HELD : Ladder.ABSENT;
	}

	/** The crate's establishment ladder, strongest first. A reading is ranked, never scored. */
	public static final List<String> ESTABLISHMENT_ORDER = List.of("active", "stale", "unchecked",
			"refused");

	/**
	 * One candidate's reading: what the gate decided about it, and where the selection stands on the
	 * establishment ladder. The rank is what selection sorts by, so a reading decides which action is
	 * chosen rather than being stamped on the action already chosen.
	 */
	public record Reading(String subject, Decision decision, String establishment) {
		public int rank() {
			return AxisGate.rank(establishment);
		}

		public boolean admitted() {
			return decision.admitted();
		}

		/** The qualified token, as any ladder value is written. */
		public String token() {
			return AxisVocabulary.Kind.ESTABLISHMENT.qualify(establishment);
		}
	}

	/** Stronger readings rank higher; an unknown rung ranks with the weakest. */
	public static int rank(String establishment) {
		int index = ESTABLISHMENT_ORDER.indexOf(establishment);
		return index < 0 ? 0 : ESTABLISHMENT_ORDER.size() - index;
	}

	/**
	 * Read a selection <em>before</em> anything is ranked. A selection that fails its own checks is
	 * {@code refused} and cannot be chosen at all; one that stands is {@code active} unless the
	 * authority has declared another rung for it, which is how a weaker reading is put on record and
	 * made to change the choice.
	 */
	public static Reading read(String subject, Selection selection, String witness, String declared) {
		Decision decision = select(subject, selection, witness);
		String establishment;
		if (!decision.admitted()) {
			establishment = "refused";
		} else if (declared != null && ESTABLISHMENT_ORDER.contains(declared.trim())) {
			establishment = declared.trim();
		} else {
			establishment = "active";
		}
		return new Reading(subject, decision, establishment);
	}

	/** Arrangement validity: every window has content and no window repeats. */
	public static Refusal arrangement(List<String> windows) {
		if (windows == null || windows.isEmpty()) {
			return new Refusal("EmptyArrangement", "the arrangement has no windows");
		}
		java.util.Set<String> seen = new java.util.LinkedHashSet<>();
		for (String window : windows) {
			if (window == null || window.isBlank()) {
				return new Refusal("EmptyWindow", "a window has no content");
			}
			if (!seen.add(window)) {
				return new Refusal("DuplicateWindow", "window repeated: " + window);
			}
		}
		return null;
	}

	/** One being's readings, closed into a chained package at a boundary. */
	public static final class Cells {
		private final Map<String, Integer> tokens = new LinkedHashMap<>();
		private final Map<String, Integer> refusals = new LinkedHashMap<>();
		private int decisions;
		private int admitted;
		private int refused;
		private int unchecked;
		private int absent;
		private long openedTick = -1;
		private long closedTick;
		private long lastEvidenceSeq;
		private String previousDigest = "genesis";

		public synchronized void record(Decision decision, AxisVocabulary.Kind kind, long tick) {
			if (openedTick < 0) {
				openedTick = tick;
			}
			decisions++;
			Refusal bare = decision.tokenRefusal(kind);
			if (bare != null) {
				// A bare or misfiled token is refused here rather than counted: it is a bug in the
				// producer, not a reading.
				refused++;
				refusals.merge(decision.subject() + ":" + bare.code(), 1, Integer::sum);
				closedTick = tick;
				return;
			}
			tokens.merge(decision.token(), 1, Integer::sum);
			if (decision.admitted()) {
				admitted++;
			} else {
				refused++;
				refusals.merge(decision.subject() + ":" + decision.code(), 1, Integer::sum);
			}
			if (!decision.checked()) {
				unchecked++;
			}
			closedTick = tick;
		}

		/** An honest zero is a reading too: no work on record is counted, never refused. */
		public synchronized void absent(String subject, long tick) {
			if (openedTick < 0) {
				openedTick = tick;
			}
			decisions++;
			absent++;
			tokens.merge(AxisVocabulary.Kind.PROCEDURAL_GATE.qualify("NOT_APPLICABLE"), 1, Integer::sum);
			closedTick = tick;
		}

		public synchronized boolean due(long tick, long evidenceSeq) {
			if (decisions == 0) {
				return false;
			}
			return decisions >= DECISION_BATCH || tick - openedTick >= TICK_BATCH;
		}

		/**
		 * Close the package: the digest covers what was read, the evidence sequence it was read at,
		 * and the previous package's digest, so a package cannot be replayed or inserted.
		 */
		public synchronized JsonObject close(long tick, long evidenceSeq, String agent) {
			JsonObject tokensJson = new JsonObject();
			for (Map.Entry<String, Integer> entry : tokens.entrySet()) {
				// Every key written here is already qualified by the ladder it came from.
				tokensJson.addProperty(entry.getKey(), entry.getValue());
			}
			JsonObject refusalsJson = new JsonObject();
			for (Map.Entry<String, Integer> entry : refusals.entrySet()) {
				refusalsJson.addProperty(entry.getKey(), entry.getValue());
			}
			JsonArray coverage = new JsonArray();
			for (AxisVocabulary.CoverageRow row : AxisVocabulary.coverage()) {
				JsonObject rowJson = new JsonObject();
				rowJson.addProperty("axis", row.kind().slug());
				JsonArray values = new JsonArray();
				row.values().forEach(values::add);
				rowJson.add("values", values);
				coverage.add(rowJson);
			}
			JsonObject pkg = new JsonObject();
			pkg.addProperty("format", FORMAT);
			pkg.addProperty("agent", agent);
			pkg.addProperty("opened_tick", openedTick);
			pkg.addProperty("closed_tick", tick);
			pkg.addProperty("decisions", decisions);
			pkg.addProperty("admitted", admitted);
			pkg.addProperty("refused", refused);
			pkg.addProperty("unchecked", unchecked);
			pkg.addProperty("absent", absent);
			pkg.add("tokens", tokensJson);
			pkg.add("refusals", refusalsJson);
			pkg.addProperty("axis_count", AxisVocabulary.AXIS_COUNT);
			pkg.add("coverage", coverage);
			pkg.addProperty("evidence_seq", evidenceSeq);
			pkg.addProperty("prev", previousDigest);
			String canonical = pkg.toString();
			String digest = SurfaceDocument.sha256(canonical);
			pkg.addProperty("digest", digest);
			previousDigest = digest;
			lastEvidenceSeq = evidenceSeq;
			decisions = 0;
			admitted = 0;
			refused = 0;
			unchecked = 0;
			absent = 0;
			tokens.clear();
			refusals.clear();
			openedTick = -1;
			return pkg;
		}

		public synchronized int decisions() {
			return decisions;
		}

		public synchronized String previousDigest() {
			return previousDigest;
		}
	}

	private AxisGate() {
	}
}

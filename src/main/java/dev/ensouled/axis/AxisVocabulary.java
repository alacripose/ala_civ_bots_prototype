package dev.ensouled.axis;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The axis vocabulary, ported from `voxel-model/src/axis.rs` (the crate the `axis` layer
 * re-exports).
 *
 * <p>Nine ladders, each its own type with its own tokens. The three rules this class exists to
 * enforce, in the crate's own words:
 *
 * <ul>
 *   <li><b>A bare ladder token is a bug.</b> {@code "A"} is an evidence grade, a unit grade and a
 *       ticket origin at once; {@code bareToken} names every ladder that claims it, so a value is
 *       always written {@code evidence_grade A} or {@code grade A}.</li>
 *   <li><b>Validity is not a quantity.</b> A ladder never renders as a percentage or a score, and
 *       a certification tier is never {@code max(evidence_grade)}: {@code [A, D, D, D]} would
 *       certify a body of work on one strong claim while three components stay unresolved.</li>
 *   <li><b>Coverage is total or it is refused.</b> A coverage row that does not describe its
 *       ladder is worse than no row, because it looks checked.</li>
 * </ul>
 */
public final class AxisVocabulary {
	/** How many ladders this build knows. Written down, and checked against the table. */
	public static final int AXIS_COUNT = 9;

	/** One ladder: its recorded name and its tokens in the source's order. */
	public enum Kind {
		EVIDENCE_GRADE("evidence_grade", new String[] { "A", "B", "C", "D" }),
		CERTIFICATION_TIER("certification_tier",
				new String[] { "bronze", "silver", "gold", "diamond", "platinum" }),
		PROCEDURAL_GATE("procedural_gate",
				new String[] { "PASS", "FAIL", "NOT_APPLICABLE", "EXPIRED", "BLOCKED" }),
		GRADE("grade", new String[] { "F", "E", "D", "C", "B", "A", "S", "SS", "SSS" }),
		ORG_RANK("org_rank", new String[] { "applicant", "associate", "member", "senior_member",
				"lead", "manager", "senior_manager", "director", "senior_director",
				"vice_president", "senior_vice_president", "chief_operating_officer", "president",
				"chief_executive_officer" }),
		SUPPORT_LEVEL("support_level", new String[] { "direct", "strongly_supported", "supported",
				"inferred", "self_reported", "unverified" }),
		TICKET_EVIDENCE("ticket_evidence", new String[] { "A", "B", "C", "D" }),
		PROSE_KIND("prose_kind", new String[] { "definition", "obligation", "prohibition",
				"permission", "fact", "principle", "condition", "procedure" }),
		ESTABLISHMENT("establishment",
				new String[] { "active", "inactive", "stale", "unchecked", "refused" });

		private final String slug;
		private final String[] tokens;

		Kind(String slug, String[] tokens) {
			this.slug = slug;
			this.tokens = tokens;
		}

		public String slug() {
			return slug;
		}

		public List<String> tokens() {
			return List.of(tokens);
		}

		/** The qualified form the crate requires: {@code grade A}, never {@code A}. */
		public String qualify(String token) {
			return slug + " " + token;
		}
	}

	/** Ladders whose tokens are recorded bare, and so can collide. */
	private static final List<Kind> BARE_CLAIMANTS = List.of(Kind.EVIDENCE_GRADE,
			Kind.CERTIFICATION_TIER, Kind.GRADE, Kind.ORG_RANK, Kind.TICKET_EVIDENCE);

	/** Every ladder that claims this token, in the crate's order. Many-valued on purpose. */
	public static List<Kind> bareTokenAxes(String token) {
		String trimmed = token == null ? "" : token.trim();
		List<Kind> claimants = new ArrayList<>();
		for (Kind kind : BARE_CLAIMANTS) {
			if (kind.tokens().contains(trimmed)) {
				claimants.add(kind);
			}
		}
		return claimants;
	}

	/**
	 * Refuse a bare ladder token: {@code UNQUALIFIED} when one ladder owns it, {@code AMBIGUOUS}
	 * when several do. Anything that is not a ladder token at all is fine.
	 */
	public static AxisGate.Refusal refuseBare(String token) {
		List<Kind> claimants = bareTokenAxes(token);
		if (claimants.isEmpty()) {
			return null;
		}
		String trimmed = token.trim();
		if (claimants.size() == 1) {
			Kind only = claimants.get(0);
			return new AxisGate.Refusal("BareTokenRefused",
					"bare ladder token \"" + trimmed + "\" is UNQUALIFIED — it belongs to "
							+ only.slug() + " and to no other ladder, but a bare ladder token is a bug. Write '"
							+ only.slug() + " " + trimmed + "'");
		}
		StringBuilder names = new StringBuilder();
		for (Kind kind : claimants) {
			if (names.length() > 0) {
				names.append(" or ");
			}
			names.append(kind.slug());
		}
		return new AxisGate.Refusal("BareTokenRefused", "bare ladder token \"" + trimmed
				+ "\" is AMBIGUOUS — claimed by " + names + "; write the ladder with the token, e.g. '"
				+ claimants.get(0).slug() + " " + trimmed + "'");
	}

	/**
	 * Refuse a token that does not belong to the ladder it is filed under, or that is bare, or
	 * that is qualified for a different ladder.
	 */
	public static AxisGate.Refusal check(Kind kind, String token) {
		if (token == null || token.isBlank()) {
			return new AxisGate.Refusal("BareTokenRefused", kind.slug() + ": no token");
		}
		String trimmed = token.trim();
		int space = trimmed.indexOf(' ');
		if (space < 0) {
			return refuseBare(trimmed);
		}
		String ladder = trimmed.substring(0, space);
		String value = trimmed.substring(space + 1);
		Kind filed = bySlug(ladder);
		if (filed == null) {
			return new AxisGate.Refusal("UncoveredAxis",
					"\"" + trimmed + "\" names no ladder this build knows");
		}
		if (filed != kind) {
			return new AxisGate.Refusal("AxisInterchangeRefused",
					trimmed + " is filed under " + kind.slug() + "; comparing across ladders is what"
							+ " makes a bare token mean two things, and neither is a number");
		}
		if (!kind.tokens().contains(value)) {
			return new AxisGate.Refusal("UncoveredAxis",
					kind.slug() + ": \"" + value + "\" is not one of its members");
		}
		return null;
	}

	public static Kind bySlug(String slug) {
		for (Kind kind : Kind.values()) {
			if (kind.slug().equals(slug)) {
				return kind;
			}
		}
		return null;
	}

	/** Why a ladder may not be reduced to a number. */
	public enum Reduction {
		PERCENTAGE("as_percentage"),
		SCORE("as_score"),
		MAXIMUM("maximum");

		private final String slug;

		Reduction(String slug) {
			this.slug = slug;
		}

		public String slug() {
			return slug;
		}
	}

	/** Validity is not a quantity: a ladder never renders as a percentage or a score. */
	public static AxisGate.Refusal refuseReduction(Kind kind, Reduction reduction) {
		String reason;
		switch (reduction) {
			case PERCENTAGE:
				reason = "validity is not a quantity; a ladder never renders as a percentage, and"
						+ " legal_sufficiency = 87% is a REFUSED value (rule 53)";
				break;
			case SCORE:
				reason = "a status is not a score (rule 53)";
				break;
			case MAXIMUM:
				reason = kind == Kind.CERTIFICATION_TIER
						? "certification is a threshold evaluation over the whole vector, not a"
								+ " maximum: [A, D, D, D] would certify a body of work on one strong"
								+ " claim while three critical components stay unresolved (rule 52)"
						: "the maximum is the wrong aggregation for a requirement gate: one strong"
								+ " member does not make the rest true";
				break;
			default:
				reason = "unrecognised reduction";
		}
		return new AxisGate.Refusal("ScalarReductionRefused",
				kind.slug() + " " + reduction.slug() + " — " + reason);
	}

	/** {@code certification_tier != max(evidence_grade)}, by name. */
	public static AxisGate.Refusal prohibitedMaxCertification() {
		return new AxisGate.Refusal("AggregationRefused",
				"certification_tier != max(evidence_grade): a maximum is the wrong aggregation for a"
						+ " requirement gate. [A, D, D, D] would certify a body of work on one strong"
						+ " claim while three critical components stay unresolved (rule 52)");
	}

	/** One coverage row: the ladder, its recorded tokens, and whether every value is covered. */
	public record CoverageRow(Kind kind, List<String> values) {
	}

	/** The one coverage table: every ladder, described once, in the axis set's order. */
	public static List<CoverageRow> coverage() {
		List<CoverageRow> rows = new ArrayList<>();
		for (Kind kind : Kind.values()) {
			rows.add(new CoverageRow(kind, kind.tokens()));
		}
		return rows;
	}

	/**
	 * A coverage row that does not describe its ladder is worse than no row, because it looks
	 * checked. Returns every refusal, not the first.
	 */
	public static List<AxisGate.Refusal> checkCoverage(List<CoverageRow> rows) {
		List<AxisGate.Refusal> refusals = new ArrayList<>();
		Map<Kind, Integer> seen = new LinkedHashMap<>();
		for (CoverageRow row : rows) {
			if (row.values().isEmpty()) {
				refusals.add(new AxisGate.Refusal("UncoveredAxis",
						row.kind().slug() + ": a coverage row that declares no values describes no ladder"));
				continue;
			}
			if (!row.values().equals(row.kind().tokens())) {
				refusals.add(new AxisGate.Refusal("UncoveredAxis", row.kind().slug() + ": the row claims "
						+ row.values() + " but the ladder's members are " + row.kind().tokens()
						+ "; a row that does not describe its ladder is worse than no row, because it"
						+ " looks checked"));
			}
			Set<String> distinct = new LinkedHashSet<>(row.values());
			if (distinct.size() != row.values().size()) {
				refusals.add(new AxisGate.Refusal("UncoveredAxis",
						row.kind().slug() + ": a coverage row lists the same token twice"));
			}
			seen.merge(row.kind(), 1, Integer::sum);
		}
		for (Kind kind : Kind.values()) {
			if (!seen.containsKey(kind)) {
				refusals.add(new AxisGate.Refusal("UncoveredAxis",
						kind.slug() + ": no coverage row"));
			}
		}
		return refusals;
	}

	private AxisVocabulary() {
	}
}

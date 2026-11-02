package dev.ensouled.axis;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * The ticking proof of work, ported from `voxel-model/src/proof.rs`.
 *
 * <p>A claim carries the bytes it was made from, the ticks it ran over, the world digest it ended
 * at, the presenter who is accountable for it, and the ticket that authorised it. A proof is
 * current only inside its declared window: an inverted span never happened, a span that ends past
 * the world's head cannot be backdated, and a proof older than the window is <b>refused rather than
 * grandfathered</b>.
 *
 * <p>Two more rules make the proof the evidence and not the claimant's word: the artifact's bytes
 * must hash to what the proof names ({@code DigestMismatch}), and the evidence origin is the
 * proof's to carry, not the claimant's to declare — a claimant that ships its own
 * {@code ticket_evidence} reading is refused.
 */
public final class AxisProof {

	/** Who decided the evidence exists. The origin is a letter, and the proof carries it. */
	public enum Origin {
		/** An external oracle decided it. */
		A("oracle"),
		/** This project looked and measured. */
		B("read_back"),
		/** Somebody said so: evidence, not authority. */
		C("self_report"),
		/** The tree computed it from bytes it read. */
		D("derived");

		private final String slug;

		Origin(String slug) {
			this.slug = slug;
		}

		public String slug() {
			return slug;
		}

		public String token() {
			return AxisVocabulary.Kind.TICKET_EVIDENCE.qualify(name());
		}
	}

	public record Proof(String artifact, String artifactDigest, String claim, Origin origin,
			String evidence, long startTick, long endTick, String worldDigest, String presenter,
			String ticket) {

		public boolean sameSpan(Proof other) {
			return other != null && startTick == other.startTick && endTick == other.endTick;
		}
	}

	/** The world's own position and how old a proof may be before it stops counting. */
	public record Window(long currentTick, long maxAgeTicks) {

		public AxisGate.Refusal isCurrent(Proof proof) {
			if (proof.startTick() > proof.endTick()) {
				return new AxisGate.Refusal("ChainNotEvaluable",
						proof.artifact() + ": tick span " + proof.startTick() + ".." + proof.endTick()
								+ " is inverted, so it never happened");
			}
			if (proof.endTick() > currentTick) {
				return new AxisGate.Refusal("ChainNotEvaluable",
						proof.artifact() + ": the span ends at " + proof.endTick()
								+ " and the world stands at " + currentTick
								+ ", so the proof cannot be backdated past the head");
			}
			long age = currentTick - proof.endTick();
			if (age > maxAgeTicks) {
				return new AxisGate.Refusal("ChainNotEvaluable",
						proof.artifact() + ": the proof is " + age + " ticks old and the window is "
								+ maxAgeTicks + ", so it is refused rather than grandfathered");
			}
			return null;
		}
	}

	/** The artifact domain, so a byte string cannot be hashed as another kind of thing. */
	public static final String DOMAIN_ARTIFACT = "planet-artifact-v1";
	public static final int ARTIFACT_SCHEMA = 1;

	/**
	 * The framed digest, byte-identical to `planet-core/src/hash.rs`: domain, little-endian u16
	 * schema, little-endian u64 length, then the bytes. The length is in the preimage, which is why
	 * a truncated artifact cannot collide with a longer one.
	 */
	public static String digest(String domain, int schema, byte[] bytes) {
		byte[] domainBytes = domain.getBytes(StandardCharsets.UTF_8);
		ByteBuffer header = ByteBuffer.allocate(domainBytes.length + 2 + 8)
				.order(ByteOrder.LITTLE_ENDIAN);
		header.put(domainBytes);
		header.putShort((short) schema);
		header.putLong(bytes.length);
		byte[] framed = new byte[header.capacity() + bytes.length];
		System.arraycopy(header.array(), 0, framed, 0, header.capacity());
		System.arraycopy(bytes, 0, framed, header.capacity(), bytes.length);
		return sha256(framed);
	}

	public static String digest(byte[] bytes) {
		return digest(DOMAIN_ARTIFACT, ARTIFACT_SCHEMA, bytes);
	}

	private static String sha256(byte[] bytes) {
		try {
			MessageDigest sha = MessageDigest.getInstance("SHA-256");
			byte[] out = sha.digest(bytes);
			StringBuilder hex = new StringBuilder(out.length * 2);
			for (byte b : out) {
				hex.append(Character.forDigit((b >> 4) & 0xF, 16));
				hex.append(Character.forDigit(b & 0xF, 16));
			}
			return hex.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 unavailable", e);
		}
	}

	/** The bytes must hash to what the proof names, or the proof is refused. */
	public static AxisGate.Refusal verifyBytes(Proof proof, byte[] bytes) {
		if (bytes == null) {
			return new AxisGate.Refusal("UnsubstantiatedEvidence",
					proof.artifact() + ": no evidence yet — the artifact is not present");
		}
		if (!digest(bytes).equals(proof.artifactDigest())) {
			return new AxisGate.Refusal("DigestMismatch",
					proof.artifact() + ": the artifact's bytes do not hash to what the proof names;"
							+ " the proof is refused rather than shown against bytes it was not made from");
		}
		return null;
	}

	/**
	 * Admit the proof, or refuse it. {@code claimantOrigin} is a reading the claimant supplied about
	 * its own evidence; a proof that carries one is refused, because the origin is the proof's.
	 *
	 * <p>{@code support} is the rest of the reading: the other ladders' qualified tokens. Upstream's
	 * own test is named {@code a_proof_alone_is_never_enough_because_the_support_grade_is_missing} —
	 * the proof is one row, never the pass. A valid proof with no supporting reading from another
	 * ladder is refused, not admitted.
	 */
	public static AxisGate.Decision admit(String subject, Proof proof, byte[] bytes, Window window,
			String claimantOrigin, String worldDigest, List<String> support) {
		String witness = "ticket:" + proof.ticket() + "@" + proof.endTick();
		String basis = proof.claim() + " | " + proof.origin().token() + " | " + proof.evidence();
		AxisGate.Refusal refusal = verifyBytes(proof, bytes);
		if (refusal == null) {
			refusal = window.isCurrent(proof);
		}
		if (refusal == null && (proof.presenter() == null || proof.presenter().isBlank())) {
			refusal = new AxisGate.Refusal("UnsubstantiatedEvidence",
					proof.artifact() + ": no presenter names who is acting; a presentation with no"
							+ " accountable actor is refused");
		}
		if (refusal == null && claimantOrigin != null && !claimantOrigin.isBlank()) {
			AxisVocabulary.Kind filed = AxisVocabulary.bySlug(
					claimantOrigin.trim().split("\\s+")[0]);
			if (filed == AxisVocabulary.Kind.TICKET_EVIDENCE) {
				refusal = new AxisGate.Refusal("DigestMismatch",
						"the origin is the proof's to carry, not the claimant's to declare");
			}
		}
		if (refusal == null && worldDigest != null && !worldDigest.equals(proof.worldDigest())) {
			refusal = new AxisGate.Refusal("DigestMismatch",
					proof.artifact() + ": the proof ends at world " + proof.worldDigest()
							+ " and this world is " + worldDigest);
		}
		if (refusal == null && !hasSupportingReading(support)) {
			refusal = new AxisGate.Refusal("UnsubstantiatedEvidence",
					"a proof alone is never enough because the support grade is missing; a proof is one"
							+ " row, never the pass");
		}
		if (refusal != null) {
			return new AxisGate.Decision(subject, false,
					AxisVocabulary.Kind.PROCEDURAL_GATE.qualify("FAIL"), basis, witness, refusal.code(),
					refusal.why());
		}
		return new AxisGate.Decision(subject, true,
				AxisVocabulary.Kind.PROCEDURAL_GATE.qualify("PASS"), basis, witness, "proof-held",
				"ticket, presenter, current window, bytes and origin agree");
	}

	/** A supporting reading is any well-formed token on a ladder other than the proof's own origins. */
	private static boolean hasSupportingReading(List<String> support) {
		if (support == null) {
			return false;
		}
		for (String reading : support) {
			if (reading == null || reading.isBlank()) {
				continue;
			}
			String[] parts = reading.trim().split("\\s+", 2);
			AxisVocabulary.Kind kind = AxisVocabulary.bySlug(parts[0]);
			if (kind == null || kind == AxisVocabulary.Kind.TICKET_EVIDENCE
					|| kind == AxisVocabulary.Kind.PROCEDURAL_GATE) {
				continue;
			}
			if (AxisVocabulary.check(kind, reading.trim()) == null) {
				return true;
			}
		}
		return false;
	}

	/** Convenience for a UTF-8 artifact. */
	public static byte[] utf8(String text) {
		return text == null ? null : text.getBytes(StandardCharsets.UTF_8);
	}

	private AxisProof() {
	}
}

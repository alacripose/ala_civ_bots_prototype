package dev.ensouled.surface;

import java.util.List;
import java.util.regex.Pattern;

/**
 * `surface.mount/2` — the append-only receipt a receiver acks, ported from `secure-shell`
 * `tools/port/store.mjs`. The last applied row per surface is the floor a later push must beat, and
 * a push that was not applied is still a row: a refusal is recorded beside the mount, not instead
 * of it.
 *
 * The receipt is not a content payload. It says a revision was mounted for a receiver, with its
 * sequence, its digest and its outcome; the windows travel in `secure-shell.served/1`.
 */
public record MountReceipt(String surface, int sequence, String digest, String patch, String pkg,
		String receiver, String state, int windows, String refusal, String at, String receiptId) {

	public static final String HEADER = "surface.mount/2";
	public static final List<String> STATES = List.of("mounted", "already-mounted", "stale-surface",
			"surface-conflict", "refused");
	public static final List<String> APPLIED = List.of("mounted", "already-mounted");

	private static final Pattern DIGEST = Pattern.compile("^sha256:[0-9a-f]{64}$");
	private static final Pattern ID = Pattern.compile("^[a-z0-9-]{1,64}$");
	private static final Pattern AT = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?Z$");
	private static final Pattern RECEIVER = Pattern.compile("^[a-z0-9:._-]{1,64}$");
	private static final Pattern PACKAGE = Pattern.compile("^[A-Za-z0-9._-]{1,128}$");

	public static MountReceipt mount(String surface, int sequence, String digest, String patch, String pkg,
			String receiver, String state, int windows, String refusal, String at) {
		MountReceipt unchecked = new MountReceipt(surface, sequence, digest, patch, pkg, receiver, state, windows,
				refusal, at, "");
		unchecked.validate();
		return new MountReceipt(surface, sequence, digest, patch, pkg, receiver, state, windows, refusal, at,
				receiptId(surface, sequence, digest, patch, pkg, receiver, state, windows, refusal, at));
	}

	public void validate() {
		if (surface == null || !ID.matcher(surface).matches()) {
			throw new SurfaceRefusal("store-schema", "surface", "'" + surface + "' is not an id");
		}
		if (sequence < 0) {
			throw new SurfaceRefusal("store-schema", surface, "sequence " + sequence + " is not a non-negative integer");
		}
		if (digest == null || !DIGEST.matcher(digest).matches()) {
			throw new SurfaceRefusal("store-schema", surface, "digest '" + digest + "'");
		}
		if (!patch.equals("-") && !DIGEST.matcher(patch).matches()) {
			throw new SurfaceRefusal("store-schema", surface, "patch '" + patch + "'");
		}
		if (!pkg.equals("-") && !PACKAGE.matcher(pkg).matches()) {
			throw new SurfaceRefusal("store-schema", surface, "package '" + pkg + "'");
		}
		if (receiver == null || !RECEIVER.matcher(receiver).matches()) {
			throw new SurfaceRefusal("store-schema", surface, "receiver '" + receiver + "'");
		}
		if (!STATES.contains(state)) {
			throw new SurfaceRefusal("store-schema", surface, "state '" + state + "'");
		}
		if (windows < 0) {
			throw new SurfaceRefusal("store-schema", surface, "windows " + windows);
		}
		if (APPLIED.contains(state) && !refusal.equals("-")) {
			throw new SurfaceRefusal("store-schema", surface, state + " carries refusal '" + refusal + "'");
		}
		if (!APPLIED.contains(state) && refusal.equals("-")) {
			throw new SurfaceRefusal("store-schema", surface, state + " carries no refusal code");
		}
		if (at == null || !AT.matcher(at).matches()) {
			throw new SurfaceRefusal("store-schema", surface, "at '" + at + "' is not RFC 3339 UTC");
		}
	}

	/** The id is a digest over the fields a reader can see, in the order they are written. */
	public static String receiptId(String surface, int sequence, String digest, String patch, String pkg,
			String receiver, String state, int windows, String refusal, String at) {
		String joined = String.join("\t", String.valueOf(surface), String.valueOf(sequence), String.valueOf(digest),
				String.valueOf(patch), String.valueOf(pkg), String.valueOf(receiver), String.valueOf(state),
				String.valueOf(windows), String.valueOf(refusal), String.valueOf(at));
		return SurfaceDocument.sha256(joined);
	}

	/** The push a row acknowledges, independent of when it was written. */
	public String pushId() {
		return SurfaceDocument.sha256(String.join("\t", surface, String.valueOf(sequence), digest, patch, pkg,
				receiver, state, String.valueOf(windows), refusal));
	}
}

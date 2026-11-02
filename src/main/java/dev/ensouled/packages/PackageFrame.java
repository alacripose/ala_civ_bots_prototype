package dev.ensouled.packages;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * The package frame: one format for data and code, content-addressed, with the digest in the file
 * name. Ported from {@code planet-package/src/frame.rs} and {@code rows.rs} — the same magic, the
 * same schema numbers, the same domain-separated digest and the same row grammar.
 *
 * <p>A frame is read whole or refused: nothing is converted, no name is trusted over the bytes, and
 * an older frame is refused rather than upgraded. That is why a being's observation and its
 * commitment travel in it — the lane the clients read is the same lane code arrives on, so a hotfix
 * and an observation are one format with two kinds.
 */
public final class PackageFrame {
	/** The magic a frame starts with; a file without it is refused, never guessed at. */
	public static final byte[] FRAME_MAGIC = "V4PKG001".getBytes(StandardCharsets.US_ASCII);
	/** The frame schema this build writes and reads. */
	public static final int FRAME_SCHEMA = 1;
	/** The payload schema this build writes and reads. */
	public static final int ROW_SCHEMA = 1;
	/** The digest domain, so a package digest cannot collide with another domain's. */
	public static final String DOMAIN = "planet-package-v1";

	/** What a package carries: data now, and code declared so its arrival does not change the frame. */
	public enum Kind {
		DATA(1, "data"),
		CODE(2, "code");

		private final int code;
		private final String recorded;

		Kind(int code, String recorded) {
			this.code = code;
			this.recorded = recorded;
		}

		public int code() {
			return code;
		}

		/** The recorded name. */
		public String recorded() {
			return recorded;
		}

		public static Kind fromCode(int value) {
			for (Kind kind : values()) {
				if (kind.code == value) {
					return kind;
				}
			}
			return null;
		}
	}

	/**
	 * The declared row kinds. A new kind is a row here and a line in the format document; a kind the
	 * table does not declare is refused, because registering one is a recorded act.
	 *
	 * <p>Both kinds carry the §47.2 envelope fields in the same declared order, so a reader that can
	 * read one row can read the other.
	 */
	public enum RowKind {
		OBSERVATION("observation", FIELDS.length),
		DECISION("decision", FIELDS.length);

		private final String recorded;
		private final int fieldCount;

		RowKind(String recorded, int fieldCount) {
			this.recorded = recorded;
			this.fieldCount = fieldCount;
		}

		public String recorded() {
			return recorded;
		}

		public int fieldCount() {
			return fieldCount;
		}

		public static RowKind fromName(String name) {
			for (RowKind kind : values()) {
				if (kind.recorded.equals(name)) {
					return kind;
				}
			}
			return null;
		}
	}

	/** The §47.2 envelope, in the order the fields are written. */
	public static final String[] FIELDS = { "message_id", "conversation_id", "sender", "sender_name",
			"recipients", "channel", "scope", "speech_act", "utterance", "content_hash", "tick",
			"dimension", "position" };

	/** One row: its declared kind, its subject, and its fields in the kind's declared order. */
	public record Row(RowKind kind, String subject, List<String> fields) {
		public Row {
			if (kind == null) {
				throw new IllegalArgumentException("row-kind: a row declares its kind");
			}
			if (fields.size() != kind.fieldCount()) {
				throw new IllegalArgumentException("row-fields: " + kind.recorded() + " declares "
						+ kind.fieldCount() + " fields, found " + fields.size());
			}
		}
	}

	/** A frame: its kind and the rows its payload carries. */
	public record Frame(Kind kind, List<Row> rows) {
		/** The payload: its schema, its row count, then every row. */
		public byte[] payload() {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			out.writeBytes(le16(ROW_SCHEMA));
			out.writeBytes(le32(rows.size()));
			for (Row row : rows) {
				putBytes(out, row.kind().recorded().getBytes(StandardCharsets.UTF_8));
				putBytes(out, row.subject().getBytes(StandardCharsets.UTF_8));
				out.writeBytes(le32(row.fields().size()));
				for (String field : row.fields()) {
					putBytes(out, (field == null ? "" : field).getBytes(StandardCharsets.UTF_8));
				}
			}
			return out.toByteArray();
		}

		/** The frame: magic, schema, kind, payload length, payload digest, payload. */
		public byte[] encode() {
			byte[] payload = payload();
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			out.writeBytes(FRAME_MAGIC);
			out.writeBytes(le16(FRAME_SCHEMA));
			out.writeBytes(le16(kind.code()));
			out.writeBytes(le64(payload.length));
			out.writeBytes(digest(FRAME_SCHEMA, payload));
			out.writeBytes(payload);
			return out.toByteArray();
		}

		/** The digest of the bytes, as the file name carries it. */
		public String digestHex() {
			return hex(digest(FRAME_SCHEMA, payload()));
		}

		/** The content-addressed name: the digest is in it, and a name without one is refused. */
		public String name(String id) {
			if (id == null || !id.matches("[a-z0-9][a-z0-9_\\-]{0,63}")) {
				throw new IllegalArgumentException("bad-id: \"" + id + "\" is not a package id");
			}
			return id + "-" + digestHex() + ".pkg";
		}
	}

	/** An empty data frame. */
	public static Frame data() {
		return new Frame(Kind.DATA, new ArrayList<>());
	}

	/** The same row, appended, so a package is built by adding facts rather than by rewriting them. */
	public static Frame with(Frame frame, Row row) {
		List<Row> rows = new ArrayList<>(frame.rows());
		rows.add(row);
		return new Frame(frame.kind(), rows);
	}

	/**
	 * Reads a frame whole, refusing every drift by name. It is the check that makes the lane honest:
	 * a row that does not decode is not half-read, it is refused.
	 */
	public static Frame decode(byte[] bytes) {
		int head = FRAME_MAGIC.length + 2 + 2 + 8 + 32;
		if (bytes == null || bytes.length < head) {
			throw new IllegalArgumentException("truncated: a frame shorter than its own header");
		}
		for (int i = 0; i < FRAME_MAGIC.length; i++) {
			if (bytes[i] != FRAME_MAGIC[i]) {
				throw new IllegalArgumentException("bad-magic: not a package frame");
			}
		}
		ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
		buffer.position(FRAME_MAGIC.length);
		int schema = Short.toUnsignedInt(buffer.getShort());
		if (schema != FRAME_SCHEMA) {
			throw new IllegalArgumentException("bad-schema: frame schema " + schema + ", this build reads "
					+ FRAME_SCHEMA);
		}
		Kind kind = Kind.fromCode(Short.toUnsignedInt(buffer.getShort()));
		if (kind == null) {
			throw new IllegalArgumentException("unknown-frame-kind");
		}
		long declared = buffer.getLong();
		byte[] expected = new byte[32];
		buffer.get(expected);
		int bodyAt = buffer.position();
		if (declared < 0 || bodyAt + declared != bytes.length) {
			throw new IllegalArgumentException("truncated: frame declares " + declared + " bytes, found "
					+ (bytes.length - bodyAt));
		}
		byte[] payload = new byte[(int) declared];
		System.arraycopy(bytes, bodyAt, payload, 0, (int) declared);
		if (!MessageDigest.isEqual(expected, digest(FRAME_SCHEMA, payload))) {
			throw new IllegalArgumentException("digest-mismatch: the frame does not hash to its header");
		}
		ByteBuffer body = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);
		int rowSchema = Short.toUnsignedInt(body.getShort());
		if (rowSchema != ROW_SCHEMA) {
			throw new IllegalArgumentException("bad-row-schema: " + rowSchema);
		}
		long count = Integer.toUnsignedLong(body.getInt());
		List<Row> rows = new ArrayList<>();
		for (long i = 0; i < count; i++) {
			String kindName = readBytes(body);
			RowKind rowKind = RowKind.fromName(kindName);
			if (rowKind == null) {
				throw new IllegalArgumentException("unknown-row-kind: " + kindName);
			}
			String subject = readBytes(body);
			long fields = Integer.toUnsignedLong(body.getInt());
			if (fields != rowKind.fieldCount()) {
				throw new IllegalArgumentException("field-count: " + rowKind.recorded() + " declares "
						+ rowKind.fieldCount() + ", found " + fields);
			}
			List<String> values = new ArrayList<>();
			for (long f = 0; f < fields; f++) {
				values.add(readBytes(body));
			}
			rows.add(new Row(rowKind, subject, values));
		}
		if (body.hasRemaining()) {
			throw new IllegalArgumentException("trailing-bytes: " + body.remaining() + " after the last row");
		}
		return new Frame(kind, rows);
	}

	/** SHA-256 over domain, schema, length and bytes: the framing is what stops two domains colliding. */
	public static byte[] digest(int schema, byte[] bytes) {
		try {
			MessageDigest hasher = MessageDigest.getInstance("SHA-256");
			hasher.update(DOMAIN.getBytes(StandardCharsets.UTF_8));
			hasher.update(le16(schema));
			hasher.update(le64(bytes.length));
			hasher.update(bytes);
			return hasher.digest();
		} catch (Exception unavailable) {
			throw new IllegalStateException("sha-256 is not available", unavailable);
		}
	}

	public static String hex(byte[] bytes) {
		StringBuilder out = new StringBuilder(bytes.length * 2);
		for (byte value : bytes) {
			out.append(Character.forDigit((value >> 4) & 0xF, 16));
			out.append(Character.forDigit(value & 0xF, 16));
		}
		return out.toString();
	}

	private static String readBytes(ByteBuffer body) {
		if (body.remaining() < 8) {
			throw new IllegalArgumentException("truncated: a field has no length");
		}
		long length = body.getLong();
		if (length < 0 || length > body.remaining()) {
			throw new IllegalArgumentException("truncated: a field declares " + length + " bytes, "
					+ body.remaining() + " remain");
		}
		byte[] value = new byte[(int) length];
		body.get(value);
		return new String(value, StandardCharsets.UTF_8);
	}

	private static void putBytes(ByteArrayOutputStream out, byte[] bytes) {
		out.writeBytes(le64(bytes.length));
		out.writeBytes(bytes);
	}

	private static byte[] le16(int value) {
		return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort((short) value).array();
	}

	private static byte[] le32(int value) {
		return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array();
	}

	private static byte[] le64(long value) {
		return ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(value).array();
	}

	private PackageFrame() {
	}
}

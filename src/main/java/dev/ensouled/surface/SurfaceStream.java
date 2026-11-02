package dev.ensouled.surface;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import dev.ensouled.EnsouledMod;
import dev.ensouled.core.MessageEnvelope;
import dev.ensouled.core.ProseLines;
import org.bson.Document;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * The server-side streamer: mount the vendored toast declaration, receipt it, and serve
 * `secure-shell.served/1` over loopback with a bearer token.
 *
 * Three owners, no second copy of any of them: the declaration is the vendored
 * `packages/toast/toast.surface`; the mount is {@link MountReceipt}, whose row is written once and
 * whose identity is the digest of the bytes it received; the served form is {@link ServedPayload},
 * rebuilt on read so it cannot drift from the mount.
 *
 * Two registers, two jobs. {@link StreamLedger} is the append-only chain and the commit point — an
 * event is landed when its row is fsynced. {@link MongoStreams} is the per-civilian projection and
 * the isolation boundary — a feed is read from the agent's own stream, verified by read-back, so a
 * citizen's refusal cannot reach another citizen's toast stack. If the store is unreachable this
 * streamer serves nothing; a single merged feed is the failure both exist to prevent.
 *
 * The display is not here. This process does not decide what a toast looks like; it names the window,
 * the wish plan and the evidence rows, and the game client's own toast stack draws them.
 */
public final class SurfaceStream {
	public static final String PROJECT = "ala-civ-bots";
	public static final String SURFACE_PATH = "packages/toast/toast.surface";
	public static final String BASELINE_PATH = "packages/toast/toast.port.json";
	public static final String RECEIVER = "ensouled-minecraft-server";
	public static final String BASELINE_FORMAT = "secure-shell.port-baseline/1";
	public static final String SYSTEM_STREAM = "ensouled-server";

	private static final Gson GSON = new Gson();
	private static final Pattern DIGEST = Pattern.compile("^sha256:[0-9a-f]{64}$");
	private static final Pattern AGENT = Pattern.compile("^[A-Za-z0-9_]{1,16}$");
	private static final int NOTICE_TAIL = 3;

	private static SurfaceStream instance;

	private final Path root;
	private final StreamLedger ledger;
	private final MongoStreams mongo;
	/**
	 * The mounted revision. A remount swaps this pair as one reference: a served payload must never
	 * be built from the new windows and the previous digest, or the old windows and the new one.
	 */
	private volatile Mounted mounted;
	private final String token;
	private final HttpServer http;
	private final ExecutorService executor;
	/** Each subscriber is bound to one agent: the feed it receives is that agent's stream alone. */
	private final Map<HttpExchange, String> subscribers = new ConcurrentHashMap<>();

	private record Mounted(SurfaceDocument document, MountReceipt receipt) {
	}

	private SurfaceStream(Path root, StreamLedger ledger, MongoStreams mongo, SurfaceDocument document,
			MountReceipt receipt, String token, HttpServer http) {
		this.root = root;
		this.ledger = ledger;
		this.mongo = mongo;
		this.mounted = new Mounted(document, receipt);
		this.token = token;
		this.http = http;
		this.executor = Executors.newFixedThreadPool(4, runnable -> {
			Thread thread = new Thread(runnable, "ensouled-stream");
			thread.setDaemon(true);
			return thread;
		});
	}

	public static synchronized SurfaceStream get() {
		return instance;
	}

	public static synchronized SurfaceStream start(Path workingDirectory) {
		if (instance != null) {
			return instance;
		}
		// The dev server runs from `run/`; the packages, the token and the ledger belong to the
		// project root. Find it by the declaration rather than by the caller's assumption.
		Path root = projectRoot(workingDirectory);
		Path surfaceFile = root.resolve(SURFACE_PATH);
		StreamLedger ledger = new StreamLedger(root.resolve("run/stream/ledger.jsonl"), PROJECT);
		if (!ledger.writable()) {
			EnsouledMod.LOG.error("[ensouled] stream ledger is not writable: {} — serving nothing",
					ledger.errors().isEmpty() ? "unverified chain" : ledger.errors().get(0));
			return null;
		}
		String token;
		try {
			token = token(root.resolve("run/stream.token"));
		} catch (IOException noToken) {
			EnsouledMod.LOG.error("[ensouled] stream token could not be written: {}", noToken.getMessage());
			return null;
		}
		if (!Files.exists(surfaceFile)) {
			EnsouledMod.LOG.error("[ensouled] stream refused @surface-missing: {}", surfaceFile);
			return null;
		}
		MongoStreams mongo;
		try {
			mongo = MongoStreams.open(System.getenv("ENSOULED_MONGO_URI"), PROJECT);
		} catch (SurfaceRefusal unavailable) {
			EnsouledMod.LOG.error("[ensouled] stream refused {} — the per-agent store is required, so nothing is served",
					unavailable.getMessage());
			return null;
		}
		SurfaceDocument document;
		MountReceipt receipt;
		try {
			document = SurfaceDocument.parse(Files.readString(surfaceFile, StandardCharsets.UTF_8), SURFACE_PATH);
			checkBaseline(root.resolve(BASELINE_PATH), document);
			receipt = mount(ledger, root, document);
		} catch (SurfaceRefusal refusal) {
			EnsouledMod.LOG.error("[ensouled] stream refused {} — nothing is served", refusal.getMessage());
			recordMountRefusal(ledger, refusal);
			mongo.close();
			return null;
		} catch (IOException unreadable) {
			EnsouledMod.LOG.error("[ensouled] stream surface could not be read: {}", unreadable.getMessage());
			mongo.close();
			return null;
		}
		HttpServer server;
		try {
			server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), port()), 0);
		} catch (IOException busy) {
			EnsouledMod.LOG.error("[ensouled] stream port is not available: {} — nothing is served", busy.getMessage());
			mongo.close();
			return null;
		}
		SurfaceStream stream = new SurfaceStream(root, ledger, mongo, document, receipt, token, server);
		stream.routes();
		server.setExecutor(stream.executor);
		server.start();
		ledger.listen(stream::announce);
		stream.replay();
		instance = stream;
		EnsouledMod.LOG.info("[ensouled] surface mounted: surface={} sequence={} state={} windows={} digest={}",
				receipt.surface(), receipt.sequence(), receipt.state(), receipt.windows(), receipt.digest());
		EnsouledMod.LOG.info("[ensouled] mount receipt {} at {}", receipt.receiptId(), receipt.at());
		EnsouledMod.LOG.info("[ensouled] stream store {} at {} (per-agent isolation, read-back verified)",
				mongo.databaseName(), mongo.uri());
		EnsouledMod.LOG.info("[ensouled] served stream on http://127.0.0.1:{} (token file run/stream.token, ledger row {})",
				port(), ledger.headSeq());
		return stream;
	}

	private static int port() {
		return Integer.getInteger("ensouled.stream.port", 7664);
	}

	private static Path projectRoot(Path start) {
		Path cursor = start.toAbsolutePath().normalize();
		for (int depth = 0; depth < 5 && cursor != null; depth++) {
			if (Files.exists(cursor.resolve(SURFACE_PATH))) {
				return cursor;
			}
			cursor = cursor.getParent();
		}
		return start.toAbsolutePath().normalize();
	}

	public static synchronized void stop() {
		if (instance == null) {
			return;
		}
		instance.http.stop(0);
		instance.executor.shutdownNow();
		for (HttpExchange exchange : instance.subscribers.keySet()) {
			exchange.close();
		}
		instance.subscribers.clear();
		instance.mongo.close();
		instance = null;
	}

	/** Project the committed chain into the per-agent store, so a restart serves the same feeds. */
	private void replay() {
		int verified = 0;
		int refused = 0;
		for (StreamLedger.Entry entry : ledger.entries()) {
			List<String> mismatches = mongo.record(entry.json().get("actor").getAsString(), entry.json());
			if (mismatches.isEmpty()) {
				verified++;
			} else {
				refused++;
				EnsouledMod.LOG.error("[ensouled] stream row {} read back different: {}", entry.seq(), mismatches);
			}
		}
		mongo.register(SYSTEM_STREAM, document().id(), receipt().digest());
		EnsouledMod.LOG.info("[ensouled] stream store replay: {} row(s) verified, {} refused", verified, refused);
	}

	/** A committed row is projected and announced; an unverified projection is not announced. */
	private void announce(StreamLedger.Entry entry) {
		List<String> mismatches;
		try {
			mismatches = mongo.record(entry.json().get("actor").getAsString(), entry.json());
		} catch (RuntimeException unavailable) {
			EnsouledMod.LOG.error("[ensouled] stream row {} could not be projected: {}", entry.seq(), unavailable.getMessage());
			return;
		}
		if (!mismatches.isEmpty()) {
			EnsouledMod.LOG.error("[ensouled] stream row {} read back different and is not served: {}", entry.seq(), mismatches);
			return;
		}
		broadcast(entry);
	}

	/**
	 * The mount: the last applied row is the floor a later push must beat, and a retry of the same
	 * bytes is `already-mounted` rather than a second row.
	 */
	private static MountReceipt mount(StreamLedger ledger, Path root, SurfaceDocument document) {
		String digest = SurfaceDocument.sha256(readSurface(root));
		MountReceipt last = lastMount(ledger);
		String at = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
		if (last != null && last.digest().equals(digest)) {
			return MountReceipt.mount(document.id(), last.sequence(), digest, "-", "toast.surface", RECEIVER,
					"already-mounted", document.windows().size(), "-", at);
		}
		int sequence = last == null ? 1 : last.sequence() + 1;
		MountReceipt receipt = MountReceipt.mount(document.id(), sequence, digest, "-", "toast.surface", RECEIVER,
				"mounted", document.windows().size(), "-", at);
		ledger.append("mount", SYSTEM_STREAM, SURFACE_PATH, digest, "committed",
				"mount:" + receipt.receiptId(), null,
				StreamLedger.detail("receipt-id", receipt.receiptId(), "surface", receipt.surface(),
						"sequence", Integer.toString(receipt.sequence()), "digest", digest,
						"state", receipt.state(), "windows", Integer.toString(receipt.windows()), "at", at));
		return receipt;
	}

	/**
	 * Push the declaration on disk as a new revision, on the running server. This is the hotfix
	 * path: the jar that reads packages does not change, the package is re-read and re-validated
	 * against its port baseline, mounted as the next sequence (`already-mounted` for the same
	 * bytes), and the mount row the ledger commits is what connected clients resume from. Nothing
	 * restarts. A declaration that fails its own rules is recorded as a refusal and stays unserved.
	 */
	public synchronized JsonObject remount() {
		SurfaceDocument next;
		MountReceipt nextReceipt;
		try {
			next = SurfaceDocument.parse(Files.readString(root.resolve(SURFACE_PATH), StandardCharsets.UTF_8),
					SURFACE_PATH);
			checkBaseline(root.resolve(BASELINE_PATH), next);
			nextReceipt = mount(ledger, root, next);
		} catch (SurfaceRefusal refusal) {
			recordMountRefusal(ledger, refusal);
			throw refusal;
		} catch (IOException unreadable) {
			SurfaceRefusal refusal = new SurfaceRefusal("surface-missing", SURFACE_PATH, unreadable.getMessage());
			recordMountRefusal(ledger, refusal);
			throw refusal;
		}
		mounted = new Mounted(next, nextReceipt);
		try {
			mongo.register(SYSTEM_STREAM, next.id(), nextReceipt.digest());
		} catch (RuntimeException unavailable) {
			// The mount is already committed; a store that cannot register it says so and keeps serving.
			EnsouledMod.LOG.error("[ensouled] remount {} could not be registered: {}", nextReceipt.receiptId(),
					unavailable.getMessage());
		}
		EnsouledMod.LOG.info("[ensouled] surface remounted: surface={} sequence={} state={} windows={} digest={}",
				nextReceipt.surface(), nextReceipt.sequence(), nextReceipt.state(), nextReceipt.windows(),
				nextReceipt.digest());
		return receiptJson(nextReceipt);
	}

	/** A push that was not applied is still a row: the refusal is written beside the mount. */
	private static void recordMountRefusal(StreamLedger ledger, SurfaceRefusal refusal) {
		try {
			ledger.append("mount", SYSTEM_STREAM, SURFACE_PATH, "-", "rejected",
					"mount-refused:" + refusal.code + ":" + Math.abs(refusal.subject.hashCode()), null,
					StreamLedger.detail("code", refusal.code, "subject", refusal.subject, "why", refusal.why));
		} catch (RuntimeException ignored) {
			// The refusal is already on the log; a ledger that cannot record it reports its own error.
		}
	}

	private static JsonObject receiptJson(MountReceipt receipt) {
		JsonObject body = new JsonObject();
		body.addProperty("header", MountReceipt.HEADER);
		body.addProperty("receipt-id", receipt.receiptId());
		body.addProperty("surface", receipt.surface());
		body.addProperty("sequence", receipt.sequence());
		body.addProperty("digest", receipt.digest());
		body.addProperty("state", receipt.state());
		body.addProperty("windows", receipt.windows());
		body.addProperty("receiver", receipt.receiver());
		body.addProperty("at", receipt.at());
		return body;
	}

	private static String readSurface(Path root) {
		try {
			return Files.readString(root.resolve(SURFACE_PATH), StandardCharsets.UTF_8);
		} catch (IOException unreadable) {
			throw new SurfaceRefusal("surface-missing", SURFACE_PATH, unreadable.getMessage());
		}
	}

	/**
	 * The highest **applied** mount. A refused push is recorded as a `mount` row too, so the newest
	 * row of that kind is not the floor: a rejection was never applied, and letting it sit at the
	 * head restarted the sequence at 1, numbering a later revision below the one clients already
	 * carry. The floor is therefore the greatest applied sequence, which also repairs a ledger that
	 * was written while that bug was live.
	 */
	private static MountReceipt lastMount(StreamLedger ledger) {
		MountReceipt floor = null;
		for (StreamLedger.Entry entry : ledger.entries()) {
			if (!"mount".equals(entry.kind()) || !SURFACE_PATH.equals(entry.subject())
					|| !"committed".equals(entry.outcome())) {
				continue;
			}
			JsonObject json = entry.json();
			if (!json.get("detail").isJsonObject()) {
				continue;
			}
			JsonObject detail = json.getAsJsonObject("detail");
			if (!detail.has("sequence") || !detail.has("digest")) {
				continue;
			}
			MountReceipt candidate = MountReceipt.mount(detail.get("surface").getAsString(),
					detail.get("sequence").getAsInt(), detail.get("digest").getAsString(), "-", "toast.surface",
					RECEIVER, detail.get("state").getAsString(), detail.get("windows").getAsInt(), "-",
					json.get("at").getAsString());
			if (floor == null || candidate.sequence() > floor.sequence()
					|| (candidate.sequence() == floor.sequence() && candidate.at().compareTo(floor.at()) > 0)) {
				floor = candidate;
			}
		}
		return floor;
	}

	/** A baseline that disagrees with the loaded surface is `port-baseline`, not a warning. */
	private static void checkBaseline(Path baselineFile, SurfaceDocument document) {
		JsonObject baseline;
		try {
			baseline = JsonParser.parseString(Files.readString(baselineFile, StandardCharsets.UTF_8)).getAsJsonObject();
		} catch (IOException | RuntimeException unreadable) {
			throw new SurfaceRefusal("port-baseline", BASELINE_PATH, "the baseline could not be read: " + unreadable.getMessage());
		}
		if (!BASELINE_FORMAT.equals(baseline.has("format") ? baseline.get("format").getAsString() : "")) {
			throw new SurfaceRefusal("port-baseline", BASELINE_PATH, "expected format " + BASELINE_FORMAT);
		}
		if (!document.id().equals(baseline.has("surface") ? baseline.get("surface").getAsString() : "")) {
			throw new SurfaceRefusal("port-baseline", BASELINE_PATH, "the baseline names another surface");
		}
		JsonArray rows = baseline.getAsJsonArray("rows");
		for (JsonElement element : rows) {
			JsonObject row = element.getAsJsonObject();
			String name = row.get("name").getAsString();
			int measured = switch (name) {
				case "surface windows" -> document.windows().size();
				case "surface nodes" -> document.nodes().size();
				case "surface actions" -> document.actions().size();
				default -> throw new SurfaceRefusal("port-baseline", name, "the baseline names a row this port cannot measure");
			};
			int expected = row.get("value").getAsInt();
			if (measured != expected) {
				throw new SurfaceRefusal("port-baseline", name,
						"the baseline says " + expected + ", the mounted surface has " + measured);
			}
		}
	}

	private void routes() {
		http.createContext("/api/health", exchange -> {
			try {
				JsonObject body = new JsonObject();
				body.addProperty("project", PROJECT);
				body.addProperty("head", ledger.headSeq());
				body.addProperty("surface", receipt().surface());
				body.addProperty("state", receipt().state());
				body.addProperty("store", mongo.databaseName());
				respond(exchange, 200, body);
			} catch (IOException closed) {
				exchange.close();
			}
		});
		// The live hotfix: the declaration on disk becomes the next revision of the mounted package
		// without touching the jar and without restarting the server. It is a POST because it changes
		// what is served; a refusal leaves the previous revision mounted and is itself a ledger row.
		http.createContext("/api/package/mount", exchange -> {
			try {
				if (!authorized(exchange)) {
					return;
				}
				if (!"POST".equals(exchange.getRequestMethod())) {
					error(exchange, 405, "mount-method", "package", "a mount is posted; a read changes nothing");
					return;
				}
				respond(exchange, 200, remount());
			} catch (SurfaceRefusal refusal) {
				error(exchange, 400, refusal.code, refusal.subject, refusal.why);
			} catch (IOException closed) {
				exchange.close();
			}
		});
		http.createContext("/api/served", exchange -> {
			try {
				if (!authorized(exchange)) {
					return;
				}
				String agent = query(exchange, "agent");
				if (agent == null || !AGENT.matcher(agent).matches()) {
					error(exchange, 400, "served-no-agent", "agent",
							"a served payload names whose stream it is; no merged feed is served");
					return;
				}
				if (!mongo.has(agent)) {
					mongo.register(agent, document().id(), receipt().digest());
					EnsouledMod.LOG.info("[ensouled] stream registered for {} (own feed, no rows yet)", agent);
				}
				respond(exchange, 200, served(agent));
			} catch (SurfaceRefusal refusal) {
				error(exchange, 400, refusal.code, refusal.subject, refusal.why);
			} catch (IOException closed) {
				exchange.close();
			}
		});
		http.createContext("/api/ledger/head", exchange -> {
			try {
				if (!authorized(exchange)) {
					return;
				}
				JsonObject body = new JsonObject();
				body.addProperty("seq", ledger.headSeq());
				body.addProperty("hash", ledger.headHash());
				respond(exchange, 200, body);
			} catch (IOException closed) {
				exchange.close();
			}
		});
		http.createContext("/api/ledger/events", exchange -> {
			try {
				if (!authorized(exchange)) {
					return;
				}
				events(exchange);
			} catch (IOException closed) {
				exchange.close();
			}
		});
		http.createContext("/api/witness", exchange -> {
			try {
				if (!authorized(exchange)) {
					return;
				}
				witness(exchange);
			} catch (SurfaceRefusal refusal) {
				error(exchange, 400, refusal.code, refusal.subject, refusal.why);
			} catch (IOException closed) {
				exchange.close();
			}
		});
	}

	/** The payload a client receives, rebuilt from the mount and read from that agent's own stream. */
	public JsonObject served(String agent) {
		return ServedPayload.served(document(), receipt(), wishes(), notices(agent), requirements(), agent, null,
				Instant.now());
	}

	private List<ServedPayload.Wish> wishes() {
		List<ServedPayload.Wish> rows = new ArrayList<>();
		String text = readSurface(root);
		List<String> nodeIds = document().nodes().stream().map(SurfaceDocument.Node::id).toList();
		boolean arrived = List.of("mounted", "already-mounted").contains(receipt().state());
		rows.add(ServedPayload.wish("package:" + SURFACE_PATH, "package", SURFACE_PATH,
				declarationLine(text, "surface", document().id()), "load " + SURFACE_PATH,
				arrived ? "loaded" : "refused", arrived ? "" : receipt().state(), receipt().digest()));
		for (SurfaceDocument.Window window : document().windows()) {
			if (nodeIds.contains(window.content())) {
				continue;
			}
			rows.add(ServedPayload.wish("content:" + window.id(), "content", SURFACE_PATH,
					declarationLine(text, "window", window.id()), "open " + window.title(), "missing",
					"content '" + window.content() + "' has no mounted source", ""));
		}
		return rows;
	}

	private List<ServedPayload.Requirement> requirements() {
		List<ServedPayload.Requirement> rows = new ArrayList<>();
		rows.add(ServedPayload.requirement("surface declaration", SURFACE_PATH, receipt().digest()));
		for (SurfaceDocument.Window window : document().windows()) {
			rows.add(ServedPayload.requirement("window " + window.id(), SURFACE_PATH, receipt().digest()));
		}
		return rows;
	}

	private List<ServedPayload.Notice> notices(String agent) {
		List<ServedPayload.Notice> rows = new ArrayList<>();
		for (Document document : mongo.noticeTail(agent, NOTICE_TAIL)) {
			rows.add(noticeFor(document));
		}
		return rows;
	}

	/** A refusal looks like a refusal: the title says the word, not only the border colour. */
	private static ServedPayload.Notice noticeFor(Document row) {
		String kind = row.getString("kind");
		boolean refused = "rejected".equals(row.getString("outcome"));
		String title = refused ? "Refused: " + kind : kind;
		String why = "";
		Object detail = row.get("detail");
		if (detail instanceof Document object) {
			for (String field : List.of("why", "code", "receipt-id")) {
				Object value = object.get(field);
				if (value instanceof String text && !text.isBlank()) {
					why = text;
					break;
				}
			}
		}
		String body = why.isEmpty() ? row.getString("subject") : row.getString("subject") + " — " + why;
		return new ServedPayload.Notice(title, body, null, null, refused);
	}

	private static int declarationLine(String text, String kind, String id) {
		List<String> lines = List.of(text.replace("\r\n", "\n").split("\n", -1));
		String needle = kind + "\t" + id;
		for (int index = 0; index < lines.size(); index++) {
			if (lines.get(index).equals(needle) || lines.get(index).startsWith(needle + "\t")) {
				return index + 1;
			}
		}
		return 1;
	}

	/** The client's witness, appended as a `test` row after the payload was checked in the client. */
	public StreamLedger.Entry witness(String agent, String command, int exitStatus, int checks, String surface,
			String digest, JsonObject detail) {
		if (command == null || command.isBlank()) {
			throw new SurfaceRefusal("test-shape", "command", "a test entry carries the command it ran");
		}
		if (checks < 0) {
			throw new SurfaceRefusal("test-shape", "checks", "a check count is not negative");
		}
		if (digest == null || !DIGEST.matcher(digest).matches()) {
			throw new SurfaceRefusal("test-shape", "digest", "a witness names the bytes it checked as sha256:<64 hex>");
		}
		if (agent == null || !AGENT.matcher(agent).matches()) {
			throw new SurfaceRefusal("served-no-agent", "agent", "a witness belongs to one agent's stream");
		}
		JsonObject evidence = new JsonObject();
		evidence.addProperty("command", command);
		evidence.addProperty("exit_status", exitStatus);
		evidence.addProperty("checks", checks);
		evidence.addProperty("surface", surface == null ? SURFACE_PATH : surface);
		evidence.addProperty("digest", digest);
		return ledger.append("test", agent, surface == null ? SURFACE_PATH : surface, digest,
				exitStatus == 0 ? "committed" : "rejected", "witness:" + UUID.randomUUID(), evidence,
				detail == null ? new JsonObject() : detail);
	}

	/**
	 * A committed fact on one being's own stream: a recovered decision, a reconciliation. It is the
	 * read-back half of the journal — the store the CUA reads instead of guessing.
	 */
	public void record(String agent, String subject, String code, String why) {
		if (agent == null || !AGENT.matcher(agent).matches()) {
			return;
		}
		JsonObject detail = StreamLedger.detail("code", code, "why", why);
		String utterance = ProseLines.record(agent, agent + " holds that " + plain(subject)
				+ (why == null || why.isBlank() ? "" : ", because " + plain(why)) + ".");
		ledger.append("record", agent, subject, "-", "committed", "record:" + UUID.randomUUID(), null,
				withUtterance(detail, agent, subject, "REPORT", utterance));
	}

	/** A refusal a being made, recorded in that being's own stream. */
	public void refuse(String agent, String subject, String code, String why) {
		if (agent == null || !AGENT.matcher(agent).matches()) {
			return;
		}
		JsonObject detail = StreamLedger.detail("code", code, "why", why);
		String utterance = ProseLines.forbid(agent, agent + " may not begin " + plain(subject)
				+ (why == null || why.isBlank() ? "" : ", because " + plain(why)) + ".");
		ledger.append("record", agent, subject, "-", "rejected", "refusal:" + UUID.randomUUID(), null,
				withUtterance(detail, agent, subject, "DECLINE", utterance));
	}

	/**
	 * Put the prose half of a row where a reader looks for it. The machine half stays in the row's
	 * own detail, so nothing that was recorded is lost by gaining a sentence.
	 */
	private static JsonObject withUtterance(JsonObject detail, String agent, String subject,
			String speechAct, String utterance) {
		JsonObject row = MessageEnvelope.self(agent, "stream:" + agent, speechAct, utterance, 0L, null,
				null, detail);
		JsonObject merged = row.deepCopy();
		JsonObject payload = merged.getAsJsonObject("payload");
		payload.addProperty("subject", subject);
		return merged;
	}

	/** Machine text a sentence can carry: ids and codes survive, punctuation that ends a clause does not. */
	private static String plain(String raw) {
		if (raw == null) {
			return "";
		}
		return raw.replace('[', '(').replace(']', ')').replace('\n', ' ').trim();
	}

	/**
	 * One decision, streamed to the being's own feed. A row whose token is not qualified for its
	 * ladder is refused here as well as at the source: a bare token in the stream is the bug the
	 * axis vocabulary names, and it does not become acceptable by arriving late.
	 */
	public void decision(String agent, JsonObject pkg) {
		if (agent == null || !AGENT.matcher(agent).matches() || pkg == null) {
			return;
		}
		JsonObject payload = MessageEnvelope.payload(pkg);
		String token = payload.has("token") ? payload.get("token").getAsString() : "";
		String ladder = token.contains(" ") ? token.substring(0, token.indexOf(' ')) : "";
		boolean qualified = dev.ensouled.axis.AxisVocabulary.bySlug(ladder) != null;
		String subject = payload.has("subject") ? payload.get("subject").getAsString() : "axis:decision";
		JsonObject detail = pkg.deepCopy();
		if (!MessageEnvelope.isProse(MessageEnvelope.utterance(pkg))) {
			// A decision row speaks or it is not streamed: the lane's job is to hand a reader
			// language, and a row without a readable sentence is the flood this refuses.
			detail.addProperty("refused_by", "NotProse");
			ledger.append("record", agent, subject, "-", "rejected", "decision:" + UUID.randomUUID(), null,
					detail);
			return;
		}
		if (!qualified) {
			detail.addProperty("refused_by", "BareTokenRefused");
			ledger.append("record", agent, subject, "-", "rejected", "decision:" + UUID.randomUUID(), null,
					detail);
			return;
		}
		boolean admitted = payload.has("admitted") && payload.get("admitted").getAsBoolean();
		String witness = payload.has("witness") ? payload.get("witness").getAsString() : "-";
		ledger.append("record", agent, subject, witness, admitted ? "committed" : "rejected",
				"decision:" + UUID.randomUUID(), null, detail);
	}

	/**
	 * One ticket on a being's own stream: what was authorised, how many cells the authorisation
	 * covered, and how it closed. A ticket is streamed on open, on every cell spent and on close, so
	 * a reader sees the same bounds the writer enforced.
	 */
	public void ticket(String agent, JsonObject pkg) {
		if (agent == null || !AGENT.matcher(agent).matches() || pkg == null) {
			return;
		}
		String state = pkg.has("state") ? pkg.get("state").getAsString() : "";
		String subject = pkg.has("ticket") ? pkg.get("ticket").getAsString() : "axis:ticket";
		String digest = pkg.has("digest") ? pkg.get("digest").getAsString() : "-";
		boolean rejected = "ABSENT".equals(state) || "REFUSED".equals(state);
		// `record` is the ledger's own kind for an accepted fact on a being's stream; `ticket` is
		// not in the sealed schema's allowlist, and a row the schema refuses is not a row.
		ledger.append("record", agent, subject, digest, rejected ? "rejected" : "committed",
				"ticket:" + UUID.randomUUID(), null, pkg);
	}

	/**
	 * An observation lane row: what one being could see, on that being's own feed. A rendering is
	 * not comparable; this is.
	 */
	public void observation(String agent, JsonObject pkg) {
		if (agent == null || !AGENT.matcher(agent).matches() || pkg == null) {
			return;
		}
		if (!MessageEnvelope.isProse(MessageEnvelope.utterance(pkg))) {
			JsonObject refused = pkg.deepCopy();
			refused.addProperty("refused_by", "NotProse");
			ledger.append("record", agent, "axis:observation", "-", "rejected",
					"observation:" + UUID.randomUUID(), null, refused);
			return;
		}
		JsonObject payload = MessageEnvelope.payload(pkg);
		String digest = payload.has("digest") ? payload.get("digest").getAsString() : "-";
		ledger.append("record", agent, "axis:observation", digest, "committed",
				"observation:" + UUID.randomUUID(), null, pkg);
	}

	/**
	 * One step of an exchange on a being's own stream: a request, an acceptance, a hand-over, a
	 * receipt. The row is a §47.2 envelope, so what the stream hands a reader is the sentence and the
	 * typed facts behind it — and the stream is the durable half, unlike the evidence ring.
	 */
	public void social(String agent, String subject, JsonObject envelope) {
		if (agent == null || !AGENT.matcher(agent).matches() || envelope == null) {
			return;
		}
		if (!MessageEnvelope.isProse(MessageEnvelope.utterance(envelope))) {
			JsonObject refused = envelope.deepCopy();
			refused.addProperty("refused_by", "NotProse");
			ledger.append("record", agent, subject, "-", "rejected", "social:" + UUID.randomUUID(), null,
					refused);
			return;
		}
		String digest = envelope.has("content_hash") ? envelope.get("content_hash").getAsString() : "-";
		ledger.append("record", agent, subject, digest, "committed", "social:" + UUID.randomUUID(), null,
				envelope);
	}

	/**
	 * A being's axis cells, closed into one chained package. Thousands of decisions travel as one
	 * record per horizon, not as thousands of rows in the stream.
	 */
	public void axisPackage(String agent, JsonObject pkg) {
		if (agent == null || !AGENT.matcher(agent).matches() || pkg == null || !pkg.has("digest")) {
			return;
		}
		String digest = pkg.get("digest").getAsString();
		ledger.append("record", agent, "axis:decisions", digest, "committed", "axis:" + digest, null, pkg);
	}

	/**
	 * A control program's ticket. The row is committed before the client is asked to move, so an
	 * action with no row is an action that never ran — `proof-no-presenter` on the acting side.
	 */
	public void control(String agent, String program, String ticket) {
		if (agent == null || !AGENT.matcher(agent).matches()) {
			throw new SurfaceRefusal("control-no-presenter", program, "a control action names its presenter");
		}
		ledger.append("record", agent, "control:" + program, "-", "committed", "control:" + ticket, null,
				StreamLedger.detail("ticket", ticket, "program", program, "presenter", agent));
	}

	private void events(HttpExchange exchange) throws IOException {
		String agent = query(exchange, "agent");
		if (agent == null || !AGENT.matcher(agent).matches()) {
			error(exchange, 400, "served-no-agent", "agent", "a feed names whose stream it is; no merged feed is served");
			return;
		}
		int after = 0;
		String query = exchange.getRequestURI().getQuery();
		if (query != null) {
			for (String part : query.split("&")) {
				if (part.startsWith("after=")) {
					try {
						after = Integer.parseInt(part.substring("after=".length()));
					} catch (NumberFormatException ignored) {
						after = 0;
					}
				}
			}
		}
		String accept = exchange.getRequestHeaders().getFirst("Accept");
		if (accept == null || !accept.contains("text/event-stream")) {
			JsonArray rows = new JsonArray();
			for (StreamLedger.Entry entry : ledger.entries()) {
				if (entry.seq() > after && agent.equals(entry.json().get("actor").getAsString())) {
					rows.add(entry.json());
				}
			}
			respond(exchange, 200, rows);
			return;
		}
		exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
		exchange.getResponseHeaders().add("Cache-Control", "no-store");
		exchange.sendResponseHeaders(200, 0);
		OutputStream out = exchange.getResponseBody();
		for (StreamLedger.Entry entry : ledger.entries()) {
			if (entry.seq() > after && agent.equals(entry.json().get("actor").getAsString())) {
				out.write(("data: " + entry.json() + "\n\n").getBytes(StandardCharsets.UTF_8));
			}
		}
		out.flush();
		subscribers.put(exchange, agent);			try {
				while (subscribers.containsKey(exchange)) {
					Thread.sleep(500);
				}
			} catch (InterruptedException interrupted) {
				Thread.currentThread().interrupt();
			} finally {
				subscribers.remove(exchange);
				exchange.close();
			}
	}

	private void broadcast(StreamLedger.Entry entry) {
		byte[] data = ("data: " + entry.json() + "\n\n").getBytes(StandardCharsets.UTF_8);
		String actor = entry.json().get("actor").getAsString();
		for (Map.Entry<HttpExchange, String> subscriber : subscribers.entrySet()) {
			if (!subscriber.getValue().equals(actor)) {
				continue;
			}
			try {
				subscriber.getKey().getResponseBody().write(data);
				subscriber.getKey().getResponseBody().flush();
			} catch (IOException gone) {
				subscribers.remove(subscriber.getKey());
				subscriber.getKey().close();
			}
		}
	}

	private void witness(HttpExchange exchange) throws IOException {
		if (!"POST".equals(exchange.getRequestMethod())) {
			error(exchange, 405, "ledger-method", "witness", "the witness is posted");
			return;
		}
		String body;
		try (InputStream in = exchange.getRequestBody()) {
			body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
		if (body.length() > 8192) {
			error(exchange, 413, "ledger-too-large", "body", "the witness body exceeds 8192 bytes");
			return;
		}
		JsonObject json;
		try {
			json = JsonParser.parseString(body).getAsJsonObject();
		} catch (RuntimeException malformed) {
			error(exchange, 400, "test-shape", "body", "the witness body is a JSON object");
			return;
		}
		String agent = json.has("agent") ? json.get("agent").getAsString() : query(exchange, "agent");
		JsonObject evidence = json.has("evidence") && json.get("evidence").isJsonObject()
				? json.getAsJsonObject("evidence") : json;
		if (!evidence.has("command") || !evidence.has("exit_status") || !evidence.has("checks")) {
			error(exchange, 400, "test-shape", "evidence", "a witness carries command, exit_status and checks");
			return;
		}
		StreamLedger.Entry entry = witness(agent, evidence.get("command").getAsString(),
				evidence.get("exit_status").getAsInt(), evidence.get("checks").getAsInt(),
				evidence.has("surface") ? evidence.get("surface").getAsString() : SURFACE_PATH,
				evidence.has("digest") ? evidence.get("digest").getAsString() : receipt().digest(), null);
		respond(exchange, 200, entry.json());
	}

	private static String query(HttpExchange exchange, String key) {
		String query = exchange.getRequestURI().getQuery();
		if (query == null) {
			return null;
		}
		for (String part : query.split("&")) {
			if (part.startsWith(key + "=")) {
				return part.substring(key.length() + 1);
			}
		}
		return null;
	}

	private boolean authorized(HttpExchange exchange) throws IOException {
		String header = exchange.getRequestHeaders().getFirst("Authorization");
		String presented = header != null && header.startsWith("Bearer ") ? header.substring("Bearer ".length()).trim() : "";
		if (MessageDigest.isEqual(presented.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8))) {
			return true;
		}
		error(exchange, 401, "ledger-unauthorized", "request", "a bearer token is required");
		return false;
	}

	private static void respond(HttpExchange exchange, int status, JsonElement body) throws IOException {
		byte[] bytes = GSON.toJson(body).getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
		exchange.getResponseHeaders().add("Cache-Control", "no-store");
		exchange.sendResponseHeaders(status, bytes.length);
		try (OutputStream out = exchange.getResponseBody()) {
			out.write(bytes);
		} finally {
			exchange.close();
		}
	}

	private static void error(HttpExchange exchange, int status, String code, String subject, String why) {
		try {
			JsonObject error = new JsonObject();
			error.addProperty("code", code);
			error.addProperty("subject", subject);
			error.addProperty("why", why);
			JsonObject body = new JsonObject();
			body.add("error", error);
			respond(exchange, status, body);
		} catch (IOException ignored) {
			exchange.close();
		}
	}

	/** 32 random bytes, owner-only, outside anything served: the token is a path, never a payload. */
	private static String token(Path file) throws IOException {
		if (Files.exists(file)) {
			return Files.readString(file, StandardCharsets.UTF_8).trim();
		}
		if (file.getParent() != null) {
			Files.createDirectories(file.getParent());
		}
		byte[] raw = new byte[32];
		new SecureRandom().nextBytes(raw);
		StringBuilder hex = new StringBuilder(raw.length * 2);
		for (byte b : raw) {
			hex.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
		}
		Files.writeString(file, hex.toString(), StandardCharsets.UTF_8);
		return hex.toString();
	}

	public SurfaceDocument document() {
		return mounted.document();
	}

	public MountReceipt receipt() {
		return mounted.receipt();
	}

	public StreamLedger ledger() {
		return ledger;
	}

	public MongoStreams store() {
		return mongo;
	}
}

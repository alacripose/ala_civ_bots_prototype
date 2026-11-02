package dev.ensouled.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dev.ensouled.EnsouledMod;
import dev.ensouled.surface.SurfaceDocument;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.text.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * The in-game end of the stream: a downstream client of the shell's package streamer.
 *
 * It reads `secure-shell.served/1` for its **own** agent, checks it against the shipped tables, and
 * draws with the game's own toast stack — one stack, the display owner's, no second palette and no
 * second window format. A refusal row is a toast whose title says "Refused"; the client decides
 * nothing about what a refusal means, it draws the row it was served.
 *
 * The witness is the client's own: it runs the checks over the payload it is showing, shows the
 * command, exit status, check count and digest, and posts the result so the server writes the `test`
 * row. The display does not write the ledger; it reports a reading and the writer commits it.
 */
public final class StreamClient {
	private static final Pattern DIGEST = Pattern.compile("^sha256:[0-9a-f]{64}$");
	/**
	 * What the display has already said, so a payload refreshed every few seconds cannot turn one
	 * fact into a queue of identical toasts. Keyed by what was drawn, not by id: the streamer writes
	 * a new row for the same refusal, and it is the same news to the person reading it.
	 */
	private static final java.util.Map<String, Long> DRAWN = new java.util.concurrent.ConcurrentHashMap<>();
	private static final long REPEAT_WINDOW_MILLIS = 120_000;
	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
	private static final int CHECKS = 6;

	private StreamClient() {
	}

	static void onJoin(MinecraftClient client) {
		// The display's own configuration, in the instance it is running from. A client reading
		// `run/stream.token` relative to a game directory it does not share with the server is how
		// the display drifts from the worktree: it has no token, says so once, and draws nothing.
		JsonObject config = config(client);
		String agent = firstNonBlank(string(config, "agent"), client.getSession().getUsername());
		String base = firstNonBlank(string(config, "url"), System.getProperty("ensouled.stream.url"),
				System.getenv("ENSOULED_STREAM_URL"), "http://127.0.0.1:7664");
		String token = firstNonBlank(string(config, "token"), System.getProperty("ensouled.stream.token"),
				System.getenv("ENSOULED_STREAM_TOKEN"));
		if (token.isBlank()) {
			token = readToken(string(config, "token_file"));
		}
		if (token == null || token.isBlank()) {
			EnsouledMod.LOG.warn("[ensouled] stream: no bearer token for {}; refusing to connect (run/stream.token)",
					agent);
			return;
		}
		String url = base;
		String bearer = token;
		Thread thread = new Thread(() -> run(client, url, bearer, agent), "ensouled-stream-client");
		thread.setDaemon(true);
		thread.start();
	}

	/** `config/ensouled-stream.json` in the instance's run directory; empty when absent. */
	private static JsonObject config(MinecraftClient client) {
		try {
			Path file = client.runDirectory.toPath().resolve("config/ensouled-stream.json");
			if (!Files.exists(file)) {
				return new JsonObject();
			}
			JsonElement parsed = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
			return parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
		} catch (IOException | RuntimeException unreadable) {
			EnsouledMod.LOG.warn("[ensouled] stream config could not be read: {}", unreadable.getMessage());
			return new JsonObject();
		}
	}

	private static String string(JsonObject json, String key) {
		return json != null && json.has(key) && json.get(key).isJsonPrimitive()
				? json.get(key).getAsString() : "";
	}

	private static String readToken(String configured) {
		List<Path> candidates = new ArrayList<>();
		if (configured != null && !configured.isBlank()) {
			candidates.add(Path.of(configured));
		}
		candidates.add(Path.of("run", "stream.token"));
		candidates.add(Path.of("stream.token"));
		for (Path file : candidates) {
			try {
				if (Files.exists(file)) {
					return Files.readString(file, StandardCharsets.UTF_8).trim();
				}
			} catch (IOException unreadable) {
				EnsouledMod.LOG.warn("[ensouled] stream token could not be read from {}: {}", file,
						unreadable.getMessage());
			}
		}
		EnsouledMod.LOG.warn("[ensouled] no stream token found (tried {}); set token_file in config/ensouled-stream.json",
				candidates);
		return null;
	}

	private static void run(MinecraftClient client, String base, String token, String agent) {
		try {
			HttpRequest request = HttpRequest.newBuilder(URI.create(base + "/api/served?agent=" + agent))
					.header("Authorization", "Bearer " + token)
					.header("Accept", "application/json")
					.timeout(Duration.ofSeconds(5))
					.GET()
					.build();
			HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				EnsouledMod.LOG.warn("[ensouled] stream refused for {}: HTTP {} {}", agent, response.statusCode(),
						response.body());
				return;
			}
			JsonObject payload = JsonParser.parseString(response.body()).getAsJsonObject();
			witness(client, base, token, agent, payload);
			for (JsonElement notice : payload.getAsJsonArray("notices")) {
				drawNotice(client, notice.getAsJsonObject());
			}
			// The wish plan travels with the payload; a display that reads only the notices is a
			// display that silently drops half of what it was served.
			if (payload.has("wishes") && payload.get("wishes").isJsonArray()) {
				for (JsonElement wish : payload.getAsJsonArray("wishes")) {
					drawWish(client, wish.getAsJsonObject());
				}
			}
			// Resume from the ledger head, not the mount sequence: the chain is the catch-up source.
			subscribe(client, base, token, agent, head(base, token));
		} catch (IOException | InterruptedException | RuntimeException unreachable) {
			if (unreachable instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			EnsouledMod.LOG.warn("[ensouled] stream unreachable for {}: {}", agent, unreachable.getMessage());
		}
	}

	/**
	 * The checks the client runs over the payload it is about to draw. They are the shipped tables'
	 * rules, not this client's opinion; a failure is reported with the same weight as a pass.
	 */
	private static List<String> check(JsonObject payload, String agent) {
		List<String> failures = new ArrayList<>();
		String format = payload.has("format") ? payload.get("format").getAsString() : "";
		if (!"secure-shell.served/1".equals(format)) {
			failures.add("format is '" + format + "'");
		}
		String servedFor = payload.has("agent") ? payload.get("agent").getAsString() : "";
		if (!agent.equals(servedFor)) {
			failures.add("served for '" + servedFor + "', not '" + agent + "'");
		}
		String digest = payload.has("digest") ? payload.get("digest").getAsString() : "";
		if (!DIGEST.matcher(digest).matches()) {
			failures.add("digest '" + digest + "' is not sha256:<64 hex>");
		}
		JsonArray windows = payload.getAsJsonArray("windows");
		for (JsonElement element : windows) {
			JsonObject window = element.getAsJsonObject();
			String lane = window.get("lane").getAsString();
			String rule = window.get("rule").getAsString();
			if (!SurfaceDocument.LANES.contains(lane) || !SurfaceDocument.RULES.contains(rule)) {
				failures.add("window " + window.get("id").getAsString() + " has lane/rule " + lane + "/" + rule);
			}
		}
		List<String> windowIds = new ArrayList<>();
		for (JsonElement element : windows) {
			windowIds.add(element.getAsJsonObject().get("id").getAsString());
		}
		for (JsonElement element : payload.getAsJsonArray("nodes")) {
			JsonObject node = element.getAsJsonObject();
			if (!windowIds.contains(node.get("window").getAsString())) {
				failures.add("node " + node.get("id").getAsString() + " names an undeclared window");
			}
		}
		for (JsonElement element : payload.getAsJsonArray("notices")) {
			JsonObject notice = element.getAsJsonObject();
			if (!notice.has("title") || notice.get("title").getAsString().isBlank()) {
				failures.add("a notice carries no title");
			}
		}
		return failures;
	}

	private static void witness(MinecraftClient client, String base, String token, String agent, JsonObject payload) {
		List<String> failures = check(payload, agent);
		int exit = failures.isEmpty() ? 0 : 1;
		String command = "GET /api/served?agent=" + agent;
		String digest = payload.has("digest") ? payload.get("digest").getAsString() : "sha256:-";
		String surface = payload.has("surface") ? payload.get("surface").getAsString() : "packages/toast/toast.surface";
		EnsouledMod.LOG.info("[ensouled] witness command={} exit={} checks={} surface={} digest={}{}", command, exit,
				CHECKS, surface, digest, failures.isEmpty() ? "" : " failures=" + failures);
		// A person watching the world does not need to be told their stream verified: exit status,
		// check count and a digest are the reader's evidence, and they are in the log and the ledger
		// row. A failure is the only part of this that is news on screen.
		if (exit != 0) {
			toast(client, "Stream refused for " + agent, String.join("; ", failures));
		}
		try {
			JsonObject evidence = new JsonObject();
			evidence.addProperty("command", command);
			evidence.addProperty("exit_status", exit);
			evidence.addProperty("checks", CHECKS);
			evidence.addProperty("surface", surface);
			evidence.addProperty("digest", digest);
			JsonObject body = new JsonObject();
			body.addProperty("agent", agent);
			body.add("evidence", evidence);
			HttpRequest post = HttpRequest.newBuilder(URI.create(base + "/api/witness?agent=" + agent))
					.header("Authorization", "Bearer " + token)
					.header("Content-Type", "application/json")
					.timeout(Duration.ofSeconds(5))
					.POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
					.build();
			HttpResponse<String> response = HTTP.send(post, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				EnsouledMod.LOG.warn("[ensouled] witness refused by the writer: HTTP {} {}", response.statusCode(),
						response.body());
			}
		} catch (IOException | InterruptedException refused) {
			if (refused instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			EnsouledMod.LOG.warn("[ensouled] witness could not be posted: {}", refused.getMessage());
		}
	}

	private static int head(String base, String token) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder(URI.create(base + "/api/ledger/head"))
				.header("Authorization", "Bearer " + token)
				.timeout(Duration.ofSeconds(5))
				.GET()
				.build();
		HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() != 200) {
			return Integer.MAX_VALUE;
		}
		JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
		return body.has("seq") ? body.get("seq").getAsInt() : Integer.MAX_VALUE;
	}

	private static void subscribe(MinecraftClient client, String base, String token, String agent, int after) {
		try {
			HttpRequest request = HttpRequest.newBuilder(
					URI.create(base + "/api/ledger/events?after=" + after + "&agent=" + agent))
					.header("Authorization", "Bearer " + token)
					.header("Accept", "text/event-stream")
					.GET()
					.build();
			HttpResponse<InputStream> response = HTTP.send(request, HttpResponse.BodyHandlers.ofInputStream());
			if (response.statusCode() != 200) {
				EnsouledMod.LOG.warn("[ensouled] stream feed refused for {}: HTTP {}", agent, response.statusCode());
				return;
			}
			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					if (!line.startsWith("data: ")) {
						continue;
					}
					JsonObject entry = JsonParser.parseString(line.substring("data: ".length())).getAsJsonObject();
					drawEntry(client, agent, entry);
				}
			}
		} catch (IOException | InterruptedException closed) {
			if (closed instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			EnsouledMod.LOG.info("[ensouled] stream feed for {} ended: {}", agent, closed.getMessage());
		}
	}

	private static void drawEntry(MinecraftClient client, String agent, JsonObject entry) {
		String actor = entry.has("actor") ? entry.get("actor").getAsString() : "";
		if (!agent.equals(actor)) {
			// The server already filters per agent; this is the display's own refusal to draw a
			// row that is not its own, so a routing mistake cannot become a toast.
			return;
		}
		String kind = entry.has("kind") ? entry.get("kind").getAsString() : "entry";
		String outcome = entry.has("outcome") ? entry.get("outcome").getAsString() : "";
		// Only a refusal is worth a toast. A `test` row is the client's own witness written back to
		// the ledger, and replaying those from the ledger head drew the reader three toasts reading
		// "test / secure-shell-toast" — internal bookkeeping shown as if it were news. The witness
		// toast above is the visible half of that row; the row itself belongs in the ledger.
		if (!"rejected".equals(outcome)) {
			return;
		}
		String subject = entry.has("subject") ? entry.get("subject").getAsString() : "";
		toast(client, "Refused: " + kind, subject);
	}

	/** A wish that is satisfied is not news; one that is missing, differs or is refused is. */
	private static void drawWish(MinecraftClient client, JsonObject wish) {
		String state = wish.has("state") ? wish.get("state").getAsString() : "";
		if ("loaded".equals(state)) {
			return;
		}
		String line = wish.has("line") ? wish.get("line").getAsString() : "";
		String kind = wish.has("kind") ? wish.get("kind").getAsString() : "wish";
		EnsouledMod.LOG.info("[ensouled] wish {} {}", state, line);
		toast(client, "Wish " + state + ": " + kind, line);
	}

	private static void drawNotice(MinecraftClient client, JsonObject notice) {
		String title = notice.has("title") ? notice.get("title").getAsString() : "notice";
		String body = notice.has("body") ? notice.get("body").getAsString() : "";
		boolean attention = notice.has("attention") && notice.get("attention").getAsBoolean();
		EnsouledMod.LOG.info("[ensouled] notice{}: {} - {}", attention ? " (attention)" : "", title, body);
		toast(client, title, body);
	}

	private static void toast(MinecraftClient client, String title, String body) {
		long now = System.currentTimeMillis();
		Long last = DRAWN.get(title + "\u0000" + body);
		if (last != null && now - last < REPEAT_WINDOW_MILLIS) {
			return;
		}
		DRAWN.put(title + "\u0000" + body, now);
		if (DRAWN.size() > 256) {
			DRAWN.entrySet().removeIf(entry -> now - entry.getValue() > REPEAT_WINDOW_MILLIS);
		}
		client.execute(() -> {
			EnsouledMod.LOG.info("[ensouled] toast queued: {} - {}", title, body);
			client.getToastManager().add(new StreamToast(SystemToast.Type.PERIODIC_NOTIFICATION,
					Text.literal(title), Text.literal(body)));
		});
	}

	private static String firstNonBlank(String... values) {
		for (String value : values) {
			if (value != null && !value.isBlank()) {
				return value;
			}
		}
		return "";
	}

	/**
	 * A toast on the game's own stack. The draw is logged once, so the HUD layer's own render is the
	 * witness that the toast was drawn — a screenshot would prove only that something was painted.
	 */
	private static final class StreamToast extends SystemToast {
		private final String drawnTitle;
		private final String drawnBody;
		private boolean drawn;

		StreamToast(Type type, Text title, Text body) {
			super(type, title, body);
			this.drawnTitle = title.getString();
			this.drawnBody = body.getString();
		}

		@Override
		public Visibility draw(DrawContext context, ToastManager manager, long startTime) {
			if (!drawn) {
				drawn = true;
				EnsouledMod.LOG.info("[ensouled] toast drawn by the client HUD: {} - {}", drawnTitle, drawnBody);
			}
			return super.draw(context, manager, startTime);
		}
	}
}

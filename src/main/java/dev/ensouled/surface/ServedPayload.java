package dev.ensouled.surface;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * `secure-shell.served/1` — what a client receives, ported from `secure-shell` `tools/port/served.mjs`.
 *
 * The receipt stays `surface.mount/2`; this is the additive record that carries the windows, the
 * content tree, the evidence rows and the toast feed. Four things are refused here, by name:
 *
 * - `tier-not-derived` — a tier that arrives as a value. A tier is a conclusion, computed on read
 *   from the requirement names and their evidence digests, or absent.
 * - `tier-incomplete` — a requirement with no evidence digest.
 * - `served-no-surface` / `served-no-windows` — a payload a client would draw with nothing in it.
 * - `wish-shape` / `wish-kind` / `wish-state` / `notice-shape` — a row whose shape this reader does
 *   not know. The fork that *built* a plan coerces an unknown state to `missing`; a validator
 *   reading a payload it did not build refuses the same value instead.
 */
public final class ServedPayload {
	public static final String HEADER = "secure-shell.served/1";
	public static final Set<String> WISH_KINDS = Set.of("package", "content", "renderer", "store", "game");
	public static final Set<String> WISH_STATES = Set.of("loaded", "planned", "missing", "differs", "refused");

	private static final Pattern DIGEST = Pattern.compile("^sha256:[0-9a-f]{64}$");

	public record Evidence(String surface, String digest) {
	}

	public record Requirement(String name, List<Evidence> evidence) {
	}

	public record Wish(String id, String kind, String surface, int line, String plan, String state, String why,
			String detail) {
	}

	public record Notice(String title, String body, String window, String layout, boolean attention) {
	}

	public static Requirement requirement(String name, String surface, String digest) {
		return new Requirement(name, List.of(new Evidence(surface, digest)));
	}

	public static Wish wish(String id, String kind, String surface, int line, String plan, String state, String why,
			String detail) {
		return new Wish(id, kind, surface, line, plan, state, why, detail);
	}

	public static Notice notice(String title, String body) {
		return new Notice(title, body == null ? "" : body, null, null, false);
	}

	/**
	 * The rung a set of requirements reaches. The material ladder is not declared in this tree, so
	 * the honest answer is the named refusal — never a placeholder grade and never a floor.
	 */
	public static String deriveTier(List<Requirement> requirements) {
		if (requirements == null || requirements.isEmpty()) {
			throw new SurfaceRefusal("tier-incomplete", "requirements",
					"a tier is derived from the requirements that were checked, and none were named");
		}
		for (Requirement row : requirements) {
			if (row.name() == null || row.name().isBlank()) {
				throw new SurfaceRefusal("tier-incomplete", "requirement", "a requirement is named");
			}
			if (row.evidence() == null || row.evidence().isEmpty()) {
				throw new SurfaceRefusal("tier-incomplete", row.name(),
						"a requirement with no evidence digest is a claim the served payload cannot support");
			}
			for (Evidence entry : row.evidence()) {
				if (entry.digest() == null || !DIGEST.matcher(entry.digest()).matches()) {
					throw new SurfaceRefusal("tier-incomplete", row.name(),
							"evidence digest " + entry.digest() + " is not sha256:<64 hex>");
				}
			}
		}
		throw new SurfaceRefusal("ladder-absent", "packages/axis/material-grade.json",
				"the material ladder is not declared in this tree, so no requirement set reaches a rung");
	}

	public static JsonObject served(SurfaceDocument document, MountReceipt receipt, List<Wish> wishes,
			List<Notice> notices, List<Requirement> requirements, String agent, String carriedTier, Instant at) {
		if (carriedTier != null) {
			throw new SurfaceRefusal("tier-not-derived", carriedTier,
					"a tier that arrives as a value is an assertion the client cannot check; it is computed on read");
		}
		if (document == null) {
			throw new SurfaceRefusal("served-no-surface", "document", "nothing was mounted, so there is nothing to serve");
		}
		if (document.windows().isEmpty()) {
			throw new SurfaceRefusal("served-no-windows", document.id(),
					"a payload with no windows is a blank screen with a receipt");
		}

		JsonArray wishRows = new JsonArray();
		for (Wish wish : wishes) {
			if (wish.id() == null || wish.id().isBlank()) {
				throw new SurfaceRefusal("wish-shape", "id", "a wish is identified by its id");
			}
			if (!WISH_KINDS.contains(wish.kind())) {
				throw new SurfaceRefusal("wish-kind", wish.id(),
						"kind " + wish.kind() + " is not one of " + String.join(", ", WISH_KINDS));
			}
			if (!WISH_STATES.contains(wish.state())) {
				throw new SurfaceRefusal("wish-state", wish.id(),
						"state " + wish.state() + " is not one of " + String.join(", ", WISH_STATES));
			}
			if (wish.surface() == null || wish.surface().isBlank()) {
				throw new SurfaceRefusal("wish-shape", wish.id(), "a wish names the surface it is about");
			}
			if (wish.line() < 1) {
				throw new SurfaceRefusal("wish-shape", wish.id(), "a wish carries the line it was declared on, 1 or greater");
			}
			if (wish.plan() == null || wish.plan().isBlank()) {
				throw new SurfaceRefusal("wish-shape", wish.id(), "a wish states the load it plans");
			}
			JsonObject row = new JsonObject();
			row.addProperty("id", wish.id());
			row.addProperty("kind", wish.kind());
			row.addProperty("surface", wish.surface());
			row.addProperty("line", wish.line());
			row.addProperty("plan", wish.plan());
			row.addProperty("state", wish.state());
			row.addProperty("why", wish.why() == null ? "" : wish.why());
			row.addProperty("detail", wish.detail() == null ? "" : wish.detail());
			wishRows.add(row);
		}

		JsonArray noticeRows = new JsonArray();
		for (Notice notice : notices) {
			if (notice.title() == null || notice.title().isBlank()) {
				throw new SurfaceRefusal("notice-shape", "title", "a notice carries the title the toast shows");
			}
			JsonObject row = new JsonObject();
			row.addProperty("title", notice.title());
			row.addProperty("body", notice.body());
			row.add("window", notice.window() == null ? com.google.gson.JsonNull.INSTANCE
					: new com.google.gson.JsonPrimitive(notice.window()));
			row.add("layout", notice.layout() == null ? com.google.gson.JsonNull.INSTANCE
					: new com.google.gson.JsonPrimitive(notice.layout()));
			row.addProperty("attention", notice.attention());
			noticeRows.add(row);
		}

		JsonArray windows = new JsonArray();
		for (SurfaceDocument.Window window : document.windows()) {
			JsonObject rect = new JsonObject();
			rect.addProperty("x", window.x());
			rect.addProperty("y", window.y());
			rect.addProperty("width", window.w());
			rect.addProperty("height", window.h());
			JsonObject row = new JsonObject();
			row.addProperty("id", window.id());
			row.addProperty("title", window.title());
			row.addProperty("lane", window.lane());
			row.addProperty("rule", window.rule());
			row.add("rect", rect);
			row.add("chrome", tokens(window.chrome()));
			row.add("moves", tokens(window.moves()));
			row.addProperty("content", window.content());
			windows.add(row);
		}

		JsonArray nodes = new JsonArray();
		for (SurfaceDocument.Node node : document.nodes()) {
			JsonObject row = new JsonObject();
			row.addProperty("window", node.window());
			row.addProperty("id", node.id());
			row.addProperty("parent", node.parent());
			row.addProperty("kind", node.kind());
			row.addProperty("role", node.role());
			row.addProperty("value", node.value());
			nodes.add(row);
		}

		JsonArray evidenceRows = new JsonArray();
		for (Requirement requirement : requirements) {
			JsonArray evidence = new JsonArray();
			for (Evidence entry : requirement.evidence()) {
				JsonObject row = new JsonObject();
				row.add("surface", entry.surface() == null ? com.google.gson.JsonNull.INSTANCE
						: new com.google.gson.JsonPrimitive(entry.surface()));
				row.addProperty("digest", entry.digest());
				evidence.add(row);
			}
			JsonObject row = new JsonObject();
			row.addProperty("name", requirement.name());
			row.add("evidence", evidence);
			evidenceRows.add(row);
		}

		JsonObject payload = new JsonObject();
		payload.addProperty("format", HEADER);
		payload.add("wishes", wishRows);
		payload.add("notices", noticeRows);
		payload.addProperty("surface", document.id());
		payload.addProperty("agent", agent);
		payload.addProperty("sequence", receipt == null ? 0 : receipt.sequence());
		payload.addProperty("digest", receipt == null ? SurfaceDocument.sha256(document.id()) : receipt.digest());
		payload.addProperty("at", DateTimeFormatter.ISO_INSTANT.format(at));
		payload.add("windows", windows);
		payload.add("nodes", nodes);
		payload.add("requirements", evidenceRows);
		return payload;
	}

	private static JsonArray tokens(String value) {
		JsonArray array = new JsonArray();
		if (value == null || value.isBlank()) {
			return array;
		}
		for (String token : value.split(",", -1)) {
			array.add(token);
		}
		return array;
	}
}

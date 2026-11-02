package dev.ensouled.surface;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * `dl-enclave.surface/1` as a reader, ported from `secure-shell` `tools/port/surface.mjs`.
 *
 * The vocabularies are the shipped tables' own, copied rather than paraphrased; nothing is renamed
 * and no lane, rule, kind or role is added here. The schema is closed: an unknown row is refused
 * rather than skipped, because a reader that ignores a row mounts a set nobody declared.
 */
public final class SurfaceDocument {
	public static final String HEADER = "dl-enclave.surface/1";

	public static final List<String> LANES = List.of("frame", "control", "tab", "world", "beacon", "motion");
	public static final List<String> RULES = List.of("fill", "full-bleed", "centre", "centre-wide",
			"right-dock", "right-lower", "right-stack", "bottom-strip", "bottom-left", "small-right");
	public static final List<String> THEME_ROLES = List.of("surface", "hud");
	public static final List<String> TYPE_ROLES = List.of("micro", "small", "body", "lead", "title", "display", "mono");
	public static final List<String> NODE_KINDS = List.of("pane", "row", "field", "label", "list", "table", "chart");
	public static final List<String> CAPABILITIES = List.of("observe", "act", "admin");
	public static final List<String> CHROME = List.of("minimize", "maximize", "close");
	public static final List<String> MOVES = List.of("drag", "resize");

	public static final int MAX_WINDOWS = 256;
	public static final int MAX_NODES = 512;
	public static final int MAX_DEPTH = 24;
	public static final int MAX_ACTIONS = 16;
	public static final int MAX_DOC = 1_048_576;

	private static final Pattern ID = Pattern.compile("^[a-z0-9-]{1,64}$");

	public record Window(String id, String title, String lane, String rule, int x, int y, int w, int h,
			int minW, int minH, boolean pinnable, boolean pinned, String chrome, String moves,
			String themeRole, String content) {
	}

	public record Node(String window, String id, String parent, String kind, String role, String value) {
	}

	public record Action(String window, String verb, String tool, String target) {
	}

	private final String id;
	private final String requires;
	private final List<Window> windows;
	private final List<Node> nodes;
	private final List<Action> actions;
	private final List<String> comments;

	private SurfaceDocument(String id, String requires, List<Window> windows, List<Node> nodes,
			List<Action> actions, List<String> comments) {
		this.id = id;
		this.requires = requires;
		this.windows = List.copyOf(windows);
		this.nodes = List.copyOf(nodes);
		this.actions = List.copyOf(actions);
		this.comments = List.copyOf(comments);
	}

	public String id() {
		return id;
	}

	public String requires() {
		return requires;
	}

	public List<Window> windows() {
		return windows;
	}

	public List<Node> nodes() {
		return nodes;
	}

	public List<Action> actions() {
		return actions;
	}

	public List<String> comments() {
		return comments;
	}

	/** Parse and validate a declaration. A refusal names the line it came from. */
	public static SurfaceDocument parse(String raw, String source) {
		String text = raw.replace("\r\n", "\n").replace("\r", "\n");
		List<String> lines = new ArrayList<>(List.of(text.split("\n", -1)));
		if (!lines.isEmpty() && lines.get(lines.size() - 1).isEmpty()) {
			lines.remove(lines.size() - 1);
		}
		int index = 0;
		List<String> comments = new ArrayList<>();
		while (index < lines.size() && lines.get(index).startsWith("#")) {
			comments.add(lines.get(index++));
		}
		if (index >= lines.size() || !lines.get(index).equals(HEADER)) {
			throw new SurfaceRefusal("surface-schema", source, "line " + (index + 1) + ": expected " + HEADER);
		}
		index++;
		String surfaceLine = field(lines, index++, source, "surface");
		String requiresLine = field(lines, index++, source, "requires");
		List<Window> windows = new ArrayList<>();
		List<Node> nodes = new ArrayList<>();
		List<Action> actions = new ArrayList<>();
		for (; index < lines.size(); index++) {
			String line = lines.get(index);
			if (line.isEmpty()) {
				throw new SurfaceRefusal("surface-schema", source, "line " + (index + 1) + ": blank row");
			}
			String[] cols = line.split("\t", -1);
			switch (cols[0]) {
				case "window" -> {
					if (cols.length != 17) {
						throw new SurfaceRefusal("surface-schema", source,
								"line " + (index + 1) + ": window row has " + cols.length + " columns, expected 17");
					}
					String subject = cols[1];
					windows.add(new Window(subject, cols[2], cols[3], cols[4], integer(cols[5], subject),
							integer(cols[6], subject), integer(cols[7], subject), integer(cols[8], subject),
							integer(cols[9], subject), integer(cols[10], subject), bool(cols[11], subject),
							bool(cols[12], subject), cols[13], cols[14], cols[15], cols[16]));
				}
				case "node" -> {
					if (cols.length != 7) {
						throw new SurfaceRefusal("surface-schema", source,
								"line " + (index + 1) + ": node row has " + cols.length + " columns, expected 7");
					}
					nodes.add(new Node(cols[1], cols[2], cols[3], cols[4], cols[5], cols[6]));
				}
				case "action" -> {
					if (cols.length != 5) {
						throw new SurfaceRefusal("surface-schema", source,
								"line " + (index + 1) + ": action row has " + cols.length + " columns, expected 5");
					}
					actions.add(new Action(cols[1], cols[2], cols[3], cols[4]));
				}
				default -> throw new SurfaceRefusal("surface-schema", source,
						"line " + (index + 1) + ": unknown row '" + cols[0] + "'");
			}
		}
		SurfaceDocument document = new SurfaceDocument(surfaceLine, requiresLine, windows, nodes, actions, comments);
		document.validate(source, text);
		return document;
	}

	private static String field(List<String> lines, int index, String source, String key) {
		if (index >= lines.size()) {
			throw new SurfaceRefusal("surface-schema", source, "document ends before its header");
		}
		String[] cols = lines.get(index).split("\t", -1);
		if (cols.length != 2 || !cols[0].equals(key)) {
			throw new SurfaceRefusal("surface-schema", source,
					"line " + (index + 1) + ": expected " + key + "<TAB>value");
		}
		return cols[1];
	}

	private static int integer(String value, String subject) {
		if (!value.matches("^\\d+$")) {
			throw new SurfaceRefusal("surface-schema", subject, "'" + value + "' is not a non-negative integer");
		}
		return Integer.parseInt(value);
	}

	private static boolean bool(String value, String subject) {
		if (value.equals("true")) {
			return true;
		}
		if (value.equals("false")) {
			return false;
		}
		throw new SurfaceRefusal("surface-schema", subject, "'" + value + "' is not true or false");
	}

	/**
	 * Every §2.5 rule that can be checked without a host. The mount calls this before it writes a
	 * receipt, so a refused surface never reaches a client.
	 */
	public void validate(String source, String text) {
		if (!ID.matcher(id).matches()) {
			throw new SurfaceRefusal("surface-id", id, "not an id");
		}
		if (!CAPABILITIES.contains(requires)) {
			throw new SurfaceRefusal("surface-capability", id, "unknown requires '" + requires + "'");
		}
		if (windows.size() > MAX_WINDOWS) {
			throw new SurfaceRefusal("window-count", id, windows.size() + " windows");
		}
		Set<String> windowIds = new LinkedHashSet<>();
		for (Window window : windows) {
			if (!ID.matcher(window.id()).matches()) {
				throw new SurfaceRefusal("surface-id", window.id(), "not an id");
			}
			if (!windowIds.add(window.id())) {
				throw new SurfaceRefusal("duplicate-window", window.id(), "declared twice");
			}
			if (window.title() == null || window.title().isEmpty() || window.title().getBytes(StandardCharsets.UTF_8).length > 64
					|| window.title().indexOf('\t') >= 0 || window.title().indexOf('\n') >= 0 || window.title().indexOf('\r') >= 0) {
				throw new SurfaceRefusal("surface-schema", window.id(), "title is not 1..64 bytes without a tab or newline");
			}
			if (!LANES.contains(window.lane())) {
				throw new SurfaceRefusal("unknown-lane", window.id(), window.lane());
			}
			if (!RULES.contains(window.rule())) {
				throw new SurfaceRefusal("unknown-rule", window.id(), window.rule());
			}
			if (window.minW() > window.w() || window.minH() > window.h()) {
				throw new SurfaceRefusal("geometry-invalid", window.id(), "min > default");
			}
			tokens(window.chrome(), CHROME, window.id(), "chrome");
			tokens(window.moves(), MOVES, window.id(), "moves");
			if (!THEME_ROLES.contains(window.themeRole())) {
				throw new SurfaceRefusal("unknown-theme-role", window.id(), window.themeRole());
			}
			if (!ID.matcher(window.content()).matches()) {
				throw new SurfaceRefusal("surface-schema", window.id(), "content '" + window.content() + "' is not an id");
			}
			if (window.pinned() && !window.pinnable()) {
				throw new SurfaceRefusal("pin-not-pinnable", window.id(), "pinned but not pinnable");
			}
		}
		Set<String> nodeIds = new HashSet<>();
		for (Node node : nodes) {
			String subject = node.window() + "/" + node.id();
			if (!windowIds.contains(node.window())) {
				throw new SurfaceRefusal("surface-schema", subject, "node names an undeclared window");
			}
			if (!ID.matcher(node.id()).matches() || !nodeIds.add(node.id())) {
				throw new SurfaceRefusal("surface-schema", subject, "node id is not an id or is declared twice");
			}
			if (!node.parent().equals("-") && !nodeIds.contains(node.parent())) {
				throw new SurfaceRefusal("unknown-node-parent", subject,
						"parent '" + node.parent() + "' has not been declared");
			}
			if (!NODE_KINDS.contains(node.kind())) {
				throw new SurfaceRefusal("unknown-node-kind", subject, node.kind());
			}
			if (!THEME_ROLES.contains(node.role()) && !TYPE_ROLES.contains(node.role())) {
				throw new SurfaceRefusal("unknown-node-role", subject, node.role());
			}
			if (!node.value().equals("-") && !node.value().startsWith("@")
					&& (node.value().getBytes(StandardCharsets.UTF_8).length > 256
							|| node.value().indexOf('\t') >= 0 || node.value().indexOf('\n') >= 0)) {
				throw new SurfaceRefusal("surface-schema", subject, "literal exceeds 256 bytes or contains a control");
			}
		}
		for (String window : windowIds) {
			long count = nodes.stream().filter(node -> node.window().equals(window)).count();
			if (count > MAX_NODES) {
				throw new SurfaceRefusal("node-count", window, count + " nodes");
			}
		}
		Set<String> actionWindows = new HashSet<>();
		for (Action action : actions) {
			String subject = action.window() + "/" + action.verb();
			actionWindows.add(action.window());
			if (!windowIds.contains(action.window())) {
				throw new SurfaceRefusal("surface-schema", subject, "action names an undeclared window");
			}
			if (!ID.matcher(action.verb()).matches() || !ID.matcher(action.tool()).matches()) {
				throw new SurfaceRefusal("surface-schema", subject, "verb or tool is not an id");
			}
			if (!action.target().equals("-") && nodes.stream().noneMatch(node -> node.window().equals(action.window())
					&& node.id().equals(action.target()))) {
				throw new SurfaceRefusal("surface-schema", subject, "target '" + action.target() + "' is not a row in the window");
			}
		}
		if (text.getBytes(StandardCharsets.UTF_8).length > MAX_DOC) {
			throw new SurfaceRefusal("surface-oversize", id, "over " + MAX_DOC + " bytes");
		}
	}

	private static void tokens(String value, List<String> allowed, String subject, String field) {
		if (value.isEmpty()) {
			return;
		}
		List<String> seen = List.of(value.split(",", -1));
		if (new HashSet<>(seen).size() != seen.size() || seen.stream().anyMatch(token -> !allowed.contains(token))) {
			throw new SurfaceRefusal("surface-schema", subject, field + " '" + value + "'");
		}
	}

	/** `sha256:<hex>` over the declaration's own bytes, the identity a mount receipt carries. */
	public static String sha256(String text) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] out = digest.digest(text.getBytes(StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder(out.length * 2);
			for (byte b : out) {
				hex.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
			}
			return "sha256:" + hex;
		} catch (NoSuchAlgorithmException impossible) {
			throw new IllegalStateException("SHA-256 is required", impossible);
		}
	}
}

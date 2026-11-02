package dev.ensouled.mind;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import dev.ensouled.EnsouledMod;
import dev.ensouled.domain.WorldState;

/**
 * The resident names whose decisions are made outside this JVM.
 *
 * <p>A non-JVM client owns its own mind: it perceives the world through its own connection,
 * chooses what to do, and sends ordinary player input. The server must not also mint a mind for
 * that session, attach a body adapter, or put control packets on a channel the client cannot read —
 * two decision owners on one body is the failure this file exists to prevent.
 *
 * <p>The list is world data, not a build-time roster: {@code world/ensouled/external-brains.txt},
 * one name per line, {@code #} for a comment. Absence of the file means no session is external and
 * the JVM behaves exactly as before. A name is read at join, so a change takes effect for the next
 * session without a rebuild; the file is re-read when its timestamp moves.
 */
public final class ExternalBrains {

	private static String loadedPath = "";
	private static long loadedModified = Long.MIN_VALUE;
	private static Set<String> names = Set.of();

	private ExternalBrains() {
	}

	/** Is this session's mind owned by its own client rather than by this server? */
	public static boolean isExternal(String name) {
		if (name == null || name.isBlank()) {
			return false;
		}
		return names().contains(name.toUpperCase(Locale.ROOT));
	}

	/** The names currently declared external, for a boot line a reader can check. */
	public static Set<String> names() {
		WorldState world = WorldState.get();
		if (world == null) {
			return names;
		}
		Path file = world.directory().resolve("external-brains.txt");
		try {
			long modified = Files.isRegularFile(file) ? Files.getLastModifiedTime(file).toMillis()
					: Long.MIN_VALUE;
			if (file.toString().equals(loadedPath) && modified == loadedModified) {
				return names;
			}
			Set<String> parsed = new LinkedHashSet<>();
			if (Files.isRegularFile(file)) {
				for (String row : Files.readAllLines(file, StandardCharsets.UTF_8)) {
					String line = row.strip();
					if (line.isEmpty() || line.startsWith("#")) {
						continue;
					}
					parsed.add(line.toUpperCase(Locale.ROOT));
				}
			}
			names = Set.copyOf(parsed);
			loadedPath = file.toString();
			loadedModified = modified;
		} catch (IOException unreadable) {
			// A list that cannot be read declares nobody external. The fallback is the old
			// behaviour, not a session silently losing its mind.
			EnsouledMod.LOG.warn("[ensouled] external brain list is unreadable ({}); no session is external",
					unreadable.getMessage());
			names = Set.of();
			loadedPath = file.toString();
			loadedModified = Long.MIN_VALUE;
		}
		return names;
	}
}

package dev.ensouled.harness;

import java.util.Optional;

import net.minecraft.server.MinecraftServer;

/**
 * Control seam for the dev-only acceptance harness. The harness lives in the test source set (a
 * companion mod on the dev server), so the main command tree cannot reference it directly. The
 * companion entrypoint installs {@link Starter} at startup; a server that has no harness answers
 * honestly instead of silently accepting a phase it cannot run.
 */
public final class HarnessControl {
	public interface Starter {
		/** Starts a phase and returns its run id, or null when a phase is already active. */
		String start(MinecraftServer server, String phase, boolean attached);

		/** Cancels the active phase. */
		boolean cancel(MinecraftServer server);

		/** One-line status of the active phase, or null when idle. */
		String status();
	}

	private static Starter starter;

	private HarnessControl() {
	}

	public static void install(Starter value) {
		starter = value;
	}

	public static boolean available() {
		return starter != null;
	}

	public static Optional<String> start(MinecraftServer server, String phase, boolean attached) {
		if (starter == null) {
			return Optional.empty();
		}
		try {
			return Optional.ofNullable(starter.start(server, phase, attached));
		} catch (RuntimeException e) {
			return Optional.empty();
		}
	}

	public static boolean cancel(MinecraftServer server) {
		return starter != null && starter.cancel(server);
	}

	public static Optional<String> status() {
		return starter == null ? Optional.empty() : Optional.ofNullable(starter.status());
	}
}

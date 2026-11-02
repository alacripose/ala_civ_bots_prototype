package dev.ensouled;

import dev.ensouled.entity.EnsouledEntities;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common (client + server) initializer. Phase 1 has no client rendering requirements: the agent
 * body lives entirely in the server simulation.
 */
public final class EnsouledMod implements ModInitializer {
	public static final String MOD_ID = "ensouled";
	public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

	/** The build the running bytecode came from, so a log names the jar it is actually executing. */
	public static String build() {
		return net.fabricmc.loader.api.FabricLoader.getInstance()
				.getModContainer(MOD_ID)
				.map(container -> container.getMetadata().getVersion().getFriendlyString())
				.orElse("unknown");
	}

	@Override
	public void onInitialize() {
		EnsouledEntities.register();
		LOG.info("[ensouled] common init build={}", build());
	}
}

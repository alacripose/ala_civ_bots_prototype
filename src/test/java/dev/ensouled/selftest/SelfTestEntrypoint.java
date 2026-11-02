package dev.ensouled.selftest;

import dev.ensouled.harness.HarnessControl;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/**
 * Dev-only entrypoint for the acceptance harness. The harness lives in the test source set, which
 * the dev server loads as a companion mod (see {@code src/test/resources/fabric.mod.json}); it is
 * never part of the shipped mod.
 */
public final class SelfTestEntrypoint implements DedicatedServerModInitializer {
	@Override
	public void onInitializeServer() {
		HarnessControl.install(SelfTestDriver.control());
		ServerLifecycleEvents.SERVER_STARTED.register(SelfTestDriver::onServerStarted);
		ServerTickEvents.END_SERVER_TICK.register(SelfTestDriver::onServerTick);
	}
}

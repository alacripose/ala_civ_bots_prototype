package dev.ensouled.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The guarantee §A6.4 names: a command that is retried after a dropped acknowledgement must not
 * found the same base twice or spend the same packet twice.
 */
final class WorldStateTest {
	private static WorldIdentity identity() {
		return new WorldIdentity("world:test", 42L, WorldIdentity.SCHEMA, 0L, 0L);
	}

	private static Command command(String key, long expectedRevision) {
		return new Command("cmd:" + key, "world:test", expectedRevision, key, "PutWish", "operator", "text");
	}

	@Test
	void aRepeatedCommandIsAReplayNotASecondChange(@TempDir Path dir) {
		WorldState world = WorldState.at(dir.resolve("domain.json"), identity());
		assertEquals(WorldState.Result.APPLIED, world.accept(command("wish|Ash|bring wood", 0L), 100L));
		assertEquals(1L, world.revision());
		assertEquals(WorldState.Result.REPLAYED, world.accept(command("wish|Ash|bring wood", 1L), 101L));
		assertEquals(1L, world.revision(), "a replay must not move the revision");
	}

	@Test
	void aCommandDecidedAgainstAnOlderRevisionIsRefused(@TempDir Path dir) {
		WorldState world = WorldState.at(dir.resolve("domain.json"), identity());
		assertEquals(WorldState.Result.APPLIED, world.accept(command("first", 0L), 10L));
		// The sender saw revision 0 and the world is now at 1: it cannot know what its change means.
		assertEquals(WorldState.Result.STALE, world.accept(command("second", 0L), 11L));
		assertEquals(1L, world.revision());
	}

	@Test
	void aCommandForAnotherWorldIsRefused(@TempDir Path dir) {
		WorldState world = WorldState.at(dir.resolve("domain.json"), identity());
		Command foreign = new Command("cmd:x", "world:other", 0L, "x", "PutWish", "operator", "text");
		assertEquals(WorldState.Result.WRONG_WORLD, world.accept(foreign, 1L));
		assertEquals(0L, world.revision());
	}

	@Test
	void identityAndRunsSurviveAReopen(@TempDir Path dir) {
		Path file = dir.resolve("domain.json");
		WorldState world = WorldState.at(file, identity());
		world.openRun("agent:ash", "bring me 8 oak logs", 5L);
		world.claimHomebase("agent:ash", "Ash", new dev.ensouled.core.Vec3i(1, 64, 3), 24, 64, 5L);
		String worldId = world.world().worldId();

		WorldState reopened = WorldState.at(file, identity());
		assertEquals(worldId, reopened.world().worldId(), "a world keeps its identity across a reopen");
		assertEquals(1, reopened.runs().size());
		assertEquals(Run.Stage.CONSIDERED, reopened.runs().get(0).stage());
		assertTrue(reopened.homebaseOf("agent:ash").isPresent());
	}
}

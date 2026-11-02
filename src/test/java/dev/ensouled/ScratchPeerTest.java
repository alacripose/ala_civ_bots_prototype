package dev.ensouled;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import dev.ensouled.core.AgentIdentity;
import dev.ensouled.core.AgentMind;
import dev.ensouled.core.EntityStimulus;
import dev.ensouled.core.MessageBus;
import dev.ensouled.core.NeedsState;
import dev.ensouled.core.Phase1GatePolicy;
import dev.ensouled.core.SkillRegistry;
import dev.ensouled.core.SocialSeam;
import dev.ensouled.core.Vec3i;

class ScratchPeerTest {
	private static final class Seam implements SocialSeam {
		private final MessageBus bus = new MessageBus();
		private final Map<String, String> entities = Map.of("uuid-asker", "agent:asker", "uuid-peer", "agent:peer");

		@Override
		public MessageBus bus() {
			return bus;
		}

		@Override
		public Optional<String> agentIdForEntity(String entityUuid) {
			return Optional.ofNullable(entities.get(entityUuid));
		}
	}

	private static AgentMind mind(String id, FakeWorld world, Seam seam) {
		return new AgentMind(new AgentIdentity(id, id, "", "unspecified", 0L, 0L, "t"),
				new Phase1GatePolicy(id, new Vec3i(0, 0, 0), 64, 64), new FakeMotor(world),
				SkillRegistry.phase1(), seam);
	}

	@Test
	void whatDoesThePeerDo() throws Exception {
		dev.ensouled.persist.MindJournal journal = TestJournal.install("scratch-peer");
		Seam seam = new Seam();
		FakeWorld askerWorld = new FakeWorld();
		askerWorld.entities.add(new EntityStimulus("uuid-peer", "minecraft:player", new Vec3i(2, 0, 0), 2.0,
				true, false));
		askerWorld.needs = new NeedsState(8, 20, 300, false, false, 0.0);
		FakeWorld peerWorld = new FakeWorld();
		peerWorld.body = new Vec3i(2, 0, 0);
		peerWorld.entities.add(new EntityStimulus("uuid-asker", "minecraft:player", new Vec3i(0, 0, 0), 2.0,
				true, false));
		AgentMind asker = mind("agent:asker", askerWorld, seam);
		AgentMind peer = mind("agent:peer", peerWorld, seam);
		for (int i = 0; i < 30; i++) {
			askerWorld.tick++;
			asker.tick(askerWorld);
			peerWorld.tick++;
			peer.tick(peerWorld);
		}
		System.out.println("PEERBUS " + seam.bus().log().size());
		peer.ledger().evidence().all().forEach(r -> System.out.println("PEER " + r.kind() + " " + r.outcome()
				+ " :: " + r.detail()));
		TestJournal.uninstall(journal);
	}
}

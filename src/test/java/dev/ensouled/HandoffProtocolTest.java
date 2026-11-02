package dev.ensouled;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.ensouled.core.AgentIdentity;
import dev.ensouled.core.AgentMessage;
import dev.ensouled.core.AgentMind;
import dev.ensouled.core.CanonicalEvent;
import dev.ensouled.core.EntityStimulus;
import dev.ensouled.core.EvidenceKind;
import dev.ensouled.core.MessageBus;
import dev.ensouled.core.NeedsState;
import dev.ensouled.core.Phase1GatePolicy;
import dev.ensouled.core.SkillRegistry;
import dev.ensouled.core.SocialSeam;
import dev.ensouled.core.Vec3i;

/**
 * The social seam between two minds: a hungry being asks a perceived peer, the peer answers from its
 * own inventory, and a refusal reaches the asker as a typed failure instead of a delivery.
 */
class HandoffProtocolTest {
	private static final String ASKER = "agent:asker";
	private static final String PEER = "agent:peer";
	private dev.ensouled.persist.MindJournal journal;

	@BeforeEach
	void openJournal() throws java.io.IOException {
		journal = TestJournal.install("handoff");
	}

	@AfterEach
	void closeJournal() {
		TestJournal.uninstall(journal);
	}

	private static final class Seam implements SocialSeam {
		private final MessageBus bus = new MessageBus();
		private final Map<String, String> entities = Map.of("uuid-asker", ASKER, "uuid-peer", PEER);

		@Override
		public MessageBus bus() {
			return bus;
		}

		@Override
		public Optional<String> agentIdForEntity(String entityUuid) {
			return Optional.ofNullable(entities.get(entityUuid));
		}
	}

	private static AgentMind mind(String agentId, FakeWorld world, Seam seam) {
		AgentIdentity identity = new AgentIdentity(agentId, agentId, "", "unspecified", 0L, 0L, "test");
		return new AgentMind(identity, new Phase1GatePolicy(agentId, new Vec3i(0, 0, 0), 64, 64),
				new FakeMotor(world), SkillRegistry.phase1(), seam);
	}

	private static FakeWorld world(Vec3i body, String peerUuid, Vec3i peerPos) {
		FakeWorld world = new FakeWorld();
		world.body = body;
		world.entities.add(new EntityStimulus(peerUuid, "minecraft:player", peerPos,
				body.distanceTo(peerPos), true, false));
		return world;
	}

	@Test
	void aPeerWithoutTheItemDeclinesAndTheAskerLearnsTheRefusal() {
		Seam seam = new Seam();
		FakeWorld askerWorld = world(new Vec3i(0, 0, 0), "uuid-peer", new Vec3i(2, 0, 0));
		FakeWorld peerWorld = world(new Vec3i(2, 0, 0), "uuid-asker", new Vec3i(0, 0, 0));
		askerWorld.needs = new NeedsState(8, 20, 300, false, false, 0.0);
		AgentMind asker = mind(ASKER, askerWorld, seam);
		AgentMind peer = mind(PEER, peerWorld, seam);

		for (int i = 0; i < 200; i++) {
			askerWorld.tick++;
			asker.tick(askerWorld);
			peerWorld.tick++;
			peer.tick(peerWorld);
		}

		assertEquals(1, seam.bus().log().stream()
				.filter(m -> m.speechAct().equals(AgentMessage.REQUEST)).count(), "one scoped request");
		AgentMessage decline = seam.bus().log().stream()
				.filter(m -> m.speechAct().equals(AgentMessage.DECLINE)).findFirst().orElseThrow();
		assertEquals("item_not_held", decline.reason(), "the peer refuses for its own reason");
		assertTrue(asker.ledger().evidence().all().stream()
				.anyMatch(e -> e.kind() == EvidenceKind.SOCIAL && e.outcome().equals("requested")),
				"the asker recorded that it asked");
		assertTrue(asker.ledger().evidence().all().stream()
				.anyMatch(e -> e.kind() == EvidenceKind.VERIFY && e.outcome().equals("failed")
						&& e.detail().contains("peer_declined")),
				"the refusal reached the asker as a failure, not as a delivery");
		assertEquals(0, asker.ledger().events().count(CanonicalEvent.ITEM_TRANSFERRED),
				"a refusal is never recorded as a transfer");
		assertEquals(1, asker.ledger().stats("request_food").failures());
	}

	@Test
	void aHumanPlayerUuidIsNotAnAddressablePeer() {
		Seam seam = new Seam();
		FakeWorld askerWorld = world(new Vec3i(0, 0, 0), "uuid-human", new Vec3i(2, 0, 0));
		askerWorld.needs = new NeedsState(8, 20, 300, false, false, 0.0);
		AgentMind asker = mind(ASKER, askerWorld, seam);

		for (int i = 0; i < 40; i++) {
			askerWorld.tick++;
			asker.tick(askerWorld);
		}

		assertEquals(0, seam.bus().log().size(), "an unregistered player entity is never addressed");
		// Speaking to itself is a social row in the ledger, so the check is that nothing was
		// *addressed*: no request was put on the bus for the human entity to answer.
		assertTrue(asker.ledger().evidence().all().stream()
				.noneMatch(e -> e.kind() == EvidenceKind.SOCIAL && e.outcome().equals("requested")),
				"no request was addressed to the unregistered player");
	}
}

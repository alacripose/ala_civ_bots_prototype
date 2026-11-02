# Corrective Implementation Directive — Human Observation and Self-Test Isolation

**Priority: Immediate architectural correction**

The self-test harness has introduced a blanket human-player disconnect policy through `SelfTestDriver.guardProbeWorld()`.

This is not an acceptable implementation of test isolation.

The system must support human observation of running agent simulations. The developer/operator must be able to join a bot's test server, watch agent behavior, and inspect the world without having to disable the entire test harness.

## 1. Remove the blanket disconnect policy

The current rule:

`player instanceof FakePlayerBody`

must not be treated as the definition of an authorized agent.

The current implementation disconnects every ordinary player who joins the test server.

Remove this unconditional behavior.

Do not replace it with a hardcoded player-name exception.

## 2. Introduce explicit participant roles

The harness must distinguish:

- Registered agent sessions.
- Authorized human observers.
- Ordinary human participants.
- Unknown or unauthorized test participants.

Agent identity must come from the agent registry and authenticated session binding, not a Java entity subclass.

This is mandatory for the networked-client architecture in Addendum D, where production bots connect using ordinary Minecraft client sessions and appear as ordinary server-side player entities.

An agent's persistent `AgentId` must survive disconnection and reconnection.

## 3. Implement observer mode

An authorized observer must be able to:

- Connect while a self-test is running.
- Watch the agents move and interact with the world.
- Inspect their behavior.
- Observe test progress and failures.
- Remain connected across test phases.

The observer must not:

- Modify test fixtures.
- Manipulate agent inventories.
- Change block or container contents.
- Issue gameplay commands that alter assertions.
- Become an agent or enter the population count.
- Satisfy an agent requirement in a test.

Use server-enforced spectator behavior and explicit permissions. Do not depend on the client voluntarily behaving.

An observer may be moved to an appropriate spectator position or observation region.

## 4. Test isolation must not depend on player absence

Redesign test assertions so they identify and inspect their own test fixtures rather than assuming the entire server contains only agent entities.

Use test-scoped entity, inventory, event, and world-state identification.

Observer connections must be excluded from:
- Agent population counts.
- Agent communication participant counts.
- Agent scheduling.
- Agent task assignments.
- Cooperation acceptance criteria.

Where observer presence could influence chunk activation or another environmental variable, explicitly account for that variable or isolate the observer in a separate observation region.

## 5. Preserve strict test integrity

The objective is not to allow arbitrary players to interfere with tests.

It is to allow authorized observation without granting test-world mutation privileges.

If an unauthorized participant attempts to modify a protected test fixture, refuse that action and record the reason.

Do not silently disconnect the operator simply because they joined.

Strict isolation may remain an optional test policy, but it must be independently configured and must never be the default behavior of the production world.

## 6. Compatibility with Addendum D

The self-test harness must support both:

1. The existing in-process `FakePlayerBody` adapter used by legacy tests.
2. Network-connected bot clients joining through ordinary Fabric player connections.

A connected bot must be recognized by registered agent identity, not by `instanceof FakePlayerBody`.

Do not let legacy test assumptions become production architecture constraints.

## 7. Required acceptance test

Demonstrate this scenario:

1. Start the self-test server.
2. Spawn two registered bot clients.
3. Start a real agent interaction or cooperative task.
4. Connect an ordinary human operator.
5. Place the operator in observer mode.
6. Verify the operator remains connected and can observe the bots.
7. Verify the bots continue their task.
8. Verify the observer is not counted as an agent.
9. Verify the observer cannot change protected test fixtures.
10. Verify test results remain valid with the observer connected.

Also demonstrate that a normal human participant can join outside strict test execution.

## 8. Architectural ownership

`SelfTestDriver` may manage test execution and test fixture isolation.

It must not unilaterally redefine who is allowed to join the Minecraft server.

Player access, agent identity, observer permissions, and simulation participation belong to their respective systems.

**The purpose of self-tests is to make agent behavior observable and verifiable. Preventing the operator from watching the simulation defeats that purpose.**
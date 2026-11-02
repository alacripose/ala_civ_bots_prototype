# Corrective Architecture Directive — Persistent Interactive Development Server and Non-Disruptive Self-Tests

**Target:** Minecraft 1.20.1 / Fabric  
**Related architecture:** `minecraft_ensouled_system1_cua_architecture_v3.md`, Addendum C (`minecraft_fake_player_first_addendum.md`), and Addendum D (`minecraft_shared_server_bot_client_population_addendum.md`).  
**Priority:** Correct the self-test/development workflow before expanding test harness behavior.

## 1. Problem being corrected

The current self-test workflow has two undesirable behaviors:

1. It adds a no-op `watcher` fake player rather than allowing the developer's real Minecraft player to remain in the world and participate normally.
2. Each self-test invocation launches another server instance with a newly generated empty superflat world. The developer must reconnect manually to every new instance and loses continuity of the world being observed and manipulated.

Neither behavior satisfies the intended development workflow.

The intended workflow is a **single, persistent, locally running Minecraft server and world**. The developer joins that server using an ordinary Minecraft client and remains free to watch, inspect, interact with, and deliberately interfere with the running tests. Self-tests run against that already-running server and must not own or replace its lifecycle.

This directive supersedes any earlier suggestion to solve the problem with a watcher fake player, spectator mode, read-only observer permissions, or mandatory separation of the operator from the test world.

## 2. Non-negotiable desired behavior

- There is one operator-selected development server process.
- There is one persistent development world/save for the interactive development session.
- The developer joins as an ordinary human player, not as a fake player, observer bot, spectator, or agent.
- The developer remains connected while test phases start, finish, and change.
- The developer may inspect and freely modify the world, including test fixtures. No spectator restrictions or special read-only permissions are imposed by this harness.
- Running a self-test must not automatically stop, restart, replace, re-create, or wipe the development server or world.
- Running another test phase must not require the developer to reconnect.
- No fake `watcher` entity is spawned. The operator's actual client is the observer and may also be an active participant.
- Test isolation is scoped to test-owned fixtures and evidence, not enforced by removing the developer from the server or sanitizing the entire world.

## 3. Separate server lifecycle from test lifecycle

The server process and its world belong to the development session. A test suite is a workload executed *inside* that session.

### 3.1 Persistent development server

Provide a development-server mode that:

- Starts the target Fabric server once.
- Loads a stable, named world/save directory, rather than a newly generated temporary superflat world for each test invocation.
- Leaves the server running after an individual test phase passes, fails, or is cancelled.
- Preserves normal player connections, agent entities, inventory, world changes, and server state across test phase boundaries.
- Can be stopped only through an explicit operator action, normal server shutdown, or a genuine unrecoverable process failure.

The server must not be a child process that the test script unconditionally tears down at the end of each phase.

### 3.2 Test execution attaches to the running server

The self-test launcher must become a client/control interface to the existing server process. It must not launch a second Minecraft server as an implicit side effect.

The implementation may use a server command, authenticated local control channel, RCON-like interface, or another project-owned control mechanism. Whichever mechanism is selected must:

- Identify the already-running target server unambiguously.
- Submit a named test/phase request to that server.
- Return a run identifier and status.
- Expose progress, evidence, assertion failures, and completion.
- Support cancellation without terminating the server.
- Prevent accidental submission to an unrelated production server.

If no development server is running, the test command should report that fact and explain how to start the intended development server. It must not silently create a fresh server/world.

### 3.3 Unit tests remain separate

Ordinary Gradle/JUnit unit tests should continue to run without needing to boot or reset a Minecraft server. They must not be confused with in-world integration/self-tests.

Maintain three distinct operations:

1. **Unit tests:** JVM-level tests; no game server lifecycle.
2. **Interactive integration tests:** execute against the already-running persistent development server/world.
3. **Optional isolated CI tests:** may launch a disposable server and disposable world explicitly for reproducible automation.

Only the third mode may own a temporary server process and freshly generated test world, and it must be explicitly requested/configured. It must never be the default behavior of the developer's interactive test command.

## 4. Remove the fake watcher concept entirely

Do not create a fake player called `watcher`, `observer`, or any equivalent no-op identity to stand in for the human developer.

The human developer is already present through their normal Minecraft client connection. The server must preserve that human player as a normal player entity with ordinary gameplay capability.

Remove any logic that:
- Spawns a fake watcher to represent the operator.
- Counts the watcher as a citizen, bot, or population member.
- Uses a watcher to satisfy an agent-count or observation assertion.
- Requires the developer to enter spectator mode.
- Prevents the developer from modifying the world or test fixtures.
- Disconnects or teleports the developer because a self-test is active.

The population registry must count persistent registered agent identities, not human operators, no-op watcher entities, or arbitrary connected clients.

## 5. Test fixtures must be local and owned—not a reason to replace the world

The current whole-world sanitation strategy (launching a fresh empty superflat world) is inappropriate for the interactive development server.

Replace it with test-scoped fixtures.

Each test run should have:
- A unique `TestRunId`.
- A named phase/scenario.
- An explicit set of fixture entities and fixture locations.
- The initial conditions it requires.
- The exact world objects and state it owns or creates.
- A list of expected state transitions and evidence.
- A cleanup policy limited to objects the test itself owns.

Possible fixture strategies include a bounded test region, a reserved test plot, a test-built structure, or a dedicated test dimension if the project chooses to maintain one. The choice must not force the entire development world to be empty, superflat, or regenerated.

### 5.1 Ownership and cleanup

Track fixture ownership explicitly. A test may clean up its own temporary entities, blocks, inventories, and markers when cleanup is requested and safe.

It must not:
- Delete or regenerate the whole world as routine cleanup.
- Reset unrelated player builds or inventories.
- Delete ordinary agent property or persistent agent memory.
- Assume that every chest, block, or entity in the world belongs to the active test.
- Restore a global snapshot that silently erases developer activity outside the fixture boundary.

Where exact rollback cannot be done safely, preserve the world and report the remaining fixture state rather than performing broad destructive cleanup.

### 5.2 Test preconditions and contamination

Tests must establish and record their own preconditions. They cannot rely on a globally pristine world.

The developer is explicitly allowed to interfere with a test. If their intervention changes an assertion's expected inputs or outcomes, the test should:

- Observe and record the resulting world state.
- Preserve the action's actor/provenance where available.
- Mark the affected assertion as passed, failed, or invalidated according to its actual postcondition and the test's stated purpose.
- Report that the fixture was externally changed when that is relevant.
- Continue or stop according to the scenario's explicit policy, without disconnecting or restraining the developer.

Do not label every developer interaction as a security violation. This is an interactive development environment where manual intervention is intentional and useful.

Tests that need deterministic, untouched fixtures may allocate their own separate bounded fixture area. They still do not get to prohibit the developer from playing elsewhere in the world.

## 6. Human activity and agent identity remain separate

The developer's player entity is a human participant. It is not assigned an `AgentId`, `AgentMind`, bot slot, or population replacement credit.

A human may:
- Watch agents work.
- Talk to agents when that feature exists.
- Give agents tasks through supported interfaces.
- Move blocks, containers, and items.
- Build or destroy things.
- Interfere with an active test to inspect how the system responds.

The test harness should record enough context to explain test outcomes, but must not convert the human into a simulated citizen or an artificial observer.

For network-connected bots under Addendum D, actual bot clients join the same persistent server. The server-side player entity for a bot is identified through the authenticated agent-session binding/registry, not by requiring it to be an instance of the legacy in-process `FakePlayerBody` class.

## 7. Test continuity and world continuity

Test phase boundaries are not server lifecycle boundaries.

When a phase ends:
- The server stays online.
- The world remains loaded and saved normally.
- The human player stays connected.
- Existing bots remain connected unless the tested behavior explicitly disconnects them.
- Agent identity and mind persistence remain governed by the agent persistence system.
- The next phase may start against the current world or provision its own fixture.

A test may explicitly request a restart test, crash-recovery test, clean-world test, or disposable-world test. Those are named scenarios with explicit lifecycle ownership—not incidental behavior of every test invocation.

Do not automatically restart between `player`, `phase2`, `verbs`, `handoff`, or future scenarios simply because the current shell script was written around one server boot per phase. Migrate the harness so multiple scenarios can be submitted to one live server process. Where an existing scenario fundamentally requires a fresh process, keep it as a separately named isolated/restart scenario and do not make the developer repeat that lifecycle for ordinary observation.

## 8. Progress visibility

The developer must be able to see what the tests are doing in the same world they are watching.

Expose:
- Current active test and `TestRunId`.
- Current phase and scenario.
- Fixture location/identity.
- Agent identities involved.
- Current test step and expected postcondition.
- Actual observed result.
- Test evidence and typed failure/refusal.
- Whether a human action changed a relevant fixture.
- Pass, fail, invalidated, cancelled, or blocked status.

Progress may be visible through server logs, in-world messages, an operator command, or a local control UI. It must not require a dummy fake player.

## 9. Explicit command/workflow contract

The exact Gradle and script task names must be inspected in the repository before implementing command changes. The following is the required workflow contract, not a claim that these exact commands already exist:

1. Start the persistent development server once using the project's Fabric 1.20.1 run configuration.
2. Join that server with the ordinary human Minecraft client.
3. Start or connect bot clients as required by the current milestone.
4. Invoke a named self-test phase through the control interface. The invocation attaches to the running server; it does not launch another one.
5. Watch and freely interact with the world while the scenario runs.
6. Invoke another phase without restarting the server or reconnecting the human client.
7. Stop the server only when the operator chooses to do so.

If an isolated CI scenario is needed, invoke it through a visibly separate command/configuration that clearly states it will start a disposable server and world.

## 10. Required acceptance tests for the harness correction

The harness correction is not complete until the following are demonstrated:

### A. No synthetic watcher
- Start the persistent development server.
- Verify no fake watcher/observer entity is created.
- Verify the human player's UUID is not registered as an agent and does not count toward bot population.

### B. Human can join and interact normally
- Join as the real human player while a test is running.
- Verify the server does not disconnect, spectator-convert, or specially immobilize the human.
- Verify the human can move, inspect, place/break blocks, open containers, and otherwise perform ordinary gameplay actions.

### C. Repeated test runs do not relaunch the server
- Record server process identity, world identity, and human connection.
- Run one self-test phase.
- Run a second phase without stopping the server.
- Verify the server process and world/save remain the same and the human connection remains active.

### D. World does not reset between phases
- Place a known marker block and store a known inventory/world-state marker outside the test fixture.
- Run at least two integration test phases.
- Verify the markers and unrelated world state remain intact.

### E. Fixture-scoped behavior
- Run a test that provisions its own chest/block/entity fixture.
- Verify the test operates only on its declared fixture.
- Verify cleanup, when requested, affects only fixture-owned resources.

### F. Human interference is permitted and observable
- While a test is running, deliberately alter a fixture as the human player.
- Verify the action is allowed.
- Verify the test reports the changed precondition/postcondition or records the resulting state honestly.
- Verify no automatic kick, forced spectator mode, rollback of unrelated world state, or hidden watcher substitution occurs.

### G. Unit-test separation
- Run ordinary Gradle unit tests.
- Verify they do not launch or terminate the interactive server.

### H. Explicit disposable mode
- Run the separately configured isolated CI integration mode.
- Verify it uses a disposable world and cleans up only its own process/directory.
- Verify it cannot silently attach to or overwrite the interactive development world.

## 11. Implementation discipline

Before changing code, inspect the actual Gradle tasks, `scripts/selftest.sh`, `SelfTestDriver`, server launch configuration, world directory selection, and test fixture lifecycle. Document the current process tree and world-save path.

Then implement the smallest architecture change that separates server ownership from test execution. Do not invent command names or assume a control interface already exists.

Do not replace the watcher with another synthetic entity. Do not implement spectator mode. Do not impose fixture write-protection on the operator. Do not solve persistence by disabling tests. Do not keep one server per test phase as the default interactive workflow.

Preserve existing unit-test coverage and in-world acceptance evidence. Tests should be adapted to attach to the persistent development server, not deleted or weakened to make the new workflow pass.

## 12. Required completion report

Report:
- Which server process owns the interactive world.
- The exact persistent world/save directory.
- How test commands attach to that process.
- Whether the human remains connected between phases.
- Confirmation that the watcher fake player was removed.
- Confirmation that ordinary human gameplay is unrestricted in the development world.
- How fixture ownership and cleanup work.
- Which tests run in-process, against the persistent server, or in an explicitly disposable CI server.
- Evidence from at least two successive test phases run without server restart or human reconnection.

**The development world is a persistent place where the operator and agents coexist. Self-tests are experiments conducted in that world—not disposable worlds that replace it, and not environments from which the developer must be excluded.**

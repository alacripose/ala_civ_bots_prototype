# Ensouled Civilization — Minecraft 1.20.1

A persistent, embodied Minecraft inhabitant driven by a **non-LLM System-1 controller**, built to
the plan in [`minecraft_ensouled_system1_cua_architecture_v3.md`](minecraft_ensouled_system1_cua_architecture_v3.md)
(§17 / §31 for cognition, Addendum B for the entity-AI transition) and
[`minecraft_fake_player_first_addendum.md`](minecraft_fake_player_first_addendum.md) — the primary
citizens are **fake players**, not mobs.

**Runtime is Minecraft 1.20.1 / Fabric**, per an explicit project instruction. Addendum B's
provisional 1.21.1 + NeoForge target is therefore not used; the architecture it describes is
loader-independent and this slice implements its **mod-owned civilian** stage.

> **Notice — Java 25 and Fabric Loader 0.20.1 (explicit override).** The client and the dev server
> run **Fabric Loader 0.20.1** on **JDK 25.0.4**. Minecraft 1.20.1 is only certified on Java 17, and
> 1.20.1 client mods are built for it, so this combination is **not supported upstream**: it works
> because the client launcher sets *Skip Java compatibility checks* (a Prism Launcher option, not a
> Fabric flag) and the server takes an explicit JVM. Point the build at a different runtime with
> `server_java_home` in [`gradle.properties`](gradle.properties); `runServer` logs
> `[build] runServer on <path>` when it uses it.
>
> The **build** stays on JDK 21 (`org.gradle.java.home`) because Gradle 8.8's Groovy compiler cannot
> read Java 25 class files — with `JAVA_HOME` at 25 the build fails with *"Unsupported class file
> major version 69"*. Bytecode is still emitted at Java 17, which is what the 1.20.1 runtime loads.
> Moving the build itself to 25 needs a Gradle upgrade first; that is a separate change.
>
> The forked **server** runs on **generational ZGC** (`-XX:+UseZGC`): the tick loop is
> latency-sensitive and pauses must not scale with the heap. Heap comes from `server_max_heap`
> (default 4G) or `ENSOULED_SERVER_XMX`. The full mod pack is remapped at every cold boot, so the
> server also needs the desktop's free memory: orphaned headless clients are the first thing to
> look for when `RuntimeModRemapper` reports `OutOfMemoryError: Java heap space`. The effective JVM
> is printed as `[build] runServer on <path>` plus the launch arguments in `logs/devserver.log`.
>
> The bot profile no longer carries `ModernUI`: it throws
> `Immediate context has not been created yet` in a headless client with no window.
>
> `Create: New Age` is **disabled** — moved to `mods/disabled/`, not deleted — on the dev server, the
> shared bot profile and the Prism instance. Under loader 0.20.1 it registers
> `create_new_age:geiger_counter` in `minecraft:sound_event` on the dedicated server and not on the
> client (`CreateNewAge.initialize` calls `CNASounds.load()` inside `catch (NoClassDefFoundError)`, so
> whichever side hits a missing class first silently loses that registration), and
> `fabric-registry-sync` then refuses **every** join with `RemapException: Received a registry entry
> that is unknown to this client`. On the Prism client the same failed login surfaced one packet
> earlier as a `CommandTreeS2CPacket` decode error, so the two symptoms are one cause. Restore the jar
> only alongside a loader that registers that sound symmetrically on both sides.

This repository currently delivers the plan's **Phase 1 vertical slice** (§17 / §31), Addendum B
Phase 2 and Addendum C §C.1, plus the first Addendum C Phase 5 slice: fake-player citizens with
local senses, a single decision owner, a procedural gate, player-semantics actions, minds that
survive a server restart, and two citizens settling one real item transfer through a structured
request/accept exchange. A mob-body adapter from the earlier slice is still present behind the same
seam. A being can ask a nearby human for a stack and wait for that human to drop it
(`request_from_human@1`), and agent-to-agent speech acts are echoed to connected players as
`[ensouled]` system lines. Those are interim diagnostics, not native chat: Addendum F requires a real
connected bot client to send and receive ordinary player chat, which is not built. The human ask→drop
→stow loop was verified with a real client on the persistent dev server on 2026-10-31; see
[`docs/IMPLEMENTATION_STATUS.md`](docs/IMPLEMENTATION_STATUS.md). Containers beyond a single
chest/GUIs, trust and relationships, society, economy, story, shadow mode, vanilla-AI takeover and
population scheduling are not built yet.

## What is in the box

| Layer | Package | Responsibility |
|---|---|---|
| Cognition core (no Minecraft imports) | `dev.ensouled.core` | `AgentMind` owns one body's decision cycle (sense → reflex → select → gate → act → verify); `MindLedger` owns everything it remembers and cites (episodes, canonical events, evidence, the practice log (skill counters are replayed from it)); `SkillLineage` derives each verb's first practice, its discovery ticket and the boots it survived; `MindPersistence` owns the save tree; plus beliefs, needs, intention FSM, arbitration, reflex list and the procedural gate |
| Skills | `dev.ensouled.skills` | `harvest_tree@1`, `eat_if_hungry@1`, the pair `request_food@1` / `give_food@1` that settles one food handoff between two citizens, the pair `request_supplies@1` / `supply_from_chest@1` that settles one storage-backed resource transfer, and `request_from_human@1` that asks a nearby human and waits for that human's own drop, all as typed state machines |
| World interaction | `dev.ensouled.interact` | the reusable contract for world objects: perceived identity and reach (`InteractionTarget`), requested operation (`InteractionRequest`), server result and observed postcondition (`InteractionOutcome`), the gated path a skill uses (`InteractionSession`), the protection hook (`OwnershipBoundary`) and the headless chest adapter (`ChestInteraction`). The chest is the first implementation, not the contract — see [`docs/WORLD_INTERACTION.md`](docs/WORLD_INTERACTION.md) |
| Participants | `dev.ensouled.participant` | who a connected session is, resolved from the agent registry rather than a class: agent or ordinary human participant. A human is never refused, disconnected, spectated or restricted by this classification |
| Social seam | `dev.ensouled.core` | `AgentMessage` (one speech act with provenance, expiry and delivery state), `MessageBus` (per-agent inboxes, shared by every mind in the world), `SocialSeam` (bus + which perceived player entities are registered agents — a human is never addressed as a peer) |
| Body adapters | `dev.ensouled.entity`, `dev.ensouled.body` | fake-player body (silent connection, real break/pickup packets) and mob body, behind one `BodyAdapter` seam; each adapter steps its own simulation through `tickBody()`, so `AgentManager` has no per-kind branch; perception-limited world view; motor primitives |
| Runtime | `dev.ensouled.mind`, `dev.ensouled.persist` | mind↔body binding, save/restore of minds in the world save |
| Tooling | `dev.ensouled.command`, `dev.ensouled.harness` | `/ensouled` commands. The acceptance harness is dev-only: `src/test/java/dev/ensouled/selftest`, declared as a companion mod by `src/test/resources/fabric.mod.json` and loaded by `runServer`. It is not in the mod jar. `scripts/devserver.sh` + `scripts/attach.sh` run a phase inside the persistent dev server; `scripts/selftest.sh` remains the disposable CI mode on its own port and universe. `tools/parity_check.py` answers "is the jar on disk the jar this worktree compiles?" from the version baked into each installed jar's `fabric.mod.json`; `scripts/bots.sh sync` puts the built mod into the shared client mods directory and refuses a launch when that directory holds another build, or two builds at once. |

The core never receives a global game-state summary: it only sees `WorldView`, which is limited to
a 16-block sight radius with a line-of-sight raycast, the body's own HUD/proprioception, and its own
inventory. Utility only ranks candidates; `ProceduralGate` decides permission, and every skill
success must be confirmed by an observed world or inventory change.

**A mounted package can be replaced while the server runs.** [`packages/toast/toast.surface`](packages/toast/toast.surface)
is the declaration the client's toast stack draws. `POST /api/package/mount` (same bearer token as
the stream) re-reads it, re-validates it against [`toast.port.json`](packages/toast/toast.port.json)
and its own rules, mounts it as the next `surface.mount/2` sequence and appends that row to the
ledger — no jar rebuild and no restart, which is why a refusal is a ledger row of its own and the
previous revision stays served. The other half is `tools/parity_check.py`: the jar itself must be
the build this worktree compiles, in `run/mods`, the bot profile and every Prism instance that
carries the mod. `scripts/client.sh start` installs that jar into the developer's own instance
(its directory read from the launcher config, not assumed) and opens the client on it.

## Requirements

- JDK 17 or newer (developed and verified on Temurin 21)
- The pinned Gradle wrapper (8.8) — no system Gradle needed
- Minecraft 1.20.1 + Fabric loader 0.16.14 (downloaded by Loom on first build)

## Build and run

```bash
./gradlew build          # compiles, remaps and runs the unit suite
./gradlew test           # core acceptance tests only
./gradlew runServer      # dev dedicated server (first run creates run/)
./gradlew runClient      # dev client with the humanoid renderer
```

A dev server needs `run/eula.txt` containing `eula=true`; `scripts/selftest.sh` creates it.

## Using it

```
/ensouled spawnplayer <name>   materialize a fake-player citizen where you stand
/ensouled spawn <name>         spawn the secondary mob-body being instead
/ensouled list                 one line per being, with current intention
/ensouled inspect <name>       identity, needs, counters, last evidence
/ensouled memory <name>        episodes and canonical events
/ensouled evidence <name>      the decision/gate/motor/verify trail
/ensouled save                 persist all minds now
/ensouled humanprobe           meet a being that will ask you, in chat, for two oak_log
/ensouled harness <phase>      run an acceptance phase in this server (operator; server stays up)
/ensouled harness status       current phase and run id, or "harness idle"
/ensouled harness cancel       cancel the active phase without stopping the server
```

### Human player in the loop (`HUM-01`)

Join the development server with your own ordinary Minecraft client (no spectator, operator or fake
observer needed), stand in the open — no natural log within seven blocks of the ground beside you, or
the command names the log and asks you to move — and run:

```
/ensouled humanprobe
```

It places a chest beside you, puts two `oak_log` in your inventory, and spawns Ash. Ash sees you, sees
that its own chest is short, and says what it needs (an `[ensouled]` system line for now — native
player chat is `CHAT-02`). Press **Q** twice beside Ash; it picks
the logs up by touch, walks back to that chest, deposits them, and records the social exchange plus
one transfer event. Check it with `/ensouled inspect Ash` and `/ensouled evidence Ash`.

The command refuses the setup if another candidate would outrank the request (a harvestable trunk
inside seven blocks); that is a declared precondition, not a hidden failure. **Status: verified with
a real client on the persistent dev server (2026-10-31)** — the recorded evidence is in
`docs/IMPLEMENTATION_STATUS.md`.

Agent-to-agent speech is echoed as a clearly tagged system line, not player chat, e.g.
`[ensouled] Ash -> Bram: request 2 oak_log (msg:6)`. Addendum F requires agents to send genuine
player chat from connected clients; that is `CHAT-01`/`CHAT-02` and is not built yet.

## Development modes (`DEV-01`)

There are two modes and they own different things. Neither one can delete a world it did not create.

**Interactive (default for development)** — one persistent server, one persistent `run/world`, your
client stays connected, phases run inside that process:

```bash
JAVA_HOME=... ./scripts/devserver.sh          # start once; owns run/world; enables local RCON control
# join with your normal client, then in another shell:
JAVA_HOME=... ./scripts/attach.sh chest       # attach a phase; server, world and sessions stay up
JAVA_HOME=... ./scripts/attach.sh handoff     # another phase, same server, no reconnect
```

Attached phases use a fixture plot at `spawn + 128/+128` (force-loaded for the run, released after),
spawn their own citizens there, and record what they created in `run/ensouled-harness-owners.txt`
so the next attached run removes only those bodies. Fixture blocks and the plot stay in the world;
nothing outside the plot is cleared or reset. The control channel is RCON on `127.0.0.1:25575` with a
random per-session password in `run/rcon.password`; it is only bound locally for this dev server.

**Disposable CI** — explicitly requested, boots its own server on port 25579 and stores its world in
`run/selftest-universe`, which is the only directory it will ever delete (it refuses without the
`.harness-owned` marker that it writes itself):

```bash
JAVA_HOME=... ./scripts/selftest.sh chest    # disposable universe; safe while the dev server runs
JAVA_HOME=... ./scripts/selftest.sh handoff  # same, one boot per invocation
JAVA_HOME=... ./scripts/selftest.sh player   # two boots: fake-player harvest + restart continuity
JAVA_HOME=... ./scripts/selftest.sh verbs    # one boot: jump, equip, mine with a tool, use/place
JAVA_HOME=... ./scripts/selftest.sh mob      # secondary mob body
```

`scripts/selftest.sh` no longer touches `run/world` in any way, and a leftover trigger file cannot
make a normal dev server auto-run a phase and stop itself (`ENSOULED_SELFTEST=1` is required).

## Verifying the Phase 1 claim

```bash
JAVA_HOME=... ./gradlew test                 # 12 core tests: gate refusals, verification, restart, reflex, arbitration, handoff refusal
```

The harvest harness builds a tree fixture (harness setup, not an agent action), spawns one being, and
requires a verified canonical collection event with a real inventory delta and a memory episode;
the second boot requires the same identity and history and forbids a duplicate collection event for
the same site. The `verbs` harness hands the body a pickaxe and a block (fixture), then drives
`jump`, `equip`, `mine_block` and `use` through the body's own motor and requires observed effects:
Y rises, the held item changes, a stone block is broken with the pickaxe taking durability, and a
dirt block is placed at the clicked face with the stack shrinking. `scripts/selftest.sh` also pins
the probe world (`level-seed`, `generate-structures=false`): village houses contain log blocks the
being legitimately harvests, which would otherwise make the continuity assertion depend on world
generation. The `handoff` harness spawns two citizens (a hungry one with no food, one holding a
single loaf), lets the hungry one ask through the message bus, and requires the recipient's own
answer, a real dropped stack, the pickup, a verified eat, and **exactly one** canonical transfer
event — recorded by the recipient from its own inventory delta. The `chest` harness gives each
citizen a real chest, lets the one whose own storage is short state the requirement, and requires
the supplier to read its chest through the gated `inspect`, withdraw through the contract, drop the
stack, and the recipient to walk back to *its* chest and deposit: one `container.withdrawn`, one
`container.deposited`, one `item.transferred` under one transfer id, both containers and both
inventories verified against the world, and typed refusals for an out-of-reach chest and an
unsupported operation. The probes create and interact with their own fixtures and do not spawn any
human, observer or watcher session; joining a probe run as a human is not part of the harness.
Results land in `run/ensouled-selftest-result.txt`.
Last recorded results:

```
SELFTEST PASS phase=player1 harvest events=1 inventory_oak_log=3 remembered=true body=player evidence=16
SELFTEST PASS phase=phase2 restart continuity kind=player site=6,-60,0 events=1 alive=true
  new_mine_commands=0 inventory=3->3 intention=idle
SELFTEST PASS phase=verbs verbs body=player jump=+0.42 equip=iron_pickaxe mine=stone_removed
  tool_durability=0->1 use=placed_at_3,-60,0 dirt=8->7 motor_evidence=10
SELFTEST PASS phase=handoff handoff citizens=2 request=msg:1 accept=msg:2 transfers=1 eats=1
  food=6->17 supplier_bread=1->0 asker_bread=0
SELFTEST PASS phase=chest chest citizens=2 transfer=msg:1 source=4->2 sink=0->2
  drawer_inventory=0 storer_inventory=0 withdrawals=1 deposits=1 transfers=1
  conserved=true refusals=not_reachable,unsupported_operation
```

## Design invariants kept by this slice

1. No language model participates in sensing, deciding, speaking or acting.
2. One decision owner per body; skills and reflexes cannot seize the hands concurrently.
3. The gate is a set of predicates with typed verdicts and stable action hashes — not a score.
4. The body is mod-owned: no vanilla goal selector competes with System-1 for movement or hands
   (Addendum B §34.1, §36.4). A fake player's position moves only through the shared player physics,
   its block breaking only through player action packets, its held item only through the slot-selection
   packet, and its placing only through the interaction manager (Addendum C §C.4).
5. The world is the authority; an action is only "done" when the world says so.
6. Identity and memory outlive the process; a restart never silently replaces a being.
7. Perception is locality-limited; the mind never receives facts a player could not acquire.


# Binding Implementation Directive — Prevent Phase-1 Completion Drift

## Primary Objective

The purpose of this project is to implement a complete System-1-driven fake-player civilization for Minecraft, with fake players as the primary embodied agents.

The project is NOT to deliver a perfectly tested fake-player proof of concept.

Phase 1 exists to establish the minimum verified foundation required for subsequent implementation. It is not the final product, and its test suite must not become an indefinite development objective.

## 1. Phase Boundaries Are Binding

Every phase must have:
- A defined implementation objective.
- A defined exit contract.
- A bounded set of acceptance tests.
- Explicitly deferred functionality.
- A successor implementation ticket.

Do not continuously expand a phase's acceptance criteria after implementation begins.

Newly discovered requirements must be classified as one of:
- Current phase blocker.
- Successor phase requirement.
- Deferred hardening work.
- Optional enhancement.
- Unrelated issue.

Only genuine current-phase blockers prevent phase exit.

## 2. Definition of a Critical Blocker

A defect is a phase blocker when it demonstrates that the current phase's required capability cannot work, or that proceeding would create a serious architectural or safety failure.

Examples:
- Fake players cannot reliably spawn or be tracked.
- Agent identity is corrupted or duplicated.
- Server-authoritative actions cannot be executed or observed.
- Actions bypass required authority checks.
- Persistence causes identity loss or data corruption.
- The implementation creates an unrecoverable server failure.

Examples of issues that do NOT automatically block Phase 1:
- Unimplemented inventory interactions.
- Incomplete dialogue or communication systems.
- Incomplete social relationships.
- Unimplemented coordinated work.
- Incomplete combat or crafting behavior.
- Unimplemented population scaling.
- Compatibility with unrelated mods that have not yet been targeted.
- Performance optimization for workloads not yet implemented.

These are future implementation requirements, not reasons to indefinitely extend the first phase.

## 3. Bounded Verification

Tests must prove the phase's stated capabilities.

Do not create additional tests merely to increase test coverage without a demonstrated requirement.

When a test exposes a defect:
1. Establish whether it violates the current exit contract.
2. If it does, fix and verify the defect.
3. If it does not, record it in the appropriate successor ticket or backlog.
4. Do not silently expand the phase's scope.

Preserve existing regression tests. Do not delete or weaken tests to create a passing result.

Do not confuse passing tests with completed product functionality.

## 4. Mandatory Phase Advancement

When all critical exit criteria pass:

1. Record the evidence.
2. Record outstanding limitations and their classifications.
3. Mark the phase complete.
4. Activate the successor ticket.
5. Begin the next implementation objective.

Do not wait for every imaginable edge case to be tested.

Do not reopen a completed phase for non-blocking improvements unless new evidence demonstrates a critical architectural defect.

Further hardening may continue in dedicated tickets without preventing unrelated downstream implementation.

## 5. Fake-Player-First Implementation Priority

The implementation priority is binding:

1. Fake-player embodiment.
2. Fake-player-to-fake-player communication.
3. Fake-player-to-human-player communication and interaction.
4. Coordinated real-world Minecraft actions.
5. Persistent cognition, memory, relationships, and independent simulation.
6. City-builder and civilization integration.
7. Population scaling.
8. Mob adapters.

Do not substitute mob AI work for fake-player functionality.

Do not substitute test infrastructure, abstract interfaces, mock simulations, or documentation for the real in-world implementation when the next milestone requires working game behavior.

## 6. Require Observable Product Progress

Every implementation cycle must deliver at least one of:
- A working in-game capability.
- A necessary verified foundation for the next capability.
- A concrete defect correction that restores a required capability.

Testing and architectural work are valuable, but they must remain subordinate to implementation of the actual system.

Prefer end-to-end vertical progress over endlessly expanding isolated test coverage.

## 7. Phase-1 Exit and Successor Work

Phase 1's purpose is to prove that the selected fake-player approach is viable.

It must not attempt to implement every eventual player capability.

Once its bounded embodiment contract passes, proceed to communication and social interaction.

The next meaningful product milestone is two fake players and one human player cooperating on an actual Minecraft task, with communication, physical actions, and observed outcomes.

This outcome is more important than exhaustive testing of a fake player that cannot yet participate in the civilization.

## 8. Reporting Requirement

At every phase boundary report:

- What working capability was implemented.
- What evidence demonstrates it works.
- Which critical criteria remain unsatisfied.
- Which discovered issues were deferred and why.
- What the next implementation ticket is.
- What actual in-game behavior the next ticket will deliver.

Do not report a phase as complete solely because its tests pass. Do not report a phase as incomplete solely because future-phase functionality has not been implemented.

**The objective is to build the system. Verification exists to make implementation trustworthy, not to become a substitute for implementation.**
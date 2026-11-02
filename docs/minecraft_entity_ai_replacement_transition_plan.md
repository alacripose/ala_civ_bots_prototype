# Minecraft Entity AI Replacement: System-1 / CUA Transition Plan

**Document type:** staged implementation roadmap and technical transition specification
**Status:** proposal; not an implementation report or verified mod patch
**Provisional target:** Minecraft Java 1.21.1 + NeoForge 21.1.x, based on the previously observed project runtime. Lock the exact NeoForge patch, mappings, modpack and Java 21 runtime before implementation.
**Companion architecture:** `minecraft_ensouled_system1_cua_architecture_v3.md`, especially Sections 22–33 and Addendum B.

## Executive decision

Replace the **decision layer** of enrolled entities, not Minecraft's whole entity engine. Keep the engine responsible for body physics, collision, damage, equipment, effects, persistence, chunk lifecycle, networking and rendering. The civilization System-1 controller owns perception, memory, drives, skill selection, social behavior and high-level motor intent. A CUA/body adapter translates bounded motor intents into actual Minecraft movement and interactions. Axis and the Governor remain separate authorities for action admissibility and evidence.

The phrase “not player-based” must be implemented precisely. Java simulation distance still controls ordinary chunk/entity ticking. The new System-1 scheduler must continue agents' cognition, identity, economic participation, social state and plans independently of player proximity. Distant agents use a regional/virtual representation; they do not secretly mine blocks, run redstone, open chests or simulate physics inside unloaded chunks. When a physical act is needed, the system requests bounded chunk activation, materializes the same identity and executes through the real game world.

The transition is intentionally incremental and fake-player-first: first prove a persistent fake-player body and its lifecycle, then player-like movement/interactions, one System-1-controlled fake player, two-agent communication/cooperation, human-player interaction, and the city-builder vertical slice. Only then begin GoalSelector and Brain mob adapters. Do not globally disable all vanilla mob AI as the first patch.

---

## Roadmap at a glance

| Phase | Deliverable | Do not proceed until |
|---|---|---|
| 0 | Lock target runtime; dedicated-server harness and real-player parity tests | Exact game/NeoForge build, mappings, Java, mod list and test world are recorded |
| 1 | Persistent player-agent identity and FakePlayer lifecycle spike | Stable unique identity resolves to one valid body per active level; cleanup/restart works |
| 2 | Active fake-player embodiment | Body is tracked/visible as intended, ticks safely, and is not misrepresented as a connected human |
| 3 | Player CUA motor and basic interaction suite | Player movement, inventory, item use, breaking, placement, combat and observations pass parity tests |
| 4 | One System-1-controlled fake player | Cognition selects skills and controls the player CUA without an LLM in every gameplay loop |
| 5 | Multiple fake players and communication | Agents communicate, coordinate and complete a tangible shared task through real bodies/items |
| 6 | Human-player interaction | Chat, offers, cooperation, trade and refusals work with identity/consent/authority boundaries |
| 7 | City-builder production/logistics vertical slice | A full work chain is completed by fake players through real Minecraft actions |
| 8 | Player-independent scheduler and virtual/regional continuity | Cognition persists away from human players; no physical work is claimed in unloaded chunks |
| 9 | Population scale, persistence and mod compatibility | Repeatable benchmarks, recovery and multiplayer tests pass |
| 10 | First GoalSelector-based mob adapter | Player-agent behavior remains unchanged; mob ownership and rollback are proven |
| 11 | Brain-based and remaining mob adapters | Each mob family has its own verified type-specific adapter |

## 34. The actual transition: take over decisions, not the whole entity engine

The first architectural rule is that *entity AI* and *entity simulation* are not the same subsystem.

The intended replacement target is the entity's decision-making and behavior-selection layer: what it notices, what it wants, which action it selects, how it coordinates with other agents, and which movement/interactions it attempts. The proposal is **not** to rewrite Minecraft's complete `Entity` or `LivingEntity` lifecycle, collision system, damage system, equipment, effects, persistence, rendering, networking or world rules.

Minecraft entities are composite runtime objects. NeoForge's entity documentation distinguishes the entity subclass, its registered `EntityType`, and its renderer, and notes that the entity class itself contains most instance behavior. It also distinguishes `Entity#tick`, `Entity#baseTick` and `Entity#rideTick`; `baseTick` contains common lifecycle behavior such as fire, freezing, swimming and portal handling, while `LivingEntity` adds living-state updates such as drowning and damage tracking. Those systems are not disposable simply because the decision-maker changes. [NeoForge entity lifecycle reference](https://docs.neoforged.net/docs/1.21.5/entities/) — the page currently documents 1.21.5, so its API details must be rechecked against the target 1.21.1 source before coding.

The desired ownership split is:

```text
MINECRAFT / NEOFORGE OWNS
  entity construction and registration
  world and dimension membership
  collision, gravity, fluids and physical movement resolution
  health, damage, equipment, effects and ordinary combat resolution
  interaction results and block/entity mutation APIs
  entity persistence, NBT and network replication
  client rendering, animation state and sounds
  chunk lifecycle and world save/load

THE CIVILIZATION SYSTEM OWNS
  persistent person/creature identity and continuity
  sensing policy and local belief formation
  needs, drives, affect, memory and relationships
  System-1 action selection and motor-skill selection
  work, household, production, logistics and civic intentions
  social acts and inter-agent coordination
  campaign/contract/ticket interpretation
  regional/distant simulation and wake/materialization policy
  the decision to request a physical action through the body adapter

AXIS / GOVERNOR OWNS
  evidence classification and proof requirements
  action-specific admissibility and current authority
  scope and authorization enforcement
  audit/refusal recording and independent validation
```

The civilization system does not become a second physics engine. Minecraft remains the physical world. The civilization layer becomes the mind and the population scheduler. The key test is that a controlled agent can make a decision without vanilla AI, but the action still resolves through ordinary game mechanics and produces verifiable world outcomes.

### 34.1 Why “turn all mobs to no-AI” is not the transition plan

A global `noAI` switch or indiscriminate removal of every `Goal` is not enough. It may stop useful vanilla behavior, may not cover all entity-specific logic, and does not by itself supply a replacement perception, action, movement or persistence loop. It also risks disabling the wrong behavior: movement, navigation, target selection, schedules, breeding, panic, work-site behavior, or cleanup logic can be spread across class overrides and multiple subsystems.

The transition must inventory and intercept the decision paths for each supported entity family. It must prove that exactly one controller owns behavior at a time:

```text
VANILLA  -> vanilla decision system owns the entity
SHADOW   -> vanilla still owns behavior; new cognition observes and records only
SYSTEM1  -> new cognition owns decisions; vanilla decision behavior is suppressed
FALLBACK -> controlled, explicit safe behavior while a required subsystem is unavailable
```

There must never be a `VANILLA + SYSTEM1` state in which two independent decision systems send movement, target or interaction commands to the same body. Nor should a missing adapter silently fall back to vanilla after the agent has transitioned to System-1; that would produce a split-brain body.

### 34.2 Three distinct meanings of “not player-limited”

The implementation must separate three mechanisms often described loosely as “activation range”:

1. **Minecraft Java simulation distance and chunk ticking.** In Java Edition, simulation distance controls which chunks receive entity/game-mechanic ticks around players. An entity in an unloaded or non-entity-ticking chunk does not continue behaving as an ordinary, fully simulated Minecraft body. The chunk system and ticket types determine whether the chunk is loaded and which processing level it receives. The Minecraft Wiki's Java Edition documentation explains the player-simulation region and chunk ticket restrictions; treat it as secondary technical reference and validate details against 1.21.1 source. [Simulation distance](https://minecraft.wiki/w/Simulation_distance), [chunks](https://minecraft.wiki/w/Chunks).
2. **Server/plugin activation range.** Some server implementations add separate entity activation or throttling policies. Do not assume a Paper/Purpur activation-range setting exists in a NeoForge server or that changing it alters vanilla Java chunk ticking.
3. **Agent cognitive scheduling.** The proposed System-1 scheduler is new and should not use distance to the nearest player as its primary eligibility rule. An agent's cognition, identity, relationships, economic participation and long-horizon intentions continue through the mod's own world scheduler even when no player is nearby.

The project wants the third mechanism, and it may want a controlled replacement for selected parts of the first. These are related but not interchangeable. Merely changing the AI selection code does not make an unloaded Minecraft chunk tick. Merely forcing a chunk to load does not provide an efficient large-world population model.

### 34.3 The two-body-state model

Every person/entity has one persistent simulation identity and may have one of two physical representations:

- **Materialized body:** an actual Minecraft entity instance inside a loaded, entity-ticking world region. It participates in real collision, movement, damage, fluid, interaction and replication. The System-1 motor drives that body through the registered adapter.
- **Virtual/regional body:** a persistent agent record representing identity, coarse position, needs, intentions, relationships, inventory claims, route progress and other state that can legitimately advance without a loaded physical entity. It is not an invisible Minecraft entity and it cannot mine blocks, open a chest, grow crops, move a minecart or trigger redstone in an unloaded chunk.

A person can transition between these representations without being created, destroyed, killed or assigned a new identity. This is a representation change, not a life-cycle event.

```text
persistent agent record
       |
       +-- region is physically active and action requires body
       |       -> materialize existing identity as a Minecraft entity
       |       -> run full body adapter / normal world physics
       |
       +-- region is not physically active
               -> maintain virtual state and region-level work
               -> do not claim block-level effects that did not run
               -> request bounded chunk activation when a physical act is due
```

This is the only scalable way to separate population simulation from player proximity without making the whole Minecraft world permanently active. If the design instead requires every entity's full vanilla physics, every block entity, redstone, fluids, random ticks and block updates to continue everywhere, then those chunks must be kept ticking and the associated cost must be budgeted honestly. The mod must not claim that virtual advancement is identical to full Minecraft simulation.

## 35. Required technical inventory before player-body integration or mob AI suppression

Before writing the takeover hook, build an executable inventory of the exact target game's behavior.

### 35.1 Lock the runtime

Record and hash:

- Minecraft version and server jar;
- exact NeoForge build (1.21.1-21.1.x patch);
- official/mapped names and mappings used by the Gradle workspace;
- Java 21 runtime vendor/build;
- complete mod list and versions, including entity, AI, pathfinding, worldgen, economy, claims and performance mods;
- server configuration: `simulation-distance`, `view-distance`, `entity-broadcast-range-percentage`, spawn limits, tick settings, gamerules and difficulty;
- relevant datapacks and tags;
- the exact entity type registry ids to be controlled.

The server must be tested as a dedicated server, not only in an integrated single-player session. NeoForge explicitly recommends dedicated-server testing because physical client and server environments differ and client-only references can crash a server. [NeoForge sides](https://docs.neoforged.net/docs/1.21.1/concepts/sides/), [Getting Started](https://docs.neoforged.net/docs/1.21.1/gettingstarted/).

### 35.2 Create a player-body action atlas first; create mob behavior atlases second

The first atlas must compare a real player with the intended fake-player body. Record the actor identity, action entry point, validation checks, server-side events, result, inventory/world deltas, replication, and lifecycle effects for movement, item use, block interaction, mining, placement, entity interaction, combat, pickup/drop, container use, damage, death/respawn, and dimension travel. The atlas must expose fake-connection and GUI/menu limitations instead of assuming they match a connected client.

Only after the player-body atlas passes should each proposed mob type receive its own entity behavior atlas. For each mob type, inspect the target-version source and record:

| Concern | Questions the atlas must answer |
|---|---|
| Decision selector | Does it use `GoalSelector`, one or more selectors, `Brain`, or custom subclass logic? |
| Perception | Which `Sensor`, sensing cache, target conditions, range checks and world queries are used? |
| Movement | Which `PathNavigation`, `MoveControl`, `LookControl`, `JumpControl`, flying/swimming controllers or direct velocity writes are involved? |
| Schedule | Does behavior depend on world time, schedule, activity, memories, profession, POIs or work sites? |
| Targeting | How are targets acquired, validated, remembered, forgotten and synchronized? |
| Interactions | Which methods perform door use, item pickup, food use, breeding, trading, work, attacks, block griefing or other effects? |
| Lifecycle | What ages, heals, starves, despawns, splits, breeds, dies, converts, teleports or changes dimension? |
| Persistence | Which NBT fields and entity data accessors are authoritative? Which values are transient? |
| External hooks | Which NeoForge events and other mods observe or modify this entity? |
| Client presentation | Which synchronized fields drive pose, animation, equipment, particles, sounds and nameplate? |

The atlas must identify *decision logic* separately from *body/lifecycle logic*. A villager, for example, is not interchangeable with a zombie simply because both extend `Mob`: a villager's schedules, POIs, profession, memory and behavior structures create additional state and transition obligations. A mob family adapter must name all these dependencies before it can claim control.

### 35.3 Establish the baseline

For at least one representative instance of every entity family, capture:

- real-player vs fake-player movement and action parity for the supported player action set;
- player-list/entity-tracking/replication and fake-connection behavior;
- target acquisition and loss for any later mob-family baseline;
- interaction and inventory behavior;
- save/unload/reload behavior;
- behavior when the player leaves simulation distance;
- behavior in a forced/ticking chunk with no nearby player;
- server tick time with 1, 10, 50 and 100 entities;
- entity count and allocation/profile data;
- all applicable log lines and unfiltered test outcomes.

This baseline is a record, not a goal to reproduce every vanilla decision. It gives the transition a measurable reference for what must be retained or intentionally replaced.

## 36. The replacement architecture

### 36.1 Core modules

The proposed runtime should be split into these modules so one subsystem cannot silently absorb another:

```text
agent_registry
  persistent AgentId <-> live Minecraft Entity UUID <-> region record

entity_adapter_registry
  EntityType -> adapter id -> supported AI hooks -> lifecycle contract

vanilla_ai_ownership
  VANILLA / SHADOW / SYSTEM1 / FALLBACK ownership state
  targeted goal/brain suppression and restoration policy

perception
  local block, entity, sound/event, inventory, POI and social observations
  timestamped beliefs with source and confidence provenance

system1
  drive activation, candidate selection, learned action policy
  deterministic reflex and interruption rules

skill_runtime
  trained/composed sensorimotor abilities; typed prerequisites and outcomes

motor / cua_body
  movement, look, jump, interact, attack, equip, use, pickup and wait controls
  entity-specific translation into Minecraft controls

regional_simulation
  chunk/region partitions, virtual position, needs, work progress, route and event updates

world_bridge
  actual block/entity/inventory observations and validated world actions
  no omniscient shortcut into agent belief state

population_scheduler
  event-driven wakeups, cognition budget, fairness, priority and regional queues

axis_gateway
  action envelope, evidence/procedure checks, authority decision and refusal record

chronicle
  append-only events, memory references, causal story and save/recovery records
```

### 36.2 Entity enrollment record

Enrollment must be explicit and durable. The exact schema should be governed before implementation; this is a design sketch, not a finalized canonical format:

```json
{
  "agent_id": "stable-person-id",
  "entity_uuid": "minecraft-entity-uuid-or-null",
  "entity_type": "minecraft:villager",
  "adapter": "minecraft-1.21.1/villager-adapter-v1",
  "ai_owner": "shadow",
  "control_epoch": 1,
  "representation": "materialized",
  "region_id": "overworld/chunk-region-key",
  "controller_schema": "system1-agent/1",
  "last_world_observation": 0,
  "last_committed_action": null,
  "last_authoritative_sequence": 0
}
```

The record must not trust a self-reported `ai_owner` flag from entity NBT alone. At server load and every transition, the authoritative registry checks the entity type, adapter compatibility, control epoch, schema and save provenance. If identity or adapter resolution fails, the entity is held in a named safe state; it is not silently assigned a different mind.

### 36.3 Exactly one decision owner

A takeover operation is a controlled transaction:

1. identify the live entity and its stable `AgentId`;
2. validate the exact entity type and adapter version;
3. capture the current vanilla-owned state needed for reversible transition or rollback;
4. establish the System-1 agent record and its sequence number;
5. disable/suppress the exact vanilla decision components that the adapter owns;
6. activate the System-1 controller at a known server-tick boundary;
7. verify that the vanilla selector/brain no longer produces movement or action writes for this body;
8. verify the System-1 motor is the sole writer;
9. append the ownership-transition event and evidence.

If any step fails, the transition does not partially apply. It either restores vanilla ownership with the same identity and state, or refuses to proceed while holding the entity in a safe, observable state. The system must never leave an entity with both controllers active or neither controller known.

### 36.4 Narrow interception, not a replacement world patch

For mod-owned entities, prefer an entity subclass with explicit System-1 integration and no unnecessary vanilla decision registrations. For existing vanilla entities, create per-type adapters and use the narrowest supported interception point that the exact 1.21.1 source permits. Depending on the class, that may involve targeted mixins/access transformers or controlled `Mob`/`Brain` hooks. This must be proven from the decompiled target-version sources; do not assume a public event can cancel or replace every AI call.

NeoForge's event system supports observing and modifying many game events, but events only cover the hooks actually exposed. `EntityJoinLevelEvent` fires when an entity enters a level; its 1.21.1 Javadoc warns that it can fire before the underlying `LevelChunk` is promoted to `ChunkStatus.FULL`, so it must not synchronously trigger unsafe world/chunk interactions. [EntityJoinLevelEvent 1.21.1 Javadoc](https://aldak.netlify.app/javadoc/1.21.1-21.1.x/net/neoforged/neoforge/event/entity/entityjoinlevelevent). Use it for enrollment detection and defer world-sensitive operations until a safe server-tick phase.

Do not build a global mixin that bypasses `Mob.serverAiStep` for every mob before one supported type has passed parity and rollback tests. A compatibility adapter is the unit of takeover.

### 36.5 Preserve physical movement and interaction

System-1 should select *skills* and produce typed motor intents; it should not directly teleport entities or mutate inventories/blocks to pretend that an action happened.

Example motor boundary:

```text
MoveInput(forward, lateral, sprint, crouch)
LookInput(yaw_delta, pitch_delta)
JumpInput(pressed)
UseInput(hand, pressed, target_hint)
AttackInput(hand, pressed, target_hint)
HotbarSelect(slot)
Wait(duration_or_condition)
```

The target hint is a perception-side reference, not permission to modify that target. The adapter must resolve the live target, range, line of sight, hand, inventory, interaction preconditions and current world state on the server thread. An action resolves through Minecraft's own collision and interaction systems. A successful button/input dispatch is not a successful gameplay result; the controller observes the consequence and records the outcome separately.

For pathfinding, first evaluate whether the existing entity's `PathNavigation` and movement controls are still appropriate as a physical primitive. If they are used, the project must explicitly distinguish **path following** from **goal selection**: vanilla navigation may execute a route chosen by System-1, but it must not secretly create a second high-level intention. If navigation only ticks from a vanilla AI method that is being suppressed, the adapter must provide a tested, exactly-once navigation update or use another motor implementation. Do not simply call navigation twice or call the entire `Entity#tick` manually.

## 37. Decoupling the civilization clock from Minecraft's player-based entity ticking

### 37.1 One world, two clocks

The plan introduces a civilization scheduler that runs from the server's logical tick independent of the currently nearby player count. It does not try to force every Minecraft entity object to execute its ordinary entity tick manually.

```text
MINECRAFT WORLD CLOCK
  server tick
  chunk/block-entity/physics/entity processing where Minecraft permits it

CIVILIZATION CLOCK
  global population scheduler
  person cognition and affect updates
  job/contract/ticket progress
  production/logistics/civic simulation
  region-level event and memory updates
  materialization requests and action queues
```

The two clocks communicate through explicit observations and action results. The civilization clock can keep an agent's identity, needs, social state, plans and regional activities progressing even when Minecraft would not run that entity's normal AI due to simulation distance. But it cannot claim that the entity has physically walked through an unloaded chunk or that a furnace/redstone circuit/crop/block entity changed while its chunk did not tick.

### 37.2 Schedule from the server, not from each mob's vanilla AI call

On the provisional NeoForge 1.21.1 target, use the server/level tick event phase as the scheduler entry point after verifying the chosen phase and event semantics in the exact patch. NeoForge 1.21.1 Javadocs document `ServerTickEvent.Pre/Post` as once per server tick, with the post event running after server work, and `LevelTickEvent.Pre/Post` as once per level tick. [`ServerTickEvent.Pre`](https://aldak0.ru/javadoc/1.21.1-21.1.x/net/neoforged/neoforge/event/tick/servertickevent.pre.html), [`ServerTickEvent.Post`](https://aldak.netlify.app/javadoc/1.21.1-21.1.x/net/neoforged/neoforge/event/tick/servertickevent.post), [`LevelTickEvent.Pre`](https://aldak.netlify.app/javadoc/1.21.1-21.1.x/net/neoforged/neoforge/event/tick/leveltickevent.pre).

Use a single scheduler per server/level partition, not one independent busy-loop per entity. The initial version should execute state mutation on the logical server thread. Pure computations over immutable snapshots may later be parallelized, but no worker thread may directly call `ServerLevel`, mutate an entity, edit a block, open an inventory, or touch chunk state. Any parallel candidate result returns to the server thread with the snapshot/version it was based on; the server revalidates before committing.

### 37.3 Event-driven cognition, budgeted execution

An agent does not need to reconsider its entire life every 50 ms. The scheduler wakes agents for relevant events:

- a survival need crosses a threshold;
- a hazard is sensed or a threat is reported;
- an assigned job becomes actionable or blocked;
- a route edge becomes unavailable;
- an inventory, reservation or delivery changes;
- a relationship/social event occurs;
- a scheduled sleep/work/leisure transition is reached;
- an action succeeds, fails or is interrupted;
- a settlement service or production process changes state;
- an expedition or contract reaches a gate.

The System-1 reflex layer handles urgent short-latency conditions. The intention layer updates less frequently and when events invalidate its assumptions. Campaign-level plans remain precompiled horizons, not a model call on every tick. This preserves the V9/Bonsai pattern: slow planning creates a route; the tick spends it; local changes can interrupt it deterministically.

The population scheduler needs explicit budgets: maximum decision work per tick, per-agent minimum service/fairness, per-region limits, max outstanding motor commands, max event fan-out, and bounded catch-up after stalls. The values must be measured; do not claim a number of agents until the target modpack and hardware are profiled.

### 37.4 Physical chunk policy: demand, not global force-loading

Do not force-load every settlement or every agent's chunk. A broad always-ticking world defeats the purpose of moving cognition out of vanilla's per-entity loop and can consume more server time than the original design.

Instead, create a bounded `PhysicalRegionManager` with:

- the set of currently loaded / entity-ticking regions;
- a queue of actions that require actual world interaction;
- a per-dimension and global chunk-ticket budget;
- a priority policy for urgent physical work;
- a deadline and timeout for chunk activation;
- a release policy when work completes;
- telemetry for loaded/ticking chunks attributable to the mod.

NeoForge 1.21.1 exposes a `TicketController` with a `forceChunk(..., add, ticking)` API; its Javadoc explains that `ticking=true` requests full chunk ticking even without a nearby player. [`TicketController` 1.21.1 Javadoc](https://aldak.netlify.app/javadoc/1.21.1-21.1.x/net/neoforged/neoforge/common/world/chunk/ticketcontroller). This is a useful targeted mechanism for bounded work, not a reason to keep the entire civilization loaded. The exact ticket ownership, persistence and unload lifecycle must be tested on the actual server.

Natural spawning is a separate policy: Minecraft's spawn-placement checks and spawn pipeline have their own constraints and event hooks. NeoForge 1.21.1's `MobSpawnEvent` documents the spawn placement, position-check and finalize-spawn flow, then `EntityJoinLevelEvent` at world entry. [`MobSpawnEvent` 1.21.1 Javadoc](https://aldak.netlify.app/javadoc/1.21.1-21.1.x/net/neoforged/neoforge/event/entity/living/mobspawnevent). Therefore, own population admission/retirement policy should not assume that a forced chunk automatically means unrestricted natural spawning.

### 37.5 Virtual simulation has explicit limits

When an agent is distant, its record may advance:

- needs and recovery where their model is supported;
- schedules and intentions;
- regional work progress whose inputs and effects are represented in the city simulation;
- social contact through actual recorded reports/messages/events;
- long-distance travel along known region-route edges;
- inventory reservations and cargo-in-transit state;
- personal and settlement memories from events the agent could legitimately know.

It may not silently advance:

- Minecraft block ticks, redstone, crop random ticks, fluid ticks or block-entity processing in unloaded chunks;
- physical mining, chopping, building, combat, item drops or entity collision;
- the opening/closing of a real Minecraft container;
- the success of a physical route if its geometry is unknown or has changed;
- discovery of locations or facts not sensed, observed or reported.

A virtual agent can make progress through abstract city processes only where the V9 city model has a clearly defined owner for that resource/process and the Minecraft world has an explicit mapping or reconciliation rule. The stock index is not a second inventory. On-hand item stacks, reserved stock, cargo in transit, network-visible supply and contractual claims remain distinct; every number must have one authoritative owner.

## 38. Staged implementation roadmap — superseded by Addendum C

The earlier Phase 0–9 sequence in Addendum B is retained in the document history but is **not the active implementation order**. It proposed a custom civilian and mob adapters before the fake-player system. Addendum C supersedes that sequence and is authoritative for the rollout.

The active priority is:

1. player-agent identity and fake-player lifecycle;
2. active fake-player embodiment and visibility;
3. player-specific CUA motor and action parity;
4. one System-1-controlled fake player;
5. multiple fake players communicating and cooperating;
6. human-player communication, interaction, consent and identity boundaries;
7. city-builder work through actual player-body actions;
8. player-independent cognition and virtual/regional continuity;
9. population scale, persistence and compatibility;
10. GoalSelector-based mob adapters;
11. Brain-based and remaining mob adapters.

See Addendum C, Sections C.12–C.14, for the phase deliverables, first vertical slice and acceptance criteria. The general runtime, scheduling, performance, governance, physical-region, failure and evidence requirements in Sections 34–37 and 39–42 remain applicable. Mob-specific behavior-atlas and takeover work is deferred until the fake-player acceptance suite passes.

## 39. Work allocation and performance design

### 39.1 Keep the server thread authoritative

Minecraft world mutation must be applied on the logical server thread. The initial safe implementation should keep perception reads that touch live world state, entity motor output, chunk activation, inventory access, damage and block/entity mutation on that thread. Optimize only after profiling. Later, pure decision calculations may consume immutable, timestamped snapshots on worker threads, but their output is only a proposal. The server thread revalidates the proposal against the current world/agent revision before it can produce a side effect.

### 39.2 Three independent scheduling rates

| Layer | Purpose | Example trigger (not final frequency) |
|---|---|---|
| reflex/motor | immediate balance, collision response, short input sequencing, emergency interruption | each physical body tick / relevant event |
| System-1 intention | choose or revise a local goal/skill from drives and local beliefs | event-driven plus bounded periodic checks |
| civilization/region | work progress, production/logistics, relationships, distant travel and settlement state | region scheduler / campaign tick |

The campaign tick's precompiled actions are not a replacement for physical 20 Hz movement, and a 20 Hz entity tick is not a reason to recompute a person's entire intention every 50 ms.

### 39.3 Do not convert low fidelity into loss of personhood

Compute tiers determine how often or how physically the simulation updates. They do not change identity, gender, affect ontology, social importance, moral value, persistence, memory ownership or eligibility to participate in history. A distant agent remains the same person. Its state may be advanced through a coarser model with explicit limits; it must not become a nameless aggregate or have its biography invented on wake.

### 39.4 Profiling matrix

Measure at minimum:

| Population condition | Measurements |
|---|---|
| 1 / 10 / 50 / 100 / 500 materialized agents | server tick p50/p95/p99, entity tick time, AI time, allocation/GC, pathfinding, memory |
| same number of virtual agents | scheduler time, wakeup rate, event fan-out, regional simulation time, persistence I/O |
| mixed population | handoff frequency, reconciliation time, duplicate-state count, chunk ticket count |
| concentrated settlement | pathfinding contention, collision density, container access, block/entity interaction throughput |
| dispersed settlements | region queue fairness, route updates, persistence volume, chunk activation latency |
| high-threat event | burst CPU, decision priority starvation, action failure/retry count |
| restart/recovery | save/load latency, identity match rate, migration failures, event replay time |

Report complete raw process output, exact run configuration, full measurement window and test inventory. Do not filter out slow samples or failed scenarios. Benchmark on the same hardware and modpack when comparing iterations.

## 40. Failure conditions that must be first-class refusals

| Condition | Required behavior |
|---|---|
| adapter absent for entity type | refuse System-1 enrollment; retain declared vanilla/default ownership |
| AI hook not found after version update | startup refusal for that adapter; do not assume the mixin still applies |
| ownership state ambiguous | freeze decision writes and recover from the last authoritative transition |
| both vanilla and System-1 write motor/target state | record split-brain refusal; stop the non-authoritative writer |
| entity instance unloaded | transition representation only; never record death or retirement automatically |
| chunk is not active | no physical world action; queue a bounded activation request or remain virtual |
| chunk ticket budget exhausted | retain virtual state and report blocked physical work; do not force-load more chunks |
| path/target no longer valid | invalidate plan step, re-observe and choose a new candidate |
| stale cognition proposal | reject when agent/world snapshot revision no longer matches |
| virtual action lacks a modeled physical equivalent | mark unsupported/blocked; do not fabricate blocks, items or outcomes |
| save contains unknown cognition schema | preserve bytes, refuse unsafe migration, request explicit migration path |
| CUA motor returns but world outcome absent | treat as attempted, not completed; observe and retry/replan only under bounded policy |
| new mod changes AI or entity lifecycle | compatibility status becomes unknown until adapter tests rerun |
| performance budget exceeded | shed optional cognition/telemetry work according to declared policy; never drop persistence or safety/authority checks silently |

## 41. Axis and authority during autonomous entity control

Replacing vanilla AI creates more actions, not fewer governance questions. Every physical side effect still needs the project's selected in-world authority semantics. The system must distinguish:

- the entity's desire/intention to act;
- the evidence that its local belief supports the action;
- the entity/controller's current capability and scope;
- the action-specific procedural gate;
- any human/owner approval required for that action class;
- the actual result of execution;
- certification of a body of completed work.

Axis rules 51–55 prohibit collapsing evidence grade, certification tier, procedural gate and state transitions into one score. Rules 68–76 separate proposal, Governor, human approval, execution gate and actual action. The game therefore needs an explicit *governed autonomy policy* for ordinary in-world acts (walking, eating, harvesting, crafting, building, trading, fighting, breeding, griefing, entering a protected area, etc.). Until that policy exists, an action class cannot be called autonomous merely because System-1 selected it. The unresolved authority rule should be represented as a design gate, not solved by granting every NPC a blanket write capability.

The agent must never be able to self-grant a capability by changing its personality, grade, social rank, campaign or memory. Historical seasons and lessons can influence what it considers; they cannot grant present permission. A story event is not a security decision and a strong evidence grade is not an authorization token.

## 42. Acceptance suite for the transition

The full transition does not pass until all required suites below are witnessed in a dedicated NeoForge server and a real Minecraft client.

### 42.1 AI ownership

1. Vanilla mode produces the vanilla baseline.
2. Shadow mode produces no gameplay change.
3. System-1 mode selects and executes a different action for the test scenario.
4. Instrumentation shows no concurrent vanilla goal/brain motor writes.
5. Rollback to vanilla preserves the same entity UUID/AgentId and valid body state.
6. Unknown entity types do not accidentally enter System-1 mode.

### 42.2 Body and world fidelity

1. Entity collision, gravity, fluids, damage, effects, equipment and animation continue to function.
2. A movement command is solved by game physics and respects geometry.
3. Interaction checks reach/range/line of sight/inventory state.
4. A failed interaction remains failed; no hidden shortcut manufactures success.
5. Save/unload/reload preserves body and person continuity.

### 42.3 Player-independent cognition

1. With no player near a controlled region, System-1 agents still update their own needs, relationships and intentions.
2. Their cognition queue continues across server ticks and restart according to the persistence contract.
3. They do not perform unloaded physical interactions.
4. A required physical act requests bounded chunk activation, runs only after an authoritative activation result and releases its ticket afterward.
5. A player entering the region materializes the same agent identity and reconciles the current physical state.

### 42.4 City-builder behavior

1. A farm shortage activates a causal diagnosis, not a generic “collect food” reflex.
2. The agent can distinguish a shortage of total stock from reserved or in-transit stock.
3. A physical logistics bottleneck affects delivery.
4. Construction cannot complete without materials, labor, valid placement and verified stages.
5. Repair and defense use real inventory and infrastructure state.
6. Ecological and weather effects only apply when the selected world/mod ontology supports them.

### 42.5 Scale and recovery

1. The system meets an explicitly declared tick-budget target on the target hardware/modpack.
2. There is no duplicate person during materialize/dematerialize handoff.
3. An unload is not recorded as death.
4. A server crash midway through a transition replays from the authoritative state without applying the action twice.
5. Schema upgrade preserves identity, event provenance and known/unknown facts.
6. The world can be reopened and inspected from its printed records and diagnostics without a model or network dependency.

## 43. Recommended first vertical slice — superseded by Addendum C

The former one-custom-civilian first slice is superseded by the fake-player-first amendment. The first meaningful slice is **two fake players, one real human player, and one shared physical task**. It must prove stable player-agent identity, actual player-body tracking and tick behavior, player-specific CUA, structured communication, cooperation, human-player interaction, observed game outcomes, and persistence/reconciliation.

The full player-first implementation contract, fake-player lifecycle risks, interaction model, communication envelope, and acceptance criteria are in `minecraft_fake_player_first_addendum.md` (Addendum C). Mob replacement follows only after this player-agent loop passes.

The non-player-proximity principle remains: cognition and identity continue under the custom scheduler, while physical actions still require an active body and a ticking world region. Chunk tickets are bounded resources; virtual activity must not be described as an actual physical Minecraft action.

---


## References and version notes

- NeoForge 1.21–1.21.1 getting started / Java 21 / dedicated-server testing: https://docs.neoforged.net/docs/1.21.1/gettingstarted/
- NeoForge 1.21.1 logical vs physical sides: https://docs.neoforged.net/docs/1.21.1/concepts/sides/
- NeoForge event system (1.21.1): https://docs.neoforged.net/docs/1.21.1/concepts/events/
- NeoForge entity lifecycle reference (page currently for 1.21.5; verify APIs against target 1.21.1 source): https://docs.neoforged.net/docs/1.21.5/entities/
- NeoForge `EntityJoinLevelEvent` Javadoc, 1.21.1: https://aldak.netlify.app/javadoc/1.21.1-21.1.x/net/neoforged/neoforge/event/entity/entityjoinlevelevent
- NeoForge `MobSpawnEvent` Javadoc, 1.21.1: https://aldak.netlify.app/javadoc/1.21.1-21.1.x/net/neoforged/neoforge/event/entity/living/mobspawnevent
- NeoForge `TicketController` Javadoc, 1.21.1: https://aldak.netlify.app/javadoc/1.21.1-21.1.x/net/neoforged/neoforge/common/world/chunk/ticketcontroller
- NeoForge `ServerTickEvent.Pre` Javadoc, 1.21.1: https://aldak0.ru/javadoc/1.21.1-21.1.x/net/neoforged/neoforge/event/tick/servertickevent.pre.html
- NeoForge `ServerTickEvent.Post` Javadoc, 1.21.1: https://aldak.netlify.app/javadoc/1.21.1-21.1.x/net/neoforged/neoforge/event/tick/servertickevent.post
- NeoForge `LevelTickEvent.Pre` Javadoc, 1.21.1: https://aldak.netlify.app/javadoc/1.21.1-21.1.x/net/neoforged/neoforge/event/tick/leveltickevent.pre
- Minecraft Wiki, Java Edition simulation distance: https://minecraft.wiki/w/Simulation_distance
- Minecraft Wiki, chunks and ticking: https://minecraft.wiki/w/Chunks

**Evidence note:** NeoForge's official documentation and 1.21.1 Javadocs ground the logical-server boundary, event lifecycle, entity join behavior, tick events and chunk-ticket API. The Minecraft Wiki is secondary material for the Java simulation-distance overview. Any direct modification to `Mob`, `GoalSelector`, `Brain`, `PathNavigation`, `Entity#tick` or `setNoAi` must be verified against the exact decompiled 1.21.1 jar and dedicated-server tests. This plan does not assert that any single public event is sufficient to replace all AI paths.

---

## Addendum C — Fake-player-first implementation amendment

This amendment changes the priority of the entity replacement project. The main simulated citizens are **fake players first; mobs second**. The full architecture and proof requirements are in `minecraft_fake_player_first_addendum.md`. The phase order in this plan has been updated accordingly.

### Revised player-agent implementation sequence

1. Lock the exact NeoForge 1.21.1 build and create a dedicated-server test harness with a real client for action-parity comparisons.
2. Prove a durable player-agent registry, unique agent-owned UUID/GameProfile, `FakePlayerFactory` lifecycle, server-level tracking, client visibility, save/restart, dimension change, and cleanup. A `FakePlayer` helper object alone is not proof of a persistent autonomous player.
3. Build a player-specific CUA motor. Do not assume mob `PathNavigation` or `GoalSelector` controls apply to player bodies. Test normal movement/look, jump, sneak/sprint, item use, hotbar/inventory, breaking, placement, pickup/drop, entity interaction and combat. Verify actual actor identity and resolved outcomes.
4. Connect one fake player to System-1 cognition: perception, beliefs, drives, memory, social state, skill selection, action proposal, CUA execution and observed result.
5. Add a second fake player and structured communication. Require real cooperation: request/accept/refuse, travel, pickup, delivery, and task reconciliation—not just chat output.
6. Add human-player communication and interaction. Preserve real-player control and identity; enforce permissions for property, inventory, claims, trade, PvP, following, and protected areas. A casual chat instruction is not automatically an authority grant.
7. Integrate the city-builder's work orders, stock/reservations and production chains through actual player-body actions.
8. Add player-independent cognition and virtual/regional continuity without phantom physical actions in unloaded chunks.
9. Benchmark and harden persistence, multiple agents, multiplayer, client replication, event compatibility, and mod interactions.
10. Only then add GoalSelector and Brain mob adapters as separate entity-type implementations.

### First pass condition

The minimum vertical slice is two autonomous fake players and one connected human player in a small settlement. One fake player requests a real item from the other; the other accepts or refuses, moves through the world, obtains the item through a supported player interaction, delivers it, and the result is verified. The human player can observe and interact with both agents. Their identities and memories survive save/reload. Every physical action is authorized and executed through the appropriate server-side player-action path; every outcome is observed rather than asserted.

### Important NeoForge caveat

NeoForge 1.21.1 documents `FakePlayer` as a `ServerPlayer` subclass for simulating player actions and provides a `FakePlayerFactory` with cache and world-unload lifecycle guidance. This is a useful base, not proof that a created helper automatically participates in every connected-player system. The implementation must explicitly test player-list membership, entity tracking, replication, ticking, fake connection behavior, menus/GUI, advancements, dimension travel, and cleanup on the exact build. See [FakePlayer Javadoc](https://aldak.netlify.app/javadoc/1.21.1-21.1.x/net/neoforged/neoforge/common/util/fakeplayer), [FakePlayerFactory Javadoc](https://aldak.netlify.app/javadoc/1.21.1-21.1.x/net/neoforged/neoforge/common/util/fakeplayerfactory), [NeoForge networking docs](https://docs.neoforged.net/docs/1.21.1/networking/), and [NeoForge issue #1487](https://github.com/neoforged/NeoForge/issues/1487).

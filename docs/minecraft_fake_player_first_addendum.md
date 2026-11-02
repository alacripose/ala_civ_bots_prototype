# Addendum C — Fake-Player-First Embodiment, Social Interaction, and World Agency

**Applies to:** `minecraft_ensouled_system1_cua_architecture_v3.md` and `minecraft_entity_ai_replacement_transition_plan.md`  
**Status:** architectural amendment and implementation requirement; not a completed mod implementation  
**Provisional target:** Minecraft Java 1.21.1 + NeoForge 21.1.x, pending an exact build lock  
**Priority statement:** fake players first; ordinary mobs second.

---

## C.1. Superseding decision: the primary citizens are fake players

The central embodied actors in this system are **fake players**, not custom civilian mobs. The system is intended to create a population of independently persistent, System-1-controlled player agents who inhabit the Minecraft world, communicate with one another, communicate and interact with human players, and perform ordinary player-like actions in the shared environment.

This is a change to implementation priority and to the embodiment model. It is not a cosmetic renaming of the earlier “civilian” entity. The previous first vertical slice—one custom civilian followed by a GoalSelector mob and then a Brain mob—is superseded. The first implementation must prove one fake player end to end, then multiple fake players interacting with one another, then interactions between fake players and human players. Mob replacement comes after those capabilities work.

The desired agent is not merely an entity that looks like a player. It must be treated as a player actor by the relevant game systems when it performs player actions, while remaining a clearly identified autonomous agent rather than impersonating a human account. It needs a durable identity, inventory, equipment, movement, perception, action controls, relationships, communication, memory, and a persistent life history. Its actions must resolve in the actual Minecraft world and produce observable game outcomes.

The core objective is therefore:

> Build a population of persistent player agents, each with its own System-1 cognition and CUA-controlled player body, able to coordinate with other agents and interact honestly with human players and the world through ordinary player mechanics.

**Priority order:**

1. Fake-player identity, lifecycle, and world embodiment.
2. Player-like movement, inventory, interaction, combat, and CUA controls.
3. Independent System-1 cognition for fake players.
4. Agent-to-agent communication and cooperation.
5. Fake-player-to-human-player communication and interaction.
6. City-builder, work, household, logistics, and social systems operating through those player bodies.
7. Player-independent scheduling and virtual/regional continuity.
8. Scale, multiplayer, and mod compatibility.
9. Mob adapters (GoalSelector-based, Brain-based, and other families) as a secondary extension.

A mob is not a simpler substitute for a player agent. A mob is a different entity family with different control, interaction, animation, persistence, and social capabilities. The system must implement each body according to its actual Minecraft type rather than forcing all entity types through one generic “AI entity” abstraction.

## C.2. Terminology: fake player, player agent, and real player are not interchangeable

The word *fake player* is used here to identify a server-side player embodiment. It does not mean an LLM-controlled client, a spoofed logged-in user, or a mob with a player skin.

Use these terms distinctly:

| Term | Meaning in this architecture |
|---|---|
| **Player agent** | The durable person/agent record: stable identity, cognition, memory, needs, relationships, permissions, skills, life history, and current plans. This exists independently of a loaded Minecraft entity object. |
| **Fake-player body** | The server-side `ServerPlayer`-family object used to embody a player agent in the world. It is the physical actor through which player-like actions occur. |
| **NeoForge `FakePlayer`** | NeoForge's concrete helper class, documented as a basic fake server player implementation for simulating player actions. It extends `ServerPlayer`. It is a useful starting point, but using this class alone does not prove the complete persistent-agent lifecycle required here. |
| **Human player** | A person using a real Minecraft client and account. Their avatar and input remain under that person's control. |
| **Connected bot client** | A separate architecture in which an independently running client or protocol endpoint connects to the server. This is not the default target of this design and must not be conflated with NeoForge's in-process `FakePlayer`. |
| **Mob** | A non-player Minecraft entity with its own behavior architecture, such as `GoalSelector`, `Brain`, navigation, or entity-specific behavior. It receives a separate adapter later. |

NeoForge 1.21.1 Javadocs describe `FakePlayer` as `public class FakePlayer extends ServerPlayer` and as a basic fake server player that can simulate player actions. The class has fake connection/network-handler implementation details. That establishes that the API is player-shaped; it does not establish that every normal connected-player lifecycle, packet, client GUI, player-list, or replication behavior is automatically provided. Those are implementation questions to prove on the exact target build. See [NeoForge 1.21.1 FakePlayer Javadoc](https://aldak.netlify.app/javadoc/1.21.1-21.1.x/net/neoforged/neoforge/common/util/fakeplayer) and [NeoForge 1.21.1 FakePlayerFactory Javadoc](https://aldak.netlify.app/javadoc/1.21.1-21.1.x/net/neoforged/neoforge/common/util/fakeplayerfactory).

### C.2.1. Do not assume the helper class is the whole product

NeoForge's `FakePlayerFactory` exposes `get(ServerLevel, GameProfile)`, `getMinecraft(ServerLevel)`, and `unloadLevel(ServerLevel)`. Its documentation describes a cache keyed by level and profile and warns consumers to use weak references, clean up on world unload, or ask the factory for the object again. That means the factory is an object-lifecycle helper; it is not the authoritative person registry, campaign store, agent database, or long-term identity system.

Required consequences:

- Every autonomous fake player must have a unique, stable agent ID and a distinct `GameProfile`/UUID reserved for that agent.
- Never use the shared `getMinecraft` fake player as the embodiment for multiple citizens.
- Never reuse a real human player's UUID as an autonomous agent's identity.
- The civilization registry owns durable identity; the current fake-player Java object is a replaceable physical representation.
- Retain factory objects only according to the documented cache/lifecycle rules. Re-resolve the appropriate body for the current `ServerLevel` and profile after level/world transitions.
- Test unload cleanup and server restart explicitly. A Java object reference surviving in a registry is not proof that its level or entity lifecycle remains valid.

A real player profile, skin, or name must not be copied in a way that makes an agent appear to be that human. A fake player should have an agent-owned identity, and its autonomous status must be clear in chat, UI, logs, and administrative tools.

## C.3. Separate the player-agent model from the Minecraft player body

The player agent remains the source of continuity. The fake-player body is the current embodied interface to the physical world.

```text
PLAYER AGENT (persistent)
  AgentId / stable UUID / agent-owned GameProfile
  identity, history, memory, beliefs, needs, feeling state
  relationships, social reputation, knowledge provenance
  skills, permissions, contracts, tickets, work assignments
  communication inbox/outbox, dialogue context, group plans
  inventory/equipment claims and reconciliation metadata
  current intentions and action state
             |
             v
PLAYER-AGENT CONTROLLER (System-1)
  perception -> belief update -> drive/intent selection
  social interpretation -> skill selection -> action proposal
  action validation -> CUA motor command -> result observation
             |
             v
PLAYER CUA / BODY ADAPTER
  player movement and look controls
  jump / sneak / sprint / swim / climb / ride
  hotbar / hand / inventory / item use
  block interaction / mining / placement
  entity interaction / combat / item pickup and drop
  menu/container adapter where supported
             |
             v
MINECRAFT SERVER PLAYER BODY
  ServerPlayer-family lifecycle and world membership
  ordinary collision, movement resolution and game rules
  player interactions, inventory, damage, effects and equipment
  server-side events, replication, persistence and dimension rules
             |
             v
OBSERVATION / EVENT BRIDGE
  action result + world delta + event provenance
  social result + inventory delta + failure/refusal reason
             |
             +----> persistent memory / relationship / work ledger
             +----> Axis and Governor audit
```

A fake player must not acquire a second independent decision owner. The fake-player controller is the owner of its autonomous intention and motor selection. Minecraft remains authoritative for physical outcomes and game-rule resolution. Axis/Governor remain independent of cognition and decide whether a proposed side effect is admissible under the project's authorization rules.

A human player is never another agent body to be commandeered. Human players are participants observed through allowed game events and interacted with through the social/action interfaces described below.

## C.4. Player embodiment is not mob embodiment

The first implementation must be built around the behavior expected of a player body. Do not route player agents through mob-only abstractions merely because mobs are easier to spawn or navigate.

### C.4.1. No `PathNavigation` assumption for fake players

A player does not have the same `PathfinderMob`/`PathNavigation` control architecture as common mobs. The fake-player body needs a player-specific motor system:

- A route planner produces a path through walkable space, but it does not teleport the player along that path.
- A motor controller converts the next route segment into bounded forward/backward and lateral movement, look direction, jump timing, sprint/sneak decisions, swimming/climbing/vehicle behavior where implemented, and stop conditions.
- Movement is resolved by the real player entity and Minecraft collision/physics. The controller observes whether the player actually moved, was blocked, fell, was pushed, took damage, or entered a different movement state.
- The controller must account for player dimensions, step height, speed modifiers, hunger/exhaustion where relevant, effects, knockback, fluids, ladders, doors, collision geometry, and equipment-dependent movement.
- A route's existence is not proof that the body can traverse it. Movement failures update local beliefs and path costs.

The player CUA layer should represent ordinary player capabilities rather than a privileged high-level command such as `teleport_to`, `mine_block_instantly`, `open_any_chest`, or `set_block`. Its low-level vocabulary should include the equivalents of:

- look at target / rotate view;
- move forward, backward, strafe left/right, stop;
- jump, crouch/sneak, sprint;
- swim or climb when the relevant body state and skill support it;
- select hotbar slot, swap hand, equip/use item;
- attack, interact, use item, place block, continue mining, stop mining;
- pick up, drop, or transfer an item through a legal interaction path;
- ride, dismount, or interact with a vehicle when supported.

The CUA controller decides how to issue these controls. The server remains authoritative about what happened. The exact implementation API and mappings must be verified against the locked 1.21.1 build; this document intentionally specifies the behavior contract rather than inventing unverified method names.

### C.4.2. Player actions must have player semantics

The action adapter must preserve the checks and side effects associated with a player action, including as applicable:

- reach and line of sight;
- active hand and item stack;
- attack cooldown and weapon attributes;
- item durability and enchantments;
- block hardness, correct tool, mining progress and drops;
- placement face, collision and placement restrictions;
- interaction result and cancellation by other mods;
- inventory capacity and slot restrictions;
- game mode, permissions, claims and protection events;
- hunger, exhaustion, effects, damage and death/respawn rules.

Do not produce the same visible outcome by mutating block states, inventories, health, or entity data behind the player's action path. A fake player's action should be recognized by the relevant server-side systems as an action by that fake player, with the fake player's identity as actor.

The event bridge must capture the result after the server resolves the action. A requested attack is not a successful hit. A requested block break is not a mined block. A requested trade is not a transfer. The body must observe the actual result and update memory accordingly.

### C.4.3. Benchmark against a real player

For each supported action, run the same scenario through a real client and the fake-player body and compare the important outcome semantics. The comparison should include the event hooks observed by mods, the actor identity, item/inventory deltas, damage, block changes, sounds/animations where relevant, and cancellation behavior.

The target is not packet-level impersonation of a real human. The target is correct server-side player semantics for the subset of actions the agent is authorized and able to perform.

## C.5. Interaction surface: fake players must participate in the actual world

The primary world interface must be the player interaction surface, not a shadow city-builder world that merely reports intended actions.

### C.5.1. First-class physical capabilities

The first supported capabilities should be selected by their ability to demonstrate a meaningful agent life and a real shared-world interaction loop:

1. Spawn/materialize as a visible player body with a stable agent-owned identity.
2. Move through a settlement using ordinary movement and collision.
3. Look at, approach, avoid, and face blocks, entities, and players.
4. Pick up, carry, equip, use, and drop items through supported player paths.
5. Open/use workstations and containers through a headless server-side interaction adapter, with exact semantics validated.
6. Mine and place blocks through the player action path.
7. Attack or defend only when the action is explicitly permitted and only through ordinary player combat rules.
8. Sleep, eat, craft, trade, ride, or use other mechanics when an explicit supported action adapter exists.
9. Observe and report the actual result of each action.

This is a capability progression, not a requirement to implement every action at once. Each skill must have preconditions, motor steps, timeout, result predicates, refusal conditions, and an outcome observer.

### C.5.2. Container and GUI handling requires special treatment

A fake player does not have an ordinary human client rendering a screen. It must not be assumed that opening a menu on the fake player is equivalent to a client opening a GUI and sending normal container click packets.

Implement two distinct cases:

- **Agent-side headless interaction:** the agent interacts with a chest, workstation, merchant, or other supported container through a server-side adapter that checks actual reach, target state, permissions, slot rules, inventory constraints, and transaction semantics. It must not directly mutate arbitrary inventories to simulate a successful click.
- **Human-side user interface:** when a real player needs to inspect, approve, offer, or accept a transaction with an agent, the real player's client can receive a normal server-opened interface or another explicit visible interaction. The fake player itself does not need a GUI client.

Container transactions must be atomic or explicitly staged, auditable, and reconcilable after interruption. The implementation must test whether the particular menu/container API can safely support the fake player's connection model; unsupported menus must return a refusal or use a deliberately designed headless adapter.

## C.6. Communication is a core gameplay subsystem, not a chat cosmetic

Agent-to-agent communication and communication with human players are central requirements. They must be designed as part of the System-1 social architecture, not bolted on after movement and jobs.

### C.6.1. Distinct communication channels

At minimum, separate:

- **Private agent message:** addressed to one agent; may be a question, offer, request, warning, or personal message.
- **Local/proximity speech:** a message visible/audible to participants in a bounded local context, subject to the chosen presentation system.
- **Settlement or organization channel:** public or role-limited communications with a known audience.
- **Direct human-player communication:** a fake player addresses a real player through clearly attributed in-game chat or a supported interaction UI.
- **World/event report:** a structured report such as “the northern gate is blocked” or “the furnace stopped,” which can be consumed by agents with relevant access.
- **Nonverbal signal:** gaze, facing, gesture, item display/offer, movement, or other supported player-body expression. These are distinct from text and must have observable semantics.

Do not expose every internal thought as chat. Cognition is not public speech. The agent decides whether communication is useful, appropriate, authorized, and addressed to a particular recipient or group.

### C.6.2. Structured message envelope

Use a first-class communication record, conceptually similar to:

```text
AgentMessage {
  message_id
  sender_agent_id
  recipient_agent_ids | channel_id
  channel_kind
  speech_act
  payload / visible_text
  referenced_world_entities_or_events
  created_tick
  expiry_tick
  visibility / audience policy
  delivery_state
  response_state
  provenance / confidence
}
```

This is a schema sketch, not a committed serialization format. Messages should have stable IDs, delivery and expiry behavior, recipient scoping, an audit trail, and clear distinction between a received message and a verified world fact.

Useful initial speech acts include: greet, introduce, ask, answer, request, offer, accept, decline, warn, report, coordinate, negotiate, thank, apologize, teach, invite, refuse, and ask for help. These actions should change social context only when delivery or the relevant interaction actually occurs.

### C.6.3. Communication should lead to observable cooperation

The minimum meaningful multi-agent test is not that two fake players print text. It is that they:

1. perceive a shared situation or receive a message;
2. form their own beliefs and intentions;
3. communicate a proposal, request, or response;
4. agree on a bounded task or coordinate complementary skills;
5. move and act through their own player bodies;
6. observe the real world result;
7. update memories, trust, relationship state, and task records from the result.

For example, one agent discovers a shortage at a workstation, asks another agent to bring material, receives an acceptance, navigates to a storage location, picks up a real item, carries it to the workstation, and verifies delivery. The communication record, item movement, task state, and memory updates must agree. No agent may simply declare that an item was delivered because another agent said it was.

Each agent retains its own memory and perspective. There is no automatic shared omniscience. A group plan may coordinate behavior, but it does not merge identities, beliefs, or personal histories.

## C.7. Human-player interactions, consent, and identity boundaries

Real players are first-class participants in the same world. Fake players should be able to greet, answer, ask, offer, trade, follow a permitted instruction, cooperate, warn, assist, and react to player actions. That does not grant the fake players blanket authority over human players or their property.

Required boundaries:

- A fake player must never control, impersonate, or inject movement/actions into a real player's body.
- A player-issued instruction is interpreted through the project's authority and ticket model; a casual chat message is not automatically an authorized system command.
- A fake player must not claim to be a human account, imitate a real player's identity, forge signed chat, or create misleading login/session state.
- The display and chat presentation must make autonomous-agent identity clear.
- Access to a human player's inventory, storage, claims, builds, vehicles, or private spaces requires explicit permission under the relevant world/claim policy.
- Following, repeated messaging, trading, combat, building, or entering a protected area must have explicit policy and stop/refusal behavior.
- PvP, theft, griefing, traps, breeding, destructive building, and other high-impact actions remain separately governed, not implied by the agent's ability to perform the mechanic.
- Real-player chat, interactions, damage, trades, and commands are observations with provenance. Do not infer private intent from them.

The social layer should support refusal and disengagement as normal actions. A fake player should be able to decline a request because it conflicts with its contract, skill, safety, schedule, consent boundary, relationship, or current commitment. A human player's refusal must be respected by the system's social interaction rules.

### C.7.1. Chat is not the only form of social interaction

Player-to-player social behavior includes looking toward a speaker, approaching or maintaining distance, exchanging items, cooperating on a build, defending a shared location, showing a tool or item, waiting, inviting, declining, and responding to a world event. Chat text is one carrier among several.

The social perception system should convert observed player actions into bounded evidence. For example, a player placing materials in a public donation chest can be observed as a donation event; it does not by itself prove the player's wider political beliefs, personality, or permanent permission grant.

## C.8. Body lifecycle and persistence

The earlier two-body-state model remains valid but must be expressed as a player-agent lifecycle.

### C.8.1. Durable identity vs transient body object

The player-agent record survives:

- chunk unload and reload;
- dimension change;
- entity removal/recreation;
- server save and restart;
- temporary loss of a compatible body adapter;
- migration between supported fake-player implementation versions.

The fake-player Java object does not need to survive any of those events. After reload, the system resolves or creates the correct body representation for the same `AgentId` and profile, restores the supported player state, reattaches the current controller, and verifies ownership before movement or interaction resumes.

### C.8.2. Required body states

Use explicit states, for example:

- `VIRTUAL`: persistent agent state only; no physical player body in the active world.
- `MATERIALIZING`: validating level, spawn/return position, profile, chunk activation, and body creation.
- `ACTIVE`: player body is present and may receive bounded CUA commands.
- `SUSPENDED`: body exists or is being unloaded, but no autonomous motor action may be issued until lifecycle checks pass.
- `RECONCILING`: body/inventory/location/state is compared against persistent agent state after an interruption or reload.
- `FALLBACK`: a known safe state is active because a required subsystem is unavailable; no silent vanilla takeover.
- `RETIRED`: agent lifecycle has been explicitly closed under the campaign/personhood rules; this is not synonymous with unloading or chunk absence.

Do not use Minecraft entity removal as an implicit death, and do not use a virtual representation as permission to claim physical work occurred in an unloaded chunk.

### C.8.3. Player-specific lifecycle tests

The initial proof must investigate and test, rather than assume, the following behaviors for the exact NeoForge 1.21.1 patch:

1. `FakePlayer` construction and `FakePlayerFactory` caching/cleanup.
2. Admission into the server level's entity tracking and player collections.
3. Whether and how the body is visible to nearby real clients.
4. Whether it appears in the server's online-player list, tab list, scoreboard, selectors, commands, and player-count systems; which of those are desirable and which must intentionally differ.
5. Player tick, movement resolution, equipment, inventory, effects, damage, death, and respawn.
6. Dimension transfer and re-resolution of the correct profile/body.
7. Player interaction events and mod hooks for block use, item use, breaking, placement, entity interaction, attack, damage, pickup, and drop.
8. Advancements, statistics, criteria listeners, and any cleanup needed to avoid stale references or leaks.
9. Behavior of fake connections and attempts to open menus or send clientbound packets.
10. Save/restart behavior and identity reconciliation.

A 1.21.1 NeoForge issue documents a `FakePlayer`/advancement-listener leak scenario when fake profiles with unrelated UUIDs are created. This is a warning to test identity/advancement cleanup, not a claim that every fake-player use leaks. See [NeoForge issue #1487](https://github.com/neoforged/NeoForge/issues/1487).

## C.9. Two simulation layers still apply, but now to player agents

The system still needs the distinction between persistent cognition and loaded physical simulation:

- **Player-agent cognition** can continue through the civilization scheduler when no human player is nearby.
- **Player-body movement and world interaction** require a valid active body and the relevant Minecraft world/chunk processing.
- **Virtual/regional activity** may update explicitly modeled schedules, needs, communication, plans, relationships, and regional logistics. It must not claim an action that requires an actual physical player body in a non-ticking chunk.
- **Materialization** restores the same agent identity into an active `ServerPlayer`-family body and then permits CUA action after reconciliation.

A player agent at a distance may decide to travel, request a destination, coordinate with a group, or wait for materialization. It may not mine a block, open a chest, place a block, attack an entity, or participate in a physical transaction in an unloaded region unless that operation is explicitly supported by a separate regional simulation rule and is recorded as an abstract regional event—not misreported as a real player interaction.

Chunk tickets remain bounded physical-resource requests, not a blanket “keep every citizen loaded forever” setting. The ticket budget, priority policy, release conditions, and player experience must be explicit.

## C.10. City-builder systems must operate through player agents

The earlier city-builder and settlement architecture remains the planning/economic substrate, but its physical workers are fake players first. The city-builder layer may define needs, work orders, stock levels, reservations, production dependencies, routes, service coverage, and civic roles. It does not directly perform the physical work.

For a physical work order:

1. the city-builder layer issues a scoped job/ticket;
2. the agent evaluates it against needs, skills, commitments, relationships, and permissions;
3. the agent accepts, declines, or requests clarification;
4. the System-1 controller selects a player skill;
5. the CUA layer carries out real movement and player interaction;
6. the server resolves the actual world action;
7. the observer records the block/item/entity result;
8. the city-builder ledger reconciles stock, reservations, delivery, and production;
9. the agent records the experience and social consequences.

The city-builder may coordinate multiple people, but each embodied worker still has a distinct body, inventory, action history, and ability to refuse. A ticket is a bounded assignment, not a remote puppet string. Agents may communicate and negotiate task division while preserving the existing Governor/Axis/contract/ticket boundaries.

## C.11. Mob replacement is a separate, later adapter family

After fake-player embodiment, movement, communication, and player-world interactions are proven, the project may add supported mobs.

The mob layer must not inherit the fake-player motor assumptions. It needs its own type-specific adapters for:

- GoalSelector-driven mobs;
- Brain-driven mobs such as villagers;
- animals and breeding-capable entities;
- aquatic and flying entities;
- hostile mobs and combat families;
- modded entities with their own behavior architecture.

Each mob adapter must preserve its class-specific lifecycle, navigation, memory, interaction, animation, damage, and reproduction rules. A mob may have a different communication vocabulary and interaction range from a player agent. Shared concepts such as identity, perception, needs, memory, social relationships, ticketing, and Axis checks may be reused, but movement and action semantics must be type-specific.

The fake-player-first architecture does not mean mobs are reclassified as players or that every mob must communicate with a full player chat system. It means the system proves its main product on the entity type it actually intends to drive first.

## C.12. Revised implementation roadmap

This roadmap supersedes the phase order in Addendum B and the earlier “custom civilian -> GoalSelector -> Brain” vertical slice.

| Phase | Deliverable | Exit condition |
|---|---|---|
| 0 | Lock exact Minecraft/NeoForge build; create dedicated-server test harness and real-player comparison tests | Target artifacts, mappings, mod list, Java runtime, and test world are recorded |
| 1 | Stable player-agent registry and FakePlayer lifecycle spike | One durable identity resolves to one valid body per active level; no profile collision, stale body, or untracked lifecycle |
| 2 | Active fake-player embodiment | Body is visible/tracked as intended, ticks safely, persists/reconciles, and does not falsely impersonate a connected human |
| 3 | Player CUA motor and basic action vertical slice | Move/look/jump/sneak/sprint, inventory, item use, break/place, entity interaction and result observation pass player-equivalence tests |
| 4 | One System-1-controlled fake player | Perception, beliefs, needs, memory, skills and action selection operate without an LLM in the gameplay loop; CUA owns motor intent |
| 5 | Two or more fake players with communication and cooperation | Agents exchange scoped messages and complete a tangible shared task using actual bodies/items/world outcomes |
| 6 | Human-player communication and interaction | Real players can converse, offer, request, trade/cooperate where permitted; identity, privacy, consent and refusal tests pass |
| 7 | City-builder settlement vertical slice | At least one production/logistics chain is completed by fake players through real movement and interactions |
| 8 | Player-independent scheduler and virtual/regional continuity | Cognition and identity persist away from players; unloaded regions never receive phantom physical actions |
| 9 | Population scale, persistence, server performance and mod compatibility | Benchmarked population tiers, restart recovery, multiplayer correctness and mod interaction tests pass |
| 10 | First mob adapter family | A selected mob type is replaced without changing fake-player behavior or violating mob-specific mechanics |
| 11 | Brain-based and remaining mob families | Each family has its own verified adapter, rollback and performance profile |

### Phase 0 — Lock the actual runtime and build a parity harness

Freeze the exact 1.21.1 patch, NeoForge build, mappings, Java 21 runtime, loader/mod list, server configuration, and dedicated-server launch procedure. Create a controlled test world with at least one real human client and server-side logging. Capture the same action tests from a real player and a fake player so differences are visible rather than guessed.

### Phase 1 — Stable identity and lifecycle spike

Create one player-agent record with a stable `AgentId`, agent-owned UUID, profile, persistent identity metadata, and no assumed player-list membership. Use NeoForge's fake-player facilities only as a starting point. Prove construction, factory caching, world unload, server restart, dimension transition, entity tracking, and cleanup. Verify exactly one body per agent in a level and no accidental aliasing between two agents.

### Phase 2 — Visible, active player body

Prove that a fake player can be present in the actual world, tick, collide, receive ordinary damage/effects, hold equipment/inventory, and be observed by a nearby real client. Decide deliberately how it participates in player lists, tab lists, scoreboards, commands, selectors, player counts, and advancements. Do not silently inherit every connected-player convention merely because the class extends `ServerPlayer`.

### Phase 3 — Player CUA and action equivalence

Build the player motor adapter before System-1 complexity. Verify movement and look without teleporting; use items; select slots; interact with a block; mine with real progress; place a block; pick up and drop items; attack an entity under normal cooldown/reach/damage rules; and recover from interruption. Test at least one supported container/workstation action headlessly. Record unsupported UI and menu cases as explicit capability gaps.

### Phase 4 — System-1 controls one fake player

Connect the persistent agent to perception, local belief, needs/drives, affect, memory, social state, skill selection, and CUA. The controller's actions must be low-level and observable. No LLM is required to interpret ordinary game state or choose each gameplay tick. The scheduler selects eligible agents and budgets work; an action-result observer supplies the outcome.

### Phase 5 — Multi-agent communication and shared work

Run at least two fake players at once. Give them distinct identities, inventories, memory, capabilities, needs, and perspectives. Implement structured messages and visible speech acts. Test greetings, requests, offers, acceptance/refusal, work coordination, item delivery, warning, and a shared task. The agents must move, interact, and verify the result; a chat transcript alone does not count as success.

### Phase 6 — Human players become social participants

Introduce a real player client. Test incoming player chat and interaction observations, outgoing fake-player chat attribution, proximity behaviors, item offers, cooperation, trade UI for the real player, explicit refusals, and permissions. Ensure that an instruction in chat is not automatically privileged authorization and that the fake agents never take over the human player's movement or identity.

### Phase 7 — City-builder integration

Add the first real settlement work loop: work order, stock/reservation, worker acceptance, travel, pickup, carry, workstation interaction, processing or delivery, observed outcome, ledger reconciliation, and memory update. Add complementary roles only after one complete chain passes.

### Phase 8 — Player-independent life and virtual/regional body state

Decouple the cognitive scheduler from nearby human players and ordinary simulation distance. Agents retain identity, communication, relationships, schedules, commitments, and explicitly modeled regional work. A physical interaction requires an active player body and a ticking region; otherwise it remains a plan or a virtual event with its own provenance.

### Phase 9 — Scale and compatibility

Benchmark materialized, virtual, and mixed populations separately. Measure System-1 scheduling, path/motor work, entity ticking, memory, message routing, chunk activation, inventory reconciliation, and network replication. Test with a real player and a modpack, including event cancellation and claim/protection systems. Do not state a maximum supported population until measured on the target hardware and mod set.

### Phase 10+ — Mobs second

Only after the fake-player system passes its acceptance suite should the project add mob takeover. Start with one contained GoalSelector family, prove one controller owns decisions and rollback works, then add a Brain-based family and additional entity classes. Keep mob-specific adapters distinct from the player CUA controller.

## C.13. First vertical slice: two fake players, one human player, one shared task

The first meaningful system slice should include:

- one dedicated server on the locked target build;
- one connected real human player used as an interaction and parity-test participant;
- two fake players with distinct agent IDs, profiles, inventories, needs, memories, and communication state;
- one settlement hub with a storage location and a workstation;
- a short traversable route between storage and workstation;
- one real item-delivery or production dependency;
- one player movement skill and one item/container interaction skill;
- one System-1 scheduler and one event-driven message queue;
- at least one fake-player-to-fake-player request/acceptance exchange;
- at least one human-player-to-agent or agent-to-human interaction;
- one bounded Axis-authorized world action per physical operation;
- an observer that verifies movement, inventory, block/entity/world changes, message delivery, and failure reasons;
- persistent save, unload, restart, and reconciliation tests.

A representative success case:

1. Agent A discovers that the workstation lacks a required item through actual perception or a recorded, provenance-bearing report.
2. Agent A sends Agent B a scoped request for the missing item.
3. Agent B evaluates the request against its own state, schedule, skills, and commitments, then accepts or declines.
4. If accepted, Agent B navigates to the storage location, searches for the item, takes it through the supported player interaction path, and carries it to the workstation.
5. Agent A or the shared work process verifies that the item arrived and the workstation can proceed.
6. Both agents update their own memories and relationship/task state based on what they personally observed or received.
7. The human player can observe the agents' presence and communication and can interact without being impersonated or having their own actions commandeered.

This proves far more of the intended system than a single mob walking to a target. It demonstrates player embodiment, social coordination, CUA, inventory, real world interaction, and multi-agent outcome verification together.

## C.14. Acceptance criteria and explicit failure states

The fake-player-first implementation is not ready to expand until these conditions pass:

**Identity and lifecycle**
- A player's identity remains stable across body recreation and server restart.
- Two agents never share a GameProfile/UUID or body object accidentally.
- Factory cache/world cleanup is correct and does not retain dead levels.
- The agent is visibly and administratively distinguishable from human players.

**Player semantics**
- Real and fake player action paths are compared for every supported action.
- Event actor identity is the fake player's own identity.
- Reach, cooldown, inventory, item durability, block rules, damage and protection logic are respected.
- No unsupported client GUI or packet behavior is silently treated as successful.

**Cognition and CUA**
- One fake player can act without a model call for each tick/action decision.
- The fake player uses a player-specific motor; it is not moved by a mob navigator or teleported to simulate work.
- One controller owns all autonomous body intent; no split-brain controller is possible.
- Requested actions and observed outcomes remain separate records.

**Communication and society**
- Two agents can exchange messages, refuse, negotiate, coordinate, and complete a real shared task.
- A message is not automatically a verified fact or a grant of authority.
- Each agent preserves its own memory, knowledge limits, and perspective.
- Human players can interact with agents while retaining control of their own avatars and property.
- Communication has attribution, scope, rate limits, delivery state, and refusal behavior.

**Physical truth and performance**
- No physical action is claimed in an unloaded/non-ticking region.
- Materialization and dematerialization preserve the same identity.
- Chunk tickets are bounded and observable.
- Performance claims are supported by repeatable benchmarks.

Immediate refusal/fallback conditions include: missing or stale body; ambiguous identity; wrong dimension; untracked player body; fake connection/menu operation not proven safe; invalid reach/line of sight; missing item; blocked route; cancelled mod event; protected target; unauthorized human/player interaction; invalid ticket; chunk unavailable; dual controller; stale action proposal; failed world-result predicate; persistence mismatch; or performance budget exhausted.

A refusal is a first-class result with a reason. It is not silently converted into a teleport, direct inventory mutation, command shortcut, or mob behavior fallback.

## C.15. Architecture module changes

Keep the earlier system modules, but split the embodiment layer explicitly:

```text
player_agent_registry
player_profile_identity
fake_player_lifecycle
player_body_tracker
player_cua_motor
player_action_adapter
player_inventory_adapter
player_container_adapter
player_interaction_observer
social_message_bus
social_perception
relationship_memory
human_player_interface
agent_scheduler
regional_simulation
citybuilder_work_bridge
axis_gateway
chronicle

mob_adapter_registry              # secondary family
mob_goalselector_adapter          # secondary family
mob_brain_adapter                 # secondary family
mob_navigation_adapter            # secondary family
```

Shared contracts may define identity, perception records, action proposals, evidence, ticket scope, audit logs, and durable memory. Do not force `PlayerMotorInput` and `MobNavigationIntent` into one fake generic action type if their operational semantics differ. Use a common outer envelope with a type-specific payload and validator.

## C.16. Governance remains independent of the player agents

The previous Axis/Governor rules remain unchanged. A player agent's ability to swing a tool or interact with a block does not grant authority to do so. A successful fake-player action adapter proves *capability*, not *permission*.

Every world-changing act remains subject to the project's explicit authorization and scope model. The player agent's social rank, gender, feelings, personality, skill level, contract, campaign, friendship, or group membership must not silently authorize a protected side effect. A human player's chat instruction is not a privileged Governor action unless the existing authority chain explicitly validates it.

The agent may propose, negotiate, accept, or refuse work. The action gateway still validates the relevant permission and ticket. The world action is recorded with actor identity, target, scope, authorization, preconditions, action attempt, actual outcome, and evidence. This keeps the player agent's apparent autonomy from becoming an unbounded privilege escalation path.

## C.17. Research and implementation references

These sources support the technical distinctions in this addendum. They are not a substitute for checking the exact target jar, mappings, and runtime behavior.

1. **NeoForge 1.21.1 `FakePlayer` Javadoc** — `FakePlayer` extends `ServerPlayer`; class description says it is a basic fake server player used to simulate player actions; documents fake connection/network-handler internals.  
   https://aldak.netlify.app/javadoc/1.21.1-21.1.x/net/neoforged/neoforge/common/util/fakeplayer
2. **NeoForge 1.21.1 `FakePlayerFactory` Javadoc** — factory methods `get`, `getMinecraft`, and `unloadLevel`; documents the cache and the weak-reference/world-unload cleanup guidance.  
   https://aldak.netlify.app/javadoc/1.21.1-21.1.x/net/neoforged/neoforge/common/util/fakeplayerfactory
3. **NeoForge 1.21.1 Networking documentation** — server/client communication is message-based; documents registering payload handlers and separating server/client message flow. This supports the mod's structured communication transport, not a claim that a fake player has a client.  
   https://docs.neoforged.net/docs/1.21.1/networking/
4. **NeoForge 1.21.1 logical/physical side documentation** — supports testing on a dedicated server and keeping game logic on the logical server.  
   https://docs.neoforged.net/docs/1.21.1/concepts/sides/
5. **NeoForge issue #1487** — documents a reported FakePlayer/advancement-listener leak scenario on Minecraft 1.21.1; use as a lifecycle test prompt, not as proof that all use leaks.  
   https://github.com/neoforged/NeoForge/issues/1487
6. **Carpet fake-player implementation (reference, not a NeoForge API contract)** — demonstrates a more complete fake-player architecture with a `ServerPlayer`-derived fake player, an action pack, and fake-connection handling. Use it to study player action sequencing and embodiment choices; do not assume its Fabric internals or hooks can be ported directly.  
   https://github.com/gnembon/fabric-carpet/blob/master/src/main/java/carpet/patches/EntityPlayerMPFake.java
   https://deepwiki.com/gnembon/fabric-carpet/4-gameplay-features
7. **Spectrum 1.21.1 NeoForge fake-player helper** — a small real-world example of obtaining a fake player from `FakePlayerFactory` with an agent/owner profile. Useful as a narrow usage reference; it is not a full autonomous citizen implementation.  
   https://github.com/DaFuqs/Spectrum/blob/1.21.1-neoforge/src/main/java/de/dafuqs/spectrum/helpers/FakePlayerHelper.java

**Version and evidence cautions:** the Javadocs describe the target API surface, but method behavior, event delivery, player-list membership, entity tracking, GUI handling, dimension changes, and full action parity still require tests on the exact 1.21.1 NeoForge build. The Carpet source is a cross-loader implementation reference only. The architecture does not assert that a basic `FakePlayer` from the factory is automatically a fully integrated, persistent, autonomous player entity.

---

**Amendment effect:** Addendum C supersedes the initial vertical slice and entity-family rollout order in Addendum B. The primary implementation is player-agent-first. Mob AI replacement remains a secondary, type-specific extension after the fake-player communication and world-interaction system is proven.

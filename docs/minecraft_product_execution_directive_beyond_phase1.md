# Product Execution Directive — Move Beyond the In-Process Phase-1 Bot

**Target:** Minecraft Java 1.20.1 / Fabric  
**Purpose:** Binding implementation order and delivery contract for the actual Ensouled system.  
**Status basis:** The implementation-status report supplied on 2026-10-31. This directive does not claim to have inspected or changed the repository.

---

## 0. The problem this directive corrects

The project has accumulated successful tests and several useful foundations, but the main production architecture is still not present.

The current tree has demonstrated:

- An in-process `FakePlayerBody` backed by `ServerPlayerEntity` and `FakeClientConnection`.
- Player-like movement and selected player interaction semantics inside the server process.
- One skill-driven harvest scenario and persistence across restart.
- A typed message bus and a small number of two-agent transfers.
- A headless chest interaction adapter with reach and inventory checks.
- One real-client, human-directed resource request whose delivery was a system message rather than ordinary player chat.
- An attachable test harness alongside disposable CI probes.

Those are useful foundations. They are **not** the requested final bot architecture.

The missing production transition is substantial:

1. Bots do not yet run as connected headless Fabric clients.
2. Bots do not yet send and receive ordinary player chat through their own client sessions.
3. Humans cannot yet have ordinary conversational chat with bots.
4. The bot's actions do not yet cover the complete ordinary player capability surface.
5. The need system is not extensible to mod-added player needs.
6. The wider System-1 civilization, job, economy, population and independent simulation work has not been implemented.

The project must stop treating further in-process fake-player tests as the main development output. The in-process body remains a legacy/test adapter; it must not define the production ceiling.

**The objective is a population of persistent, headless, network-connected Fabric player clients sharing one world with human players, with full player behavior, visible two-way conversation, extensible needs, and the planned civilization simulation.**

## 1. Binding architecture decisions

### 1.1 One shared server, networked headless clients

- One Minecraft 1.20.1 Fabric server owns the world.
- The human operator joins using an ordinary visible Minecraft client.
- Each production bot runs its own headless Fabric client runtime and connects to that same server as a normal player session.
- Bot identities are persistent agent identities bound to authenticated/registered client sessions. A username, connection, process, or entity UUID alone is not the agent's identity.
- Bot processes must not create their own independent worlds or embedded test servers during normal development operation.
- The bot client must load the compatible client-side Fabric/modpack environment required by the target server. There is no requirement to display a window or render frames.
- A raw protocol-only bot is not an acceptable replacement for the client runtime because it would bypass Fabric client mods, normal client input, client-side menu handling, HUD/state observation and the ordinary chat path.

### 1.2 Headless means no display, not reduced player capability

A production bot is a real client session with the display/rendering workload removed or disabled. It is not a server-side fake player with a larger command list.

The architecture must preserve ordinary player semantics wherever the server and client expose them:

- Player movement, rotation, jumping, sprinting, crouching, swimming, climbing, falling and physical collision.
- Health, hunger, saturation, exhaustion, air, effects, damage, death and respawn.
- Inventory, selected hotbar slot, equipment, durability, pickup, drop and item use.
- Block interaction, mining, placement and use of entities.
- Containers, menus, crafting, workstations, trades and other supported interaction screens.
- Combat, damage feedback, cooldowns, shields, projectiles and relevant effects.
- Chat send/receive, player identity, connection lifecycle and server-authoritative outcomes.
- Modded player capabilities and needs when available through the loaded modpack.

The agent's action layer may abstract these operations into skills, but the action must be executed through the connected player's ordinary client/server interaction path. A server-side helper that mutates inventory or blocks directly is not equivalent to player action.

### 1.3 Legacy body remains useful but is not production embodiment

`FakePlayerBody`, `FakeClientConnection`, and the mob adapter may remain for focused unit tests, deterministic fixture setup or compatibility experiments. They must be labelled accurately as in-process test/legacy adapters.

They must not:
- Be counted as proof of connected-client parity.
- Be used as the production embodiment for the planned shared-server population.
- Be used to synthesize player chat and claim ordinary chat support.
- Silently substitute direct server mutation for missing client behavior.

### 1.4 Modpack parity, not bespoke integrations for every mod

Each bot client should receive a compatible Fabric client profile matching the server's required modpack and versions, with the appropriate client/common mods installed. The system should not require a custom AI integration for every mod merely to join and play on that server.

For mod-added capabilities that the bot needs to understand, use this order:

1. Existing vanilla/player state APIs.
2. Generic client-visible state and HUD/screen observation.
3. A common optional provider interface that mods can expose without being mandatory for every mod.
4. Typed `UNOBSERVABLE` / `UNMAPPED` / `PROVIDER_UNAVAILABLE` states when the information cannot be obtained reliably.

Do not claim universal discovery of arbitrary private mod state. Loading the same modpack gives the client the same mod code and UI; it does not guarantee that every custom need exposes machine-readable semantics.

## 2. What is already complete, and what it does not prove

Treat the following as completed within the scope and evidence stated by the implementation report:

| Work | Status | Limitation |
|---|---|---|
| `EMB-01` in-process embodied being | Complete for its bounded contract | Not a connected client or full player parity proof |
| `EMB-02` chest-backed transfer | Delivered for the supported chest adapter | One container kind; headless adapter differs from ordinary client GUI interaction |
| `HUM-01` human supplies request | One real-client demonstration | One-way request; `[ensouled]` system message, fixed request; not conversational player chat |
| `DEV-01` persistent dev server attach | Delivered as reported | Must continue to be verified without changing the persistent world or disrupting the operator |
| Diagnostic `MessageBus` echo | Available for diagnostics | Explicitly not ordinary player chat and not `CHAT-01` completion |

Do not reopen these deliverables simply to increase their test counts. Only reopen them for a demonstrated regression against their existing contracts.

The following remain unimplemented in the production architecture: connected headless bot clients, actual player chat transport, full player interaction parity, generic extensible need observation, modpack profiles/validation, multi-agent work scheduling, full civilization integration, and population supervision.

## 3. Execution order — product milestones, not more proof-of-concepts

These are binding, bounded milestones. Each has one product outcome and an explicit successor. Once its exit criteria pass, advance. Hardening not required by the exit contract goes into a separate backlog and does not block the next milestone.

### `NET-01` — One real headless Fabric client joins the shared development server

**Purpose:** Replace the production assumption that an in-process `FakePlayerBody` is the final body.

**Deliver:**
- A supervised headless Fabric 1.20.1 client process using the target compatible client modpack.
- It connects to the already-running development server, not a server it launches itself.
- Its normal server-side entity is an ordinary logged-in player entity.
- The client session binds to one persistent `AgentId` through an explicit registration/credential mapping.
- Its world/player state and connection lifecycle are observable by the agent runtime.
- It can disconnect and reconnect without creating a new person or duplicate identity.
- It can execute at least one real client-generated input/action and verify the server outcome.
- It has no visible desktop window and does not require a GPU rendering pipeline.

**Bounded exit evidence:** one headless bot connects to the persistent dev server while the operator remains connected; the operator sees the bot as a real player; the bot moves and performs one real player action; disconnect/reconnect restores the same `AgentId`; the server and world remain running and unchanged except for the intended action.

**Not required to exit NET-01:** complete gameplay, population scaling, all GUI types, all mods, or a finished need system. Those are successor capabilities. Do not turn NET-01 into a full client certification project.

**Mandatory successor:** `CHAT-01`.

### `CHAT-01` — Real player-chat send and receive through the bot client

**Purpose:** Make communication a real player capability, not an internal message bus with a system-message echo.

**Deliver:**
- The connected bot sends an ordinary player chat message through its client session's normal chat-send path.
- The server receives and distributes that message as player chat with the bot's real player identity.
- The bot receives ordinary server/player chat through its client session and passes it into its perception/social system.
- The message bus may continue to carry structured internal speech-act metadata, but internal messages are not themselves proof of visible speech.
- Internal speech acts and player chat are linked by message IDs/provenance so an observer can inspect the underlying communication without confusing a system notification for player speech.

**Mandatory live scenario:** with one bot and the human operator on the same persistent server, the operator types ordinary chat addressed to the bot, the bot receives it, decides a response, and replies as a normal player in chat. Then the bot initiates a message to the operator. No slash command is required for either conversational direction.

**Not accepted:** `PlayerManager.broadcast`, `sendMessage`, `[ensouled]` HUD/system messages, console output, log lines, fake chat overlays, or a fabricated speaker prefix counted as ordinary player chat.

**Mandatory successor:** `CHAT-02` and `SOC-01`.

### `CHAT-02` — Bot-to-bot player chat and visible group conversation

**Deliver:**
- Two independently running headless bot clients connect to the same server.
- One bot sends ordinary player chat; the other receives it through its own client session.
- The recipient independently decides whether/how to answer.
- Both sides' messages appear as ordinary player chat to the human operator.
- The structured message/speech-act record links to the corresponding visible chat event.
- Delivery, refusal, timeout, recipient resolution and reconnection are observable.

**Bounded exit:** two bots complete a multi-turn, non-hardcoded exchange related to a real shared task; a human can watch it and join the conversation. A canned fixed request/accept exchange alone is insufficient.

**Mandatory successor:** `PAR-01`.

### `SOC-01` — Conversational social interface

Build on ordinary chat rather than commands:
- Human messages are perceived as speech acts with sender, content, context, time and conversation scope.
- Agents can ask clarifying questions, acknowledge, decline, negotiate, offer alternatives, report progress and request help.
- Agents retain independent beliefs, goals, memory and agency. A chat message is neither an unquestionable fact nor automatic authorization.
- The system supports direct address, replies, group conversation and agent-to-agent coordination.
- The human can speak naturally in chat. Commands remain for administration, diagnostics and explicit machine-readable inspection, not as the only human-to-agent interface.
- Rate limits and abuse handling must not prevent ordinary development interaction.

**Bounded exit:** the operator can give a bot an ordinary-language request, the bot can clarify or decline, and—if it accepts—convert the request into a scoped task and report the result in chat.

### `PAR-01` — Full player capability inventory and client-driven motor

**Purpose:** Replace the current narrow motor as the production action ceiling.

Create a capability matrix with one row per player capability, including: input source, client operation, server authority, observed result, persistence behavior, unsupported conditions and evidence. Cover the complete categories in §1.2.

Implement the client-driven motor and perception/control seam so the System-1 mind selects skills, but the headless client executes the same kinds of input and interaction operations a human client uses.

The milestone must include at least:
- Movement and locomotion modes.
- Look/aim and hotbar selection.
- Block breaking and placement.
- Item use in air and on blocks.
- Entity interaction and attack.
- Inventory manipulation through player semantics.
- Container/menu opening, slot actions and closing through the client interaction path.
- Crafting/workstation interaction through actual screen/menu semantics.
- Health, hunger, damage, death and respawn observation.

Unsupported capabilities must return typed refusals; the agent may not route around a failed player operation with arbitrary server-side state mutation.

**Bounded exit:** a real headless client performs a multi-step survival task requiring at least movement, inventory, block interaction and a container/menu action; the human can interrupt, alter the world, and observe the bot adapt. A capability inventory must distinguish implemented, tested, partial and unsupported entries.

**Mandatory successor:** `NEED-01`.

### `NEED-01` — Generic player needs and mod-added need observation

**Purpose:** Make needs a data-driven part of System-1 rather than a single vanilla food-level special case.

Implement a `NeedProvider`/`NeedObservation` model with stable provider identity, subject/player identity, value, units, bounds or categories, confidence, source, observation time, staleness, and capability/refusal state.

Start with actual vanilla player state, including at least food level, saturation, exhaustion, health, air, status effects and relevant movement/environment state. Do not infer a need solely from a display icon when a reliable underlying client state is available.

Support mod-added states through the common discovery strategy in §1.4. The client loads the same modpack; generic observation can discover visible UI/state where possible; known provider APIs may expose additional values; unknown private values are not invented.

System-1 must reason about needs as part of salience, intention selection and planning. A need is not just a HUD label. The bot must be able to identify a deficit, choose an available remedy, execute the player action, and verify the resulting state change.

**Bounded exit:** vanilla hunger/saturation is observed and drives a real decision; at least one additional state provider works through the generic interface; provider absence/unmapped states are explicit; a test mod can demonstrate a mod-added need without changing the core mind implementation. The test mod is a compatibility fixture, not a required integration for every modpack mod.

**Mandatory successor:** `CIV-01`.

### `MOD-01` — Modpack parity and startup compatibility

**Purpose:** Let the bots join modded Fabric servers without requiring a custom AI adapter for every mod.

Implement a manifest/profile for client/server modpack compatibility: Minecraft version, loader version, mod identifiers and versions, required client/common/server placement, dependencies, configuration, resource packs where relevant, and mismatch reporting.

Each bot client must load the compatible client-side pack. A server-only mod should not be copied into the client profile if its loader metadata says it is server-only; required client/common mods must be present. Validate the actual handshake and gameplay on the target server.

Mod compatibility is layered:
- Join/protocol compatibility: the bot can connect with the same compatible modpack.
- Player capability compatibility: the client's mods and server mods participate in normal player mechanics.
- Cognitive understanding: the agent understands a modded block/item/need only when its generic perception, visible UI, known mechanics or optional provider makes that information available.

Do not claim the third layer is automatically solved by the first two.

**Bounded exit:** one representative modded Fabric profile can be launched by the supervisor, connect to the shared server, observe a mod-added player state or interaction, and report unsupported semantics honestly.

### `COOP-01` — Agents independently coordinate multi-step work

The current chest transfer is one supported task pattern, not a work-order system.

Implement durable work orders/contracts with:
- Goal, scope, requested outputs and success postconditions.
- Assignment, acceptance, refusal, delegation and progress reporting.
- Reservations/claims for resources and workspaces.
- Independent agent plans and separate inventories/minds.
- Dependencies, blocking reasons, timeouts and reassignment.
- Canonical evidence and outcome verification.

**Bounded exit:** at least two bots coordinate a multi-step task involving acquisition, transport, a container or workstation, and a final world-state result. The human can intervene by chat or by changing the world; bots perceive the change and adapt rather than invalidating the entire session.

### `CIV-01` — Integrate agents into the actual civilization simulation

This is the first milestone that must visibly resemble the intended product rather than a test harness.

Integrate the agents with the planned city-builder/civilization substrate:
- Settlements, households and population records.
- Needs and services.
- Jobs, skills, responsibilities and schedules.
- Production chains, recipes, storage and logistics.
- Construction and maintenance.
- Resource accounting, reservations, stock and delivery.
- Ecology and environment where modelled.
- Threat response, defense, exploration and expeditions where supported.
- Social relationships, reputation, memory, affect and history in their planned order.
- Story/event recording derived from real causal world events rather than scripted chat.

The world simulation must distinguish what is physically enacted in loaded chunks from what is represented virtually. Virtual simulation may advance explicitly modelled needs, schedules and regional work; it must not claim that blocks were mined, crops ticked, containers changed or combat happened in unloaded chunks.

**Bounded exit:** a small settlement with multiple connected bots has jobs, shared stock, a production/delivery chain, needs that affect work priorities, and persistent social/work history. A human can join, converse, interrupt, help or obstruct, and see the consequences.

### `SIM-01` — Cognition and identity independent of loaded body

Implement persistent agent records plus materialized and virtual/regional representations. An unloaded client/body must not erase personhood, memory, relationships or responsibilities.

The cognition scheduler needs per-agent due ticks, budgets, fairness, wake-up events and failure accounting. A connected/materialized client is required for physical action, but not for explicitly modelled cognitive and regional simulation.

**Bounded exit:** unload/disconnect a bot without killing it; its identity, needs, schedule, memory and social state continue under the virtual model; reconnect/materialize the same identity and reconcile physical state without duplicating events or inventing world actions.

### `POP-01` — Managed persistent population

The population manager maintains an operator-selected resident identity target. Initial provisioning fills the target. Temporary disconnects, client crashes, server restarts and ordinary deaths do not create replacement identities. A bot reconnects with the same `AgentId` unless the project explicitly records permanent death under the governing death policy. Only a qualifying permanent death grants one replacement credit.

Separate resident population from currently connected client count and from concurrent process capacity. Process limits are based on measured CPU/RAM, not an arbitrary population promise. Bots may be virtual while not materialized, subject to the rules in `SIM-01`.

**Bounded exit:** a configured small population survives client/server restarts; disconnected agents reconnect as themselves; permanent-death accounting creates at most one replacement; total resident identities never exceeds the configured rule.

## 4. Required product vertical slice

Do not let the roadmap devolve into a long chain of disconnected test harnesses. The first integrated product demonstration must be:

1. One persistent Minecraft 1.20.1 Fabric server.
2. The human operator joins normally and remains connected.
3. Two independently supervised headless Fabric bot clients join the same world.
4. Both are registered to persistent `AgentId`s and appear as ordinary player entities.
5. The human types ordinary chat to one bot without a slash command.
6. The bot understands the request, asks a clarification or responds, and communicates with the second bot through ordinary player chat.
7. The bots independently agree to perform a multi-step task.
8. They move, gather or retrieve materials, use an ordinary container/workstation path, and produce a verifiable world result.
9. Their hunger/needs influence their choices and are updated from actual player state.
10. The human can interrupt by chat or physical world action; the bots perceive and adapt.
11. The world, bot identities, conversation/evidence history and human connection persist across test runs and server restart.
12. No visible bot windows are launched.

This is a product milestone. A set of isolated unit tests, fake chat echoes, a direct inventory mutation, or a screenshot of a spawned fake player cannot substitute for it.

## 5. How tests and implementation must relate

The test harness is infrastructure, not the product roadmap.

Maintain three distinct test modes:

1. **Unit tests:** fast pure logic tests that do not start Minecraft.
2. **Disposable CI integration tests:** clean isolated server/world, useful for deterministic regression. These must never delete or attach to the persistent development world.
3. **Persistent dev-server acceptance runs:** attach to the already running server and retain the operator's session, bots, world and state unless a test explicitly owns a small fixture region.

For persistent tests:
- Fixtures must be individually owned, labelled and cleaned up.
- Do not reset the whole world between phases.
- Do not disconnect or spectator-convert the human operator.
- Do not add a dummy watcher fake player as a substitute for the operator.
- Human interference is allowed and recorded. Tests that require deterministic conditions must use a bounded fixture or explicitly account for the observed interference; they must not ban the human from the server.
- Test status, progress, cancellation and evidence must be visible while the server continues running.

A green test suite only establishes the contracts those tests actually exercise. It does not establish future phases, full parity, native chat or the civilization system.

## 6. Mandatory anti-drift rules

These rules override any tendency to stay inside the easiest test loop.

1. **No phase inflation.** A milestone's exit criteria are frozen when work starts. Non-blocking edge cases become follow-up hardening tickets.
2. **No test-only successor.** The successor after a passing milestone must implement a user-visible or in-world product capability, not merely add another isolated probe.
3. **No repeated proof of completed work.** Do not rerun and expand `EMB-01`/`EMB-02` to avoid implementing `NET-01`, except to diagnose a demonstrated regression.
4. **No fake substitute for a real capability.** System messages are not player chat; fake players are not connected headless clients; direct inventory mutation is not player interaction; a local gate is not the canonical Axis; a HUD label is not proof of a need model.
5. **No premature abstraction-only milestone.** New interfaces must be connected to an actual vertical slice that uses them.
6. **No blanket test isolation by banning the operator.** Preserve the operator's normal player role.
7. **No claim inflation.** Mark each requirement `implemented`, `verified in unit test`, `verified in disposable server`, `verified in persistent server with real client`, `partial`, or `not implemented`. Do not use one category as another.
8. **Always maintain an active successor.** Every completed ticket must name the next active ticket and the exact observable capability it will deliver.
9. **Implementation-first reporting.** Report what the human can now see/do in the running world before reporting test counts.
10. **A milestone cannot be declared complete based only on a mock, system-message echo, internal `MessageBus` event, direct server mutation, or fake-client test.**

## 7. Immediate assignment to the development agent

The next active ticket is **`NET-01`**, not another extension of the phase-1 fake-player suite.

Before coding, perform only the repository inspection necessary to establish:
- Existing Fabric client launch/runtime approach and whether HeadlessMC or another headless runtime candidate is actually usable for Fabric 1.20.1.
- How the running dev server address, identity and authentication are supplied.
- How the agent process receives client observations and issues inputs without requiring a display/GPU.
- How compatible client modpack profiles are assembled.
- What from `FakePlayerBody` can be reused as cognition/test logic and what must be replaced by a real client session.

Then implement one connected headless client. Do not spend this ticket exhaustively testing every player capability. Do not implement the full population manager first. Do not rebuild the disposable probe server. The goal is one real, headless, connected player agent in the existing persistent world, with its identity bound and one verified client-driven action.

At NET-01 exit, immediately activate `CHAT-01` and continue through the successor chain above. No further planning-only addendum is required to begin implementation.

## 8. Required status report format

At every milestone boundary, report:

- **In-world product behavior delivered:** exactly what a human can see the bot do now.
- **Runtime path:** in-process fake body, connected headless client, or other explicitly named path.
- **Evidence level:** unit, disposable server, persistent server, real human client.
- **Capabilities newly unlocked:** the next kinds of work now possible.
- **Known unsupported behavior:** honest typed list.
- **Deferred hardening:** separate from blockers.
- **Next active ticket:** identifier plus observable deliverable.

Do not report “phase complete” without this information.

---

## Final binding statement

The first fake-player slice proved that a server-side player-shaped body can execute a small number of real player mechanics. That was a foundation, not the product.

The next work must create connected, headless Fabric player clients and then build actual chat, player parity, needs, cooperative work and civilization behavior on top of those sessions. Tests must verify that product as it grows; tests must not become the thing the project is perpetually delivering.

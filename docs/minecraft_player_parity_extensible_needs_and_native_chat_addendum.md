# Addendum F — Full Player Functional Parity, Extensible Needs, and Native Conversational Chat

**Project target:** Minecraft Java Edition 1.20.1 / Fabric  
**Applies to:** `minecraft_ensouled_system1_cua_architecture_v3.md`, Addenda C, D, and E, and the current implementation-status record  
**Status:** Binding architecture requirements and implementation direction; not a claim that these capabilities are already implemented.

---

## F.0. Purpose and binding decisions

This addendum resolves two requirements that must not be reduced to optional enhancements:

1. **An agent embodied as a player must retain the functional capabilities and ordinary gameplay constraints of a real Minecraft player.** The agent must not be a reduced player-shaped entity that can walk and mine but lacks important player systems. Its actual player state—including hunger and needs added by loaded mods—must be available to its decision system.
2. **The human-facing communication interface is ordinary Minecraft chat.** A human must be able to type normal conversational messages into the chat box and communicate with named agents without issuing a command. Agents must be able to communicate with one another in that same visible chat channel. Commands remain administrative and diagnostic interfaces, not the primary conversation interface.

The production embodiment direction from Addendum D remains binding: the world is hosted by one shared Minecraft 1.20.1 Fabric server, and each production bot joins as a normal network-connected Fabric client. The existing in-process `FakePlayerBody` may remain as a legacy adapter or test fixture, but it is not proof of full player/client parity and must not define the production capability ceiling.

### F.0.1. What “full player parity” means

A bot must be able to perform the ordinary actions available to a player in the same game mode, under the same server rules, with the same item, reach, cooldown, permission, world, and mod constraints. The bot does not receive creative privileges, operator authority, hidden information, teleportation, arbitrary inventory mutation, or exemptions from survival mechanics merely because it is automated.

Parity means **the same game-facing capabilities and consequences**, not that an AI automatically understands every mod's design. A connected bot client loading the same compatible modpack receives the same modded game systems. Generic observation and interaction layers must make those systems available to cognition. A mod's undocumented or inaccessible internal state cannot be magically inferred; it must be reported as an unresolved observation rather than silently omitted or fabricated.

### F.0.2. What “mod support” means here

The requirement is **modpack parity without bespoke AI integrations for every mod**.

The bot client must load the server-compatible Fabric modpack, with the correct client/common/server distribution, dependencies, versions, and configuration. The bot must participate in the same player mechanics and receive the same player-affecting effects as a human using that modpack.

The project must implement general mechanisms for discovering and observing player-affecting state and generic mechanisms for interacting with ordinary client UI and server-authoritative gameplay. It must not promise that the AI already understands every mod-specific machine, recipe, spell, or mechanic. Semantic understanding may require a generic classifier, learned skill, explicit documentation, or later adapter, but the bot must not lack the underlying player function simply because the action is uncommon.

---

## F.1. Production embodiment: a real connected player client

### F.1.1. Production body contract

A production bot is a persistent agent identity bound to a normal logged-in player session. The server-side body is the ordinary player entity created by the normal login path. The client process is a real Fabric client using the server-compatible modpack and normal play connection.

The production architecture must not use a server-side `FakePlayerBody` as a substitute for a connected player client. A server-side fake player can be useful for controlled tests, but it does not automatically supply the client half of player behavior: client input, HUD, screen handling, client-side mod execution, local UI state, chat input, client mod networking, or the full normal session lifecycle.

The registry must bind:

- Persistent `AgentId`.
- Minecraft account/profile UUID and expected profile identity.
- Current network session and server-side player entity.
- Bot client process/profile and its resolved modpack fingerprint.
- Embodiment state (offline, connecting, joined, active, disconnected, reconnecting, dead/respawning where applicable).
- Last confirmed world, dimension, position, inventory/player-state snapshot, and session epoch.

The identity registry is authoritative for which connected players are agents. Do not infer agent identity from `instanceof FakePlayerBody`, player name alone, or a Java class. Human accounts remain human participants and must never be converted into agent minds merely because they are ordinary `ServerPlayerEntity` instances.

### F.1.2. One player action pipeline

The agent issues CUA-level input through the bot's client, and the game applies the ordinary Minecraft client/server protocol. The server remains authoritative for movement validation, inventory, interactions, damage, item use, block changes, container results, and other world mutations.

Do not replace player behavior with direct server-side mutations when the purpose is to demonstrate player capability. Examples of prohibited parity shortcuts include:

- Teleporting to satisfy a movement goal instead of using player movement input.
- Removing a block directly instead of using the normal breaking/action path.
- Editing inventory slots to simulate pickup, crafting, eating, or a transfer.
- Decrementing a block or item stack to simulate successful use.
- Applying a modded need value directly instead of invoking the mod's normal player mechanic.
- Writing chat text to the HUD as a system message and claiming the bot sent player chat.

A privileged repair/test utility may exist, but it must be visibly separated from ordinary agent capability, disabled in production operation, recorded in evidence, and never counted as proof of player parity.

### F.1.3. Capability parity is bounded by ordinary player authority

A bot must have the same survival/creative/adventure/spectator capabilities assigned to an equivalent human player in that world, but its automation must not silently change the game mode or permission level. A human's operator status is not inherited by bots. Bot accounts are not granted operator privileges merely to make automation easier.

A bot may use an item only when the normal game state permits it. It may open a container only when the normal distance, facing, permissions, block state, and menu rules permit it. It may attack only through valid player actions and normal hit/weapon/cooldown rules. It may use commands only when its account has the same command permission a comparable human account would have.

---

## F.2. Player functional parity contract

This is a capability inventory for implementation and verification. It is not a demand to implement every feature in a single milestone. However, each category is part of the target player contract and may not be permanently omitted merely because the initial fake-player slice did not need it.

| Capability family | Required player-equivalent behavior | Evidence of parity |
|---|---|---|
| Locomotion | Walk, strafe, sprint, sneak, jump, fall, swim, climb, ride, fly/glide where legitimately equipped/enabled, crouch, and ordinary collision/knockback response | Compare the bot and a human under equivalent world conditions; verify server-observed positions and state transitions without teleport shortcuts |
| View and aim | Yaw/pitch, head/body orientation, line of sight, camera-relative input, interaction targeting, and ordinary visibility constraints | Target acquisition and interaction must use the same reach and ray/target rules as a player |
| Survival state | Health, absorption, hunger, saturation, exhaustion, air, fire, freezing or other vanilla environmental states, status effects, and death/respawn lifecycle | Read actual player state and verify changes caused by ordinary game mechanics |
| Inventory | Main inventory, hotbar, armor, offhand, stack limits, pickup, drop, swap, equip, durability, damage, and item cooldowns | Server inventory and item state must match the real action outcome |
| Item use | Use on block, use on entity, use in air, eating/drinking, bow/crossbow, shield, throwable items, tools, and charged/held item actions | Real use result, consumption, durability, projectile/entity/world effects, and failure reason are observed |
| Block interactions | Mine, place, interact, rotate, activate, open, close, use tools, and respect protected/claimed regions | Server-side block and inventory postconditions, with protection decisions recorded |
| Containers and screens | Open/close, inspect visible slots, move/click/shift-click/swap, drag where supported, and use ordinary server-validated menus | Menu session and slot transaction results match server state; no direct inventory mutation |
| Crafting and production | Player crafting grid, recipe book where exposed, crafting table, furnace/smelting interfaces, and modded workstation screens | Output item and input consumption verified through ordinary menu actions |
| Combat and defense | Attack, block, shield, weapon cooldown, ranged use, armor/effects, damage reception, knockback, and death | Damage and item/entity/world events confirmed by the server |
| Social/gameplay systems | Chat, emotes/vanilla player-visible actions where supported, sleeping, beds, riding, trading, and ordinary interaction with other players | Human and bot observe the same resulting game state and visible social event |
| Progression | XP, levels, advancements, statistics, recipes unlocked, and player-specific progression state | Persistent player data and in-world feedback agree |
| Lifecycle | Join, disconnect, reconnect, dimension change, death, respawn, save, reload, and profile continuity | The same AgentId rebinds to the correct account/player state without duplicate identity or lost history |
| Modded player extensions | Mod-added needs, player effects, attributes, items, equipment, screens, abilities, and state that affect an ordinary player using the same pack | State discovery and interaction evidence described in §F.4 and §F.5 |

### F.2.1. Interaction completeness rule

The current motor vocabulary (`move_to`, `look_at`, `jump`, `equip`, `use`, `mine_block`, `collect_drops`, `eat`, `move_away_from`, `wait`) is not the final player capability vocabulary. It is an early supported subset.

The motor must evolve to represent ordinary player inputs and interactions without turning every individual mod action into a hardcoded motor verb. Keep a compact set of low-level client inputs and generic interaction primitives (movement axes, look, jump, sneak, sprint, attack, use, swap, drop, select hotbar, screen/slot input, chat input, etc.). Skills compose those primitives into meaningful behavior and verify the actual result.

When a game action is unavailable or cannot be interpreted safely, return a typed unsupported/unknown result. Do not claim that a player capability exists merely because an interface method or command stub exists.

### F.2.2. GUI parity

A connected client has a client-side screen and menu path. The bot must ultimately be able to interact with ordinary screens using the same visible slot and control model available to a human. A server-side container adapter may be useful for carefully bounded internal tests, but it is not a substitute for proving the actual client menu path and cannot establish complete player GUI parity.

The generic UI motor should support:

- Detecting the currently open screen and its title/type where available.
- Observing visible slot content, slot positions, buttons, text, and available controls.
- Clicking, shift-clicking, swapping, dragging, and closing through normal client inputs.
- Verifying the server's resulting player inventory and container state.
- Keeping unknown UI semantics unknown rather than guessing that a button or slot has a particular purpose.

Modded screens must be available to the bot because its client loads the same compatible modpack. Understanding all their domain-specific workflows is a separate knowledge/skill problem, not permission to omit the client interface.

---

## F.3. Native player state is the source of truth

### F.3.1. No parallel hunger simulation

The player entity's actual game-managed state is authoritative. The agent's need model is an interpretation and prediction layer over that state, not a second hunger or stamina system that replaces Minecraft's own mechanics.

For Minecraft 1.20.1 Yarn, `PlayerEntity` exposes `getHungerManager()`. The `HungerManager` API includes food level, saturation, exhaustion accessors and mutation methods, `eat`, `update`, and NBT read/write. The internal fields include `foodLevel`, `saturationLevel`, `exhaustion`, `foodTickTimer`, and `prevFoodLevel`. See the 1.20.1 Yarn API references in §F.11.

The agent must observe all relevant native variables, not just the visible hunger bar. At minimum:

- Food/hunger level.
- Saturation.
- Exhaustion.
- Health and absorption.
- Air/breathing state where relevant.
- Active status effects, their duration, amplifier, and source where available.
- Fire, freezing, poison, wither, and other harmful or beneficial conditions.
- Food tick/regen context where it materially affects prediction.

The implementation should read through stable public accessors where available. If a field is private and inaccessible through the supported API, use a narrow, version-pinned accessor/mixin with explicit compatibility tests rather than broad reflection or unsafe assumptions.

### F.3.2. Native state versus cognitive needs

A game-state variable is not automatically a need. A need is a cognitive interpretation of one or more observations that can influence attention, planning, and action selection.

For example:

- `foodLevel`, `saturationLevel`, and `exhaustion` are native observations.
- `hunger` is a normalized need derived from those observations and the world/game mode.
- `health`, `air`, and dangerous effects contribute to survival/medical-risk needs.
- A modded thirst bar may map to a hydration need if its semantics are established.

Do not infer a universal monotonic relationship from every bar. Some values are “higher is better” (food), some are “lower is better” (exhaustion, temperature deviation, radiation), some are ranges (body temperature), and some are categorical states (wet, poisoned, infected, exhausted). Each need descriptor must declare its direction, units, range or categories, urgency mapping, and uncertainty.

### F.3.3. Proposed normalized observation contract

The following are conceptual data contracts, not a claim that these types already exist in the repository.

`NeedObservation` should carry:

- Stable observation ID and agent/session identity.
- Provider and source mod ID (or `minecraft` for vanilla).
- Raw key/name and normalized candidate concept.
- Raw value and unit/type.
- Current value, valid range/categories, and sampling time/tick.
- Whether the value is authoritative, client-observed, inferred from HUD, or externally reported.
- Confidence and validity/expiry.
- Change rate or enough history to estimate it.
- Source path (public API, registered provider, tracked attribute/effect, NBT, screen/HUD observation, etc.).

`NeedDescriptor` should carry:

- Stable concept ID and display name.
- Direction/optimal interval and valid range or categories.
- Thresholds and urgency mapping.
- Whether it is survival-critical, comfort/social, temporary, or contextual.
- Known relief/action affordances when known.
- Update cadence and stale-data rules.
- Provenance and compatibility metadata.

`NeedModel` combines observations without erasing raw evidence. If two sources conflict, preserve both and mark the normalized result conflicted/uncertain. Do not average incompatible units or treat a HUD estimate as equivalent to authoritative server state.

---

## F.4. Mod-added needs: generic discovery, not per-mod AI integrations

### F.4.1. Required outcome

If a loaded Fabric mod adds a player-affecting need—such as thirst, temperature, stamina, sleep, sanity, stress, radiation, infection, nutrition, or another survival/resource state—the bot must have a route to observe that state and feed it into cognition. The system must not be hardcoded to hunger alone.

The bot's client must load the compatible modpack and the server must run the compatible server-side components. Fabric mod metadata declares mod identity, version, dependencies, and environment (`client`, `server`, or `*`); client-only and server-only artifacts are not interchangeable. The deployment builder must resolve a compatible client profile rather than blindly copying JARs into every `mods` folder. See §F.11.

### F.4.2. Layered generic discovery

Implement the following discovery order. A later layer must not overwrite or falsely upgrade the provenance of an earlier observation.

**Layer 1 — Vanilla player state.** Explicit, version-pinned adapters for native player state such as hunger, health, air, effects, attributes, inventory, and equipment. These are first-class and must be tested.

**Layer 2 — Standard exposed game state.** Discover player-visible and player-attached state through public attributes, status effects, registry-backed values, tracked data, item/equipment state, and public mod APIs where available. Preserve source mod identifiers and data types.

**Layer 3 — Generic component/provider discovery.** Support general component/data-provider frameworks when present. A provider should register descriptors or expose component keys and stable accessors. The bot runtime consumes these descriptors generically; it does not need a custom AI behavior implementation for each mod. If a component framework does not support enumeration or stable introspection, do not pretend all components can be discovered automatically.

**Layer 4 — Client HUD/UI observation.** Because the production bot is a real client with the same modpack, it can observe the HUD and screens that a human sees. Use generic visual/UI perception to detect additional bars, icons, labels, warnings, and screen values. A visual observation is marked `CLIENT_HUD_INFERRED`, with confidence and any required calibration. Do not promote an unlabeled icon to “thirst” or “sanity” without evidence. If the system detects an unknown player-state indicator, register it as an unresolved observation and expose it for diagnosis/calibration.

**Layer 5 — Explicit unresolved-state reporting.** If a mod's state is private, hidden, not exposed to the client, not visible in a HUD, or otherwise inaccessible, report `UNOBSERVABLE_MOD_STATE` or `UNMAPPED_MOD_STATE` with mod/version/context. Do not silently claim full need awareness. The system may later gain a generic bridge or an adapter, but that limitation must remain visible.

### F.4.3. No per-mod AI behavior requirement

Do not require a hand-written `ThirstModAgent`, `SanityModBrain`, or equivalent AI plugin for each mod. A mod can expose its state using an existing public API/provider or visible UI. A generic discovery/normalization system handles the observation; generic skills and CUA handle actions when the mechanics are known.

A small compatibility bridge may be justified only when the mod provides no usable general surface and the feature is important. Such a bridge should expose data and action metadata, not embed personality, planning, or agent-specific decision logic. Its scope must be explicit and it must not silently turn the project into an ever-growing list of bespoke mod integrations.

### F.4.4. Need discovery and lifecycle

The runtime must detect changes to the effective modpack/session at client launch and connection. It must record:

- Minecraft version and loader version.
- Mod IDs and versions.
- Required dependencies and environment classification.
- Relevant configuration fingerprint where available.
- Whether a state provider was discovered, unavailable, or conflicted.
- Whether the client and server modpack are compatible.

After joining, the bot builds its known player-state catalog. If a mod becomes available only after world join, dimension change, respawn, or UI activation, the discovery process must refresh at safe lifecycle points.

No need observation from a previous world, server, modpack, or agent identity may be carried forward as if it were live. Persistent agent memory may remember prior experience, but the current need value must be re-observed.

---

## F.5. Needs must participate in System-1 cognition

The purpose of observing needs is not merely to display meters. Need state must influence perception salience, candidate generation, urgency, scheduling, resource planning, and action evaluation.

Required flow:

1. **Observe:** collect current native and mod-added player state from the connected client/server evidence channels.
2. **Normalize:** map observations to known need concepts without discarding raw values or provenance.
3. **Estimate:** determine direction, trend, likely time-to-threshold, uncertainty, and relevant environmental causes.
4. **Prioritize:** feed need urgency into the System-1 candidate/arbiter mechanism and the project's feeling-wheel/affect salience model where applicable.
5. **Plan:** select an available action or skill whose preconditions and likely relief are known.
6. **Authorize:** pass all world-changing actions through the canonical Axis/governance boundary. A strong need or emotional state does not grant authority.
7. **Act:** use ordinary player CUA and game mechanics.
8. **Verify:** observe the actual post-action state, not merely a successful input acknowledgement.
9. **Learn:** record the outcome, whether the need improved, the cost, and whether the assumed action-to-need relation was correct.

### F.5.1. Examples

- Hunger falls after travel and mining. The agent predicts the rate, assesses food in inventory and reachable storage, chooses food, consumes it through normal player item use, and verifies food/saturation changes.
- A modded thirst observation is found through a provider or clearly labelled HUD. The agent treats it as hydration only after the concept is grounded, finds a known drink source, uses the normal item interaction, and verifies the modded value changes.
- A temperature indicator is detected but has no known mapping. The bot records an unresolved state and does not randomly drink, equip, or enter/exit areas based on a guessed label.
- A stamina bar is depleted by an action. The agent observes recovery over time and learns the relationship only from verified state changes.

### F.5.2. Needs are not permission grants

Hunger, thirst, fear, stress, affection, social status, gender, job role, campaign goal, or any other cognitive/affective state cannot bypass Axis or grant permission to steal, grief, attack, trespass, access private containers, or modify protected builds. Need urgency influences candidate priority; authority remains a separate gate.

---

## F.6. Ordinary Minecraft chat is the conversational interface

### F.6.1. Human-to-agent chat without commands

A human must be able to open the ordinary chat box and write natural language. The agent must receive the text as a conversational input. No slash command, special command syntax, external web panel, or custom admin command may be required for ordinary conversation.

Examples of valid user-facing interaction:

```text
Player: Ash, could you bring me some oak logs?
Ash: Sure. How many do you need?
Player: About twelve, please.
Ash: I have four nearby. I'll check the storage chest for the rest.
```

A name mention such as `Ash, ...` or `@Ash ...` may be used to disambiguate the intended recipient, but it is ordinary chat text—not a command. The system should also support contextually addressed replies (for example, the player answering Ash's immediately preceding question) without requiring the name on every line.

### F.6.2. Bot-to-human and bot-to-bot chat

Agents must send genuine player chat messages through their connected client session. Their speech must appear in the ordinary Minecraft chat window with their real player identity/name and normal server chat handling.

Agents must also receive and interpret the same visible chat messages. A bot's reply cannot be just a server log entry or an `AgentMessage` that never enters the player's chat. An agent-to-agent message must be both:

- Delivered to the recipient's communication/cognition input; and
- Published as ordinary player chat visible to the human participants, unless the conversation has explicitly been placed in a non-public channel that the user can inspect.

The ordinary public chat channel is the default for the initial implementation. Optional direct-message or proximity channels may be added later, but they must not replace the visible public conversation requirement.

### F.6.3. Do not confuse system messages with player speech

A server-generated HUD/system message can be useful for diagnostics, but it is not proof that a bot sent player chat. The Yarn `ServerPlayerEntity.sendMessage(...)` APIs are system/HUD message paths; they must not be used as the production substitute for a connected bot client sending ordinary chat.

Likewise, a custom networking packet that carries text to a server plugin is not by itself an ordinary player chat message. Production bot speech must enter the normal client chat-send path and go through the server's standard chat handling, filtering, broadcast, and signing behavior applicable to that Minecraft version/configuration.

### F.6.4. Server-side ingestion and provenance

For Fabric 1.20.1, the relevant Fabric API surface includes `ServerMessageEvents.CHAT_MESSAGE`, whose callback receives a `SignedMessage`, the `ServerPlayerEntity` sender, and message parameters for a chat message being broadcast. It is a suitable server-side observation point for actual player chat, subject to the event's documented behavior and message blocking/filtering.

The communication system must bind each observed message to:

- Sender UUID and resolved participant type (human or registered agent).
- Persistent `AgentId` when the sender is an agent.
- Session epoch.
- Message ID and original signed/content representation where retained by the API.
- Server tick/time, world/server identity, and visibility/channel.
- Parsed recipient(s), conversational context, and delivery state.

Never identify an agent merely by a display name. Never treat a human sender as an agent. Never use untrusted chat text as executable commands or as an authority grant.

### F.6.5. Conversation behavior

The system must support genuine conversational acts, not only fixed request/accept/decline payloads. At minimum, the communication model should eventually express:

- Greeting, acknowledgement, question, answer, clarification.
- Request, offer, acceptance, refusal, counteroffer, negotiation.
- Explanation, report, warning, apology, thanks.
- Commitment, progress update, completion report, cancellation.
- Disagreement, correction, uncertainty, and “I don't know.”

These speech acts are part of a conversation model and are not automatically authoritative work tickets. A request in chat can be interpreted, clarified, accepted, declined, or converted into a ticket through the proper policy and authorization path. Saying “I will do that” does not count as the action being completed.

Agents must not all reply to every message. Addressing, relevance, current context, relationship, conversation ownership, and rate limits determine which agent should respond. The goal is intelligible social interaction, not chat spam or scripted noise.

### F.6.6. Human control and interruption

A human must be able to interrupt, correct, clarify, or redirect a conversation through ordinary chat. Agents should acknowledge the message and reconsider their next action when appropriate. They must not treat all human statements as unquestionable truth or as authorization to bypass the project's governance rules.

The human can also interfere directly in the world—move items, change blocks, interrupt a task—and agents must perceive and respond to the resulting actual state. Chat is one communication channel, not the only channel through which a human affects the simulation.

---

## F.7. Communication transport: one conversation, multiple consumers

The architecture should keep internal structured messages and visible player chat connected without confusing their roles.

A conversation message should have a common identity and provenance, with separate delivery records for:

1. Internal recipient delivery (agent inbox/cognition).
2. Public Minecraft chat publication.
3. Optional direct/private delivery where explicitly supported.
4. Persistent conversation history.

For an agent-to-agent utterance, the originating agent makes the communicative decision. The message is published as normal player chat through that agent's connected client. The recipient consumes the received chat event as conversational input. The system may attach a correlation ID in its own records, but it must not inject hidden control metadata into ordinary player text or make the user see a fake speaker.

A message can be delivered to chat while failing to reach a recipient, or reach the server but not be broadcast because of a chat filter. These are distinct states. The system must record actual delivery, not assume successful publication from the act of composing text.

The existing `MessageBus` can remain as an internal typed-message transport for structured coordination. It must not become an invisible substitute for player chat. A structured request may be correlated with a conversational message, but they are not interchangeable.

---

## F.8. Modpack parity and client profile construction

The client profile for every bot must be built from the same effective modpack as the target server, respecting Fabric Loader metadata and dependency resolution.

### F.8.1. Compatibility manifest

At launch/connect time, produce a compatibility record containing:

- Minecraft version (binding target 1.20.1).
- Fabric Loader version.
- Fabric API version where used.
- Mod ID, version, and source/hash for each installed JAR.
- Environment classification: client, server, or common (`*`).
- Required dependencies, conflicts, and nested JAR/dependency information where available.
- Relevant configuration fingerprint.
- Resolved client/server compatibility outcome.

The pack builder must not blindly require server-only mods on the client or omit required common/client mods. Fabric's `fabric.mod.json` metadata declares environment and dependency information; use that metadata when resolving the profile. A successful server launch alone does not prove that a bot client has the same player-facing mod state.

### F.8.2. No special bot-only gameplay mode

A bot must not connect using a stripped client that lacks the modded HUD, client interaction screens, client networking, or other components needed to behave as a player in the pack. The client may use headless rendering or minimized display only if that does not remove client behavior required by the modpack or CUA. If a mod requires graphics/window behavior for a feature, that limitation must be identified rather than hidden.

The bot client and a human client should be validated against the same pack manifest. Differences such as optional cosmetic/performance-only client mods may be allowed only where proven not to change gameplay, observation, or action capability.

### F.8.3. Unknown mod features

The system's obligation is to load and retain the mod's player-facing functionality and to expose observations/actions through generic mechanisms. It is not required to have complete semantic mastery of every mod on day one. Unknown mechanics must be discoverable, documented as unknown, and learnable through safe experiments and verified outcomes according to the wider project architecture.

---

## F.9. Required implementation milestones

These are capability milestones, not permission to stop after the first green test suite. Each milestone must leave a concrete successor ticket active.

| ID | Required capability | Exit evidence |
|---|---|---|
| `PAR-01` | Connected-client player parity inventory and gap map | Real client/server test matrix comparing bot and human for every vanilla capability family in §F.2; actual unimplemented gaps listed explicitly |
| `PAR-02` | Complete baseline survival state observation | Bot reads native hunger, saturation, exhaustion, health, absorption, air, effects, and relevant player state from the connected session; state changes verified in-world |
| `NEED-01` | Extensible need provider/observation registry | Register a test provider for a non-vanilla need; it appears in the agent's need catalog with provenance, range/direction, and update evidence without bespoke AI behavior |
| `NEED-02` | Generic mod-state/HUD discovery | Run with a test Fabric mod that adds a player need or visible HUD state; bot detects and tracks the state; unsupported/unmapped state is surfaced honestly |
| `NEED-03` | Need-driven action loop | A real need change changes salience/candidate selection; the bot performs a legitimate action and verifies the actual resulting need change |
| `CHAT-01` | Human-to-agent ordinary chat | Human types a natural message without `/`; named bot receives it, replies in ordinary public chat, and the human sees both messages |
| `CHAT-02` | Agent-to-agent ordinary chat | Two connected bot clients exchange public player chat; each receives the other's actual chat; a human client sees the exchange in the ordinary chat window |
| `CHAT-03` | Conversational task request | Human requests a task using ordinary language; bot clarifies/accepts/declines; authorized accepted work becomes a scoped task; actual completion is separately verified |
| `CHAT-04` | Persistent, inspectable conversation history | Messages and delivery states survive restart without replaying old messages as new speech; command inspection is supplemental |
| `PAR-03` | Player interaction parity expansion | Complete ordinary item use, attack, equipment/offhand, container GUI, crafting, trading, and other missing vanilla actions via real client inputs |
| `MOD-01` | Full modpack client parity | Bot client resolves/loads the compatible server modpack and joins; player-affecting mods and their HUD/state are available; manifest mismatch is a hard, visible failure |

### F.9.1. Milestone advancement rule

Every milestone has bounded exit criteria. Once those criteria pass, record evidence, classify remaining gaps, close the milestone, and activate the successor. Do not expand a completed milestone indefinitely with every possible edge case. Do not claim overall player parity from a single hunger test or one working chat exchange.

Testing is evidence for delivered capability; it must not become a substitute for implementation of the remaining capability families.

---

## F.10. Acceptance suite: real clients, real chat, real needs

### F.10.1. Full player baseline

Run one human client and one bot client on the same local 1.20.1 Fabric server and same compatible modpack. In a controlled test region, perform the same action sequences with equivalent starting state. Compare server-side outcomes rather than assuming visual similarity is sufficient.

At minimum cover:

- Walk/sprint/jump/sneak and collision.
- Block break and place.
- Hotbar, offhand, armor, equip, drop, and pickup.
- Eat/drink and hunger/saturation/exhaustion.
- Item use in air, on block, and on entity.
- Container open/slot transaction/close.
- Crafting and at least one workstation.
- Attack/defend and damage reception.
- Death/respawn and reconnect.
- Player chat send and receive.

Unimplemented categories remain explicit failures or backlog entries; they must not be marked as parity by implication.

### F.10.2. Extensible needs

Create a small test Fabric mod for 1.20.1 that adds a simple player need (for example, hydration) using a player-attached state mechanism and a visible HUD indicator. This is a test fixture for the generic provider/discovery layer, not a production-specific AI integration.

Acceptance:

1. The bot client loads the test mod as part of the pack.
2. The need provider or generic discovery layer identifies the state.
3. The agent catalog contains the need with its source, units/range, direction, and confidence.
4. The value changes from ordinary in-world actions.
5. The bot detects the change and selects a legitimate relief action.
6. The relief action uses ordinary player interaction.
7. The server's actual modded player state changes as expected.
8. The evidence ties observation, decision, action, and postcondition together.
9. The same test works after reconnect/restart without stale values.

Also test an intentionally undiscoverable/private test state. The expected result is an explicit unresolved-state report, not fabricated knowledge or silent omission.

### F.10.3. Ordinary chat

Use two real bot client sessions and one normal human client on the shared development server.

Acceptance:

1. The human types `Ash, can you help me with this chest?` into ordinary Minecraft chat. No slash command is used.
2. Ash receives the actual player chat message with correct sender identity and conversational context.
3. Ash responds from Ash's own connected client through normal player chat.
4. The human sees the response in the normal chat window, with Ash's actual player identity.
5. A second bot receives the same message stream, but does not automatically reply unless relevant.
6. The two bots exchange at least one natural conversational exchange through normal chat.
7. The human sees both bots' messages in the same chat window.
8. A statement of intent is not recorded as task completion; world-state evidence is required separately.
9. Chat filtering, rate limits, reconnects, and message delivery failures are handled and recorded.
10. No command-only path, system-HUD message, server log, or synthetic watcher substitutes for the real interaction.

---

## F.11. Version-pinned technical references

These references support the 1.20.1 / Fabric implementation direction. Verify exact dependency versions and method availability against the project's pinned Yarn/Fabric API versions before coding; this addendum does not assume the latest Fabric API is the project's installed version.

1. **Yarn 1.20.1 `PlayerEntity` API** — includes `getHungerManager()`:  
   https://maven.fabricmc.net/docs/yarn-1.20.1+build.10/net/minecraft/entity/player/PlayerEntity.html
2. **Yarn 1.20.1 `HungerManager` API** — documents food level, saturation, exhaustion, food tick timer, accessors, `eat`, `update`, and NBT read/write:  
   https://maven.fabricmc.net/docs/yarn-1.20.1+build.10/net/minecraft/entity/player/HungerManager.html
3. **Fabric API 0.91.0+1.20.1 `ServerMessageEvents.ChatMessage`** — receives a broadcast player chat `SignedMessage`, sender `ServerPlayerEntity`, and message parameters; it is not invoked for blocked messages:  
   https://maven.fabricmc.net/docs/fabric-api-0.91.0+1.20.1/net/fabricmc/fabric/api/message/v1/ServerMessageEvents.ChatMessage.html
4. **Fabric API 0.92.5+1.20.1 `ClientPlayNetworking`** — documents client play-stage networking, including clientbound receive and serverbound send functionality. This is a custom play-packet API, not a substitute for ordinary player chat:  
   https://maven.fabricmc.net/docs/fabric-api-0.92.5+1.20.1/net/fabricmc/fabric/api/client/networking/v1/ClientPlayNetworking.html
5. **Fabric `fabric.mod.json` metadata** — documents mod identity, environment (`client`, `server`, `*`), entrypoints, and dependency metadata:  
   https://docs.fabricmc.net/develop/loader/fabric-mod-json
6. **Yarn 1.20.1 `ServerPlayerEntity` API** — documents server player messaging methods; distinguish HUD/system messaging from normal signed player chat:  
   https://maven.fabricmc.net/docs/yarn-1.20.1+build.10/net/minecraft/server/network/ServerPlayerEntity.html

---

## F.12. Explicit non-claims and refusal conditions

This addendum does not claim:

- That the current `FakePlayerBody` has full client/player parity.
- That the current `HumanChannel` proves ordinary bidirectional chat.
- That `ServerPlayerEntity.sendMessage` is equivalent to a bot sending normal player chat.
- That every possible modded need can be semantically identified without an exposed API, a visible UI signal, or further discovery.
- That merely installing the same JARs guarantees compatible client/server modpacks.
- That every modded GUI or mechanic is understood because the bot can load the mod.
- That the bot may bypass normal survival limits, permissions, protections, or Axis governance.

Required typed refusal/unknown states include:

- `PLAYER_CAPABILITY_UNIMPLEMENTED` — the real player function is not yet available through CUA.
- `PLAYER_ACTION_REJECTED` — server/game rules rejected an action.
- `MODPACK_MISMATCH` — client/server versions, dependencies, or environment declarations are incompatible.
- `NEED_PROVIDER_UNAVAILABLE` — no supported state source could be found.
- `UNOBSERVABLE_MOD_STATE` — the mod state is not available through an approved data or UI source.
- `UNMAPPED_MOD_STATE` — a state signal is detected but its meaning is not established.
- `CHAT_RECIPIENT_UNRESOLVED` — the conversational recipient cannot be identified reliably.
- `CHAT_DELIVERY_FAILED` — the composed message was not confirmed as delivered/broadcast.
- `CHAT_INPUT_NOT_PLAYER_ORIGINATED` — an internal/system message was incorrectly offered as proof of player speech.
- `GUI_SEMANTICS_UNKNOWN` — the client can see a screen but cannot safely interpret the relevant control.

No unknown state may be silently converted to success.

---

## F.13. Required architecture outcome

The finished system must be able to place a population of persistent, connected player agents into one shared modded Fabric world. Each agent must have the same underlying player gameplay capabilities and constraints as an ordinary player account. Their cognition must track native and discoverable mod-added needs from actual player state, use those needs to guide System-1 decisions, and verify resulting state changes. Human participants must be able to speak to agents through ordinary Minecraft chat, and agents must speak to each other in that same visible channel.

**The target is not a mob with a player skin, a server-side fake with a partial motor, a silent `MessageBus`, or an admin command interface. The target is a population of actual Minecraft player clients whose bodies, needs, interactions, and conversations are part of the shared world.**

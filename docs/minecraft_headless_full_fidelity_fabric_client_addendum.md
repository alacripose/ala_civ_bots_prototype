# Addendum G — Headless Full-Fidelity Fabric Client Runtime

**Project target:** Minecraft Java Edition **1.20.1 / Fabric**  
**Architecture relationship:** Extends Addendum D (shared server and population control), Addendum E (player-visible chat), and Addendum F (full player parity and extensible needs).  
**Status:** Binding architecture amendment and implementation plan. This document does not claim that the headless client runtime has already been implemented or validated in the project.

---

## G.1. Binding decision

Production bots must run as **headless, full Minecraft Fabric clients**. They must not launch a visible desktop game window for every bot, and they must not be reduced to server-side `FakePlayer` instances or protocol-only bots that omit the Fabric client runtime.

Each production bot connects to the one shared Minecraft 1.20.1 Fabric server using a normal Minecraft client session. The bot's client runs without a visible window, display surface, graphics output, or audio output. It still loads the client-side Minecraft/Fabric runtime and the compatible client portion of the server's modpack, receives the replicated world and player state, runs the client-side mod logic needed for interaction, and sends ordinary player actions and chat through the normal client-server protocol.

**Headless means no displayed client, not no client.** It is a presentation/runtime configuration. It must not remove gameplay capability, player state, mod networking, or the client-side logic required for ordinary player actions.

This supersedes any interpretation of Addendum D or F that would launch a full visible desktop client per bot. It also supersedes the use of `FakePlayerBody` as the production embodiment. `FakePlayerBody` remains a useful legacy test adapter, but production agents are network-connected player sessions.

## G.2. The three implementations that must not be conflated

| Implementation | Role | Production status |
|---|---|---|
| Full visible Minecraft client | Human player's normal desktop client | Human/operator use; not launched for every bot |
| Headless Fabric Minecraft client | Real bot client session with client runtime/modpack but no visible display | **Required production bot embodiment** |
| Server-side `FakePlayerBody` / `FakePlayer` | In-process server-side test adapter without a normal remote client runtime | Legacy/test-only; not the production parity ceiling |
| Raw protocol-only bot | Speaks the Minecraft wire protocol without loading the Fabric client/modpack runtime | Not the production choice; may be used for narrow protocol testing only |

A raw protocol implementation can be lightweight and can perform some vanilla actions, but it cannot be assumed to load Fabric client mods, execute their client entrypoints, participate in their client-side networking, or operate modded client interfaces. It therefore cannot satisfy the chosen modpack-parity requirement by itself.

## G.3. Shared-server topology

There is one authoritative world server. Human players join it through ordinary visible clients. Bot identities join it through separately managed headless Fabric client processes.

```text
                   One Minecraft 1.20.1 Fabric server
                 (one world, one authoritative simulation)
                                |
             +------------------+--------------------+
             |                  |                    |
       Human client       Headless bot client   Headless bot client
       visible UI         Bot A / Fabric        Bot B / Fabric
       human input        no display            no display
             |                  |                    |
             +------------------+--------------------+
                                |
                 Shared world, chat, entities, blocks,
                 inventories, server rules and mod state
```

The bot supervisor manages headless client processes; it does not create worlds or launch a Minecraft server for each bot. Each bot has an isolated client profile/game directory so its configuration, logs, identity, local player data, mod configuration and runtime files cannot overwrite another bot's files. Immutable assets and libraries may be shared through safe caches.

The supervisor is responsible for:

- Starting and stopping headless client processes.
- Assigning each client a persistent `AgentId` and its authorized Minecraft login identity.
- Selecting the correct Minecraft, Fabric Loader, Fabric API and modpack profile.
- Connecting each client to the configured shared server.
- Monitoring login, configuration, play, disconnect and crash states.
- Reconnecting the same identity after temporary failure.
- Enforcing CPU, memory, process-count and launch-rate budgets.
- Reporting each bot's connection and runtime state to the population manager.

The server remains authoritative for movement outcomes, damage, food changes, inventory changes, block interaction, entity interaction, permissions and world mutation.

## G.4. What full player parity means in a headless runtime

Full player parity is defined by **gameplay capability and server-observable behavior**, not by whether a bot has a rendered window.

A headless bot must retain the capabilities available to an ordinary player under the same server rules, including:

1. **Player identity and lifecycle:** normal login, player UUID/profile, permissions, game mode, spawn, death, respawn, disconnect/reconnect and persistent server-side player state.
2. **Movement and body control:** ordinary movement inputs, look direction, jump, sprint, crouch, swimming, climbing, falling, riding, flight where allowed, collision and server-authoritative movement correction.
3. **Survival and needs:** actual food level, saturation, exhaustion, health, absorption, air, fire/freeze state, potion effects, equipment and other server-visible values. Need decisions must use observed player state, not a parallel invented hunger meter.
4. **Inventory and equipment:** ordinary slot selection, inventory operations, armor/offhand, pickup, drop, use, durability, stack rules and server-confirmed outcomes.
5. **World interaction:** mining, placing, doors, levers, buttons, beds, containers, crafting, furnaces, anvils, enchanting, brewing, trading, workstations and other player actions supported by the loaded modpack.
6. **Entity interaction and combat:** attack, defend, target, use items, mount/dismount, tame, feed, breed, trade and other supported interactions.
7. **Communication:** ordinary player chat, private messages where available, server feedback, modded communication features and visible agent-to-agent speech.
8. **Modded player mechanics:** client-side and shared mod logic needed to use the modpack's items, blocks, containers, abilities, menus, keybind-triggered actions, custom packets and player-state features.

A feature is not considered supported merely because the server accepts a command or because an internal agent ledger says it happened. The bot must use the same player-facing interaction path where one exists, receive the server result, and verify the postcondition.

### G.4.1. Headless GUI and screen interaction

The absence of a visible window must not be treated as the absence of a client screen or menu model. The headless runtime must retain the logical client screen/container state needed to interact with ordinary and modded interfaces.

The CUA layer must be capable of inspecting screen structure and invoking the same widget, slot, button, text-entry, and key-input callbacks that a real client uses. It must support opening and closing screens, selecting slots, entering values, activating controls, and observing the server response. A screenshot is optional instrumentation, not a prerequisite for interaction.

Where a mod's feature can only be reached through a custom screen, the headless client must test that screen path. It must not silently replace it with direct server-side inventory or block-entity mutation.

If a mod depends on actual pixel rendering for a cosmetic display, the bot may not need that cosmetic output. If a mod uses rendering callbacks as part of its functional logic or crashes when graphics calls are stubbed, the compatibility issue must be identified and handled explicitly; it cannot be hidden by claiming complete compatibility.

### G.4.2. Sensory and need parity

The bot must not rely only on a server-side list of selected values. Its perception layer should combine:

- Authoritative server corrections and player-state updates received by the client.
- The headless client's replicated world, entities, block entities and inventory/container state.
- The same HUD, menu and screen data models that an ordinary client uses, accessed without requiring a rendered display.
- Mod-provided synchronized state and custom packets.
- Observable changes resulting from the bot's own actions.

The need registry from Addendum F remains in force. Vanilla and modded needs should be registered from the actual values exposed to the client, server synchronization, player attributes, effects, status components, capabilities or screen data.

A private mod value that is neither synchronized nor exposed to the client cannot be inferred reliably. It must be marked `unobservable` or `unmapped`, with a source/visibility reason. The architecture must not invent a thirst, temperature, stamina or other need value just to fill a schema. Where the player can see a value in a screen/HUD but it is not otherwise exposed, the headless UI/state inspection path should be tested before declaring it inaccessible.

## G.5. Headless runtime technology

The implementation should evaluate a **headless launcher/runtime that loads the actual Fabric client**, rather than writing a second Minecraft client protocol implementation as the production bot.

HeadlessMC is a concrete candidate: its documentation describes launching Fabric clients and a headless mode that patches LWJGL to suppress rendering and remove the need for a graphics device. Its documented client-control support includes chat, keys, screens, buttons, text fields and inventory inspection through version-specific support mods. See References R1–R4.

There is an important version-specific caveat: the current HeadlessMC `HMC-Specifics` compatibility table marks Fabric support for 1.20.1–1.20.4 as unsupported, while HeadlessMC's separate `mc-runtime-test` project lists Minecraft 1.20.1 with Fabric as supported for CI client launches. These are different components and claims. Do not assume that HMC-Specifics commands are available on this project's 1.20.1 target merely because the headless launcher itself can launch a Fabric client.

Therefore:

- HeadlessMC may be used as the **launcher/LWJGL headless-runtime candidate**, subject to a 1.20.1 Fabric proof.
- The project's own Fabric 1.20.1 bot-control mod/bridge must provide the production CUA and agent binding unless a separately verified 1.20.1 control implementation is adopted.
- `mc-runtime-test` is useful evidence that a 1.20.1 Fabric client can be launched in a headless test workflow; it is a CI/runtime test harness, not by itself the production bot supervisor or population manager.
- The proof must run the project's actual modpack, not merely a clean Fabric title screen.

Do not make the runtime choice by documentation inspection alone. The selected implementation must pass the acceptance tests in §G.12 on the actual 1.20.1 Fabric project and target modpack.

## G.6. LWJGL, display and graphics boundary

Headless mode should remove the need for a visible desktop window, display server, rendering surface, GPU rendering context and audio output. This reduces the graphics/display burden and allows bot clients to run in background processes.

It does **not** imply zero CPU or RAM cost. The Minecraft client still loads game classes, the world replica, entities, mod code, networking, inventory and interface state. Resource use must be benchmarked; no fixed bot count or promise of hundreds of simultaneous clients may be inferred from the word `headless`.

The runtime must record whether a component is:

- Fully functional without rendering.
- Functional through a headless UI/control abstraction.
- Cosmetic-only and legitimately omitted.
- Incompatible with the selected LWJGL stubs and requiring a narrow compatibility fix.
- Fundamentally dependent on graphics and not yet supported.

Do not blanket-disable all client rendering-related classes or discard client-only mods. Do not add arbitrary dummy values that make graphics-dependent mods appear to initialize successfully when they do not.

Compatibility shims, if needed, must be narrow, documented, version-pinned and tested against the affected mod behavior. A shim may replace a purely cosmetic render result with a no-op; it must not bypass gameplay effects, custom packet registration, screen interaction, player state or server validation.

## G.7. Fabric modpack parity without bespoke AI integrations

The bots are not required to contain a custom AI integration for every mod. They are required to run a compatible client-side installation of the server's modpack and use the same game-facing features a player can use.

Modpack provisioning must build a client profile from the server's actual modpack manifest and configuration, including Minecraft version, Fabric Loader version, Fabric API version, mod identifiers, exact mod versions, dependencies, nested dependencies, relevant configs, resource/data packs and required server/client components.

Fabric metadata distinguishes mods that load in all environments, only on the client, or only on the server. A client-only mod is not automatically dispensable: it may provide a functional client entrypoint, keybind, screen, custom packet or other behavior needed by the player. A server-only mod must not be copied blindly to the client. An all-environment mod must use the version compatible with the server. Dependency and environment declarations must be inspected as part of profile construction. See Fabric's metadata documentation (R5).

The provisioner must produce a deterministic compatibility report with at least these categories:

- `required_client`: required to run the client or use player-facing functionality.
- `shared`: loaded on both sides; exact compatible version required.
- `server_only`: remains on the server, not copied into the client profile.
- `client_cosmetic_optional`: can be omitted only when proven not to affect gameplay, interaction, sensing or required networking.
- `headless_incompatible`: fails or loses required functionality under the headless runtime.
- `unknown`: insufficient evidence; do not silently omit.

### G.7.1. No per-mod AI integration requirement

Do not create a bespoke decision-making plugin for every mod. The general client runtime and CUA should operate through ordinary Minecraft client abstractions: player inputs, screens, widgets, slots, inventory, interaction results, world replicas, synchronized data, chat, keybinds and registered networking.

Mod-specific support is only warranted when a mod exposes a new mechanic through a client/server API that cannot be driven or observed through those general abstractions. That should be a bounded compatibility adapter, not a prerequisite for every installed mod.

### G.7.2. No false guarantee of arbitrary mod compatibility

Loading the same compatible modpack is necessary but does not prove every mod is headless-compatible. Some mods may call graphics APIs during startup, assume a visible window, require GPU resources, or encode essential state in client-only structures that are not synchronized. Those cases require a compatibility test and, where appropriate, a narrow patch or adapter.

A modded feature must be marked unsupported until the bot has demonstrated that it can perceive, invoke and verify the feature's real player interaction path. The system must not claim universal compatibility merely because every JAR loaded.

## G.8. CUA: one action path, no server-side shortcuts

The agent's System-1 brain continues to produce intentions and skill choices. A client-side CUA bridge translates approved skills into ordinary game inputs and interface operations.

The action path is:

1. System-1 proposes an intention and a skill.
2. The canonical Axis gateway validates the proposed action and scope.
3. The bot CUA resolves the target from its own permitted perception and client state.
4. The headless client sends the normal player input, interaction, inventory, container or chat operation.
5. The shared server processes the action under normal game rules.
6. The client receives server correction/result/state updates.
7. The agent verifies the postcondition from observed world/player state.
8. The mind records success, refusal or typed failure with provenance.

Server-side test/admin interfaces may arrange fixtures or inspect evidence, but they must not substitute `/setblock`, direct inventory assignment, direct block-entity mutation or direct entity-state writes for the bot's production actions.

The canonical Axis and ownership/protection boundary remain authoritative. The client bridge cannot grant itself permissions by sending a packet, choosing a UI element or replaying a request. The server must validate player authority and normal Minecraft interaction restrictions.

## G.9. Chat in a headless client

Headless bots must participate in real Minecraft chat even though no chat window is displayed locally.

- Outgoing agent dialogue is sent through the headless client's normal chat-send path as the bot's player identity.
- Incoming player and bot messages are received from the server's ordinary chat delivery path and become inputs to that agent's perception/communication system.
- The server broadcasts bot messages to the human operator's normal visible chat.
- Agent-to-agent chat must be visible to the human in ordinary chat; a private `MessageBus` record or log line is not a substitute.
- Human-to-agent messages must be ordinary chat messages, not require slash commands. Addressing/mention rules may route them to an agent, but preserve the original message, sender and context.
- The internal message bus may carry structured intent, delivery state, expiry and provenance. It must not create a separate invisible reality that is presented as spoken player chat without an actual player chat event.

The headless bot does not need to render chat pixels to send, receive, interpret or respond to chat. The control bridge may expose the message event and delivery state directly from the client runtime.

## G.10. Per-agent process and resource management

The Minecraft client has process-global/singleton-style state in important parts of its runtime. Do not attempt to place multiple independent live client sessions into one JVM by merely constructing multiple `MinecraftClient` objects. That is a separate high-risk engineering project and is not the default design.

The initial safe model is one isolated headless client process per concurrently connected bot. The supervisor may share immutable libraries, downloaded assets and cached mod JARs, but each bot must have isolated mutable runtime state and an independent server connection.

To support the population target without exhausting the host:

- Launch clients in a staggered queue, not all at once.
- Apply configurable per-client and global memory budgets.
- Track heap, non-heap, native memory, CPU, startup time, tick responsiveness, packet latency and crash rate.
- Avoid loading unnecessary resource packs, shaders, visual effects, audio devices and high-resolution textures where removal is proven not to affect gameplay.
- Keep the headless client responsive enough to process chunks, server updates, menus, inventory, chat and player input.
- Benchmark 1, 2, 4, then larger staged counts, stopping when the measured budget is exceeded.
- Never set the population cap from an unverified theoretical estimate.
- If capacity is reached, do not create extra connected clients. Preserve agent identities and use the separately specified virtual/regional simulation rules only for agents explicitly represented as not currently materialized as connected player bodies.

The population manager must distinguish:

- `resident_identity_count`: persistent agent identities that belong to the world/civilization.
- `alive_agent_count`: identities not permanently dead under the project's death contract.
- `connected_client_count`: bot client sessions currently connected to the server.
- `headless_process_count`: client processes running on the host.
- `materialized_player_count`: agents with active server-side player bodies.
- `virtual_agent_count`: persistent agents currently represented only in the regional simulation.

A disconnect, timeout, process crash, client update, server restart or temporary inability to afford a connected client is **not permanent death** and must not grant a replacement identity. The population and replacement rules in Addendum D remain binding.

## G.11. Authentication and profile isolation

Each connected bot must use a legitimate Minecraft account/session identity accepted by the target server's authentication mode. Do not bypass authentication or pretend that multiple simultaneous players can use one account if the service/server does not permit it.

Credentials and refresh tokens must be kept outside source control and ordinary agent memory/logs. The supervisor stores references to protected credentials, not plaintext credentials in AgentMind state or test output.

Each bot profile has:

- Unique persistent `AgentId` binding.
- The authorized Minecraft account/profile identity.
- Minecraft 1.20.1 version metadata.
- Exact Fabric Loader and Fabric API versions.
- A deterministic modpack manifest/hash.
- Per-bot `config`, `options`, `logs`, `crash-reports`, runtime data and game directory.
- Headless runtime configuration.
- Last connection/disconnection reason and last known server acknowledgement.

A new client process reconnecting for the same agent must recover the same identity. A process restart must never create a new citizen.

## G.12. Implementation milestones and exit contracts

These are capability milestones, not an invitation to perfect the launcher indefinitely. Every milestone has a bounded exit contract and must activate its successor when that contract passes.

### `HCL-01` — Prove a headless Fabric 1.20.1 client

**Goal:** Launch one real Minecraft 1.20.1 Fabric client without a visible window or rendering device, using the actual project runtime.

Exit criteria:
- Headless client starts without creating a visible desktop game window.
- Fabric Loader and required client entrypoints load.
- Client reaches its normal title/configuration state and connects to the existing shared Fabric server.
- Server records an ordinary player login, not a `FakePlayerBody` login.
- Client receives chunks and server updates.
- Disconnect and reconnect use the same identity.
- Logs prove no required rendering surface/GPU is used by the bot process.

Do not stop at a title-screen-only proof; the shared server login is mandatory.

### `HCL-02` — Project-owned headless CUA bridge

**Goal:** Control the headless client through a project-owned Fabric 1.20.1 bridge without a visible GUI.

Exit criteria:
- Move, look, jump, sprint/crouch, mine, place, use, equip, drop/pickup, open/operate a chest, and send chat through ordinary player client paths.
- Open/close a normal screen and read its logical state.
- Server-observed player results match the issued action.
- No direct server mutation is used as a substitute.

### `HCL-03` — Headless full-player survival parity

**Goal:** Prove that a headless bot obeys actual player mechanics.

Exit criteria:
- Hunger, saturation, exhaustion, health, damage, effects, air and relevant player state are observed from the real client/server state.
- Bot's need model responds to those states.
- Eating/drinking/use actions change real player state through normal interaction paths.
- A controlled scenario demonstrates the same server-side outcome from a visible human client and a headless bot client, allowing for different input timing.

### `HCL-04` — Modpack profile parity

**Goal:** Reproduce the project's actual client-required Fabric modpack in a headless bot profile.

Exit criteria:
- Generate a manifest-based client mod set and compatibility report.
- Load the same required/common/client-functional mods and compatible versions as the human client profile.
- Identify and classify headless-incompatible mods.
- Test at least one modded item/block/container/need mechanic from the actual target modpack.
- Do not implement bespoke agent logic for the test mod unless its feature genuinely lacks a generic CUA path.

### `HCL-05` — Native chat and conversation

**Goal:** Prove the headless clients can talk to humans and each other using ordinary player chat.

Exit criteria:
- Human sends a normal chat message addressed to a bot; bot receives and responds.
- Two bots exchange ordinary chat; the human sees both sides in the visible Minecraft chat.
- Chat is connected to the same AgentMind perception and response decisions as other social interactions.
- Message provenance and delivery are recorded.

### `HCL-06` — Supervised multi-client process management

**Goal:** Run multiple headless clients concurrently on one shared server with isolated profiles.

Exit criteria:
- Two clients connect concurrently and act independently.
- Disconnecting one client does not corrupt another client's profile or identity.
- Crash/restart reconnects the same identity.
- Staggered launch, memory accounting, CPU accounting and process telemetry work.
- Capacity limits are measured, configurable and enforced.

### `HCL-07` — Replace production embodiment

**Goal:** Make headless Fabric clients the production body for fake-player citizens.

Exit criteria:
- Production agent registry binds AgentMind to a networked player session.
- `FakePlayerBody` is retained only for explicitly named legacy/unit/probe uses.
- No production population path creates a server-side fake player instead of a client connection.
- Existing player, verbs, handoff and chest acceptance behaviors run through headless clients on the shared development server.
- Human can remain connected and observe/interact throughout test execution.

### `HCL-08` — Population threshold and lifecycle integration

**Goal:** Integrate headless client capacity with Addendum D's population and permanent-death policy.

Exit criteria:
- Initial world population fills to configured target only within measured process/resource budgets.
- Temporary disconnects preserve identities and reconnect them.
- Population slots are replaced only after the permanent-death contract is satisfied.
- Virtual/regional agents are never misreported as live connected player bodies.
- The world is not reset or replaced to restart bots or run ordinary development tests.

## G.13. Acceptance matrix

| Test | Required evidence |
|---|---|
| No visible client window | Process/window inspection and runtime logs show no displayed bot game window |
| No GPU rendering dependency | Headless process launches/plays without a graphics device or rendering context |
| Real player session | Server-side entity is an ordinary logged-in player entity associated with a network handler; not a `FakePlayerBody` |
| Ordinary movement | Server-observed position/collision outcome from client inputs; no teleport shortcut |
| Ordinary mining and placing | Server events, drops, inventory deltas and block state prove player interaction path |
| Inventory and chest | Real screen/container state and server-side slot outcomes; no direct mutation |
| Survival parity | Hunger/exhaustion/damage/effects change according to server mechanics and are observed by the bot |
| Modded parity | At least one actual target-mod feature works through the headless client path |
| Human chat | Human types ordinary chat; bot perceives and responds; no slash command required |
| Bot-to-bot chat | Both messages are normal player chat visible to the human |
| Multi-client isolation | Independent profiles and sessions; no identity/data cross-contamination |
| Population lifecycle | Crash/disconnect does not create a new identity or replacement citizen |
| Resource envelope | Measurements for process RAM/CPU, startup time, steady-state cost and maximum tested concurrent count |

## G.14. Non-goals and prohibited shortcuts

This addendum does not require:

- A visible desktop window for every bot.
- Audio output or rendered pixels as a prerequisite for agent perception.
- A separate Minecraft server/world per bot.
- Bespoke AI integrations for every installed mod.
- A hand-written replacement Minecraft protocol client as the production client runtime.
- Server-side fake players as the production embodiment.

The following shortcuts are prohibited:

- Calling a raw protocol bot “full Fabric client parity” without loading the Fabric client and required mods.
- Calling a `FakePlayerBody` “a connected bot client.”
- Deleting client-only mods merely because they are client-only.
- Omitting a modded GUI interaction without testing whether the headless client can manipulate its logical screen state.
- Using server commands or direct state writes for actions that should be performed by the player client.
- Counting disconnected/virtual agents as currently connected player sessions.
- Treating a crash or disconnect as permanent death.
- Claiming a population capacity without measurements on the target machine and modpack.
- Treating successful mod loading as proof that all modded player interactions work.

## G.15. Required reporting format

Every runtime implementation report must state:

1. Minecraft/Fabric/Loader versions and the exact headless launcher/runtime version.
2. Whether the client has a visible window, graphics context, GPU dependency or audio device.
3. Which client and common mods loaded, and which were excluded with reasons.
4. Whether the server saw an ordinary player login.
5. Which real player actions were tested and their server-observed outcomes.
6. Which modded screens/mechanics were tested and which remain unknown.
7. Human chat and agent-to-agent chat results.
8. RAM, CPU, startup and steady-state measurements per client and in aggregate.
9. Concurrent client count actually tested.
10. Whether temporary disconnect/restart preserved the same identity.
11. The next capability milestone.

Do not report “headless parity complete” because the client launched. Report only the tested capabilities and their evidence.

---

## G.16. Technical references

The following sources were checked for this amendment. They establish the documented headless-client approach and Fabric environment model; they do not prove that this project's exact modpack or control bridge has already been validated.

**R1. HeadlessMC — Launching.** Documents Fabric client launching and the `--headless` mode, implemented by patching LWJGL to avoid display/rendering requirements.  
https://headlesshq.github.io/headlessmc/launch/

**R2. HeadlessMC — Specifics.** Documents command/control concepts for chat, GUI inspection, text, keys, inventory and server connection through version-specific support mods. Its compatibility table currently marks Fabric support for 1.20.1–1.20.4 as unsupported for HMC-Specifics; do not assume that component works on the project's target.  
https://headlesshq.github.io/headlessmc/specifics/

**R3. HeadlessMC-LWJGL.** Explains the LWJGL transformation/redirection mechanism used to make the Minecraft client headless.  
https://github.com/headlesshq/headlessmc/tree/main/headlessmc-lwjgl

**R4. HeadlessMC `mc-runtime-test`.** Its documented compatibility matrix lists Minecraft 1.20.1–1.20.6 with Fabric for client runtime testing. This supports a 1.20.1 headless client proof, not the existence of this project's production bot controller.  
https://github.com/headlesshq/mc-runtime-test

**R5. Fabric Loader — `fabric.mod.json`.** Documents the `environment` field (`*`, `client`, `server`) and the distinction between common, client-only and server-only entrypoints.  
https://docs.fabricmc.net/develop/loader/fabric-mod-json

**R6. Fabric Loader documentation.** Describes Fabric mod loading, metadata, dependencies and environment-specific loading.  
https://docs.fabricmc.net/develop/loader/

---

## G.17. Binding summary

The production bot is a **real Fabric client running headlessly**. It is not a visible desktop client, not a server-side fake player, and not merely a raw protocol script. It loads the required client runtime and modpack, joins the one shared world through a normal player session, uses ordinary player actions and chat, observes actual player state and modded interactions, and runs under a supervisor that measures resource use and preserves identity.

The phrase **full player parity** means the bot retains the same server-governed gameplay capabilities and the client-side logic necessary to operate the installed modpack. Headless presentation removes display output; it does not authorize removing game mechanics. Compatibility gaps must be named and tested rather than hidden.

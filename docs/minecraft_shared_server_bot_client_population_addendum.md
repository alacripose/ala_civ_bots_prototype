# Addendum D — Shared Server Hosting, Networked Bot Clients, Population Caps, and Fabric Modpack Parity

**Target runtime:** Minecraft Java Edition 1.20.1 / Fabric  
**Status:** Binding architecture amendment  
**Related architecture:** `minecraft_ensouled_system1_cua_architecture_v3.md`, Addendum C (`minecraft_fake_player_first_addendum.md`), and the current entity-AI transition plan  
**Supersession:** This addendum supersedes any earlier assumption that each bot owns, launches, or requires a separate Minecraft server/world, and supersedes the use of an in-process `FakePlayerBody` as the primary production embodiment. It does not remove the existing fake-player implementation as a test adapter or regression fixture.

---

## D.1. Binding decision: one shared world server, bots connect as clients

The civilization must live inside **one authoritative Minecraft server instance**. The server owns the world. Bot agents connect to that server as individual Minecraft client sessions, using the normal Minecraft client/server connection and login path.

The system must not start a separate Minecraft server or separate world for each bot. A bot is not a server. A bot is an agent identity with a player body represented by one client session when online.

The intended deployment is:

```text
LOCAL MACHINE / HOST
│
├── Ensouled Supervisor / Population Controller
│   ├── persistent Agent Registry
│   ├── Population Ledger + Replacement Credits
│   ├── AgentMind instances / cognition scheduler
│   ├── Bot Client Process Manager
│   ├── Modpack Profile Builder + Parity Verifier
│   └── Local CUA / IPC Controller
│       ├── Bot Client A ── Fabric Client ──┐
│       ├── Bot Client B ── Fabric Client ──┤
│       └── Bot Client N ── Fabric Client ──┤
│                                           │
└── ONE Minecraft 1.20.1 Fabric Server ◄────┘
    ├── one authoritative world save
    ├── one server-side mod environment
    ├── one player/entity/chunk lifecycle
    ├── server-side Agent Registry integration
    └── ordinary human player connections

Human Player Client(s) ─────────────────────► same server
```

The exact process placement can evolve, but the ownership rule cannot:

- One world has one authoritative Minecraft server process at a time.
- Each concurrently embodied bot has one unique player connection/session.
- Every bot session joins the same configured server address and world.
- Human players join that same world through ordinary Minecraft clients.
- The bot supervisor manages clients and agent identities; it does not create independent bot-owned worlds.
- The Minecraft server remains authoritative for blocks, entities, inventories, damage, time, dimensions, chunks, player state, and world persistence.

The server may be manually launched, managed by a separate service, or started once by a host-level supervisor. This choice does not change the architecture. The bot manager must attach to the configured server; it must never launch one server per bot.

## D.2. Terminology: agent, client runtime, session, server body, and resident

These terms must not be conflated.

### D.2.1. Agent identity

An **Agent** is the durable individual: stable `AgentId`, identity record, cognition, memory, relationships, skills, history, authority scope, and lifecycle state. It persists independently of any Minecraft process or network session.

An agent may be online, offline, connecting, recovering, dormant, or permanently dead. A network connection is not the agent's identity.

### D.2.2. Bot client runtime

A **Bot Client Runtime** is a real Minecraft client process launched with the configured Minecraft 1.20.1 / Fabric runtime and the client-compatible modpack. It connects to the shared server as a player client.

The client runtime is the bot's temporary embodied input/output endpoint. It is not a server process and it does not own the world save.

For the first production implementation, use one isolated client instance/process per concurrently connected bot. Do not try to multiplex multiple independent player identities through one ordinary client process unless a later, separately verified implementation proves that the Minecraft client lifecycle safely supports it. Each client must have isolated per-agent game state and session state.

### D.2.3. Player session and server-side player entity

A **Player Session** is the live authenticated connection between one bot client runtime and the shared server.

When the bot logs in, the server creates and manages the corresponding ordinary server-side player entity through the normal player connection/login lifecycle. That entity is the bot's current in-world body.

The player entity is not the durable agent. Disconnecting removes the live session/body from the active world lifecycle, while the durable agent record remains.

### D.2.4. Resident population

The **Resident Population** is the number of non-terminal agent identities assigned to the world. It includes residents who are temporarily offline, restarting, or quarantined for recovery. It does not mean the number of currently connected client processes.

This distinction is mandatory for correct population caps. A disconnected bot must not create a vacancy. A crashed client must not create a vacancy. A server restart must not create a vacancy.

## D.3. Superseding the previous FakePlayer embodiment

The previous `FakePlayerBody` implementation is a server-side synthetic player object. Fabric API documents `FakePlayer` as a subclass of `ServerPlayerEntity` intended for automatically performed player actions such as block placement; it is not a separate real Minecraft client process connecting over the network. See the Fabric API 0.92.5+1.20.1 Javadoc: <https://maven.fabricmc.net/docs/fabric-api-0.92.5%2B1.20.1/net/fabricmc/fabric/api/entity/FakePlayer.html>.

That distinction matters because this addendum changes the production embodiment boundary:

- **Before:** the mod created and controlled a synthetic server-side player with a fake connection.
- **Now:** the bot launches a Fabric client and joins the shared server through the ordinary client/server protocol.
- **Before:** the mod could call or simulate server-side player behavior inside the server process.
- **Now:** the bot's motor must cause input through its connected client, and the server must process the resulting normal player actions.
- **Before:** the synthetic body could exist without any client-side rendering, client mods, client state, or ordinary client handshake.
- **Now:** the client runtime loads the client-compatible Fabric modpack and participates in the server's normal connection and synchronization lifecycle.

`FakePlayerBody` may remain for unit tests, headless server tests, controlled comparisons, or an explicitly named compatibility adapter. It must not be counted as an externally connected client bot, and it must not be the production path used to claim that a bot can join a modded server as a client.

The system must not maintain two competing embodiments for one agent at once. An `AgentId` can have one active production player session/body at a time. A test fake player and a production networked session must never both be active as duplicate bodies for the same identity in the same world.

## D.4. Server authority and client responsibility

The client/server split is a deliberate security and correctness boundary.

### D.4.1. The shared server owns world truth

The shared server owns and validates:

- world and dimension state;
- loaded chunks and block states;
- entity existence, position, collision, health, damage, effects, and death;
- inventory and container contents;
- block breaking and placement results;
- interaction reach, permissions, and protection rules;
- item entities, pickup, drops, and transfers;
- modded registries and server-authoritative mechanics;
- canonical world events and their persistence.

A client observation is a perception source, not final proof that an action succeeded. A motor acknowledgement is not proof of a world change. The server-observed postcondition remains the source of truth for canonical events.

### D.4.2. The bot client supplies ordinary player input

The bot's physical actions must be produced through its connected client session. Examples include:

- movement and directional input;
- look direction;
- jump, sneak, sprint, and selected hotbar slot;
- attack and use input;
- block interaction and mining input;
- item dropping and ordinary inventory interactions;
- container interaction through the client/server path;
- other actions available to an ordinary player under the server's rules.

The server decides whether the requested action is legal and what happens. The CUA layer observes the resulting state and records a typed success, refusal, or failure.

The agent must not gain a privileged world-mutation path merely because it is controlled by the supervisor. No direct block mutation, inventory assignment, reach bypass, creative-mode shortcut, or server-only action may be substituted for an ordinary player action while claiming player-equivalent behavior.

### D.4.3. Keep cognition independent from the client process

The AgentMind must not live only in volatile client memory. A client crash must not destroy identity, memory, relationships, campaign state, pending obligations, or canonical history.

The existing server-side persistent agent records can continue to be authoritative, or a separately supervised persistent agent service can own them. Either placement is acceptable only if:

1. each record is keyed by stable `AgentId`;
2. the player session is bound to that identity through a verified mapping;
3. the client process cannot silently replace or rewrite the durable identity;
4. reconnecting restores the same agent rather than creating a new one;
5. world mutations still pass through the shared server's ordinary player rules and the canonical authority gate.

Where the mind runs outside the server process, it must use a narrow, authenticated control channel. The channel carries scoped observations, action proposals, and results; it is not a back door to the world state.

## D.5. Player control and observation without per-mod AI integrations

The user requirement is **modpack compatibility, not bespoke AI support for every installed mod**.

The bot must be able to run the ordinary Fabric client runtime with the same compatible modpack as the server. The bot's cognition and motor must not require a special hand-written integration for every mod installed on the world.

Use one generic client-control boundary:

- a local CUA controller that can provide ordinary keyboard/mouse/controller inputs and capture the client output; and/or
- one small, version-pinned, universal agent client bridge that exposes generic input, perception transport, session identity, and health information.

The bridge, if used, is core infrastructure for controlling a Minecraft client. It is not a per-mod adapter framework. It must not contain special cases for each content mod. It must not replace the normal Minecraft client protocol or bypass server checks.

The generic control boundary may expose general game concepts that Minecraft already presents to players: view, crosshair target, player movement state, selected slot, visible inventory, open screen, interaction status, and client connection state. The server remains responsible for validating the world result.

Where a mod adds a new block, item, menu, entity, dimension, or mechanic, the bot client must load the mod so that the client can understand and render the content and participate in the mod's normal networking. The agent may initially treat an unfamiliar object through general perception and ordinary controls. It is not required to know the object's full semantics or automatically operate every mod-specific machine.

This creates a deliberate separation:

- **Mod loading compatibility:** run the appropriate Fabric mods in the client runtime so the client can join and represent the same modded world.
- **Agent capability compatibility:** decide which objects and mechanics the general agent can understand and use through its existing generic perception and motor vocabulary.
- **Optional future skill knowledge:** teach an agent about a particular mod mechanic only when that capability is actually needed. Such knowledge is a skill/content extension, not a prerequisite for loading the modpack.

Do not claim universal mod mastery merely because the client successfully loads the modpack.

## D.6. Fabric modpack parity: same compatible pack, not blindly identical JAR folders

The bot client must use a reproducible client profile derived from the server's declared modpack. The goal is version and content parity wherever the mod is intended to run on both sides, while respecting Fabric's client/server environment declarations.

Fabric's `fabric.mod.json` metadata defines mod identity, version, dependencies, and environment. The environment can be `*` (all environments), `client` (physical clients only), or `server` (physical servers only). Fabric also distinguishes `main`, `client`, and `server` entrypoints. See:

- Fabric Loader `fabric.mod.json` reference: <https://docs.fabricmc.net/develop/loader/fabric-mod-json>
- Fabric Loader documentation: <https://docs.fabricmc.net/develop/loader/>
- Fabric's entrypoint documentation: <https://wiki.fabricmc.net/documentation%3Aentrypoint>

The server and bot-client installations therefore must not be created by blindly copying every JAR into both folders.

### D.6.1. Required runtime alignment

Every bot client profile must align with the server on:

- Minecraft version: **1.20.1** for this project target;
- compatible Fabric Loader version;
- exact versions of mods that are required on both sides;
- Fabric API version where required;
- dependencies and nested dependencies;
- required registry/content definitions;
- any server-required client companion mods;
- relevant resource-pack and configuration expectations.

Pin versions and hashes in a machine-readable modpack lock file. Do not silently update a bot client independently from the server.

### D.6.2. Build client profiles using mod metadata

The profile builder must inspect each installed mod's metadata and classify it:

| Metadata / role | Server installation | Bot client installation |
|---|---:|---:|
| `environment: "*"` | Yes | Yes |
| `environment: "client"` | No | Yes |
| `environment: "server"` | Yes | No |
| explicit required dependency | according to dependency's environment | according to dependency's environment |
| optional client-only enhancement | No | Optional, pinned if included |

The profile builder must preserve version constraints and dependency resolution. If metadata is missing, malformed, contradictory, or known to be inaccurate, the build must mark the mod for an explicit compatibility check instead of guessing.

Some mods ship common code but split initialization between `main`, `client`, and `server` entrypoints. The client profile must load the client runtime's appropriate entrypoints; the server must load its server entrypoints. A mod being present on both sides does not mean that every class or entrypoint runs on both sides.

### D.6.3. Client-required and server-only content

Some servers require a client-side companion mod for rendering, custom screens, custom packet handling, registries, or other client behavior. Such a mod must be included in the bot client's profile at the version compatible with the server pack.

A server-only mod must not be forced into a client installation if its metadata declares server-only. Conversely, client-only mods must not be installed into the dedicated server merely to make the directory trees look identical.

The requirement is **same compatible Fabric modpack**, not byte-for-byte identical server and client mod directories.

### D.6.4. No mod-specific integration promise

The project does not promise that an agent can understand or operate every machine or feature added by every mod. It promises that the bot client can run the compatible Fabric client pack and join the modded server where that pack is supported.

The general bot runtime should rely on normal client behavior, generic perception, and standard input. Specialized automation for a mod's custom GUI, machine, or API is a separate feature request and must not be added preemptively.

### D.6.5. Modpack mismatch is a hard typed failure

A bot must not enter a partially compatible or incorrectly configured server session. Examples of refusal/failure states:

- `MINECRAFT_VERSION_MISMATCH`;
- `FABRIC_LOADER_MISMATCH`;
- `MOD_REQUIRED_ON_CLIENT_MISSING`;
- `MOD_VERSION_MISMATCH`;
- `MOD_DEPENDENCY_UNSATISFIED`;
- `MOD_CONFLICT`;
- `REGISTRY_SYNC_FAILED`;
- `SERVER_RESOURCE_PACK_REJECTED`;
- `CLIENT_BOOT_FAILED`;
- `LOGIN_REJECTED`.

The verifier records the actual server pack fingerprint, client pack fingerprint, and mismatch details. It does not repeatedly retry the same incompatible profile.

Do not automatically download arbitrary server-provided JARs or execute unsigned code merely because a server requests a mod. Modpack installation is an explicit trust decision controlled by the world operator.

## D.7. Local server connection and lifecycle

The configured server endpoint is a world-level setting, not an agent-level setting. All bots belonging to the world use the same configured endpoint.

Example configuration (illustrative; the population value must be chosen by the operator):

```yaml
world:
  minecraft_version: "1.20.1"
  mod_loader: "fabric"
  server_address: "127.0.0.1:25565"
  server_process_owner: "external"
  world_id: "primary-world"

bot_clients:
  runtime_profile: "./profiles/primary-world/client"
  modpack_lock: "./profiles/primary-world/modpack.lock"
  isolated_game_directory_per_agent: true
  one_client_process_per_connected_agent: true
  reconnect_same_agent_identity: true

population:
  resident_target: 8  # Example only; operator-selected.
  bootstrap_fill_once: true
  replacement_only_after_permanent_death: true
  death_policy: "permadeath"  # Must be an explicit world rule.
  max_concurrent_client_sessions: 8
```

The configuration distinguishes the resident target from the concurrent session limit. These values may be equal for a small world where every resident should be connected at once. They are not interchangeable.

### D.7.1. One host, one world save

The shared server's world directory is the authoritative save. Agent identities and world-linked records must be persisted to the same world identity or to a separately backed-up registry explicitly associated with it.

Bot clients must not maintain independent world saves. Their per-agent game directories contain only client runtime state such as options, logs, caches, selected profile, and session-specific data. A client-local cache is never authoritative world state.

### D.7.2. Server availability

The bot manager waits for the configured server to become reachable and complete its readiness checks before launching/reconnecting clients. If the server is unavailable, bots enter a reconnect/recovery state. The system must not respond by starting N alternative servers.

`server_process_owner: external` means the operator owns the server lifecycle. An optional future mode may let one world supervisor start or stop the single shared server, but it still starts exactly one server process for the world.

### D.7.3. Authentication and identity

Each connected bot must have a unique, stable player identity and a unique session. Never run multiple concurrent bot sessions with the same account credentials or UUID.

For an authenticated online-mode server, each bot connection requires a valid distinct account/session authorized to join. Authentication tokens and refresh credentials must be stored in an approved secret store, not in campaign files, logs, plain-text configuration, or memory dumps.

Offline-mode identity may be used for an isolated local development server only when deliberately configured by the operator. The server should be bound to loopback or protected by network/firewall controls. Do not present offline-mode as a way to bypass account authentication on a public server.

The agent-to-player mapping must be explicit and durable:

```text
AgentId
  -> stable bot profile
  -> expected Minecraft GameProfile / UUID
  -> current client process
  -> current network session
  -> current server-side player entity
```

A reconnect must resolve back to the same `AgentId`. A different UUID, reused profile, or ambiguous login is a hard refusal until reconciled.

## D.8. Population target and one-for-one replacement rule

The operator chooses a **Resident Population Target** `N`. This is the maximum number of living agent identities assigned to the world by this population controller.

The system initially populates the world up to `N`. Once the initial population is established, the system must not create additional agents merely because fewer than `N` clients are currently connected.

After bootstrap, **a new agent identity may be created only when a prior resident has been recorded as permanently dead and that death has created one unused replacement credit**.

This is a birth/replacement rule, not a connection-management heuristic.

### D.8.1. Initial bootstrap

On first initialization of an empty world registry:

1. Read the operator's `resident_target`.
2. Create an initialization record tied to the world ID and registry generation.
3. Create resident identities until `living_resident_count == resident_target`.
4. Persist each identity and its player-profile mapping before attempting to launch its client.
5. Mark bootstrap complete only after the registry transaction is durable.

If the supervisor or server crashes during bootstrap, the next start resumes the same transaction. It must not create duplicate identities. The bootstrap-complete marker and registry must be committed atomically or through a recoverable journal.

Once bootstrap has completed, the controller must not perform another unrestricted `fill_to_target` operation on every restart. The initial fill is a one-time world initialization action.

### D.8.2. Resident count versus connection count

The population controller counts living identities, not live sessions.

The following states continue to count toward the resident target:

- `CREATED`;
- `CONNECTING`;
- `ONLINE`;
- `DISCONNECTED`;
- `RECONNECTING`;
- `RECOVERING`;
- `DORMANT`;
- `QUARANTINED` pending identity/data recovery.

Only a durably recorded terminal state such as `PERMANENTLY_DEAD` stops counting as a living resident.

Therefore:

- a bot client crash does not create a replacement;
- a network disconnect does not create a replacement;
- a server restart does not create a replacement;
- a client update failure does not create a replacement;
- a player entity being temporarily unloaded does not create a replacement;
- a bot being kicked or banned does not automatically create a replacement;
- a missing/corrupt mind file does not automatically create a replacement;
- a manual process shutdown does not create a replacement.

These conditions require reconnect, repair, quarantine, or explicit operator action against the same identity.

### D.8.3. Permanent death is a lifecycle event, not a missing connection

A permanent death is an explicitly governed terminal transition for an agent identity. It must be recorded by the server/world lifecycle and committed to durable population history.

Minecraft's ordinary player death and respawn behavior does not automatically mean that the agent identity has permanently died. The world must choose a clear death policy:

- `RESPAWN_SAME_IDENTITY`: the player body may die and respawn, but the same `AgentId`, mind, and resident slot continue;
- `PERMADEATH`: a qualifying in-world death transitions the resident to `PERMANENTLY_DEAD`, subject to the world's defined finalization rules, and creates one replacement credit;
- `DEATH_POLICY_UNCONFIGURED`: automatic replacement is disabled until the operator explicitly chooses a policy.

The policy is world-level and versioned. It must not be inferred from connection loss or from default Minecraft behavior.

If `PERMADEATH` is selected, the implementation must define when death becomes final (for example, after the server has recorded the death and applied the configured death/respawn restrictions). If a death can be reversed by an approved recovery mechanic, it must not issue a replacement credit before the finality boundary.

The tombstone must preserve the old `AgentId`, player UUID, date/tick, cause, world/dimension/location if available, policy version, and evidence. A new resident receives a new `AgentId` and a new player identity. The replacement is not the old agent secretly renamed or reset.

### D.8.4. Replacement credits prevent duplicate births

Use a durable `ReplacementCredit` record for each qualifying permanent death.

A credit contains at least:

- unique credit ID;
- world ID;
- permanently dead `AgentId`;
- terminal death event ID;
- death-policy version;
- creation timestamp/tick;
- state: `AVAILABLE`, `RESERVED`, `CONSUMED`, or `VOIDED`;
- replacement `AgentId` once consumed.

The population controller consumes one credit to create exactly one new resident identity. The credit and new identity must be committed atomically or through an idempotent journal.

On restart, a consumed credit must never create another replacement. A death event must never mint multiple credits. A temporary disconnect must never mint a credit.

The invariant is:

```text
living_resident_count <= resident_target

bootstrap_complete == true
  AND no unused permanent-death replacement credit exists
  => no new AgentId may be created

one finalized permanent death
  => at most one replacement credit
  => at most one new AgentId
```

If the operator deliberately increases `resident_target`, that is an explicit population policy change and must be audited. It is not an automatic replacement event. Reducing the target does not silently kill or delete existing agents; it changes future admission policy and requires a separately defined retirement process.

### D.8.5. Population ledger state model

Recommended resident lifecycle:

```text
UNINITIALIZED
  -> CREATED
  -> CONNECTING
  -> ONLINE
  -> DISCONNECTED / RECONNECTING
  -> ONLINE

ONLINE
  -> BODY_DEAD (if the configured life policy permits recovery)
  -> ONLINE (same identity)                    [RESPAWN_SAME_IDENTITY]

ONLINE
  -> DEATH_PENDING_FINALIZATION
  -> PERMANENTLY_DEAD + TOMBSTONE
  -> REPLACEMENT_CREDIT_AVAILABLE
  -> REPLACEMENT_CREDIT_CONSUMED
  -> NEW_AGENT_CREATED                           [PERMADEATH]
```

`QUARANTINED` is a recovery state, not a death state. `RETIRED` or `REMOVED` must not silently be treated as permanent death; any replacement requires a specific audited world-policy decision and an explicit terminal event.

### D.8.6. No uncontrolled respawn or population refill loop

Do not implement population management as:

```text
if connected_bots < target:
    create_bot()
```

That loop would replace disconnected bots, create duplicates during startup races, and potentially grow the population after crashes.

The controller instead evaluates:

```text
if bootstrap_not_complete:
    resume_one_time_bootstrap_transaction()
else:
    consume_only_available_permanent_death_replacement_credits()

for each living resident:
    reconnect_or_recover_the_same_agent_if_policy_allows()
```

The bot client manager may restart a failed process, but it must restart the same resident with the same `AgentId` and player identity.

## D.9. Resident population and simultaneous client capacity are separate controls

A resident population of `N` does not require an unlimited number of client processes if the architecture intentionally supports dormant/offline residents. Define two separate settings:

- `resident_target`: the maximum number of living agent identities;
- `max_concurrent_client_sessions`: the maximum number of bots connected to the Minecraft server at the same time.

If `max_concurrent_client_sessions < resident_target`, some agents will be offline/dormant. They still count as residents and must not be replaced. They may only be described as continuing to think or simulate while offline once the player-independent cognition/virtual-region system actually implements that behavior.

Until that system exists, an offline bot retains its identity and history but does not physically act in the Minecraft world. The implementation must not claim that it mined, crafted, moved, or changed blocks while disconnected.

For an initial implementation where all residents are expected to be physically present, set `max_concurrent_client_sessions == resident_target` and benchmark the server and client resource costs at the configured target. Do not promise a population size before measuring it on the actual modpack and hardware.

The population cap is a governance rule, not a performance target. The maximum technically stable number of client processes is a separate benchmark result.

## D.10. Client process isolation and runtime management

For the first production implementation, start one ordinary Fabric client process per connected bot. Each process must have:

- a stable bot profile and unique player identity;
- an isolated game directory or equivalent safely isolated per-agent state;
- a pinned Minecraft/Fabric/modpack profile;
- independent logs and crash records;
- bounded memory/CPU settings;
- a known window/display or headless rendering configuration;
- a local control channel bound to the correct `AgentId`;
- a reconnect policy that never creates a new resident identity.

Shared immutable game libraries and mod artifacts may be cached to reduce disk duplication. Mutable session data, options, logs, and credentials must not be accidentally shared between bots.

Do not assume that a standard dedicated-server `--nogui` option makes a client headless. The client is still a client runtime and may need a windowing/rendering environment, even if no human watches it. A headless/off-screen mode must be proven against Minecraft 1.20.1, the selected Fabric Loader, graphics dependencies, and the actual modpack. If not proven, begin with visible or minimized client windows and optimize later.

A bot's client process exit code is an operational event only. It does not establish death of the agent.

## D.11. Required modules and responsibility changes

The revised architecture should include or adapt the following components:

| Component | Responsibility |
|---|---|
| `WorldServerConnector` | One configured server endpoint and readiness/connection status for the world. |
| `BotClientManager` | Launch, monitor, stop, and reconnect bot client processes. It never launches one server per bot. |
| `BotClientProfileBuilder` | Build isolated client runtime profiles from the locked Fabric modpack. |
| `ModpackParityVerifier` | Compare the server pack and client-compatible pack, validate versions/dependencies, and report typed mismatch states. |
| `ClientSessionRegistry` | Bind `AgentId` to expected player UUID/profile, client process, active session, and current server player entity. |
| `ClientCUAAdapter` | Issue ordinary client input and collect client-side observations/results. |
| `AgentRegistry` | Durable identity and resident lifecycle records. |
| `PopulationLedger` | Resident count, one-time bootstrap, target policy, and audited population changes. |
| `ReplacementCreditLedger` | Exactly-once one-for-one replacement after permanent death. |
| `DeathFinalizer` | Apply the configured death policy, write tombstones, and issue a single replacement credit. |
| `AgentMind` / `MindScheduler` | Persistent cognition and task decisions, independent of client-process lifetime. |
| `ServerWorldEvidence` | Verify actual world/inventory/container changes from server-authoritative state. |
| `AxisGateway` | Canonical authority checks for consequential actions; no client-side bypass. |

The current server mod can continue to provide server-side registration, durable world-linked identity, permission enforcement, and evidence recording. The bot client does not become the authority for its own actions.

## D.12. Migration from the in-process FakePlayerBody implementation

Do not discard the delivered work. Reclassify it accurately.

### D.12.1. Retain as proven test infrastructure

The existing `FakePlayerBody`, `PlayerMotor`, `BodyAdapter`, `AgentMind`, `MindLedger`, `MindPersistence`, `MessageBus`, and handoff tests remain valuable. Preserve their evidence and keep relevant regression coverage.

The fake-player harness remains useful for:

- testing server-side action semantics in a controlled harness;
- testing mind logic without launching a full client process;
- unit-testing inventory/event verification;
- comparing synthetic and connected-player outcomes;
- testing the secondary mob adapter independently.

But its results must be labelled as synthetic fake-player results, not as proof that the production networked client has joined a real server.

### D.12.2. Add the networked-client embodiment adapter

Add a separate `ConnectedPlayerClientAdapter` (name illustrative) that binds a persistent agent to an ordinary connected client session. It must not impersonate a fake connection.

This adapter is responsible for:

- launching or attaching to the bot's Fabric client process;
- authenticating/connecting to the shared server;
- proving the player profile UUID matches the registered agent identity;
- mapping motor commands into the normal client's input stream;
- observing client state and receiving server-side action evidence;
- detecting disconnects, reconnecting the same identity, and refusing duplicate sessions;
- ensuring that the server-created player entity belongs to exactly one `AgentId`.

The `AgentMind` should depend on a logical `PlayerMotor` / `WorldView` / `BodySession` interface, not on `FakePlayerBody` internals. The connected-player adapter and synthetic test adapter can implement the relevant interfaces independently.

### D.12.3. No simultaneous dual body

When an agent transitions from synthetic testing to a connected production session, the registry must atomically transfer its embodiment owner. The prior body must be removed or disabled before the connected server player is accepted as the live body. There must never be two bodies with the same `AgentId` performing actions in one world.

## D.13. Revised implementation sequence

This addendum changes the next implementation priority. Do not continue building only against the server-side fake-player harness and later assume that its behavior is equivalent to a connected Fabric client.

### `NET-00` — Lock the shared-world runtime

- Confirm Minecraft 1.20.1 / Fabric and the exact Loader/Fabric API versions.
- Choose the single server directory and world save.
- Define whether the server is externally launched or managed once by the supervisor.
- Define the server address, bind/firewall policy, authentication mode, and client profile root.
- Produce the server modpack lock and a derived client modpack lock.

**Exit:** exactly one authoritative server exists for the world, and its runtime/profile is reproducible.

### `NET-01` — One ordinary bot client joins the shared server

- Launch one Fabric client process using the locked client profile.
- Connect it to the existing shared server.
- Confirm the server sees a normal logged-in player session and creates the expected server-side player entity.
- Bind the server player UUID to one durable `AgentId`.
- Disconnect and reconnect; confirm the same identity returns.
- Confirm no extra server process or extra world is started.

**Exit:** one bot client joins and leaves the same world through the ordinary connection path, preserving the same identity.

### `NET-02` — Connected CUA parity

- Drive movement, look, jump, hotbar selection, use, mining, attack, and item handling through the connected client input path as supported.
- Verify each consequential action on the server.
- Compare at least one identical task between a real human client and a bot client.
- Preserve typed refusals and action evidence.
- Demonstrate that the client cannot bypass server reach, permission, inventory, or protection rules.

**Exit:** the connected client can carry out the initial player action vocabulary with server-verified results.

### `NET-03` — Two connected bots and one human player

- Launch two isolated client sessions.
- Bind two unique player UUIDs to two unique `AgentId` values.
- Join a human player to the same server.
- Re-run the item handoff through connected clients.
- Demonstrate that messages are tied to the correct agent identities and that the human remains a distinct participant.

**Exit:** two independently connected bot residents and one ordinary human player share one authoritative world.

### `POP-01` — One-time population bootstrap and cap

- Implement `resident_target` as an operator-configured world setting.
- Fill an empty registry up to the target once.
- Persist bootstrap completion and identity mappings.
- Demonstrate that server restart, supervisor restart, client crash, and network disconnect do not create extra AgentIds.
- Demonstrate that a temporarily offline resident reconnects as the same identity.

**Exit:** initial population is bounded and reconnect/recovery cannot create replacement residents.

### `POP-02` — Permanent-death finalization and replacement credits

- Add explicit `death_policy` configuration.
- Implement durable terminal death records and tombstones.
- Mint one replacement credit for one finalized permanent death.
- Consume the credit once to create one new identity.
- Demonstrate exactly-once behavior across server/supervisor crashes at every transaction boundary.
- Demonstrate that ordinary disconnect, client crash, and server restart never mint credits.

**Exit:** one permanent death produces at most one new resident; no other event creates a new resident after bootstrap.

### `MOD-01` — Client modpack parity

- Build the client pack from the server modpack lock and Fabric metadata.
- Include client-side and common mods as required; exclude server-only mods from client runtime.
- Validate Fabric Loader, Minecraft, dependencies, mod versions, registries, and connection handshake.
- Test at least one server pack containing a common mod, a client-only mod, and a server-only mod.
- Confirm the bot can join and perceive/interact with baseline modded content through generic controls without a special integration for each mod.

**Exit:** the bot can join the selected modded Fabric server using the derived compatible client pack.

### `SCALE-01` — Measured client population

- Benchmark increasing numbers of connected bot clients on the actual server and modpack.
- Record client memory, server memory, CPU, tick time, network use, launch time, reconnect behavior, and action latency.
- Choose an operational concurrent-session ceiling from evidence.
- Keep this ceiling distinct from `resident_target`.

**Exit:** an empirically supported operating range exists; no unsupported population promise is made.

Mob adapters remain later work. They do not substitute for connected fake-player clients and must not be used to mark this networked player embodiment work complete.

## D.14. Acceptance and recovery tests

The implementation must test the topology and lifecycle, not merely whether a process starts.

### D.14.1. One shared server

- Start one server and record its process identity, world path, and server address.
- Launch multiple bot clients.
- Confirm every bot appears in the same world, same dimension coordinate space, and same server player list.
- Confirm no bot client starts a dedicated server or creates a second world save.
- Confirm a human client can join the same world.

### D.14.2. Identity/session correctness

- Every bot has a unique AgentId and player UUID.
- Duplicate UUID/profile login is rejected or reconciled; it never binds one server entity to two agents.
- Killing one client process does not create a new identity.
- Reconnecting restores the same AgentId and persistent mind.
- Server restart restores the population registry without duplication.

### D.14.3. Population invariant tests

Given `resident_target = N`:

1. Empty bootstrap creates exactly `N` residents.
2. Restarting the supervisor any number of times creates zero additional residents.
3. Disconnecting or crashing one client leaves resident count at `N`.
4. Restarting the server leaves resident count at `N`.
5. Quarantining one damaged record leaves resident count at `N`.
6. One finalized permanent death creates one credit and reduces living residents to `N-1`.
7. Consuming that credit creates exactly one new resident, restoring `N`.
8. Replaying the same death event or restarting between credit creation and consumption does not create a second resident.
9. A second resident death creates a second credit only after its own finalization.
10. No new identity can be created without bootstrap authorization or an unused replacement credit.

### D.14.4. Modpack parity tests

- The server profile and bot client profile use Minecraft 1.20.1.
- Fabric Loader versions meet the declared compatibility policy.
- Common mods have compatible versions on both sides.
- Client-only mods are present on clients and absent from the dedicated server.
- Server-only mods remain on the server and are absent from clients.
- Required client dependencies are present.
- A deliberately missing required client mod yields a typed failure, not a partial join.
- A deliberately mismatched mod version yields a typed failure.
- The server's registry/content synchronization completes before the agent is considered online.

### D.14.5. World-action parity tests

- Move a connected bot through ordinary client input and verify server position.
- Mine a block and verify server block state plus inventory delta.
- Place a block and verify server block state plus inventory decrement.
- Interact with an ordinary container and verify server container/inventory state.
- Attempt an out-of-reach or unauthorized interaction and verify server refusal.
- Disconnect during an action and confirm that the action is recorded as unknown/interrupted until server evidence resolves its outcome; never assume success.

## D.15. Explicit non-goals and refusals

This addendum does not require:

- one private Minecraft server per bot;
- one private world save per bot;
- a custom AI integration for every Fabric mod;
- automatic understanding of every modded item, machine, menu, entity, or recipe;
- silently downloading and executing mods requested by a remote server;
- bypassing server-side checks through direct mutation;
- treating a dropped connection as permanent death;
- replacing an offline agent with a new identity;
- unlimited simultaneous client processes;
- claiming that offline agents physically act in chunks while no client/body or virtual simulation is active;
- treating a normal Minecraft death as permanent identity death without an explicit world policy.

The following must be hard failures/refusals rather than guessed workarounds:

- server endpoint is not the configured world;
- bot's profile maps ambiguously to an AgentId;
- duplicate active session for one AgentId;
- profile UUID is not the registered UUID;
- required Fabric mod or dependency is absent;
- client/server version or loader profile is incompatible;
- modpack integrity is unknown or untrusted;
- server's registry synchronization fails;
- client-side action cannot be linked to server-side evidence;
- population replacement has no valid permanent-death credit;
- replacement credit is already consumed;
- death policy is unconfigured;
- population target would be exceeded;
- client crash is misclassified as permanent agent death.

## D.16. Required operator decisions

The implementation can be built without inventing values for these settings, but the operator must ultimately choose them:

1. **Resident target `N`:** how many living bot identities should populate the world?
2. **Concurrent session limit:** should every resident be connected simultaneously, or may some residents remain offline/dormant?
3. **Server lifecycle owner:** is the single local Fabric server launched manually/external to the bot supervisor, or should one world supervisor manage that one server process?
4. **Authentication:** authenticated online-mode profiles or isolated local-only offline-mode identities?
5. **Death policy:** should a Minecraft player death respawn the same AgentId, or does the world's permadeath rule permanently terminate that identity?
6. **Client presentation:** visible/minimized clients initially, or a separately verified off-screen/headless client environment?
7. **Modpack source of truth:** which server instance/export/lock file is authoritative for constructing bot client profiles?

No implementation may silently invent a resident target or permadeath rule. Until the death policy is selected, automatic replacement remains disabled.

## D.17. Final binding rules

1. The world runs on one shared Minecraft 1.20.1 Fabric server.
2. Bots join that server as independent Fabric client sessions.
3. A bot is an agent identity plus a temporary connected player body—not a server process.
4. The production embodiment uses the normal client/server login and action path. The in-process `FakePlayerBody` remains a test/compatibility adapter only.
5. The server is authoritative for world state and action outcomes.
6. Bot clients load the same **compatible Fabric modpack**, filtered correctly for the client environment. They do not need bespoke integration for every mod.
7. The population target counts living identities, not connected processes.
8. Initial bootstrap fills the target once.
9. After bootstrap, a new identity is created only by consuming one durable replacement credit generated by one finalized permanent death.
10. Disconnects, crashes, restarts, kicks, profile errors, and missing data never count as permanent death.
11. Ordinary Minecraft player death is permanent only when the explicit world death policy says it is.
12. Every process, identity, and replacement transition must be recoverable and idempotent across restarts.

**The objective is a persistent population of agents living together in one modded Minecraft world—not a fleet of isolated servers pretending to be one civilization.**

---

## References

1. Fabric API 0.92.5+1.20.1, `FakePlayer` Javadoc. Documents `FakePlayer` as a `ServerPlayerEntity` subclass used for automated player actions; supports the distinction between a server-side synthetic player and the networked client architecture selected here. <https://maven.fabricmc.net/docs/fabric-api-0.92.5%2B1.20.1/net/fabricmc/fabric/api/entity/FakePlayer.html>
2. Fabric Loader documentation. Describes Loader, mod metadata, dependencies, entrypoints, and mod loading. <https://docs.fabricmc.net/develop/loader/>
3. Fabric `fabric.mod.json` reference. Defines the mod environment and entrypoint metadata, including client/server/all-side distinctions. <https://docs.fabricmc.net/develop/loader/fabric-mod-json>
4. Fabric entrypoints documentation. Explains `main`, `client`, and `server` entrypoints and the distinction between common and client-side initialization. <https://wiki.fabricmc.net/documentation%3Aentrypoint>
5. Fabric networking documentation. Describes client/server packets and logical-side synchronization; used here for the general client/server boundary, not as a claim that a particular modpack has been tested. <https://docs.fabricmc.net/develop/networking>

**Version note:** The project target remains Minecraft 1.20.1 / Fabric. The 1.20.1 Fabric API Javadoc is version-specific. The general Fabric documentation URLs may describe the currently maintained documentation set; implementation must still verify the precise 1.20.1 Loader/API/runtime behavior and mappings against the project's pinned dependencies before coding. No NeoForge 1.21.1 API assumptions are carried into this addendum.

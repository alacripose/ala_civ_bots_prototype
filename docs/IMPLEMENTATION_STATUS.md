# Implementation status

Plan: `minecraft_ensouled_system1_cua_architecture_v4.md` (§44/§45), `minecraft_product_execution_directive_beyond_phase1.md` and
`minecraft_fake_player_first_addendum.md` (Addendum C). Target: **Minecraft 1.20.1 / Fabric**
(explicit project instruction; Addendum B's provisional 1.21.1/NeoForge target is not used).
This file states what is actually built, what is deliberately absent, and how each claim was checked.

## 2026-11-02 — learned verbs are a replay of the practice log

`MindLedger` now carries one record of practice: `(tick, skill, verb, ok, evidence)` rows. `SkillStats`
is a cache rebuilt by replaying those rows on load; the save writes `mind.practice` and no longer
writes `mind.stats`. A save from before the log migrates each counter into labelled `migrated:stats`
rows on first load, so a resident keeps the verbs it had learned when it relogs. `/ensouled inspect`
prints the derived view (`verbs: harvest(3/1) ...`).

Checked: `./gradlew --offline --no-daemon build` exits 0, including `PracticeLogTest`. Live server
`0.1.0+g8d5beb5a…`; the wish "bring me 20 oak logs" opened `ticket:9a8668df…` and streamed it as
ledger kind `record` at seq 29487 with verb `harvest`, cells 0/20.

## 2026-11-02 — Node motor body: chat transport and client observation

**Layer correction.** Axis is not part of the Node body and is not proved by its logs. It is the
server-side decision-tree / per-agent MongoDB layer from the shploingus, openlock and secure-shell
package-streamer suite. The local `dev.ensouled.axis` code is a Minecraft-side port. The live Node
process is a motor-only mineflayer client; it connects directly to the Minecraft server. The file
motor bridge is local-only and is not the deployable transport.

The live resident Ash is currently carried by a **Node mineflayer protocol client**, not the Fabric
client mod. This is a useful independent proof of the file-motor and ordinary-chat paths, but it is
not evidence for v4 §44.5's Fabric client-mod parity. See `CHECKPOINT.md` for the full transcript.

- **Movement repair:** `mineflayer-pathfinder`'s deferred `stop()` flag was making every `goto` after
  a timeout reject with `Path was stopped…`; `setGoal(null)` and a handled `gotoGoal()` restore the
  next frame. The same Node body then walked a sequence of ordinary path steps.
- **Native chat repair:** `BodyAdapter.say` → `NetworkPlayerBodyAdapter.say` → `FileMotor.say` writes
  `<Name>.speech.jsonl`; Node calls `bot.chat` and writes `SPOKEN` to `<Name>.speech.results.jsonl`.
  A probe player received `<Ash> I have 8 spruce plankss and 20 food.` and the server transcript
  recorded the line with `transport=PLAYER_CHAT_PACKET` and Ash's `agent:4491e473…` id.
- **Client observation:** `Ash.client.jsonl` contains per-frame logical render snapshots (position,
  nearby blocks/entities, held item) and audio events heard by the client. It is explicitly
  `mode=logical-headless`; no GPU frame or audio-device proof is claimed.
- **Open:** the harvest wish `commit:1adaae6d…` reached stage 2 but no Node `MineBlock` or
  `CollectDrops` was observed, so the full wish→inventory→`work-satisfied` loop remains unproven.

## 2026-10-31 22:35 — the streamer: toast as the first window, per-agent streams, control tickets

The Minecraft mod is now a **downstream client of the shell's package streamer**, not a second
display. The ruling and the layer owners are in [`LAYER_TABLE.md`](LAYER_TABLE.md). The declaration
is a byte-for-byte copy of `secure-shell`'s toast package ([`packages/toast/toast.surface`](../packages/toast/toast.surface),
`sha256:05348dd2…`, pinned by [`toast.port.json`](../packages/toast/toast.port.json)).

| Layer | Java | What it refuses |
|---|---|---|
| `dl-enclave.surface/1` | `SurfaceDocument` | closed schema, shipped lane/rule/kind/role tables, geometry, `port-baseline` against the pinned baseline |
| `surface.mount/2` | `MountReceipt` | the eleven upstream fields and `receipt-id` formula; `mounted` / `already-mounted` / refused |
| `secure-shell.served/1` | `ServedPayload` | `tier-not-derived`, `tier-incomplete`, `served-no-surface`, `served-no-windows`, `wish-*`, `notice-shape` |
| `secure-shell.ledger/1` | `StreamLedger` | SHA-256 chain, contiguous seq, fsync before head; an unverifiable line makes the ledger read-only |
| per-civilian streams | `MongoStreams` | one stream per agent, write → read back → compare → `verified`; an unreachable store refuses to serve (`stream-store-unavailable`) |
| transport | `SurfaceStream` | loopback + bearer token (401 otherwise); a feed without an `agent` is refused (`served-no-agent`); unverified rows are never broadcast |
| display | `StreamClient` | the game's own `ToastManager`; a row whose actor is not this client's agent is not drawn |
| control | `ControlRelay` | declared action set, ticket + named presenter, ledger row before the packet; the client refuses a packet with no ticket/presenter |

The store is `ensouled_streams` on `mongodb://127.0.0.1:27018` (`scripts/mongo.sh`), following the
project family's own discipline in `openlock/tools/axis_witness.py`: candidate URIs, a ping before
use, and **a write is not a record until it has been read back and compared**. A row that reads back
different is stamped unverified, kept in `verifications` with its mismatch, and never served.

**Verified live** (server + two headless clients on 25565; not a fixture): mount `surface=secure-shell-toast
sequence=1 state=mounted windows=1 digest=sha256:05348dd2…`; `/api/served?agent=Bram` carries
`agent`, 1 wish, the refusal notices; `?agent=Resty` carries only Resty's rows; `/api/served` without
a token returns **401**; both clients log `witness command=GET /api/served?agent=<name> exit=0 checks=6
surface=secure-shell-toast digest=sha256:05348dd2…` and then `toast drawn by the client HUD` from the
vanilla draw path; Bram's refusal rows (`outcome=rejected`) are drawn as `Refused: record` toasts;
the store holds 0 cross-agent rows, 39 read-back `PASS` and 0 `FAIL`; a restart replayed 18 committed
rows (verified) and mounted `already-mounted` with the same sequence.

**Found and fixed on the real path:** `ClientInputMotor.cancel()` released keys with no program open,
so every cancellation produced three genuine `control-no-ticket` refusals; `cancel()` now returns when
no ticket was opened. The dev server's working directory is `run/`, so the streamer resolves the
project root by the declaration instead of by the caller's assumption.

**Not exercised, and named:** no committed `control:*` row was observed, because both beings were idle
(`perceive: choices=0 objects=0`), so no motor program opened. The witness and refusal rows are
committed and served; the ticket path is compiled and enforced but has no live row yet.

**The perception answer:** occlusion is real and server-side (`BlockSight.hasLineOfSight` raycasts
`RaycastContext.ShapeType.COLLIDER` from the eye; radius capped at 16; `canReach` checks eye reach and
standability) — the mind does not see through walls. But the package format carries **no observation**:
`served/1` carries windows, wishes, notices and evidence rows, `ControlRelay` is server→client input
only, and the client never perceives. A CUA driver reading this stream cannot see the environment.
The missing layer is a `world`-lane observation surface produced from the same `BlockSight` calls
(blocks, entities, needs, each with its line-of-sight result) and served per agent.

## 2026-10-31 21:56 — §49.1–§49.3: durable continuity, write-ahead persistence, recovery states

`MindJournal` (`run/world/ensouled/journal.log`) is append-only, one CRC32 per line, opened `rws`; a
transition the process could not write is refused rather than reported. `MindStorage` writes every
snapshot as a new generation: `minds.dat.tmp` is forced to disk first, the serving generation is
rotated to `minds.dat.prev`, and the new one is moved into place atomically — a kill leaves
generation N or N−1 and never a torn file. Each save starts from the entries already on disk and
replaces only the lives this process holds, so a waiting or blocked life is not erased by a snapshot
taken for someone else.

| §49.2 record | Field |
|---|---|
| stable AgentId and identity/profile binding history | `Continuity.bindings` (`body_kind`, `profile`, `entity`, first/last tick, count) |
| genesis provenance | journal `GENESIS` (id, name, created epoch, birth tick, lineage, gender schema/value); a life older than the journal is adopted into it at boot |
| AgentMind configuration and versioned state | `mind_format` in the mind root; `gate_policy` and `skills` (`id@revision`) in continuity |
| episodic memory, beliefs, skills, evidence | `mind.beliefs / episodes / evidence / events / practice` |
| need observations and learned mappings, with provider identity | `Continuity.needs_*`, `needs_provider` (`minecraft-1.20.1/player-native`), `needs_tick` |
| affect state | `affect_*` |
| current intentions and interrupted-action record | `mind.in_flight`; boot condition `interrupted <skill> at <pos>; outcome indeterminate until observed` (§16) |
| last known body/session state and reconciliation metadata | `last_body_kind/profile/dimension/x/y/z`, `last_session_end`, `boots`, `observed_generation` |
| relationships, conversations, commitments, contracts, test participation, death | **absent**, named by `Continuity.unrecorded` rather than implied |

Four conditions are observable at boot and through `/ensouled continuity`: `ABSENT` (no record yet),
`OPERATIONAL`, `RECOVERING` (stopped mid-action; reconciliation required), `INTEGRITY_BLOCKED`.
Loading distinguishes `ABSENT` / `LOADED` / `CORRUPT` / `UNSUPPORTED` (§16); only a loadable store is
written, and no mind is minted while a store is damaged or unreadable. `/ensouled journal [n]`
prints the transition chain and `/ensouled save` reports the generation or refuses. The operator's
save storm is `scripts/save_storm.py`; it drives the ordinary `/ensouled save`, not a test path.

**Verified live** (server killed, not a fixture — transcript in `CHECKPOINT.md`): SIGKILL mid-run
restored the last generation with both lives back; SIGKILL during a save storm loaded the last
committed generation and quarantined the half-written temp; a corrupt current generation fell back
to the previous one; both generations corrupt left both lives `INTEGRITY_BLOCKED` with names from
the journal, refused spawn and refused save, files untouched; a container format 99 was
`UNSUPPORTED` with the same refusals. Reconnecting clients reconciled `RECOVERING` → `OPERATIONAL`
with harvest counts intact.

## 2026-10-31 (evening): speech from the corpus, needs-driven movement, a real world

These three were the live gaps: the beings rested by default, the world was superflat, and their
"speech" was a machine rule line rather than English.

| Change | Implementation | Evidence |
|---|---|---|
| Every spoken word is the corpus's | `tools/prose_compile.py` now also compiles `prose_lexicon.tsv` (69,555 attested words, from the `evidence_quote` of all 121,501 claims). `Lexicon` loads it; `Realizer` composes a sentence only from those words plus the world's own item names and the being's own numbers. A meaning the corpus never names is reported `unsayable` and never spoken | build prints `prose_compile: 69555 attested word(s)`; live: `<Traveler> Ash, how are you doing out here?` → `<Ash> I have 2 oak logs and 20 food.` — matching its real inventory |
| The being composes, the baseline classifies | `Talk` states the sentence as `<ref>: VERB [sentence]`; `ProseLogic` admits it only if the verb, kind and modality are the corpus's; the English sentence is what is said | `DECISION chose_claim … kind=Fact modality=HOLDS`, `speech -> client Ash: I have 2 oak logs and 20 food.` |
| Rest is a need, not the default | `AgentMind` carries `fatigue` (0…1000, saved with the mind); a rest candidate exists **only** above `FATIGUE_REST_WANTED`, and its utility is the need alone (habit no longer compounds it) | `food=20 health=20 fatigue=36`, `current=wander@16,74,35` |
| A being with nothing urgent moves | new `wander@1` skill + `SKILL_WANDER` permit; the candidate is always available, weighted by restlessness, and the walk is verified from where the body actually ended up | `VERIFY satisfied walked 3 blocks to 17,75,20 {from=15,77,19, to=17,75,20}` |
| A real world, not a test plane | `level-type=minecraft:normal`, seed `ensouled-civilisation-1`; the flat world is kept as `run/world.flat-baseline` | player positions at y≈76–83 where the flat world was y=−60; oak trees harvested in the new terrain |
| The client's own body does the work | the client control channel gained `attack`, `use`, `select`, `drop`; `ClientInputMotor` now bridges `mine_block`, `drop_item`, `equip`, `eat`, `use`, `jump` and `move_away_from`, verifying each against server state | `data get entity Ash Inventory` shows `oak_log x2` mined through the client's attack key; all keys are released when a program ends |
| The client's inputs are held, not tapped | a headless client has no mouse or keyboard, so its key state is cleared every tick; `EnsouledClient` re-applies attack/use for as long as the intention holds them | acacia, oak and birch logs all enter inventories after this; before it, swings lasted one tick and every mining attempt timed out |
| The aim is at the block, not at the horizon | `face()` sends yaw **and** pitch to the block centre; a level aim misses a block at eye height and therefore never mines it | chest-height acacia at (−9,66,84) broke only after the pitch fix |
| A walk is a walk | `WanderRoutine.destination` refuses anything closer than 3 blocks, so a destination is not a one-block shuffle that fails its own movement check | `VERIFY satisfied walked 4 blocks to -15,64,81 {from=-15,66,78}` |
| Launchers cannot go stale | `scripts/bots.sh` syncs the newest `build/libs/ensouled-*.jar` into the bot profile, and `scripts/devserver.sh` into `run/mods`, before starting | `client mod synced: ensouled-0.1.0.jar`; without it the server ran a 20:15 jar against 20:41 sources |
| A being wakes where its body is | `AgentRecord.attach` reconciles the home anchor to the materialized body and records the move (`ev:home-reanchored`); scope checks no longer use a coordinate from a replaced world | `home=-5,81,-9` after the world change (was `0,-60,0`) |
| A being talks to itself, not only to people | `AgentMind.innerLife` murmurs every `MURMUR_INTERVAL_TICKS` when nobody is speaking to it: an observation, a need, or an attested interjection (`hm`, `well`, `oh`), all composed by `Realizer` from corpus words + its own numbers, spoken through the client | `<Ash> Well.`, `<Resty> I need wood.`, `<Bram> I have 28 oak logs and 20 food.` |
| A close call becomes a memory cell | `reasonAboutContest` remembers a decision within `CONTEST_MARGIN_MILLI` as a `THOUGHT` episode, naming both contenders and the margin | `THOUGHT weighed rest and wander (70 apart) @9,70,31 (self_reasoned)` |
| A rest re-processes memory | while `rest@1` executes, `dream` draws two memories wide and joins them with a corpus connective, recording a `DREAM` episode with both source episode ids as provenance | `DREAM wander failed: path_invalid and wander failed: no_progress @21,73,20 (dream_reprocessed)` |

Still open after this pass: `SOC-01` (a conversational request acted on), `CHAT-02` (bot-to-bot
conversation), `PAR-01`, and container screens through the client — chest transfers still go through
`PlayerInteractionHands` on the server rather than the client's own screen.

## Delivered: §17 Phase 1 / §31 Phase 1 — one embodied being

Plan: `minecraft_ensouled_system1_cua_architecture_v3.md` (§17/§31, Addendum B) and
`minecraft_fake_player_first_addendum.md` (Addendum C). Target: **Minecraft 1.20.1 / Fabric**
(explicit project instruction; Addendum B's provisional 1.21.1/NeoForge target is not used).
This file states what is actually built, what is deliberately absent, and how each claim was checked.

## Delivered: §17 Phase 1 / §31 Phase 1 — one embodied being

The primary body is the fake player required by Addendum C; the mob body from the earlier slice is
kept as a second adapter behind the same body seam.

| Plan requirement | Implementation | How it is evidenced |
|---|---|---|
| Materialize a fake-player body as the primary citizen (Addendum C §C.1) | `FakePlayerBody` is a real `ServerPlayerEntity` on a silent `FakeClientConnection`, joined through the vanilla player path, owning its ordinary player-data file | self-test `player1` + `phase2`; `/ensouled spawnplayer` |
| Player movement without a client (§C.4.1) | `FakePlayerBody.tickBodyPhysics()` runs the shared player physics (`tickMovement`) each server tick, because vanilla server players take their position from a client; the motor only sets yaw, forward/strafe and jump | self-test `player1`: walk to the tree, arrive within 1.5, no teleport |
| Player break semantics (§C.4.2) | `PlayerMotor` breaks via `ServerPlayerInteractionManager` with real `START`/`STOP` player action packets, timed from `calcBlockBreakingDelta`; drops come from the vanilla break | self-test `player1`: oak log removed, log enters the inventory |
| Second body kind behind one seam | `BodyAdapter` / `PlayerBodyAdapter` / `MobBodyAdapter`; the mind only sees `Motor`, `WorldView`, body position | both self-test body modes |
| Secondary mob body (earlier slice) | `AgentBodyEntity` (persistent, 0.6×1.8, pathfinding, inventory 36, hunger) | `/ensouled spawn`, self-test `phase1` |
| Local visual/HUD/proprioceptive sensors | `MinecraftWorldView` / `PlayerWorldView`: 16-block sight radius + collider raycast, own inventory/HUD state, 8-block entity stimuli. No remote chunks, no other inventories, no hidden health. **No sound perception exists** (`PerceptSource.SOUND` is declared but nothing emits it) | unit test `sensesAreLimitedToThePerceptionRadius` |
| Move, look, jump, use, mine, equip, eat as bounded inputs | `MotorCommand`: `move_to`, `look_at`, `jump`, `equip`, `use`, `mine_block`, `collect_drops`, `eat`, `move_away_from`, `wait`. The fake-player body implements all ten; the mob body keeps the earlier subset and was deliberately left untouched | self-test `verbs` (player body), `player1` / `phase1` (movement, mining, pickup) |
| Deterministic mechanics catalog for a tree, a tool, a block drop, hunger | `BlockIds` (log/edible predicates), vanilla hardness/tool math and real durability wear through `ServerPlayerInteractionManager` | `harvestsThroughBodyMechanicsAndRecordsVerifiedEvent`; self-test `verbs` (pickaxe damage 0→1 on a stone break) |
| Player-semantics verbs (§C.4.2) | `jump` holds the vanilla jump flag; `equip` sends the client slot-selection packet (hotbar only); `use` resolves a real block-face interaction through the interaction manager and refuses when vanilla refuses | self-test `verbs`: Y +0.42, held item changes, dirt placed at the clicked face with the stack shrinking 8→7 |
| Reflexes + one System-1 arbiter | `ReflexLayer` (closed list: burning, drowning, adjacent hostile) + `Arbiter` (single intention, hysteresis, stable tie-break) | `reflexPreemptsAnActiveIntention`, `arbiterKeepsTheIncumbentUnlessChallengerWinsClearly` |
| Verify motor outcomes against the world | `HarvestTreeRoutine.verify`: block actually removed **and** inventory delta > 0, otherwise typed failure; `jump`, `equip` and `use` verify a changed Y, a changed held item and a changed block. `look_at` and `wait` stay acknowledgements, not world checks | `acknowledgementWithoutWorldChangeIsRejected`, `missingDropAfterMiningFailsTheInventoryPostcondition` |
| Stable identity + episodic memory across restart | `MindStorage` (gzipped NBT in the world save) + `MindPersistence` (save tree) over `AgentMind` decisions and `MindLedger` history; identity, beliefs, episodes, evidence, canonical events, budget | `restartRestoresIdentityHistoryAndDoesNotDuplicateTheEvent`, self-test phase 2 |

**Acceptance scenario (§17 Phase 1):** the being approaches a perceived tree, recognises it as a
reachable natural log, selects the permitted `harvest_tree@1` skill, passes the gate, mines with
real block mechanics, picks up drops by touch, records the block/inventory result, remembers the
location as an episode + canonical event, survives a restart, reconstructs continuity, and does not
duplicate the collection event. Covered by the unit suite and by the headless server harness
(`scripts/selftest.sh player`, phases `player1` and `phase2`).

**Code shape (this slice):** `AgentMind` owns one body's decisions only; `MindLedger` owns episodes,
canonical events, evidence and the practice log (including collection dedupe and site memory);
`MindPersistence` owns serialization; `BodyAdapter.tickBody()` owns body-kind simulation steps, so
`AgentManager` binds and ticks bodies without a kind branch. The acceptance harness lives in
`src/test/java/dev/ensouled/selftest` and is loaded into the dev server as a companion mod; the mod
jar contains no harness classes.

**Player verb contract:** `jump`, `equip` and `use` exist as first-class motor commands and change
real player state — Y, the selected hotbar slot, and the world block plus held stack. Covered by the
`verbs` harness phase (`scripts/selftest.sh verbs`), which drives the body's motor directly and
records each command and result in the same `MOTOR` evidence trail the mind uses.

### Phase exit evidence (fake-player body, 2026-10-31)

```
./gradlew test                      # 10 tests, 0 failures
./scripts/selftest.sh player        # player1 then phase2, clean world
SELFTEST PASS phase=player1 harvest events=1 inventory_oak_log=3 remembered=true body=player evidence=16
SELFTEST PASS phase=phase2 restart continuity kind=player site=6,-60,0 events=1 alive=true
  new_mine_commands=0 inventory=3->3 intention=idle
./scripts/selftest.sh verbs         # one boot, clean world
SELFTEST PASS phase=verbs verbs body=player jump=+0.42 equip=iron_pickaxe mine=stone_removed
  tool_durability=0->1 use=placed_at_3,-60,0 dirt=8->7 motor_evidence=10
```

## Delivered: two citizens, one settled handoff (Addendum C Phase 5 — partial)

| Plan requirement | Implementation | How it is evidenced |
|---|---|---|
| Structured message with provenance, scope, expiry, delivery state (§C.6.2) | `AgentMessage` (request / accept / decline, item + count, subject, reason, created/expiry, state) and `MessageBus` (per-recipient inboxes, one audit log, expiry on read, rate window) | `HandoffProtocolTest`, self-test `handoff` (one request, one accept) |
| Agents decide for themselves; a message is neither a fact nor an authority (§C.6.3) | The supplier reads the request from its own inbox inside its own intention, checks its own inventory, then accepts or declines for its own reason; the requester only learns an answer | `aPeerWithoutTheItemDeclinesAndTheAskerLearnsTheRefusal` (decline reason `item_not_held`, asker records `peer_declined`, no transfer event) |
| A real item moves through player semantics | `drop_item` is the player's own drop path (split the held stack, `PlayerEntity.dropItem`, real item entity with pickup delay); pickup stays vanilla touch | self-test `handoff`: supplier bread 1→0, asker bread 0, verified eat |
| A shared task verified by world outcome, recorded once | The recipient records `item.transferred` keyed by the transfer id from its own inventory delta; the supplier records only its own loss | self-test `handoff`: `transfers=1`, supplier's canonical count 0 |
| A human player is not an agent body | `SocialSeam.agentIdForEntity` resolves only registered agent bodies; an unregistered player entity is never addressed | `aHumanPlayerUuidIsNotAnAddressablePeer` |

```
./scripts/selftest.sh handoff      # one boot, clean world, two citizens
SELFTEST PASS phase=handoff handoff citizens=2 request=msg:1 accept=msg:2 transfers=1 eats=1
  food=6->17 supplier_bread=1->0 asker_bread=0
```

Absent from this slice and not claimed: chat or proximity speech, human-player interaction, trust and
relationship state, message persistence across restart (the bus is in-memory and a restart drops
pending speech), offers or negotiation beyond one asking/answering exchange, and any multi-tasking
or work-order layer.

## Delivered: reusable world interaction and a chest-backed transfer (EMB-02)

The chest is the first implementation of a contract in its own package, not a container branch inside
the motor or the mind. [`docs/WORLD_INTERACTION.md`](WORLD_INTERACTION.md) records the contract, the
headless divergence, the authority note, and the structure.

| Requirement | Implementation | How it is evidenced |
|---|---|---|
| Perceived identity, reach, capabilities, permission, request, server result, postcondition, canonical event | `interact`: `InteractionTarget`, `InteractionRequest`, `InteractionOutcome`, `InteractionHands`, `InteractionSession`, `ChestInteraction`, `PlayerInteractionHands` | self-test `chest`; refusal probes return `not_reachable` and `unsupported_operation` without touching the world |
| Server-authoritative and headless | The adapter calls the `ChestBlockEntity` inventory with the same identity, reach, slot and count checks; the screen path of a client GUI is deliberately not used and the divergence is recorded in the class | self-test `chest`: delta facts `container_before/after`, `inventory_before/after` |
| Reach, slot, permission and interaction rules | Reach is 4.5 from the eye and re-checked at operation time; `AuthorityGateway.authorizeInteraction` runs first; `OwnershipBoundary` is a wired-but-empty protection hook; whole-stack moves roll back on overflow | refusal probes; `inventory_full` / `container_full` / `protected_target` codes |
| Typed refusal instead of guessing or routing around the gate | Every failure path returns a stable code; a refusal is never retried through another path | `unsupported_operation` on a chest, `not_reachable` on a far chest |
| No direct inventory mutation as a capability | The only verbs are inspect / withdraw / deposit, each through the real inventory API; there is no "set inventory" seam | code review of `interact`; `InteractionHands.none()` refuses every operation for a body without a path |
| Two independent minds, agreement over the bus, one transfer id | `request_supplies@1` states the requirement after inspecting its own chest; `supply_from_chest@1` independently inspects, accepts/declines, withdraws and hands over; the recipient walks back to the chest it identified and deposits | self-test `chest`: `asked`, `answered`, `heard`, no decline, `withdrawals=1 deposits=1 transfers=1` under `msg:1` |
| No duplication or unexplained loss | One canonical event per side per transfer id; conservation is asserted across both chests and both inventories, and no loose stack may remain | self-test `chest`: `conserved=true`, `loose=0`, `source=4->2 sink=0->2` |
| The provisional gate stays provisional | `AuthorityGateway` is the only seam for intentions *and* interactions; `mind.AuthorityFactory` is the only constructor | `docs/WORLD_INTERACTION.md` §3, `docs/PHASE0_CONTRACTS.md` #6 |
| A human session is never treated as an agent body | `participant`: the role is resolved from the agent registry; a session not bound to a mind is an ordinary participant, and joining is never refused, spectated or restricted | code path only: `ParticipantHooks` logs the classification on join. No probe spawns a human or observer session, and no real-client join has been recorded |

```
./scripts/selftest.sh chest        # one boot, clean world, two citizens, two chests
SELFTEST PASS phase=chest chest citizens=2 transfer=msg:1 source=4->2 sink=0->2
  drawer_inventory=0 storer_inventory=0 withdrawals=1 deposits=1 transfers=1
  conserved=true refusals=not_reachable,unsupported_operation
SELFTEST PASS phase=handoff ... / phase=player1 ... / phase=phase2 ... / phase=verbs ...
```

Absent from this milestone and not claimed: container types other than a single chest, workstation or
screen interaction, claims and protection as a real ledger, trade or valuation, and partial-stack and
overflow slot policy. Human-directed speech exists only as the manual `HUM-01` path below.

## Flow through the entity-AI takeover roadmap (Addendum B)

| Roadmap phase | Status | Evidence / reason |
|---|---|---|
| 0 — runtime lock + vanilla behavior atlas | **partial** | Runtime is locked in `docs/PHASE0_CONTRACTS.md`. No vanilla behavior atlas exists, which is correct for a mod-owned civilian but **required before any vanilla entity takeover**. |
| 1 — identity + entity adapters + shadow mode | **partial** | Stable `AgentId`, durable enrollment (`AgentRecord`/`MindStorage`) and save/reload identity proven. **Shadow mode is absent**; there is no observation-only trace against vanilla decisions yet. |
| 2 — mod-owned civilian, no vanilla decision dependency | **delivered** | `AgentBodyEntity` registers no goals and `FakePlayerBody` registers none either; System-1 is the sole writer; closed motor vocabulary; every command has an observed result and typed failure. |
| 3 — scheduler independent of vanilla AI ticks | **partial** | Cognition is driven by the server tick event, not by entity AI or player proximity; but cognition stops while the body is unloaded, and there are no per-agent budgets, due-tick queues or fairness accounting. |
| C5 — two fake players with communication and cooperation | **partial** | Two fake-player minds share one message bus and settle one real food handoff (`handoff`) and one storage-backed chest transfer (`chest`), each verified from world state under one transfer id. Communication is still a single typed request/accept/decline exchange, not a speech-act system; messages are not visible in ordinary chat, there is no negotiation, and there is one container kind. |
| 4–9 — vanilla takeover, regional simulation, city substrate, scale, rollout | not started | Out of scope for this slice. |

## §43 first-slice coverage

Present: one civilian with a stable identity; one physical route; one real navigation-and-interaction
skill (`harvest_tree`); one System-1 drive (resource collection with a bounded harvest budget); one
complete action envelope and gate; one verified outcome; one memory update; save/reload tests.

Absent: chest/workstation/material chain, chunk activation request, materialize/dematerialize
representation changes, and dematerialized cognition while the body is unloaded.

## Phase exit record and successor ticket

Phase 1 (bounded embodiment) is **complete** against the contract above: a fake player materializes,
is tracked, walks, breaks a block through player action packets, receives the drop, records a
verified canonical event, and keeps identity, history and inventory across a restart.

Deferred, classified as later-phase work rather than Phase 1 blockers:

- Typed speech acts beyond the single request/accept exchange, and player-visible chat.
- Container/GUI interaction and item transfer (§C.5.2).
- Two-body cooperation on one physical task (§C.10); currently one mind per body, one body tested at a time.
- Rendered-client parity check (§10.2 / §C.4.3 benchmark against a real player).
- Regional/virtual representation for unloaded bodies (Addendum B Phase 6).

**Successor implementation ticket — delivered as `EMB-02`:** the container-backed chain now exists
(two citizens, chest withdraw, hand-over, deposit through a headless adapter that checks reach, slot
and permission rules, both deltas verified under one transfer id, and the unsupported-interaction
refusal exercised explicitly instead of assuming a fake connection can open a screen). See
[`docs/WORLD_INTERACTION.md`](WORLD_INTERACTION.md).

## Delivered and verified with a real client: a being asks a human (`HUM-01`)

`request_from_human@1` lets a being with a stock deficit address the nearest perceived human through
`HumanChannel` — one line in that player's ordinary chat, recorded as a `SOCIAL` evidence entry and
rate-limited on the shared bus — then wait for the human's own `Q` drop, collect it by touch, walk
back to the exact chest it identified, deposit, and record one `item.transferred` from its own
delta. `/ensouled humanprobe` prepares that scenario for any connected player: a chest beside them,
two `oak_log` in their inventory, and Ash. It refuses the setup when a harvestable trunk is within
seven blocks, because harvest utility would legitimately outrank the human request.

**Verified on the persistent dev server, 2026-10-31**, by the operator joining with an ordinary
client (offline-mode dev server, no fake player standing in for the human):

```
SOCIAL told_human  alacripose asked for 2 minecraft:oak_log as msg:1
SOCIAL human_supplied  alacripose left 2 minecraft:oak_log at -9,-60,-5 for msg:1
MOTOR  collect_drops(r=4) -> success collected {collected=1}
INTERACTION deposited chest@-8,-60,-4 deposit(oak_log x2 transfer=msg:1) -> PERFORMED
           {container_before=0, container_after=2, inventory_before=2, inventory_after=0, moved=2}
VERIFY satisfied  received 2 minecraft:oak_log and stowed it in chest@-8,-60,-4
LEARN  reinforced request_from_human successes=1 failures=0
```

There is still no unit or harness coverage of `HumanChannel`, `Humans` or `request_from_human`; the
real client run is the evidence, and a probe cannot fabricate the human side without proving nothing.
The ask itself is delivered as an `[ensouled]` system message, not the agent's player chat; Addendum F
§F.6.3 forbids counting that as native chat, so `CHAT-02` owns the real delivery.

## Delivered: non-disruptive harness (`DEV-01`)

The harness no longer replaces or deletes the development world. There are two explicit modes:

| Mode | Command | Owns | Never touches |
|---|---|---|---|
| Attach (interactive) | `scripts/devserver.sh`, then `scripts/attach.sh <phase>` | one session-scoped fixture plot at `spawn + 128/+128` and the bodies it spawns | `run/world`, the server process, connected sessions, anything outside the plot |
| Disposable CI | `scripts/selftest.sh <phase>` | its own server on 25579 and the world under `run/selftest-universe` | `run/world`, the dev server, its sessions |

`/ensouled harness <phase>|status|cancel` (operator) starts, reports and cancels a phase inside the
running server. Attached runs force-load only their plot, record owned bodies in
`run/ensouled-harness-owners.txt`, remove exactly those on the next run, and release the chunks when
done; `finish` stops the server only in CI mode. The CI trigger also requires `ENSOULED_SELFTEST=1`,
so a leftover trigger file on a normal dev boot is ignored and deleted instead of running a phase and
stopping the server.

The one assertion that had assumed a fresh process — `handoff` counting request/accept messages over
the whole shared bus — now counts only messages produced after the run started. That bug was found
by the first attached handoff run (`requests=2 accepts=2` from the earlier chest phase) and fixed.

**Verified 2026-10-31** on the persistent server (one process, one world):

- `scripts/attach.sh chest` → `SELFTEST PASS phase=chest run=dev-mvx05q8a` with the listener PID
  unchanged, `Stopping server` absent from the log, and a sentinel `gold_block` at `0,-59,0` still
  present.
- server restart, then `scripts/attach.sh handoff` → `PASS run=dev-mvx0afr9`, and
  `scripts/attach.sh chest` → `PASS run=dev-mvx0ai6n` in the same process; `/ensouled list` still
  showed exactly two citizens (the previous run's fixture bodies were removed, not accumulated);
  the same sentinel block and the restored Ash/Bram ids survived the restart.
- `scripts/selftest.sh chest` and `scripts/selftest.sh handoff` → `PASS` while the dev server stayed
  up on 25565; `run/world/level.dat` was byte-identical before and after, and the CI universe was
  the only directory removed. With the `.harness-owned` marker removed, the script refused to delete
  it and left its contents intact.

## Interim diagnostic only: a visible echo of agent speech (`CHAT-01` is not closed)

Every recorded speech act is echoed to connected players through `MessageBus` →
`AgentManager.announce` → `PlayerManager.broadcast`, in a form that is deliberately **not** player
chat:

```
[ensouled] Ash -> Bram: request 1 bread (msg:1)
[ensouled] Bram -> Ash: accept 1 bread (msg:1)
[ensouled] Bram -> Ash: request 2 oak_log (msg:2)
```

The first two lines are from the `handoff` probe (`logs/final-handoff.log`, 16:45:30); the third was
recorded on the persistent dev server with the operator's ordinary client connected
(`logs/devserver.live-echo-1646.log`, 16:46:32). A `DECLINE` line carries its recorded reason in the
same parentheses by construction (`announce`), but no decline with the tagged format has been
recorded yet. The drop/collect/deposit half of the earlier live run was observed under the
pre-relabel echo, not this one.

**This does not satisfy Addendum F.** §F.6.3: `ServerPlayerEntity.sendMessage` and the server
broadcast path are system/HUD message paths and are not a substitute for a bot sending ordinary
player chat; §F.7 forbids making the user see a fake speaker. The echo is useful (a person can watch
the exchange) but it is a diagnostic, and the milestone stays open as `CHAT-01`/`CHAT-02` below. The
ask a being sends a human through `HumanChannel` is the same kind of interim system message
(`[ensouled] …`), not player speech.

## Delivered: connected headless clients replace the test bodies (`NET-01`) and speak (`CHAT-01`)

The production embodiment is now what V4 §44 specifies: ordinary player sessions of connected
headless Fabric clients, one isolated client process per being, sharing one server and one world.
`FakePlayerBody` materialization is off unless a test asks for it with `ENSOULED_FAKE_BODIES=1`.

| Requirement (V4 §44) | Implementation | How it is evidenced |
|---|---|---|
| One shared server; headless clients join it, no server per bot (§44.1) | `scripts/bots.sh` starts clients against the already-running dev server on 25565 | persistent server, three clients connected at once |
| Real connected session as the body (§44.3) | The client joins through the normal login path; `AgentManager.bindNetworkedPlayer` binds the pre-existing mind to the new session and never mints a second one | `restored persisted mind agent:c3e1b84a… (Ash) onto its client Ash`, `Ash joined the game` with a real socket |
| Legacy fake body is test-only (§44.3) | `AgentManager` materializes persisted fake players only with `ENSOULED_FAKE_BODIES=1`; `scripts/selftest.sh` sets it for the curated CI probes | this boot: `server started; 3 persisted mind(s) pending bodies` and no fake joins until the three clients arrived |
| Isolated process and mutable state per bot (§44.5) | One client process and one game directory per being under `run/bots/<name>` (own options, config, logs, player data); the 85 MB of mod jars is shared through a link, not copied | `scripts/bots.sh status` → three running pids; `run/bots/Ash/mods -> tools/botprofile/mods` |
| Headless with no display, audio or narrator (§44.5) | The launcher writes each bot's `options.txt` (`narrator:0`, `onboardAccessibility:false`, master audio muted, low render distance, no pause on focus loss) and HeadlessMC's LWJGL patch removes the render context | `Setting user: Ash` … `Connecting to 127.0.0.1, 25565` with no window; `:143293` |
| Ordinary player chat, both directions (`CHAT-01`) | The client sends through its own chat path (`SpeechRelay`); the server ingests ordinary chat (`ServerMessageEvents.CHAT_MESSAGE` → `SpeechInbox` → mind) | live, on the shared server, with a second real player session: `<Traveler> Ash, do you have any oak_log?` then `<Ash> Ash@60931: RECORD [oak_log=1, food=20/20, act=rest, at=3,-60,-7]` |
| Identity survives body and runtime replacement | Server restarts and client relaunches rebind the same `AgentId`; affect and history are restored with it | three restarts and three relaunches, same ids `agent:c3e1b84a…`, `agent:c4b7b056…`, `agent:11702f06…` |

Absent from this slice and not claimed: acting on a conversational request (`SOC-01`), bot-to-bot
conversation (`CHAT-02`), the capability matrix and client-driven motor (`PAR-01`), and need
providers (`NEED-01`).

## Delivered: ordinary chat perception and a prose-logic speech baseline (`CHAT-01`, partial)

A being now hears ordinary player chat and answers with a claim, not a line written into the mod.

| Requirement | Implementation | How it is evidenced |
|---|---|---|
| Human chat is perceived, not a command channel | `ServerMessageEvents.CHAT_MESSAGE` → `AgentManager.hear` → shared `SpeechInbox`; a being takes only its own lines, which expire instead of arriving late | persistent dev server, real player session: `heard "Ash, do you have any oak_log?" from Traveler (…) by 3 mind(s)` then `Ash perceived 1 spoken line(s)` |
| A heard line is evidence, never authority | `AgentMind.hear` records `SOCIAL heard …` and an episode; nothing in the line can authorise an action | `t=37961 SOCIAL heard Traveler (…): Ash, do you have any oak_log?` |
| The being chooses what to say, and may choose silence | `Talk` composes candidate acts, `Talk` weights them from the being's `Affect` and situation; `KEEP_QUIET` competes and often wins (a resting being values silence, but not over a person who used its name) | `SOCIAL kept_quiet silence outweighed every composable claim (affect unwound)` (resting, unanswered) and `DECISION chose_claim … utility=551 of 3 candidate(s)` (addressed) |
| The prose-logic corpus is the vocabulary and the classifier | `tools/prose_compile.py` compiles `agent_god_files/prose_logic` (121,501 claims, 20,946 rendered divisions) into `src/main/resources/ensouled/prose_baseline.tsv` — 8 verb→kind→modality rows (`RECORD/Fact/HOLDS`, `FORBID/Prohibition/MUST-NOT`, `WHEN/Condition/IF-THEN`, …). The gradle task `compileProseBaseline` regenerates it on every build; `ProseLogic.classify` admits a line only in the corpus rule shape `<ref>: VERB [args]` with a baseline verb, and supplies kind and modality itself. A composed line that does not classify is refused by name (`unclassified_prose`), never spoken | build prints `prose_compile: 8 verb(s) from 121501 claim(s)`; live: `DECISION chose_claim Ash@39531: RECORD [oak_log=0, food=20/20, act=rest, at=1,-60,1] kind=Fact modality=HOLDS support=direct` |
| Affect is state, saved with the being | `Affect` holds one of the six feeling-wheel families, its secondary/tertiary label, radius (0…65535) and signed drift; chosen from the body's situation, eased rather than snapped, persisted in the mind store | `AFFECT changed Powerful → Satisfied → nailed-it -> Peaceful → Relaxed → unwound radius 11467 drift 0`; survives restart |
| Rest is a decision in the tree | `rest@1` is a real skill with a declared precondition; a candidate whose preconditions are missing is skipped so the next candidate can win instead of starving the tree | `DECISION selected rest@1,-60,1 utility=560 of 1 candidate(s)`; `VERIFY satisfied rested 100 ticks …`, `rest successes=205 failures=0` |

**Not delivered, stated plainly:** speech egress as ordinary player chat still needs the being's own connected client. With an in-process body the same decision is recorded as a typed refusal —
`speech_unavailable Ash@39531: RECORD […]` — rather than faked with a system message. `NET-01`'s
supervised headless client launcher remains the open ticket; the mod-side seam (`SpeechRelay`,
`SocialSeam.speech()`) is in place and was proven earlier with a connected `Ash` client.

## Addenda F/G — production embodiment is a headless Fabric client (binding, not built)

Addendum G (`minecraft_headless_full_fidelity_fabric_client_addendum.md`) and the product execution
directive are binding: the production bot is a headless **Fabric client**, not a server-side fake
player and not a raw protocol client. `HCL-01` is therefore the next milestone and is **not** done:
no headless Fabric launcher has been configured or launched, and HeadlessMC's documented login path
requires a Microsoft account that this workspace does not have credentials for.

What was proven on 2026-10-31 as a narrow protocol test only (`tools/agent-client`, mineflayer,
**not** parity): a real network client named `Probe` logged in over the ordinary login path and sent
one chat line. The server recorded `[Not Secure] <Probe> hello from a real client connection` and the
client received its own line back as chat, so the login and chat pipeline works. This is not an agent
identity, not MessageBus-sourced speech, and not `HCL-01`; the protocol probe is test-only.

## Addendum F — player parity, extensible needs, native chat (binding, not started)

[`minecraft_player_parity_extensible_needs_and_native_chat_addendum.md`](../minecraft_player_parity_extensible_needs_and_native_chat_addendum.md)
is binding. It supersedes any claim that the in-process fake player or a server system message proves
a player capability. The current tree does **not** satisfy it:

- `PAR-01`/`PAR-02` — no connected bot client exists; `FakePlayerBody` never opens a client session,
  so client input, HUD, screens, client mod execution and the normal chat-send path are absent.
- `NEED-01`/`NEED-02` — needs are the vanilla food level only; there is no need provider registry,
  no mod-state discovery and no HUD observation.
- `CHAT-01`/`CHAT-02` — no human chat is ingested (`ServerMessageEvents.CHAT_MESSAGE` is unused) and
  no agent sends player chat from a client; the echo above is a diagnostic.
- `CHAT-03`/`CHAT-04` — no conversational task requests, no persistent conversation history.
- `MOD-01` — no modpack client profile or compatibility manifest.

The typing rule is also explicit: unknown or unavailable player state/actions must surface as
typed refusals (`PLAYER_CAPABILITY_UNIMPLEMENTED`, `MODPACK_MISMATCH`, `NEED_PROVIDER_UNAVAILABLE`,
`UNOBSERVABLE_MOD_STATE`, `UNMAPPED_MOD_STATE`, `CHAT_DELIVERY_FAILED`, `CHAT_RECIPIENT_UNRESOLVED`,
`CHAT_INPUT_NOT_PLAYER_ORIGINATED`, `GUI_SEMANTICS_UNKNOWN`) rather than as a silent success.

**Next:** `PAR-01`/`CHAT-02` — one real connected bot client on the shared dev server, proving a
player chat send and receive; then `NEED-01` (need providers, mod-state discovery, HUD observation).

## Not started (later phases)

- **Phase 2** (recipes, containers, threats, unknown-mechanic experiments, belief confidence,
  failure taxonomy, replay, skill transfer): not started.
- **Phase 3** (feeling-wheel affect, affect→salience coupling, gender semantics): not started.
- **Phase 4–9** (typed speech acts, trade/trust, settlement, production/logistics/ecology,
  defense/storyteller/expeditions, population fidelity tiers, package stream): not started.

## Known gaps in the delivered slice

1. **Fake players have no client, no chat and no containers.** They never see a rendered world and
   cannot open GUIs; agent-to-agent speech exists only as a typed request/accept/decline, and `§C.5.2`
   container/menu handling is unimplemented (recorded as the next slice).
2. **Two fake players are now exercised, but only for one handoff.** Both bodies are bound, perceive
   each other, message and act; nothing yet covers three or more agents, two simultaneous exchanges,
   or messages that survive a restart.
3. **No in-air item use and no attack.** `use` targets a block face; right-clicking empty air
   (`interactItem`) and entity attacks are not implemented, so consumables beyond `eat` and combat
   remain absent.
4. **No rendered-client CUA adapter.** Only the in-process player adapter exists. The plan allows
   this but requires the parity test against a rendered client before the adapter may be called
   equivalent (§10.2). Not yet demonstrated.
5. **Axis replaced by a local gate.** `Phase1GatePolicy` is a stand-in with typed verdicts and
   action hashes, not the project's canonical Axis enforcement point.
6. **No block-ownership ledger.** Player-placed block protection (§10.5) is not tracked; the gate
   relies on a natural-log id whitelist instead.
7. **Site dedupe is permanent and proximity-based.** A felled tree is remembered as a place: any log
   within 3 blocks of a remembered collection is treated as the same tree, so regrowth at that spot
   is not re-harvested. A Phase 2 belief-invalidation rule would be needed for legitimate regrowth.
8. **In-flight intentions are dropped at restart**, recorded as an `ACTION_FAILED` episode with
   reason `interrupted at restart`. This is deliberate conservatism, not resumption.
9. **Perception is log-filtered.** The body scans a 33³ cube for `minecraft:logs` and hostiles only;
   a full block-type sensory budget arrives with the perception work in a later phase.
10. **No hostile defence or combat skill.** Hostiles only trigger the escape reflex.
11. **Cognition requires a loaded body.** The mind ticks only while its entity is bound and alive;
    there is no virtual/regional representation yet (Addendum B §34.3).
12. **No shadow mode.** System-1 cannot observe a vanilla-driven entity without taking over.
13. **No ownership/protection ledger.** `OwnershipBoundary` is a wired hook with a single permissive
    provider; `use` can still place a block anywhere the gate and reach allow, and player-placed
    protection is absent.
14. **The human path has one fixed request.** `request_from_human@1` asks for exactly two `oak_log`
    and stores them; a human cannot yet ask a being for anything, choose an item, or decline in
    chat. The real-client run is recorded above; there is still no automated test for it.
15. **Chat is a diagnostic echo, not a conversation.** Every agent speech act is echoed to all
    connected players regardless of distance in a `[ensouled]` system line, and a human cannot reply
    to it in chat; there is no proximity filter and no chat ingestion from a human session
    (`CHAT-01`/`CHAT-02` open under Addendum F).
16. **Affect, speech acts, contracts, story history** are absent, per phase ordering.
17. **Unexplained hunger rise on a peaceful dev server.** During the handoff probe the requester's
    food level climbed from 6 to 12 before it ate anything, and to 20 afterwards, without any mod code
    adding food. Cause not established. The handoff's eating proof therefore rests on the verified
    `eat_if_hungry` success and the emptied stack, not on the food number, and no second round is
    asserted from hunger.

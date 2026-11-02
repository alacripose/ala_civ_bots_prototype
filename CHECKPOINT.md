# Execution checkpoint — the model is enforced: lifetimes, Command envelope, live frames (2026-11-01 02:35)

## 2026-11-02 — Node body: native chat restored, per-body client evidence

**Layer correction (2026-11-02).** Axis is not a Node-side decision layer. It is the server-side
decision-tree and per-agent MongoDB management layer from the shploingus / openlock / secure-shell
package-streamer suite. The local `dev.ensouled.axis` code is this tree's Minecraft-side gate/proof
port; the Node process only executes motor frames and connects directly to the Minecraft server.
The observed `civilian.axis-cells/1` rows are 200-decision count batches, not a decision tree, and
the per-agent MongoDB store was not running at this checkpoint (`stream-store-unavailable`). The
file motor is local-only; a deployable server needs an authenticated network motor transport.

**Surface.** Server `0.1.0+src.450acaf1` on 25565; resident Ash `agent:4491e473-c7c9-3195-a8de-330c79a24db4`;
the connected body is the Node mineflayer process in `tools/residents/src/main.mjs`, not a Fabric client
and not the mod's `ControlRelay`. This proves the file motor bridge on its own surface; it does not
prove §44.5's Fabric client-mod parity.

**Fixed on the real path.**
- **Pathfinder freeze.** After two genuine 18 s walk timeouts, `bot.pathfinder.stop()` left
  `stopPathing` set. The next `goto` called `setGoal`, flushed the flag, emitted `path_stop`, and rejected
  immediately as “Path was stopped…”. `stopMovement()` now calls `setGoal(null)` and `gotoGoal()` keeps
  abandoned promises handled. The same Node body then reached `-1,64,14`, `-18,70,0`, and later
  `-45,70,-2` with ordinary path steps.
- **Speech had no outbound Node transport.** `SpeechRoutes` sent `ensouled:speech` to a Fabric client;
  the Node body could hear chat but never sent any. Added `BodyAdapter.say`, the external branch in
  `NetworkPlayerBodyAdapter.say`, and `FileMotor.say` (`<Name>.speech.jsonl`). Node tails that stream and
  calls `bot.chat`, then records `<Name>.speech.results.jsonl` and a client `speech` row.
- **Per-body observation.** `tools/residents/src/observation.mjs` records logical render snapshots
  (position, yaw/pitch, held item, nearby blocks/entities, screen) on each frame, plus chat/UI and
  sounds heard by the client. It is deliberately `mode=logical-headless`: no GPU frame or audio device
  is claimed.

**Observed live.** A normal player line `hey ash` (with a temporary probe connected as a separate
ordinary session to observe delivery) produced the Node log
`heard: <alacripose> hey ash` and `speech: said I have 8 spruce plankss and 20 food.` The probe
received `PROBE_CHAT <Ash> I have 8 spruce plankss and 20 food.` and `PROBE_ASH_REPLY`.
`run/world/ensouled/motor/Ash.speech.jsonl` contains the `Say` row;
`run/world/ensouled/motor/Ash.speech.results.jsonl` says `SPOKEN` / `chat_sent`;
`run/world/ensouled/chat/transcript.jsonl` records the outbound line with
`speaker_agent=agent:4491e473…` and `transport=PLAYER_CHAT_PACKET`. `Ash.client.jsonl` contains
render snapshots and audio rows such as `block.snow.place` and `entity.rabbit.hurt`.

**Still open.** Commitment `commit:1adaae6d-1c78-4366-bf23-f03833837235` (3 logs) reached stage 2 but
no `MineBlock`/`CollectDrops` was observed, so the full wish→inventory→work-satisfied loop is not
claimed. The Node protocol client also does not exercise Fabric modpack handshakes, client screens,
or GPU/audio hardware; those remain §44.5/v4 gaps.

**Build/check.** `./gradlew --offline --no-daemon -Dorg.gradle.jvmargs=-Xmx1536m
-Dorg.gradle.parallel=false build` exited 0 (compile, test, remap, assemble); Node syntax checks pass.

## Structure after this pass

- `domain/` owns the durable facts and nothing else: `WorldState` (one writer, one file) holds the
  `WorldIdentity`, its `Homebase`s, its `Run`s and the applied-command keys; `Command` is the envelope
  a state-changing request must be in; `Encounter` is the one ephemeral lifetime and is not serialised.
- The command surface (`/ensouled wish`) is the only minter of Commands; `WorldState.accept` is the only
  gate, and its answer (`APPLIED`/`REPLAYED`/`STALE`/`WRONG_WORLD`) is what a caller must handle.
- `core/FrameParser` is pure reading; `core/FrameLedger` is the writer that stamps a frame with its
  world and encounter and appends one row; `mind/AgentManager` and the wish command are its callers.
- Vocabulary: `tools/prose_ingest_mods.py` → `tools/*_lexicon.tsv` → `tools/prose_compile.py --extra` →
  `resources/ensouled/prose_{baseline,cues,lexicon}.tsv`. Nothing in the jar is hand-written.

## Proven live (dev server on 25565, world `world:4378178c…`)

A real wish, classified and recorded where the world owns the file:

```
frame:102140:1 source=wish actor=Rcon verb=comes verb_state=named
claim_shape=WHEN shape_basis=cues:if,someone verb_basis=lexicon:911
residue=[if,someone,at,the,the] world_id=world:4378178c-0522-4cc3-9f98-adc7d0b85cb5
utterance="if someone comes at dusk wake the others while the sun is low"
→ run/world/ensouled/frames.jsonl
```

- Re-sending the same wish after a restart: `That wish is already with Ash (key wish|Ash|if someone
  comes…): a retry is not a second wish.` No revision moved, no second frame written.
- Corpus: 44 mod trees (33,091 words) + 21 project documents, `CONTEXT.md`, `README.md`,
  `AGENTS_DO_NOT_IGNORE.txt` (8,909 words; v9 alone 68,580 tokens) merged into the lexicon:
  **96,537 attested words**, up from 69,555. Per-source coverage is in `tools/project_lexicon.report.json`.
- Observations: 915 `axis:observation` rows in `run/stream/ledger.jsonl`, 0 `was not written` warnings
  since the fix below.

## Fixed on the real path

- **Frames went to `run/run/world/ensouled/`.** The ledger resolved a relative path against the server's
  working directory (`run/`) while the reader looked in `run/world/`. `FrameLedger.file()` now asks
  `WorldState.directory()`, so the world owns its own path.
- **Every observation logged "was not written" after writing.** `pkg.get("digest").getAsString()` threw
  after `stream.observation(...)` had already committed the row, so the message was the opposite of the
  truth. The digest read is null-safe now; the warning is gone and the rows stayed.
- **Claim shapes never fired.** The parser counted matched cue words and discarded the lift the cue table
  carries, so every real sentence scored within one word of a rival and `claim_shape` was always empty.
  Scoring is lift-weighted (two cue words, ≥12 combined lift, ≥1.5× runner-up). A live wish now reads
  `WHEN cues:if,someone`; `I need wood when it is cold` still reads no shape, which is the point.
- **`if` filled the verb slot.** Subordinators and indefinite pronouns are skipped there; the same wish
  now reads `verb=comes`.
- **Client toasts showed `test / secure-shell-toast`.** The display drew its own `test` ledger rows on
  every catch-up, three at a time, plus a witness toast reading `exit=0 checks=6 sha256:05348dd…` on
  every join. Only refusals are drawn from the feed now; the witness is a toast only when the checks
  fail (it stays in the log and the ledger row); and a repeat of a notice already drawn is suppressed
  for two minutes. Verified on a live client: joining now queues exactly one `Refused: record - rest`
  toast where it used to queue seven toasts of which four were internal.
- **A being's persisted life still binds and acts.** Boot: `continuity Ash RECOVERING -> OPERATIONAL
  (interrupted wander at -9,72,-16)`; its networked session carried `agent:4491e473…`, the id that owns
  its homebase, and 26 `axis:observation` rows followed with zero `was not written` warnings.
- **A test asserted `smelt` was unattested.** Ingesting the mod trees attested it (smeltery prose), so the
  test's word moved to `flange`: the contract is "a word nothing attests is recorded", not that one
  particular word is missing.

## Not exercised / not built (named)

- `RECORD` has 0 cues and is unreachable (majority shape, so no word clears the lift filter).
- The baseline is still 8 verbs: shapes come from the claim store, and no tool compiles claims out of a
  design document. Ingested prose widens the lexicon, not the claim shapes.
- A wish's object runs past its clause (`object_kind="dusk wake others while sun is low"`): only `and`
  and `,` end it.
- `WorldState.accept` writes a log line for `APPLIED` only; `STALE`/`REPLAYED`/`WRONG_WORLD` leave no
  durable row, so R6's accept/refuse journal is a stub.
- `ensouled spawn Ash` minted `agent:ac5ce2b9…` beside the persisted `agent:4491e473…` that owns Ash's
  homebase. The networked session binds to the existing life; the spawn path does not.
- Slices 3–6 (objective windows, commitment/plan/run stages, nav, client package categories) are stubs.

---

# Execution checkpoint — the streamer: toast as the first window (2026-10-31 22:40)

## What landed

- [`docs/LAYER_TABLE.md`](docs/LAYER_TABLE.md): the ruling — the mod is a downstream client of the
  shell's package streamer, the game's own toast stack is the display owner, the server reads its own
  store for the civilians, and the refusals that keep each layer from becoming a second one.
- [`packages/toast/toast.surface`](packages/toast/toast.surface): `secure-shell`'s declaration copied
  byte for byte (`sha256:05348dd262121b4f86b95f36ca7868ca58bacb00d7d3b152a92de88dbb4ef07b`), pinned by
  [`toast.port.json`](packages/toast/toast.port.json) (one window, two nodes).
- `dev.ensouled.surface`: `SurfaceDocument` (closed-schema parser, shipped lane/rule/kind/role tables,
  `port-baseline`), `MountReceipt` (`surface.mount/2`, same eleven fields and `receipt-id` formula),
  `ServedPayload` (`secure-shell.served/1`; `tier-not-derived`, `served-no-windows`, `wish-*`),
  `StreamLedger` (`secure-shell.ledger/1`, SHA-256 chain, fsync before head), `MongoStreams`
  (per-civilian streams, write → read back → compare → `verified`), `SurfaceStream` (loopback HTTP +
  SSE, bearer token, per-agent feeds).
- `dev.ensouled.client.StreamClient`: reads its own agent's payload, checks it against the shipped
  tables, draws with the game's `ToastManager`, and runs the witness (command / exit / checks / digest)
  which is committed as a `test` row and comes back as a notice.
- `ControlRelay`: declared action set, ticket + named presenter, ledger row before the packet; the
  client refuses a packet with no ticket or presenter.
- `scripts/mongo.sh` (local MongoDB on 27018, dbpath `run/mongo`), `scripts/stream.sh` (operator probe).

## Proven live (server + headless clients on 25565)

```
[ensouled] surface mounted: surface=secure-shell-toast sequence=1 state=already-mounted windows=1 digest=sha256:05348dd2…
[ensouled] mount receipt sha256:05193de0… at 2026-11-01T04:25:45.884039200Z
[ensouled] stream store ensouled_streams at mongodb://127.0.0.1:27018 (per-agent isolation, read-back verified)
[ensouled] stream store replay: 18 row(s) verified, 0 refused
[ensouled] served stream on http://127.0.0.1:7664 (token file run/stream.token, ledger row 18)
```

- `scripts/stream.sh unauth` → `401`; `scripts/stream.sh served Bram` → `agent=Bram`, 1 wish
  (`package:packages/toast/toast.surface` `loaded`), 1 window, 2 nodes, 2 requirement rows; notices
  `[Refused: record, Refused: record, test]`. `served Resty` → `agent=Resty`, notices `[test]` only.
- Client logs: `witness command=GET /api/served?agent=Bram exit=0 checks=6 surface=secure-shell-toast
digest=sha256:05348dd2…`, then `toast drawn by the client HUD: Witness Bram - exit=0 checks=6 …`.
  Refusal rows drawn too: `toast drawn by the client HUD: Refused: record - control:forward`.
- Store: `events` per agent Bram 17 / Resty 1 / Ash 2, all `verified`, **0 rows whose actor differs
  from the stream's agent**; `verifications` 39 PASS / 0 FAIL.

## Found and fixed on the real path

- `ClientInputMotor.cancel()` released keys with no program open → three genuine `control-no-ticket`
  refusals per cancellation, drawn as toasts. `cancel()` now returns when no ticket was opened; no
  `control-no-ticket` row has been written since.
- The dev server runs from `run/`; the streamer resolves the project root by the declaration instead
  of trusting its working directory (first boot refused `@surface-missing`).

## Not exercised / not built (named)

- No committed `control:*` row: both beings were idle (`perceive: choices=0 objects=0`), so no motor
  program opened. The ticket path is enforced in code and unproven live.
- No observation surface. Perception is real and occlusion-aware server-side (`BlockSight` raycasts
  `COLLIDER` from the eye, radius ≤16), but `served/1` carries windows, wishes, notices and evidence —
  not what the being sees. `ControlRelay` is output-only. A CUA driver reading the stream cannot see.
- `WanderRoutine.destination()` is hand-rolled, not the v4 horizon plan: the direction is
  `(agentId + "#" + tick/200).hashCode()` over eight fixed offsets, and `view.canReach(destination)`
  is satisfied by *a* standable spot near the target, not by a walkable path. That is why a being can
  choose a wander target behind a wall and then report `wander failed: no_progress`. The smallest
  honest correction is a step-wise clear-path check on the straight line to the target (and, later,
  the campaign/horizon planner the architecture specifies); it is **not** applied in this pass.

---

# Execution checkpoint — §49.1–§49.3 durable continuity (2026-10-31 21:56)

## What landed

- [`MindJournal`](src/main/java/dev/ensouled/persist/MindJournal.java): append-only lifecycle chain
  at `run/world/ensouled/journal.log`, CRC32 per line, opened `rws`, truncated tail preserved as
  `journal.log.tail-*` and never parsed as a record. Transaction ids make GENESIS/BIND/SNAPSHOT
  idempotent.
- [`MindStorage`](src/main/java/dev/ensouled/persist/MindStorage.java): generation-based atomic
  snapshots with `.prev` fallback, `ABSENT`/`LOADED`/`CORRUPT`/`UNSUPPORTED` load outcomes, orphan
  temp quarantine, and a merge of on-disk entries so an unwritten life keeps its record.
- [`AgentContinuity`](src/main/java/dev/ensouled/persist/AgentContinuity.java): the §49.2 record —
  genesis, binding history, observed needs with provider, running configuration, last body/session,
  boots, and the domains it does not have (`unrecorded`).
- [`ContinuityState`](src/main/java/dev/ensouled/core/ContinuityState.java): `ABSENT`,
  `OPERATIONAL`, `RECOVERING`, `INTEGRITY_BLOCKED`.
- `AgentManager`: GENESIS written before a mind exists, BIND before a body acts, UNBIND on body loss,
  interrupted actions reconciled and journaled, canonical events trigger a snapshot, and no mind is
  minted while the store cannot be read.
- Commands: `/ensouled continuity [name]`, `/ensouled journal [count]`, `state=`/`gen=` on
  `/ensouled list`; `/ensouled save` reports the committed generation or refuses.

## Proven with the live server (commands and observed lines)

Baseline before the first kill: 2 lives, Bram harvests=35, Resty=3, generation 6, journal 11.

1. Hard kill mid-run: `taskkill //F //PID <server>`; no shutdown save (file mtime unchanged). Boot:
   `journal ... replayed 11 transition(s) up to #11` · `continuity store: outcome=LOADED generation=6
   ... snapshot=current minds=2` · `Resty ... state=RECOVERING ... interrupted wander at -145,77,8` ·
   `Bram ... state=OPERATIONAL`. Clients reconnected: `restored persisted mind ... (Resty)` then
   `continuity Resty RECOVERING -> OPERATIONAL (...)`; Bram harvests still 35.
2. Kill during a save: `python scripts/save_storm.py 25575 <password> 20` while
   `taskkill //F` landed at 1.6 s. Left `minds.dat.tmp` (57,751 B) with journal #75 at generation 69.
   Boot: `an uncommitted snapshot (minds.dat.tmp) was left by a failed save; kept as
   minds.dat.tmp.orphan-<ts>` · `continuity store: outcome=LOADED generation=69 ...` — a complete
   generation, never a torn file.
3. Corrupt current generation: 60 bytes of text into `minds.dat`. Boot: `minds.dat was CORRUPT
   (Not in GZIP format); recovered generation from minds.dat.prev` · `snapshot=previous` — both
   lives readable.
4. Both generations corrupt: `continuity store: outcome=CORRUPT ...` · both lives
   `state=INTEGRITY_BLOCKED` with names from the journal · `no new mind will be minted` ·
   `/ensouled spawn Probe` → `Refused to spawn a being: the continuity store is not writable` ·
   `/ensouled save` → `Snapshot refused` · `minds.dat` still 15 bytes (untouched).
5. Container format 99 (valid NBT, unknown version): `outcome=UNSUPPORTED ... minds.dat format 99,
   this build reads 2`, same `INTEGRITY_BLOCKED` refusals.
6. Restore `run/continuity-evidence/minds.good.dat` → `outcome=LOADED loaded_gen=70`, both lives
   `OPERATIONAL`, harvest counts 35 and 4.
7. The `ABSENT` condition on the real boot path: the store directory was moved aside
   (`run/continuity-evidence/store-before-absent`) and the server booted — `continuity store:
   outcome=ABSENT loaded_gen=0 ... snapshot=none minds=0 (no snapshot on disk)` ·
   `recovery complete: 0 life(s) known, 0 awaiting bodies, minting=true`. The store was moved back:
   `outcome=LOADED loaded_gen=75` and both clients reconciled to `OPERATIONAL`. With this, all four
   conditions have been observed at boot: `ABSENT`, `OPERATIONAL`, `RECOVERING`, `INTEGRITY_BLOCKED`.

## Honest limits

- §49.2 domains with no source yet are named absent, not fabricated: relationships, conversations,
  commitments/contracts, test participation, suspension/death.
- The journal covers lifecycle transitions and snapshot commits, not every episode; episodic history
  is durable through the snapshot generation (bounded at 256 episodes), so a crash can lose up to the
  ticks since the last save. Canonical events now force a save on change, which bounds that window.
- Windows cannot fsync a directory; durability rests on `force(true)` on the file plus an atomic
  rename, which is the strongest guarantee available here.
- The `.tmp` orphan and corrupt-store tests are deliberate injections on the real load path with
  backups under `run/continuity-evidence/`; they are not a fixture suite.

---

# Execution checkpoint — NET-01 / CHAT-01 (connected clients, ordinary chat)

## Current state (2026-10-31 21:05)

- Server: `scripts/devserver.sh`, world `run/world` (normal terrain, seed `ensouled-civilisation-1`;
  the old flat world is `run/world.flat-baseline`), 25565, 3 persisted minds restored at boot.
- Clients: `scripts/bots.sh start|stop|status` launches one isolated headless Fabric client per being
  from `tools/botprofile` (Java 25 for the HeadlessMC launcher, `-g "--username <name>"`, `-lwjgl`,
  `--server 127.0.0.1:25565`). Per-bot game dirs live in `run/bots/<name>` with their own quiet
  `options.txt`; mods are shared through a link, and `start` syncs the newest built jar first.
- Verified: `<Traveler> Ash, how are you doing out here?` → `<Ash> I have 2 oak logs and 20 food.` —
  the mind chose the claim, the prose-logic baseline classified it, and every word is one the
  121,501-claim corpus attests; the 2 logs are the ones it mined itself.
- The beings walk: a low-fatigue being selects `wander` (`walked 3 blocks to 17,75,20`), and rest is
  only on the table once `fatigue` passes its threshold.
- The beings have an inner life: they mutter to themselves between decisions (`<Ash> Well.`, `<Bram>
  I have 28 oak logs and 20 food.`), a near-tie is kept as a `THOUGHT` memory (`weighed rest and
  wander (70 apart)`), and a resting body recombines two memories as a `DREAM` episode.
- The client's own body acts: mining holds the client's attack key and aims at the block (`data get
  entity Ash Inventory` shows `oak_log x5`, `acacia_log x5`); dropping, eating, equip and use are
  bridged and verified on the server. Keys are re-applied every client tick because a headless client
  has no keyboard to hold them, and released when the program ends.
- Start order matters on this machine: server first, then `scripts/bots.sh start`; starting the server
  while three clients are already up exhausts native memory and `runServer` exits.
- Fake bodies: off unless `ENSOULED_FAKE_BODIES=1` (the CI probes set it in `scripts/selftest.sh`).

## Superseded notes below (kept for the earlier evidence trail)


## Objective
One real headless Fabric client connects to the persistent dev server (25565), binds to the existing
AgentMind without creating a duplicate, and sends a genuine MessageBus speech act as ordinary player
chat that a connected player reads.

## Proven live (server log, `logs/devserver.log`, commit-ready evidence)
- Mind continuity on the networked body: after `/ensouled join` the same record
  `agent:fbae946e-…` (Ash) keeps advancing — `decisions 1 → 12/13`, evidence `t=50292 → 56406`.
  The earlier "mind goes idle on the network body" symptom is gone.
- Agent egress end to end, on the rebuilt jar:
  - `18:16:45 speech -> client Ash: do you have 2 oak_log?` → `[Not Secure] <Ash> do you have 2 oak_log?`
  - `18:17:16 speech -> client Ash: no, stock_unavailable` → `[Not Secure] <Ash> no, stock_unavailable`
    (the supplier's typed decline over its own client).
  - No `[ensouled]` duplicate for either Ash line. Bram's lines still use the tagged diagnostic
    because Bram has no client (expected until every citizen has one).
- `./gradlew build` green (includes the offline test set).

## Fixed this cycle
- Speech routing now uses the mind's own body binding (`AgentRecord.body()`), not a display-name
  scan; `SpeechRelay.networkedClient` was deleted with it.
- `AgentManager.bindNetworkedPlayer` handles a reconnect: a session whose record already holds a
  stale networked adapter replaces it, instead of silently returning and leaving the mind detached.

## Unverified / blocked
- Server cannot complete world load with the current `run/mods` pack (45 jars). Bounded experiment
  `logs/packtest.sh`: same pack, fresh `run/universe.packtest` world, port 25566 → `verdict=NO-BOOT`
  after 300 s, Server thread parked in `prepareStartRegion → waitForTasks` (ModernFix
  `managedBlock`), c2me-sched and C2ME workers idle, log frozen (`logs/packtest-dump.txt`).
  Fresh world reproduces it, so `run/world` is innocent and the pack is the variable. Prime suspect
  is the pack's chunk-task pipeline (c2me async chunk request) under dev remapping; not verified by
  bisection, and the mods folders are the operator's to change.
- Reconnect re-bind: `bindNetworkedPlayer`’s new branch is still unexercised, because no server can
  stay up with the pack. The client profile itself now gets through headless init with the pack (no
  `OpenGL 3.3` failure at 18:31), so the blocker is server-side.
- Human chat ingress (`ServerMessageEvents.CHAT_MESSAGE` → mind) is still absent: the operator typed
  "no" / "I have none" and nothing consumed it; Ash re-asks every request expiry.

## Next executable operations (in order)
1. Get the pack to complete `prepareStartRegion` (operator's mod decision). One-line test that needs
   no file moves: set `optimizeAsyncChunkRequest = false` in `run/config/c2me.toml`, or start with
   c2me absent, and re-run `logs/packtest.sh`; a `PASS` names the culprit.
2. Replay the proven chat hop on the current pack once 25565 boots: `/ensouled join` from the client,
   `clear Ash minecraft:oak_log`, expect `speech -> client Ash` plus a bare `<Ash>` line.
3. Give a mind a client body without an in-process body (restore an unbound persisted mind by name in
   `bindNetworkedPlayer`), then remove fake-body materialization.
4. Human chat ingress, then bot-to-bot visible chat (CHAT-02).

## Runtime state at checkpoint
Dev server up on 25565 (`scripts/devserver.sh`, `JAVA_HOME` must be the JDK 21 path), world
`run/world`, citizens Bram/Cindy as fake-player bodies, Ash bound to a headless client that has since
disconnected. Control: `scripts/rcon.py`, `run/rcon.password`.

## 2026-11-01 00:27 — every join died one second after it started

Symptom: `client join error` on the Prism instance — a Netty decode failure one byte past the end of a
23,829-byte `CommandTreeS2CPacket` — and, in the server log, every session (human and headless) ending
`lost connection: Disconnected` about one second after `joined the game`: alacripose at 23:57:36,
23:58:51, 00:01:58, 00:14:30, and Ash at 00:01:25, 00:14:35.

Cause, from the client side rather than the server: `run/bots/Ash/logs/latest.log` carries
`Received unknown remote registry entries from server` / `Registry entry (create_new_age:geiger_counter)
is missing from local registry (minecraft:sound_event)` / `RemapException: Received a registry entry
that is unknown to this client`. The command-tree crash on the Prism client is the same failed login
failing one packet earlier, not a separate fault.

Mod sets, worlds and configs were ruled out by evidence, not assumption: `run/mods` and
`tools/botprofile/mods` held byte-identical jars (same md5 for create-new-age), the two worlds'
`data/fabricRegistry.dat` carry the same 1,016 bytes and neither mentions the entry, and
`create_new_age-common.toml` is identical on both sides. The difference is registration: the mod's
`CreateNewAge.initialize` calls `CNASounds.load()` inside a `catch (NoClassDefFoundError)`, so the sound
event exists on the side whose class loading gets that far and is silently absent on the side that
throws — under loader 0.20.1 it lands on the server only. The last session longer than a second
(22:53 → 23:15) ran loader 0.16.14, which is why this appeared with the loader upgrade.

Fix: `create-new-age-1.2.0+fabric-mc1.20.1.jar` moved to `mods/disabled/` in `run/mods`,
`tools/botprofile/mods` and the Prism instance (nothing hard-depends on it — checked every jar's
`fabric.mod.json`), server restarted through `scripts/devserver.sh`. Verified live: server up on **160**
mods at 00:24:26, alacripose joined 00:25:32 and Ash 00:26:35, **zero** `lost connection` lines since
the restart, ensouled surface mounted (`sequence=5 state=already-mounted`), stream on
`http://127.0.0.1:7664` with 193 ledger rows replayed. Documented in the README's loader notice.

Also fixed: `scripts/bots.sh stop` killed the recorded pid, which is the **HeadlessMC launcher**, not the
game JVM it forks — so "stopped" bots stayed connected and kept every jar in the shared mods directory
open (that is why the jar could not be moved and why a stale build could look current). It now kills the
process tree (`taskkill /T /F`).

## 2026-11-01 00:35 — the being got its brain back, and the write-ahead record is now proven by a real kill

`heard "hey" … by 0 mind(s) of 0` and `/ensouled list` → `No ensouled beings` were true, and the reason was
not a broken load: `continuity store: … minds=2` **did** load, but the two persisted lives in `run/world`
are **Bram and Resty** (names in `world/ensouled/journal.log`), and neither had a body. Ash's mind lived in
the scratch universe (`run/probe-universe`), which is why the same client that had a brain there joined
here as a bare `PARTICIPANT`.

`/ensouled spawnplayer Ash` then minted one — but onto a **fake body**, so the log said `speech
unavailable: Ash has no connected client`. The join path is what repairs that, and it needs the client to
*re-join*: `scripts/bots.sh stop/start Ash` produced

```
restored persisted mind agent:fa682737… (Ash) onto its client Ash
agent agent:fa682737… now runs as its network client Ash; fake body retired
```

and `/ensouled list` now reports `Ash … state=OPERATIONAL gen=126 kind=player body=live
current=intention[intent:29 wander@-31,62,98 COMMITTED]`.

**§49.3 is now demonstrated, not claimed.** Killing the server while Ash was acting left 55 `DECISION`
rows in `world/ensouled/journal.log`, each carrying the §47.2 envelope (`message_id`, `conversation_id`,
`sender`, `sender_name`, `recipients`, `channel=WORLD`, `scope=SELF`, `speech_act=COMMIT`,
`utterance=wander@x,y,z`, `content_hash`, tick/dimension/position) plus the axis reading
(`axis=PROCEDURAL_GATE`, `axis_token=procedural_gate+PASS`, `axis_proof=need, estimate, task, targets, live
plan, no budget declared`). On restart those decisions came back out of the store as `OUTCOME … status=recovered,
recovered_tick=139054` and were named on Ash's own stream (`recovered interrupted decision #188…#199`),
after which the being continued from `intent:29`. That is the kill/resume the mission asked for: written
ahead, survived `kill`, recovered, readable.

**Axis, as the record shows it:** the ladder reading enters as `PROCEDURAL_GATE` with a token and a named
proof string *inside the decision envelope*, i.e. it is recorded as the reason a candidate was admitted.
What is still unproven is the stronger claim — that a different axis reading changes *which* action is
chosen rather than merely annotating the one already picked. Nothing in this pass tested that.

**Process note (`scripts/bots.sh`):** `stop` now re-establishes ownership from the process command line
before signalling. A first attempt killed the recorded pid's process *tree*; that pid was stale and had
been reused, and it took the dev server down with it (recovered by restarting `scripts/devserver.sh`).
On Windows a recorded launcher pid must never be signalled without checking it is still ours.

## Pass: axis reading made load-bearing, lane made prose (§47.2) — 2026-11-01

**What changed.** `Arbiter` now sorts by the establishment rung before utility, and the rung is read
per candidate by `AgentMind.readCandidate(view, candidate)` from what the world shows now
(`AxisReadings`: `active` = the target is perceived, `stale` = memory says the site was already
taken, `unchecked` = nothing perceived, `refused` = the gate refused it). A first attempt derived the
rung from each skill's success count; on the live harness that made one successful `wander` outrank
every unevaluated piece of work forever, so it was replaced. `CandidateGenerator`'s restlessness pull
is capped at 120 so a reachable tree outbids one more leg of the same walk.

**Prose lane.** `core/MessageEnvelope` + `core/ProseLines` (verbs from `prose_baseline.tsv`: RECORD,
EXECUTE, FORBID, …) are the only way a being's lane composes a sentence. Observations, decisions, the
write-ahead record and stream refusals all travel as that envelope: `utterance` is prose, and every
count, coordinate, ladder token and proof lives in typed `payload`. `SurfaceStream` refuses a row
whose utterance does not classify (`NotProse`).

**Evidence, from the running server (new build, port 25565, 2026-11-01T07:04Z).**
- `run/stream/ledger.jsonl`: 268 rows with an utterance, 0 non-prose. Samples:
  `Ash: RECORD [Ash can see 37 blocks, 1 being and 0 items within reach]`,
  `Ash: EXECUTE [Ash begins wander at 6,64,7.]`.
- `run/selftest-universe/world/ensouled/journal.log` (write-ahead, before the action):
  `utterance=Ash: EXECUTE [Ash begins wander at 6,64,7.]`,  `axis_token=procedural_gate PASS`, `establishment=establishment active`, with the typed payload and
  the ticking proof beside it.
  The same file from the pre-fix run still shows `utterance=wander@8,82,-2` — the hand-rolled token
  that used to arrive where the sentence belongs.

**Tests.** 21/21 green (`AxisSelectionTest` carries the counterfactual: same candidates and
utilities, two readings, `gather_wood` then `rest`). `RestDecisionTest`/`HandoffProtocolTest` were
updated where they encoded pre-wander and pre-murmur behaviour, and they now install a real
`MindJournal` because a decision cannot start without a durable write-ahead row.

**Observed live and NOT fixed.** `scripts/selftest.sh handoff` (disposable universe, port 25579):
`requests=1 accepts=1 supplier_transfers=0` — the ask and the acceptance go through, but the supplier
never performs the drop, and both beings fall back to wandering. This is follow-through in the
give-food routine, not the message lane. Also not shown: thin headless client vs full client parity
for the served package (no client joined this run).

## CHAT-02 pass (2026-11-01, this session)

**Implemented, in the production paths.**
- `core/ChatTranscript.java`: the transcript and its delivery trace (§47.4/§47.5). One id per line,
  one append-only JSONL row per recipient decision — `delivered`, `self_excluded` (the echo guard),
  `out_of_earshot`, `no_mind_bound` — each carrying `transport=PLAYER_CHAT_PACKET`. Written under
  `run/world/ensouled/chat/transcript.jsonl`, opened at server start so the record is world-scoped.
- `mind/AgentManager.hear`: mints the id before any delivery decision and writes every row; the
  self-exclusion is now on the record instead of only implied by the loop guard.
- Operator surface (§47.4): `/ensouled chat history [<who>] [<limit>]`, `/ensouled chat trace <id>`,
  `/ensouled chat visibility <all|off>`. Visibility governs the tagged fallback mirror only — never
  real player chat, never delivery.
- Wish lane (§47.4 boundary, per the operator's rule): there is no command that speaks for a being.
  `/ensouled wish <agent> <text>` puts a wish to it as a toast on its own stream (`Wishes`,
  `StreamNotices.noted`); `AgentMind.considerWishes` answers with its own decision and refuses
  dictation outright (`wish-dictation`: "words are mine to choose; a wish may ask for a thing, not
  for a sentence").
- `client/EnsouledClient`: the speech receiver now says whether it sent the line or dropped it for
  want of a live session (`speech sent from this client` / `speech dropped`).
- `scripts/client.sh`, `scripts/bots.sh`: a locked old jar no longer disappears into `2>/dev/null`.
  Two ensouled jars in one mods directory is the client drift; the sync now names the file and fails.

**Verified live (RCON, current server).** `chat history` reads the transcript file and its count;
`chat visibility nearby` is refused by name; `chat visibility all|off` takes; `wish` on a missing
being is refused. Earlier in this pass, on the previous world, ordinary player chat was recorded per
recipient with its delivery trace (human lines `hello`, `resty is dead`, one delivered row each).

**Not demonstrated: CHAT-02's actual acceptance.** No two bots have exchanged ordinary chat.
`tools/botprofile/mods` had been emptied and the Prism instance is mid-cleanup: it still holds
`create_connected`, `create_enchantment_industry`, `vs_eureka` and others whose hard dependencies
(`create`, `valkyrienskies`) are no longer present, so every bot client dies at Fabric load with
`Incompatible mods found!` before it ever reaches the server. The two-being exchange, its echo guard
across two real sessions, and the ask→accept→drop prose on the live server are therefore still
unshown.

**Environment notes.** The dev server now starts detached (`scripts/devserver-bg.sh`) so a tool shell
being stopped no longer takes it down; the world in use is the operator's fresh one, and the previous
world is parked at `run/world-modded-do-not-change-again`. Two full modpack clients plus the 6 GB
Prism client plus this server exceed 16 GB, which is why bot clients time out under load.

## 2026-11-01 — a mind on a human, wood counted by kind, and the frame parser

**The complaint: "it's controlling me."** The journal held the cause in one row:
`GENESIS agent:42c1ea43-5bd8-3633-a2a6-96b7b7097eaa name=alacripose ... profile=42c1ea43-...`.
A bootstrap minted a mind onto the operator's own UUID and stored it, so the restore path rebound it
on every join. Fixed in two places: `AgentManager.bindNetworkedPlayer` now refuses any session whose
name is not on the corpus roster *before* restoring as well as before minting, and the poisoned
record was quarantined to `run/world/ensouled/quarantine-20261101-012437/`. Evidence in
`logs/devserver.log`:
`[01:29:23] session alacripose is a person, not a resident: no mind restores, mints or binds`
and again at `[01:36:08]`, after the fix.

**"I need wood" while holding wood.** Two `countItem("minecraft:oak_log")` calls — in `murmur` and in
the speech observation — meant planks, oak wood or another species counted as zero, and the shortage
line was reachable with a full inventory. New `core/Supplies` counts wood by kind (log/wood/stem/
hyphae/planks, any species). The shortage arm is unreachable while wood is held, and an identical
repeated murmur is suppressed. Old behaviour is on record in the transcript:
`"text":"I have 0 oak logs and 20 food."`.

**Names come from the prose corpus.** `core/ResidentNames` puts each candidate to `Lexicon` and keeps
the corpus's answer with its count. `ensouled residents` prints `Ash <- ash (6)`, `Rest <- rest (461)`,
`Bramble <- bramble (5)`, `Reed <- reed (34)`, `Thorn <- thorn (25)`. `scripts/bots.sh` defaults to
`Ash Rest Bramble`; the old `Bram`/`Resty` could never have become residents under the gate.

**Verb classifier v1, slice 1 landed.** `core/VerbFrame` + `core/FrameParser` parse an utterance into
actor, verb, state, object, count, recipient, modality, claim shape, residue, span and evidence hash.
The sentence-to-claim-shape mapping is compiled, not hand-written: `tools/prose_compile.py` now emits
`prose_cues.tsv` from the corpus's own quotes. `FrameParserTest` (5 tests, passing) runs on lines the
world actually carried. `tools/prose_ingest_mods.py` reads mod trees for vocabulary — lang values,
Java comments and markdown only, never code — and validated at 5,057 words on Create; the full
47-tree pass and the `--extra` merge are still running.

Plan and decisions: `docs/VERB_CLASSIFIER_V1_IMPLEMENTATION_PLAN.md`.

**Left open:** the store still holds non-roster demo minds (`ham`, `jeff`, `frederick`) from the
pre-fix build; the owner has not authorized a purge, so they stay and the roster gate is the only
thing that keeps them from becoming bodies. `tools/mod_lexicon.tsv` was still being written when
this pass ended.

## 2026-11-01 — the long-horizon spine, and two defects that were hiding it

**The spine now has one owner.** `work/WorkLedger` holds a being's commitments, the plans that serve
them, the objective windows they run inside, and the run stages they advance. It is outside
`core/AgentMind` (which decides one action at a time) and `mind/AgentManager` (which owns lives and
bodies): the mind reports perception in through `observe`, reads what a step is worth through `pull`,
and hands a wish to `offer`. State is `run/world/ensouled/work.json`; every change is a `WORK` row in
the world's journal before it can be overwritten by the next tick.

**What the world's own record shows for a real being (Ash, `agent:4491e473…`, live):**

```
work-opened      commit:ec586b2e  count=6  run=run:5219ccc1  due=168120
run-survey       stage=SURVEY     condition=logs in sight      held=5
run-claim        stage=CLAIM      condition=held 5/6           held=5
window-met       condition=logs held >= 6                     held=6
work-satisfied   count=6          condition=logs held >= 6     held=6
```

`run/world/ensouled/domain.json` now reads
`run:5219ccc1 … "purpose": "bring me 6 oak logs", "stage": "ESTABLISH"`. The sixth log was harvested by
the resident between CLAIM and satisfaction; no step of that sequence was commanded. An earlier
commitment (`4bbf40e1`, 8 logs) shows the disposable half: `plan-replaced … step=wander … nothing in
sight` → `plan-replaced … step=harvest_tree … in sight again` → `window-expired … window closed`,
one continuous debt across three plan states.

**Ambiguity is clarified, not guessed.** `bring me some oak wood` → `work-unclear` + the being asked
`How many logs?` through its own client. `bring me four logs` reaches the same question (number words
are not read — named in `docs/LANGUAGE_AUDIT.md`). `bring me 3 moonrocks` names nothing and gets the
`What do you want me to bring?` question instead. Dictation is refused before work is reached.

**Run stages no longer skip.** `advanceStage` used to take the highest condition that held, so a being
that already carried one log went straight from CONSIDERED to CLAIM and SURVEY never had a row —
`Run.Stage` advanced over a stage the record never showed. It now advances exactly one stage, and only
the one after the last reached. Pinned by `WorkLedgerTest.aRunReachesOneStageAtATimeSoNoneIsSkipped`.

**Defect 1: a resident's mind froze while its session stayed online.** `AgentRecord.tick` refuses to
act through a dead body, correctly — but it also *dropped* the body, and only a JOIN ever bound one
again. After the previous restart, `ensouled list` read `body=unloaded` and `ensouled inspect ash` sat
at `tick=132639` while `time query gametime` returned `148847`. Body lifetime is now resolved from the
server's own player list every tick (`AgentManager.resolveClientBodies`), and `AgentRecord.attach`
compares the entity rather than its recycled id, so a reconnect replaces a retired session instead of
being ignored. Same resident, after the fix: `body=live`, tick advancing, intention executing.

**Defect 2: the server ran a jar nobody had built.** `run/mods` was cleaned with `ensouled-*.jar`, so a
plain `ensouled.jar` from an earlier hand-install survived it and Fabric loaded that:
`common init build=0.1.0+src.c9a8451b` while `build/libs` held `…bdf56b73`. All three launch scripts now
clean `ensouled*.jar` and refuse to launch when a stale jar cannot be removed; `devserver.sh` prints the
build it synced, and it reads `common init build=0.1.0+src.0994b069` for this pass.

**Language audit** (`docs/LANGUAGE_AUDIT.md`, extended): every producer of a being's *speech* composes
through `Realizer`/`Talk` over the corpus, and the tables of English a being *reads* (deliverable nouns,
question openers, the dictation pattern, number words) are now listed in one place with what each one
costs when a phrasing is missing.

**Left running:** dev server on 25565 (control 127.0.0.1:25575) and headless resident `Ash`
(`logs/bots/Ash.log`). **Still open:** a run interrupted mid-step and resumed without repeating the
completed step (the disconnect clause of slice 4); `ObjectiveDelta` pressure rows (R7's other half);
`RECORD` still unreachable; slice 5–6 stubs.

## 2026-11-01 — interrupted mid-step, resumed, and finished (the idempotency proof)

**The experiment, on the running server.** `ensouled wish Ash bring me 16 oak logs` opened
`commit:5a3133dc` with run `run:f0a78ad1` while the resident carried 13 logs. Its own perception took the
run through SURVEY (`logs in sight`, held 13) and CLAIM (`held 13/16`), and it began harvesting. With
`harvest_tree` **executing** (`intent:14849`, at `-106,89,-35`) the server JVM was killed with
`taskkill /F` — no shutdown hook, the same kill as a crash. Captured at that moment in
`logs/work-at-kill.json`: `state=OPEN, stage_reached=2, count=16, opened_tick=198912, run=f0a78ad1`.

**The resume, from the world's own record.** Server restarted on the same build, client rejoined, and
the journal closes with

```
work-opened    commit:5a3133dc  count=16  run:run:f0a78ad1  due=204912
run-survey     held=13  condition=logs in sight
run-claim      held=13  condition=held 13/16
-- killed here, mid-harvest --
window-met     held=16  condition=logs held >= 16
work-satisfied held=16  count=16
```

`domain.json`: `run:f0a78ad1 … "stage": "ESTABLISH"`. **One** `work-opened` row exists for that
commitment — the resume did not mint a second one, and the debt kept its id, its run and its opened
tick. The inventory carried 13 logs across the crash and the resident gathered exactly the three it
still owed (`harvests` 4 → 5, one column), so nothing was spent twice and nothing already earned was
lost. The step is not re-issued on resume because its condition is read from the body's inventory, not
from a step counter — that is what makes the resume idempotent rather than replayable.

**A defect the experiment exposed, fixed and re-proven.** The journal writes a DECISION before the
action and an OUTCOME when it ends, but `completed` and `failed` cleared the mind's current intention
without writing one. Of 3,937 decisions on disk, 1,286 had an outcome and every one of those was a boot
"recovery": each restart announced hundreds of *finished* wander steps as interrupted, and the decision
the crash actually caught was buried among them. Both endings now close their row
(`AgentMind.completed`, `AgentMind.failed`), and recovery announces only the newest pending decision —
a process can only have been inside one — closing older rows silently as `superseded`.
Observed after the fix: 7 new decisions, 6 outcomes (`completed` ×5, `failed` ×1, one still in flight),
and the next boot announced exactly one recovery: `Ash begins harvest_tree at -106,89,-35`.

**Also found, left as it is:** the resident can be refused its own work by the authority gate —
`refuse outside_home_radius: target 71 blocks from home anchor` — and then sits idle while the
commitment runs down its window. Not changed (the gate is right to refuse); recorded because it is why
an earlier interrupt attempt never reached a harvest.

**Left running:** dev server on 25565 (control 127.0.0.1:25575, build `0.1.0+src.243536ef`) and headless
resident `Ash`. **Still open:** `ObjectiveDelta` pressure rows (R7's other half); `RECORD` unreachable;
slices 5–6 stubs; the `superseded` close path has no automated test (it is exercised only by a boot).

---

# Maintainer-clarity pass — the mind's off-path, the wish reading, and the decision's lifetime (2026-11-01 04:45)

No behavior change intended, and none observed: same rows, same refusals, same prose.

## Structure after this pass

- **`core/AgentMind` decides one action at a time and nothing else.** `tick` reads as the chain it
  is: recover → sense → hear → work.observe → affect → reflex → inner life → advance the current
  decision. The off-path (murmuring, dreaming, the contest cell) is `core/InnerLife`, which was
  already owner of it and still is.
- **A wish is read once, before anything is done about it.** `work/WishReading` owns the three
  readings English allows — dictation (refused), a question about the being's work (answered from
  what it owes), anything else (offered to the ledger as a wish naming a thing). The mind branches
  on the reading and never re-parses the sentence; whether the words name a thing and a number stays
  `Deliverable`'s business inside `WorkLedger.offer`, in one place rather than two.
- **`AgentMind.release()` is the one way a decision ends.** Seven call sites set `current`,
  `activeRoutine` and `currentEnvelope` to null as a group; that triple was the same policy written
  seven times and is now one method.
- **`AgentManager.speech()`/`announce`** no longer route the §47.4 visibility rule through a
  one-parameter helper that ignored its parameter; the rule is the check, at the check.

## Proven on the running system, after a restart on this build

Dev server restarted on `0.1.0+src.3065170f`, resident `Ash` reconnected, and the same live
sequence the earlier passes established — with the changed code in the path:

```
wish:1  "bring me 3 oak logs"        → work-already-held   held=16 count=3
wish:2  "bring me 40 oak logs"       → work-opened  commit:f6437013 run:run:cf6b1702
                                     → spoke "I shall have 40 logs."   (corpus-composed)
wish:3  "what is the plan"           → answered from the open commitment: "I shall have 40 logs."
wish:5  "bring me plenty of wood"    → "How many logs?"   (addendum E.4: the missing half asked for)
run:cf6b1702  plan-replaced wander → harvest_tree (in sight again)
              run-survey  held=16  condition=logs in sight
              run-claim   held=16  condition=held 16/40
```

The reading itself, driven headlessly against the built classes (`WishReading.of`):
`say hello to everyone` → DICTATION, `tell me where you are` → DICTATION, `what is the plan` →
QUESTION_ABOUT_WORK, `where did you go?` → QUESTION_ABOUT_WORK, `bring me 40 oak logs` → OTHER,
`bring me some wood` → OTHER.

`./gradlew test`: 6 test classes, all green; `./gradlew build` green.

**Left running:** dev server on 25565 (control 127.0.0.1:25575, build `0.1.0+src.3065170f`) and
headless resident `Ash`. **Still open:** the large classes this pass did not split —
`AgentMind` (1.1k lines) still owns gate/decision bookkeeping and the speech response path beside
`InnerLife`; `AgentManager` (1.0k lines) still owns binding, recovery, persistence and speech
routing in one class. `ObjectiveDelta` pressure rows; `RECORD` unreachable; slices 5–6 stubs.

---

# Structure pass — one owner per responsibility (2026-11-01 04:55)

No behavior change: the build and the full test suite pass untouched, and the same live sequence
writes the same rows.

## Structure after this pass

| Class | Lines | The one question it answers |
|---|---|---|
| `core/AgentMind` | 1148 → 882 | what does this being do next, and did it work? (tick chain: perceive → select → gate → act → verify) |
| `core/Voice` | new, 102 | what does it say, and what may it say? (corpus-gated answer, one line per breath) |
| `core/DecisionJournal` | new, 164 | is the decision in flight durable, closed, and recovered if the process died? |
| `work/WishDesk` | new, 113 | what did this wish become — work, a question answered from the work, or a refusal? |
| `core/InnerLife` | 116 | what does it murmur and dream when nothing is at stake? |
| `mind/AgentManager` | 1038 → 690 | which body carries which mind, and what does each mind do this tick? |
| `mind/SpeechRoutes` | new, 301 | where do words meet the server? (bus, chat in and out, entity → mind) |
| `mind/ContinuityLedger` | new, 252 | what survives a restart? (journal, snapshots, continuity, waiting lives) |

Each collaborator is constructed with the narrow things it needs and is called from the tick chain
with this tick's occasion; no collaborator reaches back into the mind.

## Proven live after the split (dev server restarted on `0.1.0+src.a03f99b6`)

Same rows as before the split, with the residents' own client:

```
recovery complete: 9 life(s) known, 9 awaiting bodies, minting=true
wish:1 "bring me 30 oak logs"      → work-opened commit:0df731ed run:run:5dd2a2d5
                                    → "I shall have 30 logs."       (corpus-composed)
wish:2 "what is the plan now"      → answered from the open commitment: "I shall have 30 logs."
wish:3 "bring me lots of wood"     → work-unclear → "How many logs?"   (addendum E.4)
wish:4 "say something clever"      → refused; the only line after it is a murmur ("Well.")
plan-replaced wander (nothing in sight)   ← the work ledger still drives from perception
journal: DECISION … OUTCOME pairs written by DecisionJournal after the restart (seq 7196-7201)
```

`./gradlew test`: green, unchanged. `./gradlew build`: green.

**Left running:** dev server on 25565 (control 127.0.0.1:25575) and headless resident `Ash`.
**Still open:** `ObjectiveDelta` pressure rows; `RECORD` unreachable; slices 5–6 stubs; `AgentMind`
still carries the gate and axis bookkeeping beside the tick chain (882 lines), and `AgentManager`
still carries binding beside the tick (690 lines) — smaller, not yet one question each.

# V1 Dynamic Verb Classifier and Long-Horizon Work — plan v2 (model first)

Status: rewritten 2026-11-01 after the v9 audit. Design settled with the project owner; slice 0 not yet
built. Supersedes plan v1, which grounded long-horizon work in a single, unsourced "goal/route/plan"
lifetime and asserted v9 support it did not have.

**Provenance rule for this document.** Every long-horizon requirement below names the section of
`docs/persistent_city_builder_tower_defense_design_analysis_v9.md` it comes from and the type that
carries it. Where v9 is silent, the plan says so and names its real source. Nothing here is claimed as
settled on v9's authority that v9 does not say.

---

## 1. What v9 does not say

Recorded first, because v1's central error was presenting project doctrine as v9's findings.

- **v9 says nothing about verbs, claim shapes, a lexicon, speech, or sentence parsing.** Its planning
  vocabulary is objectives, waves, runs, commands and events. The classifier is this project's work;
  its sources are `agent_god_files/prose_logic` (for claim shapes) and
  `design_reference/docs/GRILLING-C11.md` Q292/Q495/Q496 (for deriving a verb library at a capability
  boundary rather than enumerating one).
- **v9 says nothing about residents having minds, memories, or names.** The Resident/Mind/Body split and
  corpus-derived naming are this project's ([CONTEXT.md](../CONTEXT.md)).
- **v9 says nothing about bot clients, headless sessions, or how a client joins.** Its client rules are
  the package-streamer boundary (§A6.1–§A6.6), which the project must adapt, not invent around.

---

## 2. Domain model — the types, their owner, their lifetime

Lifetimes are v9 §A7 (World → Homebase → Run → Encounter). Persistence follows §A5.2's list; "derived"
means rebuildable and therefore not serialized.

| Type | One owner | Lifetime (§A7) | Persisted (§A5.2) | Carries, from v9 |
|---|---|---|---|---|
| `World` | `core/WorldIdentity` **(new)** | World | yes: id, seed, schema_version, tick, revision | §A5.1 world record, §A5.2 |
| `Homebase` | `homebase/Homebase` **(new)** | World | yes: claim, permissions, upkeep, inventory refs, topology revision | §A4.2, §A4.4 `HomebaseRecord` |
| `Blueprint` | `homebase/Blueprint` **(new)** | World | yes, and **separate from any instance** | §A4.3 |
| `HomebaseRegistry` | `homebase/HomebaseRegistry` **(new)** | World | yes | §A4.4 |
| `Run` | `run/Run` **(new)** | World → bounded | yes: stage, objective, route, carried supplies | §A4.1 nine stages, §A5.2 "active expedition/run state" |
| `Encounter` | `run/Encounter` **(new)** | Encounter | **no** — reconstructable | §A7, §A5.2 transient clause |
| `ObjectiveWindow` | `core/ObjectiveWindow` **(new)** | Run / Encounter | yes: opens_at, closes_at, condition | §A3.2 pressure windows, §A6.2 `ObjectiveDelta` |
| `Commitment` | `core/Commitment` **(new)** | Resident, inside a World | yes | §A5.2 "objectives"; project vocabulary |
| `Plan` | `core/Plan` **(new)** | Commitment | yes, but a checkpoint is **not** a transaction | §A6.4 (see below) |
| `Command` | `core/Command` **(new)** | Encounter | journaled event, not stored as state | §A6.4 `expected_world_revision` + `idempotency_key`, §A6.5 |
| `Frame` | [core/VerbFrame](../src/main/java/dev/ensouled/core/VerbFrame.java) **(exists)** | Encounter | recorded as evidence, not authoritative state | project vocabulary; §A6.2 `EventFeed` is the transport analogue |
| `VerbLibrary` | `core/VerbLibrary` **(new)** | Resident | **derived** from the lexicon | not v9; GRILLING-C11 Q292/Q495/Q496 |
| `Route` | `nav/Route` + `nav/NavGraph` **(new)** | Body / Encounter | **derived**; invalidated by topology revision | §7.4, §2.1B, §A6.3 |
| `Resident` | [mind/AgentRecord](../src/main/java/dev/ensouled/mind/AgentRecord.java) **(exists)** | World | yes | §A5.2 "residents" |
| `Client` | [participant/ParticipantRegistry](../src/main/java/dev/ensouled/participant/ParticipantRegistry.java) **(exists)** | World (session) | session id only | §A6.2, §A6.3 |

**As built (2026-11-01).** The four lifetime types live in `domain/`, not in the `homebase/*`, `run/*`,
`core/*` packages this table proposed. One class owns a world's durable facts — `domain/WorldState` —
with `domain/WorldIdentity`, `domain/Homebase` and `domain/Run` as the records it owns, and one file
(`world/ensouled/domain.json`) holding all of it. Splitting the owner three ways would have created
three writers for one set of facts, which is what §A5.2 forbids. `domain/Encounter` is owned by
`core/FrameLedger` and is never serialised. `domain/Command` is minted by the command surface
(`/ensouled wish`) and accepted by `WorldState.accept`.

Not built, and not claimed: `Blueprint`, `HomebaseRegistry`, `ObjectiveWindow`, `Commitment`, `Plan`,
`VerbLibrary`, `Route`/`NavGraph`. Slice 3 onward is their work.

Two consequences of the table, both of which v1 got wrong:

1. **A Plan is not a transaction.** §A6.4 requires a Command to carry the revision it expects and a key
   that makes a repeat a no-op; a resumed Plan therefore cannot be trusted to avoid double-spending.
   See [ADR-0001](adr/0001-nested-lifetimes-and-idempotent-commands.md).
2. **Homebase and Run are addressed by identity, not by coordinates.** A plan step that carries a
   position is wrong the moment the Run is resumed somewhere else.

---

## 3. Long-horizon requirements, their carriers, and their evidence

Each row is a requirement v9 states, the type that carries it, and what would show it working. The
evidence column follows §14's rule that an experiment has a hypothesis, a success criterion, and a
failure condition.

| # | Requirement (v9) | Carried by | Success | Failure condition |
|---|---|---|---|---|
| R1 | Four nested lifetimes (§A7) | `World`, `Homebase`, `Run`, `Encounter` | each object has an id that survives restart and a stated owner of its state | two types storing the same fact; a Run ending a World |
| R2 | Nine-stage founding run (§A4.1) | `Run.stage`, advanced by `work/WorkLedger` | every stage reaches a recorded transition with its own evidence row (**CONSIDERED→SURVEY→CLAIM→ESTABLISH now shown live, in order**) | a stage is skipped or a run completes without `commit` |
| R3 | Claim, permissions, upkeep (§A4.2) | `Homebase` | a build refused for permission or upkeep states which rule refused it | refusal with no reason |
| R4 | Instance vs blueprint (§A4.3) | `Homebase`, `Blueprint` | editing one instance leaves the blueprint and other instances unchanged | a blueprint write mutates an instance |
| R5 | Persistence list (§A5.2), snapshot + journal (§A5.3) | World-level snapshot + journal | reopen at multiple boundaries: no duplicated reward, no lost event (also §A8 "Persistent resume") | replay diverges from the pre-close state |
| R6 | Commands with revision + idempotency key (§A6.4), consistent saves (§A6.5) | `Command` | a duplicated command after a dropped acknowledgement changes the world once | a retry double-spends or double-founds (§A6.4 verbatim) |
| R7 | Pressure windows and `ObjectiveDelta` (§A3.2, §A6.2, §A9) | `ObjectiveWindow` (**deadline half only**) | a window opens, is acted on, and the conversion is visible in the event feed (**open/met/expired rows shown live**) | a window expires with no record, or conversion is asserted without an event |
| R8 | Nav graph per map, recompute on topology change, shared caches, preview uses the same rules (§7.4, §2.1B) | `Route`, `NavGraph` | a blockage triggers exactly one recompute and the preview matches the walked route | per-tick recomputation, or a preview that disagrees with the sim |
| R9 | Path budget / anti-blocking (§2.1, §2.1B) | `NavGraph` validation | a placement that makes a goal unreachable is refused before commit | unreachable map accepted |
| R10 | Server authority: client previews, server commits (§A6.3) | `Command` validation | a foreign client's preview differs and is corrected, not obeyed | client-side state accepted as truth |
| R11 | Fidelity tiers; dormant regions keep outcomes (§A5.5, §6.4) | `World` region state | a distant resident's outcome survives without entity simulation | per-being simulation at full fidelity for every region |
| R12 | Pause-on-unload; no offline punishment by default (§A5.4) | `World.clock` | state before unload equals state after resume | a plan advances while the world is unloaded |
| R13 | Exact fault reason and causal history (§11.5) | decision + evidence rows | "why did this stop?" is answerable from rows alone | a stop with no upstream cause recorded |

---

## 4. Open question carried from v9

**Automation shape (§13 Automation).** v9 asks whether automation is configured "through simple rules,
node graphs, or a scripting language", whether it may issue construction commands, how a player
inspects why a script decided, and what permissions protect reserved resources. **This plan does not
answer it.** v1 silently chose Frames-as-automation; that claim is withdrawn.

What would close it: §14 experiments 8 and 11 — automate one mature production line and make a player
explain why the controller stopped it, plus the fault-dashboard prototype where starvation, blockage,
disconnection and overload must be identified without external help. Until then, Frames are a *parse*
of intent, and no automation surface is designed on top of them.

---

## 5. Where the classifier sits

The classifier's job is exactly one thing: turn an utterance into a `Frame` — who meant what, on which
thing, how many, to whom, in which claim shape — and record what it could not place. It is a producer
of Frames. It never produces Commands, never decides, never acts.

Its place in the model: `Frame` is Encounter-lifetime evidence (row 11 of §2). A Resident's decision
consumes Frames and produces a `Commitment`; a `Plan` decomposes the Commitment into steps; each step
becomes a `Command` with the revision and key that make a retry safe; a `Run` records which stage those
Commands advanced.

The verb library is derived per Resident from the lexicon and the environment at the capability
boundary (GRILLING-C11 Q292/Q495/Q496 — not v9). A verb the environment cannot serve is still nameable
and may be wished about; the plan's earlier wording that called this "idealized" is dropped, and the
frame states `attested` / `named` / `unknown` instead.

---

## 6. Frame schema (as implemented, plus what this revision adds)

Implemented in [VerbFrame](../src/main/java/dev/ensouled/core/VerbFrame.java): schema, frame_id,
tick, source, actor, verb, verb_state, object_kind, object_count, recipient, modality, claim_shape,
residue, utterance, evidence hash, span.

Implemented in [FrameLedger](../src/main/java/dev/ensouled/core/FrameLedger.java), which is where a
frame acquires its lifetime: `world_id`, `encounter_id`, plus `shape_basis` (the cue words that
carried the shape, or `none`) and `verb_basis` (`baseline`, `lexicon:<count>`, `unattested`). They sit
on the row rather than in the record because the record does not know the world it was read in.

Still absent: `run_id` — a frame is not yet attributable to a Run, because nothing opens a Run and
speaks inside it. A Frame carries **no** revision or idempotency key; those belong to the Command it
may become (§A6.4). `low_confidence`, which v1's schema named, is withdrawn: it was never
implemented and residue length is not a confidence measure.

Two fields are carried by the ledger and not by the record above, which is a real seam: a reader who
opens `VerbFrame.json()` alone sees no `world_id`. It is one row today and two writers tomorrow, so
the record should own them before slice 3.

---

## 7. Slices

Order follows the model: types first, then the parser into a live path, then the guarantees v9 makes.

### Slice 0 — domain vocabulary in code (**landed**)
- `World`, `Homebase`, `Blueprint`, `HomebaseRegistry`, `Run`, `Encounter`, `ObjectiveWindow`,
  `Commitment`, `Plan`, `Command` as named types with ids and a single owner as in §2. No behaviour.
- One `docs/PERSISTENCE.md` manifest stating which of them are authoritative state and which are
  derived (§A5.2), so a future reader cannot persist the wrong one.
- **Pass/fail:** R1 — every type has an id that survives a restart, and exactly one class owns its
  state.

### Slice 1 — the parser into a live path (**landed**)
- Close the two defects the audit found: `RECORD` is unreachable (`grep -c '^RECORD' prose_cues.tsv` →
  0, because the majority shape can never exceed the 2.5 lift filter), and the parse drops the
  sentence's subject silently.
- Call `FrameParser` from the speech and hearing paths and record `ensouled.frame/1` rows alongside the
  chat transcript.
- **Pass/fail:** a real chat line and a real being utterance each produce a frame row with a shape,
  and `RECORD` is reachable. Failure: a frame row whose claim shape contradicts `Talk`'s own choice for
  the same sentence.

### Slice 2 — Command envelope (**envelope landed; accept/refuse journal rows still stub**)
- `Command` with `expected_world_revision` and `idempotency_key`; journal rows for accept/refuse.
- **Pass/fail:** §A8 "Stream reconnect" and "Persistent resume" — duplicated command after a dropped
  acknowledgement changes the world once.

### Slice 3 — Objective windows (R7) — **landed on the live path, with one gap**
- `ObjectiveWindow` opened with the commitment, in `work/WorkLedger`, and closed by two different rows:
  `window-met` (the deliverable is in hand) and `window-expired` (the tick passed without it). Conversion
  is recorded apart from the window's own state, and an expiry always writes its own row.
- **Pass/fail:** met on the live server (commitment `ec586b2e`, `window-met`, `logs held >= 6`); expired
  on the live server (commitment `106e562d`, `window-expired`, `held=5`, `condition=window closed`).
- **Gap:** no `ObjectiveDelta` *pressure* row. A window here is a deadline for work the being took on,
  not a pressure window the world opens on it (§A3.2). R7 as v9 states it is therefore half carried.

### Slice 4 — Commitment, Plan, Run (R2, R12) — **landed on the live path, one clause open**
- `Commitment` outlives `Plan`; a step that stops being workable replaces the Plan and records why
  (§11.5 causal row), leaving the debt untouched. Run stages from §A4.1, **one stage per transition**:
  a stage whose condition already holds no longer carries the run past the stage before it.
- All four lifetimes have one owner outside the mind and the manager: `work/WorkLedger` (commitments,
  plans, windows, run-stage advance), persisted to `world/ensouled/work.json` and journalled as `WORK`
  rows. `core/AgentMind` reports perception into it and reads back what a step is worth; nothing else
  writes it.
- **Pass/fail — live:** `bring me 6 oak logs` → `work-opened` → `run-survey` (`logs in sight`) →
  `run-claim` (`held 5/6`) → `window-met` → `work-satisfied`, and the world's own `domain.json` shows
  `run:5219ccc1 … "stage": "ESTABLISH"`. An ambiguous wish is clarified, not guessed (see §10).
- **The disconnect-mid-step clause is closed, and it was the interesting one.** Commitment
  `5a3133dc` (16 logs, run `f0a78ad1`) was opened, surveyed and claimed live; the server was then
  killed with no shutdown hook while the resident's `harvest_tree` was *executing* (`intent:14849`,
  `logs held 13/16`). It was restarted on the same build, the client rejoined, and the world's own
  record shows

  ```
  work-opened    commit:5a3133dc  count=16  run:run:f0a78ad1  due=204912
  run-survey     held=13  condition=logs in sight
  run-claim      held=13  condition=held 13/16
  -- killed here, mid-harvest --
  window-met     held=16  condition=logs held >= 16
  work-satisfied held=16  count=16
  ```

  with `domain.json` showing `run:f0a78ad1 … "stage": "ESTABLISH"`. One `work-opened` row exists for
  that commitment, so the resume did not mint a second one; the inventory carried 13 logs across the
  crash and the resident gathered exactly the three it still owed. Failure condition not observed, and
  the run did not re-issue a completed step because the condition is read from the body's inventory
  rather than from a step counter.

### Slice 5 — Route and NavGraph (R8, R9, R10) — stub
- Graph per map keyed by topology revision; caches by (destination, movement class); local avoidance
  inside a bounded budget; validation before a blocking placement commits.
- **Pass/fail:** one recompute per topology change; preview equals the walked route.

### Slice 6 — Clients against the package boundary (R10, §A6.2) — stub
- Define the categories a client must stream (WorldManifest, Snapshot, Delta, ObjectiveDelta,
  SaveStatus, EventFeed) before adopting any field names, per §A6.6's "wait for inspection".
- **Pass/fail:** a client without the mod joins, acts and is addressed as itself; the server refuses
  its unvalidated preview.

---

## 8. What this plan deliberately does not do

- No LLM in the decision loop.
- No automation surface until §13 is closed with evidence (§4).
- No new persistence that mixes authoritative state with derived state (§A5.2).
- No purge, rename or reassignment of civilians' sessions or residents (owner instruction).

---

## 9. File ownership (unchanged where the file already owns the concern)

| Concern | Owner |
|---|---|
| What a being may say | `core/Lexicon`, `core/Realizer`, `core/Talk`, `core/ProseLines` |
| Who a being is, and who may become one | `core/ResidentNames`, `mind/AgentManager` (roster gate) |
| What it perceives of itself | `core/Supplies`, `body/*WorldView` |
| Frames | `core/VerbFrame`, `core/FrameParser`; `core/VerbLibrary` (new) |
| Commands and revisions | `core/Command` (new) |
| Lifetimes | `domain/*` (world, homebase, run identities) |
| Long-horizon work (commitment, plan, window, run stage) | `work/WorkLedger` — the one owner; `core/AgentMind` reports perception in and reads worth out |
| Movement | `body/PlayerMotor`, `body/BlockSight`, `nav/*` (new) |
| Evidence | `surface/StreamLedger`, `core/MindLedger`, `core/ChatTranscript` |
| Vocabulary compilation | `tools/prose_compile.py`, `tools/prose_ingest_mods.py` |

`mind/AgentManager` and `core/AgentMind` still own too much. Slice 4 did not need them split: the work
concern was *extracted* into `work/WorkLedger` instead, and the mind now calls it in three places
(`observe`, `pull`, `offer`) rather than holding commitments itself. That is the smaller move and it
keeps the debt out of a class that already decides one action at a time. Slices 5–6 (nav, client
package) will land in the same shared territory, and splitting the two classes is still the price of
those, not of this.

---

## 10. Landed state, and its limits

Verified on the running dev server and its headless client, not in tests alone:

- **The parser has two live callers.** `AgentManager` frames every being speech act (`source=speech`)
  and every inbound chat line (`source=chat`); the wish command frames what an operator puts to a being
  (`source=wish`). Tests are no longer its only user.
- **A real row, on the running system** — `run/world/ensouled/frames.jsonl`, written by a live wish:

  ```
  frame:102140:1 source=wish actor=Rcon verb=comes verb_state=named
  claim_shape=WHEN shape_basis=cues:if,someone verb_basis=lexicon:911
  residue=[if,someone,at,the,the] world_id=world:4378178c-… encounter_id=encounter:40b9cbef-…
  utterance="if someone comes at dusk wake the others while the sun is low"
  ```

- **The long-horizon spine is live, and its rows are the evidence.** A wish put to a resident on the
  running server opens a commitment, and from then on the resident's own perception moves it:

  ```
  work-opened      commit:ec586b2e  count=6  run=run:5219ccc1  due=168120
  run-survey       stage=SURVEY     condition=logs in sight      held=5
  run-claim        stage=CLAIM      condition=held 5/6           held=5
  window-met       condition=logs held >= 6                     held=6
  work-satisfied   count=6          condition=logs held >= 6     held=6
  ```

  and the world's own `run/world/ensouled/domain.json` reads
  `run:5219ccc1 … "purpose": "bring me 6 oak logs", "stage": "ESTABLISH"`. The 6th log was harvested by
  the resident between the CLAIM and the satisfaction; nothing in the sequence was commanded.
- **A crash mid-step is distinguishable from a finished step — after a fix it needed.** The journal
  writes a DECISION row before the action (§49.3) and an OUTCOME row when it ends, but the two normal
  endings (`completed`, `failed`) cleared the mind's current intention without writing one: of 3,937
  decisions in this world's journal only 1,286 had any outcome, and every one of those was a boot
  "recovery". So each restart announced hundreds of *finished* wander steps as "interrupted", and the
  one decision the crash really caught was indistinguishable from them. Both endings now close their
  row, and recovery announces only the newest pending decision (a process can be inside one decision
  when it dies) while closing older ones silently as `superseded`. Verified live: 7 new decisions
  produced 6 outcomes (`completed` ×5, `failed` ×1, the 7th still running), and the next boot announced
  exactly **one** recovery — `Ash begins harvest_tree at -106,89,-35`, the step that was executing.
- **A plan is replaced without the debt ending.** The earlier commitment `4bbf40e1` (8 logs) recorded
  `plan-replaced … step=wander … why=nothing in sight`, then `plan-replaced … step=harvest_tree …
  in sight again`, then `window-expired … condition=window closed` when its five minutes ran out —
  three different fates for the plan, one continuous commitment.
- **An ambiguous wish is clarified, not guessed (addendum E.4).** `bring me some oak wood` wrote
  `work-unclear` and the being asked, through its own client, `How many logs?`. `bring me four logs`
  reaches the same question because number words are not read; `bring me 3 moonrocks` writes
  `work-unclear` with nothing named. Dictation (`say …`) is refused before it reaches work at all.
- **The row lands in the world's own directory.** It used to land in `run/run/world/ensouled/` because
  the ledger resolved a relative path against the server's working directory while the reader looked
  in `run/world/`. The world now owns the path (`FrameLedger.file()` asks `WorldState.directory()`).
- **A replay writes nothing.** Re-sending an accepted wish after a restart returns `REPLAYED`, moves no
  revision and produces no second frame (§A6.4's failure named in the ADR).
- **The corpus is ingested, and coverage is checkable.** `tools/prose_ingest_mods.py` now reads a
  root's loose prose files as well as its subdirectories, takes `.txt` alongside `.md`, and accepts
  `--file` for documents outside a root; its report lists every prose file each source contributed.
  44 mod trees (33,091 words) and this project's own 21 documents plus `CONTEXT.md`, `README.md` and
  `AGENTS_DO_NOT_IGNORE.txt` (8,909 words, including v9 at 68,580 tokens) are in
  `tools/*_lexicon.tsv`; `prose_compile.py --extra` merges them, taking the attested vocabulary from
  69,555 to **96,537 words**.

Known limits, named rather than implied:

- **The baseline is still 8 verbs, and v9 cannot change that.** The baseline is one row per claim shape
  the claim corpus uses (`WHEN REQUIRE UPHOLD FORBID DEFINE ALLOW EXECUTE RECORD`). Ingested prose adds
  words a being may say; it cannot add a claim shape, because a shape is a kind of assertion the claim
  store makes, not a word. More shapes need more claims, which no tool compiles from a design record.
- **`RECORD` is still unreachable**: 0 cues, because `compile_cues` ranks words by *lift over the whole
  corpus* and `RECORD` is the majority shape, so none of its words stand out. Slice 1's named defect.
- **Shape scoring now uses the compiled lift.** It used to count matched words and ignore the third
  column, so `if someone comes…` (WHEN 2, DEFINE 1) and `you must not leave…` (REQUIRE 3, FORBID 2)
  were both "too close to call" and live frames carried no shape at all. Each matched cue now
  contributes its lift; a shape still needs two cue words, ≥ 12 combined lift, and ≥ 1.5× the
  runner-up. Both sentences now classify (`WHEN cues:if,someone`, `REQUIRE`), and one cue word still
  does not (`I need wood when it is cold` → no shape).
- **The verb slot is grammatical, and was taking subordinators.** "if" filled it. Subordinators and
  indefinite pronouns are now skipped there.
- **The object runs too far.** `if someone comes at dusk wake the others` gives
  `object_kind="dusk wake others while sun is low"`: only `and`/`,` end the object, so a second clause
  is absorbed. Reported, not fixed.
- **`Command` journals acceptance, not refusal.** `WorldState.accept` returns `STALE`, `REPLAYED` or
  `WRONG_WORLD` and writes no row for it; R6's accept/refuse journal is a stub.
- **A mob-body spawn mints a new identity.** `ensouled spawn Ash` created `agent:ac5ce2b9…` beside the
  persisted `agent:4491e473…` that owns Ash's homebase, so `correction.md` §2's "persistent AgentId must
  survive" is not true of that path. The networked session binds to the existing life; the spawn path
  does not.
- **Three of the thirteen requirements have no carrier yet** (R8, R9, R11's region state) and two are
  half carried (R7 has the deadline, not the pressure row; R12 has persistence but no world clock
  that pauses a plan). Slices 3–4 are no longer stubs; slices 5–6 are.
- **A resident's mind could stop while its session stayed connected.** `AgentRecord.tick` correctly
  refuses to act through a body that is not alive, but it *dropped* the body, and nothing bound the
  session again: after this server's restart, Ash's `inspect` sat frozen at `tick=132639` while the
  world clock passed `148847`, with `body=unloaded` and a player list that showed it online. Body
  lifetime is now resolved every tick from the server's own player list (`AgentManager.resolveClientBodies`),
  and `AgentRecord.attach` compares the entity rather than its recycled id. Fixed and observed: the
  same resident now ticks, decides and completes work after a restart.
- **The server could run a jar nobody had built.** `run/mods` was cleaned with the glob
  `ensouled-*.jar`, so a plain `ensouled.jar` from an earlier hand-install survived it and Fabric
  loaded that instead of the build: `common init build=0.1.0+src.c9a8451b` while `build/libs` held
  `…bdf56b73`. All three launch scripts (`devserver.sh`, `bots.sh`, `client.sh`) now clean
  `ensouled*.jar` and refuse to launch if a stale one cannot be removed; `devserver.sh` also prints
  the build it synced.

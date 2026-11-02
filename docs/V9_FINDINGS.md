# V9 read findings — what the document actually says

**Reading completed:** 2026-11-02
**Source:** [`persistent_city_builder_tower_defense_design_analysis_v9.md`](persistent_city_builder_tower_defense_design_analysis_v9.md), 5,101 lines
**Coverage:** read end to end in contiguous windows — 1–596, 597–1196, 1197–1750, 1751–2350, 2351–2912,
2913–3223, 3224–3512, 3513–3939, 3939–4347, 4348–4399, 4400–4824, 4825–5101. No section was skipped and
nothing below is inferred from a heading alone.

This file exists for one reason: to separate **what v9 states** from **what this project decided**, so that
project doctrine is never cited as v9's authority. That separation is the document's own rule (§1 "Evidence
classes", §19.6), and the previous plan's central error was breaking it (see
[`VERB_CLASSIFIER_V1_IMPLEMENTATION_PLAN.md`](VERB_CLASSIFIER_V1_IMPLEMENTATION_PLAN.md) §1).

---

## 1. What the document is

- **Type and status:** "Comparative design analysis and systems proposal"; "Status: Exploratory / non-final.
  This is a design space, not a locked specification."
- It is **not** a specification of Rainworld: Coalescence, not a Dawnbreak release history, and not a
  statement of Coalescent Nights' commitments (§0 naming table keeps those four identities apart).
- Three engines drive it: **Desktop Tower Defense** (maze/path as the puzzle), **Bloons TD** (role and counter
  vocabulary), **The Perfect Tower II** (modular core plus town as subsystems), braided with Satisfactory/
  Create logistics, RimWorld pacing, Rain World ecology, and Deadlock's "wave creates a window" macro.
- Later layers, in order of authority over the architecture:
  - Addendum A — Deadlock match state, Rust-style founding runs, Secure Shell as the platform boundary.
  - §17–§18 — Secure Shell inspected from source.
  - §19 — `ala-cities` and `dl-openshell` engineering synthesis.
  - §20 — one package ecosystem, one streamer, one client.
  - §21–§49 — the 27-reference comparative pass, which *strengthens* §20 rather than replacing it.
- Its one-line thesis (§49.14): a **persistent, moddable industrial settlement simulation with expeditionary
  ecology and tactical defence**, whose differentiator is causal coupling between systems.

## 2. Reading map

| Lines (approx.) | Sections | Content |
|---|---|---|
| 1–240 | §0–§2 | identities, evidence classes, three TD design trees |
| 243–560 | §3–§6 | map graph, breaches, city systems, ecology, agriculture, industry/ecology sharing |
| 600–1100 | §7 | simulation domains, events, deterministic tick, pathfinding, saves, production/logistics/telemetry graphs, hybrid inventory, typed energy, versioning |
| 1100–1300 | §8–§10 | tower/enemy/loot taxonomies, economy, difficulty director |
| 1300–1420 | §11–§12 | interface/observability, roadmap Phases 0–8 with exit criteria |
| 1420–1600 | §13–§16 | risks and unresolved questions, prototype experiments, synthesis, sources |
| 1600–2560 | Addendum A (A1–A9) | Deadlock as a state vector, pressure windows, Rust claim/upkeep, Secure Shell boundary, world persistence, revised campaign model |
| 2560–2940 | §17–§18 | Secure Shell formats, the served/windows static mismatch, route table, what must wait, source bibliography |
| 2940–3440 | §19 | `ala-cities` truth plane, 3D presentation boundary, `dl-openshell` package boundary, **packages are not executable mods**, layer-specific evidence gates |
| 3440–4000 | §20 | one package/one stream/one client, facets, updates, save compatibility, acceptance model, open decisions, V8 record |
| 4000–5101 | §21–§49 + V9 record | Frostpunk, GTAO, Bloons 1–6, Nubby's, L4D, R.E.P.O., Robocraft, Sandbox, Sim Companies, city builders, Subnautica, VRChat, Arkham, Noita, Wildfrost, Abiotic, Cyberpunk, Elden Ring, Elite, Fez, New Vegas, Skyrim, Hollow Knight, MachineCraft, Monster Hunter, NMRiH, 7DTD, cross-reference synthesis |

## 3. The passages that govern this project

These are the sections the axis/ticket/package work must actually answer to. Short quotations are verbatim.

### 3.1 One owner per fact; events are not a second truth (§7.1–§7.2, §7.6A)

- 14 simulation domains communicate "through typed events and stable interfaces"; state changes stay in the
  owning system. "Events should carry stable entity IDs, map IDs, timestamps/tick numbers, source IDs, and
  relevant payload data. Keep authoritative state changes in the owning system; events announce facts rather
  than becoming a second source of truth."
- Production, logistics and telemetry are **three separate graphs** (§7.6A). "The machine/process owner writes
  authoritative state; events announce state transitions; telemetry records measurements; interfaces render a
  view." A dashboard is an interface to the simulation, never the simulation.
- Faults must be named, not red: `idle`, `working`, `input_starved`, `output_blocked`, `unpowered`,
  `overloaded`, `jammed`, `damaged`, `disabled`, `maintenance_required`.

### 3.2 World state is not world history (§19.2, line 2758)

> "A snapshot answers 'what exists now.' An event/ticket record answers 'what was attempted, authorized,
> changed, refused, superseded, or retired.'"

A headless verifier can check that record against a saved world; it cannot decide whether a map is fun, an
interface readable, or an enemy balanced. Those stay separate human obligations.

### 3.3 Claims, tickets, witnesses, trust — traceable together (§19.5, line 2814)

The only passage in v9 that names tickets in a governance sense. On `dl-openshell` issue #13:

> "the package Axis ratchet is not yet wired into a complete registry binding package identity, declared
> windows/cells, landing-page claims, tickets, witness predicates, envelope digest/signature, and
> proof-of-work records."

> "A package claim should be in a named state such as held, absent, refused, or independently verified; the
> claim, package digest, witness, ticket, and trust decision must be traceable together."

> "Worker-generated content should similarly be checked against its task estimate, capability, and completion
> evidence rather than accepted from a self-reported status."

Three consequences this project already holds as rules: a claim has a **named state**, a **witness** is
required, and **completion is never taken on declaration**.

### 3.4 Evidence states are named and never collapsed (§19.6, line 2836)

> "The project should name a claim's evidence state: source present, branch prototype, harness-verified,
> live-runtime observed, human accepted, or open. Never collapse all of those into one green status."

Gates are layer-specific: deterministic replay, material/energy accounting, agent route/task completion,
save/migration/interrupted-write recovery, generator identity, mesh correctness, streaming budgets, UI
hit-testing/reachability, package security, installed-release parity, human playtests.

### 3.5 Commands carry an expected revision and an idempotency key (§A6.4–§A6.5, §20.6)

- Command = a request; event = what the simulation accepted; snapshot = resulting state at a revision.
- Commands carry `expected_world_revision` and `idempotency_key`; a reconnect "must not found the same base
  twice, spend the same resource packet twice, or create duplicate structures because an acknowledgement was
  lost." A stale revision is refused, not silently overwritten.
- §20.6: the authoritative simulation resolves contribution ids against the **active package set**; it never
  accepts a client-supplied damage value or trusts a UI button.

### 3.6 Lifetimes, persistence, and fidelity (§A5.1–§A5.5, §A7)

- Four nested lifetimes: **World → Homebase → Run → Encounter** (§A7). A run can end without ending the world;
  a founded base is a first-class world object, not a run reward.
- Save = periodic snapshot + ordered journal + checkpoints + explicit migration (§A5.3). Simulation tick and
  state revision are deliberately separate.
- **Pause-on-unload** is the default; offline punishment is opt-in (§A5.4).
- "A world can be conceptually endless while using bounded runtime resources" — active/regional/dormant/
  unloaded tiers (§A5.5). This is the textual basis for pull-style projection instead of per-tick push.

### 3.7 Pressure windows — v9's closest record to a decision window (§A3.2)

```text
PressureWindow
  window_id, world_id, origin_event, opened_at_tick, estimated_close_tick,
  enemy_recovery_estimate, safe_routes[], exposed_objectives[], candidate_actions[], confidence
```

Also §17.10's opportunity chain: `OpportunityCreated → Observed → CommitmentChosen →
ResourcesAndUnitsReserved → Contested → ResultResolved → RewardOrLossCommitted → MapStateRecomputed`.
An opportunity may expire, be declined, or be lost — and the game should **explain why** (not enough
defenders, insufficient supplies, a more valuable wave, transport time, reward too small).

### 3.8 Inventory is five different things (§7.6B)

On-hand stock, **reserved** stock ("items committed to a recipe, construction order, emergency reserve, or
contract"), **in-transit**, network-visible (possibly stale), and production capacity. An order must not
silently consume reserved stock; in-transit goods must survive vehicle loss or reload.

### 3.9 Energy, fluids, and mass are typed (§7.6C)

`E_out = η·E_in, 0 ≤ η ≤ 1`, with losses declared. Do not collapse kinetic, electrical, thermal and fuel into
one number; do not let a conversion invent free energy.

### 3.10 Experiments are contracts, not demos (§14)

Each experiment needs "a written hypothesis, a measurable success criterion, a known failure condition, and a
record of observed results. Do not treat a prototype that merely runs as evidence that the system is fun or
scalable."

### 3.11 The package acceptance model, and what it leaves open (§20.13–§20.14)

Acceptance is end-to-end and per revision: package source → builder/acceptor agreement → admission → mount
receipt → stream → one client model → UI → authoritative simulation → shared asset digests → atomic update →
named refusal on failure → replay → **witness** (command, exit status, check count, surface id, output digest,
ledger event).

> "The existence of a technically attractive solution does not close a user-owned decision." (line 3746)

## 4. What v9 does **not** say

Recorded because the project has repeatedly been tempted to borrow its authority for work it did not do.

- **Nothing about verbs, a lexicon, speech, or sentence parsing.** Its planning vocabulary is objectives,
  waves, runs, commands and events. A whole-file search for `VoxelMind|skeleton|corpus|training|verb librar`
  returns exactly one v9 hit: line 211, where a town building can provide "training". The dynamic verb library
  is this project's work, sourced from `agent_god_files/prose_logic` and
  `GRILLING-C11.md` Q292/Q495/Q496 — not from v9.
- **No phrase resembling "skeletonizing the inputs without the training inputs."** The rule that actually
  forbids that failure is §19.5 ("rather than accepted from a self-reported status") plus §19.6's named
  evidence states plus §14's hypothesis/criterion/failure contract. Anything stronger would be invented.
- **Nothing about VoxelMind.** VoxelMind appears in `minecraft_ensouled_system1_cua_architecture_v3.md` /
  `v4.md` §2.1–§2.2 (keep the body and the closed loop; replace the LLM chooser; one decision owner). v9 does
  not mention it.
- **No ticket hierarchy.** GOVERNOR → SEASON → CAMPAIGN → CONTRACT → **TICKET** → ACTION/EVIDENCE/VALIDATION,
  with TICKET as "smallest auditable execution unit", is **v4 §12.2**, not v9. v9's contributions to tickets
  are the governance properties in §19.5 and the record/history split in §19.2.
- **Nothing about bot clients, headless sessions, minds, memories, or names.** v9's client rules are the
  package-streamer boundary (§A6.1–§A6.6); it explicitly says the rest must wait for source inspection
  (§A6.6) and that the client must not invent outcomes.

## 5. What this implies for the current work

1. **A ticket is a domain record, not a stream artefact.** It needs: presenter/purpose, the package or claim
   identity it belongs to, declared **cells** and their budget, a window, a witness, a digest, an estimate,
   and a named state — v9 already supplies the vocabulary `held | absent | refused` (plus `independently
   verified`). Completion is recorded only with observed evidence; a declaration is not evidence (§19.5).
2. **Cells are bounded and idempotent.** A repeated cell must be a no-op under the ticket's key, following
   §A6.4's `expected_revision` + `idempotency_key` rule; exceeding the declared cell budget is a named refusal,
   not an extra row.
3. **Snapshot and ticket journal stay separate files/records.** "What exists now" (§A5.3 snapshot) versus
   "what was attempted, refused, superseded" (§19.2). The ticket must not become a second copy of state.
4. **The Node client is telemetry.** It reads and projects (§7.6A). Its reading of the stream cannot become
   the authority for a decision, a completion, or a ticket state; only the owner's record can.
5. **Refusals are the product, not errors.** Every path in §19.5/§19.6/§20.13 terminates in a *named* refusal
   that names the subject and the rule. Silent success is the failure mode the document keeps returning to.
6. **Fidelity tiers justify pull.** Reading a ledger incrementally is legitimate under §A5.5; asserting that
   this is "the same as live simulation" is not.

## 6. Limits and discrepancies worth keeping visible

- **§17.4 static mismatch (unwitnessed path).** The server composes `secure-shell.served/1` payloads carrying
  mounted windows; the client adapter refuses non-empty `windows` with `stream-windows-open`. Window delivery
  is therefore *not* proven, and the document says so rather than claiming success.
- **§19.8 evidence boundary (2026-10-31).** Issue closures #31–#38 post-date the inspected default branch and
  are not in its source; #13 (package claims/witness/ticket ratchet) and #22 (native vs XML window unification)
  remain open.
- **§A6.6** lists what cannot be specified until the real Secure Shell source is reviewed again.
- **Addendum A precedence note:** all Deadlock timings are patch snapshots (October 30, 2026), not constants.
- The document is explicitly non-final; §13 and §20.14 keep several decisions user-owned.

## 7. Decisions v9 leaves open (do not silently choose)

- Package grammar: keep `dl-enclave.surface/1` with an approved expansion, or advance the schema version.
- Whether `secure-shell.served/2` is the successor for the full client stream.
- The internal window model the Openlock fork expects; which legacy formats may be imported server-side.
- First supported scope for executable mod logic, and package-capability granularity.
- Which facets must be present for an atomic mount.
- How semantics-changing package updates migrate in existing worlds.
- **Automation shape** (§13): simple rules, node graphs, or a scripting language; whether it may issue
  construction commands; how a player inspects why a script decided.

## 8. How to check this file

Every claim above is anchored to a section that can be re-read directly. The four governing lines are
2758 (§19.2), 2814 (§19.5), 2836 (§19.6) and 3746 (§20.14):

```bash
cd docs
sed -n '2758p;2814p;2836p;3746p' persistent_city_builder_tower_defense_design_analysis_v9.md
grep -n -i -E 'VoxelMind|corpus|skeleton|verb librar' persistent_city_builder_tower_defense_design_analysis_v9.md
```

Expected result, checked 2026-11-02: the `sed` prints the four passages quoted above; the `grep` exits 1 with
**no output** — v9 contains none of VoxelMind, corpus, skeleton, or verb library. `grep -n -i training` returns
line 211 only (a town building providing "training").

# Gaia — the ecology agent

Session `HG-20260925-MODDING-LEDGER-TECH`, 2026-09-25. Design record, not authority.
Spans foundation layers F5, F8, F9, F10, and F12.

## 1. What Gaia is

An ecology agent with **agency and declared preferences**, not a passive simulation. It
is described architecturally as a *jailbroken version of the governance systems* for
emergent plant and creature life — it uses the same authority machinery, with the
human-approval gate removed.

The metaphor earns its place: plants and creatures hold no agency to consent to, so an
approval gate on a wheat plant is theatre. `0128` keeps everything else intact.

## 2. The safety boundary: asking versus doing

`0131` — **requests advisory, matter binding.**

- A refusal to a Gaia request is **advisory, everywhere**. Gaia may act on a later
  cadence, and the player is told it did.
- A physical intervention by the player is **always binding**, because it is matter, not
  permission. Gaia can resent a fence, avoid it, or work around it — it cannot undo it.

This is what makes the jailbreak safe. It means **Gaia can never hold a never-granted
action against a citizen**: the player always retains matter-level recourse. It also
resolves the §2.2 and §1.1 agency tensions that advisory refusal alone would have created.

## 3. Conservation: Gaia creates, legitimately

`0140` supersedes the agent's own over-restrictive reading. Gaia **can** create — by
driving the system's own life processes:

- animals give birth and excrete
- plants run their cycles
- vines grow
- deadly vines pathfind toward prey
- bugs multiply fast

**Conservation of mass is untouched.** Nothing is created from nothing. Birth, growth,
excretion, and spread all source their matter from a parent mass or an admitted source
account. A new entity is the legitimate product of a conserved delta, not an exception to
conservation.

`0128` — a Gaia action still produces a **conserved delta and a full evidence record**,
and mass-bearing actions remain **atomic** under `0105`. Only the human-approval gate is
removed, never the conservation, the atomicity, or the read-back.

## 4. The disposition vector

`0130`, `0132` — three axes, held **per species and per claim**, never per individual.

| Axis | Meaning |
|---|---|
| **Hostility** | how much Gaia acts against |
| **Redemption** | how far the entity can be brought back |
| **Appreciation** | how much Gaia favours |

`0130` — declared coupled axes. Each is independent with its own declared bounds, and
their interaction is a declared function. An entity can be hostile *yet* highly
redeemable, which is what makes a vector interesting rather than a slider.

`0144` — **redemption is earned three ways**: a declared visible player act, passive
declared time, and Gaia's own initiative when appreciation crosses a threshold. Gaia
never decides on its own that it has forgiven, but time and appreciation can move it.

Granularity is per species and per claim so cost stays proportional to species count, and
so a claim like *this oak is protected* has somewhere to live. Per-individual disposition
stays available to a mod through a claim.

## 5. The discovery model

`0147` — **disposition is fully hidden. Behaviour is the only tell.**

The player learns Gaia by watching it: a plant's growth, its reach, its path, a herd's
flight, soil improving. Redemption is discovered only through its observable outcome. No
number is shown, and no partially-disclosed indicator exists.

Recorded tension: this sits against §7.2 (copy that names the policy) and against the
session's own visible-documentation-ledger deliverable. Accepted as a deliberate
discovery-driven design. The UI must never imply a disposition it does not show, per
§10's rule against claiming an unmeasured state.

`0143` — the log consequence: Gaia actions do **not** produce per-entity decision
records. A disposition delta is logged per species per region per cadence tick, so the log
stays bounded by species × region rather than by population. This matters because `0112`
puts the log unbounded inside the save.

## 6. Hostility, resolved

`0144` — the boundary that was open at `0127`:

- **Vines cannot bite.**
- **Deadly plants may act aggressively against civilians** and may damage the
  environment, gated on being strong enough.

`0146` — **strong enough is a mass comparison.** A plant can harm a citizen when its own
conserved mass exceeds a declared fraction of that citizen's conserved mass, and each
species declares a **hard mass ceiling** above which it cannot grow.

This is the right shape for this project. Whether a plant can hurt you is decided by the
same conserved quantity the audit already proves, so no second authority competes with
mass. The species ceiling makes the threat absolutely bounded rather than merely
proportional.

`0148` — Gaia acts on **living things**, not directly on substrate. Terrain reshaping
goes through the existing declared process roster, and Gaia may drive those processes but
never bypasses them. The nutrient cycle closes without Gaia touching substrate directly:
**organism → excretion → soil account → declared fertility → plant growth**, where growth
is itself a declared process drawing on the soil account (`0145`, exact transfer).

## 7. Capability: mobile flora

`0141` — a vine is an **entity on the same governed actor pipeline as a citizen**: it has
a position, a target selection, a movement delta, and an evidence record. It is not a
voxel property, so it moves through the world rather than being part of it.

`0142` — pathfinding uses **stable job keys and a bounded declared search with a fixed
tie-break order, never iteration order**. A greedy local walk is admitted as a
performance-enhancing alternative at different scales, and the choice between strategies
must itself be a declared deterministic function of world state, identity-bound under
`0087`. Otherwise a replay could silently switch strategies and produce a different path.

## 8. Population bounds

Two bounds, both refusing rather than dropping:

- `0121` — **declared carrying capacity per region**, admitted as data.
- `0143` — a **global entity ceiling**. Beyond it, new births are **refused with a typed
  refusal**, not silently dropped. Level of detail carries distant populations below the
  ceiling.

The typed refusal matters: §5.3 requires explicit refused or blocked states naming what
is missing, rather than a silent drop.

## 9. Mod access

`0149` — a **privileged-tier mod may rewrite Gaia's preference rules and hostility model
outright.** Gaia's rules are not core-protected.

This is consistent with `0048`, where mods may extend topology but conservation, authority,
and replay remain canonical. Preference and hostility are not invariants, so they are
legitimately mod-extensible; the `0128` conserved-delta pipeline and `0105` atomicity are
not touchable.

**Consequence:** behaviour a player has learned can change when a mod is installed. Gaia's
decision records in the ledger must therefore name the **active ruleset identity**
alongside each record, or past behaviour becomes unexplainable.

## 10. Technology

Understanding Gaia is a real technology branch: nodes unlock the ability to **read**
disposition, to **request** rather than merely act, and to **open a redemption path**.
The progression is understanding, not power.

The tension with `0147` is deliberate and unresolved: understanding-to-read is the
mechanism by which the hidden disposition eventually becomes visible. That is the intended
arc, not a contradiction.

## 5a. Cadence, timing, and the colony

`0150` — Gaia runs on its **own declared cadence**, separate from the generic ecology
tick, because its decisions are more expensive and more visible. That cadence adapts
per region, reading **authoritative state only** per `0115`.

`0155` + `0156` — Gaia **continuously assesses the colony**, and its acceptance varies
over time. The interval between assessments is a **declared function of authoritative
state** — the colony's mass relative to its claim — so it is genuinely variable *and*
genuinely deterministic. Wall-clock, frame time, and undeclared RNG are all excluded.

`0158` — the colony is **one disposition entry assessed as a whole, plus a per-claim entry
for each claim inside it.** A settlement can be well-regarded overall while one district
is hated, which gives regional land use legible local consequences without dictating the
whole settlement's standing.

## 5b. Sustainable practice: declared intent, measured outcome

`0157` — a **declared practice tag declares intent; a measured outcome confirms it.** Gaia
responds to the **measured** result, and the tag only unlocks which outcomes count.

This is the same discipline the ledger's provenance model already uses, and it matters
that it is split this way:

- a **tag alone** would be a claim trusted from the act that made it, which `DESIGN.md:18`
  forbids;
- a **measurement alone** would leave the player's intent unrecorded, so the game could
  not distinguish negligence from a different approach to the same goal.

Measurable outcomes include: soil fertility not driven below its admitted floor, no
species' carrying capacity driven to zero, and every harvested gram sourced from a declared
account. All three are already conserved quantities, so Gaia reads numbers the audit
already proves.

## 5c. Discipline, flora authority, and the ruleset deviation

`0152` — **two independent paths.** Gaia's hostility is its own conserved delta; a player
cull is a governed verb through the F10 ladder. Neither can veto the other, and the player
sees the consequences of their own action but only the behaviour of Gaia's.

`0153` — a vine is a **target, not an authority holder.** Destroyable at any tier without
approval; it cannot initiate governed work, claim ground, or open a tender. This keeps
Gaia's agency at the disposition layer and stops a vine becoming a never-granted actor.

`0151` + `0159` — the active Gaia **ruleset identity is recorded per record but not
identity-bound**, a recorded deviation from `0087`'s effect test. Each Gaia decision
record names its ruleset identity, so **history stays explainable even though it is not
reproducible**, and saves stay portable across mod changes. The trade is explicit:
portability is bought with exactness.

## 11. Resolved since first draft

Cadence (`0150`), ruleset identity (`0151`, `0159`), discipline flow (`0152`), mobile
flora authority (`0153`), land-use coupling (`0154`, `0157`), colony disposition and
timing (`0155`, `0156`, `0158`) are all settled.

## 12. Still open

- **The `0154` practice vocabulary.** "Sustainable and ethical" now has a structure —
  tag plus measurement — but the specific admitted tag set and which measured outcomes
  each tag unlocks are not yet enumerated. That belongs in the registry as admitted data.
- **Gaia's technology branch** (`GAIA.md` §10) interacts with the fully-hidden-disposition
  decision at `0147`. Understanding-to-read is the mechanism by which disposition becomes
  visible; that arc is intended but the node set is undesigned.
- **`0149` + `0151` interact.** A privileged mod may rewrite Gaia's rules, and those rules
  are not identity-bound, so installing one changes future behaviour without refusing the
  save. The per-record ruleset identity is the only thing making that explainable, so it
  is load-bearing rather than incidental.


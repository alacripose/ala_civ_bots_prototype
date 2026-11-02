# Upstream alignment: how `secure-shell` upgrades formats, and where it already went wrong

Source, walked rather than quoted: `C:\Users\ala\Documents\secure-shell` —
`docs/DECISIONS.md`, `docs/FORMAT-SOURCES-AUDIT.md`, `docs/PROOF-OF-WORK.md`, `docs/LEDGER.md`,
`docs/MOTION.md`, `docs/SELECTIONS.md`, `AGENTS.md`.

The upstream tree's own audit opens by admitting the failure this document exists to avoid:

> *"two passes since then reported one implementation per idea as if it were the system"* —
> `docs/FORMAT-SOURCES-AUDIT.md`

So the rule for anything below: **read the crate, not a summary of the crate.** Every claim here
names the file it was read from.

---

## 1. How upstream upgrades a format

| Rule | What it requires | Where it is recorded |
|---|---|---|
| **The schema is inside the digest preimage** | `sha256(domain ‖ schema ‖ kind ‖ payload)`, so a schema bump changes the digest rather than only the header | `planet-package/src/frame.rs:5`; DECISIONS 2026-10-31 |
| **An older frame is refused by name, never converted** | `frame-schema` with `found` and `supported`; holds even when the old frame is self-consistent under its own schema; a newer schema is refused the same way | `BadSchema { found, supported }`; DECISIONS |
| **Upgrade by addition, not mutation** | `surface.mount/2` stays exactly a receipt; the client receives a new additive `secure-shell.served/1`; no silent `mount/3` that every existing row would fail | DECISIONS "mount v2 is kept as the receipt" |
| **Reserved seams are declared before they are used** | `FrameKind { Data = 1, Code = 2 }` exists now so the code frame's arrival does not change the frame; a `code` frame packs and is refused `frame-kind-reserved` until a reader exists | `frame.rs` |
| **One grammar, read by builder and acceptor** | A declaration the builder accepts is one the shell accepts; nothing is compiled twice | `planet-package/src/shell.rs`; `server/decl.mjs` |
| **Derived, never carried** | A tier is computed on read; a carried tier is refused `tier-not-derived` | `tools/port/served.mjs` |
| **A new axis is a new axis** | The expanded material ladder is `secure-shell.material-grade/1`, not a widened `certification_tier`; five shared words are a declared mapping, never an alias | DECISIONS 2026-10-31 |
| **A wish is a declared surface, not a format** | The toast is `dl-enclave.surface/1` on lane `beacon`, rule `small-right`, mounted, receipted, served | `packages/toast/toast.surface`; DECISIONS |

**The framed digest is the load-bearing rule.** Length is inside the preimage, so a truncated
artifact cannot collide with a longer one; the domain is inside it, so a byte string cannot be
hashed as another kind of thing. A bare SHA-256 is not equivalent and is refused as evidence.

---

## 2. Mistakes upstream recorded, and what this project does

| Upstream mistake | Where it is recorded | This project |
|---|---|---|
| **A leading-zero nonce as the proof of work.** `openlock.envelope/1` carries a hash lottery with a leading-zero floor; `dl-enclave`'s drift record (D-3) calls it out as *"proof of work is standing in for a trust root"* | `docs/PROOF-OF-WORK.md` §2 | `dev.ensouled.axis.AxisProof` implements the **ticking proof** only: artifact bytes, framed digest, tick span, world digest, presenter, ticket, origin. No mining, no nonce search. |
| **Treating a proof as the pass.** Upstream's own test name: `a_proof_alone_is_never_enough_because_the_support_grade_is_missing` | `docs/PROOF-OF-WORK.md` §2 | `AxisProof.admit(...)` refuses `UnsubstantiatedEvidence` when no supporting reading from another ladder is supplied: *a proof is one row, never the pass*. |
| **Counting `absent` as a pass.** The three-way answer is `held` / `absent` / `refused`, and only `refused` fails | DECISIONS "the plan is refusable"; `axis_witness` | `AxisGate.Reading` is `HELD` / `ABSENT` / `UNCHECKED`; `Cells` counts each separately and never folds them into one number. |
| **Bare ladder tokens.** `"A"` is an evidence grade, a unit grade and a ticket origin at once | `voxel-model/src/axis.rs` `refuse_bare` | `AxisVocabulary.check` refuses `BareTokenRefused` (`UNQUALIFIED` / `AMBIGUOUS`) and `AxisInterchangeRefused` for a token filed under the wrong ladder. Every emitted token is qualified: `procedural_gate PASS`, `ticket_evidence A`. |
| **Reducing a ladder to a number.** Percentage, score, or `max(evidence_grade)` as certification | `refuse_reduction`; `prohibited_max_certification` (rule 52/53) | `AxisVocabulary.refuseReduction` and `prohibitedMaxCertification` carry the same sentences; the axis package carries counts per qualified token, not a percentage. |
| **A tier or coverage table that describes itself** | `check_coverage` — *"worse than no row, because it looks checked"* | `AxisVocabulary.checkCoverage` compares each row against the ladder's declared members and refuses a missing, duplicated or invented row. |
| **A second display implementation.** A native presenter was written, then removed rather than kept behind a flag: `display-second-implementation` | DECISIONS 2026-10-31; `docs/SELECTIONS.md` S1-C | This project has **one** display owner per layer: the game client's HUD owns the toast stack; the package supplies content only. Upstream's Java client rule is the same rule. |
| **A plan with no reader.** `docs/PLAN.md` described drifts nothing checked | DECISIONS "the plan is refusable" | Any plan here must be read by a gate that answers `held`/`absent`/`refused`; a document nothing reads is not evidence. |
| **Geometry assumed, not measured.** A 16:9-only layout that overflowed 4:3 | DECISIONS "display geometry is responsive" | Client-side layout clamps to the viewport; baselines are captured at two sizes. |

### Divergence this project holds deliberately: the MongoDB store

Upstream's DECISIONS record MongoDB as **the `dl-enclave` derail** and rule it out for the shell
(*"Deployment target: a Debian host, not a game server, and no MongoDB dependency"*). This project
diverges, by the user's ruling: the MongoDB store here is the **per-civilian stream isolation
store** — the thing that keeps two beings' packages from being mixed — and it sits **under** the
ledger, not beside it. The ledger remains the chained record of what happened; Mongo is where each
civilian's stream lives so that a write to one cannot be read as another's.

That is a divergence in deployment, not in evidence discipline: the isolation store is not a second
authority, and nothing in it is evidence until a ledger row and a package digest point at it.

---

## 3. The upstream format upgrades that are not yet implemented upstream either

These six are described in the upstream shell's upgraded format and are **not implemented in the
upstream repository itself**. They are the alignment target here, in dependency order, with the
seam each one lands on. Status is stated as it is: nothing below is claimed done.

| Upstream section | What it adds | Seam in this project | Status |
|---|---|---|---|
| §49.2 Durable continuity record | A record that survives restart with its own identity and lineage | `persist/MindStorage`, `MindPersistence` | Partial: state round-trips per being; no continuity record with lineage |
| §49.3 Write-ahead persistence | Intent written before the effect, replayed after a crash | `mind/AgentManager`, `persist/MindStorage` | **Absent** |
| §49.1 Recovery states | Named states a being resumes into after an interruption | `mind/AgentRecord` | **Absent** |
| §50.1–50.2 Test lifecycle | A test's life from declared to retired, not just a pass/fail | `src/test/...` harness | **Absent** |
| §47.2 Message envelope | One envelope for every inter-agent message, with sender, ticket, origin | `core/AgentMessage`, `core/MessageBus` | Partial: a message type exists; no envelope contract |
| §16 Failure model | Every failure named, with the door it came from | scattered returns | Partial: refusals are named in the axis layer only; the mind's failures are strings |

**Order of work.** §16 first: the failure model is what the other five report through. Then §49.3
(write-ahead) because it is the only one that can lose data, then §49.1 (recovery states) which
names what write-ahead recovers into, then §49.2 (durable continuity) which gives the recovered
record its identity, then §47.2 (envelope) which gives messages the same accountability the ledger
rows already have, and §50.1–50.2 last because a test lifecycle over an unstable failure model
describes tests that do not exist.

---

## 4. What has actually been ported so far

| Piece | Java | Verified |
|---|---|---|
| Nine ladders, exact tokens, source order | `axis/AxisVocabulary.java` | compiles; table read from `voxel-model/src/axis.rs` |
| Bare-token, interchange, reduction, coverage refusals | `axis/AxisVocabulary.java` | compiles |
| Civilians' four gate checks (`Selection`, `Task`, ladder reading, `Arrangement`) | `axis/AxisGate.java` | compiles; wired into `AgentMind` before a candidate is activated |
| Ticking proof: framed digest, window, presenter, origin, "proof alone is not enough" | `axis/AxisProof.java` | compiles |
| Axis package: qualified tokens, unchecked counted, coverage carried, chained digest | `AxisGate.Cells` → `SurfaceStream.axisPackage` | compiles |

Not verified live: no bot run since these ports, so no stream package has been read back from a
running being with the new vocabulary. Asserting otherwise would be the exact failure this
document catalogues.

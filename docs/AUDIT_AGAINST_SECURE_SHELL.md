# Audit — `ala_civ_bots_prototype` against `secure-shell`

Read for this audit: `secure-shell/README.md`, `AGENTS.md`, `docs/PLAN.md` §1–§3, `docs/DECISIONS.md`,
`docs/FIRST-TEN-HOURS.md`, `packages/toast/toast.surface`; and in this tree `CHECKPOINT.md`,
`docs/IMPLEMENTATION_STATUS.md`, the source layout under `src/main/java`, and the focused test run
(`./gradlew test --tests dev.ensouled.RestDecisionTest ...`, 15 tests, 8 failing).

Verdict in one line: **the runtime is now the strongest part of this project; the record is the
weakest.** `secure-shell` is the reverse — its record and gates are built and its client is not.

## 1. What each tree owns

| Layer | `secure-shell` | This tree | State |
|---|---|---|---|
| Project activity record | `server/ledger.mjs`: SHA-256 chain, idempotent op ids, stale-sequence refusal, fsynced commit, Ed25519 checkpoints, prompt vault | none; prose in `CHECKPOINT.md` / `IMPLEMENTATION_STATUS.md` | **absent here** |
| Gates against drift | 8 named gates in `tools/drift_gate.mjs`, `held`/`absent`/`refused` | none; the §52 milestone table has exit tests no machine reads | **absent here** |
| Decisions | `docs/DECISIONS.md`: choice, rejected alternative, evidence | none; §19 open questions are assumed by code | **absent here** |
| Life continuity | not this project's layer | `MindJournal` + `MindStorage` + `AgentContinuity`, four conditions | **held here**, proven under kill |
| Window stream | `surface.mount/2` receipt; `secure-shell.served/1` served record; `secure-shell.ingest/1` terminates at the server | none yet | declared there, **absent here** |
| Display owner | fork of Openlock (`shell/openlock/`) | Minecraft's own HUD/client | **unruled** for this tree |
| Content package | `packages/toast/toast.surface` (`dl-enclave.surface/1`, lane `beacon`, rule `small-right`) | none | declared there |
| Material ladder / stars / two readers | fourteen rungs, six stars, laya + von | none; `SkillStats` counts only | **absent here** |
| History | git, one logical change per commit, commit cites a ledger entry | **no git repository at all** | **critical** |

## 2. Findings

### F1 — There is no history at all (critical)

`git -C . rev-parse --is-inside-work-tree` → `fatal: not a git repository`. 113 main sources, 16 plan
documents, a running server and three clients, and zero commits. `secure-shell`'s D1 is 9 h 40 m of
work squashed into one root commit of 57 files; this tree is the same failure taken to its limit.
`first-commit` would be **refused**, not `absent`, because tracked-subject work exists.

### F2 — The record is written after the work, by memory (critical)

The `IMPLEMENTATION_STATUS.md` evidence column reads "self-test phase 2", "unit test
`sensesAreLimited...`", "`verbs` harness". Those are real runs, but the table is prose a person
maintains by hand; nothing regenerates it and nothing refuses a claim with no run behind it. The new
`MindJournal` is the closest thing in this tree to `secure-shell`'s ledger — append-only, CRC per
line, idempotent transaction ids, writes before completion — but it records **a mind's lifecycle**,
not the project's. There is no entry for today's persistence work outside this prose.

### F3 — The open decisions were encoded instead of recorded (high)

`AGENTS.md` §3 and `DECISIONS.md` require a choice, a rejected alternative and evidence. The V4 §19
questions that block `BASE-00` — autonomous world-mutation scope, death policy, gender schema,
test-mode boundary — are still open, and the code has already answered two of them silently:
`fatigue` is now a cognitive need, and `rest` is no longer the default behaviour. Those are defensible
choices; they are not recorded as choices, so no reader can tell a decision from an accident.

### F4 — The harness is the de facto witness (high)

`AGENTS.md` §0.2 is blunt: a check that runs only in Node, a headless browser or a separate witness
page is not a witness. Here the acceptance evidence is mostly a JUnit/self-test harness plus RCON
output; the client-visible claims are verified by reading server logs. For a storage-layer claim the
server log **is** the right surface (the client cannot see a snapshot write), and the persistence
proof in `CHECKPOINT.md` is honest about that. For a claim the *player* can see — a chat line, a
toast, a window — the server log is the D2 substitute.

### F5 — Tests outran the product, measurably (medium)

Focused run: **8 of 15 tests fail** against the shipped behaviour, because the previous pass made
`wander` the default and left `RestDecisionTest` and the harvest acceptance expectations asserting
`rest`. Meanwhile `CUA-01` (screens), `CHAT-02`, `SOC-01`, `PAR-01`, `NEED-01`, `MOD-01`, `COOP-01`
are open. In `secure-shell`'s vocabulary this is a plan whose steps' exit tests no longer describe
the product; the fix is to make the product's exit tests executable, not to write more tests.

### F6 — No gate refuses a milestone claim (medium)

`PLAN.md` §2 is the model: eight gates, only `refused` exits non-zero, `absent` is a named state. This
tree's §52 table has "Exit test" prose for sixteen milestones and no reader. Nothing would have
refused eight hours of harness work while `CHAT-02` stayed unstarted.

### F7 — `secure-shell`'s own gaps are real, and this tree's persistence fills one of them (positive)

`README.md` says the ledger service and watcher are built but "the client test surface and the toast
stack are not built yet". The ledger service has no crash story comparable to what now exists here: a
checksummed transition journal, generation-based atomic snapshots, a fallback generation, and four
observable conditions with refusal-on-unreadable. That pattern is worth carrying *into* the ledger
service (its own commits) rather than maintaining two idioms.

## 3. The Java package streamer (the path from here to the port)

The user's ruling for this work: `secure-shell`'s modern, incrementing formats are the Java shell's
package streamer, bot development ports onto that surface, and the toast addon package is the first
downstream window target for the Java client.

What the Java client may read, and what it may not:

| Format | Owner | Java client |
|---|---|---|
| `secure-shell.served/1` (window records, content tree, evidence rows, tier computed on read) | `tools/port/served.mjs` | **reads** — this is the stream the in-game client consumes |
| `surface.mount/2` (receipt: surface, sequence, digest, state, windows, at) | vendored `dl-enclave` | reads only as the receipt/sequence, never re-declares it |
| `dl-enclave.surface/1` (window/content declaration) | vendored | consumes; the toast is `packages/toast/toast.surface` |
| `secure-shell.ingest/1` (server accept edge) | `server/ingest.mjs` / `decl.mjs` | **never reads** — it terminates at the server |
| ledger events (HTTP/SSE, bearer token) | `server/http.mjs` | reads; never writes |

Two rulings are still needed before a line of Java renderer is written, and neither may be assumed:

1. **Display owner for the in-game client.** `AGENTS.md` §2.2 allows one display implementation. The
   Openlock fork is the desktop display. An in-game HUD that draws the same `served/1` window records
   is a *consumer* only if it owns no palette, no second window format, and no second frame; the
   moment it invents chrome or tokens it is `display-second-implementation`. The audit's position:
   declare it a consumer, ahead of the code.
2. **Motion.** `docs/MOTION.md` §3: the toast provider supplies frame content and adds no curve of
   its own. The Java side draws the stack; the package supplies `$wish`.

First window, smallest real slice (each step a record with a command, exit status, check count and
digest, not a screenshot):

1. **Declare** — already done: `packages/toast/toast.surface`, lane `beacon`, rule `small-right`, no
   chrome, no moves, content `$wish`.
2. **Mount and receipt** on the backend; the client never learns the package tag.
3. **Serve** `secure-shell.served/1` on the existing authenticated listener.
4. **Java client connects** to the same route with the bearer token and subscribes to ledger events.
5. **One refusal toast** rendered from a real ledger event — `prompt-ledger` refusal is the declared
   first case in `DECISIONS.md`, so the content already has a producer.
6. **Witness in the client**: the toast shows the event's type, subject and digest; a check replayed
   from the client reports command, exit status, check count and digest. A screenshot alone is
   refused.

For the CUA path the same three bounds apply, unchanged: a pilot's action is bounded by the declared
action set (the narrower of driver and set wins); laya's judge role and its controller role are
separate grants; von may pilot and may not adjudicate. Every CUA action carries a ticket, a named
presenter and a ledger row, refused before it runs when the ticket is absent.

## 4. What to do, in order

1. **Put this tree under git**, one commit per unit (server, clients, persistence, commands, docs),
   each message naming the command and digest that verified it. H1's exit test, applied here first.
2. **A minimal project ledger** — append-only JSONL beside `journal.log`, one entry per deliverable:
   command, exit status, check count, artifact path and digest; `CHECKPOINT.md` generated from it.
   Reuse the journal's discipline (CRC, idempotent ids, fsync before reporting).
3. **A gate reader for §52**: refuse `claim-without-record`, `harness-only-pass` (a client-visible
   claim whose only evidence is a server log), `step-without-exit`, and a commit over the size cap.
4. **Record the §19 rulings** with rejected alternatives before more autonomy code: world-mutation
   scope, death policy, gender schema, test-mode boundary.
5. **Then the toast** as the first Java window over `served/1`, with the display-owner and motion
   rulings recorded first.
6. **Then move bot development onto the stream**, one window at a time, CUA actions ticketed.

## 5. What does not transfer

- The ledger service is Node/Debian; the Java client is a consumer of its HTTP/SSE, never a second
  store, and never MongoDB.
- No second window format where mount v2 / `served/1` already carries the stream.
- No second palette: tokens come from the landing-page addon theme and are frozen in `baselines/`
  before the first render.
- A browser page may preview a declaration; it may not be cited as a pass for the Java client.

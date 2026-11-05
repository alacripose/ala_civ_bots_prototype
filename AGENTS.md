# Standing conventions

Read this before changing anything. `CONTEXT.md` owns the vocabulary, `CHECKPOINT.md` owns the
evidence trail, and this file owns how work is done here.

## Push early and often

Commit and push coherent work as it lands instead of accumulating a local branch. A push is how a
change becomes reviewable and how another session can pick it up without guessing what exists only
on this disk. Push a change once its own check passes; do not park finished work locally waiting
for a larger batch. Never push a build that passed only because a gate was excluded: if a gate
refused, the refusal is the result to fix or report, not something to route around.

## Track work through tickets and pull requests

Every non-trivial change has a ticket naming the problem and the acceptance evidence, and lands as
a pull request rather than a direct commit to the shared branch. The ticket is where the refusal or
the requirement is stated; the pull request carries the diff, the checks that were run and the
evidence they produced. Small drive-by fixes may go straight in, but anything that changes a
contract, a store, a gate or a game system gets a ticket first. Reference the ticket in the commit
subject.

## Build the maximal system now; trim API surface later

Implement the full contract while the system is being built: every state, every refusal, every
store and every path the design names. Do not pre-trim surfaces, options or commands to keep the
first pass small, and do not leave a named state unimplemented because the common path does not
reach it. Cutting API surface is a later, deliberate pass with its own ticket; it is not a reason
to ship a thinner system now. When the full shape is in place, name what is missing rather than
silently omitting it.

## The gates are not advisory

`./gradlew build` refuses when the ingest proof, the Mongo projection or the installed-jar parity
does not hold, and `scripts/devserver.sh` refuses to take the port for the same reasons. Those
refusals exist because each failure has already happened here. Fix the cause or report the refusal;
do not add `-x`, `ignoreExitValue`, a skip flag or a hand-placed jar to get past one.

## 1. Formats: upgraded and legacy

This project carries formats at their own revisions, and the audit tools inventory them rather than
trusting a summary: `scan_drives_reaudit.py` in `secure_shell_v2` reads dotted schema identifiers and
package magics across every project, and its register is checked rather than described.

Read here on 2026-11-04 under `C:\Users\ala\Documents\ala_civ_bots_prototype` (head `a31cce2`):

- identifiers: `surface.mount/2`x22, `secure-shell.served/1`x22, `secure-shell.ledger/1`x17, `dl-enclave.surface/1`x13, `civilian.axis-cells/1`x11, `secure-shell.ingest/1`x10, `docs.gradle.org/8`x8, `civilian.decision/1`x8, `civilian.ticket/1`x6, `secure-shell.admission/1`x3, `dl-enclave.sig/1`x3, `secure-shell.port-baseline/1`x2
- magics: none read

Upgraded versus legacy, by family:

  - `docs.gradle.org`: newest `docs.gradle.org/9`, older `docs.gradle.org/8`
  - `secure-shell.served`: newest `secure-shell.served/2`, older `secure-shell.served/1`

A legacy value is evidence to read, never a value to convert silently: an older frame is refused by
name, and `dl-enclave` §1 and §3 own that rule for this family.

## 2. Status to the shell connection

This project's connection state, from commands on 2026-11-04, not from its own README:

| Question | Answer |
|---|---|
| Does a running unit, admitted package or drill step name this project? | `absent` |
| Evidence | no admitted package, running unit or drill step names this project |
| Layers this project keeps | `scripts`, `packages`, `docs` |

`held` here means the named command produced the named result. `absent` means nothing proved it yet;
it is not a judgement about the project's quality, and it changes only when a command changes it.

## 3. Resynthesizing this file

This section was appended by `secure_shell_v2/tools/resynth_agents.py`, which refuses
`agents-authority-trimmed` rather than write a file that lost a prior line. A resynthesis of this
project adds: its formats at their revisions, its connection state, and its conventions as the file
already states them. It does not delete, re-word or "tidy" an existing rule; a stale claim is
superseded by a dated line beside it.

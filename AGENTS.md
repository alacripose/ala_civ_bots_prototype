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

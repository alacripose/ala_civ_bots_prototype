# Non-JVM resident

> **Live path (2026-11-02):** `src/main.mjs` is a **motor-only mineflayer body**. The server-side
> mind makes the decisions; Axis is not run here. The older `mind.mjs` / `capabilities.mjs` files
> are not imported by this entrypoint. The file motor is local-only; deployment requires a network
> motor transport, while the Node body itself connects directly to the Minecraft server.

A resident whose mind runs in its own Node process, connected to the shared Fabric server as an
ordinary player. The server owns world truth; this process owns perception, choice and action for
one body.

## What decides

There is no task list in this directory. A **capability** is something the body can physically do
(mine, craft, eat, rest, store, deliver, explore, defend). The corpus chooses the word for each
capability: `Lexicon.prefer` answers with the most-used attested candidate, exactly as the JVM's
`Realizer` does. A capability the corpus cannot name produces no task.

Each decision tick the runtime:

1. perceives through its own connection (`perception.mjs`);
2. generates affordances from the world — visible blocks, held items, recipes that can actually be
   made, containers, people, threats (`capabilities.mjs`);
3. states the chosen affordance as a claim in the corpus's procedure shape,
   `Name: EXECUTE [verb object count]`, and refuses the tick if `ProseLogic` cannot read it;
4. carries the capability out through the client's ordinary player path and verifies the result from
   its own inventory or world state (`body.mjs`).

`journal.jsonl` holds the heard frames, the chosen frames (with `verb_basis`, `shape_basis`, the
score and the runners-up), and every outcome. A reader can check why a resident did something
without asking it.

## Run

```bash
cd tools/residents
npm install
node src/main.mjs --name Ash --host 127.0.0.1 --port 25565
```

Speak to it in ordinary chat. With its name in front, it can take an order:

```
bring me 20 oak logs
mine 8 stone
craft a wooden pickaxe
what are you doing
stop
```

The parser reads the verb at its grammatical position and judges it against the compiled lexicon.
An unknown word is reported as unknown; an ambiguous order is answered rather than guessed.

## Hand-off from the JVM

Put the resident's name in `run/world/ensouled/external-brains.txt` (one per line, `#` comments).
`AgentManager.bindNetworkedPlayer` sees it, logs `external-brain resident`, and neither mints nor
binds a server-side mind. The persisted JVM mind stays on disk untouched. `ParticipantRegistry`
still classifies the session as an agent, not a person.

On first boot the resident adopts the `AgentId` and home the world already recorded for its name
(`run/world/ensouled/work.json`, `domain.json`), so a move does not mint a second person. Its own
memory lives in `run/residents/<name>/mind.json` and survives restarts.

## Limits

- Beauty: this is a mineflayer body, not the headless Fabric client. It exercises the ordinary login
  and player-input path; it does not load client-side mods, HUD or screens.
- Speech is composed from attested words, but the full corpus classifier lives in the JVM. The
  procedure shape and verbs are checked here; deep claim-shape scoring is only used for heard lines.
- The server's `/ensouled list` does not yet show external residents; the journal and the server's
  own participant log are the record.

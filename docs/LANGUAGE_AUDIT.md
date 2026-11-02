# Audit: where the agent's language comes from

Asked for: every remaining place where a being's language is written by hand instead of composed from
the corpus. Audited 2026-11-01 against the sources of a being's *speech* (`say`), not against log or
stream text, which is the system describing itself.

## The rule

A being may speak only what the corpus attests (`Lexicon`), arranged by the being's own reading of its
own body and the world (`Realizer`), and — where it is making a claim — admitted by the corpus's claim
shapes (`ProseLogic` through `Talk`). A sentence this project invents is a sentence the world never
said; the design record treats that as a failure, not as flavour.

## Producers of a being's speech

| Producer | How the words are chosen | Verdict |
|---|---|---|
| `AgentMind.murmur` | `Realizer` over the corpus for every slot; the item name is the world's (`Realizer.worldName`) and the numbers are the body's own | corpus |
| `AgentMind.respondTo` | `Talk.candidates` → `Talk.ordered` → `ProseLogic.classify`; an unclassifiable line is dropped as `unclassified_prose`, never spoken | corpus, and gated |
| `AgentMind.considerWishes` — taking work on | `Talk.owing` → `Realizer` | corpus |
| `AgentMind.considerWishes` — answering "what is the plan?" | `Talk.owing` over the commitment's own count and kind | corpus |
| `AgentMind.considerWishes` — asking for the missing half | `Talk.askHowMany` / `Talk.askWhat` → `Realizer` | corpus |
| `Talk`, `Realizer`, `Lexicon` | the whole vocabulary, compiled by `tools/prose_compile.py` | corpus |
| Skills (`skills/*Routine`) | never speak: no `say`, no `Realizer`, no literal line | n/a — nothing to audit |

Every one of those composes through `Realizer`, so a meaning the corpus cannot name makes the line
*unsayable* rather than substituted: `Talk.Line.sayable()` and `Realizer.unsayable()` carry that
through to the record (`unsayable`, `kept_quiet`).

## Hand-written English that reaches a reader, and why it is not the agent's speech

| Text | Who writes it | Where it goes | Status |
|---|---|---|---|
| `[ensouled] [System] Ash -> Bram: request 4 bread (id)` | `AgentManager.announce` | chat, only when no connected client can say the words as a player | system line; now labelled `[System]` per addendum E.3.1 |
| `[ensouled] [System] <text>` to a human session | `AgentManager` `HumanChannel` | that session's chat | same |
| `Wish wish:3 put to Ash …Nothing is spoken for it: the wish may be refused.` | `EnsouledCommands.wish` | the operator who typed the command | operator feedback, not speech |
| `work-opened`, `plan-replaced`, `nothing of that kind in sight for 600 ticks` | `WorkLedger` | the being's own stream rows and `/ensouled work` | evidence prose: it describes work, and a reader checks it against the record |
| `Refused: record - rest` | `ServedPayload`/`StreamClient` | toast | evidence prose, drawn from a refused row |

The last two are English written in code rather than composed from the corpus. They are not claims a
being makes about the world; they are the record's annotations, and a reader can check each against an
inventory, a window and a run. That is the boundary this audit draws: **a being's sentence must be
corpus; the record's labels need not be, because nobody speaks them.**

## What the audit found and what was changed

1. **A wish that was a question got a request for clarification.** "what's the plan?" and "where'd you
   go?" produced the same sentence as "hey Ash" — `What do you want me to get?` — which is not an
   answer and reads as a canned line. A question now answers from the commitment the being is
   carrying (`I shall have 5 logs.`), and the clarify question is only for a wish that asks for
   something and leaves out half of it.
2. **Two system lines were not marked as system.** `AgentManager`'s fallback descriptions of message
   traffic went out as bare `[ensouled] …` chat. They are now `[ensouled] [System] …`, which is what
   addendum E.3.1 requires of a line the server writes about an agent.
3. **A `test` row was being served as a notice**, so a client drew a toast titled `test` with the
   surface path as its body. `MongoStreams.noticeTail` no longer serves witness rows: the client's own
   witness stays in the ledger and in its log.

## The other direction: hand-written English a being *reads*

The audit above is about what a being says. The same rule has a second edge that is easy to miss: a
table of English words in code that decides what the being understands. Those cannot come from the
corpus — they are the mapping from this world's words to this world's things — but they are language,
and a reader should be able to find them all in one place.

| Table | Where | What it decides | Consequence when it is short |
|---|---|---|---|
| Deliverable nouns (`log`, `logs`, `wood`, `oak`, `spruce`, `birch`, `plank(s)`, `bread`, `apple(s)`, `dirt`, `stone`, `cobblestone`, `rock(s)`) | `work/Deliverable.Kind` | which wish names a thing this world can deliver | a wish using a word outside the table is *ambiguous*, so the being asks instead of guessing (`bring me 3 moonrocks` → `What do you want me to bring?`) |
| Question openers (`what`, `where`, `how`, `why`, `who`, `when`, `which`, or any `?`) | `AgentMind.asksAboutItsWork` | whether a wish is asking about its work rather than for a thing | an unlisted phrasing is read as a request for goods, and gets the clarify question |
| Dictation pattern (`say`, `tell … to say`, `repeat after`, …) | `AgentMind.DICTATION` | whether a wish is trying to put words in its mouth | a wording the pattern misses reaches `WorkLedger.offer` and is answered as an unclear request rather than refused as dictation |
| Quantity words (`one`…), plurals | not handled | a wish that says a number as a word | `bring me four logs` is *unclear*, because only digits are read (`Deliverable.parse`): the being asked `How many logs?` when the number was already in the sentence. Named, not fixed |

These tables are the honest boundary: the corpus supplies the words a being may *say*, and a declared
table supplies the words this world recognises when it is *spoken to*. Each row above is exercised on
the running server: `bring me some oak wood` → `How many logs?`; `bring me 8 oak logs` → work opened;
`bring me four logs` → `How many logs?`; `bring me 3 moonrocks` → `What do you want me to bring?`.

## Still hand-written, and honestly so

- `Talk`'s sentence *patterns* are fixed English grammar with corpus words in the slots. That is the
  project's stated design (grammar is not vocabulary), but it does mean the range of what a being can
  say is bounded by the number of patterns written here — not by the corpus. Adding a pattern is a
  deliberate act, and each one is visible in `Talk`.
- `Talk.askWhat` is a single question for every wish it cannot read. The words are corpus; the
  monotony is policy. Answering a wider set of questions means writing more patterns, and that is the
  next honest piece of work in this file's territory, not a corpus fix.
- The wish command's own feedback is verbose enough to fill a chat pane. It is operator text, but it
  is on the same screen as the beings' speech and competes with it.
- A number written as a word is not counted. `bring me four logs` is answered with `How many logs?`
  even though the number is right there in the sentence. Reading number words means a table of them,
  which is the same kind of table as the nouns above; it is listed here rather than quietly absent.

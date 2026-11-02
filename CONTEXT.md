# Ensouled

A persistent world of residents who speak a compiled English, act through bodies, and leave evidence
for everything they claim. This file is the vocabulary only: no file names, no field names, no design.

## Lifetimes

**World**:
The durable container whose clock, seed, schema, and history outlive every session. Closing it pauses
it; it is not deleted.
_Avoid_: server, save, instance, game

**Homebase**:
A persistent settlement inside a World, with its own claim, permissions, upkeep, inventory and
defences.
_Avoid_: base, camp, settlement, outpost

**Run**:
A bounded operation inside a World — a survey, a founding expedition, a supply campaign — with a
beginning, a staged middle, and an end that does not end the World.
_Avoid_: quest, mission, job, task

**Encounter**:
A short simulated segment inside a Run: a wave, a defence, a conversation, a single act of work.
_Avoid_: fight, scene, round

## People and bodies

**Resident**:
A being whose name comes from the corpus and whose life persists across reconnects.
_Avoid_: agent, bot, NPC, character, player

**Mind**:
A Resident's identity, memory, vocabulary and commitments. It is not the thing that moves.
_Avoid_: brain, AI, model, controller

**Body**:
What a Mind acts through — a session, a fake body, a mob. A Body can be lost without the Mind ending.
_Avoid_: avatar, puppet, entity

**Client**:
A program a person or a Resident connects with. It streams the world's format; whether it carries the
mod is an implementation fact about that Client, not a rank.
_Avoid_: bot, jar, mod, headless player

## Language

**Corpus**:
The collected English this project treats as its baseline — the claims and their quotations.
_Avoid_: dataset, training data, dictionary

**Lexicon**:
The words the Corpus actually says, with how often it says them. A word absent from it is unsayable.
_Avoid_: vocabulary list, word list

**Claim shape**:
One of the sentence forms the Corpus gives a verb: a kind and a modality. A sentence may only be
stated in a shape that exists.
_Avoid_: intent, command type, action type

**Verb**:
A word for something that can be done. It belongs to the Lexicon; it is derived where it is used, never
enumerated as a master list.
_Avoid_: action, skill, task

**Frame**:
Who meant what, on which thing, how many, to whom — the meaning of one utterance, with what could not
be placed kept alongside it.
_Avoid_: parse, intent, command, message

**Command**:
A Frame put to the World as a request that must survive being retried. It carries the revision it
expects and a key that makes a repeat a no-op.
_Avoid_: request, action, message

## Work

**Commitment**:
What a Resident has undertaken and when it stops being owed. It outlives the plan that serves it.
_Avoid_: goal, objective, desire

**Plan**:
The ordered Frames a Resident intends to satisfy a Commitment with. It is replaced when a step fails;
the Commitment is not.
_Avoid_: stack, queue, schedule

**Objective window**:
A period during which a course of action is available and after which it is not.
_Avoid_: opportunity, event, timer

**Route**:
An ordered set of places a Body can walk between, valid only for the terrain it was computed on.
_Avoid_: path, waypoint list, navigation

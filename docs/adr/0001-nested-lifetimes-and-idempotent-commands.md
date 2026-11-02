# Nested lifetimes, and commands that survive a retry

## Status

accepted

## Context

The v1 plan treated long-horizon work as a goal stack carried by one mind, and treated a resumed plan
as a plan re-read from disk. `docs/persistent_city_builder_tower_defense_design_analysis_v9.md` §A7
distinguishes four nested lifetimes (World → Homebase → Run → Encounter) and §A6.4 requires commands
to carry `expected_world_revision` and an `idempotency_key`, because a reconnect must not "found the
same base twice, spend the same resource packet twice".

## Decision

Work is modelled at four lifetimes, and every state-changing request is a Command carrying the
revision it expects plus a key that makes a repeat a no-op. A Frame is meaning; a Command is a Frame
put to the World with those two extra facts. A Plan belongs to a Commitment, a Commitment to a
Resident, a Resident to a World; no Plan crosses a World boundary or outlives its Commitment.

## Consequences

- A resumed Plan cannot double-spend: the Command's key is what makes the retry safe, not the Plan's
  checkpoint.
- Homebase and Run are first-class persisted objects, so the planner must address them by identity
  rather than by coordinates in whatever world happens to be loaded.
- Encounter-level work stays ephemeral and must be reconstructable; persisting it is a mistake the
  plan explicitly forbids.
- The frame parser stays a producer of Frames and never of Commands: revision and key are added by
  whoever has the authority to act.

# World interaction: contract, structure, and what is deliberately absent

Milestone: `EMB-02` — reusable world interaction and cooperative work (successor to `EMB-01`
fake-player embodiment). The chest is the **first implementation** of the contract, not the contract.

## 1. The contract

One seam, `dev.ensouled.interact`, in its own package so neither the motor nor the mind grows a
container branch:

| Piece | Owner | Answers |
|---|---|---|
| `InteractionTarget` | perception | perceived object identity (`chest@x,y,z`), kind, block id, distance, reachability, declared capabilities |
| `InteractionHands` | body adapter | what this body kind can perceive, and the server-authoritative operation on a perceived object |
| `InteractionRequest` | skill | the requested operation (`inspect` / `withdraw` / `deposit`), item, count, and the transfer id it belongs to |
| `InteractionOutcome` | adapter | what the server actually did: `PERFORMED` / `REFUSED` / `FAILED` with a stable code and observed before/after facts |
| `InteractionSession` | mind | the only path a skill has to an object: gate verdict first, then the operation, with both recorded |
| `AuthorityGateway` | authority | `authorizeInteraction(request, target, view)` — capability reported by an object is never permission |
| `OwnershipBoundary` | protection | `allows(agentId, target)`; the only wired provider today is `NONE` (no claim system exists) |
| `ChestInteraction` | adapter | the headless, server-authoritative chest transfer itself |

The eight distinctions the mission requires map one-to-one: identity (`InteractionTarget.objectId`),
reachability (`reachable`, re-checked by the adapter at operation time), available capabilities
(`capabilities`), permission (`AuthorityGateway`), requested operation (`InteractionRequest`),
server result (`InteractionOutcome.status`/`code`), observed postcondition (the `container_before` /
`container_after` / `inventory_before` / `inventory_after` facts), canonical event
(`MindLedger.recordContainerMove` → `container.withdrawn` / `container.deposited`, keyed by transfer
id), plus `item.transferred` recorded by the receiving mind from its own delta.

**No direct inventory mutation is an agent capability.** The only verbs are inspect, withdraw and
deposit, and each moves items through the real inventory API with reach, slot and count rules.

### Refusal codes (typed, never a silent success)

`unsupported_object`, `unsupported_operation`, `not_reachable`, `outside_home_radius`,
`protected_target`, `container_missing`, `container_full`, `inventory_full`, `item_not_in_container`,
`item_not_in_inventory`, `bad_request`, `bad_scope`, `verification_failed`, `rollback_failed`,
`storage_lost`, `stock_unavailable`, `no_body`.

A refusal is never converted into a different path that happens to work, and a partial move is rolled
back before the refusal is reported — an item is never conjured or destroyed.

## 2. Headless divergence (recorded, not hidden)

`ChestInteraction` calls the `ChestBlockEntity` inventory directly. A connected human's transfer
travels through `ScreenHandler` slot clicks, which also synchronise an open screen and fire the
menu/screen hooks other mods listen to. This adapter performs the same identity, reach, slot and
count checks and records the difference in its class documentation rather than claiming to be a real
click. A fake connection's menu path stays unproven and is deliberately **not** used.

## 3. The temporary authority, and why it is not becoming permanent

`Phase1GatePolicy` is a **provisional** implementation of `AuthorityGateway` (formerly
`ProceduralGate`), documented as such in `docs/PHASE0_CONTRACTS.md` #5/#6. The canonical Axis
enforcement point is not present in this workspace.

The shape is what keeps it temporary:

- every decision — intentions *and* world-object interactions — crosses `AuthorityGateway`;
- the only place an authority is constructed is `mind.AuthorityFactory.provisional(...)`, so replacing
  it is one edit, not a hunt;
- nothing else may construct a gate, and no capability has a private permission path;
- a permit proves permission for that request only; capability, success and utility are never
  authorization.

## 4. Cooperation

`request_supplies@1` (requester) and `supply_from_chest@1` (supplier) are two separate minds running
two separate typed state machines over the ordinary message bus:

1. the requester reads its **own** storage chest through the gated `inspect` and finds it short of the
   declared stock, so it states the requirement to a peer it can see;
2. the supplier takes the request from its own inbox, reads its own chest, and accepts only if that
   chest can cover it (otherwise a typed decline);
3. the supplier withdraws through the contract, walks to the requester and drops the stack;
4. the requester takes the drop, walks back to the chest **it** identified, and deposits — never into
   whichever container is nearest now;
5. both sides record their own verified delta under the same transfer id: one `container.withdrawn`,
   one `container.deposited`, one `item.transferred`.

Neither mind writes the other's inventory, intention or beliefs. Each acts on its own world state.
The food handoff (`request_food@1` / `give_food@1`) is unchanged and still answers a request from the
hands, not from a chest.

## 5. Participants, observers, and test isolation

Connected sessions are classified by the **agent registry**, never by a Java subclass
(`participant.ParticipantRegistry` → `AgentManager.agentIdForPlayer`):

- `AGENT` — the session is bound to a registered mind's body (an in-process fake player today, a
  network-connected bot once Addendum D binds one);
- `OBSERVER` — an authorized human (`/ensouled`-level grant or operator permission), placed in
  server-enforced `SPECTATOR` mode by `ObserverMode.apply`;
- `PARTICIPANT` — an ordinary human, allowed because strict isolation is **off by default**;
- `UNKNOWN` — only reachable when strict isolation is explicitly switched on.

`FixtureProtection` protects a running probe's blocks and region: agents may work inside it, every
human session is refused and the attempt is recorded with its reason. Nobody is disconnected for
being human. A probe never asserts "the server contains only bots" — every assertion reads its own
fixture positions, its own agents' inventories and its own agents' event logs.

## 6. Evidence (clean world, 2026-10-31)

```
./gradlew test                      # 12 tests, 0 failures
./scripts/selftest.sh chest         # one boot, two citizens, two chests, one transfer
SELFTEST PASS phase=chest chest citizens=2 transfer=msg:1 source=4->2 sink=0->2
  drawer_inventory=0 storer_inventory=0 withdrawals=1 deposits=1 transfers=1
  conserved=true refusals=not_reachable,unsupported_operation
SELFTEST PASS phase=handoff ...     # unchanged food handoff
SELFTEST PASS phase=player1 ... / phase=phase2 ...   # harvest + restart continuity
SELFTEST PASS phase=verbs ...       # jump / equip / mine / use

# attach mode (live dev server, same process, same world):
SELFTEST PASS phase=handoff run=dev-mvx0afr9 handoff citizens=2 request=msg:1 accept=msg:2
SELFTEST PASS phase=chest run=dev-mvx0ai6n chest citizens=2 transfer=msg:3 source=4->2 sink=0->2 ...
```

The chest phase is agent-only: it spawns two citizens and their chests, and neither the probe nor
the harness creates a human, observer or watcher session. A human joins the normal development
server with their own client; no probe run is evidence that a human did.

Two modes exist (`DEV-01`, delivered):

- **Attach (interactive, default).** `scripts/devserver.sh` runs the persistent server and owns
  `run/world`; `scripts/attach.sh <phase>` submits `/ensouled harness <phase>` over the local RCON
  channel and waits for that run id's result. The server, the world and every connected session stay
  up across phases. A run uses a force-loaded plot at `spawn + 128/+128`, records the bodies it
  created in `run/ensouled-harness-owners.txt`, and the next run removes only those bodies. Verified
  on 2026-10-31 in one process: `chest` then `handoff` then `chest` all passed, the listener PID and
  the world stayed the same, a sentinel block outside the plot was still there, and the server log
  recorded no shutdown.
- **Disposable CI (explicit).** `scripts/selftest.sh <phase>` boots its own server on port 25579
  (`ENSOULED_SELFTEST_PORT`) with `--universe run/selftest-universe`, so its world is never
  `run/world`. It deletes only that universe, and only when the `.harness-owned` marker the script
  writes is present; without the marker it refuses. Verified: a CI `chest` and `handoff` ran while
  the dev server was listening on 25565, and `run/world/level.dat` was byte-identical before and
  after.

## 7. Structure after this milestone

```
core/      decision cycle and its owners
           AgentMind (decisions only) · MindLedger (memory, events, evidence) ·
           MindPersistence (save tree) · AuthorityGateway (authority seam) ·
           CandidateGenerator (candidates from perceived state) · MessageBus/AgentMessage
skills/    typed state machines: harvest_tree, eat_if_hungry, request_food, give_food,
           request_supplies, supply_from_chest, request_from_human
interact/  world-object contract: InteractionTarget, InteractionRequest, InteractionOutcome,
           InteractionHands, InteractionSession, OwnershipBoundary, ChestInteraction,
           PlayerInteractionHands
body/      BodyAdapter (tickBody, createMotor, createView, interactions) · PlayerBodyAdapter ·
           MobBodyAdapter · PlayerMotor · PlayerWorldView · BlockSight
mind/      AgentManager (binding + tick) · AgentRecord · BodyMotorProxy · BodyHandsProxy ·
           AuthorityFactory
participant/ ParticipantRole · ParticipantRegistry · ParticipantHooks (join classification only)
persist/   MindStorage; command/ `/ensouled`; test-only selftest harness
```

`AgentManager` has no per-body-kind branch: stepping is `BodyAdapter.tickBody()`, hands are
`BodyAdapter.interactions()`, and the mind is body-agnostic. The acceptance harness lives in the test
source set and is loaded as a companion mod by `runServer`; the mod jar contains no harness classes.

## 8. Follow-up tickets (not started here)

- **`HUM-01` human-player communication and interaction** — delivered as a code path and prepared by
  `/ensouled humanprobe`, but not yet run with a real client; see `IMPLEMENTATION_STATUS.md`.
- **`INT-02` more container types** — barrels, shulker boxes, trapped chests, double-chest halves,
  minecart chests; each declares its own capabilities and reuses `InteractionSession`.
- **`INT-03` workstations and screens** — furnaces, crafting tables, anvils; requires a decided
  server-authoritative screen model rather than a guessed fake GUI.
- **`INT-04` claims and protection** — implement `OwnershipBoundary` against a real claim ledger so
  `protected_target` becomes a real verdict instead of a wired-open hook.
- **`INT-05` slot-policy detail** — per-slot validity, full-inventory behaviour, stack-size rules and
  rollback are proven for whole-stack moves only; partial/overflow policies are not claimed.
- **`ECO-01` trade, offers and valuation** — deliberately out of scope for this milestone.
- **Addendum D (`shared server bot client population`)** — one shared authoritative server with bots
  connecting as ordinary client sessions; `ParticipantRegistry` already resolves agents by registry
  binding, which is the seam this needs.
- **Addendum E (`agent chat and social observability`)** — player-visible agent chat and a social
  observability surface over the existing typed exchanges.
- **`CHAT-01`/`CHAT-02` native player chat** — open. Speech acts are echoed as `[ensouled]` system
  lines (`MessageBus` → `PlayerManager.broadcast`); Addendum F §F.6.3 forbids counting that, or
  `HumanChannel.sendMessage`, as player chat. Native chat needs a connected bot client that sends and
  receives ordinary chat.
- **`DEV-01` persistent development server** — delivered: `scripts/devserver.sh` + `scripts/attach.sh`
  run phases inside the live server, and `scripts/selftest.sh` is the explicitly disposable CI mode
  confined to `run/selftest-universe` (it never deletes `run/world`).
- **`INT-06` pickup determinism** — one `chest` run left the supplier's dropped stack on the ground
  next to both bodies without anyone collecting it. Two causes were traced: minds restored from an
  earlier probe world answered the request from stale inventories and picked up the drop, and a
  requester that collected the drop could walk out of its chest's perception radius and then fail
  `storage_lost` instead of walking back. The probe now clears its own world and restored minds, and
  the receiving routine walks back to the exact chest it identified. The phase has passed since; the
  delivery step still reports only counts, not the loose stack's position.

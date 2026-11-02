# Phase 0 — canonical contracts (recorded)

Source plan: `minecraft_ensouled_system1_cua_architecture_v3.md` §17 Phase 0, §31 Phase 0 and
Addendum B §34–§38 (entity-AI transition).

Status: **partially resolved**. Items marked *blocker* need the project owner's ruling before a
later phase may claim that contract. Nothing in code claims a contract that is not selected here.

| # | Question | Decision recorded for this slice | Status |
|---|---|---|---|
| 1 | Minecraft target | **1.20.1**, Fabric loader `0.16.14`, Yarn `1.20.1+build.10`, Fabric API `0.92.12+1.20.1`, Loom `1.6.12`, Java 17 target (built on JDK 21, Gradle 8.8 wrapper) | resolved by explicit project instruction |
| 1b | Loader choice | Addendum B provisionally names 1.21.1 + NeoForge. The version instruction (1.20.1) overrides it, and **Fabric** was chosen as the 1.20.1 dev/runtime toolchain. If the project must run NeoForge for its modpack, this is a **blocker**: the cognition core is loader-independent, but the adapter layer is Fabric-specific. | assumed — confirm |
| 1c | Transition-roadmap position | Mod-owned civilian entity (Addendum B Phase 2): custom entity, no vanilla goals, System-1 is the only decision owner. No vanilla GoalSelector/Brain takeover adapter exists yet. | resolved for slice |
| 2 | Server topology | Authoritative dedicated server (or integrated server). Client is presentation only; no decisions on the client. | resolved |
| 3 | Body contract | **Fake player is the primary body** (Addendum C): `FakePlayerBody` is a real `ServerPlayerEntity` on a silent connection, with real collision, reach, block hardness/tool math, drops, pickup, inventory and hunger. The earlier `AgentBodyEntity` (virtual-player adapter, §10.2 B) remains as a secondary adapter behind the same seam. Rendered-client CUA (§10.2 A) is **not** implemented. | resolved for slice |
| 3b | Player-body semantics (§C.4) | No teleporting and no direct block/inventory mutation. Movement is the shared player physics stepped by the body itself (vanilla server players take their position from a client); breaking is real `START`/`STOP_DESTROY_BLOCK` player actions timed from `calcBlockBreakingDelta`; `jump` holds the vanilla jump flag; `equip` sends the client slot-selection packet (hotbar only); `use` resolves through `ServerPlayerInteractionManager.interactBlock`, the same call the block-use packet makes, because the packet path silently drops interactions for a client-less body. Divergence from a rendered client is limited to the absent client, and the parity benchmark is still outstanding. | resolved for slice; parity outstanding |
| 4 | Perceptibility | Senses limited to 16-block sight radius with a collider raycast; other agents' inventories and hidden health are never exposed. Block stimuli of interest: natural logs (tag `minecraft:logs`) and hostile entities within 8 blocks. | resolved for slice |
| 5 | In-world autonomous mutation (§11.7, §19.1) | **Provisional, conservative ruling** (requires owner confirmation): a being may *gather natural logs* inside a 64-block home radius with a 64-harvest budget, *eat its own food*, *ask a peer for food or answer such a request* (a handoff spends no harvest budget and moves only the giver's own stack), and *inspect, withdraw from or deposit into one chest* inside that home radius in bounded batches (≤ 8 items per operation, whole-stack moves only). Every other world mutation and every other container type is refused by the gate. | **blocker** — owner ruling outstanding |
| 6 | Axis / Governor integration | The canonical Axis implementation was not available in this workspace, so `Phase1GatePolicy` implements the local seam `core.AuthorityGateway` with typed verdicts (`PERMIT` / `REFUSE` / `HOLD`), stable action hashes and explicit reason codes. Intentions **and** world-object interactions both cross that interface; `mind.AuthorityFactory` is the only place an authority is constructed. It is a stand-in and **must be replaced** by the project's Axis enforcement point — see `docs/WORLD_INTERACTION.md` §3. | **blocker** |
| 7 | Gender ontology | Carried as opaque `genderValue` + `genderSchemaRef`, default `""` / `"unspecified"`. No semantics invented. | open |
| 8 | Identity schema | `agent:<uuid>` immutable id, display name, created epoch, birth tick, lineage note. Continuity checked on restore. | resolved for slice |
| 9 | Action vocabulary | `move_to`, `look_at`, `jump`, `equip`, `use`, `mine_block`, `collect_drops`, `drop_item`, `eat`, `move_away_from`, `wait`. `jump`, `equip` and `use` were added on explicit project-owner instruction (deliverance pass); `drop_item` was added for the two-citizen handoff and is the player's own drop path (no inventory write). Any further verb still requires a governed decision. The mob body deliberately keeps the earlier subset. | extended by owner instruction |
| 10 | Package owner / streamer | Not used in this slice; no second package format invented. | not started |
| 11 | Communication grammar | Minimal typed envelope only: `request` / `accept` / `decline`, each carrying the item, count, the subject message id, a refusal reason, creation/expiry and delivery state, in per-recipient inboxes that are **not persisted across restart**. The same envelope carries both the food handoff and the storage-backed resource request; the item class routes it to `give_food` or `supply_from_chest`. No speech acts beyond these three, no audience channels, no chat text (Addendum E is queued, not started). | minimal slice |
| 12 | Scale target | One foreground body. Perception scans a 33³ cube for log stimuli; known bottleneck for later population phases. | not started |

## Facts the code depends on

- Block breaking uses the vanilla formula: `speed / hardness / (canHarvest ? 30 : 100)` per tick,
  with `ItemStack.getMiningSpeedMultiplier` and `isSuitableFor` (1.20.1 mechanics).
- Reach is the survival block reach, 4.5 blocks, measured from the eye position.
- A drop enters the inventory only when it physically touches the body (bounded box + 0.6).
- 1.20.1 `ServerPlayerEntity.tick` never runs `tickMovement`; position, gravity and step-up come from
  the client. A client-less body must therefore step the shared player physics itself.
- 1.20.1 block breaking is accepted only from a `START_DESTROY_BLOCK` followed by a
  `STOP_DESTROY_BLOCK` after enough elapsed ticks; a start alone never breaks a block.
- Block use resolves to `ServerPlayerInteractionManager.interactBlock`; the client packet wrapper
  around it silently refused interactions from the client-less body on a booted server.
- Slot selection is valid only for hotbar slots 0–8 (`PlayerInventory.selectedSlot`).
- A damageable tool loses durability only when the block break is applied by the interaction
  manager, not when the motor merely acknowledges.
- The world is the authority: every skill's success is a comparison of observed world/inventory
  state before and after, never the motor's acknowledgement.

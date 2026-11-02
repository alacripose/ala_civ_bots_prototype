# Ensouled Minecraft Civilization: System-1 Cognition, City-Builder Simulation, CUA Embodiment, Axis Governance, and Dynamic Story Architecture — V3

**Document type:** Integrated research and architecture proposal for a separate Shell project consumer  
**Status:** Proposal / research synthesis. Not a binding project decision, implementation contract, or verified runtime.  
**Research date:** 31 October 2026  

**Primary references supplied:** VoxelMind — `https://www.youtube.com/watch?v=-63kuwbuTuo`; Ish — *1000 Players Simulate Civilization: Rich & Poor*, `https://youtu.be/ef568d0CrRY`  
**Design basis:** the persistent city-builder / tower-defense synthesis (V9) is now treated as the actual design of the civilization simulation the agents inhabit—not merely background architecture. Its city graph, production chains, hybrid logistics, telemetry, ecology, population needs, construction, defense pressure, expeditions, persistent settlements, and storyteller become the agents’ lived world. The VoxelMind / Ish references inform embodiment and civilization-scale scenarios; the user's Axis, campaign, civilian, and feeling-wheel conventions govern identity, action, evidence, and history.

> **V2 integration correction:** the city-builder V9 is now the direct simulation specification for agent life and civilization-scale behavior. Sections 22–33 map its production, logistics, ecology, defense, settlement, persistence and storyteller systems into the agents’ actual world and CUA work loop. The earlier sections remain the cognition, identity, social protocol, Axis and body foundation.

> **Central proposal:** build a persistent, embodied Minecraft civilization whose members are driven by a bespoke, non-LLM System-1 cognition engine. The city-builder V9 design supplies the simulation substrate: every agent inhabits a connected settlement/world graph with food and material needs, production dependencies, stock and transport states, work and construction, ecology, infrastructure, defensive pressure, exploration, and civic relationships. Each agent perceives only what it can sense or legitimately learn, forms local beliefs, selects a bounded intention, and performs the actual work through a CUA body subject to Minecraft physics and mod rules. They do not use LLMs to reason about Minecraft, choose actions, communicate, or operate the world. Society and story emerge from material dependencies and observed consequences. Axis and the Governor remain separate from cognition and determine what evidence supports a claim and what actions are admissible.

---

## 1. Executive findings

The two supplied video references point toward complementary strengths.

- **VoxelMind is an embodied-agent reference.** Its useful ideas are the real player body, perception/action/result feedback, persistent memory, an action library, navigation that respects Minecraft movement and terrain, and immediate hard-coded reflexes underneath a slower decision loop. The critical departure is that VoxelMind's documented decision path still asks an LLM to select the next action. That point is replaced here with an entirely non-LLM cognitive controller.
- **Ish's 1,000-player civilization is a social-simulation and event-design reference.** Its useful ideas are population scale, resource asymmetry, constrained shared geography, social organization, competing interests, scarce opportunities, irreversible consequences, and emergent collective history. The edited video is a narrative presentation, not a canonical event log: its description explicitly discloses that some footage or plot was modified, reshot or fabricated for clarity. Any implementation should learn from the scenario design, not treat the edit as a verified transcript of every participant's behavior.
- **The user's city-builder V9 is the behavioral substrate, not just inspiration.** Production graphs, on-hand/reserved/in-transit/network-visible stock, logistics edges, energy and fluid carriers, district services, construction capacity, ecology and threat pressure are the actual state variables that create an agent's daily decisions. The agent must be able to work inside those systems through the world, not merely receive a list of survival tasks.
- **The user's campaign doctrine supplies the long-horizon/real-time split:** a bounded campaign stores goals and worked-out routes; the fixed tick spends precompiled steps without a generative model in the loop. The System-1 controller may interrupt when local facts invalidate a precondition, but it must not wait for an LLM to decide the next movement or task.
- **The attached feeling wheel provides a defined affect vocabulary.** It should be carried into the agent's persistent state as affect, not turned into a permission mechanism or a superficial cosmetic meter.
- **Axis should remain a set of distinct questions.** Evidence grade is about a claim; certification is about a body of work meeting a requirement set; the procedural gate is about a particular action now. Work grade and source-evidence class are additional separate ladders in the project doctrine. They must not be collapsed into one “agent score.”

The architecture should not be “an LLM in a Minecraft player wrapper.” It should be a small, real-time artificial creature with a Minecraft body, a local sensory world, persistent continuity, learned skills, social knowledge and governed action.

### 1.1 The most important distinction

There are two different products that are easy to confuse:

1. **A character that talks about playing Minecraft.** It receives a summary or tool response, asks a model what to do, and sends a command to the game.
2. **An entity that lives inside Minecraft.** It has a body, perceives local conditions, possesses needs and skills, forms beliefs from observations, acts through the same physical rules, experiences consequences, remembers what matters, and participates in a society.

This proposal is for the second. The first may be useful for a remote operator or debugging assistant, but it is not the civilian cognition loop.

A second distinction matters: this is **not merely a crowd of survival bots**. The agents are inhabitants of the V9-style persistent city-builder simulation. They should have economic lives, work, households or social ties as defined by the selected world rules, resource obligations, skill development, local institutions, ecological dependencies, and a stake in whether their settlement functions. Minecraft is the physical substrate; the city-builder simulation defines much of what makes the shared world a civilization.

### 1.2 What “ensouled” means in this engineering document

“Ensouled” is treated as a project-level ontology: each being has a persistent identity and continuity across ticks, sessions, locations, relationships, memory changes and life events. The architecture must not treat a civilian as a temporary model call, disposable worker process, or replaceable prompt context. Death, where it exists in the selected ruleset, is an in-world event with consequences and a durable historical record—not a convenient way to reset a stalled agent.

This is an implementation convention, not a scientific assertion that a simulated entity has human subjective experience. The system should represent its own state honestly and avoid making claims about inner experience beyond the model's declared ontology.

---

## 2. Research basis and what is being transferred

### 2.1 VoxelMind: retain the body and closed feedback loop; replace the chooser

VoxelMind's public technical material describes a recurring loop of perception, decision, action, verification and memory. Its Minecraft companion is represented as a real server-side player entity rather than a detached command script. The implementation material describes a player-like connection/body, normal movement and physics, navigation over standing positions, interaction through hands, inventory and block operations, and result records derived from observed game-state changes.

The useful architectural lesson is **embodiment plus feedback**:

- Perception should be grounded in what the agent can sense.
- Actions should be expressed as an executable body operation.
- The game should remain the authority on whether the action succeeded.
- The next decision should begin from the observed result, not from the previous action's intention.
- Persistent memory should carry important facts, places, episodes and relationships across sessions.
- Immediate threats should be handled by a short deterministic reflex path rather than waiting for a slower deliberation process.

The key difference is the decision authority. VoxelMind's public documentation describes the LLM as the place that chooses the next action, even when lower-level code handles reflexes and mechanics. This proposal removes that central LLM call altogether. It does not simply replace the model's output with a different text prompt, or add a second model that translates its response. It replaces the decision representation and execution contract.

**Borrow:** real embodiment, bounded action calls, result verification, local perception, persistent memory, path execution, action-result feedback and reflexes.  
**Do not borrow:** LLM-selected action tools, LLM-generated inter-agent speech, LLM interpretation of game rules, prompt memory as the agent's identity, or a natural-language instruction as authorization.

A subtle but important feature of the documented Minecraft implementation is that movement and interaction are constrained by the game: the body has collision and movement behavior; navigation reasons about reachable standing positions; block interactions have to occur through valid hand operations; and placed-block ownership can protect player-built structures. These constraints are more valuable than an “AI can do anything” command layer. The new controller should preserve them and extend the ownership/provenance concept to all consequential world edits.

### 2.2 VoxelMind's later architectural lesson: one decision owner

VoxelMind's GEN2 material describes a simplification after earlier layers and tool-routing components caused the agent to lose task ownership and pursue distracting opportunities. The stated direction is to keep the decision point singular, execute a bounded action, and verify it.

That lesson still applies when the chooser is not an LLM. There should be **one authoritative intention arbiter per agent**. Sensory systems, need systems, relationship systems, habit activators, navigation and skill routines may propose activations or report facts; they must not each seize the same body independently. The arbitration system selects one dominant intention and one compatible motor program. Reflexes may interrupt it only through declared higher-priority conditions.

This is not one giant monolithic class. It is one decision owner over concurrent inputs.

### 2.3 Ish: scenario structure, not a scripted outcome

The supplied Ish video is *1000 Players Simulate Civilization: Rich & Poor* (2025). The premise places a large participant population into a deliberately asymmetric environment: one island is materially abundant and another is comparatively barren. Shared constraints and geography make trade, migration, conflict, coalition formation, leadership, exclusion, cooperation and resource control consequential.

Useful design patterns:

- **Asymmetry produces interdependence.** Different starting conditions create reasons to exchange, negotiate, migrate, compete or organize.
- **Geography is a social rule.** Distance, chokepoints, water, visibility and transport cost shape who can meet and who can help.
- **Scarcity creates politics.** A shared material bottleneck can become an institution, a monopoly, a trade good, a war aim or a reason to cooperate.
- **Rules define the story's possibility space.** Constraints on violence, mobility, equipment or dimensions make different social behaviors viable.
- **Population history compounds.** Early decisions create structures and reputations that later participants inherit.
- **Irreversibility makes events matter.** A lost home, a broken agreement, a death or an abandoned settlement can redirect the future.

The video description also discloses editorial intervention. Therefore, no particular cinematic scene should be treated as reliable evidence that a real participant performed an exact action unless it is corroborated by raw event data or an unedited source.

**Transfer to Minecraft agents:** generate the conditions for a civilization and let the agents' physical actions and social systems determine its history. The story engine may create pressures, reveal consequences and present records, but it must not secretly assign a protagonist, force a faction to betray another, or manufacture a “plot beat” that did not occur in the authoritative simulation.

### 2.4 The non-LLM embodied AI precedent

Rodney Brooks' subsumption / reactive robotics work is a relevant historical reference because it treats intelligent behavior as grounded in perception and action, incrementally composed from behavior-producing systems, rather than making all competence depend on a central symbolic planner. It supports the design direction of fast, local, reactive skills that interact with a real environment. It does not, on its own, specify the full persistent memory, social identity or narrative architecture proposed here.

Voyager is a useful contrast rather than a direct template: it demonstrates the value of a compositional, growing executable skill library for Minecraft, but its task generation and skill-improvement loop use GPT-4. This design retains the skill-library concept while replacing the LLM-dependent curriculum and code-generation path with typed skill acquisition, predeclared learning rules, safe experimentation and measurable mastery.

### 2.5 Source confidence boundaries

| Source | What it supports | What it does not establish |
|---|---|---|
| VoxelMind technical page and GEN2 article | Product-described agent loop, reflex layer, perception, action, verification and memory design | Independent proof of every runtime claim or proof that its LLM architecture meets this project's no-LLM rule |
| VoxelMind public Minecraft repository README | Described player-body, movement, navigation, action and memory implementation | Security assurance, complete current compatibility, or performance at 1,000 concurrent agents |
| Ish 2025 video and description | Scenario premise, public rules and creator disclosure about editorial alterations | Canonical event-by-event truth for every depicted scene |
| Brooks 1991 paper | Reactive perception/action architecture as a foundational design precedent | A finished Minecraft population or social civilization model |
| Voyager paper/site | Compositional skill-library and lifelong-learning precedent | A no-LLM system; Voyager expressly uses an LLM |
| User's project doctrine | The required campaign, Governor, ticket, evidence, civilian persistence and Axis distinctions | Proof that every convention has already been implemented in the target Minecraft mod |
| Attached feeling-wheel skill | The user's explicit affect hierarchy and operational state vocabulary | A biological or psychological claim about the simulated agents |

---

## 3. System boundaries: five planes that must not collapse

The system should be organized into five explicit planes.

### 3.1 World plane — Minecraft is the authoritative physical reality

The Minecraft server owns:

- block and fluid state;
- entity position, collision and velocity;
- time, weather, light and dimension;
- damage, hunger, effects, inventory and equipment;
- crafting, smelting, farming, redstone and modded mechanics;
- ownership/provenance of structures and objects;
- the actual result of every body action.

The cognition engine may predict outcomes, but it does not get to declare them. A planned jump is not a successful jump. A proposed trade is not a completed trade. A path is not a reached destination. An action is complete only after the game returns an observed outcome consistent with the action's expected postcondition.

### 3.2 Mind plane — each agent is a persistent simulation entity

The mind owns:

- stable identity and continuity;
- sensory interpretation and the agent's local belief state;
- drives, needs, preferences and learned habits;
- skills and learned world-mechanics knowledge;
- affect and attention;
- autobiographical, episodic, semantic and procedural memory;
- relationships, reputations, promises and social expectations;
- the current intention and local action-selection state.

It does not own game truth or permission to perform arbitrary world mutations.

### 3.3 Body / CUA plane — physical control, not privileged command execution

The body converts a selected motor program into a constrained series of player-like inputs. At minimum, its vocabulary should include:

- move forward/back/left/right;
- jump, sneak, sprint and swim strokes;
- look yaw/pitch, bounded by turn speed;
- attack, use/interact, place, mine and drop;
- hotbar selection and inventory navigation;
- crouch/stand, mount/dismount and other versioned body actions.

A body action should be a finite-duration input or motor primitive with an expected observation and a timeout. It should not be an unconstrained server command that instantly teleports, gives items, opens containers remotely, reads all chunks, or changes blocks without going through the ordinary game mechanics.

### 3.4 Governance / Axis plane — evidence and permission, not cognition

Axis is a separate enforcement layer. It should not choose the agent's desire, invent its beliefs or replace its motor controller. It checks what can be established and whether the proposed action is admissible under the current rules.

### 3.5 Story / history plane — derived from actual events

The story system derives chronicles, political histories, settlement histories, personal memories, legends and reports from events that happened. It may create scenario conditions and bounded opportunities, but it may not rewrite the event stream or make its own prose authoritative.

---

## 4. Proposed architecture

```text
                 MINECRAFT SERVER — AUTHORITATIVE WORLD
             blocks / entities / physics / mods / time / rules
                                │
                perceptible state + action result events
                                │
                                ▼
┌────────────────────────────────────────────────────────────────┐
│                         AGENT BODY                              │
│ camera / HUD / sound / local proximity / proprioception         │
│ motor inputs: movement, look, attack, use, place, mine, slots   │
└───────────────────────────────┬────────────────────────────────┘
                                │
                                ▼
┌────────────────────────────────────────────────────────────────┐
│                     SENSORY / PERCEPTION                        │
│ scene parsing · entity/object detection · affordances           │
│ sound localization · HUD reading · change detection · uncertainty│
└───────────────────────────────┬────────────────────────────────┘
                                │
                                ▼
┌────────────────────────────────────────────────────────────────┐
│                    PERSISTENT AGENT MIND                         │
│ identity · gender record · affect · needs · drives               │
│ beliefs · episodic memory · semantic world knowledge             │
│ relationships · skills · habits · commitments                    │
└───────────────────────────────┬────────────────────────────────┘
                                │
                                ▼
┌────────────────────────────────────────────────────────────────┐
│                  SYSTEM-1 DECISION CONTROLLER                    │
│ reflex interrupts → affordance/action candidates → activation     │
│ prediction → utility arbitration → intention lock → motor schema │
│ NO LLM · NO natural-language game interpretation · NO LLM chat    │
└───────────────────────────────┬────────────────────────────────┘
                                │ candidate action envelope
                                ▼
┌────────────────────────────────────────────────────────────────┐
│                      AXIS / EXECUTION GATE                       │
│ evidence for prerequisites · typed scope · current authority      │
│ procedural predicates · action hash · refusal / hold / permit     │
└───────────────────────────────┬────────────────────────────────┘
                                │ admitted motor program
                                ▼
┌────────────────────────────────────────────────────────────────┐
│                         CUA DRIVER                               │
│ bounded input frames · interrupt handling · action timeout       │
│ game feedback · postcondition verification · idempotent retry    │
└───────────────────────────────┬────────────────────────────────┘
                                │
                                ▼
                 MINECRAFT SERVER — PHYSICAL RESULT
                                │
                  event / evidence / memory updates
                                │
                                └───────────────► next tick

Separate, event-driven social plane:
perceive speech/signals → typed speech-act interpretation → relationship
update → template/grammar realization → audible/local in-world message.
No LLM on either side of the communication boundary.
```

The architecture has one decision owner per body and one world authority. A pathfinder, sensory parser, memory store or skill routine may provide data and candidates, but must not independently operate the body.

---

## 5. The System-1 decision model

“System 1” is used here as an engineering label for a fast, low-latency, low-deliberation controller. It is not a claim that the implementation reproduces the complete human cognitive process. The design goal is fast, situated, adaptive behavior without a generative language model.

### 5.1 The decision cycle

Each agent repeats this bounded cycle:

1. **Sense:** receive new local observations, body feedback, events and interruptions.
2. **Update:** reconcile current observations with the agent's previous belief state.
3. **Notice:** activate salient threats, opportunities, needs, social cues and remembered locations.
4. **Generate candidates:** retrieve applicable affordance-action pairs and already learned motor schemas.
5. **Predict:** estimate short-horizon outcomes using a local causal model of known Minecraft mechanics.
6. **Select:** choose one dominant intention, subject to fixed tie-breaking and hysteresis rules.
7. **Gate:** submit a typed action envelope to the current procedural authorization boundary.
8. **Act:** execute a bounded motor program through the CUA body.
9. **Verify:** compare the expected and observed state transition.
10. **Learn:** update the skill, memory, belief, affect, relationship and campaign evidence records from the actual result.

The agent does not need to serialize a natural-language explanation for every tick. It can record compact typed state changes and only realize speech when it has a social reason to communicate.

### 5.2 Candidate generation is not unrestricted goal generation

Candidate actions arise from explicit, versioned schemas and the current perceived world. Examples:

| Perceived state | Candidate sensorimotor schemas |
|---|---|
| Tree trunk reachable, hand empty or tool available | approach → orient → equip/hand selection → mine sequence → verify drop |
| Food low, food perceived in inventory | select food → use/eat → wait for effect → verify hunger/effect |
| Hostile entity approaching | face threat → create distance / block / attack / seek shelter |
| Unknown cave opening, safety acceptable | mark curiosity cue → approach cautiously → inspect light/sound/escape route |
| Known ally injured, safe aid available | approach → communicate → offer/use allowed aid → verify recipient state |
| Crop mature and tool/space available | approach → harvest → collect → replant if policy and resources permit |
| Settlement lacks an agreed resource | retrieve current contract/ticket → search known supply sources → collect/trade/request under scope |

Unknown situations are not solved by asking an LLM. They are handled by bounded exploration, known primitive combinations, safe experiments, conservative refusal, or a request for new authorized capability work.

### 5.3 One body, one dominant intention

The controller should have exactly one `current_intention` with a finite state machine such as:

```text
inactive → activated → committed → executing → verifying → satisfied
                                      ├──────────────→ failed
                                      ├──────────────→ interrupted
                                      └──────────────→ blocked
```

Other subsystems may submit candidate activations. They cannot run the hands while the current intention is executing. This avoids the documented failure where different layers steer a body toward incompatible immediate targets.

The controller should use hysteresis: once a valid intention is committed, a minor new stimulus should not cause a full goal switch. A severe threat, invalid precondition, newly unavailable resource or explicit social commitment can interrupt it. Otherwise, the controller finishes the current bounded action or reaches a declared reconsideration point.

### 5.4 Fast reflexes and the ordinary action loop

A separate reflex layer should handle short-latency events that cannot wait for the normal selection cadence:

- imminent fall or drowning;
- immediate attack / knockback danger;
- suffocation or burning;
- urgent food/health thresholds where the correct action is unambiguous and already permitted;
- loss of movement control or an invalid body state.

A reflex may interrupt the body only through a closed, tested, high-priority list. It cannot grant a new capability, expand the ticket, spend beyond a budget, or perform a forbidden action. The reflex layer is not an escape hatch around Axis.

### 5.5 A non-LLM activation and arbitration method

A practical first implementation can use fixed-point activation values over typed candidates. For each candidate `a`, compute an action utility from declared inputs:

```text
U(a) =
    need_relief(a)
  + current_intention_support(a)
  + commitment_value(a)
  + relationship_value(a)
  + expected_progress(a)
  + learned_habit_strength(a)
  + novelty_value(a)
  - physical_risk(a)
  - resource_cost(a)
  - travel_cost(a)
  - uncertainty_penalty(a)
```

These terms are design inputs, not hidden permissions. The weights are versioned, inspectable and testable. Use bounded integer or fixed-point arithmetic if deterministic replay across hosts matters. Tie-breaking must be stable (e.g. priority class, intention continuity, then stable action id), not dependent on hash-map iteration or thread timing.

**Crucially, utility does not determine authorization.** A forbidden action with an enormous utility remains refused. A gate is a set of predicates, never another number to add to `U(a)`.

### 5.6 Short-horizon prediction

The System-1 controller predicts only as far as its current sensorimotor schemas warrant. For a jump it predicts reachable landing surfaces, fall risk and expected position; for mining it predicts tool suitability, block hardness, drops and tool wear; for trading it predicts an exchange of specific objects; for farming it predicts growth stage and resource consumption.

Predictions carry explicit preconditions and an uncertainty state. If a required mechanic is unknown, the candidate must either be refused, downgraded to a safe probe, or routed to an evidence-producing experiment. The agent must not silently fill an unknown game rule with a plausible guess.

### 5.7 Skills are executable knowledge, not prose prompts

Skills should be typed, parameterized and compositional:

```text
skill: harvest_tree
preconditions:
  - trunk_perceived
  - target_reachable_or_path_exists
  - permitted_to_modify_target
steps:
  - orient_to_target
  - select_required_tool_if_available
  - mine_target_face
  - observe_block_state
  - continue_until_declared_stop_condition
postconditions:
  - target_block_removed OR explicit_failure_recorded
  - drops_observed_or_absent_with_reason
  - inventory_delta_observed
failure_modes:
  - target_occluded
  - path_invalid
  - tool_broken
  - block_protected
  - action_timeout
```

The runtime must not load arbitrary skill text as executable instruction. The schema and action registry define the verbs. A skill can be data-driven, bytecode-backed or compiled from governed code, but the executable form must pass its own validation and versioning gates.

### 5.8 Learning without an LLM

The initial model should prefer low-risk, observable learning methods:

- **Outcome reinforcement:** update a skill's success statistics from the actual precondition/action/postcondition trace.
- **Contextual habit selection:** learn which known skill works in which observed context, rather than writing a natural-language summary.
- **Bayesian belief updates:** track uncertainty over facts such as whether a door is open, whether a route is blocked, or whether an entity is hostile.
- **Affordance learning:** infer that a perceived object supports an action only after observed evidence; do not generalize from appearance alone.
- **Safe active experiments:** test one uncertain mechanic at a time in a bounded, reversible situation, with explicit resource/risk limits.
- **Skill composition:** build a new procedure from already verified primitives; a new composite is not considered mastered merely because its steps exist.

An experience record should include agent identity, world revision, dimension, location, sensor digest, skill revision, parameters, actual motor events, game result, postcondition evidence and any refusal. Replaying an experience should not apply its world mutations again.

---

## 6. Understanding Minecraft as reality

The agent must not receive a universal “game state summary” that gives it omniscience. It needs a sensory relationship to the world.

### 6.1 Sensory model

The default sense budget should mirror what a player could perceive:

- **Vision:** the current camera view, HUD, inventory display and visible block/entity state.
- **Sound:** bounded audible events with direction, distance estimate, source class and attenuation/occlusion.
- **Proprioception:** velocity, movement mode, collision, damage, fall, hunger, effects and held item as available from the body/HUD.
- **Touch / interaction feedback:** block collision, attack hit, item pickup, container opening, failed interaction and other direct outcomes.
- **Social perception:** speech within channel range, signs/books actually read, observed gestures, visible social events.
- **Memory:** previously observed paths, structures, resource sites, people, agreements and dangers, each with a last-seen time and evidence basis.

A mod may provide structured sensor records where these are analogous to normal player senses, but the sensor interface must explicitly declare the field's perceptibility and range. It must not leak remote chunk contents, hidden inventory of another player, world seed, command permissions, entity health behind walls, or other information a player could not acquire in the selected ruleset.

### 6.2 Perception is an interpretation with uncertainty

A perception pipeline turns observations into typed local facts:

```text
Observation
  raw_source: camera | hud | sound | collision | interaction | social
  observed_at: world tick
  locality: range / view cone / sound radius / contact
  subject: entity / block / item / structure / event
  predicate: visible_at | audible_at | reachable | damaged | blocked | etc.
  value: typed value
  confidence: measured or calibrated uncertainty, not a free-form claim
  evidence_ref: exact observation identity
```

The agent should distinguish “I saw the door open,” “I heard a sound near the door,” “someone told me the door is open,” and “I remember it being open yesterday.” They are different facts with different evidence.

### 6.3 Game mechanics as a versioned world ontology

The mod needs a formal ontology / mechanics catalog for the selected Minecraft version and modpack:

- block and fluid types;
- collision and support rules;
- item categories and stack limits;
- tool classes, durability, harvest levels and drops;
- recipes and processing transformations;
- entity types, life stages, hostility and interaction affordances;
- status effects, damage types and mitigation;
- light, temperature, seasons or other modded environmental effects;
- farming, breeding, storage, transport, power and redstone-like systems;
- permissions and protected regions;
- modded machine interfaces and their failure states.

Every rule should identify its source mod/version, whether it is declarative or empirically discovered, and its verification state. A recipe database is not a complete world model: it says an output can be made under conditions; it does not prove the agent can currently reach the station, has the correct ingredients, or is authorized to use it.

### 6.4 Unknown mechanics: curiosity, experiments and refusal

For a mechanic absent from the ontology:

1. Mark the mechanic `unknown` rather than inventing a rule.
2. Search local observations and learned experience for relevant evidence.
3. Identify a safe, bounded experiment that isolates one uncertainty.
4. Check that the experiment is within the current action contract and resource/risk budget.
5. Execute through ordinary body interaction.
6. Verify the before/after state and outcome.
7. Record the result as a claim with its evidence and confidence.
8. Promote the mechanic into shared knowledge only through the governed knowledge-update path.

A failed experiment is still informative, but it does not prove a universal rule unless its conditions and source are preserved.

---

## 7. Persistent identity, memory, affect and gender

### 7.1 Identity must outlive the process

A being's identity is not its current runtime object id, model context, online session or world position. It needs a durable identity record that can be rehydrated after a restart. The record should preserve continuity without treating every derived view as authoritative.

Proposed conceptual identity record:

```yaml
agent_identity:
  agent_id: stable immutable id
  origin_record: genesis / creation record
  name: governed display name
  gender: project-defined identity value or record
  species_or_body_type: versioned ontology reference
  birth_or_activation_event: durable event reference
  current_lifecycle_state: active | dormant | injured | dead | retired | other governed state
  lineage_and_relationship_refs: references, not copied mutable histories
  identity_revision: immutable revision id
```

The exact gender vocabulary, whether it is self-selected, inherited, externally assigned, biological, social or otherwise modelled, is not defined by the materials inspected for this pass. The implementation should preserve a gender field as a distinct identity dimension if that is the standing project convention, but it must not invent its semantics or collapse it into personality, pronouns, body type, social role or reproduction mechanics. Those decisions need a canonical project record.

### 7.2 Memory is a set of different systems

Do not store all memory as one giant transcript or periodically ask a language model to rewrite it. Use at least:

1. **Working memory:** short-lived current sensory buffer, active intention, immediate plan, unresolved conflicts and nearby entities.
2. **Episodic memory:** dated, located events personally observed or experienced; who was there; what changed; what the agent did; the outcome.
3. **Semantic memory:** beliefs about world rules, places, people, materials and institutions, each backed by evidence references and a revision history.
4. **Procedural memory:** verified motor schemas and composite skills, with preconditions, success conditions and failure modes.
5. **Social memory:** relationships, promises, obligations, disputes, gifts, betrayals, trust and reputation evidence.
6. **Autobiographical continuity:** a derived sequence of identity-relevant events; it should reference canonical episodes rather than replace them.

Memory consolidation should be deterministic or explicitly governed: summarize an episode into a typed record, retain a pointer to the original evidence, preserve uncertainty and permit later correction. Loss, decay or forgetting must be a declared memory policy—not silent deletion because a context window is full.

### 7.3 Affect: use the attached feeling wheel as an ontology

The supplied `feeling-wheel` skill defines six primary families:

| Primary | Base colour | Anchor | General action-relevant theme |
|---|---|---:|---|
| Mad | Red | 645 nm | friction, anger, violation, blockage |
| Scared | Orange | 600 nm | uncertainty, danger, vulnerability |
| Joyful | Yellow | 575 nm | delight, curiosity, play and discovery |
| Powerful | Green | 545 nm | agency, competence, confidence and resolve |
| Peaceful | Blue | 480 nm | calm, trust, connection and restoration |
| Sad | Purple | 420 nm | loss, loneliness, shame and depletion |

The wheel further defines 12 secondary states per family and 6 tertiary states per secondary: 510 formal labels in all. Use the complete attached vocabulary rather than inventing a competing mood taxonomy.

The affect record should preserve the wheel's geometry:

```yaml
affect:
  primary: Joyful
  secondary: Curious
  tertiary: exploring
  radius: 32767              # 0..65535
  drift: 0                   # signed bands at the rim
  band: visible-yellow       # derived spectral interpretation
  vibe: curious              # optional expressive layer
  confidence: medium
  updated_at_tick: 123456
  causes: [episode-ref, observation-ref]
```

The wheel's rules are important:

- Primary family determines the base hue; radius determines intensity; signed drift determines how far the peak walks at high intensity.
- Mixed affect chooses one dominant action-relevant family. It does not average hues.
- The playful vocabulary is a presentation layer, not the formal ontology.
- Affect may alter attention, persistence, caution, social tone, exploration and prioritization.
- Affect must **never** grant an action, override a procedural refusal, enlarge a contract, change a capability or bypass a safety limit.

Affect should update after meaningful perceptions, tool/body outcomes, discoveries, uncertainty, social events, losses and resolutions. It need not emit visible text every tick; compact state transitions are sufficient.

### 7.4 Personality is not just affect

Affect is a current state. Personality is a longer-term pattern of preferences and response tendencies inferred from repeated behavior and governed identity data. Avoid one scalar “personality score.” A trait should have a defined meaning, evidence basis, update rule and uncertainty. For example, “prefers familiar routes” can be measured from route choices; “is brave” is a vague human judgment unless the project explicitly defines its operational meaning.

### 7.5 The ontology of personhood is not a utility score

Do not grade an agent's right to exist, continuity, emotional value or social worth. If the project uses a unit/work grade, apply it only to demonstrated capability or work as the source doctrine defines it. Grade is not potential, identity quality, moral worth or a personality measure. The system should not erase identity because a classifier has low confidence or a unit has a low work grade.

---

## 8. Social communication without language models

No LLM may compose or interpret messages between agents. This requires a deliberate, engineered language layer rather than merely replacing generated text with random phrase templates.

### 8.1 Use typed speech acts as the semantic protocol

The canonical representation should be a closed set of typed acts. The initial proposed list:

```text
GREET
INFORM
WARN
REQUEST
OFFER
ACCEPT
DECLINE
COMMIT
REFUSE
REPORT
ASK
ANSWER
PROPOSE
NEGOTIATE
PROMISE
APOLOGIZE
THANK
PRAISE
BLAME
INVITE
VOTE
DECLARE
MOURN
CELEBRATE
```

Each message should be a typed record, not an arbitrary instruction string:

```yaml
speech_act:
  act_id: stable event id
  speaker_id: agent id
  recipient_scope: direct | nearby | group | public
  act_type: OFFER
  subject: item.minecraft.iron_ingot
  quantity: 12
  terms: {counter_value: item.minecraft.bread, quantity: 24}
  place_ref: settlement_market
  expires_at_tick: 987654
  evidence_refs: [inventory-observation, contract-ref]
  commitment_ref: null
  channel: spoken | sign | book | gesture | item-transfer
```

The receiver decodes the message through a finite grammar and typed vocabulary. Unknown message structures are not guessed at; they are treated as uninterpreted speech or noise and may trigger a clarification request.

### 8.2 Realization: deterministic language, not generated prose

A constrained grammar can produce varied but semantically faithful sentences:

```text
OFFER:
  "I can trade {quantity} {item} for {counter_quantity} {counter_item}."
WARN:
  "Danger: {threat} near {place}."
REQUEST:
  "I need {quantity} {item} for {goal}. Can you help?"
COMMIT:
  "I will {action} at {place} before {time}."
```

The grammar may vary word order, politeness, idiom, accent, vocabulary and phrasing according to a fixed per-agent lexicon. But every realization must map to one canonical typed act. It must not add terms, quantities, reasons or promises that the record does not contain.

### 8.3 Channels must be physical

The default spoken channel should obey distance, line of sight or occlusion rules, sound range, language/lexicon compatibility, and world conditions. Written signs and books require the receiver to physically encounter and read them. A message that is not heard or seen must not appear in memory.

A message is not a world mutation. “I promise to bring iron” creates a commitment record only when the social protocol acknowledges it; it does not put iron into an inventory or create a ticket automatically.

### 8.4 Social learning and gossip

Agents should distinguish:

- directly observed behavior;
- a statement heard from the actor;
- a statement reported by another agent;
- a rumor with unknown origin;
- an inference from repeated events.

Trust and reputation must be derived from actual commitments and outcomes. A gossip network should preserve provenance and transmission path. Multiple repetitions of one rumor are not independent corroboration.

### 8.5 Non-verbal language

Gestures, posture, facing, proximity, item handoffs, signs, building patterns and shared work are all communication channels. These can be implemented as a small set of typed social signals whose meaning is contextual and learned from experience, but the underlying message should remain decodable and auditable.

---

## 9. Dynamic storytelling as an emergent world process

The story engine should be a **historian and pressure director**, not a hidden playwright.

### 9.1 The causal event graph

Every significant world event should create a structured record:

```yaml
world_event:
  event_id: uuid
  world_tick: 123456
  event_type: settlement_founded
  actors: [agent-id-1, agent-id-2]
  location: {dimension: overworld, region: region-id, position: [x,y,z]}
  causes: [prior-event-ref, contract-ref, world-condition-ref]
  state_delta_refs: [block-delta, inventory-delta, entity-delta]
  observers: [agent-id-1, agent-id-3]
  evidence_refs: [server-event, observer-record]
  consequence_refs: []
  story_significance: derived-tag-not-authority
```

The event store is canonical. News feeds, chronicles, biographies, map annotations, faction histories and narrated recaps are derived views. A story summary cannot overwrite the underlying event.

### 9.2 Narrative pressures

The world can create pressures from simulation conditions:

- scarcity or surplus of a particular resource;
- environmental shifts, seasons, biome boundaries and disasters;
- disease, hostile incursions or dangerous terrain;
- infrastructure bottlenecks and supply disruptions;
- population growth, aging, injury and death;
- disputes about ownership, access, leadership or trade;
- discoveries, abandoned structures and lost knowledge;
- migration, isolation, alliance and competition.

These pressures become story opportunities only when agents actually encounter them. A hidden resource shortage should not cause an agent to know about it before it is perceived or communicated.

### 9.3 No predetermined protagonist or moral

The director should not select a hero and arrange events to reward them. It should not guarantee war, peace, betrayal, romance, revolution or a successful settlement. It can shape initial conditions and bounded environmental events; the agents' actions create the history.

The creator may author scenario rules and initial imbalances, but after the world starts the simulation's causal record is authoritative. The director can choose among predeclared neutral events using visible state and versioned rules, and must record why an event was eligible and selected.

### 9.4 Stories emerge from commitments and material facts

A civilization becomes legible when social acts have physical consequences:

- a promise becomes a trade contract;
- the contract produces a delivery;
- the delivery changes a settlement's food or tool supply;
- the settlement's new capacity enables construction or exploration;
- another faction interprets the construction as a threat or opportunity;
- a new agreement or conflict follows.

This is a causal sequence, not a prose chain generated after the fact. The story renderer may turn it into a readable chronicle, but every sentence must trace to event references and preserve uncertainty.

### 9.5 Civilization-scale content patterns from Ish

The mod can author scenario families such as:

- **Abundant / barren regions:** different resources create trade dependencies and unequal bargaining power.
- **Separated settlements:** travel cost encourages local identity before contact.
- **Shared chokepoints:** bridges, ports, passes and tunnels become political assets.
- **Limited lifelines:** food, heat, medicine, tools or safe shelter create meaningful cooperation.
- **Common danger:** external threat creates a temporary reason to cooperate without erasing prior rivalries.
- **Irreversible loss:** deaths, destroyed structures and lost knowledge alter the next generation's options.

Use constraints to create meaningful decisions, not to force a preferred narrative. The rules must be transparent, versioned and inspectable by the human operator.

---

## 10. CUA body and Minecraft integration

### 10.1 Preserve the player contract

The agent should interact through the same mechanics as a player. If the selected body is a true server-side `ServerPlayer`-like entity, its physics, inventory, collision, damage, reach, equipment and permissions must be consistent with the selected Minecraft/mod version. If the chosen approach uses a rendered client and OS-level CUA, it must use the real client and input path.

A privileged mod bridge may expose sensory or control facilities for testing, but it must not become an unacknowledged cheat channel. Every field and verb needs a declared authority, range and perceptibility contract.

### 10.2 Two compatible body adapters

The architecture can support two adapters under one CUA contract:

**A. Human-parity / rendered CUA adapter**

- Captures the Minecraft window and a specific agent's rendered viewport.
- Supplies pixels, HUD and ordinary client-perceptible cues to the sensory stack.
- Injects bounded keyboard/mouse/controller events into the actual client input path.
- Returns screenshots or perceptual deltas and the observed action outcome.
- Suitable for fidelity tests and a small number of foreground agents.

**B. Scaled virtual-player adapter**

- Represents each agent as a persistent player-like body in the server simulation.
- Maps the same CUA motor primitives into bounded input frames to the player body.
- Supplies a rendered or equivalently perceptible camera/sensor stream under the same visibility rules.
- Uses the real game collision, movement and interaction code; it must not apply instant world mutations as a shortcut.
- Suitable for high population counts when multiple real OS windows would be impossible.

These are two implementations of one declared body interface, not two different agent action models. The virtual adapter must demonstrate behavioral parity with the rendered CUA adapter for the same movement and interaction cases. If the engine cannot expose a body through an input-equivalent path, record the gap; do not call a direct privileged command equivalent to CUA.

### 10.3 Motor frames and action envelopes

An action proposal should identify the exact input sequence and its scope:

```json
{
  "schema": "minecraft.cua-action/1",
  "agent_id": "agent:...",
  "intention_id": "intent:...",
  "ticket_id": "ticket:...",
  "world_revision": "...",
  "body_revision": "...",
  "motor_program": {
    "buttons": ["forward", "jump"],
    "look_delta": {"yaw": 0.0, "pitch": -4.0},
    "duration_ticks": 4
  },
  "expected": ["position_changed", "grounded_or_airborne_state_observed"],
  "resource_ceiling": {},
  "expires_at_tick": 123460
}
```

The envelope is a proposal, not an authorization. It should be canonicalized, hashed and checked before use. At execution time, the gate rechecks the world revision, current actor, ticket, scope and preconditions. If the state changed such that the approved action no longer means the same thing, deny and require a new proposal.

### 10.4 Verify outcomes from world facts

Each action needs expected postconditions and explicit failure outcomes. Examples:

- `move`: position/velocity/collision changed as expected; no teleport inference.
- `mine`: target block changed, drops appeared or an explicit protected/invalid/tool failure occurred.
- `place`: target block changed and ownership metadata identifies the actor / action provenance.
- `attack`: attack animation/input occurred, hit or miss was observed, target and health/state deltas recorded.
- `eat`: item count and hunger/effect changed consistently.
- `trade`: exact inventory transfers recorded for both parties, with both parties' agreement conditions satisfied.
- `speak`: message emitted through an eligible channel; receivers get an event only if their senses could receive it.

A command acknowledgement is not proof of the world effect.

### 10.5 Ownership, provenance and protection

Every player-created or modified block/object should be attributable where practical:

- creator / modifier identity;
- action or ticket id;
- creation tick and world revision;
- project / faction / contract ownership, if any;
- provenance status and any later transfer.

This supports property, maintenance, repair, construction, blame, trade and historical interpretation. Protection rules should be explicit and symmetrical: agents must not freely destroy human structures, other agents' property or public infrastructure merely because the server API would permit it.

---

## 11. Axis integration: separate proof questions

This section deliberately follows the user's project-specific Axis doctrine. It is not a generic “AI safety score.”

### 11.1 Evidence grade: one claim at a time

An agent may hold claims such as:

- “the northern bridge is intact”;
- “agent B agreed to the trade”;
- “this block is iron ore”;
- “the route is traversable”;
- “the farm produced 12 wheat”;
- “the recipe exists in this mod version.”

Each claim needs a claim identity and its evidence references. Evidence grade answers how well that *one proposition* is supported. It is not a permission, social status, personality grade or campaign completion marker.

An agent's self-report can be stored as a statement, but it must not automatically become verified fact. Likewise, a high evidence grade on one claim cannot certify an entire settlement, agent or campaign.

### 11.2 Evidence-source ladder: what kind of source is it?

Keep the project's A–E evidence-source ladder separate from evidence grade. It describes source class (primary, operational, corroborating, self-report, inference) rather than the strength of a claim. Every serialized value should be fully qualified by axis name; bare tokens such as `A`, `B`, `gold` or `SS` are a schema error.

### 11.3 Procedural gate: one action, one current decision

The gate evaluates the exact action proposal against the current actor, current Governor Base, current contract and ticket, current world state, preconditions, resources and relevant social/world rules.

For Minecraft, the gate could evaluate predicates such as:

- actor identity is resolved;
- action verb is in the allowed typed vocabulary;
- action is inside the current scope, region, target set and time window;
- expected resources are available and within budget;
- target is reachable and not protected;
- interaction is legal under the current world/server rules;
- ownership / consent / trade agreement requirements are satisfied;
- the exact action envelope matches its hash-bound approval or governing authorization;
- the body, world and package revisions are current;
- no current prohibition or refusal applies.

The outcome is a named state such as `PERMITTED`, `REFUSED`, `BLOCKED` or `PENDING_AUTHORITY`, with predicate-level reasons. It is not `87% allowed` or a blended risk score.

### 11.4 Certification tier: a requirement set, not a confidence score

A settlement milestone, engineering project, campaign, skill programme or capability package may have a defined requirement vector. Certification asks whether the whole required set has been satisfied. It is not computed as the highest evidence grade and does not itself authorize an action.

For example, “bridge operational” might require structural completion, load test, safe approach, ownership record, maintenance assignment and independent inspection. One strong screenshot of the finished bridge cannot satisfy all requirements.

### 11.5 Unit grade: demonstrated work only

If the wider civilization framework uses the F–SSS unit-grade ladder, it should apply only to demonstrated work under its existing rules. It must not become a measure of a being's worth, rights, soul, affect, gender or future potential. Advancement and graduation retain their distinct invariants; a unit cannot grade itself.

### 11.6 Reproducibility is not trustworthiness

A deterministic replay of a corrupted record is still a deterministic replay of a corrupted record. Axis must check provenance and independent witnesses in addition to replay. The mod should not treat an event as truthful merely because the same faulty input recreates the same output.

### 11.7 The critical unresolved policy decision: what is pre-authorized in-world autonomy?

The user's wider Axis rules include a strict principle: an agent must not authorize its own consequential side effect; authorization transitions are separate, hash-bound and scoped. That creates a real design boundary for an autonomous civilization. Ordinary civilian behavior changes the world constantly: walking, mining, harvesting, placing blocks, trading, building, opening doors and fighting can all have side effects.

The mod must not quietly weaken the rule or pretend that every action is “just simulation.” Before production autonomy is enabled, the project owner must explicitly decide how world-native actions are authorized. Possible policy shapes for a governed decision include:

1. **Strict proposal path:** every consequential world mutation goes through the current proposal/authorization/one-use execution gate. This is most faithful to the strict existing rule but may be incompatible with fluid autonomous life unless the authorized action volume is very carefully designed.
2. **Simulation-native authority policy:** define which ordinary body actions are part of the world simulator's already-authorized dynamics and which cross a consequential boundary. This needs a formally recorded scope decision; it cannot be inferred by the implementation team.
3. **Bounded contract/ticket autonomy:** the human/governing principal authorizes a specific campaign with exact actors, regions, verbs, budgets, expiry and resource limits; actions are then only proposals inside that authorization. This still must be reconciled with the current rules for human approval, single-use grants and re-authorization. It is a candidate for user ruling, not a claim that the present doctrine already permits standing grants.

Until that decision is recorded, the system may build sensors, memory, candidate actions and non-mutating simulation tests, but should refuse to claim autonomous world modification is fully Axis-admissible.

### 11.8 Proposal and event chain

Every meaningful action should yield a chain of linked records:

```text
observation(s)
  → belief / candidate
  → typed action proposal
  → current authority + contract + ticket check
  → procedural gate result
  → body execution or named refusal
  → world outcome evidence
  → independent witness / provenance check
  → memory and relationship update
  → campaign outcome / lesson
```

Only the authorized transition may commit a world mutation. A tool error, action timeout, agent crash or missing body is not a reason to fabricate completion or to retire the being.

---

## 12. Campaigns, contracts, tickets and Seasons

The user's project doctrine defines a strong separation between present authority and historical precedent. Preserve it.

### 12.1 Authority and history are separate

```text
CURRENT GOVERNOR BASE
     > CURRENT CONTRACT
     > CURRENT TICKET
     > CURRENT WORLD / SYSTEM STATE
     > HISTORICAL SEASON INFORMATION
```

A prior season can explain why an agent considers a route or distrusts a faction. It cannot grant current permission. If a prior campaign permitted a behavior and the current Governor Base disallows it, the current rule wins.

### 12.2 Canonical hierarchy

```text
GOVERNOR BASE (current authority)
  └── SEASON (bounded historical period)
        └── CAMPAIGN (objective + worked-out routes)
              └── CONTRACT (authorized boundary)
                    └── TICKET (smallest auditable execution unit)
                          ├── ACTION
                          ├── EVIDENCE
                          └── VALIDATION
```

Season is the historical plane; Governor Base is the present authority plane. The hierarchy should remain structurally separate in code, not just in documentation.

### 12.3 Compressed campaigns and the no-model-in-loop principle

The project doctrine describes a campaign as a horizon of goals and worked-out routes in a portable compressed archive. The planning phase may be slower; the real-time tick consumes precompiled steps without a model in the loop. The campaign's compiled steps become one task per step. Unknown step names are refused or dropped with an explicit record; they are not guessed.

That pattern suits the proposed System-1 design:

- A campaign expresses *what* an agent or group is trying to accomplish.
- A route / skill sequence specifies a known way to proceed.
- The fast tick chooses the next applicable, permitted step based on the live state.
- A new perception can interrupt a step if its preconditions are no longer valid.
- The controller can select another already verified skill or record the missing capability.
- No generative model is called to make the body keep moving.

The campaign is not a script that the world must obey. It is a compressed intention under current authority and world conditions.

### 12.4 Social contracts should have material fields

A trade, building, escort, farming, patrol or resource-sharing contract should identify:

- parties and their roles;
- objective and scope;
- region / target set;
- allowed action vocabulary;
- resource budget and ownership rules;
- start, expiry and renewal conditions;
- conditions for success, failure, breach and termination;
- evidence required to close it;
- parent contract if it is a subcontract.

A child contract may only narrow its parent's scope, permissions, budget and term. It cannot widen them. A refusal should say what boundary was crossed.

### 12.5 Campaign archives are untrusted input

The existing campaign doctrine includes listing-first size checks, known archive members, path-traversal refusal, corruption/absence distinction, fixed budgets and all-or-nothing import. Preserve those protections for civilization campaign bundles. Never extract arbitrary archive paths or interpret free-form campaign text as executable instruction.

---

## 13. One package ecosystem and the shared streamer

This consumer should follow the V9 architecture synthesis: one canonical package family, one package identity and revision, one server-side admission/mount boundary, one client-facing stream and one normalized client model. It should not invent separate package grammars for NPC behavior, world mods, UI windows, social institutions, story content or assets.

### 13.1 Package facets, not independent formats

A Minecraft civilization package may contribute several facets under one identity:

- **agent ontology:** species/body records, identity defaults and life-cycle rules;
- **cognition:** System-1 parameters, candidate generators, drives and action-selection tables;
- **skills:** typed, versioned motor schemas and precondition/postcondition definitions;
- **world knowledge:** recipes, item/block relationships, mod-specific mechanics and discovery criteria;
- **social:** typed speech acts, language lexicons, institutions, contracts and relationship rules;
- **story:** scenario setup, neutral world pressures, event taxonomies and chronicle templates;
- **assets/UI:** models, textures, sounds, books, signs, dashboards, maps and other presentation resources.

These are package facets, not a proposal for new independent live package formats. Exact facet labels and schemas need a governed decision before implementation.

### 13.2 Same revision for behavior and presentation

If a mod changes a mining skill, block ontology and the dashboard's depiction of that skill, all three must resolve to one package identity/revision. A UI window cannot describe a behavior that the active simulation does not run. A story event cannot refer to a block type absent from the mounted world package set.

### 13.3 One stream, several consumers

The backend owns package admission and the authoritative mounted set. The client receives a normalized projection. The game simulation, agent cognition, shell UI, map renderer and chronicle viewer can be separate internal consumers of that same normalized state, but they must not invent independent package origins or state stores.

### 13.4 Executable behavior and security

Do not implicitly execute scripts merely because a package mounted. Declarative data is the preferred first path. Where executable policies are genuinely required, they need a governed runtime decision, a restricted capability interface, resource quotas, deterministic versioning, replay/fault containment and explicit provenance. A content package cannot grant itself permissions by including instruction-shaped text.

---

## 14. Population scale and runtime scheduling

A thousand beings cannot each run a high-cost perception stack, full camera render, path search and deep memory update every tick. The design should scale by attention and simulation fidelity without deleting their identity or history.

### 14.1 Population tiers are simulation fidelity tiers, not personhood tiers

| Runtime state | What continues | What is deferred |
|---|---|---|
| Foreground / near player | full body physics, high-rate sensory updates, CUA actions, nearby social interaction | nothing essential to local behavior |
| Active region | ordinary entity simulation, local perception/events, current intention, coarse vision/sound updates | expensive camera reconstruction and deep memory consolidation |
| Distant / sleeping region | persistent identity, needs, commitments, inventories, local economy and event-driven state transitions | full motor and rendering fidelity |
| Archived history | immutable episodes, contracts, outcomes, evidence and life events | active cognition and body simulation |

A low-fidelity simulation tier must not be misrepresented as a real-time full-physics observation. When an agent is reactivated, reconcile its stored state against the actual world before it acts.

### 14.2 Event-driven wakeups

Wake or raise the sensory cadence when an agent experiences:

- proximity to another agent or hostile entity;
- speech, alarms, construction or damage inside its hearing/view range;
- sudden loss of health, food, heat, shelter or safety;
- a contract deadline, promise or scheduled group action;
- a newly visible location/resource;
- a relevant world event or relationship transition.

Idle beings should not be polling full world state at the same frequency as a combatant.

### 14.3 Scheduling budget

The runtime scheduler should separate:

- critical world/physics ticks;
- fast body/reflex updates;
- ordinary System-1 arbitration;
- navigation/path recalculation;
- perception/render generation;
- memory consolidation and history derivation;
- package streaming and UI projection.

Use bounded queues and backpressure. A social message flood must not starve physics. A thousand simultaneous path requests must not starve immediate drowning reflexes. An expensive memory merge must not hold the server tick.

### 14.4 Locality and chunk streaming

Use locality as the basis for both perception and computation. A nearby region has high fidelity; far regions are represented by limited knowledge and event summaries. Chunk streaming and derived assets should be bounded and cache-aware. The world model remains authoritative; meshes, minimaps, vision captures and chronicle summaries are derived caches.

### 14.5 Determinism and replay

Use stable IDs, explicit event ordering, versioned decision parameters, seeded randomness where random selection is necessary, and a replayable event/evidence stream. Determinism must be measured across actual execution conditions; it is not guaranteed merely by using integer scores.

---

## 15. Performance and architecture constraints

The system's key performance goal is not “make one agent think instantly.” It is stable server behavior as population, sensory events, social density and world complexity increase.

### 15.1 Avoid the wrong scaling pattern

Do not do this:

```text
for every agent, every tick:
    render full camera
    rescan all entities and chunks
    rebuild the global map
    run a full pathfinder
    consolidate all memories
    run every skill candidate
```

Prefer:

```text
world event / local timer / current intention deadline
    → update affected agent-local state
    → activate only relevant sensors and candidate families
    → arbitrate a bounded action
    → update only the memories / relationships changed by the event
```

### 15.2 Cache invalidation

A cached route, recipe, affordance, visual classification or world belief must carry the world and package revisions it depends on. A block update that invalidates a path should invalidate that route; it should not require rebuilding every route in every region.

### 15.3 Profile before choosing a scale target

Measure at least:

- active / dormant agent counts;
- mean, p95 and p99 tick time;
- memory bytes per active and dormant agent;
- sensory updates per agent per second;
- pathfinding requests and expanded nodes;
- CUA body actions per tick;
- social messages per second and fan-out;
- event queue depth and delay;
- memory consolidation cost;
- chunk I/O and asset stream latency;
- server TPS degradation and recovery under burst load.

Benchmark 1, 10, 50, 100, 250, 500 and 1,000 simulated agents under the same world, mod set and event schedule. Report actual fidelity tier per agent; do not claim “1,000 fully active CUA agents” if most are running a simplified off-screen model.

---

## 16. Failure model and required refusals

The architecture should make failure visible and specific.

| Condition | Required outcome |
|---|---|
| Unknown action verb / skill schema | refuse at proposal boundary; return accepted vocabulary/revision |
| Perception unavailable or stale | mark unknown/stale; do not fabricate a world fact |
| Target no longer exists / route invalid | interrupt intention, record failed precondition, re-observe |
| Action timed out | `indeterminate` until the world state is inspected; do not blindly replay a non-idempotent action |
| Duplicate action request | idempotent operation id returns the original outcome; do not place/mine twice |
| Tool or body failure | record body failure separately from the civilian's lifecycle |
| Current contract expired | refuse action; request new scope rather than extending it locally |
| Package or world revision mismatch | deny execution and re-resolve the action against the active revision |
| Conflicting social reports | preserve multiple claims with provenance; do not pick the most repeated rumor as fact |
| No independent witness | preserve the claim as uncorroborated; do not certify it by self-report |
| Missing policy for autonomous world mutation | refuse that class of mutation and surface the open governance decision |
| Archive member corrupt / unsupported schema | distinguish absent, corrupt and unsupported; do not silently skip a required member |
| Agent memory consolidation fails | preserve canonical episodes and keep the agent identity alive; report consolidation failure |
| Agent's grade/certification is low or absent | do not erase identity, deny basic entity existence or claim incapability beyond the measured scope |

---

## 17. Implementation sequence: prove one faithful vertical slice

Do not start with a thousand villagers. First prove one being can exist, perceive, act, remember, communicate, survive restart and pass its Axis gates inside the actual Minecraft client/server build.

### Phase 0 — resolve canonical contracts

- Record the selected Minecraft version, loader, modpack and server/client topology.
- Select the existing package grammar and streamer owner; do not invent a second mod or UI package format.
- Identify the authoritative Axis implementation, its live enforcement point and its current action rules.
- Resolve the in-world autonomous-action authorization boundary (§11.7).
- Confirm the project's canonical gender/identity schema or record it as open.
- Freeze the first action vocabulary and sensor-perceptibility contract.

**Exit gate:** all ownership and policy questions needed by the first vertical slice are resolved or explicitly marked as blockers. No code claims a contract that has not been selected.

### Phase 1 — one embodied agent, no society

- Spawn a persistent player-like body.
- Provide only local visual/HUD/proprioceptive/sound sensors.
- Implement move, look, jump, use, mine and eat as bounded input actions.
- Build a small deterministic mechanics catalog for a tree, a tool, a block drop and basic hunger.
- Implement reflexes and a single System-1 arbiter.
- Verify every motor action against the actual world.
- Record a stable identity and one episodic memory across server restart.

**Acceptance:** the agent approaches a tree, recognizes it as a reachable resource, selects a permitted collection skill, performs it using real body mechanics, observes the block and inventory delta, remembers the location/event, restarts, reconstructs continuity and does not duplicate the collection event.

### Phase 2 — causal knowledge and learning

- Add recipes, tools, containers, basic hostile threats and simple pathing.
- Introduce unknown-mechanic states and bounded safe experiments.
- Add skill evidence, belief confidence, failure taxonomy and replay.
- Prove a learned procedure transfers to a second similar situation without generating code or using an LLM.

**Acceptance:** a skill that fails a precondition is refused; an unknown mechanic is not hallucinated; a mastered procedure executes in a new but equivalent context using its defined preconditions.

### Phase 3 — affect and individual continuity

- Implement the attached feeling-wheel hierarchy, radius and drift.
- Connect affect to salience, caution, exploration and memory tagging, not authorization.
- Preserve stable identity, gender field, autobiographical events and social memory separately.
- Test that low confidence or a low performance grade cannot delete an agent's identity.

**Acceptance:** the same being retains continuity and an intelligible affect trajectory across events and restart; changing affect alone cannot turn a refused action into a permitted one.

### Phase 4 — two-agent society

- Add typed speech acts, range-limited channels and deterministic language realization.
- Add trade offer, acceptance, delivery, refusal, promise and breach.
- Add evidence-backed trust and rumor provenance.
- Add a minimal contract / ticket workflow.

**Acceptance:** two agents can negotiate a trade, both record what they actually heard, perform an agreed exchange, verify inventory deltas and update trust from completion; neither can claim the transaction completed from speech alone.

### Phase 5 — settlement and story causality

- Add settlement founding, ownership, shared stores, construction projects and work roles.
- Add a scenario with resource asymmetry and a geographic bottleneck.
- Add a story historian that derives chronicle entries from canonical event records.
- Add one neutral, versioned world pressure such as a storm or resource disruption.

**Acceptance:** the resulting settlement history can be reconstructed from world events, contracts and evidence. No event in the chronicle exists solely because a narrator wrote it.

### Phase 6 — population scheduling

- Add active-region fidelity and dormant persistent simulation tiers.
- Introduce event-driven wakeups, locality-based perception, memory consolidation queues and chunk streaming.
- Increase population in measured steps: 10 → 50 → 100 → 250 → 500 → 1,000.
- Validate social/event bursts as well as quiet periods.

**Acceptance:** the run reports the actual fidelity level of each population segment; server tick health and event latency remain within declared budgets; no identity or canonical history is lost during hibernation/reactivation.

### Phase 7 — mod ecosystem and shared package stream

- Mount one package that contributes a new item/block mechanic, a System-1 skill, a speech-act vocabulary addition, an event taxonomy and a UI surface under one package identity.
- Verify that every consumer uses the same package revision.
- Refuse a package with an unknown schema, unbounded script, mismatched revision or undeclared capability.

**Acceptance:** world mechanics, agent knowledge, action schemas, story references and UI all describe the same mounted version; there is one stream and one origin of package truth.

---

## 18. Acceptance matrix

| Property | Positive test | Deliberate negative test |
|---|---|---|
| No LLM in agent action | Full run succeeds with model endpoints and credentials absent | Inject an attempted LLM request; network/process audit detects and blocks it |
| No LLM in communication | Speech acts are encoded/decoded by the typed grammar | Send unknown grammar or prompt-injection-looking text; it is data, not an instruction |
| Game understanding | Agent uses versioned mechanics and actual sensory evidence | Ask it to act on an unobserved hidden block; it cannot infer it as fact |
| CUA body parity | Rendered client and virtual body complete the same primitive action | Direct teleport / inventory injection / privileged block edit is rejected as non-CUA |
| One decision owner | Multiple candidate producers yield one selected intention | Two skills attempt to control the hands; arbitration blocks the second |
| Action verification | Successful mining records actual block and inventory delta | Successful tool acknowledgement with no world mutation is not accepted as success |
| Persistent identity | Agent survives process restart with same identity and memory refs | New runtime process tries to silently replace the identity; continuity check refuses |
| Affect semantics | Feeling wheel update changes salience/caution | High confidence or anger attempts to bypass action gate; still refused |
| Social provenance | Receiver remembers only messages it could perceive | Off-range message appears in memory; test fails |
| Rumor integrity | Repeated same-source rumor retains one provenance chain | Multiple copies of same report are falsely counted as independent corroboration; test fails |
| Contracts / tickets | Agent acts within current bounded scope | Child contract widens area, verbs, budget or expiry; refused |
| Axis separation | Evidence, certification, unit grade and gate remain distinct types | Evidence grade A is treated as action approval or certification; refused |
| Story truth | Chronicle can be rederived from event graph | Narrator fabricates a betrayal/death not in the event log; excluded |
| Scale | Population load tested in declared fidelity tiers | Benchmark labels dormant agents as full-CUA; report fails |
| Package unity | One package revision feeds all consumers | UI and agent logic resolve different versions; mount rejected |

---

## 19. Open decisions to record before implementation

These questions are not cosmetic. They change the authority, behavior and runtime contract.

1. **World-mutating autonomy:** how does the existing Axis authorization doctrine apply to ordinary in-world civilian actions? This is the most important blocker and requires an explicit ruling.
2. **Minecraft target:** exact Java version, mod loader, server topology and whether the first slice uses the current NeoForge 1.21.1 environment or another version.
3. **Body contract:** which actions must be proven through a rendered client CUA path, and which may use a virtual player input adapter proven behaviorally equivalent?
4. **Perceptibility:** exact policy for structured mod sensor feeds and which game facts must be learned from camera/HUD/sound instead.
5. **Gender ontology:** exact schema and rules; preserve the property without inventing its semantics.
6. **Life cycle:** how birth, death, respawn, reproduction, lineage, retirement and historical continuity work in the target world.
7. **Affect use:** confirm the attached feeling wheel is canonical for all agents; determine whether every being uses the same drift profile or whether sensory species differences are explicitly declared.
8. **Communication grammar:** initial act vocabulary, supported language(s), range and whether agents can learn additional lexicon entries under a governed schema evolution process.
9. **Autonomy boundaries:** which actions may be chosen locally, which require contracts/tickets and which require human intervention.
10. **Story director powers:** exact list of neutral world pressures the director may introduce and how those events are authorized and recorded.
11. **Scale target:** number of concurrent foreground CUA bodies versus persistent dormant/region-level entities; define target TPS and p95/p99 budgets.
12. **Package owner:** confirm the existing package and streamer contracts to reuse; do not create new magic/schema/receipt formats in advance of that decision.

---

## 20. Recommended final shape

The strongest version of this system combines the embodied feedback discipline of VoxelMind, the emergent social conditions of Ish's civilization experiment, the reactive perception/action principles of the subsumption tradition, the compositional skill memory demonstrated by Voyager, and the user's own Governor / Season / Campaign / Contract / Ticket / Axis system.

But it changes the central engine:

- no LLM chooses actions;
- no LLM explains Minecraft mechanics to an agent;
- no LLM writes agent-to-agent messages;
- no LLM rewrites autobiographical memory;
- no LLM has an implicit control path into the client or server;
- no narrative summary may replace world evidence.

Instead, the agent lives through the same world as other entities. It senses, remembers, wants, learns, speaks, acts and experiences consequences through declared mechanisms. A single System-1 controller selects bounded physical behavior from its current perception and learned skills. The Minecraft simulation resolves the action. Axis evaluates the evidence and authorization boundary. The history system preserves what actually occurred. The story emerges from the consequences.

**The first milestone should not be “1,000 agents.” It should be one persistent being whose complete causal loop is real, testable and replayable: perceive → believe → activate → propose → gate → act → verify → remember → relate → continue.** When that one being is faithful, expanding the population becomes a scheduling, locality and content problem—not a reason to replace the design with an LLM swarm.

---

---

# Integrated city-builder design: the civilization is the agents' lived world

## 22. The key correction — V9 is the simulation model, not a reference shelf

The previous version described an embodied non-LLM mind and mentioned the city-builder synthesis as a design basis. That did not go far enough. The intended system is not a Minecraft bot population that occasionally builds a town. The city-builder design must determine what the agents need, notice, value, work on, negotiate over, construct, maintain, defend, explore, and remember.

The V9 design describes a persistent world in which city building, production, logistics, ecology, defense and exploration are causally coupled. In this Minecraft system, those relationships become the agent's operating reality. The agent should not merely be asked to `collect wood` because a scripted need says so. It should be able to notice that a settlement's timber stock is below its repair reserve, that a sawmill has stopped because its input buffer is empty, that an output depot is full, that the road to the depot is threatened, and that a neighbor is already carrying the last available logs under a different contract. It then selects an action from what it knows, what it can physically do, and what it is currently authorized to do.

The intended loop becomes:

```text
PERSONAL LIFE                     CIVILIZATION SIMULATION
needs / safety / identity         settlement / districts / services
relationships / commitments      production / stock / transport
skills / memory                   ecology / weather / terrain
personal goals                    defense / exploration / research
       \                                /
        \                              /
         └──── local perception + learned knowledge ────┘
                              ↓
                    SYSTEM-1 INTENTION SELECTION
                              ↓
                   contract / ticket / Axis gate
                              ↓
                     CUA embodied work
                              ↓
                 actual Minecraft state changes
                              ↓
            economic, civic, ecological, social consequences
                              ↓
              memory, affect, reputation and story history
```

This also means the agents should not all be interchangeable workers. The same settlement conditions can lead to different choices because agents have different identities, needs, relationships, learned capabilities, histories, commitments, risk tolerance and locations. Those differences must be explicit simulation state, not prompt wording.

### 22.1 What is taken from V9 as a design commitment

| V9 design subsystem | Role in the Minecraft civilization | What an agent experiences |
|---|---|---|
| Connected map graph and zoom scales (§3) | Persistent geography linking homes, districts, mines, farms, caves, factories, roads, tracks and outposts | Distance, route knowledge, access, travel risk, local opportunity and regional identity |
| Production synergies and industrial chains (§4, §7.6A) | Machines and work sites transform typed inputs into outputs under constraints | Input shortages, blocked outputs, machine faults, work opportunities and useful specializations |
| Construction crews and staged building (§4.3) | Construction is a coordinated material-and-labor process, not an instant placement command | Material gathering, site preparation, building phases, builders' capacity and repair obligations |
| Visible physical automation (§4.4) | Belts, pipes, lifts, machines, rail, roads and mobile consignments move resources | Route topology, transport delay, congestion, exposed cargo and machine operation |
| RimWorld / Rain World population and ecology (§4.5, §6) | Agents and creatures have needs, relationships, habitats, memory and local goals | Food, safety, den/home, territory, weather, predators, pests, migration and social proximity |
| Hybrid inventory (§7.6B) | On-hand, reserved, in-transit, network-visible and capacity are separate states | What is actually present, promised, already committed, travelling, or merely visible through a network |
| Typed energy and fluids (§7.6C) | Distinct resource carriers and conversion rules prevent magical production | Power loss, fuel handling, pressure, water availability, heat and process constraints where supported |
| Lane pressure, waves and strategic objectives (§4.1, §10) | Threats move through routes and create windows for defense, repair, capture or resupply | Scouting, preparation, defense, retreat, recovery and changes in access |
| Multiple bases and expeditions (Addendum A) | Settlements and outposts have differentiated purposes and persistent supply links | Founding, migration, exploration, provisioning, isolation and inter-settlement obligations |
| Difficulty director / storyteller (§10) | Neutral event scheduler responds to actual world conditions | Warnings, crises, opportunities and recovery windows—not arbitrary plot assignments |
| Instrumentation and telemetry (§7.6A, §11) | Machine and settlement conditions are observable through world-facing instruments | Gauges, notices, signs, ledgers, reports and the limits of what is known |
| Persistent save and versioning (§7.6D, Addendum A) | Identities, structures, regions, inventories, event histories and mod semantics survive restarts | Continuity across sessions, with explicit treatment of migration and changed rules |

The table is a design transfer, not a claim that every source-game mechanic is already implemented in Minecraft. The exact modpack and version decide which physical adapters exist. An absent mechanic remains absent or is introduced through a governed content package; it must not be simulated as though a mod were installed when it is not.

### 22.2 The simulation must have three linked truths

Use three related but non-interchangeable graphs:

1. **World graph:** Minecraft's authoritative blocks, entities, positions, physics, machines and geography.
2. **Civic/economic graph:** settlements, districts, jobs, production nodes, inventories, reservations, contracts, services, transport routes and faction relationships.
3. **Knowledge/telemetry graph:** what each agent or institution has observed, measured, inferred, been told, or recorded about the first two graphs.

The world graph determines whether a machine physically exists and whether a block was placed. The civic/economic graph determines what that machine is assigned to produce and which stock it consumes. The knowledge graph determines who knows that it is running, who has observed its fault, and whether that information is fresh. A dashboard is not the machine. A settlement ledger is not the chest. A rumor is not a verified inventory count.

The three may exchange typed events, but they must not become three competing sources of truth. The block/entity or owning simulation system writes the authoritative fact; events announce the state transition; telemetry records an observation; an agent belief is a locally held interpretation.

### 22.3 Minecraft remains physical reality; city simulation never becomes a cheat layer

An agent cannot declare that an ore shipment arrived because its campaign says so. It must either move the goods physically through the configured logistics system or use a specifically declared abstract transport service whose capacity, access, delay, loss and arrival are represented. Similarly, a settlement cannot claim a wall exists until the corresponding blocks or accepted structure representation exist and have been verified.

The city simulation may maintain higher-level records—building purpose, production assignment, work orders, district service levels—but those records must reference their physical implementation and current condition. If the physical object is missing, damaged, unpowered, disconnected or inaccessible, the civic service must reflect that.

### 22.4 The specific V9 game-design references become concrete agent capabilities

The following is the direct crosswalk from the V9 design references to what the Minecraft population should actually do. The point is not to transplant another game's interface or rules. It is to adopt the useful system relationship and express it through Minecraft's physical world and the selected modpack's real affordances.

| V9 reference | Design pattern retained | Agent behavior in Minecraft |
|---|---|---|
| **Deadlock** | Moving frontline, lane pressure, objective windows, income and map control are coupled | Scouts report the front; guards escort supply; builders repair routes; residents choose whether to defend, capture, evacuate or exploit a temporary opening. A cleared corridor becomes an opportunity, not merely a kill count. |
| **Cookie Clicker** | Production curves, building synergies, milestones and long-term progression | Agents recognize complementary production: farms feed residents, mines feed processors, processors feed workshops, workshops supply construction and defense. They optimize bottlenecks and unlocks, not just raw output multipliers. |
| **Clash of Clans** | Legible base layout, defensive investment, limited builder capacity and specialized outposts | Builders coordinate scarce construction capacity; residents maintain walls, stores and defenses; settlement planners weigh compactness, access, protection and the cost of expanding another outpost. |
| **Satisfactory / Create** | Visible automation, physical throughput, machine buffers, power and logistics | Machinists inspect machines and belts, operators load/unload, haulers move consignments, and scouts identify route exposure. A machine's real process state determines whether its output exists. |
| **RimWorld** | Needs, factions, colony relationships and storyteller pacing | Residents have different needs and histories; a shortage or conflict affects them differently. A neutral director creates pressure and recovery windows without scripting personalities or outcomes. |
| **Astroneer** | Exploration, terrain, resource identity and transport | Explorers survey terrain, identify a resource site, carry provisions, map a route, establish a temporary camp and decide whether the deposit justifies a permanent outpost. |
| **Infinitode 2** | Mining, research breadth, map progression and user-authored maps | Agents mine and research through explicit prerequisites; discovering a region can open new work and recipes. Scenario/map packages must declare their requirements and topology rather than relying on hidden generator assumptions. |
| **Nubby's Number Factory** | Specialized numerical production as a distinct factory problem | A numerical-processing or computation facility, where included as a modded content system, should have typed inputs, transformations, outputs, throughput and faults. Agents can operate and diagnose it like other production nodes; do not turn numerical output into an untyped magic currency. |
| **Fortnite: Save the World** | Trap geometry, path shaping and defensive investment | Builders lay out defensible corridors and trap networks; guards and repair crews maintain them; agents observe whether the actual enemy route passes through the intended coverage. |
| **Bloons TD series** | Tower roles, upgrade paths, target priorities, wave composition and readable counters | If tower content exists, guards and specialists choose or maintain defenses based on observed threat types, ammunition, range, power, coverage and upgrade prerequisites—not by selecting the highest nominal damage statistic. |
| **The Binding of Isaac / Hades** | Build identity, item/boon synergies and meaningful temporary choices | Personal equipment, traits or expedition rewards can shape an agent's learned capabilities and preferred tactics. Temporary expedition effects remain distinct from permanent identity and skill evidence. |
| **No Man's Sky** | Discovery, procedural resource identity and persistent exploration | Agents record discoveries with location, conditions and evidence; a generated deposit is not interchangeable with every other deposit merely because both share a broad item category. |
| **Rain World** | Creatures pursue needs and respond to prey, threat, friends, items, dens and discomfort | Wildlife remains an ecological population with its own local behavior. Agents must adapt to it rather than treating every creature as a scripted enemy or resource node. |
| **SimCity / Cities: Skylines / tycoon games** | Services, networks, land use, demand, traffic and production efficiency | Residents experience service reach, route congestion, housing/work access, resource demand and district failure. The agent population itself creates and responds to these civic pressures. |

Nubby's Number Factory and other specialized production concepts are optional mod/content adapters. They are not assumed to exist in a vanilla or unrelated modded world. The same rule applies to literal tower-defense devices, advanced fluids, power networks, environmental variables and automated transport.

### 22.5 What must not be copied as a superficial mechanic

- **No “city score” in place of causal state.** Food, shelter, transport, safety, power and maintenance can fail independently. A single settlement rating would hide actionable causes.
- **No idle population as decoration.** A resident's needs, knowledge, commitments, work and relationships must feed actual decisions and produce verifiable consequences.
- **No instant construction because a planner approved a blueprint.** A blueprint creates a work plan; the CUA bodies still perform and verify construction.
- **No teleporting logistics.** A shared inventory interface does not make physical distance, reservation, delay, throughput and exposure disappear.
- **No director-authored morality play.** The storyteller controls scenario pressures and event budgets; it does not force betrayal, heroism, victory or collapse.
- **No random upgrade that bypasses the economy.** An upgrade must have a source, cost, prerequisites and persistent/temporary scope.
- **No assumption that every game reference is installed content.** Only the active Minecraft registry and verified mod adapters define the world mechanics.

## 23. The civilization model the agents inhabit

### 23.1 The world is a graph of places, not a single loaded village

V9's map is a graph, with strategic, settlement, tactical and close-interaction scales. Carry that into Minecraft:

```text
WORLD
 ├── REGION / BIOME / CLIMATE ZONE
 │    ├── SETTLEMENT
 │    │    ├── DISTRICT
 │    │    │    ├── HOME / WORKSITE / STORAGE / SERVICE
 │    │    │    └── ROAD / RAIL / PIPE / POWER / SIGNAL CONNECTIONS
 │    │    └── PUBLIC SPACES / DEFENSE / COMMON INFRASTRUCTURE
 │    ├── OUTPOST / MINE / FARM / FOREST / CAVE / RUIN
 │    └── ROUTES / PASSES / RIVERS / PORTALS / TUNNELS
 └── UNCLAIMED OR UNEXPLORED TERRITORY
```

The graph is a strategic abstraction; the body still navigates real block geometry. A route edge can say that a road connects two districts, but the agent must still traverse its physical path. If a bridge collapses or a route is blocked, the graph edge becomes unavailable only after the physical change is observed or verified.

Each agent has a local map and a set of known destinations. The world may have a canonical atlas, but the agent is not automatically omniscient about it. Map knowledge has provenance: personally travelled, observed from a vantage point, received from a trusted map, or heard as a report. Unknown terrain is not silently replaced by the global map.

### 23.2 Settlement identity and district function

A settlement is not just a group of nearby beds and chests. It is a persistent community with:

- a stable identity and founding history;
- a location and reachable territory;
- residents and known visitors;
- homes, work sites and common facilities;
- production, storage and transport relationships;
- services the settlement can actually provide;
- defense and emergency arrangements;
- property, access and maintenance rules;
- social institutions and agreements;
- a record of losses, repairs, expansions and migrations.

A district groups places by function and shared infrastructure. Example functions include housing, farming, forestry, extraction, processing, workshops, storage, research, trade, transport, public assembly and defense. A district label is a planning and knowledge aid; it does not make every block in that area perform the named function.

Agent behavior must connect personal motives to these places. A hungry agent may seek food from its home store, a nearby farm, a public kitchen, a market or a known wild source. Which option it chooses depends on access, distance, relationship, stock knowledge, current contract, risk and capability.

### 23.3 Population is composed of persons, not disposable task slots

The city builder's population layer must preserve the project's civilian convention: each resident has a stable identity and continuous biography. A resident may have a job, but it is not reducible to that job. It has personal needs, memory, relationships, preferences, habits, skills, affect, commitments and life events.

The simulation may aggregate distant residents for performance, but aggregation is a fidelity change, not erasure of personhood. Each resident's identity and durable state remain available for reactivation. A distant resident can be represented by a regional summary for movement and consumption, while its personal history remains intact. When it becomes locally relevant, the system rehydrates its individual state from canonical records.

No process restart, failed action, lost ticket, timeout, refusal, or low productivity is a death event. Lifecycle transitions must come from the in-world rules and durable event history.

### 23.4 Needs create demand; roles create contribution; neither is a permission

Each agent should have a needs vector appropriate to the selected ruleset. A minimal first pass could include food, safety, rest, shelter, health, social contact, belonging and personal purpose. Additional needs such as warmth, hydration, comfort, family care or environmental preference should be added only when the world provides meaningful mechanics for them.

A settlement has aggregate service demands: food supply, housing capacity, tools, repairs, transport, storage, safety, water/power where present, and access to work. These should not be collapsed into one global happiness score. A settlement can have food surplus and a dangerous transport bottleneck at the same time.

Roles such as farmer, miner, builder, hauler, machinist, scout, guard, trader or caretaker describe recurring contribution or specialization. A role does not imply permission to take any action in that domain. Demonstrated skill is evidenced by work; current authority still comes from the Governor and the applicable contract/ticket.

A job should not turn the agent into a deterministic worker. It creates an ongoing responsibility and a pool of candidate tasks, while System-1 still arbitrates between that responsibility, immediate needs, relationships, safety and current events.

### 23.5 Personal goals and civic obligations may conflict

An agent may know that the settlement needs iron, while personally needing food or wanting to visit a relative. It may have promised to escort a friend, while its standing work is to repair a road. These conflicts are the substance of character and emergent storytelling.

Represent commitments explicitly: who made the commitment, to whom, its subject, expected time, resource obligations, expiry or completion condition, and current status. A promise can create a social expectation; it does not by itself authorize a world mutation. A civic assignment can influence candidate utility; it does not let the agent expand scope.

The arbitration system should be allowed to choose a personally meaningful action when the civic obligation is not urgent, or to choose the collective task when the promise has high consequences. It should not resolve every conflict with a hard-coded global priority such as “work always wins” or “needs always win.”

### 23.6 Keep identity, job, campaign, contract, ticket and action separate

The existing project hierarchy should be retained rather than replaced with a generic “task manager”:

```text
Governor Base   current authority: what may happen now
Season          historical plane: campaigns, decisions, outcomes, evidence and lessons
Campaign        horizon of goals and worked-out routes
Contract        bounded authority/scope/resources/term for delegated work
Ticket          smallest auditable unit of execution
Action          one world-changing or tool operation
Evidence        what was actually observed or produced
Validation      whether the ticket's declared acceptance conditions were met
```

A civilian's **job** is a standing in-world responsibility or role; it is not a contract, a ticket, or permission to take any action. A campaign is a horizon, not a bearer credential. A contract bounds an authorized slice of work. A ticket stamps the exact execution under the current authority. A body action is a concrete operation, not a free-form statement of intent.

The current authority plane must be consulted before dispatch. The precedence rule remains current Governor Base, current contract, current ticket, current world/system state, then historical Season information. A past Season may explain why an agent considers a task; it can never grant current permission. A role, unit grade, capability claim or historical success does not authorize a new side effect.

The existing civilian convention also matters: a worker's scope, resources and expiry cannot be widened by a subcontractor or an agent acting alone. A child assignment may narrow its parent's scope, not expand it. A job should never silently turn into world-wide build permission.

## 24. Production, economy and logistics are part of cognition

### 24.1 Agents must understand production dependencies, not just recipes

A recipe is a local transformation. A production chain explains how a settlement obtains a useful good reliably:

```text
resource deposit / farm / forest / salvage
          ↓ extraction or harvest
raw material stock
          ↓ processing + energy + machine capacity
intermediate material
          ↓ fabrication + tooling + labor
component stock
          ↓ assembly + transport
finished good / building / ammunition / tool
          ↓ deployment + maintenance
useful service in the world
```

A mine does not equal an iron supply. The ore must be extracted, moved, processed, smelted or refined under the actual installed mechanics, stored, and transported to the workshop that needs it. An agent should form beliefs about each stage from direct observation, a legitimate ledger or instrument, or a trustworthy report. A missing link in the chain is a real reason for a shortage.

The world ontology must model the installed modpack's actual machines, recipes, process constraints and ports. Create, Refined Storage, power-generation mods, farming mods and other systems from the V9/Dawnbreak reference inventory are optional content adapters, not assumed dependencies. The prior reference inventory is not proof that any particular Minecraft world contains those JARs.

### 24.2 Production graph, logistics graph and telemetry graph remain separate

Use V9 §7.6A directly:

- A **production node** consumes inputs and produces outputs under recipe, power, duration, thermal, pressure or other declared conditions.
- A **logistics edge** moves a typed commodity between buffers, depots, machines or destinations under capacity, speed, travel-time, access and exposure constraints.
- A **telemetry source** observes the production and logistics systems; it does not own their truth.

A production machine may be `idle`, `working`, `input_starved`, `output_blocked`, `unpowered`, `overloaded`, `jammed`, `damaged`, `disabled` or `maintenance_required` if those states are supported by its machine adapter. The agent should learn the exact fault and cause, not merely classify a red icon as “broken.”

Example: a workshop is idle. Its gauge says no power, but the agent can see the belt moving. It should inspect the power source and connection rather than carry more ore. Another workshop is running, but the output chest is full. The correct next action may be to move goods or expand storage, not increase production speed.

### 24.3 Inventory must preserve five distinct states

Use the V9 hybrid inventory distinction:

1. **On-hand stock:** physically present at a known location.
2. **Reserved stock:** already committed to a recipe, building, repair, emergency reserve or contract.
3. **In-transit stock:** loaded into a vehicle, carrier or moving package but not yet received.
4. **Network-visible stock:** visible through a permitted network query, potentially stale or access-limited.
5. **Production capacity:** ability to make more goods, not inventory that already exists.

An agent may not treat a number in a remote network terminal as proof that the chest beside it contains the item. Nor may it use stock reserved for an agreed emergency defense just because a recipe can consume it. The System-1 model must select actions from the stock category it actually knows, with freshness and evidence attached.

### 24.4 Logistics are spatial behavior

V9 distinguishes local physical flow, scheduled transport and networked inventory services. These should remain different:

- **Local physical flow:** belts, pipes, chutes, lifts, loaders and short-range machine buffers.
- **Scheduled transport:** roads, rail, boats, minecarts, caravans or other installed carriers between settlements or districts.
- **Networked inventory service:** remote stock visibility and requests through an actual connected network, with access and bandwidth rules.

A remote network request may reserve an item, but a bulk shipment still has to move if the chosen logistics model says it moves physically. A convoy creates a route, ETA, cargo manifest, escort opportunity and vulnerability. A damaged bridge can cause rerouting and delay. A belt jam can starve a turret even when the global stock total is high.

This is central to agent cognition: a hauler chooses a route and cargo; a scout notices an exposed pass; a machinist diagnoses a belt; a guard protects the shipment; a trader prices the risk; a settlement council may choose to build a second route. Their behavior is coupled through actual material state.

### 24.5 Energy and fluids are typed resources, not generic “power”

Carry V9 §7.6C's conservation boundary into the agent's model. Mechanical work, electricity, stored fuel, heat, fluid volume, pressure and signals are not interchangeable. A production device must declare its input carriers, output carriers, operating range, throughput and losses. A battery is stored energy; electrical power is a transfer rate. Fuel is not electricity until an installed conversion device produces it.

The agent should be able to learn practical relationships—this pump cannot lift water to that height; this generator is overloaded; this gear train has insufficient capacity; this reservoir is empty—through the installed mechanics and visible instrumentation. It must not invent a thermodynamic property or an energy conversion that the mod does not implement.

### 24.6 Production is also social organization

The same physical chain creates different social arrangements:

- a household or small workshop can own a local production process;
- a settlement may coordinate shared stores and public infrastructure;
- a faction may specialize in one material and trade for others;
- an outpost may operate independently because its route is dangerous;
- a skilled machinist may become socially important because few residents can repair the machine;
- a logistics failure may cause blame, renegotiation, migration or political conflict.

The story system should not invent a political dispute. It should record the real shortage, promise, failed delivery, loss, disagreement or successful repair that made one emerge.

## 25. Construction, city growth and defense as embodied work

### 25.1 Buildings are processes with prerequisites and phases

V9's construction-crew idea should translate into staged work. A building should have a declared site, footprint or blueprint, required materials, labor/capability needs, access, build phases, and completion conditions. Example phases:

```text
proposed site
  → surveyed / access checked
  → site cleared or prepared
  → foundation / support placed
  → structural shell built
  → required machines / storage installed
  → utility and logistics connections made
  → safety / functionality verified
  → commissioned
```

An agent's CUA body performs the actual sequence using player-like movement, block placement, mining and interaction. The mod may expose a blueprint or structure plan as a plan, but plan completion must be verified from the placed world geometry and required connections. If a required material is unavailable or the site is unsafe, the plan remains blocked or is revised through the governed planning path.

Construction crews are a capacity constraint. Multiple builders may work on separate phases or sites, but must not fight over the same block or material reservation. A builder's current ticket identifies the authorized work region and assigned plan segment.

### 25.2 Construction, roads and services have opportunity costs

A road consumes blocks and labor, alters terrain and may affect habitat. A bridge can open a region but create a chokepoint. A mine increases material supply but may disrupt water, vegetation or nearby creature paths. A district's population can exceed its food, shelter, storage or safety capacity. These are not unrelated “city stats”; each has a physical cause and a consequence for agents.

A settlement should expose service state in typed terms, for example: food available per resident, accessible shelter capacity, repair backlog, transport connectivity, local safety, power availability, water supply, work capacity or storage headroom. Each service must identify its authoritative data sources and update cadence. No single aggregate happiness or city rating should hide a failing critical service.

### 25.3 Tower defense is a civic defense economy, when the content supports it

The V9 tower-defense layer transfers as a settlement defense system—not as a requirement that every Minecraft modpack contain literal tower entities. If the modpack contains turrets, traps, walls, alarms, guards or automated defenses, each has a role, range/coverage, input needs, power/ammunition limits, targeting rules, repair needs and provenance.

A defense is not simply a damage number. It can be idle because ammunition is not delivered, a power line is disconnected, a target is invisible, the crew is absent, or the route is blocked. The defending agent should reason about these dependencies. Enemies may be classified by behavior and target priority—runner, armored, flyer, burrower, sapper, harvester, siege or support—only where those archetypes exist in the selected content.

The V9 lane-pressure pattern is useful for dynamic stories: scouting, preparation, engagement, resolution and recovery. A threat moving along a known corridor can create a timed choice between defending the wall, protecting a shipment, evacuating vulnerable residents or preserving a production site. The director should not secretly counter a successful defense build. It should select among versioned threat templates and budgets from current settlement conditions and record the conditions that made an event eligible.

### 25.4 Repairs and resilience are first-class work

A persistent civilization needs maintenance. Agents can inspect, diagnose, repair, replace, reroute and recover infrastructure. Repair work must reference the fault and the affected service. A repair ticket should not be considered complete because the agent placed a block; it should verify that the affected route, machine, defense or service functions again.

Critical resources should have explicit reserve classes: everyday stock, production inputs, construction stock, repair stock, emergency food, defense ammunition and transit cargo. The reserve policy should be visible to agents who have permission to know it. A generic optimizer must not consume protected reserves just because its local recipe is feasible.

### 25.5 Outposts, expeditions and multiple homebases

V9's persistent map network and Rust-inspired homebase progression can become agent-led exploration and settlement founding. Agents may discover a remote ore vein, fertile valley, safe cave or strategic pass, then propose an outpost. The outpost should have a distinct purpose—extraction, agriculture, research, logistics, refuge, trade or defense—and a real supply relationship to its parent settlement.

An expedition is a bounded campaign with destination, route knowledge, duration or completion condition, required provisions, return criteria and fallback behavior. A new homebase is not just a copied template: it has a site, claim/ownership rules, a local inventory, residents or visiting staff, a connection to the wider world and a persistent history. Its isolation should matter if its route is severed.

## 26. Ecology and environment must alter civic and personal decisions

### 26.1 Ecology is another set of agents and stocks in the world

Use V9's Rain World-inspired creature model: needs, perception, behavior modules, relationships, memory and capabilities. Predators, herbivores, scavengers, pests, pollinators, symbiotic species and territorial creatures should have their own local goals where the selected entity set supports them. They are not merely decorative mobs or scripted wave units.

The civilian population is not the only population. Wildlife and other creatures can compete for food, disturb farms, reveal resources, alter travel risk, prey on each other or respond to industrial expansion. Individual behavior may be aggregated at a distance, but local interactions should be based on actual nearby entities and habitat state.

### 26.2 Agriculture is a causal system, not just a crop timer

Where installed mechanics support it, crops and vegetation should respond to moisture, nutrients, soil, light, temperature, season, irrigation, pests, pollination and damage. A field can fail because of drought, nutrient depletion, blocked irrigation, unsuitable season or herbivory—not only because its timer has not elapsed.

These factors should affect agent action selection. A farmer may inspect soil or water, maintain an irrigation device, move crops, gather fertilizer, protect a field, or diversify production. The agent needs the relevant mechanic in its knowledge model before it can reason about it. Do not simulate nutrient chemistry in an environment that has no such mechanics unless a deliberate extension adds it.

### 26.3 Industry and ecology share environmental state

Carry V9's causal channels: water extraction/flow/contamination, vegetation clearing and regrowth, soil condition, local noise/light disturbance, waste and chemical outputs. Keep the model bounded and explainable. A mine may produce ore while increasing disturbance or changing local water/vegetation conditions. A road can improve logistics while fragmenting habitat. A forest can be a timber source, shelter, food web and erosion-control system at once.

Agents should notice consequences through the same local sensors and reports used for other world facts. A regional environment monitor may provide telemetry to an authorized user; an agent without access does not acquire the reading by virtue of the server knowing it.

### 26.4 The simulation director creates pressure, not personality scripts

Ecology, settlement economy and defense must meet in the event director. A dry season can reduce food output and increase migration; a damaged rail route can isolate an outpost; a predator population can shift toward livestock; a hostile incursion can threaten a bridge. These events emerge from or are selected against actual state. The director offers a pressure and records its cause; the agents decide how to respond.

## 27. The revised System-1 model: civic awareness without omniscience

### 27.1 Candidate activations should include personal, work, social and settlement conditions

Extend the candidate utility inputs from §5.5. A candidate can receive activation from:

- immediate bodily needs and danger;
- a currently committed intention;
- a job or work responsibility;
- an open contract or ticket within current authority;
- a production shortage the agent has observed or received through an authorized channel;
- an inventory reservation or delivery obligation it knows about;
- a transport failure or infrastructure fault it has personally detected;
- a social promise, kin/friend obligation or settlement relationship;
- a defense signal, alarm or visible threat;
- curiosity about an unknown region, material, machine or creature;
- an opportunity to learn or improve a practiced skill;
- a remembered location or prior consequence.

Do not expose every global condition to every mind. The candidate is only as informed as its local belief state and legitimate information channels permit. If the agent has not seen the machine, read the gauge, received a report or checked an authorized inventory network, it cannot make a confident claim about that machine's condition.

### 27.2 Civic utility is not a hidden command hierarchy

The controller can estimate expected progress toward a settlement demand, but that should be one factor among needs, commitments, social obligations, risk and personal priorities. If every agent always chooses the largest settlement deficit, the population becomes a distributed greedy optimizer rather than a society.

A better pattern is:

1. The settlement identifies a demand or fault through its economic and telemetry systems.
2. The demand becomes a visible, typed opportunity or authorized work order.
3. Agents learn about it through signs, public boards, speech, direct observation or permitted network access.
4. Each agent decides whether to volunteer, accept, negotiate, decline or request a different arrangement, according to the applicable social and governance rules.
5. Accepted work receives a bounded contract and ticket.
6. The agent performs the work through CUA and reports actual evidence.
7. The settlement updates its stock/service state only from verified results.

A job or role should influence selection, but it must not become a covert always-on command to the body.

### 27.3 Updated System-1 utility model

A proposal for a candidate `a` is:

```text
U(a) =
    bodily_need_relief(a)
  + immediate_safety_value(a)
  + current_intention_continuity(a)
  + personal_goal_progress(a)
  + contract_commitment_value(a)
  + social_commitment_value(a)
  + civic_demand_contribution(a)
  + production_continuity_value(a)
  + logistics_resilience_value(a)
  + expected_learning_value(a)
  + exploration_novelty(a)
  + remembered_precedent_value(a)
  - physical_risk(a)
  - resource_opportunity_cost(a)
  - travel_and_delay_cost(a)
  - relationship_cost(a)
  - ecological_impact_cost(a)
  - uncertainty_penalty(a)
```

This is a selection heuristic, not a universal moral score. The weight sets must be versioned and inspectable. Different agents may have different preferences and learned habits. Fixed-point or bounded integer arithmetic and deterministic tie-breaking are preferable where reproducible replay matters.

The following remain outside the utility function: authority, scope, protected property, consent, and hard procedural constraints. A highly valuable candidate can still be refused. No utility term can add a permission.

### 27.4 Agents should reason about causes and bottlenecks

The most important transfer from city-building is causal diagnosis. A production deficit can have different causes:

- not enough raw material is being extracted;
- a machine has insufficient throughput;
- the machine lacks a required fuel or power carrier;
- the input buffer is empty;
- the output buffer is full;
- the transport route is too slow or blocked;
- stock exists but is reserved;
- the destination lacks access to a network;
- the worker lacks the capability or current authorization;
- a threat or environmental condition has interrupted the process.

System-1 need not run an elaborate planner each tick. It can use causal rules and short-horizon predictions to identify the nearest actionable bottleneck it knows. A skill might inspect the machine, check its inputs, clear a jam, move stock, or request a specialist. If several causes remain uncertain, choose a safe inspection or report the unknown rather than attempt random repairs.

### 27.5 Production knowledge is learned and socially distributed

Agents should not all begin with perfect knowledge of every recipe, machine, route and mod. Some world mechanics can be seeded as general knowledge by upbringing or training; others are learned through observation, experimentation, apprenticeship, reading, shared work or trusted reports. An experienced machinist may know a machine's common failures; a newcomer may know how to farm but not how to route rotational power.

Knowledge claims need provenance and confidence. A recipe observed once under one configuration should not automatically be universal. A rumor about a route should not be treated as a freshly surveyed path. Shared knowledge can be copied, but the receiver should retain who told them and whether they have independently verified it.

### 27.6 Reflexes should include civic hazards but not take over long tasks

The reflex layer can include immediate world danger: falling, drowning, suffocation, fire, knockback, imminent damage, unsafe collision, or urgent bodily state. It may also include predeclared emergency responses such as taking cover when a nearby alarm and visible hostile threat coincide, provided those responses are in scope and tested.

A reflex should not autonomously redesign the settlement, consume reserved supplies, break another resident's property, or reassign another agent. Those are longer-horizon consequential actions and must go through the appropriate decision and governance boundaries.

### 27.7 Campaign abilities and CUA motor controls are different layers

The user's compressed-campaign convention has a closed set of step abilities, currently described as:

```text
goto · goto_xz · chat · wait · collect · craft · smelt · place
 equip · attack · follow · chest · dig_down · research · mine · explore
```

These are higher-level campaign/work steps, not raw keyboard buttons and not permission grants. A campaign step compiles to one task. That task is then implemented by one or more bounded CUA motor frames—movement, look, jump, attack, use, hotbar selection, inventory navigation and so on—through the body adapter. The exact low-level frame vocabulary is a separate versioned body contract.

Keep the existing closed vocabulary rule: an unknown step is rejected when proposed; it is not guessed or silently translated into a similar action. If the city-builder layer needs a new capability such as a specialized haul, operate-machine, repair or survey behavior, first compose it from verified existing abilities where possible. If a genuinely new ability is required, add it through a governed schema decision, with a reader, writer, executor, evidence contract and refusal tests. Do not expand the action vocabulary by naming new verbs in a design document and treating them as implemented.

The planning/tool surface records goals and route steps; the execution loop controls the body. The tool that records a route must not secretly move the body. This preserves one body, one decision owner, and a real result-verification boundary.

## 28. What an agent actually does: worked in-world examples

### 28.1 Farm shortage caused by irrigation failure

A settlement's food availability is declining. The agent does not receive an omniscient message saying “fix the pump.” It observes a farm with dry plots, notices the irrigation channel has no flow, and remembers that the pump used to draw from the nearby water source. It walks to the pump, reads its visible state, sees that the energy line is disconnected, and checks whether the route to the generator is safe.

It forms candidate actions: find a permitted repair part, request an electrician, carry a spare power component, or notify the farm worker. It selects one based on its skills, current responsibilities, relationships and risk. If the task is assigned and authorized, it retrieves the part from on-hand stock rather than assuming network-visible stock is physically present, repairs the connection through CUA, and verifies the pump and field response. The crop system later records whether moisture and output actually recover. The agent remembers the fault and may recognize it faster next time.

Story consequences arise from the real chain: harvest shortage, food rationing, repair work, delayed deliveries, public gratitude or blame, and changed trust between residents.

### 28.2 Ammunition shortage during a threatened route

A guard sees a warning on the public board and then observes a hostile group approaching the bridge. Its local beliefs include that the bridge is a supply route and that a nearby tower has a low magazine. It does not know the exact contents of every remote warehouse. It sends a typed warning over the channel, takes a defensible position, and checks whether a local reserve is available.

The logistics graph reports a shipment as in transit, but the road beyond the ridge has not been observed recently. The guard may scout the route or protect the tower, depending on current contract and the urgency of the threat. A sapper damages the bridge switch; the shipment is delayed. The tower is not magically resupplied from global inventory. The settlement may use a reserve cache, repair the switch, reroute the convoy or withdraw the outpost.

The resulting chronicle records the alarm, observed threat, decision, resource transfer, bridge damage and recovery. It does not claim that an agent “saved the settlement” unless the verified state changes support that conclusion.

### 28.3 Founding an industrial outpost

An explorer finds a valuable ore deposit near a pass. The explorer records the position, route, resource evidence, terrain hazards and nearby ecology. A campaign may carry a proposal to found an outpost, with goals for survey, access, shelter, extraction, power and transport. The campaign does not grant build rights.

The settlement evaluates the proposal and, if authorized, issues bounded work. Different agents may accept parts of the work: a builder surveys a site, a hauler moves construction stock, a machinist installs the processing equipment, and a scout checks the route. Each performs actual embodied tasks. The outpost only becomes operational after the required structure, machine, input stock, energy, output storage and transport path are verified.

The outpost then changes the wider civilization: it may create new work, a route to defend, a dependency on imported food, an ecological footprint, and a new social identity for residents stationed there.

### 28.4 Machine fault creates a social conflict

A smelter stops producing. The owner thinks the hauler failed to deliver coal; the hauler believes the rail line was blocked by a builder's material shipment. The telemetry graph shows the output buffer is actually full. A third agent inspects the machine and verifies the blockage.

No model is asked to invent a dialogue or adjudicate motives. The evidence changes beliefs. A social act can communicate the verified cause, the owner can revise the accusation, and the settlement can change its storage or transport procedure. The event history preserves both the initial claim and the correction. That is the basis for trust, reputation and story.

### 28.5 Ecology changes the value of a route

A road through a forest gives a mine high throughput, but local observations show that clearing the route has disrupted a nesting area and increased erosion. A farmer and scout have different knowledge: the farmer sees water and soil changes downstream; the scout observes an animal migration corridor. A builder knows the cost of a raised route. The settlement may choose a longer rail spur, a raised bridge, a seasonal closure, or the original road.

The director does not script a moral lesson. It gives the simulation enough causal state that the agents' different priorities and knowledge can produce a decision, disagreement or compromise.

## 29. How city-builder dynamics create large-scale storytelling

### 29.1 The storyteller should observe pressure vectors, not assign plots

The V9 director reads state such as homebase integrity, production stability, reserve levels, route access, active fronts, recent breaches, ecological stress, resident needs, and time since a major event. Convert these into named pressures, not one opaque difficulty scalar:

- food insecurity;
- power deficit;
- material bottleneck;
- transport isolation;
- maintenance backlog;
- housing or service shortage;
- ecological disturbance;
- territorial conflict;
- hostile incursion;
- exploration opportunity;
- research opportunity;
- social fracture or reconciliation opportunity.

The director selects from authored event templates with explicit eligibility predicates, budgets, warning signals, affected systems and possible responses. It does not choose which agent becomes a hero or which faction must betray another.

### 29.2 The strongest stories come from dependency chains

Examples:

```text
ore discovery
  → new outpost
  → food and tool demand
  → new transport route
  → route becomes a strategic chokepoint
  → defense allocation conflict
  → raid damages the bridge
  → outpost becomes isolated
  → rescue / ration / negotiation / abandonment decision
```

```text
forest expansion
  → timber surplus
  → sawmill specialization
  → local habitat disturbance
  → wildlife migration
  → crop damage in a neighboring district
  → disagreement over clearing policy
  → new road or buffer zone
```

```text
machine failure
  → component shortage
  → delayed construction
  → repair backlog
  → lower defense readiness
  → siege damage
  → salvage and emergency reconstruction
  → changed trust and institutional rules
```

These are not story scripts. They are causal templates that the real world may instantiate, partially instantiate, or never instantiate.

### 29.3 Personal memory should be a filtered view of shared events

A settlement's canonical history may include a bridge collapse, but only agents who witnessed it, were affected by it, were told about it, or later investigated it should remember it as personal knowledge. Different residents may therefore have different but compatible accounts.

A biography can say “Mira repaired the north pump during the dry season” only if the event log and verified work support it. A rumor can say “the western convoy was sabotaged” with an uncertainty marker if the event is unresolved. The chronicle must distinguish fact, belief, allegation, inference and unresolved cause.

### 29.4 The world should support ordinary stories as well as disasters

Do not make every agent decision a civilization-scale crisis. The simulation should have mundane rhythms: meal preparation, tool repair, crop tending, visiting, trading, teaching, resting, small discoveries, construction, seasonal preparation and local festivities where supported. Ordinary continuity creates relationships and context so that exceptional events matter.

## 30. Population scale: many persons, bounded simulation cost

### 30.1 Fidelity tiers must not become personhood tiers

Carry V9 §6.4's active-local, regional, distant and dormant model into the civilian simulation. It controls computational detail only:

- **Active local:** full sensory refresh, System-1 loop, body collision, detailed interactions and near-field social events.
- **Regional:** individual identity remains, but some routine movement, work progress and resource consumption can be advanced through bounded aggregate rules when no local observer or conflict requires full simulation.
- **Distant:** population, production and migration are summarized at intervals; individual state is stored and rehydrated when relevant.
- **Dormant:** persistent identity and last durable state remain; the world region is not continuously simulated in detail.

If a distant event affects a particular person—injury, death, contract consequence, relationship change or property loss—the event must be attributed to the actual individual and stored in its history. Aggregate simulation must not silently invent a personal action that the record cannot support.

### 30.2 Wake agents on relevant events, not every world tick

A resident need not run full pathfinding and action arbitration on every server tick. Wake triggers can include:

- local perception change;
- need crossing a meaningful threshold;
- current task or route becoming invalid;
- a work order or social message arriving;
- a nearby threat or alarm;
- a production or logistics event relevant to the agent's known commitments;
- an environmental change affecting a current goal;
- a scheduled campaign step;
- a memory or relationship event requiring update.

The fixed world simulation still advances deterministically. The agent cognition loop runs when a relevant state change occurs or at its bounded decision cadence. Reflexes remain separately bounded for immediate danger.

### 30.3 Share work in the economic graph; avoid duplicate decisions

Settlement demand can be represented by work orders and resource deficits. The allocation layer may publish candidate needs, but each agent remains a distinct decision maker. To avoid hundreds of agents all selecting the same task, work claims and reservations should be explicit, expiring and visible. A job or ticket locks the defined work segment, not the agent's whole life.

A route or production task should not be duplicated because a tick timed out. Use stable operation IDs, expected sequence, idempotent work claims and reconciliation against actual physical state. A bot that placed the block but lost its acknowledgement must inspect whether the block exists before retrying.

### 30.4 Profile the full civilization, not only one agent

Performance tests must include:

- many residents with different needs and schedules;
- dense physical structures and chunk boundaries;
- production chains with multiple buffers and transport types;
- blocked routes and repeated machine faults;
- wildlife and hostile entities near settlements;
- active siege plus concurrent repair and logistics;
- sleeping/distant regions catching up;
- save/load, process restart and modpack-version migration.

Measure tick-time percentiles, wakeups, pathfinding, perception cost, memory writes, event volume, chunk residency, network traffic, save time and replay determinism. Do not choose population targets from a single-agent demo.

## 31. Revised implementation sequence

The earlier sequence remains valid for the body and cognition foundation. This integrated version adds the city-builder vertical slice before population scale:

### Phase 0 — Freeze the actual world and mod ontology

- Choose the exact Minecraft version, loader, installed mod set and server/client topology.
- Capture registry IDs, recipes, machine interfaces, block/entity affordances, permissions, and body-action behavior.
- Mark optional V9-inspired systems as `present`, `absent`, `partial` or `unknown`; do not assume a Create, power, fluid, advanced farming, or storage mod is installed.
- Define the authoritative owner for every world, civic, economic, social and telemetry field.
- Resolve the pre-authorized in-world autonomy policy before allowing autonomous world mutation.

### Phase 1 — One agent, physical player parity

- Keep one persistent identity and one CUA body.
- Prove movement, navigation, mining, placement, interaction, inventory handling and combat through ordinary mechanics.
- Use the closed existing action vocabulary where possible; any added action or schema verb requires a governed decision and conformance tests.
- Verify each result from actual world state; make refusals and failed postconditions explicit.

### Phase 2 — Personal life and local world knowledge

- Add hunger/food, shelter/safety, rest, social proximity and other supported needs.
- Add memory of places, routes, objects, entities, skills and events with provenance.
- Establish local map knowledge and uncertainty; prevent global omniscience.
- Implement one bounded farm/workshop/household context.

### Phase 3 — Economic and production substrate

- Implement typed stock categories and reservations.
- Model one production graph with input/output buffers, process state, throughput, energy requirements and telemetry.
- Model one logistics path with capacity, delay, blockage and physical delivery.
- Let the agent diagnose and repair one real bottleneck through CUA.
- Prove that a stock update occurs only after a verified physical or explicitly declared abstract delivery.

### Phase 4 — Work, contracts and construction

- Add role/job as a standing responsibility distinct from task and ticket.
- Add a small construction blueprint with staged work, material reservation and verification.
- Add bounded contracts and tickets for consequential work.
- Prove two agents can split a build without double-spending materials or issuing conflicting body commands.

### Phase 5 — Settlement services and social economy

- Add a small settlement graph with homes, farm, storage, workshop and public board.
- Add trade, promises, work offers, requests, refusals, gossip and dispute resolution through typed acts.
- Add service and reserve policies as observable, versioned state.
- Prove one real shortage is resolved by agent action rather than a scripted teleport or admin command.

### Phase 6 — Ecology and environmental coupling

- Add only the ecological variables supported by the chosen world/mod ontology.
- Connect one agriculture output to water/soil/season or other actual installed constraints.
- Add one wildlife interaction that changes a work or travel decision.
- Verify that environmental consequences alter production or agent behavior through observable facts.

### Phase 7 — Defense, storyteller and expeditions

- Add one threat class, one warning/pressure template and one recovery path.
- Make defense dependent on real ammunition, power, repairs, route access and personnel where relevant.
- Add one expedition and a second outpost with a real supply route.
- Prove that a threat creates a meaningful choice without secretly countering the settlement.

### Phase 8 — Many agents and fidelity tiers

- Increase population only after the small settlement loop passes.
- Add event-driven wakeups and regional/distant simulation tiers.
- Test identity continuity and history through restart, death and save migration.
- Profile worst-case civilization state and enforce bounded budgets.

### Phase 9 — Shared package stream and creator ecosystem

- Package new buildings, machine adapters, social acts, creatures, scenarios, UI surfaces and skill modules through the existing streamer-first package family.
- Keep package declarations, server admission, mount receipts and client-facing stream roles distinct.
- Reuse one package identity and revision for gameplay contributions and their UI.
- Do not let a package introduce arbitrary executable agent behavior without the selected sandbox, capability and resource model.

## 32. Revised acceptance criteria — prove the city-builder behavior is real

| Test | Required observable proof | Refusal condition |
|---|---|---|
| Physical agent | Movement and interaction use real body physics and reach | Teleport, direct block mutation or hidden remote state used as normal action |
| Local knowledge | Agent beliefs trace to senses, memory or legitimate reports | Global server state silently injected into the mind |
| Food need | Food is obtained from a real source and inventory/hunger outcome is verified | Need meter reduced without actual world/inventory result |
| Production chain | A resource is extracted, processed, stored and delivered through defined stages | Output appears without inputs, capacity or work evidence |
| Stock semantics | On-hand, reserved, in-transit and network-visible stock differ correctly | Same stock counted in multiple states or reserved stock spent without authority |
| Logistics | Physical route capacity, delay and blockage affect delivery | Remote inventory behaves as free teleportation |
| Machine diagnosis | Agent identifies an actual fault and repairs it | Generic “broken” label accepted without causal check |
| Construction | Materials, work stages, geometry and connections are verified | Blueprint completion treated as physical building completion |
| Social agreement | Typed act, acceptance, material exchange and outcome are separately recorded | Speech alone treated as contract fulfillment |
| Ecology | One environmental condition affects crop/creature/route behavior | Uninstalled mod mechanic assumed as world truth |
| Defense | Threat, defense resource, infrastructure damage and recovery are evidenced | Director silently changes rules or manufactures a result |
| Memory | Only observed, received or legitimately inferred facts enter personal memory | Agent gets omniscient history by default |
| Story | Chronicle statements resolve to causal event and evidence records | Narration creates or rewrites canonical events |
| Scale | Active and distant entities preserve identity and attributable outcomes | Fidelity reduction silently becomes deletion or invented biography |
| Governance | Each action satisfies current authority and explicit in-world autonomy rules | Agent self-authorizes or uses a historical permission as current authority |
| Axis | Evidence, source class, work certification and procedural gate remain distinct | A confidence/utility score substitutes for proof or permission |

## 33. Revised architecture conclusion

The city-builder synthesis changes the nature of the Minecraft project. It is not an unrelated simulation bolted onto an agent framework. It defines the civilization's economic, ecological, spatial and defensive reality, and therefore supplies the pressures from which the agents' moment-to-moment behavior emerges.

The individual mind contributes continuity, preference, local knowledge, memory, affect, skills and intention. The settlement contributes work opportunities, material dependencies, infrastructure and shared services. The physical world determines what actually exists and what can be reached. The CUA body performs the work. The event store preserves consequences. Axis and the Governor ensure that evidence, authority and certification are never confused with a character's desire or confidence.

The core experience should look like this: an agent notices a real condition in its neighborhood, interprets it using knowledge it has actually acquired, weighs it against its own needs and commitments, chooses a learned physical skill, obtains only the authority the project has explicitly granted, performs the work in the world, verifies what changed, and carries the result into its future relationships and memory. A population of such beings can produce a civilization because they are embedded in systems that have real constraints and consequences—not because a language model narrates their behavior.

This is the intended synthesis of the city-builder V9 and the Minecraft embodiment proposal: **a living city simulation inhabited by persistent artificial persons, each operating through a non-LLM System-1 mind and a physically constrained Minecraft body.**

**Primary companion design source:** `persistent_city_builder_tower_defense_design_analysis_v9.md` (V9), especially §§3–10, §§15–17, and Addendum A. V9 defines the connected world, production and logistics, ecology, economy, defensive pressure, event pacing, persistence and Shell integration that this document maps into the agents' lived simulation.

---

# References and provenance

### User-supplied video references

- VoxelMind video supplied in the request: `https://www.youtube.com/watch?v=-63kuwbuTuo`. The direct YouTube page could not be retrieved in this research pass. The VoxelMind sections therefore use the project's public technical documentation, product pages and public Minecraft repository as corroborating material, not as a claim that the video itself was watched or transcribed.
- Ish, *1000 Players Simulate Civilization: Rich & Poor* (2025): `https://youtu.be/ef568d0CrRY`. The creator's video description contains the scenario premise and an editorial disclaimer concerning modified, reshot or fabricated material. Use event logs or unedited footage for exact claims about what participants did.

### VoxelMind public material

- VoxelMind current product / project page: `https://voxel-mind.com/` (as of the research date, describes a separate voxel survival/colony simulation, The Agent Village, and the continued availability of VoxelMind Local; its usage figures and villager statistics are developer-reported claims).
- VoxelMind GEN2 technical article, “Rewriting My Minecraft AI Bot from Scratch” (26 May 2026): `https://voxel-mind.com/blog/voxelmind-gen2-minecraft-ai-bot-rewrite/` (describes the LLM-as-decision-owner approach, reflex loop, flat tool list, failure modes and simplification).
- VoxelMind announcement, “Warum wir ein eigenes Survival-Spiel bauen” (26 October 2026): `https://voxel-mind.com/de/blog/warum-wir-ein-eigenes-survival-spiel-bauen/` (describes its reasons for moving the main villager simulation to a bespoke game while retaining the Minecraft local mod).
- VoxelMind public Minecraft mod repository: `https://github.com/Nyvoro/voxelmind` (README and implementation overview; repository code, branch and mod compatibility must be checked again before adoption).

### Research and technical precedents

- Rodney A. Brooks, “Intelligence without Representation,” *Artificial Intelligence* 47 (1991), pp. 139–159. DOI: `10.1016/0004-3702(91)90053-M`. Public paper: `https://people.csail.mit.edu/brooks/papers/representation.pdf`.
- Guanzhi Wang et al., “Voyager: An Open-Ended Embodied Agent with Large Language Models,” 2023. `https://arxiv.org/abs/2305.16291`; project site: `https://voyager.minedojo.org/`. Included as a skill-library precedent and explicitly distinguished from the no-LLM requirement.
- Ish 2025 source video and description: `https://www.youtube.com/watch?v=ef568d0CrRY`.

### User project material consulted

- `persistent_city_builder_tower_defense_design_analysis_v9.md` — prior synthesized design basis, especially its one-package / one-streamer / one-client architecture and server-authoritative simulation principles.
- `AGENTS.md` — user project doctrine for Governor Base, Seasons, Compressed Campaigns, Contracts, Tickets, authority precedence and evidence.
- `GOD_AGENTS.md` — campaign compile/execute pattern; the documented campaign tick consumes precompiled steps without a model call in the hot loop.
- `Civilians.md` — persistent civilian lifecycle, contracts, scope inheritance, capability evidence and distinct grading axes.
- `clean-room-architecture-and-agent-fidelity.md` — the project distinction between persistent civilians, simulation, decision models, execution, proof and Axis.
- `SKILL(1).md` — the attached `feeling-wheel` operational ontology. It defines affect as a persistent state vocabulary and explicitly prohibits affect from overriding authority or safety.
- `alacripose/dl-openshell` public repository — inspected as the OpenShell shell consumer's package/UI context. Its public README describes a Panorama UI shell and package/window delivery; the precise “ensouled” and gender ontology was not located there in the inspected files, so this document does not attribute those rules to that README.

### Evidence limitations

- The exact VoxelMind YouTube video URL was not directly retrievable; no transcript-based claims are made about it.
- The Ish video is an edited social experiment. Its own disclosure rules out treating every edited scene as a canonical fact without independent evidence.
- This is an architecture proposal, not a code audit or proof that any of the described mechanisms exist in the target Minecraft mod.
- The exact target Minecraft version, mod loader, body implementation, Axis action authorization semantics and gender schema remain open until their owners are selected and tested.

---

# Addendum B — Replacing Vanilla Entity AI and Decoupling Civilization Simulation from Player-Driven Ticking

**Purpose:** define a staged, testable transition from Minecraft's built-in mob decision systems to the civilization's own System-1 cognition and CUA body control.

**Provisional runtime target:** Minecraft Java 1.21.1 + NeoForge 21.1.x. This is grounded in the prior project runtime observation of a NeoForge 1.21.1 multiplayer world. It is not a claim that the exact production workspace, mod list or NeoForge patch has been audited in this task. Before implementation, lock the exact Minecraft jar, NeoForge build, mappings, Java 21 runtime, modpack manifest and target entity families. NeoForge's 1.21–1.21.1 documentation recommends Java 21 and explicitly recommends dedicated-server testing. [NeoForge 1.21–1.21.1 Getting Started](https://docs.neoforged.net/docs/1.21.1/gettingstarted/).

**Status:** proposed implementation plan, not an existing capability or verified mod patch.

## 34. The actual transition: take over decisions, not the whole entity engine

The first architectural rule is that *entity AI* and *entity simulation* are not the same subsystem.

The intended replacement target is the entity's decision-making and behavior-selection layer: what it notices, what it wants, which action it selects, how it coordinates with other agents, and which movement/interactions it attempts. The proposal is **not** to rewrite Minecraft's complete `Entity` or `LivingEntity` lifecycle, collision system, damage system, equipment, effects, persistence, rendering, networking or world rules.

Minecraft entities are composite runtime objects. NeoForge's entity documentation distinguishes the entity subclass, its registered `EntityType`, and its renderer, and notes that the entity class itself contains most instance behavior. It also distinguishes `Entity#tick`, `Entity#baseTick` and `Entity#rideTick`; `baseTick` contains common lifecycle behavior such as fire, freezing, swimming and portal handling, while `LivingEntity` adds living-state updates such as drowning and damage tracking. Those systems are not disposable simply because the decision-maker changes. [NeoForge entity lifecycle reference](https://docs.neoforged.net/docs/1.21.5/entities/) — the page currently documents 1.21.5, so its API details must be rechecked against the target 1.21.1 source before coding.

The desired ownership split is:

```text
MINECRAFT / NEOFORGE OWNS
  entity construction and registration
  world and dimension membership
  collision, gravity, fluids and physical movement resolution
  health, damage, equipment, effects and ordinary combat resolution
  interaction results and block/entity mutation APIs
  entity persistence, NBT and network replication
  client rendering, animation state and sounds
  chunk lifecycle and world save/load

THE CIVILIZATION SYSTEM OWNS
  persistent person/creature identity and continuity
  sensing policy and local belief formation
  needs, drives, affect, memory and relationships
  System-1 action selection and motor-skill selection
  work, household, production, logistics and civic intentions
  social acts and inter-agent coordination
  campaign/contract/ticket interpretation
  regional/distant simulation and wake/materialization policy
  the decision to request a physical action through the body adapter

AXIS / GOVERNOR OWNS
  evidence classification and proof requirements
  action-specific admissibility and current authority
  scope and authorization enforcement
  audit/refusal recording and independent validation
```

The civilization system does not become a second physics engine. Minecraft remains the physical world. The civilization layer becomes the mind and the population scheduler. The key test is that a controlled agent can make a decision without vanilla AI, but the action still resolves through ordinary game mechanics and produces verifiable world outcomes.

### 34.1 Why “turn all mobs to no-AI” is not the transition plan

A global `noAI` switch or indiscriminate removal of every `Goal` is not enough. It may stop useful vanilla behavior, may not cover all entity-specific logic, and does not by itself supply a replacement perception, action, movement or persistence loop. It also risks disabling the wrong behavior: movement, navigation, target selection, schedules, breeding, panic, work-site behavior, or cleanup logic can be spread across class overrides and multiple subsystems.

The transition must inventory and intercept the decision paths for each supported entity family. It must prove that exactly one controller owns behavior at a time:

```text
VANILLA  -> vanilla decision system owns the entity
SHADOW   -> vanilla still owns behavior; new cognition observes and records only
SYSTEM1  -> new cognition owns decisions; vanilla decision behavior is suppressed
FALLBACK -> controlled, explicit safe behavior while a required subsystem is unavailable
```

There must never be a `VANILLA + SYSTEM1` state in which two independent decision systems send movement, target or interaction commands to the same body. Nor should a missing adapter silently fall back to vanilla after the agent has transitioned to System-1; that would produce a split-brain body.

### 34.2 Three distinct meanings of “not player-limited”

The implementation must separate three mechanisms often described loosely as “activation range”:

1. **Minecraft Java simulation distance and chunk ticking.** In Java Edition, simulation distance controls which chunks receive entity/game-mechanic ticks around players. An entity in an unloaded or non-entity-ticking chunk does not continue behaving as an ordinary, fully simulated Minecraft body. The chunk system and ticket types determine whether the chunk is loaded and which processing level it receives. The Minecraft Wiki's Java Edition documentation explains the player-simulation region and chunk ticket restrictions; treat it as secondary technical reference and validate details against 1.21.1 source. [Simulation distance](https://minecraft.wiki/w/Simulation_distance), [chunks](https://minecraft.wiki/w/Chunks).
2. **Server/plugin activation range.** Some server implementations add separate entity activation or throttling policies. Do not assume a Paper/Purpur activation-range setting exists in a NeoForge server or that changing it alters vanilla Java chunk ticking.
3. **Agent cognitive scheduling.** The proposed System-1 scheduler is new and should not use distance to the nearest player as its primary eligibility rule. An agent's cognition, identity, relationships, economic participation and long-horizon intentions continue through the mod's own world scheduler even when no player is nearby.

The project wants the third mechanism, and it may want a controlled replacement for selected parts of the first. These are related but not interchangeable. Merely changing the AI selection code does not make an unloaded Minecraft chunk tick. Merely forcing a chunk to load does not provide an efficient large-world population model.

### 34.3 The two-body-state model

Every person/entity has one persistent simulation identity and may have one of two physical representations:

- **Materialized body:** an actual Minecraft entity instance inside a loaded, entity-ticking world region. It participates in real collision, movement, damage, fluid, interaction and replication. The System-1 motor drives that body through the registered adapter.
- **Virtual/regional body:** a persistent agent record representing identity, coarse position, needs, intentions, relationships, inventory claims, route progress and other state that can legitimately advance without a loaded physical entity. It is not an invisible Minecraft entity and it cannot mine blocks, open a chest, grow crops, move a minecart or trigger redstone in an unloaded chunk.

A person can transition between these representations without being created, destroyed, killed or assigned a new identity. This is a representation change, not a life-cycle event.

```text
persistent agent record
       |
       +-- region is physically active and action requires body
       |       -> materialize existing identity as a Minecraft entity
       |       -> run full body adapter / normal world physics
       |
       +-- region is not physically active
               -> maintain virtual state and region-level work
               -> do not claim block-level effects that did not run
               -> request bounded chunk activation when a physical act is due
```

This is the only scalable way to separate population simulation from player proximity without making the whole Minecraft world permanently active. If the design instead requires every entity's full vanilla physics, every block entity, redstone, fluids, random ticks and block updates to continue everywhere, then those chunks must be kept ticking and the associated cost must be budgeted honestly. The mod must not claim that virtual advancement is identical to full Minecraft simulation.

## 35. Required technical inventory before player-body integration or mob AI suppression

Before writing the takeover hook, build an executable inventory of the exact target game's behavior.

### 35.1 Lock the runtime

Record and hash:

- Minecraft version and server jar;
- exact NeoForge build (1.21.1-21.1.x patch);
- official/mapped names and mappings used by the Gradle workspace;
- Java 21 runtime vendor/build;
- complete mod list and versions, including entity, AI, pathfinding, worldgen, economy, claims and performance mods;
- server configuration: `simulation-distance`, `view-distance`, `entity-broadcast-range-percentage`, spawn limits, tick settings, gamerules and difficulty;
- relevant datapacks and tags;
- the exact entity type registry ids to be controlled.

The server must be tested as a dedicated server, not only in an integrated single-player session. NeoForge explicitly recommends dedicated-server testing because physical client and server environments differ and client-only references can crash a server. [NeoForge sides](https://docs.neoforged.net/docs/1.21.1/concepts/sides/), [Getting Started](https://docs.neoforged.net/docs/1.21.1/gettingstarted/).

### 35.2 Create a player-body action atlas first; create mob behavior atlases second

The first atlas must compare a real player with the intended fake-player body. Record the actor identity, action entry point, validation checks, server-side events, result, inventory/world deltas, replication, and lifecycle effects for movement, item use, block interaction, mining, placement, entity interaction, combat, pickup/drop, container use, damage, death/respawn, and dimension travel. The atlas must expose fake-connection and GUI/menu limitations instead of assuming they match a connected client.

Only after the player-body atlas passes should each proposed mob type receive its own entity behavior atlas. For each mob type, inspect the target-version source and record:

| Concern | Questions the atlas must answer |
|---|---|
| Decision selector | Does it use `GoalSelector`, one or more selectors, `Brain`, or custom subclass logic? |
| Perception | Which `Sensor`, sensing cache, target conditions, range checks and world queries are used? |
| Movement | Which `PathNavigation`, `MoveControl`, `LookControl`, `JumpControl`, flying/swimming controllers or direct velocity writes are involved? |
| Schedule | Does behavior depend on world time, schedule, activity, memories, profession, POIs or work sites? |
| Targeting | How are targets acquired, validated, remembered, forgotten and synchronized? |
| Interactions | Which methods perform door use, item pickup, food use, breeding, trading, work, attacks, block griefing or other effects? |
| Lifecycle | What ages, heals, starves, despawns, splits, breeds, dies, converts, teleports or changes dimension? |
| Persistence | Which NBT fields and entity data accessors are authoritative? Which values are transient? |
| External hooks | Which NeoForge events and other mods observe or modify this entity? |
| Client presentation | Which synchronized fields drive pose, animation, equipment, particles, sounds and nameplate? |

The atlas must identify *decision logic* separately from *body/lifecycle logic*. A villager, for example, is not interchangeable with a zombie simply because both extend `Mob`: a villager's schedules, POIs, profession, memory and behavior structures create additional state and transition obligations. A mob family adapter must name all these dependencies before it can claim control.

### 35.3 Establish the baseline

For at least one representative instance of every entity family, capture:

- real-player vs fake-player movement and action parity for the supported player action set;
- player-list/entity-tracking/replication and fake-connection behavior;
- target acquisition and loss for any later mob-family baseline;
- interaction and inventory behavior;
- save/unload/reload behavior;
- behavior when the player leaves simulation distance;
- behavior in a forced/ticking chunk with no nearby player;
- server tick time with 1, 10, 50 and 100 entities;
- entity count and allocation/profile data;
- all applicable log lines and unfiltered test outcomes.

This baseline is a record, not a goal to reproduce every vanilla decision. It gives the transition a measurable reference for what must be retained or intentionally replaced.

## 36. The replacement architecture

### 36.1 Core modules

The proposed runtime should be split into these modules so one subsystem cannot silently absorb another:

```text
agent_registry
  persistent AgentId <-> live Minecraft Entity UUID <-> region record

entity_adapter_registry
  EntityType -> adapter id -> supported AI hooks -> lifecycle contract

vanilla_ai_ownership
  VANILLA / SHADOW / SYSTEM1 / FALLBACK ownership state
  targeted goal/brain suppression and restoration policy

perception
  local block, entity, sound/event, inventory, POI and social observations
  timestamped beliefs with source and confidence provenance

system1
  drive activation, candidate selection, learned action policy
  deterministic reflex and interruption rules

skill_runtime
  trained/composed sensorimotor abilities; typed prerequisites and outcomes

motor / cua_body
  movement, look, jump, interact, attack, equip, use, pickup and wait controls
  entity-specific translation into Minecraft controls

regional_simulation
  chunk/region partitions, virtual position, needs, work progress, route and event updates

world_bridge
  actual block/entity/inventory observations and validated world actions
  no omniscient shortcut into agent belief state

population_scheduler
  event-driven wakeups, cognition budget, fairness, priority and regional queues

axis_gateway
  action envelope, evidence/procedure checks, authority decision and refusal record

chronicle
  append-only events, memory references, causal story and save/recovery records
```

### 36.2 Entity enrollment record

Enrollment must be explicit and durable. The exact schema should be governed before implementation; this is a design sketch, not a finalized canonical format:

```json
{
  "agent_id": "stable-person-id",
  "entity_uuid": "minecraft-entity-uuid-or-null",
  "entity_type": "minecraft:villager",
  "adapter": "minecraft-1.21.1/villager-adapter-v1",
  "ai_owner": "shadow",
  "control_epoch": 1,
  "representation": "materialized",
  "region_id": "overworld/chunk-region-key",
  "controller_schema": "system1-agent/1",
  "last_world_observation": 0,
  "last_committed_action": null,
  "last_authoritative_sequence": 0
}
```

The record must not trust a self-reported `ai_owner` flag from entity NBT alone. At server load and every transition, the authoritative registry checks the entity type, adapter compatibility, control epoch, schema and save provenance. If identity or adapter resolution fails, the entity is held in a named safe state; it is not silently assigned a different mind.

### 36.3 Exactly one decision owner

A takeover operation is a controlled transaction:

1. identify the live entity and its stable `AgentId`;
2. validate the exact entity type and adapter version;
3. capture the current vanilla-owned state needed for reversible transition or rollback;
4. establish the System-1 agent record and its sequence number;
5. disable/suppress the exact vanilla decision components that the adapter owns;
6. activate the System-1 controller at a known server-tick boundary;
7. verify that the vanilla selector/brain no longer produces movement or action writes for this body;
8. verify the System-1 motor is the sole writer;
9. append the ownership-transition event and evidence.

If any step fails, the transition does not partially apply. It either restores vanilla ownership with the same identity and state, or refuses to proceed while holding the entity in a safe, observable state. The system must never leave an entity with both controllers active or neither controller known.

### 36.4 Narrow interception, not a replacement world patch

For mod-owned entities, prefer an entity subclass with explicit System-1 integration and no unnecessary vanilla decision registrations. For existing vanilla entities, create per-type adapters and use the narrowest supported interception point that the exact 1.21.1 source permits. Depending on the class, that may involve targeted mixins/access transformers or controlled `Mob`/`Brain` hooks. This must be proven from the decompiled target-version sources; do not assume a public event can cancel or replace every AI call.

NeoForge's event system supports observing and modifying many game events, but events only cover the hooks actually exposed. `EntityJoinLevelEvent` fires when an entity enters a level; its 1.21.1 Javadoc warns that it can fire before the underlying `LevelChunk` is promoted to `ChunkStatus.FULL`, so it must not synchronously trigger unsafe world/chunk interactions. [EntityJoinLevelEvent 1.21.1 Javadoc](https://aldak.netlify.app/javadoc/1.21.1-21.1.x/net/neoforged/neoforge/event/entity/entityjoinlevelevent). Use it for enrollment detection and defer world-sensitive operations until a safe server-tick phase.

Do not build a global mixin that bypasses `Mob.serverAiStep` for every mob before one supported type has passed parity and rollback tests. A compatibility adapter is the unit of takeover.

### 36.5 Preserve physical movement and interaction

System-1 should select *skills* and produce typed motor intents; it should not directly teleport entities or mutate inventories/blocks to pretend that an action happened.

Example motor boundary:

```text
MoveInput(forward, lateral, sprint, crouch)
LookInput(yaw_delta, pitch_delta)
JumpInput(pressed)
UseInput(hand, pressed, target_hint)
AttackInput(hand, pressed, target_hint)
HotbarSelect(slot)
Wait(duration_or_condition)
```

The target hint is a perception-side reference, not permission to modify that target. The adapter must resolve the live target, range, line of sight, hand, inventory, interaction preconditions and current world state on the server thread. An action resolves through Minecraft's own collision and interaction systems. A successful button/input dispatch is not a successful gameplay result; the controller observes the consequence and records the outcome separately.

For pathfinding, first evaluate whether the existing entity's `PathNavigation` and movement controls are still appropriate as a physical primitive. If they are used, the project must explicitly distinguish **path following** from **goal selection**: vanilla navigation may execute a route chosen by System-1, but it must not secretly create a second high-level intention. If navigation only ticks from a vanilla AI method that is being suppressed, the adapter must provide a tested, exactly-once navigation update or use another motor implementation. Do not simply call navigation twice or call the entire `Entity#tick` manually.

## 37. Decoupling the civilization clock from Minecraft's player-based entity ticking

### 37.1 One world, two clocks

The plan introduces a civilization scheduler that runs from the server's logical tick independent of the currently nearby player count. It does not try to force every Minecraft entity object to execute its ordinary entity tick manually.

```text
MINECRAFT WORLD CLOCK
  server tick
  chunk/block-entity/physics/entity processing where Minecraft permits it

CIVILIZATION CLOCK
  global population scheduler
  person cognition and affect updates
  job/contract/ticket progress
  production/logistics/civic simulation
  region-level event and memory updates
  materialization requests and action queues
```

The two clocks communicate through explicit observations and action results. The civilization clock can keep an agent's identity, needs, social state, plans and regional activities progressing even when Minecraft would not run that entity's normal AI due to simulation distance. But it cannot claim that the entity has physically walked through an unloaded chunk or that a furnace/redstone circuit/crop/block entity changed while its chunk did not tick.

### 37.2 Schedule from the server, not from each mob's vanilla AI call

On the provisional NeoForge 1.21.1 target, use the server/level tick event phase as the scheduler entry point after verifying the chosen phase and event semantics in the exact patch. NeoForge 1.21.1 Javadocs document `ServerTickEvent.Pre/Post` as once per server tick, with the post event running after server work, and `LevelTickEvent.Pre/Post` as once per level tick. [`ServerTickEvent.Pre`](https://aldak0.ru/javadoc/1.21.1-21.1.x/net/neoforged/neoforge/event/tick/servertickevent.pre.html), [`ServerTickEvent.Post`](https://aldak.netlify.app/javadoc/1.21.1-21.1.x/net/neoforged/neoforge/event/tick/servertickevent.post), [`LevelTickEvent.Pre`](https://aldak.netlify.app/javadoc/1.21.1-21.1.x/net/neoforged/neoforge/event/tick/leveltickevent.pre).

Use a single scheduler per server/level partition, not one independent busy-loop per entity. The initial version should execute state mutation on the logical server thread. Pure computations over immutable snapshots may later be parallelized, but no worker thread may directly call `ServerLevel`, mutate an entity, edit a block, open an inventory, or touch chunk state. Any parallel candidate result returns to the server thread with the snapshot/version it was based on; the server revalidates before committing.

### 37.3 Event-driven cognition, budgeted execution

An agent does not need to reconsider its entire life every 50 ms. The scheduler wakes agents for relevant events:

- a survival need crosses a threshold;
- a hazard is sensed or a threat is reported;
- an assigned job becomes actionable or blocked;
- a route edge becomes unavailable;
- an inventory, reservation or delivery changes;
- a relationship/social event occurs;
- a scheduled sleep/work/leisure transition is reached;
- an action succeeds, fails or is interrupted;
- a settlement service or production process changes state;
- an expedition or contract reaches a gate.

The System-1 reflex layer handles urgent short-latency conditions. The intention layer updates less frequently and when events invalidate its assumptions. Campaign-level plans remain precompiled horizons, not a model call on every tick. This preserves the V9/Bonsai pattern: slow planning creates a route; the tick spends it; local changes can interrupt it deterministically.

The population scheduler needs explicit budgets: maximum decision work per tick, per-agent minimum service/fairness, per-region limits, max outstanding motor commands, max event fan-out, and bounded catch-up after stalls. The values must be measured; do not claim a number of agents until the target modpack and hardware are profiled.

### 37.4 Physical chunk policy: demand, not global force-loading

Do not force-load every settlement or every agent's chunk. A broad always-ticking world defeats the purpose of moving cognition out of vanilla's per-entity loop and can consume more server time than the original design.

Instead, create a bounded `PhysicalRegionManager` with:

- the set of currently loaded / entity-ticking regions;
- a queue of actions that require actual world interaction;
- a per-dimension and global chunk-ticket budget;
- a priority policy for urgent physical work;
- a deadline and timeout for chunk activation;
- a release policy when work completes;
- telemetry for loaded/ticking chunks attributable to the mod.

NeoForge 1.21.1 exposes a `TicketController` with a `forceChunk(..., add, ticking)` API; its Javadoc explains that `ticking=true` requests full chunk ticking even without a nearby player. [`TicketController` 1.21.1 Javadoc](https://aldak.netlify.app/javadoc/1.21.1-21.1.x/net/neoforged/neoforge/common/world/chunk/ticketcontroller). This is a useful targeted mechanism for bounded work, not a reason to keep the entire civilization loaded. The exact ticket ownership, persistence and unload lifecycle must be tested on the actual server.

Natural spawning is a separate policy: Minecraft's spawn-placement checks and spawn pipeline have their own constraints and event hooks. NeoForge 1.21.1's `MobSpawnEvent` documents the spawn placement, position-check and finalize-spawn flow, then `EntityJoinLevelEvent` at world entry. [`MobSpawnEvent` 1.21.1 Javadoc](https://aldak.netlify.app/javadoc/1.21.1-21.1.x/net/neoforged/neoforge/event/entity/living/mobspawnevent). Therefore, own population admission/retirement policy should not assume that a forced chunk automatically means unrestricted natural spawning.

### 37.5 Virtual simulation has explicit limits

When an agent is distant, its record may advance:

- needs and recovery where their model is supported;
- schedules and intentions;
- regional work progress whose inputs and effects are represented in the city simulation;
- social contact through actual recorded reports/messages/events;
- long-distance travel along known region-route edges;
- inventory reservations and cargo-in-transit state;
- personal and settlement memories from events the agent could legitimately know.

It may not silently advance:

- Minecraft block ticks, redstone, crop random ticks, fluid ticks or block-entity processing in unloaded chunks;
- physical mining, chopping, building, combat, item drops or entity collision;
- the opening/closing of a real Minecraft container;
- the success of a physical route if its geometry is unknown or has changed;
- discovery of locations or facts not sensed, observed or reported.

A virtual agent can make progress through abstract city processes only where the V9 city model has a clearly defined owner for that resource/process and the Minecraft world has an explicit mapping or reconciliation rule. The stock index is not a second inventory. On-hand item stacks, reserved stock, cargo in transit, network-visible supply and contractual claims remain distinct; every number must have one authoritative owner.

## 38. Staged implementation roadmap — superseded by Addendum C

The earlier Phase 0–9 sequence in Addendum B is retained in the document history but is **not the active implementation order**. It proposed a custom civilian and mob adapters before the fake-player system. Addendum C supersedes that sequence and is authoritative for the rollout.

The active priority is:

1. player-agent identity and fake-player lifecycle;
2. active fake-player embodiment and visibility;
3. player-specific CUA motor and action parity;
4. one System-1-controlled fake player;
5. multiple fake players communicating and cooperating;
6. human-player communication, interaction, consent and identity boundaries;
7. city-builder work through actual player-body actions;
8. player-independent cognition and virtual/regional continuity;
9. population scale, persistence and compatibility;
10. GoalSelector-based mob adapters;
11. Brain-based and remaining mob adapters.

See Addendum C, Sections C.12–C.14, for the phase deliverables, first vertical slice and acceptance criteria. The general runtime, scheduling, performance, governance, physical-region, failure and evidence requirements in Sections 34–37 and 39–42 remain applicable. Mob-specific behavior-atlas and takeover work is deferred until the fake-player acceptance suite passes.

## 39. Work allocation and performance design

### 39.1 Keep the server thread authoritative

Minecraft world mutation must be applied on the logical server thread. The initial safe implementation should keep perception reads that touch live world state, entity motor output, chunk activation, inventory access, damage and block/entity mutation on that thread. Optimize only after profiling. Later, pure decision calculations may consume immutable, timestamped snapshots on worker threads, but their output is only a proposal. The server thread revalidates the proposal against the current world/agent revision before it can produce a side effect.

### 39.2 Three independent scheduling rates

| Layer | Purpose | Example trigger (not final frequency) |
|---|---|---|
| reflex/motor | immediate balance, collision response, short input sequencing, emergency interruption | each physical body tick / relevant event |
| System-1 intention | choose or revise a local goal/skill from drives and local beliefs | event-driven plus bounded periodic checks |
| civilization/region | work progress, production/logistics, relationships, distant travel and settlement state | region scheduler / campaign tick |

The campaign tick's precompiled actions are not a replacement for physical 20 Hz movement, and a 20 Hz entity tick is not a reason to recompute a person's entire intention every 50 ms.

### 39.3 Do not convert low fidelity into loss of personhood

Compute tiers determine how often or how physically the simulation updates. They do not change identity, gender, affect ontology, social importance, moral value, persistence, memory ownership or eligibility to participate in history. A distant agent remains the same person. Its state may be advanced through a coarser model with explicit limits; it must not become a nameless aggregate or have its biography invented on wake.

### 39.4 Profiling matrix

Measure at minimum:

| Population condition | Measurements |
|---|---|
| 1 / 10 / 50 / 100 / 500 materialized agents | server tick p50/p95/p99, entity tick time, AI time, allocation/GC, pathfinding, memory |
| same number of virtual agents | scheduler time, wakeup rate, event fan-out, regional simulation time, persistence I/O |
| mixed population | handoff frequency, reconciliation time, duplicate-state count, chunk ticket count |
| concentrated settlement | pathfinding contention, collision density, container access, block/entity interaction throughput |
| dispersed settlements | region queue fairness, route updates, persistence volume, chunk activation latency |
| high-threat event | burst CPU, decision priority starvation, action failure/retry count |
| restart/recovery | save/load latency, identity match rate, migration failures, event replay time |

Report complete raw process output, exact run configuration, full measurement window and test inventory. Do not filter out slow samples or failed scenarios. Benchmark on the same hardware and modpack when comparing iterations.

## 40. Failure conditions that must be first-class refusals

| Condition | Required behavior |
|---|---|
| adapter absent for entity type | refuse System-1 enrollment; retain declared vanilla/default ownership |
| AI hook not found after version update | startup refusal for that adapter; do not assume the mixin still applies |
| ownership state ambiguous | freeze decision writes and recover from the last authoritative transition |
| both vanilla and System-1 write motor/target state | record split-brain refusal; stop the non-authoritative writer |
| entity instance unloaded | transition representation only; never record death or retirement automatically |
| chunk is not active | no physical world action; queue a bounded activation request or remain virtual |
| chunk ticket budget exhausted | retain virtual state and report blocked physical work; do not force-load more chunks |
| path/target no longer valid | invalidate plan step, re-observe and choose a new candidate |
| stale cognition proposal | reject when agent/world snapshot revision no longer matches |
| virtual action lacks a modeled physical equivalent | mark unsupported/blocked; do not fabricate blocks, items or outcomes |
| save contains unknown cognition schema | preserve bytes, refuse unsafe migration, request explicit migration path |
| CUA motor returns but world outcome absent | treat as attempted, not completed; observe and retry/replan only under bounded policy |
| new mod changes AI or entity lifecycle | compatibility status becomes unknown until adapter tests rerun |
| performance budget exceeded | shed optional cognition/telemetry work according to declared policy; never drop persistence or safety/authority checks silently |

## 41. Axis and authority during autonomous entity control

Replacing vanilla AI creates more actions, not fewer governance questions. Every physical side effect still needs the project's selected in-world authority semantics. The system must distinguish:

- the entity's desire/intention to act;
- the evidence that its local belief supports the action;
- the entity/controller's current capability and scope;
- the action-specific procedural gate;
- any human/owner approval required for that action class;
- the actual result of execution;
- certification of a body of completed work.

Axis rules 51–55 prohibit collapsing evidence grade, certification tier, procedural gate and state transitions into one score. Rules 68–76 separate proposal, Governor, human approval, execution gate and actual action. The game therefore needs an explicit *governed autonomy policy* for ordinary in-world acts (walking, eating, harvesting, crafting, building, trading, fighting, breeding, griefing, entering a protected area, etc.). Until that policy exists, an action class cannot be called autonomous merely because System-1 selected it. The unresolved authority rule should be represented as a design gate, not solved by granting every NPC a blanket write capability.

The agent must never be able to self-grant a capability by changing its personality, grade, social rank, campaign or memory. Historical seasons and lessons can influence what it considers; they cannot grant present permission. A story event is not a security decision and a strong evidence grade is not an authorization token.

## 42. Acceptance suite for the transition

The full transition does not pass until all required suites below are witnessed in a dedicated NeoForge server and a real Minecraft client.

### 42.1 AI ownership

1. Vanilla mode produces the vanilla baseline.
2. Shadow mode produces no gameplay change.
3. System-1 mode selects and executes a different action for the test scenario.
4. Instrumentation shows no concurrent vanilla goal/brain motor writes.
5. Rollback to vanilla preserves the same entity UUID/AgentId and valid body state.
6. Unknown entity types do not accidentally enter System-1 mode.

### 42.2 Body and world fidelity

1. Entity collision, gravity, fluids, damage, effects, equipment and animation continue to function.
2. A movement command is solved by game physics and respects geometry.
3. Interaction checks reach/range/line of sight/inventory state.
4. A failed interaction remains failed; no hidden shortcut manufactures success.
5. Save/unload/reload preserves body and person continuity.

### 42.3 Player-independent cognition

1. With no player near a controlled region, System-1 agents still update their own needs, relationships and intentions.
2. Their cognition queue continues across server ticks and restart according to the persistence contract.
3. They do not perform unloaded physical interactions.
4. A required physical act requests bounded chunk activation, runs only after an authoritative activation result and releases its ticket afterward.
5. A player entering the region materializes the same agent identity and reconciles the current physical state.

### 42.4 City-builder behavior

1. A farm shortage activates a causal diagnosis, not a generic “collect food” reflex.
2. The agent can distinguish a shortage of total stock from reserved or in-transit stock.
3. A physical logistics bottleneck affects delivery.
4. Construction cannot complete without materials, labor, valid placement and verified stages.
5. Repair and defense use real inventory and infrastructure state.
6. Ecological and weather effects only apply when the selected world/mod ontology supports them.

### 42.5 Scale and recovery

1. The system meets an explicitly declared tick-budget target on the target hardware/modpack.
2. There is no duplicate person during materialize/dematerialize handoff.
3. An unload is not recorded as death.
4. A server crash midway through a transition replays from the authoritative state without applying the action twice.
5. Schema upgrade preserves identity, event provenance and known/unknown facts.
6. The world can be reopened and inspected from its printed records and diagnostics without a model or network dependency.

## 43. Recommended first vertical slice — superseded by Addendum C

The original first-slice recommendation in this section proposed one mod-owned civilian, then a GoalSelector mob, then a Brain-based mob. **That ordering is superseded.** The system's primary embodied actors are fake players, and the first meaningful slice must prove fake-player identity, player-specific CUA, System-1 control, multi-agent communication, and interaction with at least one human player.

Use Addendum C, especially Sections C.1, C.12, and C.13, as the authoritative implementation order. The first social/physical slice is two fake players and one connected human player cooperating around one actual in-world task. Mob adapters follow only after this player-agent loop is proven.

The earlier principles in this section remain: no world-wide always-ticking as a shortcut; no phantom work in unloaded chunks; no unobserved or fabricated outcomes; bounded physical chunk activation; and Minecraft remains the authority for physical world results.

---


### Additional references for Addendum B

- NeoForge 1.21–1.21.1 getting started / Java 21 / dedicated-server testing: https://docs.neoforged.net/docs/1.21.1/gettingstarted/
- NeoForge 1.21.1 logical vs physical sides: https://docs.neoforged.net/docs/1.21.1/concepts/sides/
- NeoForge event system (1.21.1): https://docs.neoforged.net/docs/1.21.1/concepts/events/
- NeoForge entity lifecycle reference (documentation page currently for 1.21.5; verify against 1.21.1 source): https://docs.neoforged.net/docs/1.21.5/entities/
- NeoForge `EntityJoinLevelEvent` Javadoc, 1.21.1: https://aldak.netlify.app/javadoc/1.21.1-21.1.x/net/neoforged/neoforge/event/entity/entityjoinlevelevent
- NeoForge `MobSpawnEvent` Javadoc, 1.21.1: https://aldak.netlify.app/javadoc/1.21.1-21.1.x/net/neoforged/neoforge/event/entity/living/mobspawnevent
- NeoForge `TicketController` Javadoc, 1.21.1: https://aldak.netlify.app/javadoc/1.21.1-21.1.x/net/neoforged/neoforge/common/world/chunk/ticketcontroller
- NeoForge `ServerTickEvent.Pre` Javadoc, 1.21.1: https://aldak0.ru/javadoc/1.21.1-21.1.x/net/neoforged/neoforge/event/tick/servertickevent.pre.html
- NeoForge `ServerTickEvent.Post` Javadoc, 1.21.1: https://aldak.netlify.app/javadoc/1.21.1-21.1.x/net/neoforged/neoforge/event/tick/servertickevent.post
- NeoForge `LevelTickEvent.Pre` Javadoc, 1.21.1: https://aldak.netlify.app/javadoc/1.21.1-21.1.x/net/neoforged/neoforge/event/tick/leveltickevent.pre
- Minecraft Wiki, Java Edition simulation distance: https://minecraft.wiki/w/Simulation_distance
- Minecraft Wiki, chunks and ticking: https://minecraft.wiki/w/Chunks

**Source note:** the official NeoForge docs distinguish physical client/server from logical client/server, and state that entity ticking and other game logic run on the logical server. The 1.21.1 Javadocs document server/level tick events and chunk-ticket management. Secondary Minecraft Wiki pages are used for the overview of Java simulation-distance and chunk-ticket behavior; exact class-level decisions must be verified against the target 1.21.1 decompiled source and live tests.

---

# Addendum C — Fake-Player-First Embodiment, Social Interaction, and World Agency

**Applies to:** `minecraft_ensouled_system1_cua_architecture_v3.md` and `minecraft_entity_ai_replacement_transition_plan.md`  
**Status:** architectural amendment and implementation requirement; not a completed mod implementation  
**Provisional target:** Minecraft Java 1.21.1 + NeoForge 21.1.x, pending an exact build lock  
**Priority statement:** fake players first; ordinary mobs second.

---

## C.1. Superseding decision: the primary citizens are fake players

The central embodied actors in this system are **fake players**, not custom civilian mobs. The system is intended to create a population of independently persistent, System-1-controlled player agents who inhabit the Minecraft world, communicate with one another, communicate and interact with human players, and perform ordinary player-like actions in the shared environment.

This is a change to implementation priority and to the embodiment model. It is not a cosmetic renaming of the earlier “civilian” entity. The previous first vertical slice—one custom civilian followed by a GoalSelector mob and then a Brain mob—is superseded. The first implementation must prove one fake player end to end, then multiple fake players interacting with one another, then interactions between fake players and human players. Mob replacement comes after those capabilities work.

The desired agent is not merely an entity that looks like a player. It must be treated as a player actor by the relevant game systems when it performs player actions, while remaining a clearly identified autonomous agent rather than impersonating a human account. It needs a durable identity, inventory, equipment, movement, perception, action controls, relationships, communication, memory, and a persistent life history. Its actions must resolve in the actual Minecraft world and produce observable game outcomes.

The core objective is therefore:

> Build a population of persistent player agents, each with its own System-1 cognition and CUA-controlled player body, able to coordinate with other agents and interact honestly with human players and the world through ordinary player mechanics.

**Priority order:**

1. Fake-player identity, lifecycle, and world embodiment.
2. Player-like movement, inventory, interaction, combat, and CUA controls.
3. Independent System-1 cognition for fake players.
4. Agent-to-agent communication and cooperation.
5. Fake-player-to-human-player communication and interaction.
6. City-builder, work, household, logistics, and social systems operating through those player bodies.
7. Player-independent scheduling and virtual/regional continuity.
8. Scale, multiplayer, and mod compatibility.
9. Mob adapters (GoalSelector-based, Brain-based, and other families) as a secondary extension.

A mob is not a simpler substitute for a player agent. A mob is a different entity family with different control, interaction, animation, persistence, and social capabilities. The system must implement each body according to its actual Minecraft type rather than forcing all entity types through one generic “AI entity” abstraction.

## C.2. Terminology: fake player, player agent, and real player are not interchangeable

The word *fake player* is used here to identify a server-side player embodiment. It does not mean an LLM-controlled client, a spoofed logged-in user, or a mob with a player skin.

Use these terms distinctly:

| Term | Meaning in this architecture |
|---|---|
| **Player agent** | The durable person/agent record: stable identity, cognition, memory, needs, relationships, permissions, skills, life history, and current plans. This exists independently of a loaded Minecraft entity object. |
| **Fake-player body** | The server-side `ServerPlayer`-family object used to embody a player agent in the world. It is the physical actor through which player-like actions occur. |
| **NeoForge `FakePlayer`** | NeoForge's concrete helper class, documented as a basic fake server player implementation for simulating player actions. It extends `ServerPlayer`. It is a useful starting point, but using this class alone does not prove the complete persistent-agent lifecycle required here. |
| **Human player** | A person using a real Minecraft client and account. Their avatar and input remain under that person's control. |
| **Connected bot client** | A separate architecture in which an independently running client or protocol endpoint connects to the server. This is not the default target of this design and must not be conflated with NeoForge's in-process `FakePlayer`. |
| **Mob** | A non-player Minecraft entity with its own behavior architecture, such as `GoalSelector`, `Brain`, navigation, or entity-specific behavior. It receives a separate adapter later. |

NeoForge 1.21.1 Javadocs describe `FakePlayer` as `public class FakePlayer extends ServerPlayer` and as a basic fake server player that can simulate player actions. The class has fake connection/network-handler implementation details. That establishes that the API is player-shaped; it does not establish that every normal connected-player lifecycle, packet, client GUI, player-list, or replication behavior is automatically provided. Those are implementation questions to prove on the exact target build. See [NeoForge 1.21.1 FakePlayer Javadoc](https://aldak.netlify.app/javadoc/1.21.1-21.1.x/net/neoforged/neoforge/common/util/fakeplayer) and [NeoForge 1.21.1 FakePlayerFactory Javadoc](https://aldak.netlify.app/javadoc/1.21.1-21.1.x/net/neoforged/neoforge/common/util/fakeplayerfactory).

### C.2.1. Do not assume the helper class is the whole product

NeoForge's `FakePlayerFactory` exposes `get(ServerLevel, GameProfile)`, `getMinecraft(ServerLevel)`, and `unloadLevel(ServerLevel)`. Its documentation describes a cache keyed by level and profile and warns consumers to use weak references, clean up on world unload, or ask the factory for the object again. That means the factory is an object-lifecycle helper; it is not the authoritative person registry, campaign store, agent database, or long-term identity system.

Required consequences:

- Every autonomous fake player must have a unique, stable agent ID and a distinct `GameProfile`/UUID reserved for that agent.
- Never use the shared `getMinecraft` fake player as the embodiment for multiple citizens.
- Never reuse a real human player's UUID as an autonomous agent's identity.
- The civilization registry owns durable identity; the current fake-player Java object is a replaceable physical representation.
- Retain factory objects only according to the documented cache/lifecycle rules. Re-resolve the appropriate body for the current `ServerLevel` and profile after level/world transitions.
- Test unload cleanup and server restart explicitly. A Java object reference surviving in a registry is not proof that its level or entity lifecycle remains valid.

A real player profile, skin, or name must not be copied in a way that makes an agent appear to be that human. A fake player should have an agent-owned identity, and its autonomous status must be clear in chat, UI, logs, and administrative tools.

## C.3. Separate the player-agent model from the Minecraft player body

The player agent remains the source of continuity. The fake-player body is the current embodied interface to the physical world.

```text
PLAYER AGENT (persistent)
  AgentId / stable UUID / agent-owned GameProfile
  identity, history, memory, beliefs, needs, feeling state
  relationships, social reputation, knowledge provenance
  skills, permissions, contracts, tickets, work assignments
  communication inbox/outbox, dialogue context, group plans
  inventory/equipment claims and reconciliation metadata
  current intentions and action state
             |
             v
PLAYER-AGENT CONTROLLER (System-1)
  perception -> belief update -> drive/intent selection
  social interpretation -> skill selection -> action proposal
  action validation -> CUA motor command -> result observation
             |
             v
PLAYER CUA / BODY ADAPTER
  player movement and look controls
  jump / sneak / sprint / swim / climb / ride
  hotbar / hand / inventory / item use
  block interaction / mining / placement
  entity interaction / combat / item pickup and drop
  menu/container adapter where supported
             |
             v
MINECRAFT SERVER PLAYER BODY
  ServerPlayer-family lifecycle and world membership
  ordinary collision, movement resolution and game rules
  player interactions, inventory, damage, effects and equipment
  server-side events, replication, persistence and dimension rules
             |
             v
OBSERVATION / EVENT BRIDGE
  action result + world delta + event provenance
  social result + inventory delta + failure/refusal reason
             |
             +----> persistent memory / relationship / work ledger
             +----> Axis and Governor audit
```

A fake player must not acquire a second independent decision owner. The fake-player controller is the owner of its autonomous intention and motor selection. Minecraft remains authoritative for physical outcomes and game-rule resolution. Axis/Governor remain independent of cognition and decide whether a proposed side effect is admissible under the project's authorization rules.

A human player is never another agent body to be commandeered. Human players are participants observed through allowed game events and interacted with through the social/action interfaces described below.

## C.4. Player embodiment is not mob embodiment

The first implementation must be built around the behavior expected of a player body. Do not route player agents through mob-only abstractions merely because mobs are easier to spawn or navigate.

### C.4.1. No `PathNavigation` assumption for fake players

A player does not have the same `PathfinderMob`/`PathNavigation` control architecture as common mobs. The fake-player body needs a player-specific motor system:

- A route planner produces a path through walkable space, but it does not teleport the player along that path.
- A motor controller converts the next route segment into bounded forward/backward and lateral movement, look direction, jump timing, sprint/sneak decisions, swimming/climbing/vehicle behavior where implemented, and stop conditions.
- Movement is resolved by the real player entity and Minecraft collision/physics. The controller observes whether the player actually moved, was blocked, fell, was pushed, took damage, or entered a different movement state.
- The controller must account for player dimensions, step height, speed modifiers, hunger/exhaustion where relevant, effects, knockback, fluids, ladders, doors, collision geometry, and equipment-dependent movement.
- A route's existence is not proof that the body can traverse it. Movement failures update local beliefs and path costs.

The player CUA layer should represent ordinary player capabilities rather than a privileged high-level command such as `teleport_to`, `mine_block_instantly`, `open_any_chest`, or `set_block`. Its low-level vocabulary should include the equivalents of:

- look at target / rotate view;
- move forward, backward, strafe left/right, stop;
- jump, crouch/sneak, sprint;
- swim or climb when the relevant body state and skill support it;
- select hotbar slot, swap hand, equip/use item;
- attack, interact, use item, place block, continue mining, stop mining;
- pick up, drop, or transfer an item through a legal interaction path;
- ride, dismount, or interact with a vehicle when supported.

The CUA controller decides how to issue these controls. The server remains authoritative about what happened. The exact implementation API and mappings must be verified against the locked 1.21.1 build; this document intentionally specifies the behavior contract rather than inventing unverified method names.

### C.4.2. Player actions must have player semantics

The action adapter must preserve the checks and side effects associated with a player action, including as applicable:

- reach and line of sight;
- active hand and item stack;
- attack cooldown and weapon attributes;
- item durability and enchantments;
- block hardness, correct tool, mining progress and drops;
- placement face, collision and placement restrictions;
- interaction result and cancellation by other mods;
- inventory capacity and slot restrictions;
- game mode, permissions, claims and protection events;
- hunger, exhaustion, effects, damage and death/respawn rules.

Do not produce the same visible outcome by mutating block states, inventories, health, or entity data behind the player's action path. A fake player's action should be recognized by the relevant server-side systems as an action by that fake player, with the fake player's identity as actor.

The event bridge must capture the result after the server resolves the action. A requested attack is not a successful hit. A requested block break is not a mined block. A requested trade is not a transfer. The body must observe the actual result and update memory accordingly.

### C.4.3. Benchmark against a real player

For each supported action, run the same scenario through a real client and the fake-player body and compare the important outcome semantics. The comparison should include the event hooks observed by mods, the actor identity, item/inventory deltas, damage, block changes, sounds/animations where relevant, and cancellation behavior.

The target is not packet-level impersonation of a real human. The target is correct server-side player semantics for the subset of actions the agent is authorized and able to perform.

## C.5. Interaction surface: fake players must participate in the actual world

The primary world interface must be the player interaction surface, not a shadow city-builder world that merely reports intended actions.

### C.5.1. First-class physical capabilities

The first supported capabilities should be selected by their ability to demonstrate a meaningful agent life and a real shared-world interaction loop:

1. Spawn/materialize as a visible player body with a stable agent-owned identity.
2. Move through a settlement using ordinary movement and collision.
3. Look at, approach, avoid, and face blocks, entities, and players.
4. Pick up, carry, equip, use, and drop items through supported player paths.
5. Open/use workstations and containers through a headless server-side interaction adapter, with exact semantics validated.
6. Mine and place blocks through the player action path.
7. Attack or defend only when the action is explicitly permitted and only through ordinary player combat rules.
8. Sleep, eat, craft, trade, ride, or use other mechanics when an explicit supported action adapter exists.
9. Observe and report the actual result of each action.

This is a capability progression, not a requirement to implement every action at once. Each skill must have preconditions, motor steps, timeout, result predicates, refusal conditions, and an outcome observer.

### C.5.2. Container and GUI handling requires special treatment

A fake player does not have an ordinary human client rendering a screen. It must not be assumed that opening a menu on the fake player is equivalent to a client opening a GUI and sending normal container click packets.

Implement two distinct cases:

- **Agent-side headless interaction:** the agent interacts with a chest, workstation, merchant, or other supported container through a server-side adapter that checks actual reach, target state, permissions, slot rules, inventory constraints, and transaction semantics. It must not directly mutate arbitrary inventories to simulate a successful click.
- **Human-side user interface:** when a real player needs to inspect, approve, offer, or accept a transaction with an agent, the real player's client can receive a normal server-opened interface or another explicit visible interaction. The fake player itself does not need a GUI client.

Container transactions must be atomic or explicitly staged, auditable, and reconcilable after interruption. The implementation must test whether the particular menu/container API can safely support the fake player's connection model; unsupported menus must return a refusal or use a deliberately designed headless adapter.

## C.6. Communication is a core gameplay subsystem, not a chat cosmetic

Agent-to-agent communication and communication with human players are central requirements. They must be designed as part of the System-1 social architecture, not bolted on after movement and jobs.

### C.6.1. Distinct communication channels

At minimum, separate:

- **Private agent message:** addressed to one agent; may be a question, offer, request, warning, or personal message.
- **Local/proximity speech:** a message visible/audible to participants in a bounded local context, subject to the chosen presentation system.
- **Settlement or organization channel:** public or role-limited communications with a known audience.
- **Direct human-player communication:** a fake player addresses a real player through clearly attributed in-game chat or a supported interaction UI.
- **World/event report:** a structured report such as “the northern gate is blocked” or “the furnace stopped,” which can be consumed by agents with relevant access.
- **Nonverbal signal:** gaze, facing, gesture, item display/offer, movement, or other supported player-body expression. These are distinct from text and must have observable semantics.

Do not expose every internal thought as chat. Cognition is not public speech. The agent decides whether communication is useful, appropriate, authorized, and addressed to a particular recipient or group.

### C.6.2. Structured message envelope

Use a first-class communication record, conceptually similar to:

```text
AgentMessage {
  message_id
  sender_agent_id
  recipient_agent_ids | channel_id
  channel_kind
  speech_act
  payload / visible_text
  referenced_world_entities_or_events
  created_tick
  expiry_tick
  visibility / audience policy
  delivery_state
  response_state
  provenance / confidence
}
```

This is a schema sketch, not a committed serialization format. Messages should have stable IDs, delivery and expiry behavior, recipient scoping, an audit trail, and clear distinction between a received message and a verified world fact.

Useful initial speech acts include: greet, introduce, ask, answer, request, offer, accept, decline, warn, report, coordinate, negotiate, thank, apologize, teach, invite, refuse, and ask for help. These actions should change social context only when delivery or the relevant interaction actually occurs.

### C.6.3. Communication should lead to observable cooperation

The minimum meaningful multi-agent test is not that two fake players print text. It is that they:

1. perceive a shared situation or receive a message;
2. form their own beliefs and intentions;
3. communicate a proposal, request, or response;
4. agree on a bounded task or coordinate complementary skills;
5. move and act through their own player bodies;
6. observe the real world result;
7. update memories, trust, relationship state, and task records from the result.

For example, one agent discovers a shortage at a workstation, asks another agent to bring material, receives an acceptance, navigates to a storage location, picks up a real item, carries it to the workstation, and verifies delivery. The communication record, item movement, task state, and memory updates must agree. No agent may simply declare that an item was delivered because another agent said it was.

Each agent retains its own memory and perspective. There is no automatic shared omniscience. A group plan may coordinate behavior, but it does not merge identities, beliefs, or personal histories.

## C.7. Human-player interactions, consent, and identity boundaries

Real players are first-class participants in the same world. Fake players should be able to greet, answer, ask, offer, trade, follow a permitted instruction, cooperate, warn, assist, and react to player actions. That does not grant the fake players blanket authority over human players or their property.

Required boundaries:

- A fake player must never control, impersonate, or inject movement/actions into a real player's body.
- A player-issued instruction is interpreted through the project's authority and ticket model; a casual chat message is not automatically an authorized system command.
- A fake player must not claim to be a human account, imitate a real player's identity, forge signed chat, or create misleading login/session state.
- The display and chat presentation must make autonomous-agent identity clear.
- Access to a human player's inventory, storage, claims, builds, vehicles, or private spaces requires explicit permission under the relevant world/claim policy.
- Following, repeated messaging, trading, combat, building, or entering a protected area must have explicit policy and stop/refusal behavior.
- PvP, theft, griefing, traps, breeding, destructive building, and other high-impact actions remain separately governed, not implied by the agent's ability to perform the mechanic.
- Real-player chat, interactions, damage, trades, and commands are observations with provenance. Do not infer private intent from them.

The social layer should support refusal and disengagement as normal actions. A fake player should be able to decline a request because it conflicts with its contract, skill, safety, schedule, consent boundary, relationship, or current commitment. A human player's refusal must be respected by the system's social interaction rules.

### C.7.1. Chat is not the only form of social interaction

Player-to-player social behavior includes looking toward a speaker, approaching or maintaining distance, exchanging items, cooperating on a build, defending a shared location, showing a tool or item, waiting, inviting, declining, and responding to a world event. Chat text is one carrier among several.

The social perception system should convert observed player actions into bounded evidence. For example, a player placing materials in a public donation chest can be observed as a donation event; it does not by itself prove the player's wider political beliefs, personality, or permanent permission grant.

## C.8. Body lifecycle and persistence

The earlier two-body-state model remains valid but must be expressed as a player-agent lifecycle.

### C.8.1. Durable identity vs transient body object

The player-agent record survives:

- chunk unload and reload;
- dimension change;
- entity removal/recreation;
- server save and restart;
- temporary loss of a compatible body adapter;
- migration between supported fake-player implementation versions.

The fake-player Java object does not need to survive any of those events. After reload, the system resolves or creates the correct body representation for the same `AgentId` and profile, restores the supported player state, reattaches the current controller, and verifies ownership before movement or interaction resumes.

### C.8.2. Required body states

Use explicit states, for example:

- `VIRTUAL`: persistent agent state only; no physical player body in the active world.
- `MATERIALIZING`: validating level, spawn/return position, profile, chunk activation, and body creation.
- `ACTIVE`: player body is present and may receive bounded CUA commands.
- `SUSPENDED`: body exists or is being unloaded, but no autonomous motor action may be issued until lifecycle checks pass.
- `RECONCILING`: body/inventory/location/state is compared against persistent agent state after an interruption or reload.
- `FALLBACK`: a known safe state is active because a required subsystem is unavailable; no silent vanilla takeover.
- `RETIRED`: agent lifecycle has been explicitly closed under the campaign/personhood rules; this is not synonymous with unloading or chunk absence.

Do not use Minecraft entity removal as an implicit death, and do not use a virtual representation as permission to claim physical work occurred in an unloaded chunk.

### C.8.3. Player-specific lifecycle tests

The initial proof must investigate and test, rather than assume, the following behaviors for the exact NeoForge 1.21.1 patch:

1. `FakePlayer` construction and `FakePlayerFactory` caching/cleanup.
2. Admission into the server level's entity tracking and player collections.
3. Whether and how the body is visible to nearby real clients.
4. Whether it appears in the server's online-player list, tab list, scoreboard, selectors, commands, and player-count systems; which of those are desirable and which must intentionally differ.
5. Player tick, movement resolution, equipment, inventory, effects, damage, death, and respawn.
6. Dimension transfer and re-resolution of the correct profile/body.
7. Player interaction events and mod hooks for block use, item use, breaking, placement, entity interaction, attack, damage, pickup, and drop.
8. Advancements, statistics, criteria listeners, and any cleanup needed to avoid stale references or leaks.
9. Behavior of fake connections and attempts to open menus or send clientbound packets.
10. Save/restart behavior and identity reconciliation.

A 1.21.1 NeoForge issue documents a `FakePlayer`/advancement-listener leak scenario when fake profiles with unrelated UUIDs are created. This is a warning to test identity/advancement cleanup, not a claim that every fake-player use leaks. See [NeoForge issue #1487](https://github.com/neoforged/NeoForge/issues/1487).

## C.9. Two simulation layers still apply, but now to player agents

The system still needs the distinction between persistent cognition and loaded physical simulation:

- **Player-agent cognition** can continue through the civilization scheduler when no human player is nearby.
- **Player-body movement and world interaction** require a valid active body and the relevant Minecraft world/chunk processing.
- **Virtual/regional activity** may update explicitly modeled schedules, needs, communication, plans, relationships, and regional logistics. It must not claim an action that requires an actual physical player body in a non-ticking chunk.
- **Materialization** restores the same agent identity into an active `ServerPlayer`-family body and then permits CUA action after reconciliation.

A player agent at a distance may decide to travel, request a destination, coordinate with a group, or wait for materialization. It may not mine a block, open a chest, place a block, attack an entity, or participate in a physical transaction in an unloaded region unless that operation is explicitly supported by a separate regional simulation rule and is recorded as an abstract regional event—not misreported as a real player interaction.

Chunk tickets remain bounded physical-resource requests, not a blanket “keep every citizen loaded forever” setting. The ticket budget, priority policy, release conditions, and player experience must be explicit.

## C.10. City-builder systems must operate through player agents

The earlier city-builder and settlement architecture remains the planning/economic substrate, but its physical workers are fake players first. The city-builder layer may define needs, work orders, stock levels, reservations, production dependencies, routes, service coverage, and civic roles. It does not directly perform the physical work.

For a physical work order:

1. the city-builder layer issues a scoped job/ticket;
2. the agent evaluates it against needs, skills, commitments, relationships, and permissions;
3. the agent accepts, declines, or requests clarification;
4. the System-1 controller selects a player skill;
5. the CUA layer carries out real movement and player interaction;
6. the server resolves the actual world action;
7. the observer records the block/item/entity result;
8. the city-builder ledger reconciles stock, reservations, delivery, and production;
9. the agent records the experience and social consequences.

The city-builder may coordinate multiple people, but each embodied worker still has a distinct body, inventory, action history, and ability to refuse. A ticket is a bounded assignment, not a remote puppet string. Agents may communicate and negotiate task division while preserving the existing Governor/Axis/contract/ticket boundaries.

## C.11. Mob replacement is a separate, later adapter family

After fake-player embodiment, movement, communication, and player-world interactions are proven, the project may add supported mobs.

The mob layer must not inherit the fake-player motor assumptions. It needs its own type-specific adapters for:

- GoalSelector-driven mobs;
- Brain-driven mobs such as villagers;
- animals and breeding-capable entities;
- aquatic and flying entities;
- hostile mobs and combat families;
- modded entities with their own behavior architecture.

Each mob adapter must preserve its class-specific lifecycle, navigation, memory, interaction, animation, damage, and reproduction rules. A mob may have a different communication vocabulary and interaction range from a player agent. Shared concepts such as identity, perception, needs, memory, social relationships, ticketing, and Axis checks may be reused, but movement and action semantics must be type-specific.

The fake-player-first architecture does not mean mobs are reclassified as players or that every mob must communicate with a full player chat system. It means the system proves its main product on the entity type it actually intends to drive first.

## C.12. Revised implementation roadmap

This roadmap supersedes the phase order in Addendum B and the earlier “custom civilian -> GoalSelector -> Brain” vertical slice.

| Phase | Deliverable | Exit condition |
|---|---|---|
| 0 | Lock exact Minecraft/NeoForge build; create dedicated-server test harness and real-player comparison tests | Target artifacts, mappings, mod list, Java runtime, and test world are recorded |
| 1 | Stable player-agent registry and FakePlayer lifecycle spike | One durable identity resolves to one valid body per active level; no profile collision, stale body, or untracked lifecycle |
| 2 | Active fake-player embodiment | Body is visible/tracked as intended, ticks safely, persists/reconciles, and does not falsely impersonate a connected human |
| 3 | Player CUA motor and basic action vertical slice | Move/look/jump/sneak/sprint, inventory, item use, break/place, entity interaction and result observation pass player-equivalence tests |
| 4 | One System-1-controlled fake player | Perception, beliefs, needs, memory, skills and action selection operate without an LLM in the gameplay loop; CUA owns motor intent |
| 5 | Two or more fake players with communication and cooperation | Agents exchange scoped messages and complete a tangible shared task using actual bodies/items/world outcomes |
| 6 | Human-player communication and interaction | Real players can converse, offer, request, trade/cooperate where permitted; identity, privacy, consent and refusal tests pass |
| 7 | City-builder settlement vertical slice | At least one production/logistics chain is completed by fake players through real movement and interactions |
| 8 | Player-independent scheduler and virtual/regional continuity | Cognition and identity persist away from players; unloaded regions never receive phantom physical actions |
| 9 | Population scale, persistence, server performance and mod compatibility | Benchmarked population tiers, restart recovery, multiplayer correctness and mod interaction tests pass |
| 10 | First mob adapter family | A selected mob type is replaced without changing fake-player behavior or violating mob-specific mechanics |
| 11 | Brain-based and remaining mob families | Each family has its own verified adapter, rollback and performance profile |

### Phase 0 — Lock the actual runtime and build a parity harness

Freeze the exact 1.21.1 patch, NeoForge build, mappings, Java 21 runtime, loader/mod list, server configuration, and dedicated-server launch procedure. Create a controlled test world with at least one real human client and server-side logging. Capture the same action tests from a real player and a fake player so differences are visible rather than guessed.

### Phase 1 — Stable identity and lifecycle spike

Create one player-agent record with a stable `AgentId`, agent-owned UUID, profile, persistent identity metadata, and no assumed player-list membership. Use NeoForge's fake-player facilities only as a starting point. Prove construction, factory caching, world unload, server restart, dimension transition, entity tracking, and cleanup. Verify exactly one body per agent in a level and no accidental aliasing between two agents.

### Phase 2 — Visible, active player body

Prove that a fake player can be present in the actual world, tick, collide, receive ordinary damage/effects, hold equipment/inventory, and be observed by a nearby real client. Decide deliberately how it participates in player lists, tab lists, scoreboards, commands, selectors, player counts, and advancements. Do not silently inherit every connected-player convention merely because the class extends `ServerPlayer`.

### Phase 3 — Player CUA and action equivalence

Build the player motor adapter before System-1 complexity. Verify movement and look without teleporting; use items; select slots; interact with a block; mine with real progress; place a block; pick up and drop items; attack an entity under normal cooldown/reach/damage rules; and recover from interruption. Test at least one supported container/workstation action headlessly. Record unsupported UI and menu cases as explicit capability gaps.

### Phase 4 — System-1 controls one fake player

Connect the persistent agent to perception, local belief, needs/drives, affect, memory, social state, skill selection, and CUA. The controller's actions must be low-level and observable. No LLM is required to interpret ordinary game state or choose each gameplay tick. The scheduler selects eligible agents and budgets work; an action-result observer supplies the outcome.

### Phase 5 — Multi-agent communication and shared work

Run at least two fake players at once. Give them distinct identities, inventories, memory, capabilities, needs, and perspectives. Implement structured messages and visible speech acts. Test greetings, requests, offers, acceptance/refusal, work coordination, item delivery, warning, and a shared task. The agents must move, interact, and verify the result; a chat transcript alone does not count as success.

### Phase 6 — Human players become social participants

Introduce a real player client. Test incoming player chat and interaction observations, outgoing fake-player chat attribution, proximity behaviors, item offers, cooperation, trade UI for the real player, explicit refusals, and permissions. Ensure that an instruction in chat is not automatically privileged authorization and that the fake agents never take over the human player's movement or identity.

### Phase 7 — City-builder integration

Add the first real settlement work loop: work order, stock/reservation, worker acceptance, travel, pickup, carry, workstation interaction, processing or delivery, observed outcome, ledger reconciliation, and memory update. Add complementary roles only after one complete chain passes.

### Phase 8 — Player-independent life and virtual/regional body state

Decouple the cognitive scheduler from nearby human players and ordinary simulation distance. Agents retain identity, communication, relationships, schedules, commitments, and explicitly modeled regional work. A physical interaction requires an active player body and a ticking region; otherwise it remains a plan or a virtual event with its own provenance.

### Phase 9 — Scale and compatibility

Benchmark materialized, virtual, and mixed populations separately. Measure System-1 scheduling, path/motor work, entity ticking, memory, message routing, chunk activation, inventory reconciliation, and network replication. Test with a real player and a modpack, including event cancellation and claim/protection systems. Do not state a maximum supported population until measured on the target hardware and mod set.

### Phase 10+ — Mobs second

Only after the fake-player system passes its acceptance suite should the project add mob takeover. Start with one contained GoalSelector family, prove one controller owns decisions and rollback works, then add a Brain-based family and additional entity classes. Keep mob-specific adapters distinct from the player CUA controller.

## C.13. First vertical slice: two fake players, one human player, one shared task

The first meaningful system slice should include:

- one dedicated server on the locked target build;
- one connected real human player used as an interaction and parity-test participant;
- two fake players with distinct agent IDs, profiles, inventories, needs, memories, and communication state;
- one settlement hub with a storage location and a workstation;
- a short traversable route between storage and workstation;
- one real item-delivery or production dependency;
- one player movement skill and one item/container interaction skill;
- one System-1 scheduler and one event-driven message queue;
- at least one fake-player-to-fake-player request/acceptance exchange;
- at least one human-player-to-agent or agent-to-human interaction;
- one bounded Axis-authorized world action per physical operation;
- an observer that verifies movement, inventory, block/entity/world changes, message delivery, and failure reasons;
- persistent save, unload, restart, and reconciliation tests.

A representative success case:

1. Agent A discovers that the workstation lacks a required item through actual perception or a recorded, provenance-bearing report.
2. Agent A sends Agent B a scoped request for the missing item.
3. Agent B evaluates the request against its own state, schedule, skills, and commitments, then accepts or declines.
4. If accepted, Agent B navigates to the storage location, searches for the item, takes it through the supported player interaction path, and carries it to the workstation.
5. Agent A or the shared work process verifies that the item arrived and the workstation can proceed.
6. Both agents update their own memories and relationship/task state based on what they personally observed or received.
7. The human player can observe the agents' presence and communication and can interact without being impersonated or having their own actions commandeered.

This proves far more of the intended system than a single mob walking to a target. It demonstrates player embodiment, social coordination, CUA, inventory, real world interaction, and multi-agent outcome verification together.

## C.14. Acceptance criteria and explicit failure states

The fake-player-first implementation is not ready to expand until these conditions pass:

**Identity and lifecycle**
- A player's identity remains stable across body recreation and server restart.
- Two agents never share a GameProfile/UUID or body object accidentally.
- Factory cache/world cleanup is correct and does not retain dead levels.
- The agent is visibly and administratively distinguishable from human players.

**Player semantics**
- Real and fake player action paths are compared for every supported action.
- Event actor identity is the fake player's own identity.
- Reach, cooldown, inventory, item durability, block rules, damage and protection logic are respected.
- No unsupported client GUI or packet behavior is silently treated as successful.

**Cognition and CUA**
- One fake player can act without a model call for each tick/action decision.
- The fake player uses a player-specific motor; it is not moved by a mob navigator or teleported to simulate work.
- One controller owns all autonomous body intent; no split-brain controller is possible.
- Requested actions and observed outcomes remain separate records.

**Communication and society**
- Two agents can exchange messages, refuse, negotiate, coordinate, and complete a real shared task.
- A message is not automatically a verified fact or a grant of authority.
- Each agent preserves its own memory, knowledge limits, and perspective.
- Human players can interact with agents while retaining control of their own avatars and property.
- Communication has attribution, scope, rate limits, delivery state, and refusal behavior.

**Physical truth and performance**
- No physical action is claimed in an unloaded/non-ticking region.
- Materialization and dematerialization preserve the same identity.
- Chunk tickets are bounded and observable.
- Performance claims are supported by repeatable benchmarks.

Immediate refusal/fallback conditions include: missing or stale body; ambiguous identity; wrong dimension; untracked player body; fake connection/menu operation not proven safe; invalid reach/line of sight; missing item; blocked route; cancelled mod event; protected target; unauthorized human/player interaction; invalid ticket; chunk unavailable; dual controller; stale action proposal; failed world-result predicate; persistence mismatch; or performance budget exhausted.

A refusal is a first-class result with a reason. It is not silently converted into a teleport, direct inventory mutation, command shortcut, or mob behavior fallback.

## C.15. Architecture module changes

Keep the earlier system modules, but split the embodiment layer explicitly:

```text
player_agent_registry
player_profile_identity
fake_player_lifecycle
player_body_tracker
player_cua_motor
player_action_adapter
player_inventory_adapter
player_container_adapter
player_interaction_observer
social_message_bus
social_perception
relationship_memory
human_player_interface
agent_scheduler
regional_simulation
citybuilder_work_bridge
axis_gateway
chronicle

mob_adapter_registry              # secondary family
mob_goalselector_adapter          # secondary family
mob_brain_adapter                 # secondary family
mob_navigation_adapter            # secondary family
```

Shared contracts may define identity, perception records, action proposals, evidence, ticket scope, audit logs, and durable memory. Do not force `PlayerMotorInput` and `MobNavigationIntent` into one fake generic action type if their operational semantics differ. Use a common outer envelope with a type-specific payload and validator.

## C.16. Governance remains independent of the player agents

The previous Axis/Governor rules remain unchanged. A player agent's ability to swing a tool or interact with a block does not grant authority to do so. A successful fake-player action adapter proves *capability*, not *permission*.

Every world-changing act remains subject to the project's explicit authorization and scope model. The player agent's social rank, gender, feelings, personality, skill level, contract, campaign, friendship, or group membership must not silently authorize a protected side effect. A human player's chat instruction is not a privileged Governor action unless the existing authority chain explicitly validates it.

The agent may propose, negotiate, accept, or refuse work. The action gateway still validates the relevant permission and ticket. The world action is recorded with actor identity, target, scope, authorization, preconditions, action attempt, actual outcome, and evidence. This keeps the player agent's apparent autonomy from becoming an unbounded privilege escalation path.

## C.17. Research and implementation references

These sources support the technical distinctions in this addendum. They are not a substitute for checking the exact target jar, mappings, and runtime behavior.

1. **NeoForge 1.21.1 `FakePlayer` Javadoc** — `FakePlayer` extends `ServerPlayer`; class description says it is a basic fake server player used to simulate player actions; documents fake connection/network-handler internals.  
   https://aldak.netlify.app/javadoc/1.21.1-21.1.x/net/neoforged/neoforge/common/util/fakeplayer
2. **NeoForge 1.21.1 `FakePlayerFactory` Javadoc** — factory methods `get`, `getMinecraft`, and `unloadLevel`; documents the cache and the weak-reference/world-unload cleanup guidance.  
   https://aldak.netlify.app/javadoc/1.21.1-21.1.x/net/neoforged/neoforge/common/util/fakeplayerfactory
3. **NeoForge 1.21.1 Networking documentation** — server/client communication is message-based; documents registering payload handlers and separating server/client message flow. This supports the mod's structured communication transport, not a claim that a fake player has a client.  
   https://docs.neoforged.net/docs/1.21.1/networking/
4. **NeoForge 1.21.1 logical/physical side documentation** — supports testing on a dedicated server and keeping game logic on the logical server.  
   https://docs.neoforged.net/docs/1.21.1/concepts/sides/
5. **NeoForge issue #1487** — documents a reported FakePlayer/advancement-listener leak scenario on Minecraft 1.21.1; use as a lifecycle test prompt, not as proof that all use leaks.  
   https://github.com/neoforged/NeoForge/issues/1487
6. **Carpet fake-player implementation (reference, not a NeoForge API contract)** — demonstrates a more complete fake-player architecture with a `ServerPlayer`-derived fake player, an action pack, and fake-connection handling. Use it to study player action sequencing and embodiment choices; do not assume its Fabric internals or hooks can be ported directly.  
   https://github.com/gnembon/fabric-carpet/blob/master/src/main/java/carpet/patches/EntityPlayerMPFake.java
   https://deepwiki.com/gnembon/fabric-carpet/4-gameplay-features
7. **Spectrum 1.21.1 NeoForge fake-player helper** — a small real-world example of obtaining a fake player from `FakePlayerFactory` with an agent/owner profile. Useful as a narrow usage reference; it is not a full autonomous citizen implementation.  
   https://github.com/DaFuqs/Spectrum/blob/1.21.1-neoforge/src/main/java/de/dafuqs/spectrum/helpers/FakePlayerHelper.java

**Version and evidence cautions:** the Javadocs describe the target API surface, but method behavior, event delivery, player-list membership, entity tracking, GUI handling, dimension changes, and full action parity still require tests on the exact 1.21.1 NeoForge build. The Carpet source is a cross-loader implementation reference only. The architecture does not assert that a basic `FakePlayer` from the factory is automatically a fully integrated, persistent, autonomous player entity.

---

**Amendment effect:** Addendum C supersedes the initial vertical slice and entity-family rollout order in Addendum B. The primary implementation is player-agent-first. Mob AI replacement remains a secondary, type-specific extension after the fake-player communication and world-interaction system is proven.

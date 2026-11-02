# Technical Design Analysis: A Persistent City-Building Tower Defense Ecosystem (Version 9)

> **Version 9 research update (31 October 2026):** This revision adds a broad comparative design pass across 27 requested game references. It examines survival-city aesthetics and policy, free-roam and mission ecosystems, tower-defense evolution, systemic and procedural level design, creator platforms, ecological simulation, exploration, combat, production economies, and voxel construction. The prior V8 streamer-first package synthesis remains intact. New material is explicitly separated into observed reference characteristics, design inference, and recommendations for this proposed game.


**Document type:** Comparative design analysis and systems proposal  
**Status:** Exploratory / non-final. This is a design space, not a locked specification.  
**Scope:** Desktop Tower Defense and maze-defense descendants; Bloons Tower Defense and related modes/spinoffs; The Perfect Tower II; transferable systems from Deadlock, Cookie Clicker, Clash of Clans, Satisfactory, RimWorld, Astroneer, Infinitode 2, Nubbies Number Factory, Fortnite, The Binding of Isaac, Hades, No Man's Sky, Minecraft/Create, and Rain World; new comparative references include Frostpunk, Grand Theft Auto Online, the Bloons TD series, Nubby's Number Factory, Left 4 Dead, R.E.P.O., Robocraft, The Sandbox, Sim Companies, Cities: Skylines, Subnautica, VRChat, Batman: Arkham City, Noita, Wildfrost, Abiotic Factor, Cyberpunk 2077, Elden Ring, Elite Dangerous, Fez, Fallout: New Vegas, Skyrim, Hollow Knight, MachineCraft, Monster Hunter: World, No More Room in Hell, and 7 Days to Die; comparative engineering context for the Dawnbreak-to-Rainworld: Coalescence project lineage and the Coalescent Nights community; and repository-grounded engineering case studies of `ala-cities` and `dl-openshell`.

## Project context, naming, and evidence boundary

This is a **proposed game-design and systems-architecture analysis**, not an official specification for the Minecraft project Rainworld: Coalescence, not a technical description of every Dawnbreak release, and not a statement of Coalescent Nights' development commitments. The Minecraft history and community are included as a practical comparative case study: their surviving mod-reference inventory and project context expose useful engineering patterns in industrial automation, material flow, world simulation, ecology, audiovisual feedback, multiplayer operations, and long-lived releases.

The names must remain distinct:

| Name | Meaning in this document | What must not be inferred |
|---|---|---|
| **Coalescent Nights** | The small, multi-game gaming community whose principal focus is Minecraft. It also supports Don't Starve, Deadlock, Roblox, Team Fortress 2, and other games. | It is not the name of a modpack or of the Minecraft mod itself. |
| **Rainworld: Coalescence** | The named Minecraft project at the centre of the Coalescent Nights community. The exact capitalization and punctuation are retained from the owner-provided description. | Its name alone does not prove a technical or licensing relationship to the commercial game *Rain World*. |
| **Dawnbreak** | An earlier stage in the broader project history, according to the project owner. The owner described Dawnbreak as having advanced into the Coalescent Nights ecosystem. | The surviving public evidence does not yet establish whether this was a direct pack successor, a rework, a multi-release lineage, or a broader continuation. |
| **Rain World** | The independent videogame used here as a reference for ecology, creature needs, relationship tracking, and emergent behavior. | It is not interchangeable with the name Rainworld: Coalescence. |
| **The proposed game in this document** | A separate persistent city-building tower-defense ecosystem concept that synthesizes patterns from the listed games and technical projects. | It should not be described as a mode, feature, or promised successor of Rainworld: Coalescence unless the project owner separately establishes that relationship. |

### Evidence classes used in the synthesis

- **Owner-provided project context:** the project and community descriptions, Dawnbreak's place in the history, Google Drive distribution, and the statement that most historical material is on the community server. These are primary statements from the project owner, but the underlying private archives have not been independently inspected in this research pass.
- **Public project documentation:** the stated capabilities, dependencies, and compatibility of mod projects as published by their maintainers. These descriptions are version-sensitive and must be checked against the exact file used in any actual pack release.
- **Supplied reference inventory:** the uploaded `references.md`, containing 95 mod/project references and three additional non-mod research references. It is a research index, not a manifest or a server `mods/` directory.
- **Cross-reference inference:** conclusions about the apparent design interests of the Dawnbreak list, such as a preference for measurable industrial systems, hybrid logistics, agriculture, ecology, and performance engineering. These are interpretations of the inventory, not verified release features.
- **Repository source inspection:** the public source, documentation, branches, commits, issues, and recorded measurements in `alacripose/ala-cities` and `alacripose/dl-openshell`. These are independent engineering case studies; neither is presumed to be part of Rainworld: Coalescence or the Coalescent Nights community.
- **Repository-recorded test evidence:** results reported by those repositories. They are reproduced as project-reported evidence, not as tests independently run during this research pass. Live runtime observations, harness results, default-branch source, and non-default-branch prototypes are kept distinct.
- **Design proposal:** original recommendations in this document for how a separate game could translate those patterns into coherent systems.

The core correction is methodological: the Dawnbreak inventory should inform the design without being mistaken for a verified snapshot of a particular Rainworld: Coalescence release. The full research dossier remains later in this document, but the implications are now integrated into the relevant design sections below.


---

### Additional engineering case studies added in Version 7

Two public repositories provide implementation-level evidence that complements the earlier genre and Dawnbreak studies:

- [`ala-cities`](https://github.com/alacripose/ala-cities) demonstrates deterministic city simulation, exact material accounting, citizen/task separation, governed operations, headless world-readback, and a separate 3D chunk/asset-generator prototype on `design-package-cli`.
- [`dl-openshell`](https://github.com/alacripose/dl-openshell) demonstrates a data-driven Deadlock UI shell, contextual Popit tool layers, declarative window/package formats, bounded data-only rendering, explicit refusals, and installation/runtime evidence boundaries.

These are independent software projects, not new names for Dawnbreak, Rainworld: Coalescence, Coalescent Nights, or the previously discussed Secure Shell architecture. The distinction matters: `ala-cities` is primarily a world-simulation/economy reference; `dl-openshell` is primarily a client-interface/package-boundary reference. Neither is treated as a complete implementation of the proposed tower-defense ecosystem.

## 1. Executive design thesis

The proposed game is best understood as a **persistent defensive settlement attached to a network of connected, scalable battle maps**. The player develops a homebase and its surrounding city, then expands into adjacent tracks, districts, resource sites, expeditions, and hostile frontiers. The primary track remains a legible threat corridor: enemy waves travel toward the homebase and can damage or breach it. Other maps are connected spaces that can be scouted, built, mined, automated, contested, or defended.

The design should combine three distinct tower-defense traditions rather than flatten them into one:

1. **Maze construction and spatial denial** from *Desktop Tower Defense*: the player shapes enemy movement by placing structures and manipulating the traversable field.
2. **Readable fixed-route defense and deep tower identity** from *Bloons TD*: towers have roles, targeting rules, upgrade branches, status interactions, hero-like support, and wave-specific counters.
3. **Buildcraft, modular optimization, incremental progression, and automation** from *The Perfect Tower II*: the player can create highly specialized defensive configurations and build a town whose subsystems materially improve the defensive core.

The city-builder should not be a detached idle menu. It should produce tangible battlefield capabilities: ammunition, construction materials, power, repair capacity, scouting, specialized units, research, transport, consumables, and new map connections. Conversely, the battle layer should feed the city with resources, salvage, discoveries, prisoners or recruits (where appropriate), creature samples, blueprints, and strategic territory.

The long-term play surface should be **effectively unbounded in progression and expandable in geography**, while the actual simulation remains finite and controlled at any given moment. “Endless” should mean there is always another frontier, map segment, challenge, or deeper system to uncover—not that the engine must simulate an infinite number of active entities at full fidelity.

### Design principles

- **One world, multiple zoom scales:** strategic city view, district/map view, and tactical track view should represent the same underlying world state.
- **Readable threat first:** players must understand which enemies are approaching the homebase, which route they will take, and what failure costs.
- **Meaningful spatial choices:** placement, pathing, line of sight, terrain, elevation, transport, and logistics should matter.
- **Combinatorial builds, bounded complexity:** upgrades and loot should create synergies without making every item interact unpredictably with every other item.
- **Automation is earned and inspectable:** the player may automate mature, understood tasks, but should be able to see why an automation rule acted.
- **Ecology is not just enemy spawning:** creatures can hunt, flee, migrate, compete, nest, scavenge, and respond to changes in habitat.
- **No single mandatory play style:** active tactical play, construction, optimization, exploration, and incremental management should all contribute.
- **Expansion should not invalidate earlier investments:** old districts should remain useful through logistics, specialization, defense, trade, or resource production.
- **The design remains open:** every proposed system should have a minimum viable form and optional deeper layers.
- **Industrial systems must be observable:** a machine that is blocked, starved, unpowered, misconfigured, or damaged must expose the reason, not merely stop producing.
- **Transport is part of the strategy:** a resource has no defensive value until it reaches the place and time where it is needed.
- **Environment is a simulation input:** soil, water, weather, vegetation, and wildlife should influence production and traversal through understandable rules.
- **Project history is not a manifest:** inspiration inventories and historical pack references must never be used as substitutes for exact release artifacts.

---

## 2. The three tower-defense design trees

### 2.1 Desktop Tower Defense and maze-defense descendants

*Desktop Tower Defense* (2007) established a highly influential form of maze tower defense: enemies cross an open grid, and player-built towers also act as obstacles. The path is not merely a level-authored spline; it is partly a consequence of the player's construction. This makes the battlefield itself a puzzle.

#### Core design tree

**A. Open-field maze construction**
- The map begins with a traversable field, entry points, and an exit or protected objective.
- Towers occupy cells and alter pathfinding.
- The player constructs corridors, loops, choke points, and firing lanes.
- The central question is not only “which tower is strongest?” but “what route does my layout force the enemy to take?”

**B. Path validation and anti-blocking rules**
- The game must prevent placements that make the goal unreachable unless deliberate trapping is a supported mechanic.
- If multiple entrances/exits exist, path validation must cover every active route.
- Enemy pathfinding should be deterministic enough that players can reason about it.
- Path recalculation should be triggered by topology changes, not every simulation frame.

**C. Tower placement as geometry**
- Towers are blockers, weapons, utility structures, or hybrid structures.
- Their footprint matters. A large artillery tower may occupy more cells but cover a wider region.
- Rotatable structures, walls, gates, bridges, slow fields, and destructible barricades can extend the vocabulary.
- Terrain can add constraints: cliffs, water, rubble, vegetation, roads, tunnels, and buildable platforms.

**D. Enemy response**
- Basic enemies follow the shortest or lowest-cost route.
- Armored units may prefer wide roads or break weak barriers.
- Flying units can ignore ground maze geometry but remain affected by anti-air coverage and weather.
- Burrowers, climbers, teleporters, sappers, and path-disruptors can challenge overly static layouts.
- Enemies should not arbitrarily cheat path rules; special movement needs clear telegraphing and counterplay.

#### What this contributes to the proposed game

Maze defense is the strongest foundation for the **connected-map construction layer**. Rather than treating each map as a fixed track, the player could gradually alter districts, build bridges, redirect canals, open tunnels, raise walls, and create defensive corridors. However, unrestricted maze building can produce degenerate loops or force extreme enemy travel times. The design therefore needs explicit path-cost rules and anti-exploit limits.

Recommended rule: use a **path budget** or soft path-length constraint. A map may allow detours and loops, but enemy routes cannot be extended indefinitely by a single wall placement. Some enemy classes may attack structures when the detour exceeds a threshold. The player should see the predicted route and estimated travel-time change before confirming a build.

#### Useful descendants and adjacent patterns

- Fixed-lane games such as *Plants vs. Zombies* simplify spatial planning and make lane-specific counters legible.
- Multi-path TD games create route specialization: one lane can be optimized for slow, another for burst, another for area damage.
- Map editors and user-authored levels, as seen in *Infinitode 2*, can make spatial design itself a progression and community activity.

**Risk:** If maze construction dominates every other system, the player may solve the game with one perfect corridor and then repeat it. Counter this with multiple objectives, changing terrain, enemy adaptation, environmental hazards, and maps with distinct spatial constraints.

### 2.2 Bloons Tower Defense and its series/spinoffs

The *Bloons TD* family demonstrates the value of a fixed, readable route combined with a broad roster of towers, upgrade paths, target priorities, heroes, abilities, enemy properties, and escalating rounds. Its core strength is the clarity of the combat grammar: the track is understandable, towers have identifiable jobs, and enemies create specific problems.

#### Core design tree

**A. Fixed route and track identity**
- Each map has one or more authored paths.
- The player places towers in legal areas adjacent to the track.
- Track shape, bends, loops, elevation, water, obstacles, and line-of-sight blockers define tower value.
- The map is a stable tactical reference; challenge comes from wave composition and build choices.

**B. Tower roles and upgrade branches**
- Towers should have distinct primary functions: single-target damage, area damage, crowd control, support, economy, detection, anti-armor, anti-air, and boss damage.
- Upgrade paths should create different strategic identities, not simply three numerical versions of the same tower.
- Cross-path restrictions can preserve meaningful trade-offs.
- Tiered upgrades can change attack behavior, targeting, projectile type, range, or utility—not just damage.

**C. Targeting and combat rules**
- Targeting priorities such as first, last, strongest, weakest, closest, or special-class targeting create player agency without requiring constant manual aiming.
- Damage types and enemy resistances create a reason to diversify.
- Status effects—slow, stun, burn, poison, corrosion, knockback, mark, vulnerability, and armor shred—should have explicit stacking rules.
- Projectiles need clear rules for travel time, piercing, splash, homing, and collision.

**D. Heroes and active abilities**
- A hero-like commander can provide a persistent battlefield identity.
- Active abilities create timing decisions during dense waves or boss phases.
- Abilities should have readable cooldowns, range, area, and resource costs.
- The commander can also exist as a persistent character in the city/exploration layer, but battlefield power must remain balanced separately from city progression.

**E. Enemy families and wave grammar**
- Basic enemies teach the base damage loop.
- Fast enemies test coverage and targeting.
- Armored enemies test damage composition.
- Shielded or regenerating enemies test sustained damage.
- Camouflaged/hidden enemies test detection.
- Splitters create post-death swarms.
- Support enemies buff, heal, shield, or accelerate other enemies.
- Bosses create multi-phase objectives that demand preparation rather than only high DPS.

**F. Round economy**
- Players earn currency for defeating enemies, completing rounds, or meeting objectives.
- Spending between waves creates a deliberate preparation window.
- Optional early-start or risk-reward mechanics can trade preparation time for bonus income.
- Difficulty modes can alter starting resources, enemy scaling, lives, and available upgrades.

#### What this contributes to the proposed game

Use Bloons-style combat readability as the **tactical language of the main track**. Every tower should communicate its role through silhouette, effects, targeting behavior, and concise stats. Every enemy should communicate its threat through visible traits and a short combat descriptor.

A particularly useful approach is to combine fixed tracks and player-shaped tracks:
- Main homebase approach: authored, readable, and strategically important.
- District defense maps: partially editable maze spaces.
- Expedition maps: more exploratory and terrain-driven.
- Special challenge maps: fixed geometry with unusual rules.

This avoids forcing every map into the same pathing model.

#### Risks and adaptations

- Too many tower upgrades can overwhelm the player. Use a role taxonomy and clear upgrade previews.
- Pure DPS scaling creates a solved meta. Add enemy behaviors, objectives, terrain, and logistics constraints.
- A hero can become a mandatory carry. Ensure towers, infrastructure, and tactical positioning remain independently valuable.
- Avoid copying Bloons' exact tower identities, upgrade names, visual language, or proprietary presentation. Transfer the structural design lessons, not protected expression.

### 2.3 The Perfect Tower II

*The Perfect Tower II* takes a different approach: instead of placing a field of towers, the player configures and improves one central tower using modules, while a town provides multiple supporting minigames and progression systems. Its official materials describe a module-based tower, a town with twelve buildings, many minigames, automation scripts, offline progression, and several modes. This makes it especially relevant to the proposed game's **persistent meta-system**.

#### Core design tree

**A. Modular defensive core**
- A central tower or homebase can accept interchangeable modules.
- Modules can be offensive, defensive, utility, active, passive, or special.
- A blueprint is a saved configuration that can be swapped between challenges.
- Elemental or damage-type specialization encourages matchup planning.
- Modules can create conditional effects and interactions rather than only raw stat increases.

**B. Town as a collection of meaningful subsystems**
- Buildings have individual gameplay loops.
- A power facility can manage generation, storage, and distribution.
- A mine can discover and extract materials.
- A factory can transform raw resources into components.
- A laboratory can unlock research and experiments.
- Other buildings can provide training, trade, scouting, defense, or specialized minigames.

**C. Incremental progression**
- Long-term upgrades can persist across runs.
- Prestige or tier resets can unlock new layers without deleting the player's sense of progress.
- Endless modes support long-horizon scaling.
- Offline production allows mature systems to continue at a bounded rate.

**D. Scripting and automation**
- Repeated tasks can be expressed as rules or scripts.
- Automation turns solved micro-management into a strategic configuration problem.
- The player should be able to inspect, pause, and override automation.
- Scripts should operate within a clear permission model and resource budget.

#### What this contributes to the proposed game

The strongest adaptation is to make the homebase itself a **modular defensive organism**. It has fixed core functions—health, shields, command, storage, power, and emergency systems—but its modules can be specialized for a particular campaign, region, or threat.

Examples:
- A shield module that converts surplus grid power into temporary barrier capacity.
- A targeting module that marks elite enemies for all nearby towers.
- A logistics module that increases ammunition delivery but consumes fuel.
- A salvage module that increases recoverable components from defeated enemies.
- An emergency repair module that activates only when the base falls below a health threshold.
- A command module that improves allied minion coordination or defensive unit response.

The town should not be twelve disconnected minigames by default. Each building should have an explicit input-output relationship with the wider simulation. Minigames are optional interaction surfaces for optimizing the building, not isolated chores.

**Important lesson:** modularity is powerful when the module taxonomy and interaction rules are discoverable. Avoid hundreds of upgrades whose effects cannot be compared. Provide tags, previewed interactions, blueprint comparisons, and a combat log explaining what a module contributed.

---

## 3. Unified world structure: an endless connected map network

### 3.1 The map is a graph, not one giant level

Represent the world as a graph of map nodes and connections.

- **Node:** a district, track, city sector, mine, forest, cave, factory site, outpost, biome, or special encounter.
- **Connection:** road, rail, tunnel, bridge, river route, portal, elevator, gate, or discovered passage.
- **Map instance:** the actual navigable local space shown when zoomed in.
- **Strategic region:** a larger grouping of map instances with shared climate, factions, resources, and threat pressure.

The player navigates from a strategic overview into a district, then into a tactical track or building interior. These are not separate saves; they are different representations of the same persistent world state.

### 3.2 Endless and “impossible” default mode

The default campaign should be an open-ended mode with no mandatory final wave. “Impossible” can be a difficulty profile or challenge modifier, but should not be confused with the infinite map itself.

Suggested mode structure:
- **Open Frontier (default):** ongoing expansion, persistent city, connected maps, escalating threat.
- **Endless Siege:** the main track continues escalating until the homebase falls or the player withdraws.
- **Expedition:** enter a remote map for exploration, resource recovery, or boss encounters.
- **Challenge Contract:** fixed map, fixed constraints, curated objective.
- **Sandbox/Creative:** test towers, map layouts, production chains, and enemy behavior.
- **Scenario/Story:** authored arcs that introduce factions, regions, and mechanics.

In the open-ended mode, progression can generate new connected map nodes using seeded procedural rules. A new node should be generated from a constrained template and validated for path connectivity, buildable area, resource placement, spawn rules, and performance limits. Procedural generation should create variation within a designed grammar—not arbitrary noise.

### 3.3 Zoom and map continuity

A useful camera hierarchy:

| Scale | View | Primary decisions |
|---|---|---|
| Strategic | World/region map | Choose expansion direction, assign resources, inspect threat fronts |
| City | Homebase and districts | Place buildings, plan roads, manage residents and production |
| Tactical | Individual track/map | Place towers, manage waves, target enemies, use abilities |
| Close interaction | Building/interior/encounter | Operate machinery, craft, loot, solve a minigame |

The camera should support smooth zoom and map transitions, but the simulation should not require all map geometry to be loaded simultaneously. Use stable map IDs and world-space coordinates so a player can zoom out, select a neighboring map, and zoom in without losing orientation.

### 3.4 The main track and homebase threat

The main track is the persistent strategic threat corridor. Enemy minions travel along it toward the homebase. This creates a continuous defensive reason to maintain the city.

The main track should support:
- Multiple spawn regions and wave compositions.
- Enemy squads with roles and formations.
- Lane pressure and occasional route changes.
- Forward defenses and secondary outposts.
- Boss or siege units that attack infrastructure.
- Minions that can target towers, logistics nodes, power lines, gates, or the homebase depending on their role.
- Breach consequences: damage, temporary loss of district services, resource theft, fires, contamination, or forced repairs.

Do not make every enemy attack every structure. Give enemy archetypes explicit target priorities. A raider might target storage; a sapper might disable power; a siege unit might attack walls; a standard minion might continue toward the homebase. This makes defensive planning legible.

### 3.5 Persistent damage and recovery

A city-builder defense game needs failure that matters without making one lost wave permanently ruin a long save.

Possible consequence ladder:
1. **Local damage:** damaged towers, walls, roads, and machines.
2. **Service interruption:** power, water, transport, or production temporarily reduced.
3. **District breach:** enemies enter a district and create a short cleanup/containment objective.
4. **Homebase damage:** persistent health loss or disabled modules.
5. **Critical breach:** emergency retreat, evacuation, or a recovery scenario.

Recovery should require meaningful resources and time, but not demand tedious manual repair of every tile. Provide repair crews, automated maintenance, salvage recovery, and emergency plans that can be prepared before a siege.

---

## 4. City-builder systems and transferable game inspirations

### 4.1 Deadlock: lane pressure, minion waves, objectives, and economy

*Deadlock* offers a useful model for making lanes more than enemy conveyor belts. Its lanes connect income, wave control, map pressure, and objective progression. Troopers create a moving frontline; destroying structures changes access and strategic options.

Adaptations:
- Friendly militia or drones spawn from barracks and move along roads or tracks.
- Enemy and friendly waves can clash before reaching towers.
- Clearing a wave can create a window to capture a resource node, repair a forward base, or attack an enemy structure.
- Lane objectives can unlock new district services, map connections, or build permissions.
- A courier or supply convoy can carry a high-value resource across the map, creating a timed escort/defense event.
- Optional neutral camps can provide materials, temporary buffs, recruitable units, or rare crafting components.

Do not turn the game into a MOBA clone. Borrow the strategic relationship between wave position, objective access, and resource collection. The player should be able to influence the frontline through tower placement, production, route control, and tactical abilities.

### 4.2 Cookie Clicker: production curves, synergies, and prestige

*Cookie Clicker* is useful as a reference for readable incremental economies: buildings produce resources, upgrades alter production, and synergies make old investments scale in new ways.

Adaptations:
- Every production building has a base output, upkeep, and upgrade curve.
- Building classes can synergize: mines feed smelters; smelters feed factories; factories feed towers; farms feed population and biofuel.
- Milestone upgrades can alter a building's function rather than only multiply output.
- Prestige can unlock new production rules, automation capacity, or map-generation options.
- Offline production should be capped and should account for storage capacity, power, and input availability.

Avoid exponential number inflation as the only progression. Use multiple resource dimensions and meaningful bottlenecks: throughput, power, labor, storage, transport, pollution, and local availability.

### 4.3 Clash of Clans: readable base layout, builders, and defensive investment

*Clash of Clans* demonstrates the strategic appeal of a compact base where defensive buildings, resource storage, walls, and upgrade timing are visible. Builder capacity creates a meaningful construction queue, while separate bases can support different progression loops.

Adaptations:
- Construction crews are a limited strategic resource.
- Buildings have upgrade durations or staged construction, but the game should offer active alternatives such as supplying materials, assigning specialists, or completing a short construction task.
- Defensive buildings can have secondary utility functions.
- Separate outposts can specialize in mining, research, logistics, or forward defense.
- A builder unit or repair crew can become part of the defense if the player invests in protecting it.

Avoid making wait timers the primary source of friction. The construction queue should create planning choices, not monetized waiting.

### 4.4 Satisfactory and Minecraft Create: visible automation and logistics

*Satisfactory* provides a strong model for factory throughput, conveyors, pipes, power generation, vehicles, and long-distance logistics. The Create mod for Minecraft is especially relevant because it emphasizes visible, spatially arranged mechanical contraptions rather than hiding all production behind menus.

Adaptations:
- Conveyors move ammunition, ore, components, food, and salvage.
- Pipes move liquids, coolant, fuel, or chemical reagents.
- Belts and lifts connect production buildings to depots and defensive platforms.
- Trains and trucks connect distant map nodes.
- Mechanical power can drive certain machines; electrical power can support advanced systems.
- Storage buffers and throughput limits make logistics design matter.
- Production lines can be physically inspected and debugged.
- Machines can have modular recipes and alternate production paths.

A key design decision is how much physical logistics to simulate. A good compromise is **visible logistics with abstracted throughput**: belts and trains are spatially present and can be disrupted, but the engine does not need to simulate every item as a full rigid-body object. Item packets can move along a path with deterministic throughput.

Create-inspired contraptions could include:
- Rotating gear-driven gates.
- Mechanical lifts that move supplies between map elevations.
- Train-mounted artillery or mobile depots.
- Sorting systems that route salvage by material tag.
- Water wheels, windmills, turbines, and flywheels.
- Automated assembly arms that load ammunition into turrets.

#### 4.4.1 Dawnbreak as an industrial-systems case study

The supplied Dawnbreak reference inventory reinforces the value of treating automation as a layered production and logistics simulation rather than a single “factory” feature. Its most distinctive cluster combines the base Create mechanical-automation framework with projects for fuel production, electrical conversion, process control, fluid instrumentation, metalworking, sifting, package logistics, storage, and rail infrastructure. The list also includes Refined Storage and bridge projects that connect otherwise separate logistics models.

The list does **not** establish that all of these projects were present together in one playable release. It contains 95 mod/project references and three non-mod references; the actual release boundaries, loader versions, and exact JAR set require the Google Drive archives or manifests. Accordingly, the following is a design reading of the reference cluster, not a claim about the contents of every Dawnbreak or Rainworld: Coalescence build.

| Technical pattern in the reference set | Example projects from the supplied inventory | Transferable design lesson |
|---|---|---|
| Physical mechanical production | [Create](https://github.com/Creators-of-Create/Create), [Create Metalwork](https://github.com/AverageAnime/create-metalwork), [Create Sifting](https://github.com/oierbravo/createsifter) | Machine placement, motion, recipe order, and material handling can be spatial decisions rather than menu-only crafting. |
| Fuel and energy conversion | [Create: Diesel Generators](https://modrinth.com/mod/create-diesel-generators), [Create New Age](https://gitlab.com/antarcticgardens/create-new-age/) | Distinct energy carriers create interesting production choices. Diesel, rotational power, electricity, heat, and fuel logistics should not collapse into one generic energy number. |
| Process measurement and control | [Create: Factory Controller](https://modrinth.com/mod/create-factory-controller), [Create Extra Gauges](https://github.com/LIUKRAST/CreateExtraGauges), [Create Pressure Gauges](https://modrinth.com/mod/create-pressure-gauges) | Players need instrumentation: counters, thresholds, alarms, pressure/flow readings, network status, and control surfaces. A dashboard is an interface to the simulation, not the simulation's authoritative state. |
| Physical and networked inventory | [Refined Storage](https://refinedmods.com/refined-storage/), [Create Storage](https://modrinth.com/mod/create-storage-neo-forge), [StockBridge](https://modrinth.com/mod/stockbridge%28refinedstrotage%2Bcreate%29), [Create Repackaged](https://github.com/LIUKRAST/Repackaged) | Local buffers, network storage, and addressed packages are different logistics abstractions. They should interoperate through explicit adapters and costs. |
| Distributed transport | [Create Mobile Packages](https://github.com/timplay33/Create-Mobile-Packages), [Create Threaded Trains](https://github.com/MisterJulsen/Create-Threaded-Trains) | A factory network can be geographically distributed. Transport capacity, route planning, dispatch, contention, and failure are systems in their own right. |
| Heavy industrial progression | [The Factory Must Grow](https://github.com/DrMango14/Create-The_Factory_Must_Grow), [GregTech CEu Modern](https://github.com/GregTechCEu/GregTech-Modern), [Tinkers' Construct](https://github.com/SlimeKnights/TinkersConstruct) | Material depth is compelling when it adds distinct processing constraints, not when it merely increases recipe length. These branches need clear identities and curated overlap. |

The most useful design lesson is **observability before automation**. A player should first be able to see that a pump is under-delivering, a smelter is input-starved, a belt is backed up, an energy source is overloaded, or a storage network has disconnected. Only after those states are legible should the player automate responses. This yields a progression from manual operation, to instrumentation, to alarms, to rule-based control, to high-level logistics scheduling.

A related lesson is to preserve the difference between **physical throughput** and **network availability**. A global storage network can make items available without showing the route they travel. A belt or train exposes a physical route, but introduces capacity, delay, congestion, and vulnerability. A mobile package can move an addressed batch between endpoints. These are not interchangeable features; each solves a different logistical problem and should carry a different cost model.

The proposed game should therefore support at least three logistics modes:

1. **Local physical flow:** short-range belts, pipes, lifts, chutes, loaders, and machine buffers. Best for visible factories and vulnerable battlefield supply.
2. **Scheduled transport:** rail, road, convoy, aircraft, or mobile carrier routes between districts. Best for moving bulk goods across the world graph.
3. **Networked inventory services:** remote requests and shared stock visibility, subject to bandwidth, network range, permissions, power, and storage limits. Best for convenience and high-level production coordination.

A single item may move through more than one mode. For example, an ore batch can leave a mine on a conveyor, be loaded onto a train, arrive at a smelter depot, and then enter a networked stock ledger. The game should make each transfer explicit enough to debug, while allowing players to abstract mature routes.

### 4.5 RimWorld: colony needs, factions, and storyteller pacing

*RimWorld* is useful for the social and narrative layer. Its storyteller systems modulate event pressure, while colonists have needs, relationships, skills, and individual stories.

Adaptations:
- Residents have roles, skills, fatigue, morale, and workplace preferences.
- Specialists improve specific building categories or repair/production tasks.
- Factions can trade, threaten, ally, raid, migrate, or request help.
- A storyteller director chooses events based on the player's current stability and recent pressure.
- Events should create narrative variety: storms, disease, sabotage, refugee arrivals, trade caravans, power failures, animal migrations, raids, and discoveries.
- Residents can generate emergent stories through work assignments, injuries, rivalries, rescues, and successful defenses.

The storyteller should not secretly cheat by spawning arbitrary counters to the player's build. It should select from a documented event budget and respect cooldowns, threat ceilings, and player recovery windows. Give the player difficulty controls for event frequency and severity.

### 4.6 Astroneer: exploration, terrain, resources, and transport

*Astroneer* contributes a tactile exploration and resource-discovery loop:
- Terrain can be excavated or reshaped.
- Resource deposits are discovered in the environment.
- Vehicles and rails extend reach.
- Remote sites require power and transport planning.
- Research objects and discoveries create reasons to explore beyond the homebase.

Adaptations:
- A map can contain buried deposits, caves, unusual flora, ruins, and environmental hazards.
- Players can build extraction outposts and connect them with rail or convoy routes.
- Terrain deformation can open passages, expose ore, or create defensive berms.
- Portable tools can let the player survey, scan, repair, or temporarily power a remote site.
- Expedition rewards should include new recipes and machine components, not only currency.

### 4.7 Infinitode 2: research breadth, mining, and user maps

*Infinitode 2* is a particularly direct reference for a research-heavy TD progression tree. Its published feature list describes many tower types, tower leveling and abilities, modifiers, mining, a large research tree, and a map editor.

Adaptations:
- Research nodes unlock new tower roles, map tools, production recipes, enemy analysis, and logistics systems.
- Towers gain battlefield experience and can specialize through a small set of meaningful choices.
- Mining can occur during defense, creating a trade-off between economy and coverage.
- Modifiers can boost nearby towers or production structures.
- Player-authored maps can become shareable challenge spaces with validation rules.

The research tree should be divided into understandable domains: combat, construction, logistics, ecology, exploration, automation, and civic development. Avoid one enormous flat tree.

### 4.8 Nubbies Number Factory: numerical production as a specialized subsystem

*Nubbies Number Factory* is a less widely documented reference than the other titles in this analysis, so this section is intentionally provisional. The transferable idea is a factory/incremental loop where numerical transformations, generators, multipliers, and production chains become the central puzzle.

Potential adaptation:
- A dedicated computation or synthesis facility converts base quantities into advanced data, energy, research points, or exotic components.
- Machines can combine, split, normalize, compress, multiply, or transform resource streams.
- The player can optimize a production graph for throughput, efficiency, or a target output.
- Higher-level recipes can require specific ratios rather than simply larger quantities.

This should remain a specialized optional system, not a mandatory mathematical minigame. The design can use visible node graphs and simple throughput rules while allowing advanced players to optimize ratios and conversion chains.

### 4.9 Fortnite: Save the World and trap-defense construction

The most relevant Fortnite reference is *Save the World*, particularly its base defense and trap-placement concepts, rather than Battle Royale building alone.

Adaptations:
- Trap corridors can be built into walls, floors, ceilings, and chokepoints.
- Traps can trigger based on proximity, weight, enemy class, or environmental conditions.
- Players can prepare a defense before a storm or siege event.
- Construction materials have durability and repair costs.
- Enemy waves can attack structures and create multiple breach points.
- Defenders can use gadgets or deployable support devices.

This pairs naturally with Desktop Tower Defense maze logic: the player shapes the route and equips it with layered traps, while the Bloons-style tower roster provides active coverage.

---

## 5. Roguelite, loot, crafting, and upgrade systems

The roguelite references should expand the *variety and recombination* of builds. They should not simply add random stat drops to a tower-defense game.

### 5.1 The Binding of Isaac: item pools and emergent synergies

Useful patterns:
- Items are drawn from contextual pools rather than one universal loot table.
- Rooms, bosses, shops, secrets, and risk-reward areas can have different reward identities.
- A run can transform dramatically through a small number of synergistic items.
- Some items modify the behavior of other items, creating surprising but legible combinations.

Adaptation:
- Loot pools should be associated with biome, faction, boss, facility, expedition type, and risk level.
- A swamp boss might drop toxin, spore, or regeneration modules; a machine boss might drop gears, targeting logic, or power components.
- Relics can alter tower behavior: projectiles split on kill, slow effects spread, burning enemies leave hazard tiles, or nearby towers share a percentage of overkill damage.
- The player should be able to inspect the source pool and understand why a reward was possible.

Use a synergy-tag system rather than hand-authoring every possible pair. Example tags: `projectile`, `burn`, `chain`, `summon`, `shield`, `kinetic`, `poison`, `construct`, `economy`, `minion`, `terrain`. Synergy rules can operate on tags with explicit priority and stacking limits.

### 5.2 Hades: boon choices, build identity, and duo synergies

Useful patterns:
- Rewards are offered as meaningful choices rather than only random drops.
- Boons create an evolving run identity.
- Duo or cross-domain synergies reward deliberate combinations.
- Keepsakes and weapon aspects support planning before the run begins.

Adaptation:
- Each expedition or siege can offer a small set of boons after milestones.
- Boons may modify the commander, towers, minions, city services, or the homebase.
- A pre-run doctrine or relic can bias reward pools.
- Cross-domain synergies can reward combining, for example, electricity with water, machinery with fire, or ecology with poison.
- The player should see the opportunity cost of each choice.

A boon should ideally alter a rule, not just add 8% damage. Example: “Overcharged Supply Lines” makes powered towers fire faster when a nearby generator has surplus capacity, but increases grid instability during storms.

### 5.3 No Man's Sky: procedural discovery and resource identity

Useful patterns:
- Procedural worlds create a large discovery space.
- Planetary conditions and biomes affect resources and hazards.
- Scanning and cataloguing create knowledge as a reward.
- Refining converts raw materials into more advanced components.
- Technology upgrades can be discovered, installed, and compared.

Adaptation:
- Each generated region has a resource profile, climate, terrain grammar, ecology, and hazard profile.
- A survey can reveal deposits, points of interest, creature behavior, ruins, and environmental modifiers.
- Rare resources should have distinct uses, not simply be a higher-tier version of common ore.
- Refiners can convert raw materials into alloys, catalysts, bio-components, and module ingredients.
- Discovered recipes and map data can persist even if the player abandons an outpost.

### 5.4 Minecraft and Create: crafting as a physical production problem

Adaptations:
- Crafting recipes can be represented as production graphs.
- Advanced components require multiple stages and machine types.
- Create-like mechanical systems can physically transport, rotate, sort, press, mill, mix, and assemble components.
- Player-built contraptions can improve throughput or reduce labor.
- Blueprints allow the player to reproduce a proven machine layout in another district.

The game should offer two levels of interaction:
1. **Quick craft / abstract recipe:** a low-friction interface for simple items.
2. **Physical production line:** optional spatial construction for advanced, high-volume, or specialized goods.

This prevents the factory layer from becoming a mandatory burden for players who primarily enjoy tower defense.

---

## 6. Rain World-inspired ecology: creatures as agents, not wave decorations

Rain World's ecosystem is especially interesting because creatures can be built from modular behavioral components and relationship trackers. A creature's behavior can emerge from prey, threats, allies, items, and home locations rather than from one fixed attack script.

### 6.1 Proposed creature model

Each creature type can be composed from:
- **Needs:** food, shelter, safety, territory, reproduction, warmth, hydration.
- **Perception:** nearby prey, predators, threats, resources, dens, terrain, sounds, light, scent.
- **Behavior modules:** hunt, flee, forage, scavenge, guard, migrate, investigate, cooperate, carry, nest, rest.
- **Relationships:** prey, predator, competitor, neutral, symbiotic, domesticated, faction-aligned.
- **Memory:** recent threat, known food source, home route, player interaction, learned avoidance.
- **Capabilities:** climb, swim, fly, burrow, glide, camouflage, poison, carry, swarm, construct.

A modular behavior architecture allows the same broad creature family to behave differently depending on traits and environment.

### 6.2 Ecological interactions

- Predators hunt smaller creatures.
- Herbivores consume local vegetation and may migrate when food is depleted.
- Scavengers gather remains and can compete with the player for salvage.
- Parasites or pests can affect crops, storage, or machines.
- Pollinators or symbiotic species can improve local production.
- Territorial creatures can defend nests or resource sites.
- Some species can be domesticated, recruited, repelled, studied, or harvested.

### 6.3 Ecology and tower defense

Ecology should intersect with defense without making every creature hostile.

Examples:
- A migrating herd can trample an exposed road or reveal a hidden resource deposit.
- Predators may attack enemy minions, creating an unpredictable third party.
- A burrowing species can undermine walls but also expose underground minerals.
- A territorial creature may defend a nest that the player can route around or negotiate with.
- Pollution or excessive extraction can change local populations.
- Certain enemies can exploit ecological corridors, nests, or darkness.

### 6.4 Simulation budget and fidelity tiers

A fully simulated ecosystem across an endless world would be prohibitively expensive. Use simulation levels:
- **Active local:** full behavior and pathing near the player or active battle.
- **Regional:** aggregate population, food, migration, and threat calculations.
- **Distant:** statistical updates at intervals, with no individual creature simulation.
- **Dormant:** deterministic seed plus stored changes; instantiate when visited.

The world should remain coherent when revisited. If a region's prey population was depleted while distant, that fact should persist even if individual creatures were not simulated.

### 6.5 Rain World as an ecological reference; Rainworld: Coalescence as a separate project name

The two names serve different purposes in this analysis. *Rain World* is the videogame reference for creatures that pursue their own survival needs and interact through prey, threat, friend, item, den, and discomfort relationships. **Rainworld: Coalescence** is the owner-provided name of the separate Minecraft project associated with Coalescent Nights. The shared words do not establish a shared codebase, license, or official relationship. The ecological model below is attributed to the *Rain World* design reference, not presumed to be an implemented feature of Rainworld: Coalescence.

A 2025 Unity developer article on *Rain World: The Watcher* describes modular behavior trackers such as prey, threat, friend, item, den, and discomfort tracking, along with the way combinations can produce interactions that were not scripted as bespoke events. It also describes the practical balance between emergent systems and occasional designer-authored regional attraction rules ([Unity, “Exploring Procedural Design in Rain World: The Watcher”](https://unity.com/blog/exploring-procedural-design-rain-world)). That is a useful technical reference because it describes the underlying architecture rather than simply calling the game world “alive.”

The Dawnbreak reference inventory adds a separate ecological and environmental perspective. Projects such as [Dynamic Trees](https://modrinth.com/mod/dynamictrees), Dynamic Trees Plus, [Serene Seasons](https://www.curseforge.com/minecraft/mc-mods/serene-seasons), [Immersive Weathering](https://www.curseforge.com/minecraft/mc-mods/immersive-weathering), [Ecologics](https://www.curseforge.com/minecraft/mc-mods/ecologics), Boids Reforged, Boids Birds, Ratatouille, and Curious Mobs point toward a world where vegetation, seasons, ambient conditions, and creature motion can be visible systems. The exact mod version and whether each item belongs to a given release are not established by the reference list.

Agriculture deserves its own state model rather than being reduced to a crop timer. The separate public repository [Agricultural Enhancements](https://github.com/alacripose/Agricultural_Enhancements/tree/1.21.1-neoforge) documents a 1.21.1 NeoForge branch with irrigation control, sprayers, fertilizer production, crop management, harvesting machinery, and soil moisture/nutrient-aware growth. It is **not** part of the supplied 95-project Dawnbreak list, and it must not be presented as an installed Dawnbreak dependency. It is relevant as a separate development reference for how crop growth could be coupled to resources and environment.

For the proposed game, a field should be modeled through a minimum set of state variables:

- plant species and cultivar; growth stage; health; harvestable biomass;
- soil type and depth; moisture; nutrient pools; compaction or damage;
- local temperature, light, season, and precipitation;
- irrigation/fertilizer inputs and the provenance of those inputs;
- pests, herbivory, pollination, disease, and competing vegetation where the selected fidelity tier supports them.

Do not simulate every water molecule or nutrient ion. Use a bounded cell/plot model with mass-balance constraints and discrete growth updates. Water entering through rain or irrigation should increase available moisture; evapotranspiration, drainage, uptake, and runoff should reduce or redistribute it. Fertilizer changes nutrient pools according to a defined composition. A crop's growth rate is bounded by its limiting factors rather than being multiplied without limit by every positive modifier.

This creates meaningful connections to the defense loop. A drought can reduce crop output and therefore limit food, bio-reagents, or livestock support. Heavy rain can saturate soil, slow off-road vehicles, flood low tracks, or increase hydro-power potential. Wildlife can damage crops, pollinate them, or consume pest species. Pollution may improve or harm particular processes depending on type and threshold, but should not operate as a vague universal penalty.

### 6.6 Ecology and industry must share environmental state

An ecological system becomes more interesting when industrial activity changes the habitat that supports it. The game can model a small number of causal channels rather than a complete global climate simulation:

- **Water:** extraction, irrigation, drainage, contamination, flooding, and downstream flow.
- **Vegetation:** clearing, regrowth, canopy cover, root stabilization, food availability, and fire fuel.
- **Soil:** nutrient depletion, compaction, erosion, moisture retention, and contamination.
- **Noise/light:** local disturbance that changes the attractiveness of habitats to selected species.
- **Waste/chemistry:** industrial byproducts that require containment or treatment, with species-specific exposure effects.

These variables should affect production and creature behavior through explicit rules. For example, removing canopy may change local temperature and moisture, alter the distribution of a shade-dependent species, and increase erosion on a slope. The simulation need not reproduce every ecological process; it must preserve a comprehensible causal chain that players can observe and influence.

---

## 7. Core systems architecture

### 7.1 Separate simulation domains

Recommended high-level domains:

1. **World graph service:** map nodes, connections, region ownership, discovery.
2. **Map simulation:** terrain, buildable cells, path topology, local hazards.
3. **Wave director:** schedules, enemy compositions, escalation, boss phases.
4. **Entity simulation:** enemies, allied minions, creatures, residents, vehicles.
5. **Combat resolver:** targeting, damage, status, armor, resistance, projectile impacts.
6. **Construction system:** placement, footprint, collision, build permissions, upgrades.
7. **Economy and inventory:** resources, currencies, storage, loot, recipes.
8. **Production/logistics:** factories, power, transport, throughput, automation.
9. **City simulation:** residents, services, morale, district status, events.
10. **Research and progression:** unlocks, blueprints, prestige, permanent upgrades.
11. **Roguelite run layer:** temporary boons, relics, run modifiers, reward pools.
12. **Ecology simulation:** populations, relationships, habitats, migration.
13. **Persistence:** save snapshots, event log, procedural seeds, version migration.
14. **UI/telemetry:** route previews, build validation, combat logs, production graphs.

These domains should communicate through typed events and stable interfaces. For example, a tower kill can emit `EnemyDefeated`; the economy system grants rewards, the ecology system updates local population, the loot system rolls eligible drops, and the wave director updates the remaining threat count. Avoid direct hard-coded calls from every subsystem into every other subsystem.

### 7.2 Event-driven simulation

Example event flow:

```text
EnemySpawned
  -> PathAssigned
  -> EnemyEnteredTowerRange
  -> TargetAcquired
  -> ProjectileLaunched
  -> ProjectileImpact
  -> DamageResolved
  -> StatusApplied
  -> EnemyDefeated
       -> RewardGranted
       -> LootRolled
       -> EcologyUpdated
       -> WaveProgressUpdated
       -> QuestOrContractProgressed
```

Events should carry stable entity IDs, map IDs, timestamps/tick numbers, source IDs, and relevant payload data. Keep authoritative state changes in the owning system; events announce facts rather than becoming a second source of truth.

### 7.3 Deterministic tick model

A fixed simulation tick is recommended for combat, logistics, and reproducibility. Rendering can run independently at a variable frame rate.

Potential update order:
1. Apply queued player commands.
2. Update map topology and construction changes.
3. Update wave director and spawn schedules.
4. Update entity perception and decisions.
5. Update movement and path following.
6. Resolve targeting and weapon firing.
7. Resolve projectiles and damage.
8. Apply status effects and deaths.
9. Update production and logistics.
10. Update economy and objective state.
11. Emit telemetry and save checkpoints.

The exact order should be tested because it determines whether, for example, a tower can fire at an enemy that entered range during the same tick.

### 7.4 Pathfinding strategy

- Use grid or navmesh pathfinding depending on map representation.
- Maintain a navigation graph per map.
- Recompute paths when topology changes, not for every unit every frame.
- Use shared path caches for units with the same destination and movement class.
- Support weighted terrain and enemy-specific traversal.
- Use local avoidance for moving crowds without constantly rebuilding global routes.
- Make route previews use the same rules as the authoritative simulation.

For maze maps, path validation should happen before construction is committed. For fixed-track maps, use authored splines or waypoint graphs with optional branches and dynamic blockers.

### 7.5 Entity and performance strategy

- Use data-oriented entity storage for large crowds.
- Separate high-fidelity entities from aggregated distant populations.
- Pool projectiles and short-lived effects.
- Avoid per-frame allocations in hot simulation loops.
- Use spatial partitioning for range checks and creature perception.
- Use level-of-detail simulation for distant maps.
- Profile worst-case wave density, not only normal gameplay.

### 7.6 Save architecture

Persist:
- World seed and generated map identifiers.
- Map topology edits and building placements.
- Entity state that must persist.
- City inventory, production queues, and resource stores.
- Research, unlocks, and blueprints.
- Faction and ecology state.
- Active run modifiers and campaign objectives.
- Event-log checkpoint and save schema version.

Do not serialize every transient projectile or every distant creature. Save authoritative state and enough information to deterministically reconstruct ephemeral entities.

### 7.6A Production, logistics, and telemetry as explicit graphs

The Dawnbreak reference cluster suggests that production, transport, and observability should be modeled as related but separate graphs. This is a design recommendation derived from the mod-reference inventory, not a claim that the Minecraft project used this exact architecture.

**Production graph.** A node transforms inputs into outputs under recipe, energy, temperature, pressure, and time constraints. Its state should be inspectable. A compact abstract schema is:

```text
ProductionNode
  id
  map_id
  recipe_id
  input_buffers[]
  output_buffers[]
  energy_ports[]
  process_state
  progress
  operating_limits
  fault_code
  last_state_change_tick
```

Recommended `process_state` values include `idle`, `working`, `input_starved`, `output_blocked`, `unpowered`, `overloaded`, `jammed`, `damaged`, `disabled`, and `maintenance_required`. The UI should explain the state in plain language and identify the missing condition. Avoid a generic red “not working” icon that hides the cause.

**Logistics graph.** A directed edge moves a typed commodity between buffers or depots. It should describe capacity per unit time, travel delay, loss/spoilage where relevant, route distance, energy demand, permissions, and physical exposure. Item, fluid, energy, signal, and mobile-package flows should be distinct edge types. A conveyor cannot transport electricity; a power wire cannot transport fuel; a radio signal cannot make an unpowered machine run.

**Telemetry graph.** Measurements observe the other graphs: production rate, buffer level, temperature, pressure, power draw, network connectivity, vehicle ETA, and fault events. A dashboard queries or subscribes to telemetry, but does not own the authoritative machine state. If a gauge is destroyed or disconnected, the machine's state still exists; only its observation is lost.

This separation avoids a common software-design trap: using a UI gauge, cached dashboard value, or automation script variable as the source of truth for the machine. The machine/process owner writes authoritative state; events announce state transitions; telemetry records measurements; interfaces render a view.

### 7.6B Hybrid inventory: distinguish stock, buffers, and moving consignments

The design should explicitly separate:

- **On-hand stock:** items physically stored at a location.
- **Reserved stock:** items committed to a recipe, construction order, emergency reserve, or contract.
- **In-transit stock:** goods loaded into a vehicle or package but not yet delivered.
- **Network-visible stock:** a remote query result that may be stale or subject to network permissions.
- **Production capacity:** the ability to make more goods, which is not itself inventory.

An order should not silently consume an item that is already reserved for base repair or a critical ammunition reserve. An inventory service can present a convenient global view, but each item lot must retain a location and status. In-transit goods should be recoverable after a vehicle loss, route interruption, or save/load cycle.

This creates an important tactical choice: centralize stock for convenience and efficiency, or decentralize it into forward depots for resilience. An enemy raid that severs a transport route should delay replenishment, not magically delete every remote inventory record. Conversely, a global network should not teleport bulk goods to any arbitrary machine without modeled throughput, power, and access rules.

### 7.6C Energy and fluids require typed conservation rules

The mod-reference cluster includes mechanical, electrical, fuel, fluid, pressure, and industrial processing concepts. The game should preserve these distinctions through typed resource carriers and conversion nodes. Every conversion should state its inputs, outputs, efficiency, operating range, and waste/byproduct behavior.

For a simple energy conversion, use a bounded relation such as:

\[
E_{out} = \eta E_{in}, \qquad 0 \leq \eta \leq 1
\]

The remaining energy may appear as heat or another modeled loss term. This is not a demand for thermodynamic-grade simulation; it is a guard against free energy loops and confusing “power” with stored energy. Power is a rate of energy transfer, while fuel or a battery represents stored potential. The interface should display the units it uses and avoid comparing unlike quantities.

Fluid networks can use a simplified pressure/flow model with throughput limits, pump curves or tiered pump ratings, pipe capacity, elevation costs, and leakage/damage. It is enough for players to reason that a remote high-elevation farm may need a stronger pump or an intermediate reservoir; it is not necessary to solve computational fluid dynamics.

### 7.6D World and release versioning

The Minecraft case study highlights why a persistent world is not just a save file. Mod-loader versions, world-generation changes, configuration changes, datapacks, registries, and content IDs can affect whether a region remains loadable and whether new chunks match old chunks. The proposed game should apply the same discipline even without a mod loader.

Every world save should record:
- game build and save-schema version;
- world-generation algorithm/version per generated region;
- content registry version and stable IDs for items, entities, buildings, and recipes;
- simulation-rule version for ecology, production, and combat;
- configuration and mod/plugin/module set where applicable;
- migration history and checksums for critical data.

A new generator must not silently rewrite already-generated regions. Use explicit migration policies: preserve old region data, regenerate only unvisited regions, or require a user-confirmed conversion with a backup. New content should have stable IDs and migration mappings so a renamed item or building does not become an orphaned save reference.

---

## 8. Combat, towers, enemies, and loot: proposed taxonomies

### 8.1 Tower taxonomy

A starting taxonomy could include:

| Role | Primary purpose | Example interaction |
|---|---|---|
| Ballistic | Reliable direct damage | Strong against ordinary targets |
| Artillery | Long-range area damage | Punishes dense formations |
| Beam/energy | Sustained or chained damage | Interacts with shields and conductive targets |
| Cryo/control | Slow, freeze, or route manipulation | Extends allied firing windows |
| Chemical/biological | Damage-over-time and debuffs | Synergizes with ecology or corrosion |
| Anti-air | Targets flying units | Limited ground utility |
| Detection | Reveals stealth or hidden threats | Enables other towers |
| Support | Buffs, repairs, reloads, or shields allies | Requires placement coordination |
| Economy | Generates or improves resources | Sacrifices immediate damage |
| Trap/terrain | Alters pathing or triggers on movement | Combines with maze layouts |
| Summoner | Deploys allied units | Creates a mobile defensive layer |
| Command | Marks targets or coordinates units | Strong strategic utility |

### 8.2 Enemy taxonomy

Enemies should be defined by behavior and counterplay, not only health:
- Swarm: many low-health bodies.
- Runner: high speed, low durability.
- Armored: high physical resistance.
- Shielded: regenerating or directional protection.
- Stealth: requires detection.
- Flyer: ignores ground obstacles.
- Burrower: bypasses or undermines some defenses.
- Sapper: attacks infrastructure.
- Support: heals, buffs, or shields nearby enemies.
- Splitter: creates smaller units on defeat.
- Harvester: steals or consumes resources.
- Siege: prioritizes walls and large structures.
- Elite: combines multiple traits.
- Boss: phased encounter with telegraphed mechanics.

Enemy combinations should be authored with clear intent. Avoid stacking so many traits that the player cannot infer a counter.

### 8.3 Loot categories

- **Materials:** common construction and crafting inputs.
- **Components:** refined machine and tower parts.
- **Modules:** installable defensive or production upgrades.
- **Relics:** rare rule-changing effects.
- **Boons:** temporary run-specific effects.
- **Blueprints:** reproducible structures or recipes.
- **Research samples:** unlock knowledge or creature studies.
- **Artifacts:** exploration rewards with unique utility.
- **Currency:** trade, construction, or upgrade medium.

### 8.4 Reward source and rarity logic

Use contextual pools:
- Enemy family pool.
- Boss pool.
- Biome pool.
- Faction pool.
- Expedition pool.
- Facility/production pool.
- Challenge contract pool.
- Secret/rare discovery pool.

A reward should have an intelligible provenance. Show whether it came from a boss, biome, research node, faction contract, or random drop. This supports player learning and reduces the feeling that progression is arbitrary.

### 8.5 Factory-backed defense and infrastructure targeting

The industrial case study suggests a useful category beyond “tower gets ammunition”: **defense infrastructure is a consumer and protector of the same production/logistics graph that powers the city**.

Examples:
- An artillery platform consumes shells from a local magazine. A belt, loader, or mobile carrier replenishes it. Its firing rate can be limited by ammunition delivery even when its weapon cooldown is ready.
- An electrical shield consumes power and may be deprioritized during a grid shortage. A priority controller can reserve power for the homebase before nonessential industry.
- A chemical sprayer consumes a finite reagent and water supply. Its area effect may be powerful, but requires an industrial process chain and safe storage.
- A repair drone can restore walls or machines only if repair parts and energy reach its service depot.
- A train-mounted gun or mobile depot can move defense to a threatened district, but becomes vulnerable to track damage, route congestion, or interdiction.

Enemies that target infrastructure should have explicit target priorities and counterplay. A sapper might attack exposed cables; a raider might steal an in-transit package; a siege unit might damage a rail junction; a burrower might compromise a buried pipe. The player must be able to protect, reroute, repair, or bypass these systems. Avoid attacks that simply disable the whole economy with no warning or practical recovery mechanism.

This adds a strategic layer to tower placement: a tower is not useful solely because it has the highest damage per second. Its ammunition route, power availability, line of sight, repair access, and reserve stock determine its actual combat uptime.

---

## 9. Economy, production, and balancing

### 9.1 Avoid one universal currency

Use a small number of resource classes with distinct roles:
- **Construction materials:** buildings, walls, roads, repairs.
- **Industrial components:** towers, machines, advanced upgrades.
- **Energy:** power budget and active abilities.
- **Food/biomass:** residents, creatures, bio-production.
- **Research data:** unlocks and experiments.
- **Salvage:** recovered from battle and exploration.
- **Rare regional materials:** specialized recipes and modules.

A separate soft currency can simplify trading, but should not replace all material flows.

### 9.2 Throughput is a balancing axis

A factory's output should be constrained by:
- Input availability.
- Machine speed.
- Power supply.
- Transport throughput.
- Storage capacity.
- Labor or automation level.
- Environmental conditions.
- Damage and maintenance.

This creates meaningful city planning without needing arbitrary build caps everywhere.

### 9.3 Defensive economy trade-offs

A tower that generates resources should have a real opportunity cost. It may:
- Occupy a valuable firing position.
- Consume power.
- Need protection.
- Produce less than a dedicated factory.
- Require a nearby logistics connection.
- Be vulnerable to raiders.

The player should be able to make a defensible choice between immediate combat strength and future economic growth.

### 9.4 Prestige and long-term progression

Prestige should unlock new decision spaces, not erase all meaningful history. Possible reset scopes:
- A temporary expedition run resets its boons and carried loot.
- A regional campaign reset can regenerate enemy pressure while retaining the homebase.
- A prestige tier can unlock new building families or automation capabilities.
- A high-level era can introduce new enemy rules, resource types, or map dimensions.

Always communicate exactly what resets, what persists, and what is gained.

### 9.5 Model the supply chain as part of the economy

A resource should have more economic meaning than its nominal unit price. The relevant cost can include extraction, processing, storage, spoilage, transport, energy, labor, risk, and opportunity cost. A remote alloy may be cheap to manufacture but expensive to ship; a local substitute may be less efficient but available during a siege.

Use explicit stages for high-value goods:

`raw input -> processed material -> component -> finished good -> deployed asset -> maintenance/recovery`

Each stage can add value, but also introduces throughput constraints and points of failure. A player may specialize one district in smelting, another in chemical processing, and another in ammunition assembly, then connect them with scheduled transport. This should be a strategic option, not a mandatory multi-hour conveyor puzzle for every player. Offer a compact “abstract logistics” option for low-volume or low-risk goods and reserve physical construction for important routes, high throughput, or vulnerable supply chains.

A useful balance measure is **delivered cost per useful output**, not only raw recipe cost. If one turret consumes 10 shells/minute, the relevant production design is not the shell recipe alone; it is the complete line's reliable delivery rate under expected transport and power conditions.

For critical defense supplies, let the player choose among:
- local magazines with high resilience but expensive duplicate stock;
- a central depot with efficient accounting but vulnerable long-range routes;
- distributed caches with an upkeep/stocking cost;
- flexible mobile supply that can reinforce different fronts but is exposed while moving.

This makes logistics choices meaningful without making one layout universally optimal.

---

## 10. Difficulty director and pacing

Use a RimWorld-like storyteller concept combined with explicit wave scheduling.

The director can consider:
- Current homebase health.
- Recent breaches and losses.
- Defensive strength and tower composition.
- Resource reserves and production stability.
- Number of active map fronts.
- Player-selected difficulty.
- Time since last major event.
- Recent rewards and recovery opportunities.

The director should select from designed event templates and budgets. It should not secretly hard-counter a player's build every time the player becomes strong.

### Suggested pressure rhythm

1. **Signal:** scouting reveals an incoming threat or environmental change.
2. **Preparation:** player builds, repairs, crafts, reroutes, or reallocates.
3. **Engagement:** waves attack and secondary objectives appear.
4. **Resolution:** rewards, damage, salvage, and map-state changes are applied.
5. **Recovery/expansion:** player repairs, researches, explores, or opens another connected map.

Endless mode can vary the duration and intensity of these phases. It should include periods of lower pressure so the city can grow and the player can interact with production systems.

---

## 11. Player-facing interface and information design

The game will have many interacting systems. UI clarity is a core technical requirement, not a polish task.

### 11.1 Strategic overview
- Show connected map nodes, threat direction, ownership, resources, and travel/logistics links.
- Use icons and concise labels for map categories.
- Show which maps are active, dormant, contested, or undiscovered.
- Let players compare resource and threat profiles before expanding.

### 11.2 Tactical track interface
- Show enemy route, predicted travel time, homebase destination, and path changes.
- Show tower range, target priority, damage type, status effects, and line of sight.
- Preview construction legality and the route after placement.
- Show wave composition and special enemy traits before they arrive where possible.
- Provide combat logs and damage attribution for advanced players.

### 11.3 City and factory interface
- Show production inputs, outputs, throughput, power draw, storage, and bottlenecks.
- Use overlays for power, transport, threat, pollution, and service coverage.
- Allow a building to be inspected without opening a maze of nested menus.
- Offer quick actions for repair, upgrade, assign worker, and configure recipe.
- Provide a blueprint placement preview and a way to compare variants.

### 11.4 Build and loot interface
- Filter by role, damage type, tags, rarity, source, and compatibility.
- Show exact module effects and stacking rules.
- Compare current and proposed loadouts.
- Explain synergies in plain language.
- Preserve saved blueprints and loadout history.

### 11.5 Industrial observability and fault explanation

Factory Controller, Extra Gauges, and pressure-gauge projects in the Dawnbreak reference list illustrate a broader design requirement: once the player builds more than a handful of machines, the game needs an inspection language. A factory should not become a wall of unexplained moving parts.

The UI should support:
- machine status and exact fault reason;
- input/output buffer levels and reserved quantities;
- production rate over a recent time window;
- energy use and supply priority;
- fluid pressure/flow or equivalent transport constraints;
- route capacity, in-transit consignments, and estimated delivery;
- alerts for starvation, blockage, overload, leaks, damage, and disconnected networks;
- a history view showing when a fault began and which upstream event caused it.

An automation dashboard should offer controls but also expose permissions and consequences. A player should be able to ask “why did this controller stop the chemical line?” and see a trace such as: reserve threshold reached → priority rule activated → nonessential consumer disabled → available energy stabilized. Do not bury control logic in an opaque black box.

The dashboard must be useful at two scales: a compact overview for the whole settlement and a detailed drill-down for a single machine, route, or production graph. This is an observability feature, not a requirement to show every data point continuously.

---

## 12. Development roadmap and minimum viable sequence

The project is broad enough that building all systems simultaneously would create high integration risk. A staged implementation is recommended. Before game-system implementation, add a short evidence and world-contract phase.

### Phase 0 — Establish the project and release evidence boundary
- Decide whether this design is a standalone game proposal, a future module for another project, or a research document only. Do not silently conflate it with Rainworld: Coalescence.
- Maintain a stable project vocabulary and separate the Coalescent Nights community identity from the Minecraft project identity.
- For any Minecraft-derived release being used as a baseline, preserve the exact Google Drive archive, manifest/lockfile, client/server split, configs, datapacks, and checksums.
- Create a minimal reference matrix: confirmed feature, source artifact, version, confidence, unresolved dependency, and intended design use.
- Define save-schema versioning and world-generation migration rules before persistent world content becomes expensive to change.

**Exit criteria:** a dated, immutable baseline and a clear statement of which claims belong to this proposed design versus the separate Minecraft project.

### Phase 1 — Prove the defense loop
- One homebase.
- One fixed track.
- A small tower roster with distinct roles.
- A small enemy roster with clear counters.
- Wave director and round economy.
- Base health, damage, defeat, and recovery.
- Deterministic combat tests and route tests.

**Exit criteria:** a complete, replayable defense loop with understandable balance and no major pathing/combat inconsistencies.

### Phase 2 — Add maze maps and connected map graph
- Grid-based maze map.
- Placement validation and route preview.
- Multiple map nodes connected by roads or gates.
- Strategic zoom and tactical zoom.
- Map persistence.

**Exit criteria:** players can move between maps, build a maze, and preserve topology changes across save/load.

### Phase 3 — Add city production
- A small set of buildings: mine, power plant, factory, storage, repair depot, research lab.
- Resource inputs and outputs with typed items, fluids, and energy carriers.
- Power, local buffers, and one transport constraint.
- Machine states with inspectable faults: starved, blocked, unpowered, overloaded, damaged.
- Construction and repair with reserved supply.
- Basic worker or crew assignments.

**Exit criteria:** city production materially supports defense and defense materially supports city production; a player can diagnose a broken production chain without reading developer logs.

### Phase 4 — Add modular homebase and buildcraft
- Homebase modules.
- Tower upgrades and blueprints.
- Damage types and resistances.
- Active abilities.
- Contextual loot pools.

**Exit criteria:** multiple viable defensive builds exist and can be compared through telemetry.

### Phase 5 — Add expeditions, roguelite rewards, and exploration
- Procedural map templates.
- Biome resources and hazards.
- Boons, relics, artifacts, and crafting recipes.
- Boss encounters and expedition extraction.
- Persistent discoveries.

**Exit criteria:** expeditions create useful long-term rewards without replacing the city or main defense loop.

### Phase 6 — Add ecology and storyteller
- Creature behavior modules with explicit prey/threat/friend/item/home relationships.
- Population and habitat simulation with active, regional, and dormant fidelity tiers.
- Crop/soil/water state for one test biome before adding many species.
- Faction relationships and a bounded event director.
- Ecological changes that affect resources and threats through visible causal chains.

**Exit criteria:** local ecology produces observable, coherent behavior; it can be explained from its state and logs; and dormant regions preserve their outcomes without full entity simulation.

### Phase 7 — Add automation and advanced logistics
- Conveyor and pipe networks with measurable capacities.
- A local inventory model, one mobile consignment type, and one inter-district route.
- Trains/vehicles only after route planning and save recovery tests pass.
- Blueprint copying with dependency and footprint validation.
- Scriptable automation with permissions, reserved-stock protection, and inspectable logs.
- Offline production with caps and explicit catch-up rules.

**Exit criteria:** automation reduces repetitive management without hiding failures; goods remain accounted for through blockage, vehicle loss, server restart, and save/load.

### Phase 8 — Endless scale and community maps
- Procedural region generation with generator/schema versioning.
- Infinite frontier expansion with finite active simulation budgets.
- Challenge contracts and map editor validation.
- Community contribution workflow for maps/content where a community is actually part of the product.
- Long-run performance, multiplayer, and save migration tests.

**Exit criteria:** world expansion remains performant and new regions are meaningfully different rather than just larger numbers.

---

## 13. Risks, design questions, and unresolved decisions

This document intentionally does not resolve every design choice. The following questions should be answered through prototypes and playtests.

### Spatial design
- How much of the main track can the player modify?
- Can towers block paths, or are some tower foundations non-blocking?
- Are roads also enemy routes, or do enemies use separate paths?
- How are flying, burrowing, climbing, and teleporting enemies represented?
- Is terrain deformation a core mechanic or an expedition-only feature?

### City simulation
- Are residents individually simulated or mostly role-based?
- How much upkeep should buildings require?
- Can production continue during a siege?
- Can enemies destroy logistics lines, and how quickly can they be repaired?
- How many active districts should be simulated at full fidelity?

### Combat
- Is the player directly controlling a commander, or primarily issuing strategic orders?
- Do friendly minions fight automatically, follow lanes, or receive tactical commands?
- Can towers target structures and infrastructure as well as enemies?
- How many status effects can stack, and which are mutually exclusive?

### Roguelite progression
- Are boons temporary per expedition, per siege, or per campaign chapter?
- Can rare relics permanently alter the homebase?
- Should the player be able to reroll rewards, and what resource should that cost?
- How do item pools avoid repetitive or invalid combinations?

### Endless progression
- Does the world expand horizontally through new regions, vertically through deeper layers, or both?
- How does enemy scaling avoid making old towers obsolete?
- Does prestige introduce new mechanics or mostly larger numerical ranges?
- What is the long-term purpose of exploring a map after its resources are depleted?

### Ecology
- How much creature behavior should be simulated outside active maps?
- Can the player meaningfully protect or restore an ecosystem?
- Are some creatures neutral, beneficial, or domesticated?
- How do ecological systems remain understandable when many species interact?

### Content and compatibility provenance
- Which exact release manifest, loader, and dependency set defines the baseline when using a Minecraft modpack as a reference?
- Are project links being mistaken for installed JARs, or are multiple incompatible loader generations being merged into one theoretical pack?
- Which world-generation and content-registry changes require migration, and which changes must be restricted to new regions?
- Are client-only rendering optimizations being confused with server simulation improvements?
- Do threaded or tick-altering performance mods change semantics as well as speed, and what regression tests detect that?

### Community and contribution boundary
- Is the community being asked to test, build, moderate, or develop code, and are these roles described separately?
- Are volunteer tasks bounded and accurately described as unpaid where that is the arrangement?
- Who can publish a release, change server configs, or approve world migrations?
- Can contributors reproduce a bug from a pinned release archive rather than an unversioned current install?

### Automation
- Is automation configured through simple rules, node graphs, or a scripting language?
- Can automation issue construction commands, or only operate existing systems?
- How does the player inspect why a script made a decision?
- What permissions prevent automation from consuming reserved resources or dismantling critical defenses?

---

## 14. Recommended prototype experiments

Before committing to the full architecture, build small experiments that answer the highest-risk questions.

1. **Maze-vs-fixed-track prototype:** compare player comprehension, path exploits, and tactical variety.
2. **Multi-map zoom prototype:** test whether a world graph and seamless zoom are navigable and understandable.
3. **Main-track breach prototype:** test whether minions attacking a persistent homebase creates compelling pressure or becomes a repair chore.
4. **Tower synergy prototype:** test a small tag-based interaction system with 10–15 modules.
5. **Factory-throughput prototype:** test whether visible belts, storage, and power constraints are fun at small scale.
6. **Ecology prototype:** simulate three prey species, two predators, one scavenger, and one resource patch; measure whether behavior is legible.
7. **Storyteller prototype:** compare fixed wave schedules with a budgeted adaptive director.
8. **Automation prototype:** let players automate one mature production line and inspect every decision.
9. **Offline progression prototype:** test bounded production, storage overflow, and power failure while away.
10. **Long-run performance prototype:** stress-test thousands of entities across active and dormant maps.

11. **Factory telemetry prototype:** build a tiny process line with two machines, a buffer, a power source, and a dashboard. Deliberately cause starvation, blockage, disconnection, and overload. Measure whether a new player can identify the fault and its upstream cause without external instructions.
12. **Hybrid inventory prototype:** compare physical conveyor delivery, mobile consignment delivery, and abstract network availability. Test reservations, stale state, in-transit loss, network outage, and save/load recovery.
13. **Ecology–agriculture prototype:** simulate one crop, one soil type, rain/irrigation, a herbivore, and a pollinator or pest. Verify conservation bounds and whether the player can identify why yield changed.
14. **Industrial disruption prototype:** let one enemy class damage a transport edge or raid a consignment. Test recovery choices, alternate routing, local reserve policy, and whether combat remains readable during supply interruption.
15. **Release migration prototype:** create a world under one content/generator version, update the content registry, and test the three policies: preserve old regions, generate only new regions with the new rules, or explicitly migrate with backup.
16. **Multiplayer contribution prototype:** test a community map or build contribution through validation, permissions, provenance, review, deployment, and rollback. Do not assume a build-team role automatically confers code-release authority.

Each experiment should have a written hypothesis, a measurable success criterion, a known failure condition, and a record of observed results. Do not treat a prototype that merely runs as evidence that the system is fun or scalable.

---

## 15. Synthesis: the strongest combined design

A coherent version of this game could work as follows:

The player begins with a small homebase on a clearly defined main track. Enemies approach along the track in readable waves. The player places a handful of towers, traps, and support structures, learning the Bloons-style combat language. The track has enough editable space to allow limited maze construction, borrowing Desktop Tower Defense's spatial puzzle.

Between waves, the player expands the settlement. A mine produces raw materials, a power plant supports the defenses, a factory creates ammunition and modules, and a repair depot maintains the homebase. Production is visible and spatially connected, drawing from Satisfactory and Create. Residents and specialists improve the city, while a storyteller introduces raids, weather, trade, discoveries, and faction events in a controlled rhythm inspired by RimWorld.

The player then opens a nearby map node. It may be a resource district, a cave, a forest, a ruined industrial site, or a new track. Each map has a distinct terrain and ecological profile. The player can explore it, build an outpost, establish transport, and connect it back to the city. The world map becomes a network of useful places rather than a menu of disconnected levels.

Expeditions provide roguelite rewards. The player chooses boons, discovers artifacts, crafts modules, and builds synergies inspired by Isaac and Hades. Procedural regions and resource discoveries provide the sense of exploration found in No Man's Sky and Astroneer. The ecology can create emergent interactions: predators hunt prey, creatures avoid threats, scavengers seek remains, and some species can disrupt or assist the player's operations, using Rain World as a behavioral reference.

As the settlement grows, the homebase becomes modular. It can be configured for shields, artillery, logistics, command, repair, or specialized damage types. The player saves defensive blueprints and can switch configurations for different threats, using The Perfect Tower II's modular buildcraft as a foundation. The research system expands into multiple domains, while automation gradually takes over repetitive production and maintenance.

The main track never becomes irrelevant. It remains the central strategic pressure that gives the city a reason to exist. But the player is no longer confined to a single arena: the track is the heart of a growing network of maps, production sites, ecological regions, and defensive fronts.

The result is not simply a tower defense game with a city attached. It is a **persistent strategy sandbox in which tower defense is the primary conflict-resolution language, city building is the long-term preparation layer, exploration expands the available world, and automation converts accumulated understanding into scale**.

### 15.1 Re-synthesis informed by the Dawnbreak engineering case study

The combined design now has five mutually dependent layers rather than four isolated feature lists:

1. **Conflict:** tracks, waves, enemy behavior, towers, walls, bosses, and homebase integrity.
2. **Production:** extraction, processing, assembly, energy conversion, agriculture, maintenance, and research.
3. **Logistics:** local buffers, belts and pipes, mobile packages, trains/vehicles, depots, and networked stock visibility.
4. **Living world:** weather, soil, vegetation, water, wildlife, habitats, and regional ecological change.
5. **Information and control:** gauges, route previews, production dashboards, alarms, automation rules, event histories, and save/migration metadata.

The conflict layer consumes the outputs of the other layers. The production layer creates goods but does not determine where they arrive. Logistics delivers them, but its routes can be congested or attacked. The living world changes resource availability and route conditions. Information/control lets the player observe and manage the whole system. None of these layers should silently overwrite another's authoritative state.

#### 15.1.1 The settlement as a network of production cells

Treat a district as a collection of production cells. Each cell owns a small set of machines, buffers, workers or automations, and local consumers. The cells connect through a transport network. A mining district may export ore and stone; an agricultural district may export food, fiber, and biological reagents; a smelting district may export ingots and alloys; an ammunition district may export shells and cartridges; a forward-defense district may import ammunition, power, and repair parts.

This avoids a single global crafting menu that makes geography irrelevant. It also avoids the opposite extreme in which every item must be manually carried through a visually accurate belt maze. Local movement can be physical and visible; long-haul movement can be abstracted into scheduled consignments with explicit capacity and travel time. The player chooses where detail matters.

#### 15.1.2 The homebase as both objective and industrial consumer

The homebase is not a static health bar with decorative rooms. It is a collection of systems with competing demands: shields, weapons, sensors, command systems, life support, repair, storage, and communications. A siege creates a demand spike. The player may reroute energy, prioritize ammunition assembly, suspend luxury production, deploy emergency reserves, or withdraw from a remote district to reinforce the main track.

A meaningful “breach” should have staged consequences. Damage to an outer wall may expose a logistics corridor; a damaged magazine may reduce local ammunition capacity; a disabled sensor array may degrade detection; a broken pump may compromise cooling. Critical failure should be telegraphed and recoverable until the player has exhausted meaningful options. This allows the city to matter during combat without making every defeat a catastrophic world reset.

#### 15.1.3 Ecology is a productive and adversarial subsystem

Vegetation, wildlife, and weather should influence both the economy and tactical geometry. Forests can provide biomass, shade, habitat, and concealment; clearing them may open a firing lane but increase erosion or remove a food source. Wetlands can slow heavy vehicles and support certain species. Pollinators can improve crop output; herbivores can damage fields; predators may control pests but threaten livestock. Industrial contamination can change species distribution or make water treatment necessary.

Creatures should not be spawned solely as wave counters. The *Rain World* reference is valuable because it encourages designers to model a creature's own needs and relationships; the Dawnbreak mod inventory is valuable because it points toward visible environmental and agricultural state. The synthesis is a local ecosystem that interacts with player infrastructure, not a requirement to simulate a complete planet.

#### 15.1.4 The player progresses from handling to instrumentation to automation

A strong progression sequence is:

- **Manual:** the player inserts fuel, moves supplies, selects a recipe, and repairs a machine.
- **Instrumented:** gauges show stock, pressure, throughput, temperature, energy, and faults.
- **Controlled:** thresholds and simple logic automate pumps, switches, and production priorities.
- **Scheduled:** transport and production orders run according to demand, capacity, and route state.
- **Strategic:** the player supervises multiple districts through a dashboard and intervenes when a high-level plan fails.

Automation should not remove accountability. Every automated action should have a traceable rule, permission, trigger, and result. Reserved supplies and protected structures should not be consumed or dismantled by a general-purpose optimizer unless the player explicitly authorizes it.

#### 15.1.5 A concrete play sequence

A representative midgame event can connect all layers:

1. A scout discovers a high-grade ore deposit in a forested valley and maps a narrow pass to the site.
2. The player establishes a mine, a small power source, and a stockpile, then chooses whether to clear vegetation for a road or build a more expensive rail spur that preserves part of the habitat.
3. Local wildlife changes its movement after the construction. Some species avoid the noisy mine; another scavenges dropped material; a territorial predator continues to use a den near the transport corridor.
4. The mine's output travels in mobile consignments to a central smelter. A gauge reports rising queue time because the smelter's input buffer is full and a downstream rolling machine is unpowered.
5. The player inspects the dashboard, sees the fault chain, and redirects one generator from nonessential industry to the rolling line. The machine restarts, but the resulting alloy shipment misses its scheduled train.
6. A hostile wave begins on the nearby track. The forward turret has enough ammunition for the first part of the wave but depends on the delayed shipment. The player can use local reserve shells, call a slower convoy, or change the turret's firing policy.
7. A sapper attacks the exposed transport switch. The player chooses between defending the switch, rerouting through a longer protected line, or accepting a temporary supply interruption while the homebase changes to an energy-efficient shield mode.
8. After the siege, the player repairs the line, audits the fault/event history, and builds a second local magazine. The valley remains altered; ecological populations, vegetation, and industrial structures retain their saved state.

This sequence illustrates why the game's depth should come from **causal coupling with readable state**, not from adding many disconnected subsystems. The player's decisions have spatial, economic, ecological, and tactical consequences, while the interface provides enough information to understand those consequences.

#### 15.1.6 Community context is a development environment, not a game mechanic

The Coalescent Nights description provides a concrete example of a small community organized around a primary Minecraft project while supporting other games. Its stated roles distinguish volunteer mod helpers from build-team members who receive creative build access; game-specific and other roles can be self-assigned through `#reaction-roles`. The community describes itself as LGBTQIA+ friendly, actively moderates its Discord and Minecraft server, can organize events, and uses Monkestation for Minecraft server hosting. The community description identifies **@Cosmic** as the point of contact for people seeking addition to the volunteer helpers team and explicitly says the role is unpaid.

Those details belong in project operations and community documentation, not in the simulated world economy. A build-team role is not equivalent to source-code write access; helper-team membership is not automatically release authority; a Discord role is not a server permission until the server's permission configuration makes it so. If a game project uses volunteer contributions, it should separately define scope, review, attribution, access, approval, deployment, and rollback. The community's role model is a useful real-world example of separating participation pathways, but the technical authorization rules still need to be explicit.

The permanent invite supplied by the owner is [discord.gg/sQYHfY6Eyc](https://discord.gg/sQYHfY6Eyc). It is included as project context, not as a claim that the assistant has inspected private channels or verified their contents.

#### 15.1.7 Re-synthesized design statement

The strongest version of the concept is a persistent, expandable world in which a defensive settlement survives through an industrial and ecological network. Tower defense supplies the central conflict grammar; city building creates productive capacity; explicit logistics turns capacity into delivered resources; ecology makes geography and extraction consequential; and instrumentation/automation lets players scale their management without losing the ability to inspect or override the system.

The Dawnbreak reference inventory supports the usefulness of studying physical production, hybrid logistics, process measurement, world ecology, and performance constraints together. It does not prove that those systems were all installed together, nor does it establish that Rainworld: Coalescence already contains this proposed game design. The role of the case study is to inform a more technically grounded proposal while preserving the boundary between historical evidence and original design.


---

## 16. Reference notes and source links

These sources support the factual descriptions of the reference games and their publicly documented mechanics. Some design interpretations and all proposed integrations are original synthesis rather than claims made by the source games.

### Primary and high-value references

- Desktop Tower Defense / maze-defense overview: https://compendium.tallmove.com/mechanics/tower-defence
- The Perfect Tower II official site: https://www.perfecttower2.com/
- The Perfect Tower II developer page: https://www.fs-studios.com/games/the-perfect-tower-2
- The Perfect Tower II wiki — modules: https://wiki.perfecttower2.com/Modules
- The Perfect Tower II wiki — main page: https://wiki.perfecttower2.com/Main_Page
- Bloons TD 6 reference document (community-maintained technical overview; verify details against current game versions): https://github.com/roger765/Monkey-Bloons-Tower-Defence/blob/main/BTD6-REFERENCE.md
- Infinitode 2 official press kit: https://infinitode.prineside.com/?m=press_kit_en
- Infinitode 2 Steam page: https://store.steampowered.com/app/937310/Infinitode_2Infinite_Tower_Defense/
- Deadlock lanes and troopers: https://deadlock.io/en/mechanics/lanes-and-troopers
- Deadlock guardians: https://www.deadlock.io/en/mechanics/guardians
- Supercell support — Builders and Builder's Huts: https://support.supercell.com/clash-of-clans/en/articles/builders-4.html
- Supercell support — Builder Base: https://support.supercell.com/clash-of-clans/en/articles/builder-base-the-basics-2.html
- Satisfactory Wiki — systems and logistics: https://satisfactory.wiki.gg/wiki/Satisfactory_wiki
- RimWorld Console Edition — storyteller systems: https://rimworld.double11.com/features
- No Man's Sky official site: https://www.nomanssky.com/about/
- Minecraft Create mod overview: https://www.curseforge.com/minecraft/mc-mods/create
- Rain World creature behavior and ecosystem design (Unity developer article): https://unity.com/blog/exploring-procedural-design-rain-world
- The Binding of Isaac: Rebirth — item pools: https://bindingofisaacrebirth.wiki.gg/wiki/Item_pool
- Cookie Clicker production systems reference: https://teemo.dev/game-design/cookie-clicker/systems/buildings-and-cps-reference/

### Dawnbreak, Rainworld: Coalescence, and ecological/industrial case-study sources

- Unity developer interview: [Exploring Procedural Design in Rain World: The Watcher](https://unity.com/blog/exploring-procedural-design-rain-world) — describes creature relationship trackers, configurable behavior modules, and the balance between emergent behavior and authored regional guidance.
- [Create](https://github.com/Creators-of-Create/Create) — base mechanical automation project.
- [Create: Diesel Generators](https://modrinth.com/mod/create-diesel-generators) — fuel processing, engines, crude oil/refinery features, and industrial machinery; the published project page exposes several Minecraft/loader branches, so the exact file matters.
- [Create: Factory Controller](https://modrinth.com/mod/create-factory-controller) — centralized dashboard for factory gauges and logistics control. Its documentation explicitly notes that gauge extensions need compatibility support before their custom gauges can appear inside the controller.
- [Create Mobile Packages](https://github.com/timplay33/Create-Mobile-Packages) — mobile, addressed item packages and logistics networks.
- [Dynamic Trees](https://modrinth.com/mod/dynamictrees) — growing multi-block tree structures and a stated focus on limited scope/performance and compatibility.
- [Agricultural Enhancements](https://github.com/alacripose/Agricultural_Enhancements/tree/1.21.1-neoforge) — separate public mod repository used as an agriculture-systems reference; not evidence of inclusion in the supplied Dawnbreak list.
- [Dawnbreak and Rainworld: Coalescence research dossier](dawnbreak_rainworld_coalescence_research_v2.md) — detailed analysis of the supplied 95-project reference inventory, version fingerprints, compatibility caveats, and evidence gaps.
- [Coalescent Nights project and community context](coalescent_nights_project_and_community_context.md) — owner-supplied identity, community roles, distribution history, and archival priorities.

### Reference limitations

- The exact mechanics and balance of live-service games can change. Treat game-specific values and patch-sensitive behavior as version-dependent.
- Some references are community-maintained wikis or technical summaries, not primary developer documentation.
- Nubbies Number Factory is included as a provisional inspiration because publicly accessible technical documentation is limited; its proposed role should be validated against direct gameplay observation.
- Fortnite references here are focused on *Save the World*-style base defense and traps; Battle Royale construction is a separate inspiration for rapid spatial building, not the primary model.
- This document deliberately avoids copying exact protected tower rosters, item names, characters, maps, art direction, or proprietary formulas. It extracts general design patterns and proposes original combinations.

---

# Addendum A — Deadlock match-state, persistent homebase expeditions, Rust-inspired settlement founding, and Secure Shell integration

**Research snapshot:** October 31, 2026  
**Purpose:** Extend the original design with a deeper analysis of Deadlock's changing match state, high-level coaching perspectives, expedition-based homebase founding, persistent world saves, and the intended Secure Shell platform boundary.

> **Precedence note:** This addendum supersedes any older or simplified Deadlock timing, lane-count, Mid-Boss, Rejuvenator, or comeback-mechanic statements elsewhere in this document. Deadlock is an actively updated game; exact timers and values below are patch snapshots, not constants to hard-code into the proposed game.

## A1. Evidence method: distinguish rules, measured outcomes, and coaching advice

Deadlock is useful because its game state changes in several interacting dimensions. It is not enough to study the match clock or the order of objectives. A match evolves through lane pressure, resource acquisition, contested map events, item power spikes, player availability, structure vulnerability, and the ability to convert a temporary advantage into permanent map control.

Three evidence classes must be kept separate:

1. **Rules and patch facts:** mechanics documented by Valve's in-game guide or patch notes. These can be implemented as game rules, but must be versioned.
2. **Measured match observations:** statistics such as average match length or the average time of the first major objective. These are samples from a particular period and matchmaking population, not guaranteed schedules.
3. **Coach/player decision principles:** heuristics used by high-level players to decide when to farm, push, rotate, group, contest, or disengage. These are contextual policies, not fixed engine rules.

This separation matters. For example, a scheduled objective may spawn at a known time, but a team should not automatically contest it if doing so abandons a large wave, exposes a structure, or gives the opponent a better trade elsewhere.

### A1.1 Deathy: diagnose the player's mental model, not only their mechanics

Deathy's public coaching profile identifies him as a verified Deadlock coach, full-time content creator, and former high-ranked competitive player. The profile documents more than 25 completed coaching sessions. Student reviews describe a method centered on asking the player to explain what they believe is happening, then correcting inaccurate assumptions and identifying repeatable decision errors. Reviews specifically mention misconceptions about midgame strategy, role understanding, and decision-making across match phases.

His indexed *How to Navigate Deadlock's Mid-Game — 2026* material separates macro between fights, wave pushing before farming, and converting a won fight into the nearest objective. His October 8, 2026 beginner guide is especially useful as a current-version index for map movement, the Soul Urn, catch-up systems, and additional soul sources. The indexed moments are evidence of the topics covered; they are not a substitute for a full transcript of every coaching session.

**Design translation:** the game should expose enough state that a player can form and revise a mental model. It should not only display an objective marker and say “go here.” It should explain the current wave, next event, structure risk, available resources, and likely cost of leaving a position. Post-action summaries should identify which assumptions were correct or incorrect.

### A1.2 POSHYPOP: competitive team coordination, macro priorities, and closing a lead

POSHYPOP's public coaching profile records a history as a professional player, Rank 1 NA, eight consecutive Deadlock Fight Night championships with Melee Creeps, a first LAN victory, and a subsequent transition into coaching. The current profile records him as Head Coach of Melee Creeps from July 2026. Publicly indexed reviews describe VOD review, objective approach, shot-calling/IGL considerations, macro, how to preserve a lead, and a midgame concept described as “half Mid-Boss.”

His *Deadlock Coaching Session on Midgame Macro as Pick* video dates to October 20, 2025. It is valuable as a coaching example, but predates the current three-lane map and several major objective changes. It should be used for the reasoning patterns of coordinated midgame play, not for current route timings or old lane structure.

**Design translation:** a multi-unit settlement should have a coordination layer. It needs to know who is assigned to hold a route, who can safely leave, what capability is ready, what objective is being prepared, and which structure or resource will be lost if the group rotates. A commander's or manager's job is not to issue every action; it is to coordinate units with different roles and readiness states.

### A1.3 Midknight: teach by match phase, role, and VOD review

Midknight's *Everything I've Learned From 1 Year of Coaching in Deadlock* (September 8, 2025) is structured around laning mistakes, mid/late game, and general principles. His July 29, 2026 *How to Play Spirit Carry* role guide is more current and separates role fundamentals, laning, midgame, and late game. The publicly indexed video descriptions identify his coaching and VOD-review background, but the underlying videos are not full-text transcripts in this research set.

**Design translation:** the game should teach both general rules and role-specific responsibilities. An artillery crew, scout, engineer, carrier, repair team, or mobile defender should receive different advice because their position and contribution differ. The same event can be correct for one role and a mistake for another.

### A1.4 Research limitations

This research uses current public game mechanics pages, the October 30, 2026 patch record, public coaching profiles/reviews, and indexed creator-video chapters. It does **not** claim to have exhaustively watched every stream or recovered complete transcripts from Deathy, POSHYPOP, or Midknight. The coach section therefore identifies documented teaching emphases and translates them into proposed mechanics; it does not fabricate quotations or attribute unverified build orders to the creators.

## A2. Deadlock as a changing match-state system

The most useful model is not “early game, midgame, late game” as three fixed scripts. It is a state vector that changes as events, structures, economy, and unit availability change.

### A2.1 Match state vector

A match director or AI opponent should evaluate at least these dimensions:

| Dimension | Variables to observe | Why it changes decisions |
|---|---|---|
| Lane pressure | Wave position, wave size, push direction, time to structure, route access | Determines whether it is safe to rotate or whether a structure will be pressured |
| Economy | Earned resources, unspent resources, carried/unsecured rewards, income rate, distribution between players | A nominal lead is not the same as a usable immediate power spike |
| Map control | Structures, neutral sites, camps, transport routes, vision/knowledge, safe regions | Determines which routes and objectives can be approached safely |
| Unit readiness | Alive/downed units, health, cooldowns, ammunition, active abilities, role coverage | A numerically even team may still lack the tools for a particular engagement |
| Objective schedule | Spawn timers, warning windows, capture/contest state, reward duration | Creates short windows when map control can be converted into value |
| Structure state | Protection, health, repair, nearby friendly wave, exposed attack surfaces | Determines whether an objective can actually be damaged or captured |
| Threat information | Visible enemies, missing enemies, recent sightings, likely rotations | Absence is information; missing units should increase caution around high-value events |
| Power spikes | Purchased or unlocked upgrades, item breakpoints, technology readiness | A player or squad may be temporarily stronger even if total economy is similar |
| Comeback pressure | Relative wealth, bounties, bonus income, exposed high-value targets | A lead that is not converted into permanent objectives can be reversed |
| Time horizon | Respawn duration, event time, travel time, build/repair time | Determines whether a temporary advantage lasts long enough for a particular action |

A suitable engine representation is:

```text
MatchState
  clock
  map_revision
  lanes[]
  structures[]
  active_units[]
  downed_units[]
  objective_events[]
  neutral_sites[]
  economy_by_player[]
  economy_by_team[]
  unspent_resources[]
  unsecured_rewards[]
  active_buffs[]
  cooldown_state[]
  known_enemy_positions[]
  threat_estimates[]
  current_power_spikes[]
  next_decision_deadlines[]
```

The state vector should be recomputed as events occur, then used by a decision layer. A “phase” is a derived interpretation of this state, not the authoritative truth.

### A2.2 Current three-lane opening: collection and lane pressure

The current main Deadlock map uses three lanes, with teams beginning in a 2-2-2 distribution. The old four-lane assumptions in older guides are obsolete. Trooper waves arrive every 30 seconds, then more frequently later in the match: every 25 seconds from 20:00 and every 20 seconds from 35:00 in the current timing reference.

The early game is a repeated collection-and-positioning problem:

- Clear or contest the lane wave.
- Collect the resulting rewards rather than treating damage dealt as automatically secured income.
- Decide whether the wave should be pushed forward or held in a safer position.
- Use the interval before the next wave for a nearby camp, crate, shop, buff, or short rotation.
- Avoid leaving a wave in a position where it will pressure one's own structure.
- Watch missing enemy players because they may be moving toward a camp, another lane, or an objective.

The key lesson is that a wave is both income and positional leverage. Clearing it creates time and changes the next engagement location. It is not always best to clear as fast as possible: the important question is what the resulting position enables.

Deadlock's current soul collection model also demonstrates the difference between damage, reward generation, and reward security. Trooper rewards can include a ground pickup and a contestable airborne orb. A player who shoots a wave and immediately leaves may abandon part of the reward. The design should make this distinction legible.

**Transfer to the proposed game:** a defensive wave should create an economic and operational window. Defeating attackers might release salvage, a capture opportunity, or a short interval in which builders can repair a gate, a convoy can move, or a scout can establish a forward point. The reward should not be considered banked until it has been secured or transported.

### A2.3 Transition: scheduled objectives create coordination pressure

As the laning phase winds down, the match becomes less about maintaining one local position and more about moving between several valuable map events. Current sources place the first Soul Urn at 10:00 and every five minutes thereafter. The Unstable Rift is a separate event that appears on a random side lane at roughly seven-minute intervals, with a variation window and a short warning. Bridge power-ups and neutral sites create other timing opportunities.

The important structure is not a single fixed rotation. It is a competition between:

- The lane wave that will be lost if a player leaves.
- The objective reward if the team arrives in time.
- The enemy's likely ability to contest.
- The travel path and movement options.
- The possibility of trading a different objective on the opposite side.

A team that cannot contest one event may gain more by taking a structure, collecting a resource, or pushing another route while the opponent is committed elsewhere.

**Transfer to the proposed game:** add scheduled and event-driven world opportunities that compete for the player's attention. A rare ore bloom, traveling convoy, creature migration, weather event, siege warning, or temporary power surge should not all be automatically mandatory. Each event should have a window, opportunity cost, and possible counter-trade.

### A2.4 Midgame: map control and objective conversion

The midgame emerges when the map opens and players can rotate between lanes, neutral sites, and structures. Deadlock coaching and guides emphasize a repeating macro cycle:

1. Clear a wave that would otherwise pressure a structure.
2. Push it when it is safe and useful.
3. Use the interval before the next wave to farm a nearby camp, collect supplies, or rotate.
4. Arrive early enough to establish position around the next objective.
5. Fight only when unit count, readiness, positioning, and reward justify it.
6. After a won fight, convert the temporary advantage into a nearby permanent objective.
7. Reset, spend resources, recover, and repeat.

This is a decision cycle rather than a mandatory sequence. If the home structure is threatened, the player should defend. If an objective is uncontestable and a different trade is available, the player should not sacrifice an entire squad for a low-probability contest.

A kill or local victory should be interpreted as a temporary absence of opposing actions. It creates a time budget. The correct follow-up depends on what can be reached before the defeated force returns.

In Deadlock, objective progression also unlocks strategic capacity. Destroying Walkers grants team Flex Slots. Later structures alter the access to the base and the strength of allied troopers. A structure kill is therefore more than a payout: it changes the future capability set of the team.

**Transfer to the proposed game:** a successful defense or expedition should change the world graph or settlement capabilities. Capturing a depot could create an additional logistics connection. Securing a region might increase construction privileges. Defeating a siege engine could unlock an artillery blueprint. Protecting an industrial site could create a permanent supply link. These are persistent conversions, not only currency rewards.

### A2.5 Late game: long recovery times and irreversible objectives

Late Deadlock matches are shaped by long respawn windows, grouped fights, base access, and the need to convert a won engagement into an objective before defenders return. An October 2026 guide records illustrative respawn times of 38 seconds at 20 minutes, 70 seconds at 30 minutes, and 85 seconds at 40 minutes, with a 90-second ceiling. These are version-specific values, not a formula for this game.

The late match structure progresses through increasingly protected objectives. The enemy base requires the relevant defensive layers to be removed before the Patron can be attacked. Super Troopers and the Mid-Boss reward support the final push. A lost late fight can expose the base; a won fight can create a short window to finish a structure.

Teamfight advice also emphasizes conditions before engagement: numbers, cooldowns, position, role coverage, and an objective worth winning. Focus fire and staggered abilities create more value than six units independently choosing targets. A team that wins but fails to take an objective has not fully converted its advantage.

**Transfer to the proposed game:** late-stage threats should change the cost of failure. A lost battle may expose a homebase module, regional command facility, or transport hub. A successful defense may create an unusually long construction/repair window or permit a counter-expedition. The game can make late combat consequential without requiring a final victory screen.

### A2.6 Current October 30, 2026 patch: a concrete example of dynamic rules

The October 30, 2026 Deadlock gameplay update is especially important because it changes the value and risk profile of the Mid-Boss event:

- Mid-Boss health and per-minute health scaling increased by 20%.
- Resistance reductions are only half as effective against it.
- Its roar now occurs at 25% health rather than 50%.
- Its bounty fell from 3,000 (+50/min) to 1,500 (+25/min).
- The Rejuvenator now drops as three crystals placed around the pit rather than one central crystal.
- Each crystal is broken with one heavy melee and grants one team Rejuvenator; all three together preserve the total three rewards.
- Breaking each crystal now grants a full heal and a personal bounty, and the crystal drop time increased from six to 7.5 seconds.
- Haunt rewards and difficulty rose; Haunted Soul secure timing became slower at baseline but can accelerate for the team behind.
- Non-hero-source comeback gold increased from a 26% maximum to 30% at a roughly 20% net-worth deficit, and the two poorest players also receive extra tick-soul benefit.

This alters the event's tactical grammar. The boss is harder and less directly lucrative, while the reward phase becomes spatially distributed. Teams must decide who clears the boss, who controls the pit, who can contest the crystals, and whether to split or protect the reward collectors. The game did not merely increase a number; it changed the sequence of post-boss decisions.

**Design application:** world events should be data-driven and versioned. A boss may change from one dropped reward to several scattered caches, forcing a different post-fight formation. A patch-like world modifier could alter the reward's spatial layout, defense requirements, or catch-up support without changing the entire combat system.

## A3. Translate Deadlock's match dynamics into a persistent tower-defense world

The game should borrow the decision principles, not the MOBA's exact six-player structure or its hero combat. The equivalent systems can be expressed in a city-builder's language:

| Deadlock concept | Proposed game equivalent |
|---|---|
| Lane wave | Enemy convoy/minion column on a route |
| Trooper wave push | Friendly militia, drones, or escort convoy advancing the frontline |
| Souls | Salvage, materials, data, energy, or secured supply crates |
| Contested airborne orb | Exposed reward that must be collected, carried, or defended |
| Neutral camp | Local hazard site, creature den, abandoned facility, or resource pocket |
| Soul Urn | Timed high-value convoy, relic, or regional delivery event |
| Unstable Rift | Temporary hazard/reward zone with a capture or containment window |
| Guardian/Walker | Outer and inner district defenses or enemy route anchors |
| Flex Slot | Permanent additional building/module capability |
| Mid-Boss | Regional siege organism, command engine, or dangerous neutral objective |
| Rejuvenator | Temporary reinforcement/respawn/repair reserve for the next push |
| Respawn timer | Time before an enemy raid group or rival faction can act again |
| Backdoor protection | Objective resistance unless a friendly escort/wave establishes proximity |
| Comeback souls | Adaptive recovery resources for a settlement that has fallen behind |

### A3.1 Dynamic action priorities

The game should compute candidate actions and their costs instead of executing a rigid script. A possible priority evaluation:

1. Prevent an imminent critical breach or protect a structure whose loss disconnects the world.
2. Respond to a scheduled event if its reward is valuable and the arrival window is feasible.
3. Convert a recent win into a nearby objective before the opponent recovers.
4. Secure a wave reward or resource packet before it expires or is stolen.
5. Push a route if it creates a safe period for construction, repair, or extraction.
6. Collect nearby neutral resources when doing so does not sacrifice a higher-value wave or event.
7. Recover, resupply, spend earned resources, and reassign roles.

The player may override this priority. The UI should display the expected opportunity cost: for example, “Leaving this route now risks one wave and exposes Gate B, but allows the convoy to reach the quarry before its 40-second departure.”

### A3.2 State-aware “pressure windows”

Rather than fixed rounds alone, define **pressure windows**. A pressure window opens when a force is defeated, an enemy structure is disabled, a wave is cleared, a boss is interrupted, or an event is captured. It has an estimated duration based on enemy travel time, reinforcement arrival, repair capacity, and active objectives.

```text
PressureWindow
  window_id
  world_id
  origin_event
  opened_at_tick
  estimated_close_tick
  enemy_recovery_estimate
  safe_routes[]
  exposed_objectives[]
  candidate_actions[]
  confidence
```

This system makes timing consequential without tying it to a single match clock. A player can see that the window is likely to last long enough to repair a wall or establish a forward claim, but not long enough to build a complete factory.

### A3.3 Catch-up that preserves strategy

Deadlock's comeback systems are a useful example of preventing an early lead from becoming automatically irreversible. The proposed game can provide catch-up resources without simply inflating every output:

- Lost districts may expose salvage caches.
- The two least-developed outposts may receive emergency supply contracts.
- A besieged settlement may gain a temporary evacuation or repair bonus.
- A weaker faction may receive migration or trade opportunities.
- A player behind in research may discover lower-tier technologies that remain useful.

Catch-up should be transparent and should not erase the consequences of bad decisions. It should create a new path to recovery, not secretly equalize every state.

## A4. Rust-inspired runs that found new homebases

The proposal should distinguish a **run** from the lifetime of a **world**.

A run is a bounded expedition, raid, survey, resource campaign, or settlement-founding operation. A world is the persistent container in which the homebase network, maps, factions, production, ecology, and history continue to exist. A run can conclude successfully or unsuccessfully; the world does not need to conclude.

### A4.1 Founding expedition loop

A homebase-founding run can have the following structure:

1. **Depart:** choose a transport, crew, equipment, construction kit, supplies, and a destination region from an existing homebase.
2. **Survey:** inspect candidate locations for resources, elevation, choke points, hazards, water/energy access, creature activity, and routes back to existing territory.
3. **Claim:** deploy a claim core or founding charter. This creates a new base identity and reserves a build/management area under the world's ownership rules.
4. **Establish:** build minimum shelter, storage, power, communications, and a defensive perimeter.
5. **Survive the founding window:** defend the site while local threats, creatures, weather, and hostile factions react to the new presence.
6. **Bootstrap:** establish at least one repeatable supply chain or local resource source.
7. **Connect or specialize:** connect a road, rail, power, data, or supply route to an existing base, or configure the location as a deliberately isolated outpost.
8. **Commit:** register the new base in the world's Homebase Registry and save its full state.
9. **Return or switch focus:** the player can leave it, revisit it, expand it later, or launch another founding run elsewhere.

The run should provide tension and meaningful risk, but it should not delete the homebase simply because the player exits the session. The run is an operation within a persistent world, not a wipe of the world.

### A4.2 Rust's Tool Cupboard as a claim-and-upkeep reference

Rust's Tool Cupboard provides a useful spatial ownership model. It establishes building privilege in a radius, tracks authorized players, and is tied to upkeep that prevents structural decay. Separate disconnected buildings generally require their own claim/upkeep object. Rust's 2026 updates also demonstrate that upkeep can vary with authorization/group size.

The proposed game can translate these ideas into a **Claim Core** or **Homebase Charter**:

- Establishes a base identity and the local construction/management radius.
- Defines who can build, dismantle, configure, or automate structures.
- Supports permission tiers for visitors, crew, faction allies, and automation agents.
- Maintains an upkeep buffer based on structural complexity, defensive systems, and logistics burden.
- Supports local boundaries, gates, and external defenses.
- Can be damaged, disabled, captured, or relocated under world rules.

Do not copy Rust's offline decay as the default. The user requirement is that worlds persist and can be resumed. A stored base must not be silently destroyed while the player is away. Upkeep should normally be evaluated in simulated world time only while the world is active, or through an explicitly configured bounded catch-up system. Any harsher decay/raid behavior should be an opt-in server/world ruleset.

### A4.3 Base instances versus blueprints

A **base instance** is a living object in a particular world. It contains current damage, inventory, workers, production queues, power, defenses, local topology, and relationships.

A **base blueprint** is a reusable design description. It may contain a construction layout, machine placements, routing plan, module loadout, and required materials, but it should not overwrite the state of an already founded base.

These must be separate data types. A player can save a blueprint from an existing base, found a new base from that blueprint, then modify the new instance independently.

### A4.4 Homebase Registry

```text
HomebaseRecord
  base_id
  world_id
  display_name
  region_id
  map_node_id
  claim_core_id
  founding_run_id
  founded_at_tick
  last_active_tick
  topology_revision
  structure_ids[]
  resident_and_crew_ids[]
  inventory_reference
  production_queue_reference
  power_network_reference
  logistics_network_reference
  defensive_state
  local_threat_state
  access_policy
  upkeep_buffer
  supply_connections[]
  activity_mode
  save_revision
```

A world can contain multiple homebases. The player may have one active focus at a time, but other bases remain in the registry and can be inspected, visited, or simulated at reduced fidelity.

### A4.5 Multiple bases should have different jobs

Avoid making every base a complete copy of the first. Give the player reasons to establish specialized settlements:

- Main homebase: command, storage, research, and central defense.
- Mining base: extraction and ore refinement.
- Rail junction: logistics, transfer, and repair.
- Forward fort: defensive staging and route control.
- Research station: rare materials, anomalies, and experiments.
- Agricultural/ecological reserve: food, biomass, and species management.
- Expedition camp: temporary deployment and remote recovery.

The player can still create general-purpose bases; specialization is a strategic incentive, not a mandatory class system.

## A5. Persistent worlds and resumable simulation

The world is the durable unit of continuity. A match/run/session is a temporary activity inside it.

### A5.1 World lifecycle

```text
World
  world_id
  world_seed
  schema_version
  content_version
  simulation_tick
  world_clock
  owner_and_access_policy
  map_graph
  homebase_registry
  faction_state
  ecological_state
  economy_state
  research_state
  active_run_registry
  event_history_cursor
  snapshot_revision
  save_status
```

The game should support several world records per account/profile. Each world should have its own independent save lifecycle, seed, settings, and history. Resuming a world should restore the exact strategic state, not merely reload the player's last camera position or an approximate base layout.

### A5.2 What must be persisted

At minimum, save:

- Map graph and map generation seeds.
- Terrain/topology revisions and player construction.
- Structure and entity identities.
- Homebase Registry and all base instances.
- Resources, inventories, reservations, and storage contents.
- Production queues and machine state.
- Power, transport, and automation configuration.
- Research, upgrades, modules, loot, and blueprints.
- Residents, factions, creature populations, and ecological aggregates.
- Active expedition/run state, objectives, route, and carried supplies.
- Wave director, objective timers, and world event schedule.
- RNG state or deterministic event seed state.
- Simulation tick, event sequence, save revision, and schema version.

Transient visual particles and reconstructable UI state need not be saved as authoritative world data.

### A5.3 Snapshot plus journal

A robust persistence model is a periodic full snapshot plus an ordered journal of state-changing events since that snapshot.

- **Snapshot:** a consistent serialized view of the authoritative world at tick N.
- **Journal:** ordered domain events after tick N.
- **Checkpoint:** a durable point that allows recovery without replaying the entire world history.
- **Migration:** explicit transformation from one schema/content version to another.

On resume, load the most recent valid snapshot, replay the ordered journal, verify the resulting revision, then reopen simulation. The journal should have bounded retention/compaction so a world that lives for years does not require replaying every historic event on every launch.

### A5.4 Offline behavior and catch-up

The default should be **pause-on-unload**: when the player closes a world, its world clock and active threats stop. This makes resumability reliable and prevents offline punishment from undermining a persistent sandbox.

A separate opt-in mode can allow bounded catch-up:

- Production can advance up to a configured duration.
- Storage overflow is recorded and visible.
- Power/fuel constraints continue to apply.
- Ecology can advance at regional aggregate level.
- No irreversible raid, base destruction, or player-character death should occur while the world is unloaded unless the player explicitly enabled that world rule.

For online shared worlds, offline behavior is a distinct server policy and should be configurable per world. It must not be conflated with local save semantics.

### A5.5 Infinite duration is not infinite simulation cost

A world can be conceptually endless while using bounded runtime resources:

- Active map: full entity simulation.
- Nearby map: lower-frequency updates.
- Distant region: aggregate populations, production, and threat statistics.
- Dormant region: stored state and deterministic re-instantiation.
- Unloaded world: paused by default.

World age and region count should not require every creature, projectile, item, or conveyor packet to remain a live runtime object.

## A6. Secure Shell as the platform boundary

The user has identified the repository [alacripose/secure-shell](https://github.com/alacripose/secure-shell) as the intended source of:

1. Backend server architecture.
2. Frontend game UI.
3. A package-streamer format.

This document treats those as user-supplied project requirements. The repository itself has **not yet been inspected**, so this section is an integration proposal, not a claim about Secure Shell's existing modules, protocols, package schemas, or implementation quality. A GitHub connection has been suggested so the repository can be examined directly once connected.

### A6.1 Boundary principle

Secure Shell should provide the platform foundation; the game should provide domain-specific simulation services. Do not duplicate the server, UI, or streaming foundations inside the game if Secure Shell already supplies them.

The game-specific backend should expose typed domain operations such as:

- Create/load/save world.
- Resume/pause world.
- Start/restore/resolve expedition run.
- Found/inspect/rename/archive homebase.
- Place/remove/upgrade structure.
- Assign unit or crew role.
- Configure tower, module, or automation.
- Query world/map/region state.
- Subscribe to world-state changes.
- Retrieve snapshots and replay recent event history.

The frontend should issue commands and render authoritative results. It should not independently decide combat outcomes, award loot, or commit resource transactions.

### A6.2 Suggested message categories for the package-streamer boundary

The actual Secure Shell streamer/package contract must be inspected before adopting field names or framing. The game should nevertheless define clear semantic message categories that can be mapped onto the existing format:

| Message category | Use |
|---|---|
| WorldManifest | World ID, schema/content version, world settings, save revision |
| WorldSnapshot | Consistent initial state for a new connection or resume |
| WorldDelta | State changes after a known sequence/tick |
| MapTopologyDelta | Terrain, path, structure, and connection changes |
| EntityDelta | Entity creation, update, and removal |
| EconomyDelta | Resource balances, reservations, production, storage |
| ObjectiveDelta | Waves, pressure windows, timed events, and goals |
| HomebaseRegistryDelta | New, modified, captured, archived, or visited bases |
| RunState | Expedition lifecycle, current stage, results, and carried inventory |
| PlayerCommand | A requested action from the client; subject to server validation |
| EventFeed | Human-readable and machine-readable world events |
| SimulationClock | Authoritative world tick and pause/resume state |
| SaveStatus | Checkpoint, durable-write confirmation, and recovery state |

For reconnect support, messages need an ordering model. The implementation may use the Secure Shell format's existing envelope, but the game-level semantics should include stable world ID, session or connection ID, ordered sequence/tick information, message type, schema version, and a payload. Chunking, compression, checksums, acknowledgements, backpressure, and resume tokens should be handled by the transport/package layer rather than reinvented ad hoc in each gameplay system.

### A6.3 Server-authoritative simulation

The backend should own:

- World clock and deterministic tick.
- Pathfinding and map topology revisions.
- Combat resolution.
- Resource creation, collection, and spending.
- Production, power, and logistics state.
- AI decisions and faction/ecology updates.
- Run and homebase founding transactions.
- Save/checkpoint state.

The frontend should own presentation concerns:

- Camera, zoom, layout, and visual interpolation.
- Input mapping and selection.
- Local panels, filters, tooltips, and visual overlays.
- Command composition and previews.
- Streaming presentation and user feedback.

The client may preview a building placement or route, but the server validates it against the authoritative map, permissions, resources, and topology revision before committing it.

### A6.4 Commands, events, and idempotency

Use a clear distinction:

- A **command** asks the world to do something.
- An **event** records something that the authoritative simulation accepted and did.
- A **snapshot** describes the resulting world state at a known revision.

Example:

```text
Command: FoundHomebase
  world_id
  expedition_run_id
  candidate_region_id
  claim_core_blueprint_id
  expected_world_revision
  idempotency_key

Events on success:
  ClaimSiteValidated
  ClaimCorePlaced
  HomebaseFounded
  HomebaseRegistered
  FoundingRunAdvanced
```

Use idempotency keys for commands that may be retried after a disconnected stream. A reconnect must not found the same base twice, spend the same resource packet twice, or create duplicate structures because an acknowledgement was lost.

### A6.5 Save consistency

A save should correspond to a consistent tick/revision boundary. Do not serialize half of a combat transaction and half of a resource transaction. The backend should either checkpoint a world after the tick commits or use a transactional write strategy that guarantees a consistent snapshot.

### A6.6 What must wait for repository inspection

The following cannot be specified accurately until Secure Shell's actual source and package format are reviewed:

- Existing server lifecycle and process model.
- Whether the stream is framed, chunked, message-oriented, or file-oriented.
- Package manifest and metadata schema.
- Frontend UI framework and component lifecycle.
- Serialization format and versioning conventions.
- Existing authentication/authorization boundaries.
- Whether multiplayer, remote administration, or multiple concurrent world sessions are already supported.
- How the game should register domain plugins or packages.

The integration goal is to adapt the game to Secure Shell's real contracts, not create a parallel architecture based on guesses.

## A7. Revised long-running campaign model

The game should distinguish at least four nested lifetimes:

| Layer | Lifetime | Example |
|---|---|---|
| World | Persistent; can remain open-ended | A named generated world with its map graph, factions, ecology, and base registry |
| Homebase | Persistent world object | Main city, mining base, rail junction, or frontier fort |
| Run/expedition | Bounded activity within a world | Surveying and founding a new base, retrieving a relic, clearing a siege site |
| Encounter/round | Short simulation segment | Enemy wave, defense event, boss encounter, convoy ambush |

A world can contain many completed and active runs. A run can end without ending the world. A new homebase can be founded from a run and stored as a separate first-class object. The player can then return to any base, launch another expedition, or continue the main defense.

This is a stronger fit for the requested open-ended design than treating “run” as a full roguelike reset. The roguelite structure can still exist within each run—temporary boons, loot, item synergies, and risky choices—but successful discoveries can be committed to the persistent world through blueprints, research, resources, and new settlements.

## A8. New prototype tests

| Prototype | Hypothesis | Required observation |
|---|---|---|
| Deadlock-style wave window | Clearing a wave creates useful time for a second action | Measure whether the player can predict the safe window and choose between repair, farm, rotate, or push |
| Objective trade | Two simultaneous opportunities create real prioritization | Compare decisions when the player cannot contest both events |
| Power-spike readiness | Unlock thresholds change when an expedition should begin | Track purchases/upgrades and whether timing matters more than total resource count |
| Multi-base founding | A successful run creates a useful permanent settlement | Verify that the new base is revisitable and has independent state |
| Base blueprint vs instance | A layout can be reused without overwriting a live base | Modify one instance and ensure the blueprint and other instances remain distinct |
| Claim-core permissions | Territory and access rules are legible | Test build authorization, automation permissions, and separate disconnected claims |
| Persistent resume | A world resumes at its last committed state | Close/reopen at multiple save boundaries; verify no duplicated rewards or lost events |
| Pause-on-unload | Offline time does not destroy a base | Compare state before unload and after resume |
| Bounded catch-up | Optional production simulation remains predictable | Verify overflow, energy limits, and resource reservations |
| Stream reconnect | Client can resume after losing the connection | Reconcile snapshot revision, event sequence, and idempotent command retry |
| Secure Shell boundary | Game logic can be hosted without duplicating platform UI/server facilities | Confirm actual repository APIs and package-streamer contracts before implementation |

## A9. Revised synthesis

The game is a persistent network of worlds and homebases, not one campaign that must eventually end. The player begins from a primary settlement and defends a central route, but can launch bounded expeditions into connected regions. Some expeditions return with resources or knowledge; others establish entirely new homebases. Each successful founding operation creates a distinct base instance in the world's registry, with its own terrain, structures, crews, inventory, production, defenses, and history.

Deadlock contributes the idea that the state between fights matters as much as the fight itself. Waves create time windows. Resource collection is not the same as resource security. A won engagement creates a temporary opportunity that must be converted into a structure, route, supply connection, or objective. High-level coaching emphasizes understanding the decision context, recognizing when to leave a lane, coordinating roles, and turning a lead into durable advantages. These concepts become the game's pressure-window and objective-conversion systems.

Rust contributes a base-claim and territory grammar: a central claim object, local build privileges, permissions, upkeep, and the tension of defending a physical settlement. The adaptation should not punish a player by silently decaying a world while it is closed. Instead, each world saves its authoritative state and resumes it when loaded, with optional and explicit catch-up rules.

Secure Shell is intended to provide the server architecture, UI foundation, and package-streamer format. The game should integrate through those real interfaces once the repository is inspectable. The backend remains authoritative for simulation and world persistence; the frontend presents a streamed view and submits validated commands. Each world has stable identity, snapshot/checkpoint state, ordered changes, and a resumable simulation clock.

The resulting game has no mandatory final round. It can continue as long as the player wants to develop it. The player may pause, leave, and later resume a world; establish a new homebase through a run; switch between stored settlements; and keep expanding the same persistent simulation.

---

## Addendum A — Sources and verification

### Deadlock rules and patch state

- Valve/official mechanics guide — lanes and troopers: https://www.deadlock.io/en/mechanics/lanes-and-troopers
- Valve/official mechanics guide — souls: https://www.deadlock.io/en/mechanics/souls
- Current Deadlock update, October 30, 2026 (patch record): https://deadlock.wiki/Latest_Update
- SteamDB mirror of the October 30, 2026 patch notes: https://steamdb.info/patchnotes/26333912/
- Deadlock Labs match timers and spawn reference: https://deadlocklabs.gg/tools/mechanics/
- Deadlock late-game strategy reference (updated October 7, 2026): https://deadlockhandbook.com/strategy/late-game/
- Deadlock team-fighting strategy reference: https://deadlockhandbook.com/strategy/team-fighting/
- Deadlock macro/rotation guide (October 2026 source and creator index): https://deadlock.vi.ki/macro-and-rotations
- Deadlock current macro guide, lane pressure and farm decisions: https://www.deadlockmeta.dev/en/guides/macro-and-rotations

### Coaches and player perspectives

- Deathy coaching profile and public reviews: https://coachlock.gg/coaches/deathy/
- Deathy's *How to Navigate Deadlock's Mid-Game — 2026* (February 14, 2026): https://www.youtube.com/watch?v=YwWdz1h97xc
- Deathy's *Deadlock for Beginners in 20 Minutes — Updated Late 2026* (October 8, 2026): https://www.youtube.com/watch?v=7L-j1mjASk0
- POSHYPOP coaching profile and public reviews: https://coachlock.gg/coaches/poshypop/
- POSHYPOP, *Deadlock Coaching Session on Midgame Macro as Pick* (October 20, 2025): https://www.youtube.com/watch?v=4JvPv78QYi0
- Midknight, *Everything I've Learned From 1 Year of Coaching in Deadlock* (September 8, 2025): https://www.youtube.com/watch?v=Tw_NEU1FY7E
- Midknight, *How to Play Spirit Carry | Deadlock Role Guide* (July 29, 2026): https://www.youtube.com/watch?v=ViQx1AXkn5s

### Rust settlement ownership and upkeep

- Facepunch official Rust Wiki — Tool Cupboard: https://wiki.facepunch.com/rust/The_Tool_Cupboard
- Facepunch official Rust update — Breach and Clear (September 3, 2026): https://rust.facepunch.com/news/breach-and-clear

### Secure Shell

- User-designated repository: https://github.com/alacripose/secure-shell
- Repository-specific architecture and package-streamer details remain pending direct source inspection. The design recommendations in Section A6 are explicitly provisional until the repository's real interfaces are reviewed.

### Version and attribution limitations

- Deadlock mechanics are patch-sensitive. Exact lane counts, wave intervals, neutral-event timers, rewards, respawn values, and objective behavior must be externalized into versioned data.
- Match averages are descriptive samples and must not be treated as guaranteed encounter timings.
- Public coaching profile biographies/reviews establish experience and coaching topics, but do not prove every individual strategy claim. Creator-video chapter metadata establishes subject and phase coverage, not a verbatim transcript.
- Rust is a survival game with rules designed around its own server persistence and decay model. The proposed game intentionally changes the default offline behavior to preserve resumable worlds.
- Secure Shell's intended role is supplied by the user; no claim is made here about uninspected source code or actual protocol behavior.

---

# 17. Secure Shell integration: repository-grounded architecture

**Repository inspected:** `alacripose/secure-shell`  
**Branch inspected:** `main`  
**Inspection snapshot:** October 31, 2026  
**Purpose:** Map the proposed game onto the actual Secure Shell architecture and identify what can be reused, what is not implemented yet, and what needs a new user-owned design decision.

This section is based on source and documentation reads from the private repository, not assumptions inferred from the project name. The analysis is intentionally read-only: no repository files, branches, issues, or commits were changed as part of this inspection.

## 17.1 What Secure Shell actually is

Secure Shell is a desktop-class shell with a Debian-hosted backend, a forked Openlock window manager and taskbar, a package stream, and a project-activity ledger. Its architecture separates the backend's responsibilities from the client's responsibilities:

| Responsibility | Current owner in Secure Shell | What that means for the game |
|---|---|---|
| Package admission and mounted-package records | Debian backend | The game should arrive through the established package path, not mount itself |
| Project activity ledger | Backend | Records important development, package, build, test, and mount events |
| Activity watcher and event publication | Backend | Changes can be announced to connected clients |
| Shell windows, frames, focus, taskbar, toasts | Forked Openlock client | The game must consume the existing display rather than implement another desktop |
| Window content | Mounted package/content | Game views belong inside the shell's content area |
| Security verdicts and trust decisions | Enclave/backend side | The client displays the verdict; it must not invent or self-issue one |
| Tests that count as shell evidence | Actual client surface | A Node-only or browser-only test cannot prove the real client experience |

This is a valuable foundation for the game's backend services and UI integration, but it is not yet a game engine. I found no implemented world simulation, tactical renderer, creature simulation, factory simulation, world save database, or homebase registry in the inspected Secure Shell paths. Those remain game-domain work.

The repository README explicitly describes the client test surface and toast stack as not yet built. The architecture document lists the client display fork, axis lane, client test surface, and live toast path as subsequent work. Therefore, the repository contains substantial backend infrastructure and a stream adapter, but a complete, witnessed desktop client path should not yet be claimed.

Sources: [README.md](https://github.com/alacripose/secure-shell/blob/main/README.md), [AGENTS.md](https://github.com/alacripose/secure-shell/blob/main/AGENTS.md), [docs/ARCHITECTURE.md](https://github.com/alacripose/secure-shell/blob/main/docs/ARCHITECTURE.md).

## 17.2 Three separate planes: packages, game state, and activity

The integration should explicitly separate three kinds of data. Treating all three as one stream would create unnecessary coupling and make both persistence and client delivery harder to reason about.

### Plane A — package and presentation control

This is the existing Secure Shell package and display path. It answers questions such as:

- Which package has been admitted?
- Which mounted surface is being presented?
- Which client windows and content tree are available?
- Which package wishes were loaded, missing, refused, or differ from the plan?
- Which display notices should be shown?

This is where a game UI package, launcher integration, game content view, theme-compatible controls, and package lifecycle information belong.

### Plane B — persistent game simulation

This is new game-domain state. It answers questions such as:

- Which world is this?
- What is its current simulation tick and revision?
- Which maps, districts, tracks, structures, creatures, factions, factories, and logistics networks exist?
- Which homebases have been founded, and what is their state?
- Which expedition is active or completed?
- What resources and unlocks exist in each world and base?
- What state should be restored when the user resumes?

The simulation plane needs snapshots, deterministic or explicitly specified state transitions, and durable save/recovery. It should not be written as thousands of project-ledger entries per second.

### Plane C — project and user-visible activity

This is what the Secure Shell project ledger and its event feed already do well. It answers questions such as:

- A game package was built, admitted, mounted, or updated.
- A world was created, imported, exported, or deleted.
- A homebase was founded, renamed, transferred, or archived.
- An expedition completed and produced a durable result.
- A save snapshot was committed or a recovery was refused.
- A test, build, or integrity check passed or failed.

This plane is suitable for noteworthy, auditable events and notifications. It is not the authoritative storage for every moving enemy, projectile, resident, or conveyor item.

**Design rule:** the world store owns the world; the package stream owns delivery of the UI package; the Secure Shell ledger owns the audit trail and activity notifications. They may reference one another by stable IDs and digests, but must not become competing copies of the same fact.

## 17.3 The package formats and their actual boundaries

The repository has several distinct formats with different jobs. They must not be conflated.

| Format or mechanism | Current purpose | Game integration rule |
|---|---|---|
| `dl-enclave.surface/1` | Declarative shell surface and window/content declaration | Use the existing declaration and mount lineage for shell content |
| `surface.mount/2` | Append-only mount receipt for a receiver | Reuse for mount acknowledgement; do not turn it into the world save format |
| `secure-shell.ingest/1` | Upstream package admission frame | It terminates at the backend; the client must never parse it |
| `secure-shell.admission/1` | Backend admission result | Treat as server-side admission evidence, not live simulation state |
| `secure-shell.served/1` | Server-to-client served presentation record | This is the served shell payload, not the save-game schema |
| `secure-shell.ledger/1` | Project activity/audit record | Use for coarse important events and test/build/mount evidence, not every sim tick |
| `dl-enclave.sig/1` | Signed checkpoint format for a ledger head | A checkpoint proves the signed head bytes, not that the underlying game world is fun, valid, or truthful |

### `secure-shell.ingest/1`

The implementation is in `server/ingest.mjs`, with the shared declaration grammar in `server/decl.mjs` and the builder/acceptor path represented by `tools/ingest-cli.mjs`.

The inspected implementation declares:

- Magic: `SHINGEST`.
- Frame schema: `1`.
- Domain: `secure-shell.ingest`.
- Kinds: `decl` and `code`.
- Maximum payload: 1 MiB.
- Digest preimage includes the domain, schema, kind, and payload.
- The declaration parser is shared by the builder and the acceptor.
- Older and newer frame schemas are refused by name.
- The `code` kind is currently reserved and refused with `frame-kind-reserved` until a reader exists.

The declaration grammar names a package and title and includes windows, claims, witnesses, a presenter, denials, and optional migration declarations. Windows use lanes and rules from the existing surface table. These are useful package-governance and presentation contracts, but they do not define a full game world or a simulation snapshot.

**Important:** the presence of the word `game` in another schema's wish vocabulary does not mean the package door accepts a game binary. The `game` wish kind is a plan/status row, not an executable game-package reader. Similarly, a reserved `code` kind is not permission to stream executable content.

Sources: [server/ingest.mjs](https://github.com/alacripose/secure-shell/blob/main/server/ingest.mjs), [server/decl.mjs](https://github.com/alacripose/secure-shell/blob/main/server/decl.mjs), [server/test/ingest.test.mjs](https://github.com/alacripose/secure-shell/blob/main/server/test/ingest.test.mjs), [docs/DECISIONS.md](https://github.com/alacripose/secure-shell/blob/main/docs/DECISIONS.md).

### `surface.mount/2`

`tools/port/store.mjs` records a receiver's acknowledgement of a mounted surface. A row carries the surface ID, sequence, digest, patch/package references, receiver, state, window count, refusal, time, and receipt ID. Its states distinguish applied results from refused, stale, and conflicting pushes. The store also provides an applied row and a high-water floor for a receiver.

This is the right place to prove that a shell surface arrived at a receiver. It is not the right place to store homebase inventory, enemy wave history, or factory production state. The mount receipt store and the project ledger are separate mechanisms with different invariants.

Source: [tools/port/store.mjs](https://github.com/alacripose/secure-shell/blob/main/tools/port/store.mjs).

### `secure-shell.served/1`

`tools/port/served.mjs` builds a client payload from a mounted surface and associated evidence. The inspected payload includes:

- Format identifier.
- Wishes and notices.
- Surface ID, sequence, digest, and timestamp.
- Window records and content nodes.
- Requirement names and evidence digests.

It derives a tier from named requirements and evidence. It refuses a carried-in tier (`tier-not-derived`) because a client should not accept a conclusion it cannot reproduce from evidence. It also refuses missing requirements/evidence, missing surfaces, or empty window sets.

This schema is a presentation and evidence envelope. It should not become the high-frequency transport for all world simulation state. A moving enemy's position is not a shell window declaration; a factory's item throughput is not a package wish; a world snapshot is not a served toast.

Source: [tools/port/served.mjs](https://github.com/alacripose/secure-shell/blob/main/tools/port/served.mjs).

## 17.4 A concrete stream/display integration issue found in source

There is a specific static contract mismatch worth resolving before relying on the current end-to-end path.

1. `server/shell.mjs` mounts `packages/toast/toast.surface` and calls `served(...)` with the mounted surface's windows and nodes.
2. `served.mjs` serializes the mounted windows into the `windows` array of `secure-shell.served/1`.
3. The current client adapter, `shell/openlock/panorama/scripts/secure_shell_stream.js`, parses the served payload and explicitly refuses when `payload.windows` is non-empty, returning `stream-windows-open` because the mounted-set-to-display-window mapping is still an open design layer.
4. The current `applyServed` path applies wishes; it does not map the mounted window record into the Openlock display's window record.

That means the source currently contains both a server path that produces a window-bearing served record and a client adapter that refuses non-empty window records. This is a **static integration mismatch**, not a claim that I ran the client and observed a live failure. The repository documentation itself says the client witness is not yet complete.

The right fix is not to silently drop `windows`, and not to make a second renderer. The next step is to resolve the existing `Display contract` layer in `AGENTS.md` §2.1 / `docs/SELECTIONS.md`, record the user's chosen mapping in `docs/DECISIONS.md`, and add a projection test proving that every mounted window maps to exactly one display window with the same identity, title, and geometry—or is refused by ID. Until that is resolved, claims that the stream fully delivers game windows should remain **unwitnessed**.

Sources: [server/shell.mjs](https://github.com/alacripose/secure-shell/blob/main/server/shell.mjs), [tools/port/served.mjs](https://github.com/alacripose/secure-shell/blob/main/tools/port/served.mjs), [secure_shell_stream.js](https://github.com/alacripose/secure-shell/blob/main/shell/openlock/panorama/scripts/secure_shell_stream.js), [docs/SELECTIONS.md](https://github.com/alacripose/secure-shell/blob/main/docs/SELECTIONS.md).

## 17.5 What the existing server provides—and what it does not

The current backend contains a substantial project-activity service:

- `server/ledger.mjs`: canonical JSON, SHA-256 hash chain, contiguous sequence, idempotency by operation ID, expected-sequence validation, fsynced append, tamper verification, and Ed25519 checkpoints.
- `server/watch.mjs`: digest-based project file scan, with a default two-second poll interval, emitting one typed entry per changed path.
- `server/http.mjs`: authenticated health/ledger/checkpoint/append routes and an SSE event feed.
- `server/shell.mjs`: a composition layer that mounts a declared surface and constructs a served payload.
- `server/test/`: backend checks for ledger, watcher, HTTP, ingest, and served/stream behavior.

The documented HTTP routes include:

| Route | Current function | Proposed game use |
|---|---|---|
| `GET /api/health` | Liveness, project identity, current head | Shell/backend status only |
| `GET /api/ledger/head` | Current activity sequence/hash | Activity cursor, not world tick |
| `GET /api/ledger/verify` | Verify activity chain | Audit/integrity view |
| `GET /api/ledger/events?after=N` | JSON page or SSE event feed | Toasts and notable events, not full combat state |
| `GET /api/ledger/checkpoint` | Retrieve signed checkpoint | Audit history view |
| `POST /api/ledger/checkpoint` | Sign current ledger head | Administrative/audit operation |
| `POST /api/ledger/entries` | Append typed activity entry | Authorized coarse-grained world lifecycle records, only if the ledger's project/actor semantics are deliberately extended |
| `GET /api/shell/served` | Get served shell package/presentation payload | Shell surface delivery |

The README and architecture docs describe the first slice as one project and one writer token, loopback by default, with TLS termination required before remote access and multi-user scopes not yet implemented. The HTTP source currently exposes a token-based boundary; it is not a full account/role/party/world authorization model.

For a single-player local prototype, this can be sufficient to establish the first service boundary. For multiplayer, shared homebases, player-owned worlds, or a public hosted world, it is not yet enough. The game needs world-scoped authorization, actor identity, permissions, ownership/transfer semantics, rate limits, and a clear remote transport policy. Do not mistake a bearer token for a complete player identity system.

Sources: [server/ledger.mjs](https://github.com/alacripose/secure-shell/blob/main/server/ledger.mjs), [server/http.mjs](https://github.com/alacripose/secure-shell/blob/main/server/http.mjs), [server/watch.mjs](https://github.com/alacripose/secure-shell/blob/main/server/watch.mjs), [docs/LEDGER.md](https://github.com/alacripose/secure-shell/blob/main/docs/LEDGER.md).

## 17.6 Game-world persistence must be a separate domain contract

The user requirement is explicit: the game does not necessarily end, and the player must be able to resume a world in the state where it was left. Each world must therefore have a stable identity and durable independent state.

Do not store each simulation tick as a project-ledger entry. The ledger's schema is intentionally bounded to project activity and its detail field is limited. The world has a different write frequency, state shape, recovery model, and authority boundary.

The recommended model is:

- **World catalog:** discovers and indexes saved worlds.
- **Per-world state:** owns the actual current simulation state.
- **Per-world event journal:** records accepted domain mutations and deterministic world events between snapshots.
- **Periodic snapshots:** allow bounded replay and fast restore.
- **Secure Shell activity ledger:** records selected lifecycle operations and proof that a save, import, export, migration, or validation occurred.
- **Served shell stream:** tells the UI what to display and which important notices to show; it does not contain the complete live world.

A world must be resumable without depending on the lifetime of a shell window, a browser tab, a process's memory, or a temporary client connection.

### Proposed world identity and save metadata

```text
WorldRecord
  world_id                 stable, immutable identifier
  display_name             mutable player-facing name
  world_schema              explicit domain save schema
  generator_id              world-generation ruleset identity
  world_seed                deterministic generation seed
  created_at                creation time
  last_saved_at             last durable save time
  simulation_tick           deterministic simulation clock
  state_revision            committed domain revision
  snapshot_id               last verified snapshot
  snapshot_digest           digest of snapshot bytes
  event_head                last journal event/revision
  run_registry[]            run IDs associated with this world
  homebase_registry[]       base IDs within this world
  resume_policy             paused-on-unload by default
  current_focus              last viewed map/base, not an authority field
```

The simulation tick and state revision are deliberately separate. A tick measures game progression; a revision measures committed state changes. Several commands can be committed during one tick, and a tick can advance without a structural change. Neither should be substituted for the Secure Shell project-ledger sequence.

### Proposed snapshot and journal contract

```text
WorldSnapshot
  world_id
  world_schema
  state_revision
  simulation_tick
  parent_snapshot_id
  generator_id
  state_blob_ref
  state_digest
  created_at

WorldEvent
  world_id
  event_seq
  simulation_tick
  operation_id
  actor_id
  command_kind
  subject_id
  expected_revision
  outcome
  payload_ref_or_bounded_payload
  previous_event_digest
  event_digest
```

The snapshot is a complete recoverable state at one committed revision. The journal contains subsequent accepted state transitions. Recovery loads the last verified snapshot, validates its digest and schema, then replays later events in order. If a journal record is malformed, stale, or inconsistent with the expected revision, recovery should stop at a named boundary rather than silently skip it.

Use stable IDs for world, region, map, entity, run, base, structure, and inventory records. Do not identify world objects by their current array index or screen position.

### Save lifecycle

1. A user command arrives with a world ID, actor, operation ID, and expected state revision.
2. The server validates ownership/permission, schema, available resources, target existence, and preconditions.
3. The authoritative simulation applies the command or refuses it by code, subject, and reason.
4. The committed result advances the world revision and enters the per-world journal.
5. A save policy periodically writes a snapshot from a consistent committed revision.
6. The snapshot is written to a temporary object, validated, flushed, and atomically promoted to the catalog's current snapshot reference.
7. On resume, the server validates the snapshot and replays journal entries after its revision.
8. Secure Shell records a coarse-grained `record`, `milestone`, `build`, or other approved activity event for notable lifecycle operations—not one ledger event per tick.

The exact file/database format remains a user-owned decision. No implementation should add a second package format or repurpose `secure-shell.ledger/1` to bypass that decision.

### Pause and offline progression

Default behavior should be **pause-on-unload**. When the player leaves a world, its simulation tick stops. This directly supports the user's requirement that worlds can be resumed without the world silently advancing or being destroyed while the player is away.

Optional world modes can be considered later:

- Paused single-player world.
- Explicit offline progression, using bounded aggregate updates rather than simulating every entity.
- Hosted shared world, where server time continues under a player-visible policy.

These are distinct rules and must not be mixed implicitly. Rust-style base vulnerability does not require the game to punish players for being offline. A hosted PvP mode, if one is ever chosen, can have explicit raid windows, upkeep, or offline vulnerability, but it should not be the default persistence policy.

## 17.7 Runs that found new homebases: Rust-inspired progression

The Rust reference should be used for the feeling of establishing a physical foothold in a persistent world: claim a site, place a core, create building privilege, establish protected storage, construct barriers, set up production, and defend the first vulnerable period.

It should not imply Rust's wipe cadence or offline decay as mandatory features. The user specifically wants saved worlds that can be resumed; the proposed game should preserve founded bases unless the player deliberately abandons them or an explicit game mode allows permanent loss.

### Separate the concepts

- **Run:** a bounded expedition or challenge with a launch point, objective set, temporary run state, and durable result.
- **Founding action:** an outcome of a run that establishes a new homebase at a discovered or secured site.
- **Homebase instance:** a persistent settlement with its own inventory, structures, residents, production, defenses, and state revision.
- **Blueprint:** a reusable design/template. It is not the same thing as a live base.
- **World:** the persistent container that owns a map graph, regions, resources, factions, and one or more homebases.

A single world can contain many homebases. The player can also maintain several separate worlds in the world catalog. This preserves the difference between founding a new base inside a world and starting a wholly new world.

### Proposed run-to-homebase sequence

1. The player selects an origin homebase and launches an expedition.
2. The run establishes a temporary forward context: discovered map, route, supplies, temporary equipment, party/units, and objective state.
3. The player explores and secures a candidate site.
4. A founding condition is satisfied: enough construction material, a deployable core/claim device, suitable terrain, and a defensible perimeter or a deliberate risk acceptance.
5. The run commits `HomebaseFounded` with a stable new base ID and a reference to the founding run.
6. The new base enters `founding` state with its own starting inventory, structures, territory/claim footprint, threat level, and construction capacity.
7. The player can return to the origin base, continue building the new base, or begin another expedition.
8. The base remains registered and resumable after leaving the map or closing the game.

The run result should be a durable result object, not a narrative-only pop-up. A failed founding attempt can still retain discoveries, mapped routes, salvage, and partial construction if the game rules say those outcomes survive.

### Proposed base state model

```text
Homebase
  base_id
  world_id
  founding_run_id
  site_id
  region_id
  map_id
  status                  founding | operational | besieged | breached | abandoned
  claim_core_id
  claim_footprint
  owner_or_faction_id
  base_revision
  inventory_ref
  structures_ref
  residents_ref
  production_ref
  defense_ref
  logistics_links[]
  blueprint_ref
  founded_at_tick
  last_active_tick
```

Each base owns its own inventory by default. Resources should not teleport between bases merely because they share one world. Moving resources requires a transport link, convoy, train, portal, or a specifically unlocked logistics capability. This makes remote bases meaningful and gives the player reasons to build roads, rails, freight routes, and defensive corridors.

### Rust-inspired territorial mechanics

| Rust concept | Adaptation | Design caveat |
|---|---|---|
| Tool Cupboard / building privilege | Claim core that authorizes construction and repair in a footprint | The core is a world object with a visible authority radius, not an opaque ownership flag |
| Upkeep | Optional maintenance input for structures and claim coverage | Do not silently decay an offline single-player base |
| Foundations and building tiers | Structural classes with load, durability, cost, and upgrade requirements | Must remain compatible with the game's voxel/grid/terrain rules |
| External building / raid risk | Enemies can attack claim infrastructure, depots, and perimeter routes | Communicate attack priorities and consequences clearly |
| Storage and loot protection | Physical, destructible, or protected storage with local access rules | Do not make every loss permanent by default |
| Blueprint / construction planning | Save and reuse base layouts | Separate blueprint data from live inventory, damage, and entity state |

The central Rust-like lesson is **base identity and spatial commitment**. The player has to choose where to establish a foothold, how much territory to control, how to defend access, and how to connect the new site to the larger network.

## 17.8 Many bases, one world: networked settlement strategy

Once a world can contain multiple homebases, the city-builder becomes a settlement network rather than one central city with distant disposable outposts.

A base can specialize:

- Main homebase: central defensive command, high-capacity storage, research.
- Mining base: extraction and raw material processing.
- Railhead: transport interchange and freight storage.
- Frontier fortress: defense and staging for expeditions.
- Agricultural base: food, biomass, biological processing.
- Research outpost: rare samples, laboratories, observation facilities.
- Ecological reserve: protected habitat, creature study, renewable resources.
- Industrial base: advanced machinery, heavy artillery, ammunition, and components.

Bases should exchange goods through explicit network edges. Each edge has throughput, travel time, capacity, ownership, route vulnerability, and possibly power/communications dependencies.

```text
Homebase A (central city)
  -> freight route
  -> Homebase B (mine)
  -> rail interchange
  -> Homebase C (industrial works)
  -> protected convoy
  -> Homebase D (frontier fortress)
```

The player can save a base blueprint, found a new base from a run, and then choose whether it remains independent or joins a larger supply/defense network. A disconnected base should continue to have local production and local needs; it should not depend on a global inventory pool.

The world map should support a base registry and a clear base-selection UI. The player should be able to inspect:

- Current status and last saved revision.
- Threat and nearby enemy pressure.
- Local production and stock levels.
- Route connectivity.
- Base privilege/claim footprint.
- Damaged or disabled infrastructure.
- Which expedition founded it.
- Which blueprint it was based on.
- Whether it is currently loaded in full simulation or represented by a regional aggregate.

## 17.9 Deadlock's changing match state as a simulation reference

The game should borrow Deadlock's dynamic macro structure, not a fixed list of minute marks. The match changes because lane waves, resources, power spikes, map access, neutral objectives, team positions, and defensive structures change relative to one another.

The useful abstraction is a **state of opportunity and pressure**. A wave creates a collection window and pushes the frontline. A cleared wave can enable a rotation or objective; a player who chases a low-value fight may miss the next wave or camp timing. A structure changes what space is safe. A neutral objective can turn local control into a larger team advantage. The next decision depends on the current state, not simply the timer.

### Coach viewpoints as separate evidence streams

The research tracks the three requested creators separately:

- **Deathy:** coaching/VOD review material is useful for identifying the player's underlying decision error—missed farm, bad trade, mistimed rotation, weak objective conversion, or poor awareness—rather than only prescribing a build or repeating a route.
- **POSHYPOP:** high-level competitive/team-coaching material is useful for coordinated rotations, timing team commitments, balancing lane pressure against grouping, and converting an advantage into an objective rather than merely recording a kill.
- **Midknight:** long-form coaching breakdowns provide a useful structure for separating laning errors from mid/late-game macro and general decision rules. Older coaching videos should be used for durable reasoning patterns, not patch-specific current timings.

These are not three fixed AI policies. The game should model the decisions and the state variables behind them. Current official mechanics and patch notes remain the authority for mechanics that can change; coaches are evidence for how experienced players interpret those mechanics.

### Match state as a vector, not a phase enum

```text
MatchState
  elapsed_tick
  team_economy[]
  unspent_resources[]
  player_power_profiles[]
  lane_wave_positions[]
  lane_pressure[]
  structure_health_and_control[]
  neutral_objective_state[]
  camp_and_resource_respawn_state[]
  team_positions[]
  vision_or_information_state[]
  respawn_and_reinforcement_state[]
  available_rotations[]
  current_power_spikes[]
```

A phase label such as `opening`, `midgame`, or `late-game siege` can be computed for UI explanation, but it should not be the authoritative logic. A game may enter a midgame-like state early because a structure falls or a large economy lead develops; a stalled game may remain in a lane-control state much later.

### Functional phases and their state transitions

| Functional state | Typical conditions | Important decisions | Proposed game translation |
|---|---|---|---|
| Lane establishment | Initial waves and resource routes are active; players have limited equipment and incomplete map information | Secure resources, manage wave position, avoid losing farm to low-value fights, trade when the exchange supports the next collection cycle | Early defense and scouting: protect the main track while investing in the first production chain |
| First power windows | A player/unit gains a key upgrade, resource threshold, or ability; nearby structure/camp/objective is contestable | Convert the spike into pressure, a camp, a structure, or an advantageous rotation | Use new tower modules, artillery, or units to secure an outpost or resource site before the opportunity closes |
| Map opening | A lane/structure falls or wave control creates safe travel space | Rotate, invade a resource area, reinforce another route, or pressure an objective | The player can deploy a convoy, claim a district, open a new map connection, or attack enemy logistics |
| Midgame contest | Several routes and objectives are live; resource advantages and map position interact | Split pressure vs grouping, defend a valuable route, contest neutral resources, avoid over-rotating | Multiple fronts are active; a homebase must choose where to commit defenders, power, and construction capacity |
| Objective conversion | A team secures a major neutral/objective advantage | Convert the advantage into structures, resource control, territory, or a decisive push | A successful boss/siege/expedition unlocks a new base location, regional access, or a durable industrial advantage |
| Late-game siege | Equipment and economy are mature; the cost of losing a coordinated fight is high; objectives and structures determine access | Manage waves, prevent base exposure, choose when to force or avoid a fight, coordinate an end-state push | Defend a network of homebases, protect critical routes, prepare a coordinated siege, and keep fallback infrastructure available |

### Map objectives are opportunities with costs

A neutral objective should not be a button that gives an unconditional buff. It should create a timing and commitment problem:

- What lane pressure must be maintained while the objective is contested?
- Which units or players must leave their current route?
- What resource or structure will be exposed while they rotate?
- What is the payoff if the objective is secured?
- What happens if the enemy arrives first?
- Can a smaller force stall while another force secures a different advantage?

The game can use this structure for neutral creature nests, industrial control points, energy wells, boss arenas, rare deposits, and transport junctions.

### Patch-aware objective design

Deadlock's October 30, 2026 update changed the Mid-Boss/rejuvenator interaction: the objective's reward can be split across separately contestable crystals, while the boss itself and its bounty/catch-up economy were adjusted. The design lesson is not to copy that specific reward. It is that a major objective can become a series of contestable sub-rewards instead of one all-or-nothing pickup. A fortress siege could similarly expose multiple salvage cores, power cells, or command relays that can be captured independently.

**Patch-specific caution:** exact lane count, trooper behavior, neutral timings, souls rules, structure rules, and objective rewards must be versioned in the game's Deadlock research notes. Do not freeze old coaching advice as a timeless mechanic.

## 17.10 Event timing, state transitions, and the game's tick model

A Deadlock-like decision system should be implemented around events and conditions:

```text
OpportunityCreated
  -> OpportunityObserved
  -> CommitmentChosen
  -> ResourcesAndUnitsReserved
  -> RouteOrObjectiveContested
  -> ResultResolved
  -> RewardOrLossCommitted
  -> MapStateRecomputed
```

An opportunity can expire, be contested, be secured, be lost, or be converted into another opportunity. A player may intentionally decline it. The game should explain why: not enough defenders, insufficient supplies, a more valuable wave approaching, transport time too long, or the expected reward too small.

The simulation tick remains separate from the shell activity sequence. The server can advance world ticks in batches when appropriate, but each authoritative mutation is committed against a world revision. Deterministic replay should be possible from a snapshot and recorded commands/events, subject to the game's chosen randomness and simulation determinism policy.

## 17.11 Client architecture: Secure Shell should host the game, not become the game engine

The Openlock fork is the selected owner of window chrome, focus, taskbar, launcher, and toasts. The game should not create a second desktop or its own floating-window system inside a shell window.

A good first game surface is one substantial content window with an internal view/router. Inside that content area the game can switch between:

- World selection and resume.
- Strategic world map.
- Homebase overview.
- District/city construction.
- Main-track tactical defense.
- Expeditions and founding runs.
- Production/logistics inspection.
- Research and run rewards.
- Creature/ecology observation.

These are game views, not automatically separate Secure Shell windows. The player should not be overwhelmed with a dozen empty panes. Additional shell windows should be added one at a time only when a real interaction and its evidence are ready, following the repository's own rule.

### Separate shell UI from simulation rendering

The shell can own menus, status, inventory panels, map controls, logs, configuration, world selection, and notification surfaces. The actual tactical and world renderer still needs a deliberate technology/embedding decision. The inspected repository includes a Panorama-specific adapter under `shell/openlock/panorama/scripts/`, but that fact alone does not prove a complete 3D/voxel renderer or a cross-platform game rendering host.

The game renderer should expose a bounded interface to the shell content view:

- Current world/base/map identity.
- Current simulation tick and world revision.
- Selected entity and current tool/action mode.
- Commands the player is allowed to submit.
- Renderable snapshot/delta references.
- Explicit interaction errors/refusals.
- A way to show the corresponding proof/log entry for notable actions.

The shell should not directly mutate world state by writing package files or ledger rows. It sends a typed game command to the game backend; the backend validates and commits; the shell renders the result and any relevant notice.

## 17.12 Proposed backend service boundaries

The following is a proposal for game-specific services that can sit beside Secure Shell's existing components. It is not a statement that these services already exist.

| Service | Owns | Must not own |
|---|---|---|
| `WorldCatalog` | World IDs, display names, save locations, last verified revision, resume listing | Full simulation logic |
| `WorldHost` | Active world sessions, tick scheduling, snapshot/replay coordination | Shell window state |
| `WorldCommandService` | Typed mutations, validation, idempotency, expected-revision checks | Package admission |
| `WorldStateStore` | Per-world snapshots and domain journal | Project activity ledger schema |
| `RunService` | Expedition launch, temporary run state, outcome, founding eligibility | Persistent package mount state |
| `HomebaseRegistry` | Base identity, site, status, blueprint link, ownership/permissions | Global item teleportation |
| `SimulationSystems` | Combat, pathing, production, logistics, ecology, factions | User-facing desktop chrome |
| `GameStream` | World snapshot/delta delivery and reconnect from world revision | Secure Shell package format |
| `ShellIntegration` | Served content view, package wishes, notices, launch/resume commands | Authoritative simulation mutation without a backend command |

A minimal first version can combine some of these modules in one process, but their ownership boundaries should remain explicit. Splitting them into microservices before the game loop exists would add deployment complexity without proving any gameplay value.

### World command envelope (proposal)

```json
{
  "world_id": "world-...",
  "operation_id": "op:...",
  "expected_revision": 42,
  "actor_id": "player:...",
  "command": {
    "kind": "found_homebase",
    "run_id": "run-...",
    "site_id": "site-...",
    "starting_blueprint_id": "blueprint-..."
  }
}
```

The backend returns either a committed result with the new world revision and event reference, or a named refusal with the subject and reason. Retrying the same operation ID must not found two bases. A stale revision must be rejected rather than silently overwriting a newer base or world state.

This resembles Secure Shell's typed command boundary at the design level, but the game command contract remains a separate domain schema. It should not be added to `secure-shell.ledger/1` as new arbitrary fields.

## 17.13 Notifications: coarse events to the shell, high-volume state to the game

The existing SSE ledger feed is appropriate for human-visible activity:

- World saved.
- World restored.
- Expedition completed.
- New homebase founded.
- Base breached or recovered.
- Major objective secured.
- Research unlocked.
- Package updated or refused.
- Integrity check failed.

It is not appropriate for:

- Every minion coordinate update.
- Every projectile position.
- Every creature perception check.
- Every item moving one belt segment.
- Every building production tick.

Those should use the game's own snapshot/delta or state replication mechanism. That mechanism can be designed to support reconnect by world revision and bounded delta batches. It must apply backpressure and fall back to a fresh snapshot if a client has fallen too far behind.

A single noteworthy event may be represented in both systems, but the roles differ: the world journal is authoritative game history; the Secure Shell ledger records the user-visible project/activity fact and references the world's event or snapshot digest. There should not be two independent event sources that can disagree about whether a homebase exists.

## 17.14 Security and multiplayer implications

The current Secure Shell boundary is bearer-token-based, loopback-first, and single-project/single-writer in the documented first slice. It is a strong local capability boundary, not a finished public game account system.

Before public multiplayer or remote hosting, the game needs explicit answers for:

- Who owns a world?
- Who may join it?
- Who may issue building, inventory, trade, and combat commands?
- Can a homebase be transferred or shared?
- What authority is needed to found a new base?
- Can visitors modify terrain or only interact with public surfaces?
- How are actions rate-limited?
- How are disconnections and reconnects handled?
- What does the server do when a player submits an old world revision?
- How are package integrity and game-world authority kept separate?

A shell token should not automatically become a game player's identity. A valid package digest does not prove the user's authority to claim a site. A valid ledger signature does not prove the world simulation was balanced or that a player had permission to execute a command. Each fact needs its own authority and witness.

The shell's own rules say it renders security verdicts and does not issue them. The game client should follow the same pattern: the server or enclave issues the verdict; the client displays the outcome and reason.

## 17.15 Current-state audit: confirmed, open, and not present

| Area | Repository evidence | Status for game integration |
|---|---|---|
| Debian-hosted Node 22 backend | `server/ledger.mjs`, `server/http.mjs`, README | Present as project infrastructure |
| Project ledger with idempotency and fsync | `server/ledger.mjs`, docs/LEDGER.md | Present; use for audit/activity, not world tick storage |
| Digest watcher and SSE notices | `server/watch.mjs`, `server/http.mjs` | Present as project activity service |
| Package admission frame | `server/ingest.mjs` | Present for declarations; `code` kind still refused |
| Surface mount receipt | `tools/port/mount.mjs`, `tools/port/store.mjs` | Present as mount/receipt path |
| Served shell format | `tools/port/served.mjs`, `server/shell.mjs` | Present, includes windows/nodes/wishes/notices/evidence |
| Openlock stream adapter | `shell/openlock/panorama/scripts/secure_shell_stream.js` | Present; reads wishes/notices, refuses nonempty window mapping |
| Mounted-window-to-display mapping | AGENTS.md §2.1; docs/SELECTIONS.md | Open; requires user-owned ruling and projection test |
| Completed client test surface | README / docs/ARCHITECTURE.md | Not yet built according to repository status |
| Complete desktop toast/client witness | README | Not yet built according to repository status |
| Game renderer / tactical map / city view | Not found in inspected source paths | Not present in inspected Secure Shell slice |
| Game world save/load catalog | Not found in inspected source paths | Not present; must be designed |
| World snapshots and replay journal | Not found in inspected source paths | Not present; must be designed |
| Runs and persistent homebase registry | Not found in inspected source paths | Not present; must be designed |
| Player/world multiplayer permissions | Docs state single project and one token, future multi-user scopes | Not present as a complete game permission model |

This table is deliberately conservative. “Not found in inspected source paths” is not a claim about every branch or uninspected file in every related repository. It is the status of the current `secure-shell` source and documentation I examined.

## 17.16 Recommended integration sequence

The correct order is to establish the shell's real delivery contract, prove one usable game surface, then add resumable game state and only then expand into multiple homebases. This avoids building an entire game UI on a window contract that the shell itself still marks open.

### Integration stage A — settle the existing display boundary

- Resolve the mounted set to Openlock display-window projection.
- Record the choice and the rejected alternatives in the repository's decision record.
- Keep the existing `dl-enclave.surface/1` and `surface.mount/2` ownership.
- Do not create a third window/package format.
- Prove the served payload can be read by the real client without silently dropping windows.
- Use the client test surface when it exists; do not report a Node or browser fixture as the desktop witness.

### Integration stage B — one meaningful game content surface

- Select one initial content window with an actual producer and complete behavior.
- Use the existing Openlock frame/taskbar/toast owner.
- Build a content view that can show world selection/resume and one initial world state.
- Do not add multiple empty windows for tactical, city, logistics, ecology, or research before those views exist.
- Preserve responsive behavior at the repository's 1440×900 and 1024×768 baseline sizes.

### Integration stage C — world persistence prototype

- Declare the world schema, stable IDs, state revision, simulation tick, snapshot format, and resume policy.
- Implement one world, one base, one primary track, and a very small enemy wave.
- Save a snapshot, stop the process, restart, restore, and verify the same world ID, base ID, revision, and key state.
- Test corruption, partial writes, stale commands, duplicate command retries, missing snapshot references, and schema mismatch.
- Emit one coarse Secure Shell activity event for a successful save/restore; do not flood the shell ledger with per-tick state.

### Integration stage D — founding runs and multiple bases

- Implement an expedition that discovers one candidate site.
- Add the founding command with operation ID and expected world revision.
- Verify a retry cannot create a duplicate base.
- Verify the base appears in the world catalog after restart.
- Verify each base has separate inventory and local production.
- Add one physical connection between bases and test transport capacity/failure.

### Integration stage E — strategic/macro simulation

- Model lane wave positions, resource windows, power spikes, rotations, structures, camps/objectives, and available reinforcements as state.
- Use conditions rather than hard-coded match minutes for state transitions.
- Introduce several active fronts only after the player can understand pressure and opportunity cost.
- Add coach-informed AI decision explanations: what the agent was trying to gain, what opportunity it gave up, and why the rotation/defense made sense.

### Integration stage F — broader simulation

- Add industrial networks, modules, loot, creature ecology, factions, and regional simulation as independently testable systems.
- Keep snapshots and deltas bounded.
- Add dormant-map aggregation and regional simulation only when the single-map simulation is already correct.
- Add multiplayer scopes only after a single-player world and permission model have clear invariants.

## 17.17 Concrete test obligations

The test plan should prove properties, not merely launch successfully.

| Test | Required proof |
|---|---|
| Package admission | A valid declaration is admitted; tampering, unknown kinds, and unsupported schemas are refused by name |
| Mount and display projection | Each mounted window maps exactly once; ID/title/geometry remain identical; unrepresentable windows are refused |
| Stream round trip | The exact server-produced `secure-shell.served/1` payload is accepted by the client adapter and rendered in the real shell |
| World identity | A saved world retains its ID and seed through restart and restore |
| Snapshot integrity | Changed bytes are refused and the last verified snapshot is identified |
| Journal replay | Snapshot + later events reconstruct the same state digest and revision |
| Duplicate command | Retrying a founding/build command with the same operation ID does not duplicate its effect |
| Stale command | A command using an older world revision is refused rather than overwriting newer state |
| Base founding | A successful run creates exactly one persistent homebase with a source run ID |
| Base independence | Two homebases do not share stock by default; transport is required |
| Pause/resume | A paused world does not advance while unloaded; resumed tick/state matches its saved boundary |
| Homebase recovery | Breach/damage is persisted; a repair operation changes the base revision and can itself be replayed |
| Shell notices | Save/found/restore events create a human-readable notice with a named state, not just a color |
| Client witness | The real client renders the view and presents the command, exit status, check count, surface, and digest |

## 17.18 Architectural conclusion

Secure Shell is a strong candidate to provide the game's **desktop shell, package admission/delivery, project activity trail, and user-facing notification path**. Its existing ledger and mount mechanisms are useful precisely because they have narrow, explicit responsibilities.

It should not be stretched into the game database. The game needs a separate world-domain state store with per-world snapshots and a replayable event journal. That store should borrow the same principles—typed commands, stable identity, idempotency, revision checks, explicit refusals, integrity verification, recoverable commits—but it must not pretend to be `secure-shell.ledger/1` or `surface.mount/2`.

The desired user experience is therefore:

1. Secure Shell opens the game package in its existing display.
2. The game presents a catalog of persistent worlds.
3. The player resumes any saved world at its last committed state.
4. A run explores beyond the current settlement and can found another homebase.
5. The new base is stored as a first-class world object, not a disposable run reward.
6. The player can return to any base or world, expand its logistics network, and continue playing indefinitely.
7. The shell reports important activity and package/security events, while the game simulation remains authoritative for all world state.

That separation allows the game to grow into the full city-building tower-defense ecosystem without turning the shell package stream, desktop UI, or activity ledger into a fragile all-purpose system.

---

# 18. Secure Shell source bibliography

All source links below refer to `alacripose/secure-shell` on `main`, inspected on October 31, 2026. A link identifies the source inspected; it does not imply that a runtime/client witness has passed.

- [README.md](https://github.com/alacripose/secure-shell/blob/main/README.md) — repository identity, status, commands, backend/client boundary.
- [AGENTS.md](https://github.com/alacripose/secure-shell/blob/main/AGENTS.md) — project conventions, evidence rules, layer owners, security boundary, one-window-at-a-time requirement.
- [docs/ARCHITECTURE.md](https://github.com/alacripose/secure-shell/blob/main/docs/ARCHITECTURE.md) — backend/client separation, ledger, HTTP/SSE, test surface, deployment model.
- [docs/LEDGER.md](https://github.com/alacripose/secure-shell/blob/main/docs/LEDGER.md) — `secure-shell.ledger/1`, append protocol, routes, evidence, limitations.
- [docs/DECISIONS.md](https://github.com/alacripose/secure-shell/blob/main/docs/DECISIONS.md) — user decisions for the package lineage, display owner, backend and ledger.
- [docs/SELECTIONS.md](https://github.com/alacripose/secure-shell/blob/main/docs/SELECTIONS.md) — selected and open package/display layers.
- [docs/PLAN.md](https://github.com/alacripose/secure-shell/blob/main/docs/PLAN.md) — staged plan and machine-readable exit gates.
- [docs/FIRST-TEN-HOURS.md](https://github.com/alacripose/secure-shell/blob/main/docs/FIRST-TEN-HOURS.md) — work order and witness requirements.
- [docs/FORMAT-SOURCES-AUDIT.md](https://github.com/alacripose/secure-shell/blob/main/docs/FORMAT-SOURCES-AUDIT.md) — package lineage and format-source audit.
- [server/ledger.mjs](https://github.com/alacripose/secure-shell/blob/main/server/ledger.mjs) — activity ledger implementation.
- [server/http.mjs](https://github.com/alacripose/secure-shell/blob/main/server/http.mjs) — authenticated HTTP routes and SSE event stream.
- [server/watch.mjs](https://github.com/alacripose/secure-shell/blob/main/server/watch.mjs) — digest-based activity watcher.
- [server/ingest.mjs](https://github.com/alacripose/secure-shell/blob/main/server/ingest.mjs) — `secure-shell.ingest/1` admission frame.
- [server/decl.mjs](https://github.com/alacripose/secure-shell/blob/main/server/decl.mjs) — shared declaration grammar.
- [server/shell.mjs](https://github.com/alacripose/secure-shell/blob/main/server/shell.mjs) — mount-to-served composition.
- [tools/port/mount.mjs](https://github.com/alacripose/secure-shell/blob/main/tools/port/mount.mjs) — mount host and reconnect behavior.
- [tools/port/store.mjs](https://github.com/alacripose/secure-shell/blob/main/tools/port/store.mjs) — `surface.mount/2` receipt store.
- [tools/port/served.mjs](https://github.com/alacripose/secure-shell/blob/main/tools/port/served.mjs) — `secure-shell.served/1` payload validation and derivation.
- [tools/port/shell-record.mjs](https://github.com/alacripose/secure-shell/blob/main/tools/port/shell-record.mjs) — shell record verification.
- [packages/toast/toast.surface](https://github.com/alacripose/secure-shell/blob/main/packages/toast/toast.surface) — first toast/wish demo surface.
- [shell/openlock/panorama/scripts/secure_shell_stream.js](https://github.com/alacripose/secure-shell/blob/main/shell/openlock/panorama/scripts/secure_shell_stream.js) — client stream adapter and current open window-mapping refusal.
- [server/test/http.test.mjs](https://github.com/alacripose/secure-shell/blob/main/server/test/http.test.mjs) — backend HTTP, SSE, checkpoint, and stale-sequence tests.
- [server/test/ingest.test.mjs](https://github.com/alacripose/secure-shell/blob/main/server/test/ingest.test.mjs) — admission, digest, schema, declaration, and reserved-kind tests.

## 18.1 Inspection limitations

This was a source/documentation inspection through the connected GitHub repository interface. I did not clone or execute the repository, run its tests, connect to a live Debian server, render the client, inspect every related upstream repository, or validate a live game package. Statements about the current client path are therefore based on source contracts and repository status documentation; the identified served-window mismatch is a static finding awaiting an end-to-end client witness.

# 19. Repository-grounded engineering synthesis: `ala-cities` and `dl-openshell`

## 19.1 Why these repositories matter to the proposed game

The earlier design analysis identified the game systems that should exist: an endless connected world, a city/factory economy, persistent defensive infrastructure, loot and upgrades, ecology, and a readable strategic/tactical interface. The two inspected repositories add something the inspiration games alone cannot provide: concrete examples of how to keep these systems from corrupting one another.

`ala-cities` treats authoritative world state, material flows, agent work, historical records, and derived 3D presentation as separate domains. `dl-openshell` treats package declarations, game-owned panels, client rendering, and server-delivered data as separate domains. Together they suggest a robust architecture for a game in which players build factories and defenses, employ agents, run a persistent settlement, and extend the interface or content through packages.

## 19.2 The simulation truth plane: lessons from `ala-cities`

### World time must not be a side effect of rendering

Use a fixed or otherwise explicitly versioned authoritative simulation clock. The renderer may interpolate movement and animation, but it cannot advance production, enemy movement, ammunition, decay, weather, or creature needs. Slow systems should run on declared cadences and bounded catch-up, rather than being updated opportunistically whenever a frame happens to render.

### Agents need separate jobs, tasks, paths, and work progress

A worker's employer, current assigned task, current route, carried resources, and completed work are separate facts. A worker can be unemployed but still complete a civic/settlement task. The same distinction helps construction crews, haulers, repair crews, and logistics drones. Creatures require ecology-specific motivations, but should still expose their current intent, route, needs, and action result for inspection.

### Materials must be traced across the entire defense economy

A resource should not teleport from a mine to a turret. It is extracted, moved, stored, transformed, consumed, wasted, repaired, recycled, or returned to the environment. Exact integer quantities and explicit transformation recipes can make the system auditable. A tower that lacks ammunition should expose the upstream cause: no raw input, a stopped process, a full buffer, insufficient power, a blocked belt, a lost transport route, or a stock-priority conflict.

For processes that transform mass, define an input/output balance. For energy, fluids, pressure, heat, and chemical transformations, use appropriately typed quantities and conservation/transfer rules rather than a universal float inventory. The model can simplify sub-grid effects, but it should never silently invent free mass or discard material with no declared sink.

### World state and world history need separate records

A snapshot answers “what exists now.” An event/ticket record answers “what was attempted, authorized, changed, refused, superseded, or retired.” A headless verifier can check the record against a saved world. The verifier still cannot decide whether a defense map is fun, whether the interface is readable, or whether an enemy is balanced. Those are separate human/playtest obligations.

## 19.3 The 3D world and presentation boundary: lessons from `ala-cities`' non-default branch

The `design-package-cli` branch is ten commits ahead of `main` and contains a target 3D prototype, not a completed canonical world generator. Its useful engineering boundary is:

1. seed + generator revision + signed coordinates produce derived baseline world chunks;
2. typed deltas record player/world changes over that baseline;
3. authoritative chunk state carries quantities and material identity;
4. an asset generator turns authoritative chunks into sparse octree/raycast data and greedy-meshed presentation assets;
5. the renderer consumes those assets;
6. a bounded streamer schedules derived chunk builds and evicts presentation cache entries without mutating canonical world state.

The branch also separates semantic object authoring (the example is a gear with named `Teeth`, `Opening`, and `Accent` controls) from world voxel geometry. Editing a shape recipe must not scale or alter the world substrate. Its cache identity includes world seed, generator revision, coordinate, builder hash, and a digest of world/neighbour boundary inputs.

The documented first benchmark records a p50 of approximately 5.14 ms for a cold starter chunk and 0.219 ms for a warm cached chunk, but the 32³ cube preview costs about 33.15 ms p50 and the 27-chunk stream takes about 440 ms p50 across its 27-tick sequence on the tested Ryzen 7 5700G host. These are useful first measurements, not pass/fail budgets. The report explicitly excludes concurrent generation, disk I/O/residency, process-restart replay, GPU memory, client integration, and human acceptance.

The full generator, material/texture promotion, runtime streamer integration, LOD, generator/world digests, save lineage, quantitative Earth source table, and performance/playtest gates remain open. Do not present the branch prototype as a completed infinite-world solution.

## 19.4 The presentation and content plane: lessons from `dl-openshell`

A complex interface should render declared content and declared actions rather than inventing a separate authority for every tool window. `dl-openshell` shows how a desktop-like shell can frame game-owned panels without reparenting them, attach contextual tool layers to windows, and deliver package-declared addon windows through a restricted node tree.

For the proposed game, this suggests:

- a shell that owns taskbar/window organization but does not own simulation state;
- tactical, city, inventory, production, map, ecology, and diagnostics windows declared with stable IDs;
- contextual tools that are declared beside the view they operate on;
- action routing through a single command/authorization path;
- a separate theme palette and chrome/form definition;
- client-side layouts whose data is bounded and allowlisted;
- visible named refusals when a tool cannot act or a panel is not available.

A contextual tool surface is particularly valuable for complex game views. Selecting a factory can offer inspect, configure, trace supply, set priority, pin, or compare; selecting an enemy can offer inspect, track, log encounter, or set threat marker. The surface should not expose actions the selected object cannot support.

### UI performance, input reachability, and evidence scope

`dl-openshell` documents a real UI scheduling repair: content-bound walks were turned into resumable scans of approximately 4 ms per tick; a measured 31-second idle/open run reported 3.9 ms UI mean, 5.2 ms peak, 0% samples above 16.7 ms, and 16.02 ms average engine frame. Earlier logs had shown 255–330 ms binds, 139–236 ms HUD scans, and multi-second stalls. The same report warns that subsequent Popit/delivery changes were not remeasured against the final archive. This is a good example of measuring UI discovery and layout as their own workload, but not a certified performance promise for every build.

The UI pass is also explicit about what was not yet proven: keyboard/gamepad reachability has not been implemented, there is no user-facing reduced-motion switch, audio mix was not measured on a device, and the live proof captured the LBP3 Popit frame but not the other five declared frame styles or the Picker. The proposed game should include these as deliberate accessibility and acceptance gates. A mouse-only tool shell may be acceptable for one narrow PC interface, but it must not be treated as a universal interaction architecture for controller, touch, or accessible play.

## 19.5 Packages are not executable mods

`dl-openshell`'s parser and renderer demonstrate a useful restricted model for user-created UI: bounded archive sizes, versioned recipes, path checks, known tags, allowlisted attributes and styles, no script evaluation, bounded node count/depth, and atomic replacement. It leaves the prior working package mounted if a replacement is refused.

A proposed game's package architecture should distinguish at least:

- **visual/UI package:** declarative panel/layout/theme data only;
- **content package:** data definitions for towers, enemies, materials, maps, recipes, or campaigns;
- **blueprint package:** validated structural/production layout data;
- **executable extension:** a different trust class, capability model, runtime sandbox, and security review.

Do not imply that a digest or proof-of-work field makes a package safe. The inspected `dl-openshell` envelope is optional; unsigned packages load. Its HMAC-SHA256 key is machine-local, so it is not a portable public signing identity. A release checksum and its checksum file from the same origin establish integrity relative to that origin, not independent publisher authenticity. If publisher authentication is needed, use an asymmetric signature with a pinned public key and an independent release digest.

### Package security and claim governance

The repository evidence also identifies a missing layer above the parser. Open issue #13 says the package Axis ratchet is not yet wired into a complete registry binding package identity, declared windows/cells, landing-page claims, tickets, witness predicates, envelope digest/signature, and proof-of-work records. The proposed project should not let a package become “verified” merely because its bytes parse or a checksum matches. A package claim should be in a named state such as held, absent, refused, or independently verified; the claim, package digest, witness, ticket, and trust decision must be traceable together. Worker-generated content should similarly be checked against its task estimate, capability, and completion evidence rather than accepted from a self-reported status.

A second integration issue remains open in `dl-openshell` (#22): package windows with source XML can use a live node-tree renderer, but game-owned windows/cells still lack a unified source-tree feed and retain the compiled-layout path. For the proposed game, do not build a first-class package renderer while leaving native simulation views on a parallel, behaviorally inconsistent UI route. One declared view model should feed native windows and permitted addon windows, with an explicit fallback when source data is unavailable.

## 19.6 Evidence gates must be layer-specific

The two repositories strongly reinforce the project's existing evidence doctrine. A successful simulation test does not prove the rendered image is legible. A UI fake-tree test does not prove a real game's live panel can be clicked. A benchmark does not create a performance budget by itself. A source-level implementation does not prove a package works in the release client.

For the proposed game, separate gates should cover:

- deterministic simulation/replay;
- world material and energy accounting;
- agent route/task completion;
- save, migration, and interrupted-write recovery;
- generator identity, chunk boundaries, and unload/reload;
- mesh correctness against world truth;
- streaming tick/frame budgets and process/GPU memory;
- UI geometry, hit-testing, keyboard/controller reachability, and rendered legibility;
- package parser security, permissions, signature verification, and compatibility;
- installed release parity;
- human playtests and balance.

The project should name a claim's evidence state: source present, branch prototype, harness-verified, live-runtime observed, human accepted, or open. Never collapse all of those into one green status.

## 19.7 How the repository sweeps change the proposed roadmap

The existing roadmap should prioritize a coherent founding/defense loop before world scale. The repository studies refine that order:

1. settle the canonical world, simulation time, material quantities, generator identity, and save semantics;
2. implement one small world region and one complete extraction/hauling/processing/consumption chain;
3. add one worker with separate job/task/path/work state;
4. add one enemy route, one defensive tower, ammunition/fuel/power dependencies, and a readable supply-failure reason;
5. test deterministic replay, conservation, chunk load/unload, and world-readback;
6. add derived mesh generation and bounded chunk streaming with measured budgets;
7. add a declared window shell and one contextual tool surface, with real-client hit-testing and refusal tests;
8. add content packages/blueprints after capability and trust boundaries are explicit;
9. only then scale to multiple regions, production networks, ecology layers, multiple bases, and community content.

This sequence reduces the chance of building a visually impressive world whose economy and agent systems cannot be audited, or a sophisticated interface that can show systems it is not authorized to control.

## 19.8 Evidence boundary and repository-specific unresolved work

At the time of inspection (2026-10-31):

- `ala-cities` default `main` is a 2D C1 city-builder baseline. The 3D founding-world and asset-generator path exists on `design-package-cli`, a non-default branch 10 commits ahead. The branch's own records retain open generator, save/replay, capability-packet, quantitative-source, performance, and human-playtest work. The design verifier is intentionally nonzero while required questions remain open; its benchmark is first evidence, not a performance pass.
- `dl-openshell`'s README defines the primary product as a Deadlock Panorama UI shell, with a companion Deadworks plugin. The repository records live Popit/UI captures and idle-performance measurements, while the delivery report still distinguishes earlier live observer-toast evidence from harness-only atomic replacement and the final failed console proof attempt. Its older `GROUND-TRUTH.md` and `openlock.manifest.json` disagree about whether Deadworks was installed/loaded; the manifest itself says gameplay modifiers have not run in game. That status must remain unresolved until fresh, same-build evidence reconciles it.
- Issues #31–#38 closed on 2026-10-31 describe native desktop presentation, Debian X11/Wayland witnesses, an empty desktop and package round trip, pointer/placement/window-region work, a package store, and conversion of an external MOTD addon. The inspected `dl-openshell` default-branch head `cb73b7b` is dated 2026-10-30 07:57 UTC, before those closures, and its source tree does not contain a complete `dl-enclave`/external Go-shell implementation. Treat the issue closures as evidence of a related port/workstream, not as code merged into this default branch.
- `dl-openshell` still has two relevant open integration gaps: issue #13's package claims/witness/ticket ratchet is not complete, and issue #22's unification of native game windows/cells with the live XML surface remains open.

The complete source-by-source sweep, issue links, benchmark table, and threat-model notes are provided in the accompanying research dossier: [`github_research_sweep_ala_cities_dl_openshell.md`](github_research_sweep_ala_cities_dl_openshell.md).

---

# Dawnbreak: Public Research, Version Archaeology, and Mod-List Analysis

**Research date:** 31 October 2026  
**Source supplied by the user:** `references.md` (100 lines; 3 non-mod references and 95 mod/project references)  
**Scope:** Identify what can be verified about the Dawnbreak Minecraft project and its modded distributions/variants; classify the supplied mod-reference list; inspect public repository/project history where available; and extract technical design lessons relevant to the persistent city-builder / tower-defense proposal.

> **Evidence boundary.** The uploaded file is a URL/reference inventory, not a CurseForge manifest, Modrinth index, Packwiz lockfile, launcher export, or server `mods/` directory. It does not itself prove that every linked project was installed in every Dawnbreak version. This report therefore distinguishes (1) facts visible in the supplied list, (2) public facts from the linked projects, (3) likely compatibility groupings, and (4) facts that remain unverified.

---

## 1. Executive findings

### 1.1 What I could confirm about Dawnbreak itself

The connected GitHub account is `alacripose`. It contains a public repository named [`alacripose/dawnbreak-coders`](https://github.com/alacripose/dawnbreak-coders). The repository metadata identifies it as a public repository created in July 2025, but it is empty in the currently visible repository state: its reported size is zero and its visible history consists of a single initial commit dated 19 July 2025. GitHub code searches within it did not expose a README, modpack manifest, `modrinth.index.json`, `manifest.json`, `mods.toml`, or a mod list. It confirms that the name has been reserved/used in your public GitHub workspace; it does **not** document the pack's playable versions or its history.

I searched public web and GitHub indexes for the exact name in combination with Minecraft, modpack, CurseForge, Modrinth, Create, NeoForge, Packwiz, and your GitHub handle. I did not find a public CurseForge or Modrinth project that I could confidently attribute to your Dawnbreak project. The owner has since clarified that project versions were distributed through Google Drive and that most historical information is held on the Coalescent Nights community server. Therefore, the negative public-platform search is narrow: it means I did not identify an attributable indexed CurseForge/Modrinth project. It does **not** mean that Dawnbreak had no releases or no version history. For chronology and release reconstruction, the relevant primary sources are the Google Drive archives and dated Discord messages; X posts and surviving local files are supplementary.

The visible CurseForge result named **The Voyager's Kit-Dawnbreak** is a false positive for this investigation. It is a different modpack by SevArktic, for Minecraft 1.12.2, with a sequence of 2018 release names such as `The Voyager's Kit-Dawnbreak-1.11.3.5`. Its title happens to contain the word Dawnbreak; it is not evidence about your pack. Other search hits for a DawnBreak SMP, a Steam game titled *Dawn Break*, and the *Dawnbreak Forest* biome in another Minecraft mod are also unrelated name collisions.

### 1.2 What the supplied list actually contains

The uploaded list contains **95 mod/project references**, plus three additional references that are not mods: a Gist, an article about greedy/backtracking algorithms, and the *Age of Engineering* modpack page. It also states that libraries are not included. The list combines several kinds of material:

- The Create industrial-production and transportation ecosystem.
- Refined Storage logistics and automatic replenishment.
- GregTech, Tinkers' Construct, BuildCraft, Calculator, and other technology/progression systems.
- World generation, terrain, plants, seasons, weather, creature behavior, and environmental sound.
- Performance, rendering, memory, networking, debugging, and quality-of-life projects.
- A datapack (`Archipelago`) and several libraries/APIs or compatibility pieces, not only end-user content mods.

The list reads much more like a curated set of references across the pack's development and variants than a clean, single-version manifest. It is not safe to assume all 95 references were simultaneously installed in one release.

### 1.3 Strong evidence for multiple Minecraft/loader generations

The strongest version fingerprints are:

| Fingerprint in the supplied list | Public compatibility evidence | What it means for reconstruction |
|---|---|---|
| BuildCraft | Its familiar official line is the Minecraft 1.12.2 Forge generation. | Treat it as a legacy profile/version unless an exact fork/port is supplied. |
| Calculator (SonarSonic) | Public release history points to the older 1.12.2 generation. | It cannot be assumed to coexist with current Create 1.21.1 builds. |
| Tinkers' Construct | Current mainstream releases in the searched official sources are centered on 1.20.1 Forge/NeoForge and earlier supported versions. | Likely a 1.20.1-era progression profile or a separately curated branch; exact jar needed. |
| Create add-on cluster | Many entries have explicit 1.21.1 NeoForge releases and/or 1.20.1 Forge releases. | A modern Create-oriented branch is plausible, but each add-on must be pinned to the same Minecraft, loader, and Create API generation. |
| Fabric-focused projects | Resquake, Krypton, Lazy Language Loader, and some visual/QoL mods are Fabric-oriented or expose Fabric-only releases in the checked pages. | These suggest a Fabric profile, optional client profile, or version-specific alternatives—not automatic compatibility with a NeoForge server pack. |
| TerraFirmaCraft | Current public releases exist for 1.21.1 NeoForge and 1.20.1 Forge/NeoForge, with significantly different survival/worldgen behavior. | Could be a distinct survival-oriented variant, or a major world-generation/progression dependency within one profile. Verify the actual manifest. |

A plausible high-level reconstruction is that Dawnbreak has had (or considered) multiple generations: an older 1.12.2 technology/progression branch; a 1.20.1 Forge/NeoForge branch; and a more modern 1.21.1 NeoForge Create/automation branch, with some Fabric-specific projects used in separate builds or client-only alternatives. This is a hypothesis inferred from the linked projects' public compatibility, **not a verified release timeline for Dawnbreak**.

---

## 2. Public identity and version search: what is and is not established

### 2.1 Public repository and limits of public-platform search

[`alacripose/dawnbreak-coders`](https://github.com/alacripose/dawnbreak-coders) is the only directly relevant repository found under the connected GitHub account. Its current public state is too sparse to establish:

- which Minecraft versions Dawnbreak targeted;
- whether the pack used Forge, NeoForge, or Fabric in a given release;
- the exact mod IDs, filenames, file hashes, or dependency versions;
- release names, changelogs, server configs, datapacks, resource packs, or world-generation settings;
- which content belongs to a branch maintained jointly with your partner.

The repository name alone is not enough to identify Dawnbreak's actual pack source. It should be treated as one surviving public breadcrumb, not assumed to be the canonical release archive. The owner identifies Google Drive as the release-distribution location and the Coalescent Nights Discord as the main historical record. I did not treat unrelated repositories or search hits containing the same word as your work.

### 2.2 False positives worth excluding from the history

- **The Voyager's Kit-Dawnbreak:** an unrelated 2018 CurseForge modpack release series for Minecraft 1.12.2. It is useful only as a warning about name collisions.
- **DawnBreak SMP listings:** public directory results describe a Java-only modded SMP with factions, but the listings do not identify your pack and do not match the supplied list. Do not attribute them to you.
- **Dawnbreak Forest:** a biome name in the unrelated *Adventurer's Beyond* mod. It is not evidence of a Dawnbreak modpack release.
- **Dawn Break (Steam):** a separate non-Minecraft game.

### 2.3 What would establish the version history conclusively

A release's name, icon, or mod-count is not enough. For this project, begin with each original Google Drive distribution and match it to the corresponding Discord announcement or changelog. The authoritative technical artifact is the manifest/lockfile for each distributed build, backed by the exact downloaded files. For every Dawnbreak or successor release, the history should preserve:

1. Pack release ID and human release label.
2. Minecraft version and loader family/version.
3. The pack manifest format and schema version (CurseForge manifest, Modrinth index, Packwiz lockfile, Prism export, or another source).
4. Every mod's project ID, file/version ID, mod ID, file name, download origin, and SHA-256 digest.
5. Required libraries and APIs, even though the supplied curated list intentionally says libraries are not included.
6. Optional client-only mods, server-only mods, datapacks, resource packs, shader packs, and config overrides as separate categories.
7. Removed, replaced, and newly introduced projects between pack releases.
8. Known compatibility exceptions and the reason each one was retained.
9. A world-generation compatibility statement: whether a world may be upgraded, whether old chunks retain their generator, and whether new chunks can be generated safely.

A diff between these files would let us produce a genuine Dawnbreak changelog: what changed, why it changed, which systems it affected, and whether an existing world could move to the new version. The supplied reference list is a strong research index, but it cannot replace those manifests.

---

## 3. Mod-list analysis by system

The sections below interpret each project according to its public purpose where it could be verified. For names whose public source did not establish purpose or compatibility clearly, I explicitly mark the entry as unresolved rather than infer mechanics from its name. Minecraft and loader versions stated below refer to public project pages checked on or around 31 October 2026; they are not proof that Dawnbreak used that exact release.

### 3.1 Create: industrial production, process control, transport, and factories

This is the clearest design identity in the supplied list. The cluster is not merely a collection of decorative Create add-ons. It appears to build a multi-layer industrial simulation: material extraction and conversion, energy, fluid networks, machine status, factory control, automated distribution, storage, and rail logistics.

| Supplied reference | Verified or likely role | Compatibility / evidence note |
|---|---|---|
| [Create](https://github.com/Creators-of-Create/Create) | Base kinetic automation framework: rotating shafts, gears, belts, mechanical power, contraptions, processing machinery, and physical logistics. It is the core dependency for much of the cluster. | The repository link is authoritative, but the uploaded list does not name the exact Create build. Add-ons must match its exact Minecraft, loader, and Create API generation. |
| [Create Diesel Generators](https://github.com/george8188625/Create-Diesel-Generators) | Adds fuel-oriented industrial machinery and energy production. It makes fuels, fluids, and processing chains meaningful parts of a factory rather than treating rotational power as the only energy language. | Public project pages expose 1.21.1 NeoForge and older 1.20.1/1.19.2 branches. Check the selected file, not just the mod name. |
| [Create: Factory Controller](https://github.com/nbcss/create-factory-controller) | Factory-scale monitoring and orchestration. A central display can collect gauges, show machine/network state, organize views, and provide logic or control hooks. | Public 1.21.1 NeoForge and 1.20.1 Forge releases exist. The checked listing notes a compatibility limitation: Extra Gauges can coexist, but its custom gauge types cannot be embedded in Factory Controller dashboards. |
| [Create Extra Gauges](https://github.com/LIUKRAST/CreateExtraGauges) | Extends measurement and logic: integer/logic/string/passive gauges, arithmetic, counters, selectors, and boolean operations. This is a major step from visual machines to inspectable control systems. | Public 1.21.1 NeoForge and 1.20.1 Forge releases surfaced. Verify the exact version pair against Factory Controller. |
| [Create: The Factory Must Grow](https://github.com/DrMango14/Create-The_Factory_Must_Grow) | Heavy industrial expansion: additional production chains, material processing, industrial infrastructure, fluids, and large factory equipment. | Public releases include 1.21.1 NeoForge and 1.20.1 Forge branches; some files are beta. It must be treated as a major progression system, not a harmless decorative add-on. |
| [Create New Age](https://gitlab.com/antarcticgardens/create-new-age/) | Bridges Create to electrical and thermal power: generators, motors, coils, wires, solar/heat systems, and reactor/boiler integration. This creates multiple energy conversion and distribution problems. | Public 1.21.1 NeoForge and older 1.20.1/1.19.2 loader variants surfaced. Exact Botarium/dependency requirements differ by version. |
| [Create: Pantographs & Wires](https://github.com/MisterJulsen/Create-Pantographs-and-Wires) | Adds catenary wires, poles/masts, and pantographs for electrified trains. It links transport infrastructure to energy distribution and visual engineering. | Public 1.21.1 NeoForge files are beta; Create 6 releases have distinct labels/dependencies. |
| [Create Pressure Gauges](https://modrinth.com/mod/create-pressure-gauges) | Diagnostic tool for fluid networks: reports pump distance and remaining pressure/throughput along connected pipe runs. This makes fluid systems observable rather than opaque. | Public Modrinth page identifies Minecraft 1.21.x NeoForge and client/server support. |
| [Create Threaded Trains](https://github.com/MisterJulsen/Create-Threaded-Trains) | Moves train-network calculations to another thread so large rail graphs may be processed alongside the main server tick. | Public builds exist for 1.21.1 NeoForge and older 1.20.1 Forge/Fabric lines. It has a significant thread-safety caveat for add-ons that modify or depend on Create rail internals; test with a copy of the world and the whole add-on set. |
| [Create Mobile Packages](https://github.com/timplay33/Create-Mobile-Packages) | Logistics-network shipping: package depots/ports and mobile carriers move inventory between endpoints. This is a physicalized alternative to a single global inventory. | Public 1.21.1 NeoForge and 1.20.1 Forge release lines surfaced. The 1.21.1 line has specific NeoForge/Create minimums. |
| [Create Repackaged](https://github.com/LIUKRAST/Repackaged) | Extends Create's packaging/logistics ideas; the current project page describes package handling for resources and fluid/energy-related logistics. | Public 1.21.1 NeoForge release is present. Treat it as a Create logistics add-on, not as Secure Shell's package-stream format. |
| [Create Metalwork](https://github.com/AverageAnime/create-metalwork) | Adds a molten-metal and metal-processing layer, with ore/dust/fluid transformations and yield stages. It can make ore refinement a sequence with different efficiencies rather than a one-step recipe. | Public 1.21.1 NeoForge and 1.20.1/1.19.2 branches surfaced. Add-on configs matter because overlapping ore/metal recipes may duplicate or conflict with other metallurgy mods. |
| [Create Sifter / Create Sifting](https://github.com/oierbravo/createsifter) | Adds hand/mechanical sifting and configurable mesh tiers, with filtering and redstone-controlled processing on higher-tier machines. It supplies an early-to-midgame resource-recovery chain. | The repository link resolves to the Create Sifting project family; 1.21.1 NeoForge files exist. The newer line has a Mechanical's Lib dependency. |
| [Create More Vertical Additions](https://github.com/rekales/create-more-vertical-additions) | The current project identity surfaced as **Create More: Vertical Belts**: vertical belt variants improve vertical item transport and compact factory layouts. | Public 1.21.1 NeoForge and 1.20.1 Forge lines surfaced. The project title/slug has changed from the wording in the supplied reference. |
| [Create EZ Stock Ticker](https://github.com/cakeGit/Create-Ez-Stock-Ticker/) | Client-side quality-of-life changes for stock-ticker quantity entry and interaction; reduces friction when setting large item requests or batch sizes. | The current listing appears under the name **Create Easy Stock Ticker**. Confirm the exact JAR/mod ID when deduplicating. |
| [Create Storage (NeoForge)](https://modrinth.com/mod/create-storage-neo-forge) | Tiered storage and linked inventory networks that join Create logistics with expandable item storage. | Public 1.21.1 NeoForge and 1.20.1 Forge lines surfaced; Create minimum versions matter. |
| [Rubberworks](https://github.com/hadron13/Rubberworks) | Reference to a project in the rubber/material-processing space. The exact released functionality and current loader/version were not established conclusively from the available indexed material. | Keep as an unresolved project reference until its selected release/JAR and README are available. Do not assign it an inferred recipe chain. |
| [SteamPowered](https://git.tilera.org/Anvilcraft/SteamPowered) | Steam/industrial technology reference. The supplied link establishes a repository, but the available research did not establish which build, loader, or exact machine set belongs to the Dawnbreak variants. | Verify repository release, dependencies, and Minecraft version before placing it into the modern Create branch. |
| [DenseOres](https://github.com/NewJumper/DenseOres) | Ore-density/resource-distribution reference. Exact current project behavior and compatibility remain unconfirmed from the evidence retrieved. | Treat ore-generation balance and worldgen compatibility as open questions, not confirmed pack mechanics. |

**Interpretation:** this cluster implies a design that values *production as a visible physical process*. Machines are not just crafting recipe buttons. Their placement, power source, material inputs, pressure, network connectivity, throughput, and control signals all matter. For the proposed game, that suggests an industrial simulation whose logic can be understood by inspecting the machine graph, not a single abstract “factory level.”

### 3.2 Refined Storage, inventory networks, and recurring demand

| Supplied reference | Role | Compatibility note |
|---|---|---|
| [Refined Storage](https://github.com/refinedmods/refinedstorage) | Networked item/fluid storage and crafting. Centralizes access without requiring each machine to physically touch every other machine. | Exact main branch/file version not specified in the uploaded list. Keep storage network identity and physical transport as separate concepts. |
| [Extra Disks](https://modrinth.com/mod/extra-disks) | Extends storage capacities beyond the base disk progression. | Public 1.21.1 NeoForge line surfaced. Verify dependency and tier compatibility. |
| [Cable Tiers](https://modrinth.com/mod/cable-tiers) | Adds throughput/upgrade tiers to Refined Storage cables and network devices, changing transfer speed and/or filtering capacity. | Public 1.21.1 NeoForge support surfaced. Exact tier values must come from the selected config/release. |
| [StockBridge (Refined Storage + Create)](https://modrinth.com/mod/stockbridge(refinedstrotage%2Bcreate)) | Exposes Refined Storage inventory to Create logistics through network interfaces so depots, funnels, arms, and stock systems can interact with the digital inventory. | Public 1.21.1/1.20.1 loader builds surfaced. The project slug in the uploaded URL contains the spelling `RefinedStrotage`; preserve that exact source identity when matching files. |
| [Refined Fluid Substitution](https://modrinth.com/mod/refined-fluid-substitution) | Allows fluid requirements to participate in Refined Storage autocrafting without manually substituting filled buckets for each fluid request. | Public 1.21.1 NeoForge/Fabric variants surfaced. Requires Refined Storage. |
| [Create Storage (NeoForge)](https://modrinth.com/mod/create-storage-neo-forge) | Bridges physical storage infrastructure to Create workflows. Listed above as part of the Create cluster; it is relevant here too. | Avoid double-counting the reference in a manifest. |
| [Step Crafter](https://github.com/starforcraft/Step-Crafter) | Adds step-based recursive autocrafting and minimum/maximum stock targets to Refined Storage; it can replenish a resource when stock falls below a minimum. | Public 1.21.1 Fabric and NeoForge builds surfaced. This mod introduces demand-driven production rather than only player-triggered crafting. |

The key separation is between **inventory access** and **physical movement**. Refined Storage can let a player access a remote stock; Create packages, trains, arms, belts, and depots move goods through a spatial network. That contrast is directly applicable to a game with multiple homebases: some resources can be in a shared account/network, while others must be shipped through actual routes with capacity, delays, risk, and transfer costs.

### 3.3 Technology and progression branches beyond Create

| Supplied reference | Role | Version evidence / caution |
|---|---|---|
| [GregTech CEu Modern](https://github.com/GregTechCEu/GregTech-Modern) | Large industrial progression system with staged materials, machines, recipes, energy tiers, and increasingly advanced production. | Public 1.20.1 Forge and 1.21.x NeoForge development/build lines surfaced; the newest 1.21 line has experimental/alpha/beta status in parts. Do not treat a new version label as proof of a stable pack-compatible release. |
| [Tinkers' Construct](https://github.com/SlimeKnights/TinkersConstruct) | Modular tools, material traits, smeltery and alloying progression. It offers a player-facing metallurgy/crafting system distinct from Create automation. | Mainstream release history surfaced around 1.20.1 Forge/NeoForge and earlier versions; do not assume a 1.21.1 file exists for the exact supplied release. |
| [BuildCraft](https://github.com/BuildCraft/BuildCraft) | Legacy industrial automation: pipes, engines, quarries, pumps, and transport. | Official public project line is the old 1.12.2 Forge ecosystem. Treat as a separate old pack generation, unless a specific maintained port is provided. |
| [Calculator](https://github.com/SonarSonic/Calculator) | Legacy technology/crafting system built around calculator-driven recipes, machines, power generation, and specialized tools. | Public project history is centered on 1.12.2. It is not evidence of a modern 1.21.x Create add-on. |
| [Age of Engineering](https://www.curseforge.com/minecraft/modpacks/age-of-engineering) | Not a mod in this inventory: it is a modpack reference. It is likely included as a progression/design source rather than an installed Dawnbreak dependency. | Excluded from the count of 95 mods. |

These technologies should not be merged into one undifferentiated progression tree. GregTech and Tinkers' Construct can each own distinct material/tool/industrial progression. BuildCraft and Calculator are more likely historical branch references, and the *Age of Engineering* pack should be treated as a progression inspiration unless a Dawnbreak manifest says otherwise.

### 3.4 World generation, biomes, terrain, and geology

| Supplied reference | Role | Version/compatibility note |
|---|---|---|
| [Archipelago](https://modrinth.com/datapack/archipelago) | A datapack, not a conventional mod JAR. Generates an island/archipelago-style world and changes the spatial distribution of terrain/biomes. | Public datapack support surfaced across 1.20.x and 1.21.x. Worldgen datapacks must be recorded with the world preset and config, not just the mod list. |
| [BigGlobe](https://github.com/Builderb0y/BigGlobe) | Major world-generation framework with configurable terrain, biomes, ores, flowers, structures, and other features. | The public project line is Fabric-oriented. It needs explicit compatibility testing with other terrain/biome mods and is not a safe assumption for a NeoForge build. |
| [TerraFirmaCraft](https://github.com/TerraFirmaCraft/TerraFirmaCraft) | Major survival and world-generation overhaul: geological progression, terrain/continents, rivers, mountains, flora/fauna, and a different survival economy. | Public 1.21.1 NeoForge and 1.20.1 Forge/NeoForge branches surfaced. TFC changes far more than a cosmetic worldgen layer; it may define a separate Dawnbreak variant. |
| [Lithostitched](https://github.com/Apollounknowndev/lithostitched/) | World-generation/structure integration library and extension point. | This is closer to infrastructure than visible content; include it in dependency/loader accounting even when the curated list excludes libraries. |
| [DenseOres](https://github.com/NewJumper/DenseOres) | Potential resource/ore distribution tuning layer. | Exact behavior and build compatibility unresolved; do not infer its worldgen effect from its name alone. |
| [Chunky](https://github.com/pop4959/Chunky) | Chunk pre-generation for servers/worlds. It can move expensive terrain creation out of live exploration and reduce first-visit spikes. | Exact version is not given. Server-side generation settings and worldgen mod set must be pinned together. |
| [Resquake](https://github.com/polina4096/resquake) | Quake-style movement (bunnyhopping, trimping, sharking) rather than world generation; listed separately here only in the mechanical traversal layer below. | Public README describes a Fabric rewrite; server installation is needed for consistent fall-damage behavior. |

**A major world migration hazard:** terrain generation is not simply a visual asset that can be upgraded at any time. A world created under one generator may contain terrain/biome IDs and chunk-generation assumptions that a later profile no longer reproduces. For Dawnbreak versions, the manifest and world preset must be treated as one compatibility unit. For the proposed game, world generation parameters should be frozen in a per-world generation record; changing the generator for future unexplored territory must be an explicit, versioned world transition.

### 3.5 Plants, agriculture, seasons, and weather

| Supplied reference | Role | Version/compatibility note |
|---|---|---|
| [Dynamic Trees](https://github.com/DynamicTreesTeam/DynamicTrees) | Trees grow as connected structures with branch/leaf networks rather than remaining static blocks. Growth, shape, and leaf distribution are simulated. | Public 1.21.1 multi-loader builds surfaced. The living-tree model is relevant to a persistent ecology simulation. |
| [Dynamic Trees Plus](https://github.com/DynamicTreesTeam/DynamicTreesPlus) | Expands the Dynamic Trees model to more plant-like structures and advanced vegetation. | Public 1.21.1 NeoForge and older 1.20.1 branches surfaced; depends on Dynamic Trees. |
| [Serene Seasons](https://github.com/Glitchfiend/SereneSeasons) | Adds seasonal visual and climate changes; seasons can affect temperatures, snow, and crop growth. | Public 1.21.1 NeoForge and other loader branches surfaced. Seasonal effects must be reconciled with other agriculture mods. |
| [Immersive Weathering](https://github.com/Silversmith-Mods/Immersive-Weathering) | Environmental aging of blocks: moss, rust, cracking, char, icicles, and other weather-driven changes. | The indexed project line surfaced chiefly for 1.20.1 and older in the checked references; do not assume a current 1.21.1 file without checking the exact release. |
| [Water Your Crops](https://www.curseforge.com/minecraft/mc-mods/water-your-crops) | Makes crop survival/growth depend more directly on adequate water/moisture; the public listing describes dry farmland causing crop growth regression or crop loss under configured rules. | The checked page's common file line is 1.20.1 Forge. Exact 1.21.x loader support was not confirmed. |
| [Ecologics](https://github.com/samedifferent/Ecologics) | Vanilla-style biome ecology: flora, small wildlife, and biome-specific features such as coconut palms, crabs, squirrels, walnuts, and penguins. | Public 1.21.1 NeoForge release surfaced. |
| [Create Sifter](https://github.com/oierbravo/createsifter) | Included in the industrial section; it also matters to agricultural/mineral material recovery loops. | See §3.1. |
| [SeedDelight](https://github.com/HiedaCamellia/SeedDelight) | A food/agriculture-related project reference. Its exact current feature set and compatibility were not fully established by the indexed evidence. | Keep marked unresolved until the selected JAR/README is checked. |
| [Ratatouille](https://github.com/TeamStarfruit/Ratatouille) | A Create-adjacent food processing/culinary automation mod reference. | Public 1.21.1 NeoForge and 1.20.1 Forge/NeoForge branches surfaced; verify the exact source version and dependencies. |
| [Create Diesel Generators](https://github.com/george8188625/Create-Diesel-Generators) | Listed in §3.1; it also provides a path for biofuel/fuel production and agricultural-industry coupling depending on the pack's configs/recipes. | Do not infer exact crops/biomass recipes without the selected recipe/config files. |

A separate public repository under your account, [`Agricultural_Enhancements`](https://github.com/alacripose/Agricultural_Enhancements/tree/1.21.1-neoforge), is especially relevant even though it is **not present in the supplied 95-link list**. Its README and build properties describe a 1.21.1 NeoForge agricultural mod (version 0.3.21 in the inspected branch; NeoForge 21.1.73; requiring VapourWare 0.2.36) with:

- an **Irrigation Controller** that pressurizes irrigation pipe networks up to 15 blocks away and supplies sprayers that affect a 3×3 crop/soil area;
- a **Fertilizer Producer** that breaks ingredients into macro-nutrients and produces fertilizer at an NPK ratio of 1:1:1, with storage/output constraints that can jam the machine if nutrient balances are mishandled;
- a **Crop Manager** that tills, plants, and maintains soil nutrition using fertilizer;
- a **Harvester** with destructive and non-destructive modes, where non-destructive harvesting returns growth to zero when supported;
- **Tilled Soil** with moisture and nutrient state, moisture propagation, rainfall/biome-precipitation effects, crop growth consumption, and soft trampling.

This is a direct example of agriculture treated as a material/biological process rather than a recipe list. It is a strong candidate for a Dawnbreak integration comparison, but I did not find it in the uploaded curated references and therefore do not claim it is installed in Dawnbreak.

### 3.6 Wildlife, creature movement, and living-world behavior

| Supplied reference | Role | Version/compatibility note |
|---|---|---|
| [Boids Reforged](https://www.curseforge.com/minecraft/mc-mods/boids-reforged) | Boids-style flocking/herding behavior for entities such as birds and fish; emergent movement can make groups behave as a coordinated flock rather than unrelated pathfinders. | The project's current name surfaced as **Boids** with 1.21.1 multi-loader support. Confirm exact file ID and whether the older Forge project is the same lineage. |
| [Boids Birds](https://github.com/Leclowndu93150/Boids-Birds) | Bird species/content layer for the Boids behavior framework. | Public 1.21.1 NeoForge/Fabric support surfaced. Check the exact dependency version. |
| [Curious Mobs](https://www.curseforge.com/minecraft/mc-mods/curious-mobs) | Changes mob behavior so creatures can move toward a player's last known location after losing aggro, extending pursuit/memory beyond direct line of sight. | The indexed files are centered on older 1.20.1/1.19.x Forge releases; a current 1.21.1 build was not confirmed. |
| [Dynamic Trees](https://github.com/DynamicTreesTeam/DynamicTrees) | Listed above; its growing tree structures are part of the habitat/vegetation simulation. | See §3.5. |
| [Ecologics](https://github.com/samedifferent/Ecologics) | Adds biome-linked wildlife and flora. | See §3.5. |
| [BigGlobe](https://github.com/Builderb0y/BigGlobe) | Alters the landscape and ecological distribution on which wildlife depends. | Fabric-oriented; see §3.4. |

This cluster has a clear connection to the requested game's Rain World-inspired ecosystem. There are at least three distinct levels: (1) an individual creature's internal goal/last-known-target state, (2) group movement such as boids/flocking, and (3) habitat/resource distribution generated by world terrain and plants. A simulation should not collapse those into one “AI quality” slider. Each level should have its own rules and observable evidence.

### 3.7 Construction, traversal, and player-facing interaction

| Supplied reference | Role | Version/compatibility note |
|---|---|---|
| [Effortless Building](https://github.com/huskuraft/effortless) | Shape-based building, placement patterns, mirrors/arrays, clipboard-style copying, and faster structure construction. | Public multi-loader 1.21.1 support surfaced. It is a powerful building tool, not just a cosmetic convenience. |
| [Resquake](https://github.com/polina4096/resquake) | Quake-like movement: bunnyhopping, trimping, sharking and tunable movement values. | Fabric-focused; public README says installing on the server is necessary for consistent fall-damage handling. |
| [Personality](https://github.com/team-abnormals/personality) | Adds sitting, crawling, and movement/animation “gamefeel” interactions. | The official Team Abnormals project lists Forge and a 1.20.1 line; Blueprint is a dependency. |
| [Mouse Tweaks](https://github.com/YaLTeR/MouseTweaks) | Improves inventory drag/scroll interactions, reducing mechanical friction in item handling. | Client-side UX; exact build must match loader/Minecraft profile. |
| [Minecraft Smooth Scrolling](https://github.com/SmajloSlovakian/Minecraft-Smooth-Scrolling) | Smooth scrolling in chat, hotbar, inventory, server/world lists, and resource-pack lists. | The source describes a Fabric mod; do not assume it can be used in a NeoForge-only profile. |
| [Pickup Notifier](https://github.com/Fuzss/pick-up-notifier) | Makes item pickup events more visible, improving feedback when collecting drops and resources. | Loader and exact release need to be pinned. |
| [Enhanced Tooltips](https://modrinth.com/mod/tooltips-enhanced) | Adds contextual item information such as tool durability/mining speed, armor stats, enchantment descriptions, and food nutrition/saturation/effects. | There are similarly named Enhanced Tooltips projects. The supplied link resolves to one particular project; match the actual mod ID before conflating it with other projects of the same name. |
| [Jade](https://github.com/Snownee/Jade) | In-world inspection overlay for blocks/entities/machines. Especially valuable for understanding modded machine state. | API/loader version must match the relevant pack branch. |
| [Searchables](https://github.com/jaredlll08/searchables) | Search UI support/library used by other mods' searchable lists and menus. | Treat as infrastructure/dependency rather than a content feature. |
| [Camera Overhaul](https://github.com/Mirsario/Minecraft-CameraOverhaul) | Camera behavior/feel changes tied to player motion. | Client-facing; confirm exact release and loader before assigning to a build. |
| [Minecraft AR Mod](https://github.com/mt1006/mc-ar-mod) | The linked project is an experimental asynchronous frame-reprojection/AR-style visual prototype, not a general gameplay feature. | The checked project material describes a proof-of-concept Fabric 1.20.1 line and warns that it can hurt main-thread performance even when disabled. Treat as experimental, not a baseline optimization. |

### 3.8 Audio, weather presentation, and environmental feedback

| Supplied reference | Role | Version/compatibility note |
|---|---|---|
| [AmbientSounds](https://github.com/CreativeMD/AmbientSounds) | Rich biome/environment ambience with custom sound engines and sound states responsive to the environment. | Public pages show broad 1.21.x support and multiple loaders; requires CreativeCore. |
| [Cool Rain](https://github.com/Leclowndu93150/Cool-Rain) | Adds different rain sounds depending on the material/surface: glass, water, lava/magma, foliage, metal, wool; volume responds to proximity and surrounding blocks. | Public pages list 1.21.x and 1.20.1 files; loader support differs by release (Fabric/NeoForge surfaced). |
| [Presence Footsteps](https://github.com/Sollace/Presence-Footsteps) | Surface-aware player footsteps and movement audio. | Client-side sound presentation; check the chosen build's loader and compatibility. |
| [Matmos Ambient Sound](https://modrinth.com/mod/matmos-ambient-sound) | Environmental and interaction soundscape, with spatial sources, weather, biome, indoor/outdoor, cave, and Nether behavior. | Public pages describe 1.18.2–1.21.8 and newer support; requires TCTCore. |
| [Biome Music](https://github.com/someaddons/biomemusic) | Adjusts music delay/variety and improves music selection for modded biomes. | Public 1.21.x/NeoForge and Fabric releases surfaced. |
| [Sound Physics Remastered](https://www.curseforge.com/minecraft/mc-mods/sound-physics-remastered) | Acoustic occlusion, reverberation, and propagation behavior that makes caves, enclosed spaces, and materials sound different. | Verify a build compatible with the specific Minecraft/loader profile. |
| [Immersive Thunder](https://www.curseforge.com/minecraft/mc-mods/immersivethunder) | Weather/thunder audiovisual enhancement. | Exact selected build and loader were not confirmed in this research pass. |
| [Cool Rain](https://github.com/Leclowndu93150/Cool-Rain) | Also a physical-material audio system; listed once in the manifest but relevant to both weather and environmental presentation. | Avoid counting the same project twice. |

The pack appears to treat sound as world information, not just decoration: rain communicates what surface it strikes, footsteps communicate ground material, music changes with biome, and ambience distinguishes enclosed/exposed spaces. The game design could adopt the same principle for settlement feedback: power hums, machine load, water movement, distant creature calls, structural stress, approaching waves, and weather should help players infer simulation state without opening a dashboard.

### 3.9 Performance, rendering, networking, and stability

This is the largest and most compatibility-sensitive group. Several entries are alternative rendering paths or low-level mixin/runtime patches. Their presence in one reference inventory does not establish that they all belong together or that they are all enabled at once.

| Supplied reference | Role | Key caveat |
|---|---|---|
| [Sodium](https://github.com/CaffeineMC/sodium) | Optimizes terrain rendering. | Exact mod loader and Sodium branch must match all dependent rendering add-ons. |
| [ImmediatelyFast](https://github.com/RaphiMC/ImmediatelyFast) | Batches/optimizes immediate rendering work for entities, block entities, particles, text, and GUI. | Verify compatibility with the exact rendering pipeline. |
| [FerriteCore](https://github.com/malte0811/FerriteCore) | Reduces memory use, especially from model/state data. | Core optimization layer; use matched loader release. |
| [Lithium](https://github.com/CaffeineMC/lithium) | Optimizes game logic/tick hot paths. | Not a guarantee of identical behavior under every mixin combination; pin and test. |
| [ModernFix](https://github.com/embeddedt/ModernFix) | Broad performance, memory, startup, and bug-fix optimization. | Overlaps with other optimizers in some areas; profile rather than assume cumulative gains. |
| [Krypton](https://github.com/astei/krypton) | Network protocol/stack optimization. | Fabric/Quilt-oriented in the checked release ecosystem; don't list it as a universal NeoForge dependency. |
| [Reese's Sodium Options](https://github.com/FlashyReese/reeses-sodium-options) | Alternate/expanded Sodium options UI. | Requires a compatible Sodium/options API version. |
| [Sodium Dynamic Lights](https://github.com/txnimc/SodiumDynamicLights) | Dynamic-light behavior integrated with Sodium. | Rendering backend-specific; verify the exact fork/build. |
| [Sodium Extras](https://github.com/txnimc/SodiumExtras) | Additional rendering/visual settings for Sodium. | Requires matching Sodium and options infrastructure. |
| [Sodium Leaf Culling](https://github.com/txnimc/SodiumLeafCulling) | Improves culling of leaves/foliage for rendering performance. | Visual compatibility depends on block models/resource packs and Sodium version. |
| [Sodium Options API](https://github.com/txnimc/SodiumOptionsAPI) | API used by Sodium option integrations. | Library/API role; likely part of the omitted dependency accounting rather than an end-user feature. |
| [Flywheel](https://github.com/Engine-Room/Flywheel) | Rendering engine/library used for Create's animated instanced visuals. | Important Create dependency/path; exact branch must match Create. |
| [Distant Horizons](https://gitlab.com/distant-horizons-team/distant-horizons) | Far-distance LOD rendering to display terrain beyond normal render distance. | It changes rendering workload and visual representation; do not assume it is compatible with Voxy or every shader/render mod. |
| [Voxy](https://github.com/MCRcortex/voxy) | Alternative/different high-distance voxel/LOD renderer. | Treat as a separate rendering experiment/profile until compatibility is proven. Its coexistence with Distant Horizons is not established by the reference list. |
| [Accelerated Rendering](https://github.com/Argon4W/AcceleratedRendering) | GPU/compute-oriented rendering acceleration. | Requires a capable OpenGL 4.6 path in the checked project description. Hardware/driver coverage must be tested. |
| [Accelerated Recoiling](https://github.com/wiyuka-owo/AcceleratedRecoiling) | Optimizes entity collision/pushing/cramming calculations while attempting to retain vanilla behavior. | Low-level behavior patch; test multiplayer entity interactions and collision edge cases. |
| [TT20](https://github.com/snackbag/tt20) | TPS-oriented timer behavior changes intended to keep actions feeling responsive under low TPS. | This is not necessarily a semantics-preserving optimization: depending on configured hooks, elapsed/tick-based gameplay behaviors can change when TPS is low. Test crop growth, breeding, fluids, sleep, projectiles, and other timer-dependent systems rather than calling it purely performance-neutral. |
| [Mercuryium](https://github.com/tcgqimingxc/mercuryium-mod) | Performance-related project reference. | Exact current implementation, build, and compatibility were not established here; treat as unresolved pending source/JAR. |
| [Redirected](https://github.com/txnimc/Redirected) | Small memory/enum-array allocation optimization. | Multi-loader project with 1.21.1 support surfaced; verify the exact selected file. |
| [Neruina](https://github.com/Bawnorton/Neruina) | Error containment/recovery for ticking entities and block entities to prevent one broken object from repeatedly crashing the world. | A recovery mechanism is not proof that a mod bug is harmless; preserve crash/error reports and affected entity identity. |
| [MixinTraceReforged](https://github.com/txnimc/MixinTraceReforged) | Diagnostics for mixin stack traces and mod conflict investigation. | Developer/debug utility, not a runtime gameplay system. |
| [Chunky](https://github.com/pop4959/Chunky) | Pregenerates chunks to reduce first-visit generation spikes on servers. | Must be run with the intended worldgen mods/config active. |
| [Lazy Language Loader](https://github.com/ChachyDev/lazy-language-loader) | Reloads language data without rebuilding all resources when changing the client language. | Fabric-only in the current public project line; client-side. |
| [Shuttfup](https://modrinth.com/mod/shuttfup) | The current public project is named **Stfu** and targets client annoyances/performance/loading and UI noise. | Fabric/Quilt-oriented in the current indexed page. See possible duplicate/alias with the separate `Stfu` GitHub reference. |
| [Stfu](https://github.com/ItsFelix5/Stfu) | Appears closely related to the Shuttfup/Stfu project lineage. Its feature set includes disabling startup advice/warnings, toasts, narrator/Realms notifications, chat/particle/HUD noise, and other annoyances. | Do not install both URLs as separate mods without comparing mod IDs and exact files; they may be the same project or a fork/renamed distribution. |
| [No More Knockback Latency](https://modrinth.com/mod/no-more-knockback-latency) | Removes the historical ~150 ms knockback delay and can provide more immediate feedback. | Public compatibility is 1.20.1/1.20.6 and multiple loaders; the exact pack build must match. It affects player feel/network response, not only rendering. |

**Compatibility note:** Distant Horizons, Voxy, Sodium, Sodium add-ons, Flywheel, Accelerated Rendering, and other render-path mods should be tracked as an explicit compatibility matrix. A list of mod names alone cannot tell whether a given profile has one renderer, an experimental alternative, or a tested combined setup. Similarly, low-level mixin/tick modifications should each have a test suite covering their specific behavioral claim.

### 3.10 Remaining utility, content, and unresolved references

These references are still important to preserve, even where the current search pass did not establish a detailed implementation or exact supported build.

| Supplied reference | Best-supported classification | What remains to verify |
|---|---|---|
| [Optical](https://github.com/luccaPossamai/optical) | Unresolved project reference; likely in the client/rendering/visual category from its linked repository context, but not enough evidence was retrieved to state its exact job. | Read the selected release README and mod metadata; verify Minecraft version and loader. |
| [Rubberworks](https://github.com/hadron13/Rubberworks) | Unresolved industrial/material project. | Exact recipes, features, loader, and release. |
| [SteamPowered](https://git.tilera.org/Anvilcraft/SteamPowered) | Unresolved steam/technology project. | Exact branch and runtime compatibility. |
| [DenseOres](https://github.com/NewJumper/DenseOres) | Unresolved ore-generation/material project. | Confirm whether it changes ore density, drops, worldgen, or a separate processing mechanic. |
| [SeedDelight](https://github.com/HiedaCamellia/SeedDelight) | Agriculture/food-oriented project reference. | Exact feature and supported version. |
| [Resquake](https://github.com/polina4096/resquake) | Quake-style movement; described in §3.7. | Match Fabric build and server/client arrangement. |
| [Minecraft Camera Overhaul](https://github.com/Mirsario/Minecraft-CameraOverhaul) | Camera-motion/presentation change; described in §3.7. | Exact release and compatibility with other camera/movement mods. |
| [Countered's Smooth Steps](https://www.curseforge.com/minecraft/mc-mods/countereds-smooth-steps) | Movement/step interaction and presentation utility. | Exact feature details and selected build. |
| [Matmos Ambient Sound](https://modrinth.com/mod/matmos-ambient-sound) | Environmental audio; described in §3.8. | TCTCore version and compatibility with AmbientSounds/Sound Physics Remastered. |
| [Immersive Thunder](https://www.curseforge.com/minecraft/mc-mods/immersivethunder) | Weather audio/visual presentation. | Exact selected file, loader, and overlap with Cool Rain/Matmos. |
| [ReplayMod](https://github.com/ReplayMod/ReplayMod) | Replay capture/editing and cinematic observation tooling. | Supported versions and loader; check whether it is used for player-facing content capture rather than normal gameplay. |
| [Minecraft Smooth Scrolling](https://github.com/SmajloSlovakian/Minecraft-Smooth-Scrolling) | UI scrolling; described in §3.7. | Fabric version/file. |
| [Create Pressure Gauges](https://modrinth.com/mod/create-pressure-gauges) | Create fluid diagnostics; described in §3.1. | Ensure it is not just a reference for a future branch. |
| [Tidal](https://github.com/Icywhenhot/Tidal) | Project reference whose current indexed lineage may have changed name to Wavify; a beach/ocean tidal-wave effect is described by the current project page. | Confirm whether the Dawnbreak item corresponds to the old Tidal source, the Wavify project, or a particular fork. |
| [Create Storage NeoForge](https://modrinth.com/mod/create-storage-neo-forge) | Create/linked storage; described in §3.2. | Exact version. |
| [Extra Disks](https://modrinth.com/mod/extra-disks) | Refined Storage capacity extension; described in §3.2. | Exact file. |
| [Cable Tiers](https://modrinth.com/mod/cable-tiers) | Refined Storage capacity/throughput upgrade; described in §3.2. | Exact file. |
| [StockBridge](https://modrinth.com/mod/stockbridge(refinedstrotage%2Bcreate)) | Refined Storage/Create bridge; described in §3.2. | Exact file and source spelling/mod ID. |
| [Create Mobile Packages](https://github.com/timplay33/Create-Mobile-Packages) | Package-based physical distribution; described in §3.1. | Exact file and dependency. |
| [Create Repackaged](https://github.com/LIUKRAST/Repackaged) | Package logistics; described in §3.1. | Exact file and dependency. |
| [Create Factory Controller](https://github.com/nbcss/create-factory-controller) | Machine monitoring/control; described in §3.1. | Dashboard/gauge compatibility. |
| [Create Extra Gauges](https://github.com/LIUKRAST/CreateExtraGauges) | Gauge logic; described in §3.1. | Factory Controller interaction. |
| [Create Diesel Generators](https://github.com/george8188625/Create-Diesel-Generators) | Fuel production/energy; described in §3.1. | Exact fuel recipe set. |
| [Create The Factory Must Grow](https://github.com/DrMango14/Create-The_Factory_Must_Grow) | Industrial progression; described in §3.1. | Beta/alpha branch stability. |
| [Create Metalwork](https://github.com/AverageAnime/create-metalwork) | Metalworking and molten-metal chain; described in §3.1. | Recipe overlap and configs. |
| [Create New Age](https://gitlab.com/antarcticgardens/create-new-age/) | Electricity/heat; described in §3.1. | Dependencies per version. |
| [Create Pantographs and Wires](https://github.com/MisterJulsen/Create-Pantographs-and-Wires) | Electrified train line; described in §3.1. | Create 6 / DragonLib versions. |
| [Create Threaded Trains](https://github.com/MisterJulsen/Create-Threaded-Trains) | Train calculation threading; described in §3.1. | Thread-safety with all rail addons. |
| [Create Sifter](https://github.com/oierbravo/createsifter) | Mechanical/hand sifting; described in §3.1. | Mechanical's Lib and version. |
| [Create EZ Stock Ticker](https://github.com/cakeGit/Create-Ez-Stock-Ticker/) | Stock ticker UX; described in §3.1. | Current project name and ID. |

The “remaining” table intentionally includes repeated project references only where their original role has already been explained. In a real manifest inventory, each JAR/project should appear once; this report repeats a few for cross-reference rather than implying duplicate installation.

---

## 4. The three non-mod references in the uploaded file

The first three URLs are not entries in the 95-project mod list:

1. [Victor Taelin Gist](https://gist.github.com/VictorTaelin/91837951a5ce5b38f341ec1ba1df6449) — an algorithm/source reference. The supplied file does not state which Dawnbreak system it informed.
2. [Greedy and Backtracking](https://blackflow.co.uk/custom-software-development/greedy-and-backtracking/) — an algorithmic reference. It may be relevant to search/planning/solver design, but the list does not specify its exact intended application.
3. [Age of Engineering](https://www.curseforge.com/minecraft/modpacks/age-of-engineering) — a distinct modpack reference, not a Dawnbreak mod. It may have been included as progression research or inspiration.

These should be stored as design/research references with an attribution and a short “used for” note, rather than copied into the mod manifest.

---

## 5. What the list says about Dawnbreak's design identity

This is an inference from the set of projects, not a statement from a Dawnbreak README.

### 5.1 The central axis appears to be physical industry plus logistics

The Create cluster is unusually broad: base kinetic machinery, industrial fuel, metalworking, sifting, gauge logic, factory dashboards, stock tickers, mobile packages, trains, electrification, vertical belts, and storage bridges. It implies a pack where factories are intended to become large systems with observable production state and meaningful distribution.

This is more specific than “a tech modpack.” The combination points toward factories that can:

- extract or acquire raw materials;
- separate, wash, sift, crush, melt, cast, alloy, or otherwise refine inputs;
- power machines through rotational, fuel, electrical, and possibly thermal systems;
- monitor pressure, inventory, gauge state, and production levels;
- use logic or stock thresholds to control machines;
- package and ship goods across a rail/logistics network;
- feed remote inventories or separate settlements through an explicit network bridge.

That is a strong systems reference for the proposed game. A city could have districts that specialize in mining, primary refining, metallurgy, energy, agriculture, machine assembly, logistics, and defensive production. Its industrial UI should expose actual bottlenecks: missing input, blocked output, lack of power, low fluid pressure, train congestion, or a stock target that has not been fulfilled.

### 5.2 It appears to value measurable machine state

Factory Controller, Extra Gauges, Pressure Gauges, stock ticker UX, and Refined Storage stock management point toward an important design value: **automation should be inspectable**. The player should be able to see the thresholds and state behind an automated decision. That is compatible with the existing design goal of avoiding opaque autonomous agents.

For the game, this means automation rules should be modeled as declarative conditions and actions with a visible reason: “produce ammunition because the homebase reserve is below its minimum,” “reroute because the freight buffer is full,” or “suspend because the steam manifold is under pressure.” It should be possible to inspect the exact sensor inputs and the rule that fired.

### 5.3 Logistics may be physical, networked, or hybrid

Create trains and packages represent physical delivery with path capacity and travel time. Refined Storage represents network-addressable inventory. StockBridge and Create Storage bridge these concepts. This is a particularly useful design pattern for a game with multiple persistent homebases:

- A homebase has its own local warehouses and factory buffers.
- A trade/industrial network may expose selected inventories remotely.
- A physical expedition train/convoy still has cargo capacity, a route, a delay, and a chance of interception or loss.
- A shared network may move data/requests instantly but cannot magically make a resource appear at a remote battlefield unless a physical delivery path exists.

This prevents the familiar design problem where every base becomes an arbitrary view of one global inventory and geography stops mattering.

### 5.4 The pack balances technology against a living world and environmental presentation

Dynamic Trees, Dynamic Trees Plus, Boids, Boids Birds, Curious Mobs, Ecologics, Serene Seasons, TerraFirmaCraft, BigGlobe, Archipelago, Immersive Weathering, and the weather/audio mods show a separate interest: the world is not meant to be only a flat factory canvas. It has vegetation, climate, terrain, wildlife, and environmental character.

That has direct consequences for the tower-defense design. Clearing a forest could change sightlines and routes but also remove habitat and food. A quarry could supply ore but disrupt water/soil. A seasonal shift could change crop yields, river behavior, creature migration, or road maintenance. Wildlife should not only be hostile units to kill; their populations and behavior should respond to habitat changes.

### 5.5 Performance is treated as a system constraint, not an afterthought

The list includes memory, rendering, tick, network, culling, collision, replay, and mixin-debug tools. The likely lesson is that a rich modded environment demands careful performance engineering. For the proposed game, performance must be separated by simulation layer: active tactical entities, nearby ecology, distant world state, factory networks, and persistence/streaming. High-distance rendering should not force all distant creatures and machines to tick at the same fidelity as the active battlefield.

---

## 6. Compatibility risks and release archaeology

### 6.1 The actual pack is a product of exact files, not project names

A project page can support 1.21.1, but a particular file may support only Fabric; another file for the same project may support NeoForge. One add-on may require Create 6.0.9, while another branch targets Create 0.5.1. The mod name is not enough to determine whether two JARs can coexist.

Each variant must pin:

- Minecraft exact version (not only “1.21”);
- loader family and exact build (Forge, NeoForge, Fabric, Quilt);
- Java major version;
- exact Create version for Create add-ons;
- API/library versions (Flywheel, Ponder, Registrate, Architectury, Cloth Config, Mod Menu, Balm, Blueprint, etc., where actually required);
- server/client/both environment;
- worldgen datapacks and their load order;
- configs, tags, KubeJS/CraftTweaker scripts, and datapack overrides.

### 6.2 Loader-family collisions are visible in the reference set

Several entries have Fabric-oriented compatibility in their current release ecosystem (Resquake, Krypton, Lazy Language Loader, Smooth Scrolling, and some Sodium add-ons). Others have clear NeoForge/Forge Create releases. This is not a reason to assume the pack is inconsistent; it is a reason to treat the references as multiple profiles or version branches until the actual manifests prove a single loader arrangement.

### 6.3 Some names likely refer to aliases, renamed projects, or duplicate distribution links

- **Shuttfup / Stfu:** the Modrinth slug and the GitHub source name may refer to the same Stfu project lineage or a fork/renamed distribution. Compare mod ID, version, and JAR digest before counting as two installed mods.
- **Tidal / Wavify:** the repository reference may point to a project that has since evolved or been renamed. Verify the exact release lineage.
- **Create More Vertical Additions / Create More: Vertical Belts:** the public project title surfaced in the latter form; compare exact file identity.
- **Create EZ Stock Ticker / Create Easy Stock Ticker:** public name appears normalized to Easy Stock Ticker.
- **Create Sifter / Create Sifting:** project title has evolved; use the file's mod ID/version.
- **Enhanced Tooltips:** multiple projects with similar titles exist. Match the link to the correct project ID, not the display name.
- **Boids Reforged / Boids and Boids Birds:** confirm whether the older project and current project are the same lineage and which dependency set is used.

### 6.4 Rendering and tick optimizers require compatibility baselines

A pack with Sodium, add-ons, Distant Horizons, Voxy, Flywheel, and GPU/compute experiments needs an explicit matrix. Do not interpret “more optimizers” as “more performance.” They may hook the same rendering paths, change culling assumptions, or require different loaders/drivers. Record at least:

- clean launch and world load;
- average and 1% low frame time in a fixed scene;
- memory use at startup and after long sessions;
- server MSPT/TPS under a fixed Create factory workload;
- entity/collision stress scene;
- chunk generation and view-distance load;
- Create contraption and train scene;
- shader/LOD interactions (if present);
- crash and mixin trace output.

TT20-like timing adjustments deserve a separate semantic regression suite because a lower perceived delay can be achieved by changing game-time behavior. Crop growth, breeding, fluids, TNT, sleep, projectiles, and other timer-driven mechanics should be compared against the unmodified baseline.

---

## 7. Translating Dawnbreak's systems into the proposed game

The strongest reusable concepts are not individual Minecraft blocks; they are the simulation contracts between systems.

### 7.1 Industrial graph

Represent each factory as a directed graph of sources, machines, buffers, and destinations. Each machine has explicit input/output slots, process duration, energy demand, heat/pressure constraints, and failure state. The graph should support:

- item throughput per tick or production interval;
- fluid volume and pressure loss;
- electrical load, generation, storage, and brownout behavior;
- mechanical power demand and capacity;
- thermal generation, transfer, and dissipation;
- machine state telemetry and event logs;
- stock thresholds and control rules;
- physical transport capacity and travel time.

Don't use one global “power” number for kinetic, electrical, thermal, and fuel systems. Conversion devices should own their conversion ratio and losses.

### 7.2 Warehouse and shipping model

Use two distinct abstractions:

1. **Inventory network:** a logical set of authorized storage nodes that exposes counts and can reserve/claim resources. This mirrors Refined Storage.
2. **Logistics graph:** physical edges with capacity, speed, queueing, blockage, cargo and route state. This mirrors belts, packages, and trains.

A remote homebase can be on the same inventory network only if the design explicitly grants it that connection. Physical deployment still needs a shipment/route. This preserves the significance of geography, raids, and defended corridors.

### 7.3 Living agriculture and plant morphology

A plant should have a persistent state, not just an age integer. Relevant state might include species, growth stage, branch graph, leaf canopy, moisture, nutrients, sunlight, temperature, biome compatibility, damage, and reproduction status. Soil has nutrient/water state; crop growth consumes nutrients; rain and season modify moisture; harvest can be destructive or non-destructive. These state dimensions fit the user's cuboid voxel shaper idea: a voxel profile provides the geometric growth envelope, while the plant's physiological rules determine which regions are occupied and when.

### 7.4 Ecology and behavior

Use separate individual and group behavior layers. A creature can track threat, prey, food, den, social bonds, and last-known position. Flocks/herds use a group-motion model, but individual decisions can break a flock when hunger, danger, mating, migration, or injury changes the local goal. A biome/season/resource layer supplies the environmental state the behavior consumes.

### 7.5 Tactical defense informed by a factory economy

A tower or defensive machine should be more than a purchased damage number. It might require a feedstock, ammunition, power, heat management, crew, line-of-sight, repair parts, and resupply. A turret can be powerful but idle because an ammunition belt is blocked; a high-energy cannon may force a local grid upgrade; an automated rail delivery can keep distant outposts supplied if the line remains secure.

### 7.6 Versioned worlds

A persistent world should store its generation profile, content registry, and compatibility contract. The save must know which worldgen algorithms, biome IDs, structure rules, block/entity definitions, recipes, and simulation semantics were used when it was created. Mod updates should be classified as:

- content-compatible (new content only, old IDs and rules preserved);
- data migration required (explicit conversion with a verifiable migration report);
- forward-only generation change (old chunks remain as-is; only new regions use a new profile);
- incompatible (refuse to load the world under that profile).

This is especially important if Dawnbreak has TFC, BigGlobe, Archipelago, Dynamic Trees, or other generator changes across releases.

---

## 8. Recommended next evidence collection for Dawnbreak

To turn this report into a true history rather than a reference-list analysis, the best artifacts are:

1. **One manifest per distributed Dawnbreak version.** CurseForge `manifest.json`, Modrinth `modrinth.index.json`, Packwiz `pack.toml` plus lockfile, Prism export, or whichever format was distributed.
2. **The actual mod JAR inventory for each variant.** Include file hashes; filenames alone may be ambiguous where projects fork/rename.
3. **The server pack and client pack separately.** This identifies client-only rendering/audio/QoL files and server-side simulation dependencies.
4. **Configs/scripts/datapacks.** These can be as important as the mod versions for recipes, world generation, Create processing balance, seasons, ecology, and performance.
5. **Release/change history.** Pack version labels, update notes, known issues, and worlds that were migrated or intentionally kept on older builds.
6. **Partner-owned public distribution/repository location if applicable.** I did not guess the partner's account or attribute unrelated DawnBreak listings to them.

With those records, we can compute a per-version mod diff, separate common/core mods from branch-specific mods, identify dependency and loader changes, and make an accurate graph of Dawnbreak's evolution. The current report deliberately does not claim that the 95 linked projects were all used in a single Dawnbreak release.

---

## 9. Source register and provenance

### User-provided source

- `references.md` uploaded in this conversation. The 95 mod/project links are the scope of the mod-list review; the first three links are separate research references. The source includes the note “Libraries are not included in this list.”

### Dawnbreak identity and false-positive checks

- [`alacripose/dawnbreak-coders`](https://github.com/alacripose/dawnbreak-coders) — repository metadata/history; no manifest/readme/version history was visible during this inspection.
- [The Voyager's Kit CurseForge page](https://www.curseforge.com/minecraft/modpacks/the-voyagers-kit/files/all) — unrelated 1.12.2 pack with historical “Dawnbreak” file names. It is cited only to explicitly exclude this false positive.
- Other search results for DawnBreak SMP, Steam's Dawn Break, and another mod's Dawnbreak Forest were also excluded as unrelated.

### Direct project pages and useful primary references

- [Create](https://github.com/Creators-of-Create/Create)
- [Create Diesel Generators](https://github.com/george8188625/Create-Diesel-Generators)
- [Create Factory Controller](https://github.com/nbcss/create-factory-controller)
- [Create Extra Gauges](https://github.com/LIUKRAST/CreateExtraGauges)
- [The Factory Must Grow](https://github.com/DrMango14/Create-The_Factory_Must_Grow)
- [Create New Age](https://gitlab.com/antarcticgardens/create-new-age/)
- [Create Pantographs & Wires](https://github.com/MisterJulsen/Create-Pantographs-and-Wires)
- [Create Threaded Trains](https://github.com/MisterJulsen/Create-Threaded-Trains)
- [Create Mobile Packages](https://github.com/timplay33/Create-Mobile-Packages)
- [Create Metalwork](https://github.com/AverageAnime/create-metalwork)
- [Create Sifting](https://github.com/oierbravo/createsifter)
- [Create Pressure Gauges](https://modrinth.com/mod/create-pressure-gauges)
- [Create Storage](https://modrinth.com/mod/create-storage-neo-forge)
- [Refined Storage](https://github.com/refinedmods/refinedstorage)
- [Extra Disks](https://modrinth.com/mod/extra-disks)
- [Cable Tiers](https://modrinth.com/mod/cable-tiers)
- [StockBridge](https://modrinth.com/mod/stockbridge(refinedstrotage%2Bcreate))
- [Refined Fluid Substitution](https://modrinth.com/mod/refined-fluid-substitution)
- [Step Crafter](https://modrinth.com/mod/step-crafter)
- [GregTech CEu Modern](https://github.com/GregTechCEu/GregTech-Modern)
- [Tinkers' Construct](https://github.com/SlimeKnights/TinkersConstruct)
- [BuildCraft](https://github.com/BuildCraft/BuildCraft)
- [Calculator](https://github.com/SonarSonic/Calculator)
- [TerraFirmaCraft](https://github.com/TerraFirmaCraft/TerraFirmaCraft)
- [BigGlobe](https://github.com/Builderb0y/BigGlobe)
- [Archipelago datapack](https://modrinth.com/datapack/archipelago)
- [Dynamic Trees](https://github.com/DynamicTreesTeam/DynamicTrees)
- [Dynamic Trees Plus](https://github.com/DynamicTreesTeam/DynamicTreesPlus)
- [Boids](https://www.curseforge.com/minecraft/mc-mods/boids-reforged)
- [Boids Birds](https://github.com/Leclowndu93150/Boids-Birds)
- [Ecologics](https://github.com/samedifferent/Ecologics)
- [Serene Seasons](https://github.com/Glitchfiend/SereneSeasons)
- [Immersive Weathering](https://github.com/Silversmith-Mods/Immersive-Weathering)
- [AmbientSounds](https://github.com/CreativeMD/AmbientSounds)
- [Cool Rain](https://github.com/Leclowndu93150/Cool-Rain)
- [MAtmos](https://modrinth.com/mod/matmos-ambient-sound)
- [Biome Music](https://github.com/someaddons/biomemusic)
- [Resquake](https://github.com/polina4096/resquake)
- [Effortless Building](https://github.com/huskuraft/effortless)
- [Personality](https://github.com/team-abnormals/personality)
- [Sodium](https://github.com/CaffeineMC/sodium)
- [Lithium](https://github.com/CaffeineMC/lithium)
- [FerriteCore](https://github.com/malte0811/FerriteCore)
- [ImmediatelyFast](https://github.com/RaphiMC/ImmediatelyFast)
- [ModernFix](https://github.com/embeddedt/ModernFix)
- [Krypton](https://github.com/astei/krypton)
- [Distant Horizons](https://gitlab.com/distant-horizons-team/distant-horizons)
- [Voxy](https://github.com/MCRcortex/voxy)
- [Accelerated Rendering](https://github.com/Argon4W/AcceleratedRendering)
- [Accelerated Recoiling](https://github.com/wiyuka-owo/AcceleratedRecoiling)
- [TT20](https://github.com/snackbag/tt20)
- [Neruina](https://github.com/Bawnorton/Neruina)
- [No More Knockback Latency](https://modrinth.com/mod/no-more-knockback-latency)
- [Lazy Language Loader](https://github.com/ChachyDev/lazy-language-loader)
- [Stfu](https://github.com/ItsFelix5/Stfu)
- [Enhanced Tooltips](https://modrinth.com/mod/tooltips-enhanced)
- [Minecraft Smooth Scrolling](https://github.com/SmajloSlovakian/Minecraft-Smooth-Scrolling)
- [Consistent Block Drops](https://github.com/tnoctua/consistent-block-drops)
- [Agricultural Enhancements (your separate public mod repository)](https://github.com/alacripose/Agricultural_Enhancements/tree/1.21.1-neoforge)

### Research notes

- Compatibility values are time-sensitive; check the selected file's exact Minecraft, loader, and dependency versions before assembling a profile.
- “Supported” does not mean “tested together.” A mod's project page establishes its own release claims, not compatibility with every other project in this list.
- Version conclusions about Dawnbreak are inferences until release manifests or file inventories are provided.

### Additional Dawnbreak inventory verification (31 October 2026)

Two entries in the uploaded `references.md` list needed explicit rows in the mod analysis: **Particle Core** and **Just Enough Items (JEI)**. Particle Core provides client-side particle culling and configurable per-type particle reduction, with public loader variants including NeoForge and Fabric; JEI is the item/recipe browser used to inspect the large set of processing chains. For JEI, note the server-side boundary change: up through Minecraft 1.21.1 it can run client-only with some features disabled, while from 1.21.2 onward full modded recipe synchronization requires server installation. See [Particle Core's source](https://github.com/fzzyhmstrs/pc), [JEI's source](https://github.com/mezz/JustEnoughItems), and the public project compatibility notes. <Cite refs={['turn803362search1','turn803362search2','turn803362search3','turn803362search6']}/>


# Appendix: Dawnbreak, Rainworld: Coalescence, and the Coalescent Nights Community


---

## Project and community identity correction (31 October 2026)

This section supersedes any earlier wording in this report that treated **Coalescent Nights** as the name of a modpack or as a simple public rebrand of Dawnbreak.

### The three distinct identities

The project's current identity is best represented as three related but separate layers:

1. **Coalescent Nights — the community.** A small, multi-game gaming community whose main focus is Minecraft. It also supports *Don't Starve*, *Deadlock*, *Roblox*, *Team Fortress 2*, and other games. Community members can socialize across games and arrange community events.
2. **Rainworld: Coalescence — the Minecraft project.** This is the named Minecraft project at the centre of Coalescent Nights. The community description supplied by the project owner identifies it as the principal Minecraft focus. The exact spelling and punctuation above preserve the supplied project title.
3. **Dawnbreak — an earlier project stage.** The owner states that Dawnbreak advanced into the present Coalescent Nights ecosystem. In light of the clarification that Coalescent Nights is the community and Rainworld: Coalescence is the Minecraft project, Dawnbreak should be recorded as an earlier stage in the same project history—not automatically described as an earlier name for the community or as a confirmed one-to-one rename of the present Minecraft project.

The available evidence does not yet establish the exact transition mechanics between Dawnbreak and Rainworld: Coalescence. It may have been a successor pack, a renamed/reworked server project, or a broader continuation with multiple release branches. A per-release manifest or dated server announcement is required before choosing among those interpretations.

### Distribution and historical record

The owner reports the following archive pattern:

- **Google Drive:** versions of the Minecraft project were released/distributed here.
- **Coalescent Nights Discord:** most historical information about the project and community is held on the community server.
- **X:** the owner's account contains sparse public posts, so it is a supplementary public timeline rather than a complete development archive.
- **Local systems:** only sparse surviving files remain, so the local archive is incomplete.

This changes the interpretation of the earlier public-platform search. Failure to find a publicly indexed CurseForge or Modrinth project does not imply that the project was unreleased or did not have versioned distributions. Google Drive and Discord are the relevant primary archival locations according to the owner. The supplied permanent Discord invite is: <https://discord.gg/sQYHfY6Eyc>.

The invite page could not be inspected for channel contents through the available browser access. No Discord messages, channel names, file attachments, Google Drive archives, release manifests, or X posts have been independently read for this update. The statements in this section are therefore clearly attributed to the project owner's supplied description, not presented as independently verified Discord/Drive findings.

### Community structure and operating details

According to the community description supplied by the owner:

- Coalescent Nights is LGBTQIA+ friendly.
- Its Discord and Minecraft server are actively moderated and regularly updated.
- Members interested in helping create the Minecraft mod for the server can contact **@Cosmic** to request addition to the helpers team. This is a volunteer role and provides no compensation.
- A **build team** role is available for people who want creative build access on the server.
- Build-team, game-specific, and other self-assignable roles are managed through the Discord `#reaction-roles` channel.
- The community can organize events for people who want to play together, hang out, or chat.
- The Minecraft servers are hosted by **Monkestation**.

These points describe the community's stated organization and recruitment model. They should not be mistaken for details of the Minecraft mod's codebase, pack composition, or release history.

### Corrected research model

Future documentation should keep four records separate and cross-reference them by date and release identifier:

| Record | Proper subject | Primary evidence to collect |
|---|---|---|
| Community history | Coalescent Nights as a multi-game community | Discord server overview, dated announcements, role configuration, event posts, moderation/community policy revisions |
| Minecraft project history | Rainworld: Coalescence | Development discussions, feature plans, source repositories, issue threads, screenshots, server changelogs |
| Predecessor and release lineage | Dawnbreak and its successor stages | Google Drive release archives, dated ZIPs, launcher exports, manifests, mod JARs, changelogs, world/config migrations |
| Infrastructure history | Minecraft server hosting and operation | Monkestation service records or owner-provided invoices/panels where appropriate, server software/version notes, deployment and maintenance announcements |

For each Google Drive release, the ideal evidence bundle is the original archive, its Drive filename and timestamp, any accompanying changelog, `manifest.json` / `modrinth.index.json` / Packwiz lockfile or launcher export, client and server mod lists, configuration/scripts/datapacks, and hashes of the included JARs. A release index should record the exact release label and date, Minecraft version, loader, server/client split, world compatibility, and its predecessor release.

For Discord, the most valuable sources are the earliest project-origin messages, channel/topic descriptions, release announcements, patch notes, pinned messages, bug reports, discussions about the Dawnbreak-to-Rainworld: Coalescence transition, and messages identifying which Drive archive corresponds to each server deployment. Message timestamps and attachment links should be retained. Where possible, export the messages with author and channel IDs intact so that quotations and chronology can be attributed accurately.

### Revised confidence statement

**Owner-confirmed in this conversation:** Coalescent Nights is the community; Rainworld: Coalescence is its principal Minecraft project; Dawnbreak is an earlier stage in the same broader lineage; versions were distributed on Google Drive; most historical information is on the community server; the X and local archives are sparse; and the community structure/roles/hosting details listed above.

**Not yet independently inspected:** the contents of the Discord server, the actual Google Drive release files, the full X post history, exact release dates/version numbers, the complete mod lists per release, and the precise technical transition from Dawnbreak to Rainworld: Coalescence.

**Still supported by the earlier mod-list analysis:** the uploaded `references.md` is a useful project-reference inventory, but it is not a release manifest and cannot prove that all 95 mod/project links were installed together in one version. Exact pack composition must be reconstructed from the Drive archives or equivalent release artifacts.


---

## Version 6 synthesis record

**What changed from Version 5:**
- Reframed the Dawnbreak inventory as a comparative engineering case study throughout the main design argument rather than leaving its relevance largely in a late appendix.
- Explicitly separated Coalescent Nights (community), Rainworld: Coalescence (Minecraft project), Dawnbreak (earlier project stage), and Rain World (the independent ecological game reference).
- Added design detail for typed production/logistics graphs, hybrid inventory, machine telemetry, energy/fluid accounting, agricultural state, ecology–industry coupling, and versioned world persistence.
- Connected industrial supply chains to tower uptime, homebase resilience, enemy infrastructure attacks, and the economy.
- Added observability, migration, contribution-governance, and community-context considerations to the roadmap, risks, and prototype experiments.
- Retained the existing comparative design analysis, Deadlock/Rust/Secure Shell appendices, detailed Dawnbreak mod inventory, and evidence limitations.

**Evidence limitation carried forward:** the project owner's description establishes the community/project naming and the stated distribution/history context. The private Discord history and Google Drive release archives have not been inspected here. The 95-project reference inventory does not prove that all projects coexisted in one release. Exact Minecraft versions, loaders, file IDs, hashes, release chronology, and the technical transition from Dawnbreak to Rainworld: Coalescence remain to be established from release artifacts and dated community records.

# 20. Final resynthesis: one package ecosystem, one streamer, one client

## 20.1 The architectural decision that best unifies the research

The design now points toward a single content ecosystem that serves the simulation, the game presentation, the client shell, and the mod system. The key rule is not that every subsystem must use the same in-memory structure or be rendered by one giant module. The rule is that every installed or streamed contribution must share one authoritative package identity, one declared schema family, one admission and dependency lifecycle, one revision/receipt history, and one client-facing stream.

A tower definition and the tower's build-menu icon are not two packages. An enemy faction and its bestiary page are not two packages. A biome and the UI that reports its environmental state are not two packages. A production building, its recipes, its storage port, its model/material/audio assets, and its inspection view should be facets of one package contribution set. A UI-only mod is still a package; a gameplay-only mod is still a package; a mod that contributes both is one atomic package with both facets.

This directly addresses the user's concern about diverging UI and game package formats. The client should not ask whether a contribution came from a “UI package” or a “game package” and then launch a different installation, validation, versioning, or update process. It should consume one admitted package projection and hand typed contributions to the relevant internal consumer.

The recommended high-level shape is:

```text
                   one package declaration
      identity · schema · origin · dependencies · claims
         surface facets · gameplay facets · assets
                           │
                  one builder / grammar
                           │
                 server-owned accept edge
                           │
           dependency resolution + trust policy
                           │
                one mounted package set
                           │
                 durable mount receipts
                           │
                 one client-facing stream
           snapshots · revisions · deltas · assets
                           │
                 one client stream reader
                           │
                  one normalized model
                    ┌──────┴──────┐
                    │             │
              shell/UI views   game/world views
                    │             │
                    └──────┬──────┘
                           │
                  same package revision
```

The shell and the world renderer are separate presentation consumers because their jobs are different. They are not separate package ecosystems. The simulation is a third consumer on the authoritative server side. It consumes the package's gameplay definitions but remains the authority over state transitions and outcomes.

## 20.2 What must remain separate, and what must become unified

A single ecosystem still needs precise boundaries. The distinction is between multiple *roles* in one lifecycle and multiple competing *sources of truth*.

| Concern | Keep distinct? | Architectural rule |
|---|---|---|
| Package declaration | One owner | One canonical grammar declares all package facets |
| Server ingest frame | Yes, as a transport/admission envelope | Server-only; client never parses it |
| Admission/trust verdict | Yes, as an authority boundary | The enclave/server determines trust and policy; the client displays the verdict |
| Mount receipt | Yes, as a receipt | `surface.mount/2` records mount state; it is not the package payload |
| Client served stream | One | One versioned client-facing stream for packages, UI/game contributions, assets, notices, and revisions |
| Shell/window renderer | One | Built-in and mod windows use the same shell frame, focus, taskbar, actions, and toast stack |
| World renderer | One game renderer with internal subsystems | Built-in and mod world content resolve through the same presentation pipeline |
| Simulation | Authoritative server-side domain | The client cannot establish combat, economy, construction, loot, or world outcomes |
| Asset decoders/caches | May be specialised internally | Shared content identity, digest, cache invalidation, and stream lifecycle |
| Package formats for UI vs gameplay | No | Different typed facets, not separate formats or readers |
| Simulation state vs rendered geometry | Yes | Simulation/world truth remains canonical; geometry and UI are derived projections |

This clarification avoids a superficial “one format” implementation that merely places two independent schemas under one filename. The package grammar, identity, dependency resolver, stream sequencing, trust model, and revision lifecycle must really be shared.

## 20.3 Why the package must describe both world and interface contributions

The game's design already contains systems that naturally expose themselves in several places. A factory building can exist in the world, be selected on a map, be inspected in a side panel, appear in construction/build menus, produce output in an economy ledger, and be referenced by research/progression. If each surface uses a separately authored package, the system can drift: the UI can show a different cost than the simulation applies; an item icon can refer to a removed item id; an upgrade screen can expose an option no longer admitted by the server.

A single package declaration should be able to relate these contributions:

| Contribution | Authoritative package facet | Consumers |
|---|---|---|
| Tower | `gameplay` definition: range, target policy, damage/effects, cost, upgrade graph | Server simulation, world renderer, tower catalogue, inspection UI |
| Enemy | `gameplay` definition: movement, resistances, health, threat class, drop policy | Server encounter system, world renderer, bestiary, combat log |
| Building | `gameplay` and `economy` definitions: footprint, build cost, input/output ports, operating state | Construction validation, simulation, city view, factory inspector |
| Recipe | `economy` definition with exact typed inputs/outputs and process conditions | Server production simulation, recipe browser, machine UI |
| Biome | `world` profile: terrain/material/ecology/climate inputs and generation revision | World generator, minimap, weather/ecology views |
| Item | `gameplay` definition, inventory behaviour, loot/crafting references | Server inventory and reward systems, item renderer, tooltip, item catalogue |
| Window | `surface` declaration: title, lane/placement role, bounded nodes, actions and theme roles | Existing client shell and its taskbar/focus model |
| Mesh/material/audio/icon | `asset` references and interpretation metadata | Game renderer, UI renderer, sound layer, cache and stream service |

One package can carry all of these or only a subset. A content-only package does not need to declare a new window. A UI-only package does not gain permission to modify game state. A mixed package is tied to one version, origin, digest, dependency closure, admission decision, and update transaction.

## 20.4 A mod is a package, not a separate mod subsystem

Dawnbreak's reference list illustrates how large a mod ecosystem becomes when many domains overlap: Create's machines and logistics, Refined Storage's networks, GregTech's processing, Tinkers' materials, agriculture, wildlife, seasons, weather, terrain, audio, world generation, and performance infrastructure. These are not only independent gameplay features. They all produce user-facing information, world assets, dependency requirements, and runtime resource needs.

The proposed game should not recreate the familiar but fragile arrangement in which each mod loader or content subsystem invents its own package registry and UI bridge. Instead:

1. Every mod declares one package identity and its contribution facets.
2. Dependencies, conflicts, required engine/game API, and capabilities are resolved before activation.
3. The server admits or refuses the package and records the result.
4. The package's content and UI facets join the same mounted set.
5. The same stream revision is delivered to the client.
6. The client projects its windows through the existing shell and world content through the existing game renderer.
7. The simulation consumes the authoritative gameplay definitions on the server.
8. A package update replaces the prior revision atomically; it never leaves UI and simulation on different revisions.

This also makes server/client split explicit. The server may admit a mod's simulation definition while the client receives the declared render assets and UI facets. The client does not decide that a locally available mod is active in a multiplayer world. The session's admitted package set is authoritative.

## 20.5 `secure-shell` is the integration pattern, not the whole game runtime

The inspected `secure-shell` repository provides a useful package/stream foundation, but its current implementation is not yet the complete game platform. Its source contains:

- `dl-enclave.surface/1` as the selected surface declaration;
- `surface.mount/2` as a receipt format;
- `surface.patch/1` as a constrained, content-only surface patch;
- `secure-shell.ingest/1` as the server-side accept frame;
- `secure-shell.served/1` as a client projection for wishes/notices and window/node data;
- a client stream reader that currently refuses non-empty window sets with `stream-windows-open`;
- two declaration grammars, `server/decl.mjs` and the surface parser, which still need one authoritative ownership model;
- an unfinished client display fork and client witness surface.

The design should preserve each layer's job while completing the common path. The ingest frame can remain a server-only boundary. The mount receipt can remain a receipt. The served stream should become the single complete client-facing delivery contract. The declaration grammar must converge. The shell and game renderers should consume the same normalized client model.

The detailed sibling specification is [Secure Shell — Streamer-First Package Architecture](secure-shell-streamer-first-package-unification.md). It proposes the schema, stream records, package facets, security boundaries, update model, asset flow, acceptance tests, and open decisions for `secure-shell`'s `docs/` directory.

### 20.5.1 Do not turn `openlock.addons/2` into another permanent stream

`dl-openshell`'s `openlock.addons/2` path demonstrates useful implementation behaviour: live XML/CSS-to-node-tree conversion, bounded node rendering, package revision checks, atomic replacement, observer notices, and reusing the Openlock shell's own `OpenlockStream` path. That work is valuable as a display implementation reference.

However, if the target game receives UI contributions through `openlock.addons/2` while gameplay packages arrive through a secure-shell-specific stream, the original divergence is reproduced. The preferred approach is to port the rendering and interaction behaviour into the one display owner, then project canonical package surface facets into its internal window model. A legacy Openlock importer may exist at the server-side migration boundary, but the live client should not keep multiple independent package readers.

## 20.6 The authoritative simulation must consume the same package set

The streamer is not a way for clients to decide what exists in the game. It is a way to deliver a coherent, admitted package view to all relevant consumers.

The authoritative simulation uses the same package digests and content definitions that are presented to players. When a tower is built, the server resolves the contribution id against the active package set; it does not accept a client-supplied damage value or trust a UI button. When an enemy drops loot, the server applies the package's declared drop rules under the simulation's deterministic/randomness policy. When a recipe runs, its declared mass/energy/fluid inputs and outputs are applied to authoritative ledgers. The client asks for actions using typed ids and expected revisions; the server validates and commits them.

This is especially important for the persistent city/economy/defense loop. A factory's UI should read the server's actual input buffers, throughput, energy draw, maintenance condition, and output state. It must not maintain an independent “UI inventory” that can drift from the logistics simulation. A tower's displayed ammunition, power reserve, firing rate, and repair state should be a projection of the same machine/city records used by combat and economy systems.

## 20.7 Carry forward `ala-cities`: canonical truth, derived assets, bounded streaming

The strongest connection between the city-builder research and secure-shell is the source-versus-projection boundary.

`ala-cities`' 3D branch demonstrates a world model based on signed coordinates, seeded derivation, typed deltas, material accounting, and explicit chunk identity. The asset generator produces octree-backed raycast data and indexed greedy meshes from the authoritative world. The `ChunkAssetStreamer` schedules builds under a configured per-tick budget and bounded cache. It does not own, mutate, or generate canonical world truth.

That should become the game-content rule as well:

- package declarations define simulation facts and references;
- the authoritative world defines current world facts;
- stream records expose admitted definitions and current state;
- mesh/texture/animation/audio/UI projections are derived and cacheable;
- cache identity covers every input that affects the result;
- updates and cache invalidation never rewrite the world ledger.

The branch's September 24 benchmark is informative but not a release gate: it reported a 5.139 ms p50 cold starter chunk, 0.219 ms warm cached chunk, 33.154 ms cold cubic preview, and 440.145 ms p50 for a 27-chunk sequence with one build per tick. It used a single Ryzen 7 5700G host, a dirty development branch, and no process-restart replay, resident-set/GPU-memory high-water, or live runtime-client measurement. These values should not be copied as budgets. The architectural lesson is deterministic bounded work, not a performance claim.

## 20.8 One package must serve shell UI, world rendering, and presentation tools

The game's UI should be understood as views over package/world data rather than separate content stores. The same resource definition can appear in the world, build palette, map, inventory, loot window, inspection view, and post-run history.

The shell remains the only owner of general window mechanics: frames, taskbar/dock, focus, close/minimize/resize, notifications, and contextual tool surfaces. The world renderer remains the only owner of world geometry, camera-space presentation, particles, and simulation-derived visual state. A mod supplies content and approved actions. It cannot introduce a parallel shell or renderer.

This preserves the strengths of the existing UI research without inheriting the format proliferation that occurred as individual Openlock features were added at different times. Themes, forms, and content are still distinct concerns: a package may select an approved theme role/form token, but it does not own the global shell vocabulary.

## 20.9 Executable mods need a separate trust decision, not a second package format

“Everything through the streamer” should not be misread as “download and execute arbitrary code.” The package identity and stream lifecycle can be unified while execution capabilities remain carefully restricted.

The first mod ecosystem should prefer declarative definitions, bounded formulas, state machines, event/action tables, and server-registered behaviour implementations. A package may declare that it needs a capability, but the server checks whether it is granted and whether the actor is allowed to exercise it. A UI button that says “build tower” is only a command request, not authority to create a tower.

If downloaded logic is later necessary, it should be a typed facet in the same package, but execution must wait for an explicit sandbox/runtime decision, import allowlist, CPU/memory quota, deterministic replay policy where needed, and fault containment. The current `secure-shell.ingest/1` `code` kind is reserved; it should remain so until there is a real reader and security model.

## 20.10 Package updates and save compatibility are part of persistence

A persistent world needs to know which gameplay definitions it was built with. Save metadata therefore pins the active package set by exact digest and records the simulation schema, ruleset, world seed/generator revision, and any migration lineage. Reconnecting a client requires reconstructing the same admitted package set and replaying the stream from a valid sequence or full snapshot.

A visual-only update may be compatible with an existing save if the package declares that it changes no authoritative simulation semantics and the relevant evidence supports that classification. A gameplay update that changes recipe output, tower damage, enemy behaviour, resource distribution, or world generation cannot be treated as a harmless UI refresh. It requires an explicit save-compatibility/migration decision or a named refusal/unresolved state.

This avoids the common failure where the UI updates to new content but an existing world silently retains a different ruleset, or a client uses a new icon/tooltip to describe an old server definition.

## 20.11 Performance should be solved through budgets, not by splitting formats

One stream does not mean one giant payload or synchronous decoding of everything at once. The stream carries bounded records, content-addressed resources, and chunked asset data. The client schedules parsing, decompression, upload, mesh builds, and UI tree construction under explicit budgets. A large package cannot block the input loop or pause a game tick for seconds.

Different queues and decoders are allowed internally: a visible UI icon, a world mesh, ambient audio, and background terrain need different priority and device upload paths. They share package identity, digest validation, dependency logic, and stream resumption. Internal scheduling specialisation does not create a new public format.

## 20.12 Revised implementation order

The previous roadmap remains directionally valid, but content breadth should follow the unified package path rather than precede it. The recommended order is now:

1. Record the package/stream/display ruling and converge the declaration grammar.
2. Prove one built-in package can be built, admitted, mounted, served, rendered, and witnessed by the actual client.
3. Prove one mixed mod that adds a gameplay definition, assets, and a UI contribution through that exact path.
4. Connect authoritative simulation to the same package definition and typed command boundary.
5. Add world snapshots/deltas and digest-addressed chunk/resource streaming with bounded work.
6. Add the larger tower, enemy, item, recipe, factory, ecology, agriculture, climate, and world-generation facet catalogue.
7. Expand mod dependencies, updates, save migration, and optional logic only after their gates exist.

This sequence intentionally moves the first mixed UI/game mod ahead of broad content volume. The project should prove that its architecture does not split before accumulating hundreds of towers, items, machines, and panels.

## 20.13 Revised acceptance model

An implementation is not complete because a package archive exists or a parser test passes. The end-to-end proof must show that one package revision is the source for every layer:

| Proof | Required observation |
|---|---|
| Package source | One declaration contains the UI and gameplay contribution facets |
| Builder/acceptor agreement | Same bytes produce the same canonical rows and digest |
| Admission | Server/enclave verdict is recorded and dependencies resolved |
| Mount | One receipt identifies the exact package revision and receiver |
| Stream | Client receives the full mounted view with sequence and content digests |
| Client model | One reader resolves the package into one normalized contribution set |
| UI | Mod window is framed, focused, and navigated by the existing shell |
| Game | Tower/enemy/item/etc. is consumed by the authoritative simulation |
| Assets | Both UI and game renderers resolve the same package-owned resource digests |
| Update | One atomic revision replaces UI and simulation contributions together |
| Failure | Tampered asset, missing dependency, unsupported facet, or stale revision is refused by name; prior revision stays active |
| Replay | Reconnect/restart reconstructs the same package set and presentation state from receipts/revisions |
| Witness | The actual client surface records the command, exit status, check count, surface id, output digest, and ledger event |

## 20.14 Open questions carried forward

The following remain explicit decisions, not assumptions embedded in the implementation:

- whether the package declaration remains `dl-enclave.surface/1` with an approved expansion or advances to a new unified schema version;
- how the current `server/decl.mjs` grammar and `surface.mjs` become one canonical grammar;
- whether `secure-shell.served/2` is the approved successor for the full client stream;
- the exact internal window model expected by the Openlock display fork;
- which legacy Openlock package formats may be imported at the server boundary;
- the first supported scope for executable mod logic;
- the granularity of package capabilities and server grants;
- which facets/assets are required for atomic mount and which can be optional streamed resources;
- how package updates that alter simulation semantics are migrated in existing worlds.

The existence of a technically attractive solution does not close a user-owned decision. Until the decisions are recorded, implementations that depend on an open layer remain blocked.

## 20.15 Final design thesis

The game should be built as one persistent simulation with one content ecosystem. The streamer is the delivery and lifecycle boundary for all admitted content; the package is the unit of identity, dependency, trust, versioning, and compatibility; the client model is the shared projection for both shell and world presentation; the simulation remains authoritative and independently verifiable.

There can be many kinds of content and many internal consumers. There should not be many competing package systems. A mod that adds a tower and its selection panel, a factory and its recipe browser, a biome and its map display, or an enemy and its bestiary entry should be one package travelling through one stream and one revision lifecycle. That is how the game can grow into a large modded ecosystem without its UI, simulation, assets, and package manager slowly becoming different games.

**Related commit-ready specification:** `secure-shell-streamer-first-package-unification.md`, intended for `secure-shell/docs/STREAMER-UNIFICATION.md`. This document proposes the architecture; it does not claim to have committed or implemented it in the secure-shell repository.

---

## Version 8 synthesis record

**What changed from Version 7:**

- Added streamer-first package unification as the main architectural synthesis, linking the tower-defense/city simulation, mod ecosystem, Secure Shell, `dl-openshell`, and `ala-cities` evidence.
- Reframed built-in game content, mod content, UI windows, gameplay definitions, and assets as typed facets of one package identity and one admitted revision lifecycle.
- Distinguished server ingest, mount receipts, the served client stream, normalized client data, simulation truth, and derived rendering assets so they are not confused or implemented as parallel package formats.
- Added a unified update/replay/dependency model, server-authoritative command boundary, asset chunking/cache identity requirements, and explicit security constraints on executable mods.
- Added a staged implementation order that proves one mixed UI/game mod before scaling the content catalogue.
- Added a sibling secure-shell specification intended for `docs/STREAMER-UNIFICATION.md`.

**Evidence boundary:** the unified streamer design is a synthesis and proposal based on inspected repository sources. It is not evidence that the secure-shell client currently mounts and renders full window/game package records. In the inspected source, `secure-shell.served/1` still carries wishes/notices and its client reader refuses non-empty windows. The package-format and display-contract decisions remain user-owned and must be recorded before implementation. The secure-shell repository was not edited or committed in this session.

# Part V — Comparative design-reference pass (Version 9)

## 21. Research scope, method, and transfer rules

This pass studies the requested games as design references for the proposed persistent city-building tower-defense ecosystem. It is not a ranking, a review scorecard, or a suggestion that the proposed game should reproduce all of these titles. A successful reference study asks what a game is unusually good at making the player understand, choose, build, risk, remember, or discover, and then identifies the smallest transferable system that creates that effect.

The requested set is unusually broad. It includes survival city-builders, a long-running live-service sandbox, tower defense, roguelikes, social creation platforms, systemic shooters, immersive survival games, open-world RPGs, voxel construction systems, and a detailed action-hunting game. These references should not all be applied to the same layer:

- **World and society simulation:** Frostpunk, Cities: Skylines, SimCity 2000, Sim Companies, the tycoon lineage, Elite Dangerous.
- **Combat and defense composition:** Bloons TD, Wildfrost, Noita, Batman: Arkham City, Monster Hunter: World, 7 Days to Die.
- **Encounter, mission, and pacing design:** Grand Theft Auto Online, Left 4 Dead 1/2, R.E.P.O., No More Room in Hell, Cyberpunk 2077, Fallout: New Vegas.
- **Exploration and spatial navigation:** Subnautica, Elden Ring, Fez, Hollow Knight, Skyrim, Abiotic Factor, Monster Hunter: World.
- **Creation, customization, and user-generated content:** The Sandbox and The Sandbox Evolution, VRChat, Robocraft, MachineCraft.
- **Incremental combination and production optimization:** Nubby's Number Factory, Sim Companies, Bloons TD, Wildfrost, Noita.

### 21.1 Evidence discipline

Descriptions of features and version history are tied to official product pages, developer interviews, official patch notes, documentation, or specialist reporting where available. When early releases are no longer maintained or their primary documentation is unavailable, the chronology is identified as a reconstructed historical account rather than as a current developer specification. The original game, its sequel, a remaster, a platform port, and a later live-service build are not treated as interchangeable.

The analysis uses three labels conceptually:

1. **Reference observation:** what the released game or its developer documentation supports.
2. **Design inference:** what the feature implies about player motivation, readability, pacing, economy, ecology, or production.
3. **Proposed transfer:** how the idea could be adapted to this project without copying a game's protected expression or inheriting its weaknesses.

### 21.2 The transfer test

A reference is worth adopting only if it answers all of these questions:

- Which player decision does it improve?
- Which authoritative state changes because of that decision?
- How can the player observe the result and understand the cause?
- What prevents the mechanic from becoming an exploit or an unbounded combinatorial system?
- Can it be represented in the existing package and streamer architecture?
- Does it reinforce the persistent settlement, connected world, tactical defense, or community-creation premise?

A strong visual reference can still be a poor simulation reference. A strong combat mechanic can still be a poor city-management mechanic. The goal is a coherent ecosystem, not a museum of borrowed mechanics.

---

## 22. Frostpunk and Frostpunk 2 — society survival, industrial form, and dieselpunk evolution

### 22.1 What the original game is doing

*Frostpunk* is not simply a city-builder with a cold-weather modifier. It is a society-survival simulation in which spatial planning, heat, resource stockpiles, labor, time, law, hope, discontent, and narrative events converge on one problem: the settlement must remain viable through a sequence of increasingly severe constraints. The city is a physical diagram of its survival dependencies. The generator is not decorative machinery; it is the centre of the heat network and the visual expression of the city's single most important dependency.

The circular crater, concentric street pattern, and radial heat zones make the rule legible before the player understands every number. Buildings closer to the generator benefit from heat coverage; the player sees the settlement grow outward from a shared lifeline. The game's spatial language and its interface are aligned. The console edition's radial UI was explicitly described by the lead designer as an extension of the radial city concept, rather than an unrelated menu treatment. [Xbox Wire: radial-driven gameplay and controls](https://news.xbox.com/en-us/2019/09/20/frostpunk-console-edition-radial-driven-design-of-gameplay-and-controls/)

### 22.2 The industrial aesthetic: why it works

The original *Frostpunk* is often described as steampunk, but the developer's own explanation is more specific and more useful: the strongest visual reference was industrial architecture across the world, with selected Victorian architectural cues, rather than Victorian nostalgia alone. Its style is built from forms that look engineered to survive industrial workloads: huge steel structures, riveted housings, pipes, bridges, cranes, pressure vessels, workshops, chimneys, scaffolding, and dense mechanical assemblies. [PC Gamer interview with 11 bit studios](https://www.pcgamer.com/frostpunk-developers-on-hope-misery-and-the-ultimately-terrifying-book-of-laws/)

The aesthetic works because the machines make the simulation's costs visible. A generator consumes fuel and produces heat; furnaces and chimneys make energy use visible; buildings are dirty and frost-bitten; citizens are small against the infrastructure. The city does not look prosperous merely because the resource bars are green. It looks like a precarious machine kept alive through labor and fuel.

The palette reinforces the same thesis. Cold steel blues and bleached colours dominate; warm light is scarce and therefore meaningful. The daily cycle's brief morning brightness is a perceptual relief, while cold UI surfaces, coal-like event panels, frost at the screen edges, particles, dirt and sharp ice details make the world feel hostile at every scale. This is not random grime. It creates a visual hierarchy in which heat, shelter and working machinery become focal points. [PlayStation Blog: Frostpunk art and The Last Autumn](https://blog.playstation.com/2021/07/07/how-frostpunk-reimagined-its-world-in-upcoming-prequel-expansion-the-last-autumn/)

### 22.3 Steam-age survival versus Frostpunk 2's dieselpunk futurism

The distinction matters. The first game is dominated by a nineteenth-century industrial survival vocabulary: coal, steam, Victorian-derived architecture, heavy ironwork, and a single central generator. *Frostpunk 2* is set after the immediate survival crisis, when oil and industrial expansion become central. Its visual language shifts toward larger, denser, more monumental structures: steel and stone buildings, sealed façades, streamlined forms, expansive districts, transport corridors, and a city that is no longer organised solely around a single heat engine.

The sequel's creative directors connected its new visual direction to the post-World-War-I period and interwar futurism: a society proud of engineering progress, attracted to streamlined modern forms, but still capable of repeating the destructive patterns that caused earlier catastrophe. This creates a useful form of dieselpunk: oil-powered industrial scale and interwar futurist optimism under the shadow of authoritarian politics, environmental extraction and social conflict. The project's own concept artist described the visual work as a frost-covered dieselpunk world. [Epic Games Store interview with the Frostpunk 2 directors](https://store.epicgames.com/news/frostpunk-2-exclusive-interview-leave-your-fingerprint-on-a-frozen-world); [Frostpunk 2 concept art by Tsu-Wei Chen](https://chenzoowe.artstation.com/projects/3EeBgo); [11 bit studios announcement describing oil as the new strategic foundation](https://ir.11bitstudios.com/en/news/11-bit-studios-s-a-works-on-frostpunk-2/)

The first game's city is an emergency life-support system. The sequel's metropolis is an extractive, political and industrial organism. The change in scale is accompanied by a change in time: individual buildings and day-scale emergencies give way to districts, population groups, longer planning horizons and political blocs. The sequel uses city growth to express the changing problem, rather than merely making the original map larger. [Epic Games Store interview](https://store.epicgames.com/news/frostpunk-2-exclusive-interview-leave-your-fingerprint-on-a-frozen-world)

### 22.4 The Book of Laws and policy as mechanics

The Book of Laws turns governance into an explicit progression system. A policy is not just a passive modifier; it can solve an immediate shortage while changing labour, social expectations, hope, discontent, or the future events that become possible. Laws are therefore more interesting when treated as commitments with delayed consequences rather than isolated upgrades.

The important pattern is a **policy consequence graph**:

- a present problem creates pressure for a policy;
- the policy creates a short-term operational effect;
- its social consequences change the city's future risk profile;
- later events test whether the earlier decision was worth its cost;
- the player must govern the system they created, not reset to a morally neutral baseline.

A player should not be able to buy a permanent positive modifier without making a meaningful trade. At the same time, the game must avoid pretending that every choice is equally consequential. The player needs enough feedback to infer why the settlement changed.

### 22.5 Transfer to the proposed game

Adopt the original game's visual-to-mechanical alignment: the largest and most recognisable structures should be the actual critical dependencies of the city. Power, heat, water, food, transport, defence, communications and waste handling should have visible infrastructure, visible failure states and spatial effects.

Adopt the sequel's change in industrial language as a technological progression model. Early settlement machinery can be exposed, repairable, smoky, rough and steam-driven. Mature industry can become larger, enclosed, more efficient, more automated and more politically consequential. A technological age should change silhouettes, material palettes, exhaust, lighting, noise, worker routes and the shape of districts—not just replace one icon with another.

Use policy as a typed, versioned ruleset with declared effects and conditions. A labour law, rationing rule, militia policy or industrial regulation should be inspectable and should record its effects in the city ledger. Modded laws must enter the same package/stream lifecycle as the buildings and UI that represent them.

**Do not copy:** Frostpunk's exact generator, circular crater, fictional laws, factions, character events, or signature architecture. The transferable pattern is the alignment of infrastructure, policy, emotional tone and spatial form.

---

## 23. Grand Theft Auto Online — free-roam as a persistent activity layer and missions as a content ecosystem

### 23.1 Free-roam is the connective tissue

*GTA Online* is best studied as a persistent shared world that hosts multiple kinds of activities, not simply as an open-world action game. Players exist in a common free-roam space with vehicles, properties, shops, social encounters, random events, jobs and player-created activities. A mission or business activity can move the player out of free-roam into an instanced or controlled activity, then return them to the persistent world with money, unlocks, stock, reputation or new access.

This produces two different kinds of play:

- **ambient free-roam:** travel, social contact, exploration, emergent conflict, optional activity discovery, collecting and world interaction;
- **directed activity:** contact missions, adversary modes, races, survivals, business sourcing and selling, heists, story missions, and creator-made jobs.

The shared world makes travel and chance encounters part of the experience. The job framework provides structure and repeatability. The free-roam layer is not a replacement for missions; it is the social and spatial environment that connects them.

### 23.2 Mission grammar and progression

Many of the game's larger criminal-business activities use a staged structure: acquire an enabling property or business, access a planning interface, perform one or more preparation tasks, launch a finale, divide rewards, then invest in the next capability. The heist format is especially instructive because it gives the player an explicit planning phase and an explicit execution phase. Players can coordinate roles and equipment before the high-risk finale. Business activity provides a different cadence: gather/source inputs, maintain or defend stock, transport or sell output, then reinvest proceeds.

The activity catalogue is intentionally varied. A good mission structure does not require every mission to be a gunfight. It can be a race, pursuit, stealth infiltration, vehicle delivery, collection task, survival encounter, escort, heist setup, or multi-stage finale. The common contract is that every activity has a readable objective, a completion condition, a failure condition, a payout, and an ownership/host/matchmaking rule.

The 2026 Mission Creator expands this into user-generated mission logic. Rockstar's 2026 patch notes describe configurable actors, objectives, tasks, paths, interactables, cameras, cutscenes, time-of-day progress, mission flags, score limits, team progress and controlled stealth behavior. The 2026 Kortz Center Heist also demonstrates a prep-to-finale structure that launches from owned infrastructure or through a phone call in Freemode. [Rockstar Mission Creator announcement](https://www.rockstargames.com/newswire/article/39a3412379o79o/play-thrilling-new-gta-online-experiences-built-by-the-community-with); [Rockstar Title Update 1.73 notes](https://support.rockstargames.com/articles/4vRqEDvjUs9h7nqRUgc8YO/gtav-title-update-1-73-notes-ps5-ps4-xbox-series-x-or-s-xbox-one-pc-enhanced-legacy)

### 23.3 How the versions differ

GTA Online is not one identical build running everywhere. Its platform history affects content, player population and continuity.

| Generation / release family | Important distinction | Design consequence |
|---|---|---|
| PS3 / Xbox 360 launch generation | Original 2013-era online branch. Rockstar ended online services on 16 December 2021. Character progression could not be migrated from this branch to current generations. | A historical first-generation live world; not an active compatibility target. |
| PS4 / Xbox One | Expanded generation that continued to receive major content and represents the longer-lived last-generation console branch. | Broad shared free-roam and mission catalogue, but without some later-generation exclusive features. |
| Original PC / GTAV Legacy | PC branch that continued alongside the later PC Enhanced release. | Legacy PC players remain separated from Enhanced PC players in matchmaking. |
| PS5 / Xbox Series XS | Expanded & Enhanced generation with additional performance/visual capabilities and features such as Career Builder and HSW content. | More advanced feature surface and a different onboarding economy. |
| PC Enhanced (March 2025 onward) | Free upgrade for existing PC owners, adding many features previously associated with current-generation consoles, including ray tracing options, HSW content, GTA+ access and modern performance features. Legacy remains available as a separate branch. | PC users must select the matching branch to play with friends; the same nominal game now has a platform-specific client contract. |

Rockstar's support page records that PS3/Xbox 360 services ended on 16 December 2021 and that no character transfer from those systems was possible. Its PC upgrade announcement states that Enhanced and Legacy are separate versions and separate online populations. Rockstar's migration rules also state that PS4/Xbox One profiles can move to the corresponding current console family, but not between console families or from console to PC. [PS3/Xbox 360 service closure FAQ](https://support.rockstargames.com/articles/7dADpZ41gSg4Ma8LHO6dF3/end-of-online-services-for-playstation-3-and-xbox-360-versions-of-grand-theft-auto-v-max-payne-3-and-l-a-noire-faq); [PC Enhanced announcement](https://www.rockstargames.com/newswire/article/7551a7k917554a/free-upgrade-for-grand-theft-auto-v-on-pc-now-available); [character transfer rules](https://support.rockstargames.com/articles/6ahGxgqBAXzmfn9EL6IIir/grand-theft-auto-online-character-transfers)

Career Builder is another explicit difference: it is available on PS5, Xbox Series X|S and PC Enhanced, and lets a new or reset character select one of four business/criminal paths—Biker, Executive, Gunrunner or Nightclub Owner—with a one-time GTA$4 million starting allocation, of which at most GTA$1 million carries into play. [Rockstar Career Builder overview](https://support.rockstargames.com/articles/1UfYjMN0NnpJcoC5DLZkf4/gta-online-career-builder-overview)

As of 31 October 2026, Rockstar's official Title Update 1.73 notes include current-generation/Enhanced-only Mission Creator additions and the Kortz Center Heist. The update illustrates why version-specific analysis matters: a creator mission may rely on new interactables, interiors, warp points, task logic, actors, objective objects or cutscene options unavailable on older clients. [Rockstar Title Update 1.73](https://support.rockstargames.com/articles/4vRqEDvjUs9h7nqRUgc8YO/gtav-title-update-1-73-notes-ps5-ps4-xbox-series-x-or-xbox-one-pc-enhanced-legacy)

### 23.4 Transfer to the proposed game

Adopt the distinction between a persistent social world and an authored activity contract. The player should be able to roam, meet other players, see public events, inspect construction, visit markets and discover optional content without every action becoming a forced quest. Mission content should be launchable from physical places, a dispatch board, a terminal, a map marker or a player/community interface, with consistent rules for host, party size, reward, retry and world impact.

Use GTA's business loops as a reference for **operational production missions**. A factory can have sourcing, transport, protection, delivery and maintenance jobs, while the underlying production simulation continues to exist. Mission play should interact with persistent supply chains rather than create fake inventory in a separate activity instance.

Adopt player-created missions only through the unified package contract. A creator should author typed objectives and actors, not arbitrary unchecked client code. Test mode, memory budgets, dependency resolution, version compatibility, moderation and nomination/curation are first-class platform systems.

**Do not copy:** Los Santos, the criminal fantasy, vehicle catalogue, characters, branded properties, mission writing or proprietary map. Transfer the free-roam/activity separation, staged contract grammar, business loop, and creator-authoring lessons.

---

## 24. Bloons Tower Defense — six mainline iterations and the evolution of tower identity

### 24.1 What is being compared

The phrase “Bloons TD” covers six numbered mainline tower-defense games plus expansions and adjacent spin-offs. This pass treats BTD1 through BTD6 as the main progression, while noting *Bloons TD 4 Expansion*, *Bloons TD Battles*, *Bloons Monkey City* and *Bloons TD Battles 2* where they contribute distinct design lessons. It does not collapse all Bloons-branded titles into one ruleset.

### 24.2 Mainline evolution

| Game | Major design step | What the player is learning |
|---|---|---|
| Bloons Tower Defense (BTD1, 2007) | Small tower roster, one simple track, a fixed wave sequence and a clear life-loss condition. The compact rules make tower placement and basic damage roles the whole puzzle. | Spatial coverage, attack speed, range, damage and timing. |
| BTD2 (2007) | More tracks, difficulty options, additional tower choices and bloon types. The track becomes a variable challenge rather than a single tutorial-like lane. | Adaptation to topology and difficulty rather than repeating one solved layout. |
| BTD3 (2008) | New maps, new tower roles and more demanding bloon compositions, including the MOAB-class threat. The game adds threats that require different damage and crowd-control responses. | Counter selection and target prioritisation. |
| BTD4 (2009) | Larger tower roster, more substantial upgrade progression, saving, unlock progression, sandbox experimentation and Apopalypse continuous play. Water tiles introduce placement constraints and water-specific towers. | Longer-term account progression and testing builds outside the standard wave loop. |
| BTD4 Expansion (2010) | Adds towers such as the Dartling Gun and Spike Factory, additional tracks, Deflation, and tower-specialty choices. | Specialisation, constrained starting economies and alternative rule sets. |
| BTD5 (2011 onward, with later platform versions) | More developed tower roster and upgrade structure, two upgrade paths per tower, Special Agents, Monkey Lab, mission challenges, Freeplay, daily/random missions and co-op. Later editions accumulated additional maps and content. | Build identity, account-wide progression, repeatable missions and team coordination. |
| BTD6 (2018 onward) | 3D presentation, heroes, three upgrade paths per tower with path-selection restrictions, Monkey Knowledge, co-op, expanded challenge formats, boss events, Odysseys, Paragons and a large evolving map/tower catalogue. | Combinatorial strategy, hero progression, advanced build synergies and specialised high-difficulty solving. |

The historical sequence is corroborated by the series chronology and individual game records; the official Xbox BTD5 listing documents 21 towers, two upgrade paths, Special Agents, Monkey Lab, missions, daily challenges and co-op. The Epic listing for BTD6 documents 25 towers with three upgrade paths, heroes, Paragons, Monkey Knowledge, co-op and a large expanding map catalogue. [Bloons TD history](https://en.wikipedia.org/wiki/Bloons_Tower_Defense); [BTD5 product description](https://www.xbox.com/en-US/games/store/bloons-td-5/C3Z7H6LR7223); [BTD6 product description](https://store.epicgames.com/p/bloons-td-6-bf95a0)

### 24.3 Tower roles are a vocabulary, not a list of damage values

Bloons' strongest design contribution is the way tower roles create a legible vocabulary. Some towers are reliable basic attackers; some attack quickly; some hit multiple targets; some slow, freeze, glue, stun or otherwise control bloons; some detect camouflaged targets; some improve nearby allies; some generate income; some specialise in particular bloon properties. The upgrades deepen those identities rather than merely raising a single damage number.

BTD6's three-path structure creates a particularly useful constraint: players can pursue a primary path while taking limited cross-path benefits, but cannot buy every powerful path on one tower. The restriction forces identity and prevents a tower from becoming an all-purpose answer. Heroes add a different progression curve because they level during a match and often provide activated abilities or support effects. Paragons represent a capstone transformation that consumes multiple developed tower branches and changes the role of the investment.

### 24.4 Threat language and counter-design

The enemy roster communicates its own rules through silhouettes, colours, speed and layered armour. A new bloon type should not simply have more health. It may resist a damage category, move faster, regenerate, split into layers, conceal itself or arrive in a large high-threat carrier. This allows a game to create counterplay from the interaction between enemy attributes and tower capabilities.

The important rule for the proposed game is that counters must be visible and diagnosable. A tower that cannot damage an enemy should explain why. A unit that resists fire should communicate that through a material/trait indicator, an inspection record, combat log or effect feedback. The player should not have to memorise arbitrary exceptions.

### 24.5 Maps, game modes and meta-progression

The track is a readable spatial puzzle. Different path shapes, multiple lanes, water, line-of-sight obstructions, obstacles, elevation and difficulty rankings create distinct placement problems. Alternative modes then modify the contract: Deflation constrains money, Apopalypse changes the temporal rhythm, Sandbox enables experimentation, and higher-difficulty rule sets restrict lives, income or available units. BTD5's missions and BTD6's boss/odyssey/challenge ecosystem make the game repeatable beyond a single standard survival mode.

The account layer adds a second progression channel. A player can unlock towers, tower upgrades, powers, agents, knowledge and challenges over many sessions, while each individual map has its own in-match economy. This is an important split: persistent unlocks should give the player more options, not simply make the current run's economy irrelevant.

### 24.6 Transfer to the proposed game

Adopt the taxonomy of counter roles, targeting modes, upgrade paths, hero-like support units and special challenge rules. Towers should be defined by a role contract: targets, effective damage classes, range/geometry, firing cadence, control effects, resource use, support aura, and upgrade path. An upgrade should change a meaningful behaviour or tradeoff, not only add +5% damage.

Adopt multiple rule sets over the same battle map. A single map can support standard defence, low-resource challenge, endless survival, boss assault, escort, co-op, special enemy composition or player-authored scenario without each being a separate game package.

**Do not copy:** monkey characters, bloon taxonomy, exact upgrade names, tower artwork, hero designs or the original track catalogue. The transferable value is the disciplined vocabulary of tower roles, enemy counters, map geometry, challenge modes and long-term unlocks.

---

## 25. Nubby's Number Factory — stochastic production, quota pressure and item synergies

### 25.1 Core loop

*Nubby's Number Factory* turns a Plinko-like board into a production system. The player launches a spherical character through a pegboard, where the path and collisions generate numbers; the objective is to meet increasingly demanding quotas. More than fifty items can alter the production process and combine into unexpected output strategies. Failure is framed through surreal corporate horror: missing the quota means the sun explodes. [Steam product page](https://store.steampowered.com/app/3191030/Nubby%27s_Number_Factory)

The central design trick is that a probabilistic physical process is made to feel like a factory. The player is not manually adding numbers. They are changing the conditions under which a production event occurs. Board geometry, item selection, item combinations, multiplier logic and chance become the machinery of the player's output.

### 25.2 Why it is satisfying

The game combines three layers:

1. **Immediate readable event:** launch Nubby and watch the board resolve.
2. **Build construction:** choose items that alter the distribution or multiply specific outcomes.
3. **Quota deadline:** convert a fun experiment into a production requirement.

The player is encouraged to discover synergies, but the quota prevents indefinite experimentation. It creates a reason to test items and a deadline to turn the current build into reliable performance.

### 25.3 Transfer to the proposed game

This is a reference for production mini-games, salvage processing, research experiments, rare-drop conversion and factory optimisation—not a replacement for the main deterministic city economy. A player might tune a sorter, refinery, material synthesiser or probabilistic research apparatus through a compact interactive board. The output should still resolve into audited resource quantities and must not be an exploit that creates unlimited rare materials.

Useful transferable principles:

- make the production process visually legible;
- allow several items/modifiers to interact in a bounded rule set;
- give the player a quota, contract or deadline that makes output matter;
- let the player inspect which modifiers contributed to an outcome;
- let a build specialise in reliability, peak output, variance reduction, bonus yield or resource conservation.

Use stochastic production for optional high-variance systems. Keep essential city materials and authoritative crafting outputs deterministic or tightly bounded so players can plan and audit their supply chains.

---

## 26. Left 4 Dead and Left 4 Dead 2 — authored spaces, adaptive encounter pacing, and the AI Director

### 26.1 The Director is not a random level generator

The most important correction when studying the *Left 4 Dead* Director is that it does not replace level design. The maps, safe rooms, traversal routes, choke points, finales, set pieces, lighting cues and environmental composition are authored. The Director controls the population and pacing of threats and supplies within that authored structure. It observes team performance and adjusts intensity to create rises, rests and renewed danger. In a 2008 interview, Valve writer Chet Faliszek described the system as monitoring player behaviour and shaping action procedurally, while deliberately inserting breaks because constant action creates combat fatigue. [Valve interview](https://www.sknr.net/2008/10/14/left-4-dead-interview/)

This is the distinction to preserve: **authored topology, adaptive performance**. A Director that randomises every enemy and event without understanding the map can produce noise, not drama.

### 26.2 The pacing model

A useful abstraction is a tension curve:

- low-pressure traversal and observation;
- ambient threat or an early warning;
- rising horde pressure;
- a focused special-infected or obstacle event;
- a temporary safe interval;
- a crescendo such as a holdout, escape, alarm, bridge crossing or finale;
- a recovery interval in the safe room.

The system uses team condition and recent pressure to choose when to intensify or ease off. Health, incapacity, separation, ammunition and recent combat events are possible inputs to a pressure model. It should also consider whether the group is moving efficiently, stalling, splitting, or exploiting a route.

### 26.3 What Left 4 Dead 2 changes

Valve's discussion of *Left 4 Dead 2* describes additional pacing work, mini-finales, dynamic weather, ambient infected, and map components that can be selected differently on each run. The Director can therefore change not only the amount of combat but certain environmental configurations and atmospheric conditions. Lighting and texture cues also serve a navigational function because the game intentionally avoids filling the screen with directional HUD instructions. The art is doing both mood work and wayfinding work. [Game Developer interview with Valve's Mattie Swift](https://www.gamedeveloper.com/game-platforms/q-a-valve-s-swift-on-i-left-4-dead-2-i-s-production-ai-boost)

### 26.4 Mission and level structure

Campaigns are composed of connected maps that each have a start, a route, one or more major beats, and a safe-room exit. The entire campaign provides a larger arc, but each map has its own local rhythm. Safe rooms create a physical pause, resupply and checkpoint. Crescendo events force the team to make noise, hold position or cross a dangerous space. Finale events are recognisable high-pressure contracts with a clear completion condition.

The environments use silhouettes, lighting, signposting, sound, debris, blocked paths, landmarks and safe-room visibility to guide the player. Good level design gives the Director several valid places to stage threats without making every run feel identical.

### 26.5 Transfer to the proposed game

Adopt a Director-like **encounter scheduler** for expeditions, defence missions and open-world threats. It should not decide the entire world state. The world simulation decides which threats exist, where they are migrating from, what they want, and what routes are plausible; the encounter scheduler decides when and how those threats become an active playable encounter.

Inputs can include:

- player squad size and capability;
- current health, ammunition and defensive supplies;
- time since last major encounter;
- prior losses or success streaks;
- route progress and player clustering;
- mission objective state;
- local ecology, weather, noise and time of day;
- authored encounter anchors and maximum active threat budget.

Outputs may include threat intensity, ambient population, patrols, special-unit composition, supply placement, weather/visibility, audio mood, and alternative map configurations. Every output should respect the mission's authored safety and pacing envelope.

The Director should use bounded, explainable weights and a replayable seed. An outcome may be surprising, but it must be reproducible for debugging and must never override the world's authoritative ecology or spawn impossible enemies without a declared event rule.

**Do not copy:** the exact Director algorithm, infected roster, campaign layouts or audiovisual identity. Transfer the layered relationship between authored routes and adaptive pacing.

---

## 27. R.E.P.O. — physics as co-op communication, risk, and comedy

*R.E.P.O.* gives players a simple contract: locate valuable objects in dangerous environments, move them with a physics-based grabbing tool, and extract them without destroying too much value. It supports up to six players, proximity voice chat, scary creatures, and fully physical items. Even enemies are affected by gravity. [Steam product page](https://store.steampowered.com/app/3241660/R.E.P.O.)

Its most useful design idea is that physics becomes a social problem. A large or fragile object creates a coordination task. The team must decide who carries, who scouts, who clears a path, whether to move slowly, and whether a shortcut is worth the risk. An object can be valuable and still become a liability when its mass, shape or fragility makes the route difficult. The same physics system can produce fear, teamwork, slapstick and emergent stories without the designer authoring each anecdote.

The extraction contract adds stakes to ordinary object interaction. The object is not just scenery; it has a price, a condition, a destination and a failure cost. The team must transport it while preserving enough value to meet its objective. The upgrade shop then gives the proceeds a persistent use.

### Transfer

Use physical handling for salvage, large construction components, portable machinery, fuel drums, unstable relics, heavy weapon systems and co-op repair. A meaningful cargo item should have typed properties such as mass, centre of mass, fragility, dimensions, grip points, environmental sensitivity and damage state. These properties should be visible before a player commits to a difficult transport.

Use proximity communication and multi-person handling to create cooperative tasks. A two-player lift can require players to coordinate rather than simply press one shared button. A fragile object can encourage escort, clearing, route planning and careful pace.

Keep physics inside a clear contract: collision damage and value loss must be deterministic enough to be understood; networked physics must reconcile; and every cargo type needs a bounded cost in CPU/network simulation. Do not simulate every decorative object at full fidelity merely because a physics sandbox allows it.

---

## 28. Robocraft — modular machine authorship, functional geometry, and the risk of live-service dependence

### 28.1 Construction as vehicle identity

*Robocraft*'s central appeal was that a player assembled a functional machine from modular blocks and parts, then took that construction into competitive play. The build was not just a cosmetic skin: its geometry, mass distribution, armour, propulsion, weapons and component placement altered performance. The design connected construction directly to combat feedback. A vehicle could be recognisably self-authored while still operating under a shared physics and combat ruleset.

This is a powerful pattern for a tower-defense ecosystem: the player or modder should be able to assemble machines from parts whose functions are composable. A vehicle, turret, mining rig, drone or mobile base can be a structured collection of chassis, power, drive, sensor, weapon, storage and control components.

### 28.2 Build constraints are essential

The interesting part is not unlimited placement. It is the negotiation between a player's intended design and the system constraints: mass, power, heat, stability, turning, lift, thrust, armour, cost, component exposure and damage propagation. If every design is equally viable, construction has no strategic language. If the constraints are too strict, experimentation disappears.

### 28.3 Platform caution

Freejam's January 2025 announcement stated that production on *Robocraft 2* had ceased and the studio was closing; *Robocraft 1* and *CardLife* were to be delisted and their servers shut down during that week. This is a reminder that a creator-driven competitive ecosystem can be compelling but dependent on a sustainable operating model, server ownership, moderation and a path to preserve community assets. [Freejam Studio Closure announcement (20 January 2025)](https://steamcommunity.com/games/301520/announcements/detail/569242775933944077)

### Transfer

Adopt modular functional construction, visible physical constraints, build sharing, inspection, test rigs and combat replay. A machine definition should be data-driven and versioned. The player should be able to inspect which component gives it thrust, which provides power, what its centre of mass is, and how damage propagates.

A creator ecosystem should not depend solely on live central servers for players to preserve their own designs. Support package export, signed identity, version pins, and an offline or locally hosted test environment where feasible. Competition rules, rating, matchmaking and moderation must remain separate from the machine's canonical build definition.

---

## 29. The Sandbox and The Sandbox Evolution — elemental simulation and accessible creation tools

### 29.1 Disambiguating the name

This section refers primarily to Pixowl's original 2D pixel sandbox *The Sandbox* (2012) and its 2016 sequel *The Sandbox Evolution*. A separate later 3D platform also uses the name *The Sandbox* and evolved toward a creator/ownership ecosystem. These should not be conflated with the 2D physics-pixel games. The platform's own history notes the 2012 2D game's evolution and the later move toward a 3D creator platform. [The Sandbox official history](https://docs.sandbox.game/en/what-is-tsb)

### 29.2 The original and Evolution

The 2D games are built around pixel materials and elemental interactions. A world is not only a drawing. It is a field of substances and behaviours that react when placed next to each other or exposed to a force. Players can create, destroy, mix and observe. The sequel expanded the side-scrolling world, physics, visual effects and creative controls. Its press kit describes worlds around ten times larger than the predecessor, more than two hundred elements and entities, game-making components, and distinct creator modes: sandbox creation, game creation, laser construction, chiptune creation and pixel-art drawing. It also includes authored campaigns and an online gallery for sharing user-made worlds. [The Sandbox Evolution press kit](https://www.thesandboxgame.com/evolution/); [Steam store description](https://store.steampowered.com/app/466940/The_Sandbox_Evolution_Craft_a_2D_Pixel_Universe/)

### 29.3 Creation modes are different user intents

The useful platform lesson is that a single world representation can support several authoring modes. Someone who wants to experiment with material interactions, someone who wants to construct a platforming game, and someone who wants to draw pixel art are not asking for the same editing interface. They are still contributing to one creative ecosystem.

This maps well to the streamer-first package architecture: the package and resource identity remain shared, while specialised tools provide different views and authoring affordances. The editor can offer build, terrain, logic, sound, script-like rules, and presentation panels without creating unrelated package formats.

### Transfer

Use material/element rules that compose in readable ways. A water tile, fuel, soil, ore, heat source, toxin, conductor or biological material should have traits and reaction rules that can be inspected and tested. The same resource vocabulary should appear in the world simulation, editor, recipe graph, bestiary and UI.

Support multiple creation modes over one canonical project. A terrain editor, blueprint editor, mission editor, automation graph and UI editor can be separate tools, but their outputs should share the same project identity, revision, dependency graph and asset references.

**Do not copy:** the pixel art style, element names, specific reaction table, campaign levels or proprietary 3D blockchain platform model.

---

## 30. Sim Companies — production chains, market demand, quality, and player-run firms

*Sim Companies* is a multiplayer business simulation in which companies build facilities, produce goods, buy inputs, sell outputs, trade through an exchange or contracts, and invest in research to improve product quality. Its research guide explains that higher-quality inputs constrain the maximum quality of outputs, while quality affects retail sales speed and profitability. [Sim Companies research guide](https://www.simcompanies.com/pages/research-guide/); [beginner guide](https://simcompanies.atlassian.net/wiki/spaces/GUIDES/pages/196609/Guide%20for%20beginners)

### 30.1 The useful abstraction: firms are production graphs

A company is a directed graph of assets, inputs, processes, outputs, labour and market transactions. For example, raw input production feeds processing, which feeds manufacturing, which feeds retail. The company can specialise in one step, vertically integrate several steps, or buy/sell on the market. This creates economic interdependence between players.

The quality system is especially interesting because it propagates through the supply chain. Higher-quality output requires sufficiently high-quality inputs; the production chain therefore carries quality as a resource property. It is not simply a bonus that can be purchased in isolation. This provides a model for material grade, purity, durability, technology level or certification in the proposed game.

### 30.2 Market economies need guardrails

Player markets introduce price discovery and specialisation, but they also introduce volatility, hoarding, manipulation, monopolies, collusion, and barriers to new entrants. The proposed game's economy should distinguish between:

- **simulation price:** an indicative cost derived from scarcity, production time and logistics;
- **player market price:** a negotiated or listed price between players;
- **administered contract price:** a fixed or bounded price for missions, public procurement or baseline supplies;
- **strategic value:** a resource's value to a faction or defensive plan, which may differ from its market price.

A game with persistent worlds should not allow a market bottleneck to make basic survival impossible for new players. It should also avoid hidden NPC purchases that silently create unlimited demand or erase economic consequences.

### Transfer

Use player-run firms, factories, contractors and public utilities as explicit roles in the settlement economy. Let players trade intermediate goods and specialised materials; let quality be a property that propagates through recipes; and let contracts guarantee a minimum level of demand for necessary public services.

Every production chain should have visible inputs, outputs, throughput, labour, energy, storage, quality and transport constraints. Price and profit should be computed from auditable transactions. The UI should not maintain a shadow company ledger separate from the authoritative economic model.

---

## 31. Cities: Skylines, SimCity 2000, and the tycoon lineage — urban systems, legibility, and operational feedback

### 31.1 City builders are not one genre of simulation

The broad city-builder lineage contains several distinct design traditions. *SimCity 2000* foregrounds the legibility of a growing settlement: zoning, roads, utilities, civic services, terrain and the consequences of spatial decisions. The player composes a city from discrete parcels and networks, then reads demand, budgets, land value, traffic and service coverage as feedback. Its isometric presentation makes the map feel like a comprehensible miniature model. The player is simultaneously planner, mayor and systems debugger.

*Cities: Skylines* takes the road-and-network layer much further. Its road tool, lane hierarchy, junction controls, public transit, service coverage, district policies, land-use patterns and traffic simulation make circulation one of the central strategic systems. A city can be wealthy but still function badly because its spatial structure creates congestion, long commutes or poorly connected services. The city is not merely a set of buildings; it is a graph of trips, flows, capacities and catchment areas.

Other tycoon traditions focus on a different unit of optimisation. *Transport Tycoon* and its open-source successor *OpenTTD* make networks, timetables, cargo demand, vehicles and route efficiency the central problem. *RollerCoaster Tycoon* turns spatial layout, throughput, guest satisfaction, ride maintenance and business pricing into an attraction-management problem. *Theme Hospital* and related management games concentrate on staff, queues, room layouts, customer/patient needs and operational bottlenecks. *Caesar III* and settlement-management games add supply chains, civic order, military pressure and the spatial delivery of goods. These are related, but they should not be reduced to one generic “city simulation.” [OpenTTD official project](https://www.openttd.org/); [SimCity 2000 official product page](https://www.ea.com/games/simcity/simcity-2000); [SimCity 2000 manual](https://www.retrogames.cz/manualy/DOS/SimCity_2000_-_Manual_-_PC.pdf); [Cities: Skylines official site](https://www.paradoxinteractive.com/games/cities-skylines/about)

### 31.2 The city is a network of networks

A convincing urban simulation has multiple overlapping graphs:

- **Physical access:** roads, paths, rail, waterways, pedestrian links and service access.
- **Utility distribution:** electricity, water, sewage, heat, communications and waste removal.
- **Economic flows:** workers, goods, services, capital, customers and deliveries.
- **Civic coverage:** emergency response, education, health, administration, recreation and safety.
- **Social geography:** household composition, affordability, neighbourhood identity, pollution exposure and access to opportunity.

These graphs interact. A factory can create jobs and tax income but also freight traffic, pollution and housing pressure. A transit line may improve labour access and retail activity while shifting land values. A new road may relieve one junction while inducing extra traffic or severing a neighbourhood. A fire station is not just a radius decal; response depends on road access, dispatch, travel time, staffing and simultaneous incidents.

The key is that feedback must be intelligible. The player should be able to trace why a district is failing rather than being shown only a red overlay. A service-coverage overlay can show theoretical reach, while the operational view reports response times, unavailable units, blocked routes and unmet demand. A traffic view can distinguish volume, congestion, vehicle class, destination and freight flow. This distinction between potential coverage and actual service is crucial for meaningful planning.

### 31.3 SimCity 2000 and the value of a small, coherent simulation

*SimCity 2000* is a useful reference because its simulation is bounded enough for the player to grasp. It creates the feeling of a growing regional organism without requiring every citizen to be a fully simulated person. The visual model, zoning demand, service overlays and citywide indicators provide a readable abstraction. The player learns by seeing a road, utility or zoning choice change a visible pattern.

The proposed game should preserve that readability even if its underlying world is more detailed. It should not require the player to inspect hundreds of individual agents to understand why a neighbourhood has no water or why a production chain is late. Aggregate views should be honest summaries of the underlying simulation, not invented explanations. Every aggregate should be drillable to representative records, flow edges or an explanation of the rule that produced it.

### 31.4 Cities: Skylines — road tools as a game system

The most transferable design is not “use roads like Cities: Skylines.” It is the treatment of infrastructure construction as a high-frequency interaction with immediate spatial feedback. Placement tools communicate snapping, elevation, slope, curvature, collision, zoning frontage, intersection rules and cost while the player is still dragging the mouse. This reduces the gap between intention and execution.

Traffic management is effective when it supports diagnosis at several scales: citywide desire lines; district-level commute and freight patterns; road-class throughput; intersection queues; and local lane changes. A single average traffic percentage is insufficient to explain why a location is failing. Transport simulation should track trip purpose and vehicle class when those distinctions have gameplay consequences, but avoid simulating unnecessary detail simply because it can be done.

Cities: Skylines also provides a useful caution about systems whose complexity can exceed their reliability. Traffic agents, pathfinding, public services, citizen lifecycle, economy and modded assets can interact in ways that are hard to diagnose. The intended game should use deterministic or replayable systems where possible, publish performance budgets, and ensure that a debug view can explain a stalled agent or unserved demand. Its modding and tool ecosystem also demonstrates that a mature city-builder is partly a platform for player-authored content, not merely a fixed simulation. Current Cities: Skylines development and leadership arrangements have changed over time; the document treats the game as a design reference, not as a prediction about the future of its studio. [Paradox Cities: Skylines news](https://www.paradoxinteractive.com/games/cities-skylines/about); [Paradox announcement: Cities franchise moves to Iceflake Studios (17 November 2025)](https://www.paradoxinteractive.com/games/cities-skylines-ii/news/an-update-on-cities-skylines-ii)

### 31.5 Transport and tycoon games — logistics as a first-class player problem

In transport tycoons, a route is a production system. It has a source, destination, capacity, frequency, vehicle, infrastructure, cost, loading time, demand and often a chain of intermediate stations. The player's challenge is not simply to draw a line; it is to keep a network balanced under changing demand and finite resources. *OpenTTD* is especially useful as a long-lived example of the *Transport Tycoon Deluxe* model extended with large maps, modernised systems and multiplayer. [OpenTTD](https://www.openttd.org/)

A settlement game can borrow the same abstraction. A mine produces ore; a haulier moves it to a processor; a processor consumes energy and labour; a factory converts the refined material into components; a warehouse buffers output; and a contractor delivers it to a construction site. Every edge needs a capacity and cost. When the chain fails, the player should see whether the issue is production, storage, route capacity, vehicle availability, access, priority or demand spike.

Tycoon games also teach that a queue is a gameplay object. A queue is evidence that demand exceeds service capacity at a particular node. It can be solved by adding capacity, changing routing, adjusting price, increasing service quality, changing schedule or reducing demand. The interface should show throughput and waiting time, not only a queue length.

### 31.6 Transfer to the proposed game

Build the city as a collection of explicit, inspectable networks with shared spatial coordinates and a common simulation clock. Provide a small number of high-value analytical overlays that answer concrete questions: where resources originate, where they are consumed, where travel is slow, where service is missing, and which bottleneck limits production. Keep a citywide strategic view and a local operational view, but make them projections of the same authoritative data.

Use zoning and land-use policies as constraints and incentives, not as arbitrary colour painting. Let roads influence access, freight and service response. Let utilities have capacity, topology, loss and failure states. Let production and housing interact through labour availability, affordability and commuting. Let public services have finite staffing, vehicles and response times where that detail changes player decisions.

**Do not copy:** proprietary city assets, named districts, specific UI art, or the exact simulation implementation of any referenced title. Transfer the legibility, network thinking, operational feedback and scale management.

---

## 32. Subnautica, Below Zero, and Subnautica 2 — environmental discovery, depth progression, and survival through curiosity

### 32.1 Exploration is the central progression system

The first *Subnautica* is an unusually strong reference for making exploration itself the engine of progression. The player begins with a small safe space and incomplete knowledge, then moves outward into increasingly unfamiliar biomes. New tools, oxygen capacity, vehicles, base modules and material recipes make previously dangerous regions accessible. Progress is therefore both technological and epistemic: the player knows more about the world and gains the means to go farther into it.

The world is organised around gradients of risk and information. Depth, visibility, pressure, temperature, darkness, oxygen, navigation difficulty and creature threat can change together, but not always at the same rate. The player reads the environment through colour, silhouettes, sound, movement, resource placement, wreckage, radio messages and geological structure. A biome's identity is a system of cues rather than a label printed on the screen.

The GDC design discussion of *Subnautica* highlights discovery, the unknown, crafting, fear and the use of signals and clues to structure a sandbox without turning the experience into a conventional quest corridor. [GDC: The Design of Subnautica](https://www.gdcvault.com/play/1025745/The-Design-of-Subnautica)

### 32.2 The safety-to-uncertainty gradient

A good survival world needs safe and unsafe spaces that are mechanically and emotionally distinct. A small starter zone can teach resource collection, oxygen management, crafting and basic threat recognition. Nearby biomes widen resource options and introduce more complex navigation. Deeper zones can require specialised equipment and preparation. The key is that the player's home base remains a stable reference point while exploration increases uncertainty.

This avoids the common survival-game failure where every region is equally hostile and the player never experiences a meaningful change in risk. Safe space is not wasted space: it gives the player somewhere to interpret discoveries, sort materials, build plans, recover and make the next expedition intentional.

### 32.3 Tools as access permissions

A tool in *Subnautica* is more than a weapon or convenience. It can grant access to a new class of world interaction: cutting through sealed doors, repairing systems, scanning unknown objects, harvesting resources, building structures, diving deeper or moving faster. This creates a coherent lock-and-key progression because the obstacle is environmental and the new tool has a clear functional purpose.

The best tools are multifunctional enough to stay useful after the first gate. If a tool is created for one door and never matters again, it feels like a disposable quest key. If it expands a whole class of interactions, it becomes part of the player's mental model of the world.

### 32.4 Base-building as an extension of exploration

A base is a safe laboratory, storage system, transport node and personal expression space. Its value is not just cosmetic. It can shorten expeditions, store specialised resources, provide fabrication and power, and serve as a landmark. Placement matters because the geography of a base changes the player's relationship with the surrounding biome.

The game also makes material selection and power management meaningful. Building consumes resources that must be gathered and transported. Power generation has a relationship with local conditions. The base is therefore a logistical investment in a region, not a free menu that appears anywhere.

### 32.5 Below Zero: different scale and environmental contrast

*Subnautica: Below Zero* should be treated as a related but distinct design exercise, not simply a content expansion. Its setting places the player in a different part of the planet, one year after the first game, and gives much greater emphasis to ice-covered surface regions alongside underwater biomes. Its official product description foregrounds exploration, gathering, crafting, survival and the contrast between land and sea. [Below Zero official page](https://store.steampowered.com/app/848450/Subnautica_Below_Zero/)

That land/water contrast changes navigation, visibility, travel and threat. Surface spaces can create exposure and weather/temperature pressures; underwater spaces create depth, oxygen and visibility pressures. The design lesson is that a new biome should not be merely a different palette. It should change the movement and preparation problem.

### 32.6 Subnautica 2: co-op as a change to social systems

As of 31 October 2026, *Subnautica 2* is in Early Access, which began on 14 May 2026; the game supports solo play or co-op with up to three other players. The 19 August 2026 Buddy System update added proximity voice chat, player revives, emotes and item trading. On 15 October, Unknown Worlds began a developer-video series titled “Voyage to Early Access 2”; that announcement is a development-series kickoff, not evidence that an Early Access 2 build had already shipped by 31 October. These co-op systems change the survival loop itself: they alter expedition planning, rescue, resource sharing, risk-taking and the meaning of a safe base. [Unknown Worlds: Early Access launch (14 May 2026)](https://unknownworlds.com/en/news/subnautica-2-early-access-released); [Buddy System update (19 August 2026)](https://unknownworlds.com/en/news/subnautica-2-buddy-system-update); [“Voyage to Early Access 2” series begins (15 October 2026)](https://unknownworlds.com/en/news/voyage-early-access-2-begins)

The sequel is still an evolving Early Access product. Its current systems, content and balance should not be treated as final design law. The enduring reference is the series' approach to biome discovery, risk gradients, tools as new capabilities, and the transformation of unknown space into a familiar working environment.

### 32.7 Transfer to the proposed game

Use exploration as a source of both materials and knowledge. A new region should introduce a new combination of environmental constraints, creatures, resource affordances and navigational questions. Let tools expand what the player can perceive and do. Use base sites as logistical anchors and research stations. Make expedition preparation meaningful by giving the player a clear relationship between expected hazard, equipment, supplies, route and potential reward.

A world streamer can deliver biome identity, local encounter sets, ambient sound, resource distributions and asset bundles as one versioned region package. The simulation should still decide the actual state of creatures, resources and hazards. The client receives the region presentation and current state through the unified stream; it should not invent a separate “underwater game package” or “base package.”

**Do not copy:** the planet, creatures, named tools, story revelations, visual designs or specific progression gates. Transfer the discovery architecture and survival readability.

---

## 33. VRChat — user-authored social spaces, persistence, scripting, and platform governance (not a VR hardware study)

### 33.1 The relevant subject is the world platform

This reference intentionally does not focus on headsets, tracking, locomotion comfort or virtual-reality hardware. Its relevance is VRChat as a platform for player-authored spaces, avatars, social activities, interaction logic, world-specific rules, community identity and persistent data.

A VRChat world can be a room, game, event venue, social hub, puzzle, performance space or interactive exhibit. The creator does not merely place static props: they assemble interaction systems, networked state, scripted behaviours and player-facing interfaces. The world itself becomes a playable package authored by someone other than the platform developer. [VRChat creator documentation](https://creators.vrchat.com/)

### 33.2 Separate authored content from platform runtime

The important platform boundary is between the creator's world and the common runtime that loads it. A world author provides content and behaviour within a set of platform rules. The runtime handles identity, networking, instancing, loading, moderation and the shared user experience. This is closely aligned with the streamer-first package proposal: creator content should be modular and versioned, while trust, permission, resource limits and runtime policy remain platform responsibilities.

VRChat creator tooling offers both visual and code-oriented approaches to interaction through Udon and UdonSharp. A creator can build stateful behaviours, buttons, triggers, games and networked interactions without the player having to install a separate executable. The distinction between a visual graph and a C#-like workflow is a useful example of multiple authoring interfaces feeding one platform runtime. [VRChat Udon documentation](https://creators.vrchat.com/worlds/udon/)

### 33.3 Network synchronisation and ownership

Worlds need to distinguish local-only state from state that should be shared. A button's local hover animation does not need to be synchronised. A game score, door state, puzzle completion, ownership of a physics object or round timer may need a networked representation. VRChat's networking documentation describes synchronised variables, events and object ownership; a joining client needs the current state rather than replaying every interaction from the beginning. [VRChat networking documentation](https://creators.vrchat.com/worlds/udon/networking/)

This is a valuable rule for persistent game packages: replicate the minimum authoritative state needed to reconstruct the current situation. Do not stream every historical input as if it were current state. Preserve event history where audit or replay requires it, but send compact state snapshots and versioned deltas for the live view.

### 33.4 Persistence is scoped, not magically global

VRChat's persistence model distinguishes player data and world-bound storage. Its documentation includes explicit limits and scoping for persistent data; this is a useful reminder that user-created worlds need quotas, ownership rules and clear lifecycle semantics. [VRChat persistence documentation](https://creators.vrchat.com/worlds/udon/persistence/)

For the proposed game, distinguish: world-authoritative persistent records; player profile state; per-session transient state; local preferences; and derived caches. A mod must not be allowed to write arbitrary global state simply because it can draw a UI panel. Persistence capabilities should be declared and mediated by the server.

### 33.5 Social presence is an interaction system

A social space works because the environment gives players reasons to gather and interact: shared activities, navigable landmarks, visible gestures, group affordances, rooms with different purposes and opportunities for self-expression. The lesson is not to fill every world with chat windows. It is to design spaces that support both casual co-presence and structured activity.

World creators need reliable content discovery, versioning, moderation and ways to update their spaces without breaking communities. Player-made content must also be inspectable and reportable. When the world is part of a persistent economy or shared simulation, creator permission should never imply unlimited access to player data or server authority.

### 33.6 Transfer to the proposed game

Make modded spaces, lobbies, mission rooms, social hubs, player markets and minigames all first-class packages in the same content ecosystem. Give each package a canonical identity, dependency graph, permissions, update policy and content revision. Allow visual tools and code-like tools to create compatible contributions, but gate executable behaviour with a sandbox, quotas, capability checks and server-side validation.

Persist only data that is declared and authorised. Provide join-in-progress reconstruction from current state. Keep the client shell and common world renderer responsible for presentation; mod packages describe their contributions rather than shipping an independent client interface framework.

**Do not copy:** VRChat branding, avatars, worlds, Udon APIs or platform-specific monetisation. Transfer the creator-platform architecture and social-space patterns.

---

## 34. Batman: Arkham City Game of the Year — authored open-world storytelling, combat rhythm, stealth spaces, and environmental narrative

### 34.1 Scope and exclusions

This section analyses *Batman: Arkham City* Game of the Year as a design reference only. It does not propose importing Batman, DC characters, names, lore, storylines, gadgets, symbols, assets, voice performances or distinctive branded presentation. The relevant lessons are abstract: how a compact urban world supports narrative discovery; how combat and stealth use readable spaces; how traversal connects districts; and how optional content enriches the main story without dissolving its pacing.

The GOTY edition bundles the main game with additional content, including story and challenge material. The analysis treats the core campaign and the GOTY package as related but distinguishes the content bundle from the base world's design. [Arkham City GOTY product page](https://store.steampowered.com/app/200260/Batman_Arkham_City_GOTY/)

### 34.2 The city as a narrative instrument

A strong authored open world is not simply a large map containing missions. It uses geography to make the story visible. Districts can have different security conditions, social groups, architecture, sound, lighting and environmental histories. The player's movement through the city becomes a sequence of discoveries about who controls a place, what happened there and which routes are safe.

The useful design question is: what does the player learn by entering this street, climbing this structure or looking through this window? A location should communicate at least one of the following: narrative context, traversal opportunity, combat/stealth affordance, resource opportunity, faction presence or a mystery worth investigating. Empty distance is acceptable when it creates scale or atmosphere, but empty distance should be intentional.

The game's world design was explicitly shaped around making the player feel capable within its authored spaces, with distinct modes for direct combat, detective investigation and predator-style stealth. The important point is that the world is laid out to support those verbs rather than being a neutral terrain where every mechanic is pasted on afterward. [Pan Studio interview on Arkham City level design](https://panstudio.co.uk/batman-arkham-city/)

### 34.3 Combat as a readable state machine

The combat system is useful as a study of readable commitments and defensive responses. The player is surrounded by multiple opponents, but each threat has a legible state and response window. Attacks, counters, stuns, grabs, weapon threats and crowd positioning create a rhythm. The player is not asked to memorise a long arbitrary combo string; they learn a vocabulary of attack, response, repositioning and opportunity.

For a different game, the transferable insight is to make every threat communicate intent through animation, sound, pose, distance and timing. A defensive action should have a clear cost and a clear success condition. A counter should not be a generic invulnerability button. A heavy opponent, ranged threat, area threat and support unit should force different spatial responses.

The encounter designer must also protect readability when several enemies act simultaneously. Attack telegraphs need separation in timing and visual language. The camera, effects and hit reactions must not obscure a critical incoming threat. Difficulty should rise by adding interacting demands, not by hiding essential information.

### 34.4 Stealth arenas as authored problem spaces

Predator-style encounters are not ordinary combat rooms with the lights turned off. They are spaces with sightlines, cover, elevation, access routes, isolated opponents, patrol loops, destructible or interactive objects, and escape routes. Their purpose is to create a tactical puzzle that can be solved in multiple orders.

A well-designed arena supports more than one viable approach: isolate a guard; use vertical access; create a distraction; disable a hazard; cross a sightline during a patrol gap; or retreat and re-plan after detection. The level must signal the affordances without making the optimal solution obvious. A good stealth space also has a readable escalation path when the player is discovered: the encounter changes state rather than simply resetting.

### 34.5 Detective/investigation play as causal reconstruction

Investigation mechanics work when clues are connected by an explicit causal model. The player should be able to observe a physical trace, infer an event, test a hypothesis and identify the next useful location. A detective interface that only highlights objects is a scavenger hunt; one that connects evidence into a timeline or reconstruction gives the player a reasoning task.

For the proposed game, an investigation can identify a production sabotage, monster migration, structural failure, missing shipment, faction conspiracy or unexplained environmental anomaly. The clue system should distinguish observed fact, inferred cause and unresolved hypothesis. Player-facing certainty should match the evidence actually collected.

### 34.6 Mission layers and environmental story

The city can carry main-plot missions, faction tasks, side stories, challenge encounters, collectible lore and emergent events. These layers should not all have the same urgency. The primary narrative needs a coherent dramatic line; optional activities can expand the setting, expose consequences or teach mechanics. The player should be able to pause side content without losing track of the main objective.

Environmental storytelling is most effective when it is local and specific. A damaged room, barricaded hallway, abandoned meal, improvised medical area or altered power system can tell a story without an exposition dump. It should be possible to understand the evidence from the placement and condition of objects, with optional logs or dialogue adding nuance.

### 34.7 Transfer to the proposed game

Build compact, layered districts that combine transit, vertical routes, social spaces, combat/defence sites, investigation clues and optional tasks. Give each location a strong operational and narrative identity. Use encounter templates for direct combat, stealth, rescue, sabotage and investigation, but keep each mission's actual objective and context authored.

A district package should be able to contribute terrain, buildings, soundscapes, ambient actors, encounter definitions, mission records and UI surfaces using the unified package model. The same location identity must be used by the world simulation, mission system, map and shell. The client should not receive one map format for exploration and a second incompatible format for missions.

**Do not copy:** branded superhero systems, named gadgets, characters, story beats, visual iconography or proprietary assets. Use the encounter grammar and world composition techniques only.

---

## 35. Noita — material simulation, emergent causality, and dangerous experimentation

### 35.1 Materials are active rules, not decorative textures

*Noita* is built around a world in which individual pixels participate in simulated material behaviours. Its official description emphasises that every pixel is simulated and that materials react with one another. This creates a world where fire, liquids, gases, solids, electricity, explosives and other substances can interact in ways the player may not have anticipated. [Noita official website](https://noitagame.com/)

The important lesson is not that every voxel must be simulated as a particle. It is that material identity should have consequences. Water can conduct or extinguish; oil can burn; gas can spread; loose material can fall; heat can ignite or transform; and barriers can redirect flows. When these interactions are consistent, players can make hypotheses and test them.

### 35.2 Emergence depends on consistent local rules

Emergent interactions only feel fair when the rules are stable enough to learn. An explosive should not randomly change its behaviour because the engine needs a surprise. If a chain reaction occurs, the player should be able to reason backward from the materials and triggers. A powerful material system is a grammar of local interactions whose global outcomes can be surprising.

For performance, the proposed game should not adopt a full-resolution falling-sand simulation everywhere. Instead, use multiple levels of fidelity: exact or fine-grained simulation in active local regions; aggregated flow or cellular rules at medium range; statistical or event-based models for distant and inactive regions. The transition between levels must conserve important quantities and avoid visible discontinuities.

### 35.3 Wand construction as a compositional system

*Noita*'s wand and spell design provides a second reference: abilities are assembled from components whose ordering and casting rules matter. Modifiers, projectiles, triggers and timing can combine into effects the player did not receive as a single predesigned ability. This is a good pattern for modular tools, weapons, traps and automation logic. However, combinatorial systems need robust validation and clear inspection: players should be able to see the order of operations, resource costs, trigger conditions, and failure modes.

### 35.4 Transfer to the proposed game

Use materials and reactions as a shared simulation vocabulary across terrain, factories, weather, weapons, construction and ecology. Create a reaction registry with explicit preconditions, outputs, energy and mass changes, propagation rules, and safety limits. Use region-level simulation fidelity so the world remains scalable. Expose debug views that show which reaction fired and why.

Allow players to compose machinery and abilities from typed parts, but keep the composition declarative and inspectable. A package can contribute a new material or reaction only through declared schemas, capability review and performance tests. Avoid arbitrary code execution as the default way to add every new behaviour.

**Do not copy:** Noita's exact pixel art, spell catalogue, wand content, biome design or proprietary implementation. Transfer local-rule causality and compositional experimentation.

---

## 36. Wildfrost — tactical lane combat, telegraphed intent, and run-level town progression

### 36.1 A compact tactical language

*Wildfrost* is a roguelike deck-building tactics game whose battle system compresses a large amount of strategic information into a small board. Units occupy lanes, attacks and effects are governed by visible counters, and the player can inspect enemy intent before committing to a turn. The run is structured around map choices, encounters, rewards, shops, events and a developing town. Its official description highlights companions, items, counters, a branching map and town expansion/unlocks. [Wildfrost on Steam](https://store.steampowered.com/app/1811990/Wildfrost/)

The design strength is the legibility of consequence. A unit's attack is not an abstract damage number that may or may not happen later; its counter communicates timing. The player can read what the enemy is preparing, identify which unit will be attacked, and make a tactical trade. A small number of visible rules supports deep sequencing and combo creation.

### 36.2 Telegraphing turns into planning

The game demonstrates why visible intent is essential to tactical fairness. Enemy intent lets the player plan around future states instead of reacting to hidden randomness. A player may block, reposition, heal, remove a threat, accelerate an ally, delay an attack or accept a deliberate sacrifice. These choices create a useful distinction between risk that is unknown and risk that is understood but costly.

For a tower-defense or settlement encounter, the same rule can apply to enemy waves, siege engines, weather fronts, raids and boss behaviours. A telegraph should reveal enough for counterplay without fully revealing every random outcome. The player should know what class of event is approaching, when it is expected, and which assets are threatened. Uncertainty can remain in exact composition, secondary effects or route choice.

### 36.3 Run progression and persistent collection

A run is a sequence of constrained choices. The reward structure makes the player adapt a build rather than simply accumulate every available upgrade. Town unlocks create a longer-term progression layer that gives repeated runs a sense of accumulating knowledge and options. The distinction between run-local power and persistent unlocks is important: if the persistent layer becomes too strong, the run stops being a meaningful tactical test; if it is too weak, repeated play can feel unproductive.

### 36.4 Transfer to the proposed game

Use visible countdowns, target markers and unit intent for important threats. Make tactical counters composable: armour, area control, suppression, healing, decoys, barriers, mobility, sensors and disruption should answer different classes of threat. Keep the player-facing board and map uncluttered by surfacing the next important event and allowing detail to be inspected on demand.

Use two progression clocks: local campaign/run progression and persistent account/world progression. Make persistent unlocks broaden the player's strategic vocabulary rather than simply overpower every subsequent encounter. Record unlock provenance and package revision so a modded unit or encounter can be removed without corrupting the player's save.

**Do not copy:** the game's characters, exact cards, iconography, art, map events or proprietary balance tables. Transfer the compact state language and visible tactical intent.

---

## 37. Abiotic Factor — systemic survival inside a coherent research facility

### 37.1 A facility is a world with institutional structure

*Abiotic Factor* places cooperative survival in an underground research facility. Its setting is not a generic wilderness: it contains laboratories, offices, service corridors, storage, power infrastructure, security areas and experimental spaces. The official store description presents it as a 1–6 player survival-crafting experience with scientists, paranormal threats and a sprawling facility. [Abiotic Factor on Steam](https://store.steampowered.com/app/427410/Abiotic_Factor/)

This is useful because the environment has a legible reason for its spaces and objects to exist. A laboratory contains equipment, samples and power requirements; a maintenance area contains tools and access panels; a cafeteria has food and waste; storage has containers and logistics. The location's institutional purpose explains its prop language, resource placement and traversal structure.

### 37.2 Survival progression through repurposing

The facility's familiar infrastructure can be repurposed for survival. Players gather, craft, fortify, move supplies, establish safer routes and adapt their equipment to unfamiliar threats. This makes exploration a process of learning the building's original function and discovering what can be reused. The player does not merely collect generic wood and stone; they interpret an existing industrial environment as a material and logistical resource.

The cooperative format adds role differentiation without requiring strict classes. One player may scout, another craft, another carry materials, another manage equipment or hold a corridor. Good co-op systems create reasons to coordinate while preserving the ability for a group to improvise.

### 37.3 The value of multi-purpose spaces

A corridor can be a travel route, an ambush site, a shortcut, a storage area or a defensive choke point. A lab can be a research objective, a crafting station, a hazard zone or a temporary base. This density makes a fixed environment feel expansive. The designer does not need a separate arena for every activity; spaces can change meaning as the player's knowledge and equipment evolve.

### 37.4 Transfer to the proposed game

Design settlements and industrial sites around believable institutional functions. A research complex, mine, hydro plant, rail depot, hospital, reactor, warehouse or factory should have consistent access, utility, storage and safety logic. When the site is damaged or abandoned, players can repurpose its systems. Let the environment itself teach the production and maintenance vocabulary.

For co-op, support task complementarity without hard role locks. One player can identify a threat, another can prepare a countermeasure, another can operate heavy machinery, and another can transport or repair. A mission can have parallel tasks, but each should remain understandable to a single player where possible.

**Do not copy:** facility layouts, named organisations, creatures or story specifics. Transfer institutional world logic and survival-through-repurposing.

---

## 38. Cyberpunk 2077 — authored open-world density, vertical geography, and quest-specific spaces

### 38.1 A city is a navigable social and economic structure

*Cyberpunk 2077* is a useful reference for a dense city whose visual layers communicate social difference. Architecture, signage, advertising, transit, vertical circulation, lighting, crowding and infrastructure establish neighbourhood identity. Night City is not only a backdrop; it is a social and economic environment that influences how quests are framed and how the player approaches locations.

Interviews with the level-design team describe a distinction between the larger open-world city—whose broad composition is led by environment art—and individual quest/combat locations, where designers author spaces for particular gameplay needs. This division is useful because citywide continuity and encounter-specific readability are related but different responsibilities. [CD Projekt Red level-design interview (2026)](https://www.cdprojektred.com/en/blog/188/paths-and-possibilities-the-art-of-level-design-transcript-included); [Cyberpunk 2077 environment/level-design interview](https://www.pcgamer.com/cyberpunk-2077-interview-we-wanted-to-test-ourselves-as-a-studio-to-make-a-game-so-different-than-the-witcher/)

### 38.2 Verticality must create usable routes

A city can look vertically dense but still be mechanically flat if upper and lower levels are only decorative. The useful reference is verticality that affects traversal, sightlines, access, mission approach, faction boundaries, escape routes and the discovery of hidden spaces. Elevation changes how the player sees the city and how they reach objectives.

A vertical district needs dependable navigation language: stairs, lifts, ramps, bridges, service shafts, rooftops, public transport and controlled entrances. If multiple levels overlap visually, the map and route guidance must clarify the player's actual floor and access path. Verticality should create choices, not constant navigation confusion.

### 38.3 Authored gigs and mission spaces

The game's side missions and gigs demonstrate the value of authored spaces that support several approaches. A target can be approached by stealth, direct combat, conversation, environmental manipulation or an alternate access route, depending on the mission. The space is designed around the objective rather than generated from a generic template. Interviews describe quest spaces as intentionally authored, not simply procedurally populated. [Cyberpunk 2077 level-design interview with Miles Tost](https://www.invenglobal.com/articles/8416/e3-what-kind-of-character-do-you-wish-to-be-interview-with-cyberpunk-2077s-level-designer-miles-tost)

This helps distinguish a systemic open world from a procedurally generated mission. The city can be broad and reactive, while its key story spaces are deliberately staged. The player benefits from both scale and specificity.

### 38.4 Transfer to the proposed game

Create dense districts with distinct vertical layers, mobility networks, service routes, faction access and economic identity. A mission location should be designed for its core verbs: defend, infiltrate, repair, investigate, extract, negotiate, sabotage or rescue. Allow several approaches where they meaningfully change the player's risk and resource use.

Keep broad city streaming and mission-specific encounter definitions in one package ecosystem. A mission can reference locations, actors, assets, objectives and UI markers by stable identity. It should not package a separate version of the city or ship a mission-only rendering interface.

**Do not copy:** Night City, its districts, characters, story, branded vehicles, cyberware, logos, dialogue or distinctive visual assets. Transfer spatial density and quest-space authorship.

---

## 39. Elden Ring — landmark-led exploration, spatial revelation, and the relationship between open field and dungeon

### 39.1 The visible landmark creates a promise

*Elden Ring* uses distant landmarks to create navigational curiosity. A tower, tree, castle, bridge, ruin or unusual silhouette is visible before the player knows how to reach it. The landscape creates a question; movement and exploration answer it. Interviews with director Hidetaka Miyazaki discuss the value of seeing a distant point of interest, exploring toward it and using the map as a tool that enriches, rather than replaces, the experience. [Miyazaki interview on Elden Ring's open world](https://www.gamesradar.com/elden-ring-fromsoftware-hidetaka-miyazaki-interview/)

This is a more compelling navigational pattern than filling the screen with dozens of identical markers. Landmarks are meaningful when their silhouette, scale, position and environmental context distinguish them. A player can choose to follow a road, cross a ridge, explore a cave or approach from an unexpected direction.

### 39.2 Open field and authored dungeon are complementary

The open landscape provides freedom, discovery, optional encounters and routes around danger. Dungeons provide controlled pacing, tighter topology, denser enemy placement and more deliberate reveals. The two formats reinforce each other: the open world creates anticipation and the dungeon concentrates challenge.

A dungeon can be a compact spatial puzzle with loops, shortcuts, locked gates, vertical connections, hidden rooms and a memorable boss or objective. Its topology should communicate progress while preserving uncertainty. A shortcut that reconnects to an earlier space gives the player a sense of mastering the structure.

### 39.3 Difficulty and player agency

Open routes allow a player to defer a threat, search for resources or approach from a different direction. This makes difficulty partly a question of preparation and route choice. It also gives the world a sense of scale. The player is not always forced to solve the immediate obstacle at the moment it appears.

### 39.4 Transfer to the proposed game

Use large landmarks to support navigation and world identity. A settlement's generator, a mountain refinery, a colossal tree, a ruined orbital structure or an old railway bridge can orient the player from multiple locations. Design dungeons, factories, ruins and underground facilities as authored spatial modules with loops, shortcuts and controlled encounter beats.

Use procedural systems to vary resource placement, minor encounters and environmental states, but preserve authored topology for critical locations. The world can be systemic without every space being algorithmically assembled.

**Do not copy:** Elden Ring's lore, bosses, characters, visual motifs, weapon designs or exact map layout. Transfer landmark-led exploration and the open-world/dungeon relationship.

---

## 40. Elite Dangerous — a deterministic galaxy model, simulation layers, and long-horizon exploration

### 40.1 Procedural scale needs a coherent generative model

*Elite Dangerous* is notable for its immense galaxy, generated through Frontier's Stellar Forge system. The developer has described the system as producing a physically plausible galaxy from astronomical data and procedural rules, with roughly 400 billion star systems represented in the game. [Frontier developer interview on Stellar Forge](https://www.mmorpg.com/elite-dangerous/interviews/catching-up-with-the-dev-team-2000103763)

The lesson is not simply “make a very large procedural map.” It is to define a coherent generative model that creates plausible relationships among star systems, stars, planets, orbits, resources and travel distances. When a generated place is revisited, it should remain the same place. Deterministic generation from stable seeds and versioned rules is essential for persistence, navigation and community knowledge.

### 40.2 Galaxy simulation and local gameplay are different scales

A galaxy map can be represented with abstracted data: star positions, types, routes, faction influence, economy and discovered status. When a player enters a system, the simulation can load a more detailed local representation of bodies, stations, resource sites and hazards. This is analogous to level-of-detail in rendering, but applied to simulation and data availability.

The same principle can scale a persistent settlement game. A regional map need not simulate every citizen's footsteps. It can represent labour, freight, resource extraction and faction influence as aggregates. When the player visits a town, the local model expands into buildings, roads, agents and individual jobs. A good abstraction preserves the quantities and relationships that matter to the player.

### 40.3 Background simulation creates player-scale consequences

Elite's faction and economy simulation gives actions a wider context. Trade, combat and missions can affect local faction states and the wider balance of power. The player is not only completing isolated tasks; they participate in a persistent backdrop. The design challenge is to make those causal relationships visible enough that players understand their contribution.

A background simulation becomes meaningless if players cannot distinguish a real effect from flavour text. It needs public state changes, measurable thresholds, clear time horizons and a history of what changed. Missions should explain which faction or system they affect and what type of change is expected, without promising deterministic outcomes when other players and simulation events can intervene.

### 40.4 Transfer to the proposed game

Use deterministic, versioned generation for large regions, star systems, underground complexes or wilderness biomes. Store compact canonical seeds and state deltas; stream detail as the player approaches. Keep generation rules stable across updates or provide migration for world versions that change.

Build a strategic background simulation for faction control, trade, migration, production and conflict, then load detailed local simulation around active settlements and player activity. Show the player how local actions influence regional indicators. Avoid pretending that every distant individual is simulated at full fidelity.

**Do not copy:** Elite's galaxy, ships, factions, lore, cockpit UI, or proprietary system-generation algorithm. Transfer deterministic large-scale world generation and layered simulation.

---

## 41. Fez — spatial reinterpretation, cohesive world structure, and optional meta-puzzles

### 41.1 One world, multiple readings

*Fez* is useful for its relationship between a simple underlying spatial structure and a changing player interpretation of that structure. The player sees a 2D projection of a 3D world and can rotate the view, changing which surfaces align into a navigable path. The mechanic turns spatial perspective into a puzzle language. Its world is presented as a cohesive connected place rather than a sequence of disconnected stages. [Phil Fish interview on Fez's design and exploration](https://www.shacknews.com/article/70677/indiecade-2011-fez)

The broader lesson is that an environment can support multiple modes of understanding without needing entirely separate content. A world map, a logistics map, a threat map and an underground view can be different projections of the same underlying spatial truth.

### 41.2 Cohesion and optional layers

A cohesive world encourages players to remember places and connect discoveries. Optional high-difficulty puzzles deepen the experience for players who want to investigate more than the main progression requires. This is a useful way to support different engagement levels: the critical path should remain comprehensible, while optional meta-systems reward careful observation and long-term memory.

### 41.3 Transfer to the proposed game

Provide multiple views of one world model: physical terrain, utility networks, ownership, ecology, logistics, threat, underground structures and historical states. A change of view should reveal different relationships without changing the underlying identity of an object or place.

Use optional advanced puzzles and hidden systems, but ensure that the core campaign does not depend on obscure information that the player has no reasonable way to infer. Hidden mechanics should reward observation and experimentation, not require external wiki research to avoid failure.

**Do not copy:** Fez's characters, art, music, symbols, glyphs, puzzle solutions or exact rotation mechanic. Transfer world cohesion and multiple spatial readings.

---

## 42. Fallout: New Vegas — factional choice, consequence architecture, and dialogue as a system

### 42.1 Factions should represent competing futures

*Fallout: New Vegas* is a strong reference for faction design because its principal groups embody competing political and social visions. The player is not simply choosing a colour or reputation meter; the decision changes who governs places, how institutions operate, which groups are protected or suppressed, and what kind of future is plausible. The world contains local interests, ideological conflicts and personal motivations that do not always align with the faction's public doctrine.

The GDC talk by Joshua Sawyer, “Do (Say) the Right Thing: Choice Architecture, Player Expression, and Narrative Design,” is especially relevant to how player choice is framed and expressed through dialogue and quest structure. [GDC Vault: Do (Say) the Right Thing](https://www.gdcvault.com/play/1016166/Do-%28Say%29-The-Right-Thing)

### 42.2 Choice architecture is more than branching dialogue

A meaningful choice has four parts:

1. **Comprehensible stakes:** the player can understand what each option is likely to change.
2. **Conflicting values:** more than one option can be defensible, rather than one obviously virtuous and one arbitrarily cruel.
3. **World response:** characters, locations, resources, security and political relationships respond to the decision.
4. **Persistence:** the consequence remains visible later, rather than existing only in a line of dialogue.

Choices can be local, factional, economic, personal or systemic. A player may resolve one immediate problem at the cost of another group's long-term position. The game should make the trade legible while allowing consequences to unfold over time.

### 42.3 Dialogue as a capability check and expression layer

Dialogue options can express skills, prior evidence, faction relationships, background, resources or knowledge. A successful persuasion route should feel different from intimidation, technical reasoning or demonstrating proof. This creates a character-expression system where the player is not merely selecting the most rewarding line.

The underlying quest logic must remain explicit. A dialogue branch should lead to a defined state transition: information acquired, attitude changed, access granted, task accepted, resource exchanged, threat removed or an unresolved conflict left in place. A dialogue system should not become an untraceable collection of flags.

### 42.4 Transfer to the proposed game

Give factions material interests, institutional structures, internal disagreements and distinct approaches to infrastructure, ecology, labour, security and resource allocation. Let the player negotiate contracts, public works, defence alliances, environmental restrictions, trade routes and settlement governance. Track consequences through named state variables and public history.

Use evidence-aware dialogue. A player who has scanned a ruin, interviewed witnesses or recovered a production record should gain new ways to argue. The interface should distinguish verified evidence from rumour, hypothesis and bluff. Faction response should be deterministic where it can be, and explicitly probabilistic where hidden interests or other world actors can intervene.

**Do not copy:** Fallout lore, factions, characters, locations, dialogue, iconography or story choices. Transfer the political consequence architecture and player-expression design.

---

## 43. Skyrim — modular world production, environmental storytelling, and exploration through authored spaces

### 43.1 Modular kits support a large authored world

*Skyrim* is useful as a production reference for modular level design. Its modular approach allows environment teams to assemble dungeons, buildings, corridors and other spaces from reusable kits while still creating distinctive compositions. A Game Developer article on Skyrim's modular approach discusses how reusable pieces support production scale and consistency. [Skyrim modular level design (Joel Burgess and Nathan Purkeypile)](https://www.gamedeveloper.com/design/skyrim-s-modular-approach-to-level-design)

This is directly relevant to a content platform. A modular kit should not mean every building looks identical. The kit provides compatible dimensions, attachment rules, materials and functional interfaces; composition, damage, decoration, lighting and local history provide variation.

### 43.2 Spaces teach through placement

Exploration is supported by rooms and landmarks that imply purpose. A mine has work surfaces, rails, supports and storage; a ruin has structural changes and evidence of its past use; a household has the objects and routes of daily activity. The player can infer what happened from spatial relationships before reading a journal entry.

The world also creates micro-stories through the placement of enemies, loot, traps and environmental clues. The most effective examples do not need a long exposition text. They let the player assemble a short causal explanation from the space.

### 43.3 Radiant variation and authored anchors

Procedural or semi-procedural tasks can make a world feel active, but they work best when anchored by authored locations, characters and constraints. Repeated generic objectives become obvious when they do not respect local geography or faction context. A task should know where it occurs, who owns the location, why the objective matters and what changes when it is completed.

### 43.4 Transfer to the proposed game

Create modular kits for houses, workshops, factories, defensive structures, research facilities, roads, industrial interiors and ruins. Each kit should declare dimensions, sockets, utility ports, collision, navigation and permitted attachments. The streamer should load the assembled building and all required assets through the same package system as gameplay objects and UI contributions.

Use hand-authored anchors for important settlements and story locations; use data-driven variation for secondary interiors, resource nodes, patrols and repeatable contracts. Every generated mission should be grounded in the real state of the location and faction, not just a template with random nouns.

**Do not copy:** Skyrim's locations, characters, lore, art or modular assets. Transfer modular production and environmental storytelling.

---

## 44. Hollow Knight — interconnected map topology, gating, and the memory of revisited places

### 44.1 The map is a network to be learned

*Hollow Knight* is an important reference for interconnected level design. Its regions have distinctive identities, but routes between them create a coherent world that the player gradually understands. Shortcuts and loops make a place feel spatially real rather than like a list of independent levels. Interviews with Team Cherry discuss how the map evolved from a more linear structure and how revisiting areas became a major part of its design. [Team Cherry interview on Hollow Knight's map design](https://www.pcgamer.com/how-to-design-a-great-metroidvania-map/)

### 44.2 Gating can be spatial, informational and capability-based

Progression is not only about keys. A route can require a movement ability, a tool, a combat capability, knowledge of a hidden path or a willingness to accept risk. Once an ability is acquired, an old region can reveal new routes and items. This makes backtracking meaningful because the player's understanding and capability have changed.

The world should provide recognizable landmarks and map-building opportunities. Players need enough information to understand where they have been, while preserving the pleasure of discovering a route that was previously invisible or unreachable.

### 44.3 Environmental continuity creates memory

A revisited region can show altered enemies, opened shortcuts, new NPCs, changed hazards or newly accessible spaces. The player recognises the location and sees that their earlier actions or new capabilities have changed its meaning. The result is a world that feels persistent even when its map is relatively compact.

### 44.4 Transfer to the proposed game

Build regions with multiple connections, meaningful loops, shortcuts and vertical transitions. Let research, equipment, social permissions and construction alter which routes are available. When a player returns, show changes caused by faction control, repaired infrastructure, ecological shifts, settlement expansion or previous missions.

Keep map discovery and map annotation in the same client representation. A region package should identify its entrances, exits, route graph, landmarks and access rules in a way that the server can validate and the client can render. Avoid separate hand-authored navigation data that disagrees with collision or gameplay geometry.

**Do not copy:** Hallownest, characters, bosses, art, music or specific ability gates. Transfer the topology, backtracking and stateful recontextualisation.

---

## 45. MachineCraft — functional construction, control systems, and player-built machines

### 45.1 Construction should produce behaviour

*MachineCraft* is a sandbox engineering game in which players assemble machines from parts and blocks, including vehicles and other contraptions. Its Steam description highlights construction with functional components and control logic, as well as multiplayer rooms with configurable rules. [MachineCraft on Steam](https://store.steampowered.com/app/573060/MachineCraft/)

The useful distinction from purely decorative building is that a machine's form is directly connected to its behaviour. Wheels, motors, lift, steering, weapons, sensors, logic and structural parts contribute to a working object. The player tests, observes failure and iterates.

### 45.2 Control logic is part of the authored object

A machine is more expressive when the player can define how components respond to inputs. But this is also where construction systems become difficult to validate. A control system needs a bounded execution model, input/output types, update frequency, resource limits and clear failure handling. It should be possible to inspect why a machine turns, fires, stalls or loses power.

### 45.3 Transfer to the proposed game

Treat machines, defensive platforms, automated factories, drones and mobile bases as compositions of typed components. Give each component declared interfaces: energy, material, signal, force, fluid, data, mounting and control. The assembled machine should have a canonical graph and a deterministic simulation contract.

Support blueprint export, versioning, test arenas, component diagnostics and package dependencies. Do not turn a blueprint into an opaque executable blob. A control graph should be inspectable and sandboxed; the game should enforce limits on cycles, memory, execution cost and communication.

**Do not copy:** MachineCraft's exact blocks, UI, assets or control API. Transfer functional modular engineering.

---

## 46. Monster Hunter: World (with Iceborne as a separate expansion layer) — ecology, tracking, preparation, combat grammar, and a complete material economy

### 46.1 Why Monster Hunter World deserves a systems-level reading

*Monster Hunter: World* is one of the most useful references in this list because it joins several systems that are often built separately: world ecology, creature behaviour, environmental navigation, player research, equipment preparation, skill-based combat, material extraction, crafting progression, repeatable investigations and cooperative play. The result is a loop in which learning the world and learning the combat system reinforce one another.

This is not a generic “hunt a boss, receive loot” structure. The player studies an environment, follows evidence, identifies a target, prepares a loadout, pursues the creature through a living locale, responds to its behaviours and the surrounding terrain, then converts the hunt into new capabilities. The next hunt becomes different because the player has acquired knowledge and gear—not merely because a number went up.

The game's development interviews described a deliberate move away from earlier loading-zone field structures toward seamless, denser environments. Director Yuya Tokuda discussed how the team needed to make monsters behave as if they could track the hunter through scent and use the field's terrain, including vegetation and hollow spaces, as part of their movement and concealment. The ecosystem work also considered relationships from smaller life forms and herbivores up through apex predators, rather than treating monsters as isolated combat encounters. [Monster Hunter: World / Iceborne developer interview](https://www.siliconera.com/monster-hunter-world-iceborne-developer-interview/); [Game Developer interview on seamless maps and monster tracking](https://www.gamedeveloper.com/design/how-i-monster-hunter-world-i-s-director-breathed-new-life-into-the-game-s-old-bones)

### 46.2 The central loop: observe, prepare, track, engage, recover, craft

A useful abstract model is:

1. **Select a target or investigation.** The player has a goal, a reason to enter a locale and an expected reward profile.
2. **Prepare.** The player chooses a weapon, armour, skills, consumables, traps, bombs, ammunition or coatings, food and optional support tools.
3. **Enter the locale.** The player reads the environment, locates tracks and other evidence, gathers resources, and begins building knowledge of the target's current location and behaviour.
4. **Track and pursue.** Evidence helps the player find a monster. The creature may travel, feed, rest, flee, fight other creatures or move between zones of the continuous locale.
5. **Fight.** The hunter reads attacks, creates openings, manages stamina and positioning, exploits terrain and environmental opportunities, and chooses whether to capture or kill where the quest permits.
6. **Recover evidence and materials.** Tracks, breakable parts, carvings, captures, rewards, endemic life and research records feed different progression channels.
7. **Research and craft.** Materials become weapons, armour, upgrades and useful resources. Research improves understanding and helps future hunts.
8. **Choose the next hunt.** The player uses new equipment, knowledge and unlocked investigations to pursue a different challenge.

Every step produces a reason to perform the next. The system avoids a common survival-game problem where crafting is detached from exploration or combat is detached from the world. Materials are obtained by interacting with the creature and its ecosystem; the equipment made from them changes how the next hunt is approached.

### 46.3 The locale is a continuous ecological stage, not a flat arena

The Ancient Forest is particularly instructive because it is layered vertically and horizontally. Paths, roots, branches, caves, water, clearings and high routes create a dense environment with multiple ways to travel and pursue a monster. A creature can move through the locale in ways that are constrained by its body, habitat and behaviour. The player must understand how to reach it, where it may rest, which routes are faster and which local hazards can be used.

This design creates a useful distinction between **combat space** and **travel space** without separating them into different maps. A passage that seems to be a traversal route can become a chase route, an ambush opportunity or an escape path. A water channel may affect movement; vines or foliage can hide the hunter; a narrow corridor can constrain a large monster; a ledge or drop can create a position advantage. An environment becomes memorable when its geometry affects behaviour.

The locale should also have a clear ecological identity. The Ancient Forest, Wildspire Waste, Coral Highlands, Rotten Vale and other fields do not merely have different textures. Their terrain, resources, climate, visibility, endemic life and monster populations create distinct travel and combat problems. The player can learn a region's logic and use that knowledge.

### 46.4 The ecosystem: food, territory, competition and co-presence

The most important ecology lesson is that creatures need relationships. The environment feels alive when species have different niches and interact in ways that make sense:

- Herbivores feed, migrate, flee, gather or defend themselves.
- Predators search for food and respond to prey, scent, sound, injury and territory.
- Apex creatures contest resources, attack rivals or avoid dangerous opponents.
- Small endemic life occupies niches that may be harmless, useful, hazardous or interactive.
- Scavengers can appear around remains or disturbed areas.
- Some species are territorial; others are more opportunistic or defensive.

These behaviours need not require every creature to simulate a full ecological model at all times. The game can use a hierarchy of simulation fidelity. An active nearby creature can run detailed sensing, movement and combat. A distant population can be represented through population state, habitat suitability, resource availability and event-level movement. When the player approaches, the local model resolves into actors while preserving plausible continuity.

The ecology is most convincing when it generates visible consequences. A predator's presence changes prey behaviour. A large monster can disturb plants, break terrain or displace smaller creatures. A territorial encounter may interrupt a hunt. Tracks and feeding sites reveal prior activity. Even if a monster is not currently visible, the locale communicates that it has been there.

Monster-on-monster interaction should not be reduced to a scripted cinematic. It is powerful because two creatures with their own attack patterns, territorial priorities and movement can create a fight that the player must interpret. The player may use the chaos as an opportunity, retreat to avoid being caught between them, or lure one creature into a more advantageous position.

### 46.5 Tracking and research: the player becomes a field naturalist

The tracking system turns evidence gathering into a progression path. Footprints, scratches, mucus, feathers, broken plants, shed material and other traces are meaningful because they tell the player which creature has passed through and where it may have gone. Repeated collection can improve the player's tracking capability and unlock more precise guidance.

This has two related but distinct functions:

- **Immediate navigation:** evidence helps locate the current hunt target.
- **Long-term knowledge:** evidence contributes to research records, understanding of species and unlocks that improve later hunts.

The separation matters. A player should not have to collect every possible clue simply to complete a mission, but careful observation should produce a tangible long-term benefit. The research interface should preserve which evidence has been found, what it supports, and which questions remain unresolved.

This can be adapted to the proposed world without copying scoutflies or the exact tracking UI. Players can examine tracks, damaged structures, disturbed material, heat traces, resource depletion, animal calls, chemical residue, vehicle paths or signals. Evidence should help infer what happened, where a target is likely to be and what conditions it may respond to.

### 46.6 Monster design: readable identities and persistent behaviours

A strong monster is defined by more than health and damage. Each creature should have an identity expressed through:

- **Locomotion:** how it moves, turns, climbs, flies, swims or navigates narrow terrain.
- **Senses:** what it notices—visual movement, sound, scent, vibration, heat or other cues.
- **Temperament:** aggressive, defensive, territorial, evasive, opportunistic, social or solitary tendencies.
- **Habitat:** regions and microhabitats it can occupy, including feeding and resting areas.
- **Attack grammar:** a small set of clearly telegraphed actions that combine into a coherent fighting style.
- **Defence:** armour, scales, hide, shells, mobility, elemental resistance, posture or vulnerabilities.
- **Damage response:** flinches, staggers, part breaks, enragement, injury, fatigue, retreat or altered movement.
- **Resource relationship:** what it eats, what it competes for, and what materials it provides.
- **Ecological relationships:** prey, rival, scavenger, symbiote, or species that it displaces.

This produces a creature that remains recognisable when it is not fighting the player. A monster that only exists to perform a combat moveset is a boss. A monster that also feeds, rests, travels, responds to injury, interacts with competitors and occupies a habitat feels like part of the world.

Behaviour must also be bounded and testable. A creature should not be given dozens of loosely defined instincts that conflict unpredictably. Start with a small state machine or utility model: idle, forage, travel, investigate, flee, threaten, attack, recover, rest, defend territory and respond to another creature. Define transition conditions and priorities. Add complexity only when playtests demonstrate a useful emergent result.

### 46.7 Combat: weapons as distinct languages of commitment

The fourteen weapon classes in *Monster Hunter: World* are not merely skins over different damage values. They create different movement speeds, reach, timing, stamina demands, defensive options, commitment windows, positioning requirements and mastery curves. Great Sword asks the player to plan heavy commitments; Sword and Shield supports versatile movement and item use; Dual Blades emphasise rapid attacks and mobility; Hammer and Hunting Horn reward positioning around a monster's head; Lance and Gunlance express defensive control and deliberate openings; Switch Axe and Charge Blade use mode or resource transformations; Insect Glaive adds aerial movement and a kinsect; Light and Heavy Bowguns create ranged ammunition and firing patterns; Bow uses stamina and range management. These are broad identities, not a complete weapon tutorial.

The central design principle is that weapon choice changes the player's relationship to time and space. A slow weapon must predict an opening and commit. A fast weapon can exploit shorter windows but may need more hits. A shielded weapon can hold a line but sacrifices other capabilities. A ranged weapon changes spacing and resource management. Each should feel like a different way to read the same enemy rather than a different game with different rules.

Combat is fair when the monster communicates intent. Body posture, sound, wind-up, direction, distance and recovery all tell the player what is about to happen. Attacks should have discernible active areas and recovery windows. The player should be able to learn the creature's grammar through repeated observation. Difficulty can increase through combinations, timing, positioning and environmental pressure rather than invisible damage spikes.

The damage model also supports a strategic layer. Part breaks, wounds, stagger thresholds, elemental weaknesses, status effects, traps and capture opportunities reward target selection and preparation. The system should communicate which part is vulnerable and whether a damage type or status is having an effect. Avoid hidden arbitrary resistances that make the player feel their equipment choice was meaningless.

### 46.8 Preparation: equipment, consumables, food and loadout identity

The pre-hunt preparation step creates anticipation and agency. The player is not expected to use one universal build for every target. Armour skills, elemental defence, resistances, weapon properties, ammunition or coatings, traps, bombs and healing items can be selected for the expected challenge.

Preparation becomes meaningful when the target's behaviour gives the player information to act on. A creature that uses poison encourages antidotes or resistance. A flying target may reward ranged options or environmental opportunities. A fire-breathing creature makes heat and elemental resistance relevant. A target with a hard shell may reward a different weapon or attack angle. These choices should matter, but should not require perfect prior knowledge to make progress.

Loadout management should be fast enough that experimentation is welcome. Saved sets, clear skill descriptions, material previews and an equipment comparison reduce friction. The game should not punish the player for trying a new weapon by requiring an hour of repetitive menu work.

### 46.9 Materials and crafting: the economy is coupled to ecology

Hunting produces parts, hides, scales, bones, minerals, plants, ores and other materials. Those materials are not just a pile of currency; they are named outputs with different uses. Equipment trees connect a creature's identity to the player's capabilities. A player who wants to make a weapon from a particular monster may need to break a specific part or hunt a higher-ranked variant. This gives the player a reason to observe and target particular behaviours.

The crafting system creates a meaningful economy when recipes are transparent. A weapon path should show known requirements, missing parts and source creatures. If a material is rare, the player should understand whether rarity comes from a low drop rate, a part break, a capture, a rank restriction or a specific investigation. The system can preserve surprise while still communicating the rules.

An important distinction is between material **supply**, material **quality** and equipment **function**. Rarer materials can unlock a new branch, but not every upgrade should be a strictly higher damage number. Equipment can express elemental damage, status, defence, affinity, slots, sharpness or skill tradeoffs. That makes crafting a strategic choice instead of a linear treadmill.

### 46.10 Quests, investigations and repeatability

The quest system gives structure to a broad sandbox. Assigned story hunts can introduce new creatures, locales and mechanics. Optional quests can teach systems, unlock services or provide focused rewards. Investigations provide repeatable tasks with variations in target, conditions, time, faint limits and reward slots. This creates controlled variation: the environment and monster are familiar, but the contract changes the risk and incentive.

The guiding rule is that a procedural mission should not feel like a randomised objective pasted onto a map. Its conditions should interact with the target and location. A restricted time limit changes preparation; a reduced faint allowance raises risk; a multi-monster hunt changes route and stamina management; a reward bonus can encourage breaking a particular part. Each modifier needs a clear impact on player decisions.

### 46.11 Multiplayer: cooperative hunts as shared problem solving

Cooperative play changes the hunt because players can coordinate roles, share pressure and create additional openings. A team might distribute damage, use support items, mount or stagger at different times, trap a creature, heal, or manage a more dangerous phase. But the monster should not simply become a stationary health sponge. Its movement, target selection and attack behaviour need to remain legible as the number of hunters increases.

The SOS-style help system is useful because it allows a player to ask for support without requiring a permanent party. The mission should communicate player count, target state, objective progress and reward rules. Joining players need a current state snapshot and must be able to understand the local situation without replaying the whole session history.

The design should also distinguish shared progress from individual contribution. The party shares the objective, but each player should still have a reason to engage, prepare and learn. A cooperative game can create role diversity without hard classes by rewarding complementary tools and decisions.

### 46.12 Iceborne: an expansion that changes the challenge space

*Iceborne* should be treated as an expansion layer with its own progression and balance implications, not silently folded into the base game's exact content set. It adds a new major region, new monsters, a higher difficulty tier, additional moves and systems, and the Guiding Lands as a longer-horizon endgame environment. The important design lesson is that an expansion can increase depth by connecting new ecology and progression to the existing game rather than replacing the original loop.

However, expansion systems can produce a complexity cliff. A returning player needs to understand what changed, which old equipment remains useful, how new materials connect to prior trees and what the new endgame loop is for. Content versioning and migration matter: old investigations, loadouts and saves should have defined compatibility behaviour.

### 46.13 Transfer to the proposed game: a compact but deep ecology stack

The proposal should borrow the architecture, not attempt to simulate every biological detail at launch. A practical initial scope is:

1. **Habitat definition:** biome, climate, terrain, resources, cover, food and environmental hazards.
2. **Species definition:** body profile, movement, senses, needs, temperament, diet, defence and material drops.
3. **Local behaviour:** a small bounded state model for forage, travel, investigate, flee, defend, attack, rest and recover.
4. **Ecological relations:** predator/prey, competition, territorial conflict, scavenging and habitat constraints.
5. **Player evidence:** tracks, damage, remains, calls, movement signs and resource changes.
6. **Hunt/mission contracts:** target, location, modifiers, time, success condition and reward.
7. **Preparation:** equipment, consumables, environmental protection and scouting.
8. **Combat grammar:** telegraphed attacks, readable hit regions, part states, stagger, status and capture/kill outcomes.
9. **Material economy:** inspectable drops, recipes, equipment trees and quality/trait propagation.
10. **Research progression:** evidence-based unlocks that improve knowledge and preparation.

Each species, habitat, attack pattern, equipment item, material, recipe and mission should be a typed package contribution under the common versioned package ecosystem. The package should describe data and assets; it should not ship a private renderer or bypass the authoritative simulation. The server owns creature state, damage, drops, progression and mission results. The client streams the current state and presents it through the common game/world renderer and shell.

Use layered simulation fidelity. Nearby active monsters can use detailed behaviour and navigation. Distant populations can be modelled through counts, habitat quality and event-level state. When an animal becomes relevant, resolve it from deterministic seed/state and preserve continuity. This creates ecological believability without simulating every creature at maximum detail across the entire world.

Use a research system that makes evidence useful but does not require collecting every trace before a player can play. Preserve observed facts, inferred behaviour and unresolved hypotheses separately. Use an investigation board or bestiary that links each claim to its evidence source and version. This supports the user's broader evidence-first design doctrine without presenting an unverified inference as fact.

### 46.14 Failure modes to avoid

- **Decorative ecology:** creatures exist only to fight and do not affect prey, resources, habitat or one another.
- **Unbounded simulation:** every animal runs expensive AI all the time, causing performance collapse.
- **Opaque drop rules:** players cannot tell whether a material comes from carving, part break, capture, rank or a specific mission.
- **Weapon homogenisation:** all weapons become similar damage-per-second tools with different animations.
- **Boss-only encounters:** every creature is a stationary arena enemy and the locale does not matter.
- **Grind without learning:** repetition increases numbers but teaches no new behaviour or preparation.
- **Multiplayer health inflation:** co-op changes only enemy health rather than encounter behaviour and role opportunities.
- **Research as a checklist tax:** evidence collection is mandatory but does not improve understanding or open new options.
- **Procedural mission arbitrariness:** mission modifiers do not interact with the chosen target or environment.

### 46.15 Final design reading

The central contribution of *Monster Hunter: World* is the loop between **ecology, evidence, preparation, skill, material and mastery**. The player is not merely rewarded for defeating a creature. They learn to understand it, then use the products of that understanding to change what they can do in the world. A successful adaptation would make every hunt simultaneously a tactical encounter, an ecological observation, a resource expedition and a step in a coherent technology/equipment economy.

**Do not copy:** the Monster Hunter name, monsters, weapon art, creature silhouettes, animation, armour sets, UI, quests, story or proprietary assets. Transfer the ecology/track/hunt/craft architecture and the principle that each weapon class expresses a different combat language.

---

## 47. No More Room in Hell — scarcity, cooperative objective maps, and information deprivation

### 47.1 Survival horror through constrained information

The original *No More Room in Hell* is a cooperative survival-horror game where ammunition is scarce, the interface is deliberately restrained and players must pay attention to their surroundings. Its official description highlights up to eight-player co-op, objective maps, survival modes, limited ammunition and infection concerns. [No More Room in Hell official website](https://www.nomoreroominhell.com/)

The game uses the absence of information as a design tool. A minimal HUD means the player must read weapon condition, ammunition, teammate position, environmental sound and threat proximity. This increases tension, but also risks making the experience frustrating if critical information is simply hidden. The distinction is between **diegetic information**—readable in the world—and **missing information** that the player could not reasonably infer.

### 47.2 Scarcity creates cooperative responsibilities

Limited ammunition and healing mean that players must coordinate. A player who wastes resources may endanger the group later. A team needs to decide whether to fight, retreat, secure a route, split supplies or complete an objective under pressure. The survival game becomes a resource-allocation problem with immediate tactical consequences.

The infection threat adds another kind of scarcity: time and future agency. A player who is infected may have to communicate their condition, make a decision about their remaining usefulness and avoid becoming a greater danger to the team. Such mechanics work only when players understand the signs and consequences; hidden infection without readable cues becomes arbitrary punishment.

### 47.3 Objective maps and survival maps

Objective maps provide a sequence of tasks embedded in a level: find access, restore a system, locate supplies, open a route, defend a position or escape. Survival modes change the same spatial language into a question of endurance, wave pressure and resource conservation. The map layout becomes part of the co-op problem: doors and corridors can be defended, alternate routes can become escape paths, and a safe room can provide temporary relief.

### 47.4 No More Room in Hell 2 is a distinct sequel state

The sequel should not be folded into the original game's exact structure. *No More Room in Hell 2* moved through Early Access and reached its 1.0 launch in August 2026 according to the publisher's announcement. Its development and release history should be treated separately from the original community mod/game. [Torn Banner 1.0 launch announcement (11 August 2026)](https://www.gamespress.com/en-US/NO-MORE-ROOM-IN-HELL-2-SETS-August-11-LAUNCH-DATE-BRINGING-8-PLAYER-ZO)

This distinction matters when assessing versions: the original title's mission/survival design and its long-established rules are a different reference from the sequel's larger-scale development and later feature set.

### 47.5 Transfer to the proposed game

Use scarcity to force meaningful choices, not to create needless inconvenience. Make ammunition, fuel, medical supplies, spare parts, battery charge, oxygen and defensive materials visible and countable. Give squads tasks that can be completed cooperatively: carrying heavy objects, operating multiple switches, covering a repair, evacuating civilians or defending a temporary position.

Use objective maps with clear spatial beats and optional routes. A player should understand the mission goal while still having to discover the route or solve the local challenge. Use audio and environmental cues to communicate threats, but preserve enough interface support for accessibility and reliable co-op coordination.

**Do not copy:** NMRiH zombies, level layouts, distinctive UI, assets, characters or mission scripting. Transfer scarcity, teamwork, restrained information and objective-map pacing.

---

## 48. 7 Days to Die — voxel survival, structural systems, preparation cycles, and version evolution

### 48.1 The core loop is a repeating preparation-and-pressure cycle

*7 Days to Die* combines voxel terrain, scavenging, crafting, construction, survival needs, combat and periodic horde pressure. The seven-day rhythm creates a recurring planning horizon: explore and gather, improve equipment, secure a site, prepare defences, then withstand a predictable escalation. Players can choose how much to invest in mobility, fortification, traps, weapons, resource production or cooperative roles.

The value of the cycle is that it gives the player a deadline without making every moment urgent. Daytime can support travel and scavenging; nights or an approaching horde create a different risk profile. The player can plan a base around the likely threat and then assess which parts of the plan worked.

### 48.2 Voxel terrain and structural construction

The world is not just a flat map of prefabricated buildings. Terrain can be mined, excavated and reshaped. Structures are composed of blocks with material and support properties. This creates a building game where load, damage, collapse, access and repair can matter. A defensive structure is both a visual construction and a mechanical claim about what forces it can withstand.

A useful building system should expose support, material strength, load, damage, repair, access and resource costs. Collapse should follow understandable rules. If the player cannot infer why a floor failed, the structural simulation becomes a frustration generator rather than a planning tool.

### 48.3 POIs, scavenging and reusable level grammar

Points of interest are authored spaces that combine loot, navigation, threat placement, environmental storytelling and sometimes traversal puzzles. Their interiors can be structured around a visual theme and a sequence of discovery. Players learn that a building is more than an exterior shell: it may have an intended route, a hidden cache, a blocked entrance, a roof access or a dangerous interior.

This is a useful hybrid production model: use a modular block/asset vocabulary, but compose authored points of interest and curated routes. Randomised placement should vary the world without destroying the readability of these spaces. A procedural town can be a set of authored modules assembled under road and terrain constraints.

### 48.4 The version history matters

The game's version labels represent meaningful stages in the product's development, and “7 Days to Die” should not be treated as one frozen ruleset. The 1.0 release arrived after a long Alpha period; later major 2.x and 3.x updates continued to change systems and content. As of 31 October 2026, the latest stable branch is version 3.4.0 (b9 hotfix), announced on 26 October 2026. [Official 7 Days to Die Steam announcement feed](https://store.steampowered.com/news/posts/?appids=251570&feed=steam_community_announcements); [version 3.4.0 Stable release record](https://steamdb.info/patchnotes/26166997/)

This is a reminder to record the version being studied. A survival title can change its progression, loot, skills, crafting, world generation, vehicle, zombie and quality-of-life systems across major revisions. Design conclusions based on an old Alpha build may not describe the current product. The current version number is not itself proof that every underlying system is better; each change should be evaluated on its own mechanics and tradeoffs.

### 48.5 Transfer to the proposed game

Use a clear preparation cycle with visible deadlines and escalating threats. Let players reshape local terrain and build defences from materials with readable structural properties. Combine procedural settlement generation with authored POIs and modular structures. Make resource scarcity and repair demand feed into the production/logistics simulation.

A defence plan should be testable. The player should be able to inspect why a wall failed, which support collapsed, which path the attackers selected, where traps triggered, how much ammunition was consumed and which repairs are required. The event log should produce actionable lessons rather than only a victory/defeat summary.

**Do not copy:** 7 Days to Die's zombie fiction, POI layouts, item names, art, perk names or exact crafting tables. Transfer the periodic pressure cycle, voxel construction, structural readability and authored/procedural hybrid.

---

## 49. Cross-reference synthesis — what these games collectively imply for the proposed persistent city-builder / tower-defense project

### 49.1 The references describe six connected design layers

The title list is most useful when classified by what it teaches. Each game contributes a different design grammar. The project should combine the grammars selectively, not reproduce the original games as feature bundles.

| Design layer | Strong references | Reusable principle | What it should influence |
|---|---|---|---|
| Society, scarcity and civic survival | Frostpunk, Frostpunk 2, SimCity 2000, Cities: Skylines | Infrastructure is political, economic and spatial; scarcity creates tradeoffs. | Settlement simulation, law/policy, heat/power, public services, district planning and crisis choices. |
| Production and logistics | Sim Companies, Transport Tycoon/OpenTTD, RollerCoaster/Theme Hospital tycoon lineage, Nubby's Number Factory | Production is a chain with throughput, bottlenecks, quality, stochastic output and contracts. | Factories, companies, markets, resource conversion, production reports, automation and contracts. |
| Tactical encounters and adaptive pressure | Bloons TD, Left 4 Dead 1/2, Wildfrost, No More Room in Hell, 7 Days to Die | Threats need readable roles, counters, telegraphs, pacing and preparation windows. | Tower roles, wave composition, director pacing, mission objectives, co-op defence and horde planning. |
| Ecology, exploration and world knowledge | Monster Hunter: World, Subnautica, Below Zero, Subnautica 2, Abiotic Factor, Elden Ring | Regions should be systems to learn, with distinct hazards, landmarks, resources and behaviours. | Biomes, creatures, expeditions, evidence, field research, resource discovery and base placement. |
| Spatial storytelling and interconnected worlds | Batman: Arkham City (abstract mechanics only), Cyberpunk 2077, Fallout: New Vegas, Skyrim, Hollow Knight, Fez | Spaces carry narrative and gameplay; factions and landmarks shape exploration. | District design, quest chains, investigation, faction state, modular POIs, vertical navigation and backtracking. |
| Creator tools and modular composition | The Sandbox / Evolution, VRChat, Robocraft, MachineCraft, Noita | A shared runtime can host authored content, compositional machines, material rules and scripted interactions. | Mod packages, editor tools, UI contributions, blueprints, material definitions, creator governance and validation. |
| Persistent large-scale simulation | Elite Dangerous, Cities: Skylines, Sim Companies, Subnautica 2, VRChat | Large systems need deterministic state, snapshots, versioning, scoped persistence and join-in-progress. | World host, region streaming, background simulation, save migration, multiplayer, packages and long-term operations. |

### 49.2 The core gameplay loop should braid settlement, expedition, defence and production

The project should not become a tower-defense game with a city-builder bolted on, or a city-builder with arbitrary combat interruptions. The systems should feed one another:

1. **Settlement:** the player establishes or expands a place with housing, utilities, services, storage and civic rules.
2. **Production:** the settlement extracts, refines, manufactures, researches and trades resources.
3. **Expedition:** players explore surrounding regions, investigate threats, discover resources, map routes and gather evidence.
4. **Preparation:** the settlement converts expedition findings into equipment, buildings, units, defensive plans and logistics.
5. **Threat/mission:** a raid, environmental event, faction conflict, creature migration, sabotage or contract places pressure on the system.
6. **Response:** players deploy towers, mobile units, squads, traps, machines or direct intervention; the encounter uses readable counters and adaptive pacing.
7. **Consequence:** damage, material loss, faction change, ecological disruption, new territory or knowledge feeds back into the settlement.
8. **Learning:** reports show what failed, what succeeded, which evidence supports the conclusion and what the player can change next.

This makes the city the strategic anchor, the world the source of opportunities and threats, expeditions the knowledge engine, production the capability engine, and combat/defence the pressure test. Each layer has a distinct purpose but uses the same resources, identities, locations and package definitions.

### 49.3 Keep one authoritative world and one package family

The prior V8 streamer-first synthesis remains binding as the architectural direction of this proposal. This new reference pass strengthens rather than replaces it.

- One canonical package identity can contribute UI surfaces, buildings, towers, weapons, creatures, materials, recipes, missions, biomes, audio, assets and editor definitions.
- One package declaration and dependency graph describe all of those facets.
- One server-side admission and mount boundary validates content, capabilities, trust, compatibility and resource budgets.
- One current client-facing stream carries the permitted projection of mounted content and current state.
- One normalized client model represents the objects and relationships.
- The shell/window renderer and the world renderer are separate consumers of that model, not separate package ecosystems.
- Server-authoritative simulation owns world state, economy, combat, AI decisions, mission outcomes and persistence.
- The client presents the state, supports local interaction and submits typed commands; it does not independently invent outcomes.

A modded tower and a modded UI panel that configures it should share package identity, dependency and version provenance. A biome's terrain assets, creature population definition, resource table and research records should reference the same region/package revision. A machine's component graph and its in-world model should not be maintained as disconnected objects. A mission's map marker, objective state and simulation contract should share a stable identifier.

The references should expand the canonical package vocabulary, not create separate “Bloons tower format,” “Monster Hunter creature format,” “VRChat world format,” “city building format” and “mission format.” Internal domain schemas can differ by facet, but they should participate in one package identity, admission, versioning, trust, dependency and stream contract.

### 49.4 The adaptive Director should guide pacing, not author the world

Left 4 Dead's Director is a strong model for adapting pressure to the group, but it should not become an invisible omnipotent storyteller. The Director should operate within authored mission topology and declared encounter rules. It can adjust pacing, spawn windows, ambient threats, resource placement within constraints and intensity breaks. It should not arbitrarily teleport threats into view, invalidate safe routes or rewrite the mission objective without communicating the change.

For the proposed world, use a **Threat Director** that observes:

- player location, health, equipment and recent damage;
- squad size, separation and coordination state;
- settlement readiness, local defence, active repairs and available supplies;
- current encounter intensity and time since the last major threat;
- known mission objective and authored encounter gates;
- recent losses and resource pressure;
- relevant ecological or faction state.

It should then select from bounded, authored options: spawn timing, patrol pressure, route selection, reinforcements, hazard escalation, lull or new objective. It should preserve causal plausibility and log why a decision occurred. The result should be replayable or at least auditable from its seed, observed state and decision record.

### 49.5 Frostpunk provides the social stakes; dieselpunk provides the visual grammar

The original *Frostpunk* is useful for the moral and civic logic of survival; *Frostpunk 2* is especially useful for the more explicit dieselpunk transition. The design should connect visual form to operating constraints:

- Power plants, boilers, turbines and fuel depots communicate energy systems.
- Thick insulated doors, pipes, pressure valves, exhaust stacks and heat exchangers communicate heat and industrial maintenance.
- Heavy vehicles, crawler tracks, armoured transport, rail infrastructure and oversized engines communicate harsh terrain and logistics.
- Mechanical controls, analogue gauges, paper diagrams, signal lamps and warning placards communicate an industrial interface culture.
- Soot, frost, oil staining, condensation, chipped paint, worn brass, dark steel, riveted panels and utilitarian repair work communicate long service under stress.
- Civic halls, labour boards, ration distribution, clinics, barracks, workshops and public notice surfaces communicate institutional life.

Dieselpunk should not be applied as a cosmetic filter over a generic modern city. It should arise from the technology, fuel, materials, manufacturing capability, weather, labour and political economy of the setting. If the world uses oil, heavy machinery and combustion, show the infrastructure and consequences: fuel extraction, refining, storage, transport, emissions, fire risk, maintenance and labour. A diesel aesthetic without an energy and logistics model is only surface decoration.

### 49.6 Monster Hunter and Subnautica establish exploration as a productive activity

Exploration should yield more than random loot. It can provide:

- maps and route knowledge;
- material deposits and renewable resource sites;
- biological evidence and new species knowledge;
- environmental hazard data;
- faction intelligence and abandoned infrastructure;
- machine components and salvage;
- new settlement and expedition sites;
- recipes, equipment adaptations and research hypotheses.

This means a field trip contributes to the economy and technology tree even when the player does not complete a kill or mission. A failed expedition can still produce evidence or map knowledge, as long as the game makes that persistence explicit.

### 49.7 City and tycoon systems should create legible bottlenecks

A production bottleneck should be traceable from a player-facing symptom to a causal chain. “The factory is idle” should lead to a diagnostic path: no input stock, missing labour, no power, blocked transport, full output storage, disabled recipe, maintenance state, market restriction or invalid package dependency. The player should be able to inspect the actual records, not only a vague tooltip.

Use a common data model for stock, flow, capacity, quality, time, cost, ownership and destination. A resource should not have one definition in the city economy, a different definition in crafting and a third definition in the market. The same canonical resource ID and version should be shared across systems. Derived reports can use different views, but not fork the truth.

### 49.8 Tactical design should be readable before it is difficult

Bloons teaches tower/enemy role vocabulary; Wildfrost teaches visible enemy intent; Left 4 Dead teaches intensity pacing; Arkham City demonstrates encounter spaces designed around distinct verbs; No More Room in Hell demonstrates the tension of scarcity; and 7 Days to Die demonstrates preparation against a predictable threat horizon.

Combine these principles into a consistent threat language:

- Each enemy has explicit movement, attack, defence, sensing, target, resistance and vulnerability traits.
- Each threat is readable through silhouette, movement, sound, animation and inspection.
- Each defensive structure has a role, targeting policy, effective range, cadence, resource use and counter profile.
- Each encounter advertises its major threat classes and likely direction before it becomes unavoidable.
- Special events introduce new constraints rather than simply increasing health and damage.
- The player receives an after-action report: route, damage, target selection, wasted fire, resource use, structural failure, casualties and objective state.

A challenge should test a player's understanding of systems they have been taught. It should not hinge on undocumented exceptions or arbitrary counter requirements.

### 49.9 Use authored geography with systemic variation

A completely procedural world risks generic spaces; a fully hand-authored world can become expensive and repetitive. The best compromise from these references is:

- Hand-authored regional landmarks and major hubs.
- Modular building and facility kits with strict attachment and functional rules.
- Authored critical mission/dungeon/POI topology.
- Procedural or data-driven secondary encounters, resource nodes and ambient events.
- Deterministic regional generation for broad terrain and distant systems.
- Bounded dynamic changes driven by the simulation: damage, repairs, faction control, population movement, weather, ecology and production.

The resulting world has authored identity but systemic life. A player returning to a location should recognise its structure and see the persistent consequences of their actions.

### 49.10 Creator and mod support must be safe, inspectable and portable

Robocraft, MachineCraft, The Sandbox, VRChat and Noita demonstrate different aspects of a creative ecosystem. The proposed editor should expose multiple authoring modes over one project/package identity:

- **World/terrain editor:** regions, terrain, roads, water, biome, resource nodes and landmarks.
- **Building editor:** modular kits, attachment sockets, utility ports, structural properties and navigation.
- **Machine editor:** typed components, energy, motion, sensors, weapons, storage and control graph.
- **Gameplay editor:** towers, enemies, weapons, abilities, upgrades, missions, encounters and balance data.
- **Material/reaction editor:** traits, phases, energy transfer, reactions and environmental effects.
- **UI/surface editor:** data-bound views, forms, alerts, maps, inventory, crafting, mission and management panels.
- **Audio/visual editor:** sounds, materials, effects, animation, particles and lighting definitions.

A package may contribute to several facets. The package system must validate the dependencies, capabilities, schemas, bounds, resource budgets and compatibility of those contributions. Executable mods, where allowed, need a separate explicit trust decision and a sandboxed capability contract. A package is not automatically trusted because its hash is known or because its creator is popular.

Blueprints and authored assets should be exportable and versionable. Users should be able to inspect dependencies and understand why an older project cannot mount. A server shutdown should not necessarily erase locally held authored content. The project's long-term safety depends on portability and an understandable content provenance model.

### 49.11 A coherent visual identity should emerge from the simulation

The list spans dieselpunk industry, underwater science fiction, urban cyberpunk, medieval fantasy, pixel-material systems, survival facilities and stylised tactics. It does not imply that the project should mix all of those art styles into one inconsistent world. Instead, extract visual principles:

- Strong silhouettes that explain function and threat.
- Materials with consistent physical and industrial logic.
- Reusable modular forms that still allow local identity.
- UI that reflects the world without compromising legibility.
- Distinct palette and lighting by biome, district or institutional role.
- Persistent signs of wear, repair, pollution, weather and use.
- World landmarks that support orientation at a distance.

A diesel-industrial survival setting can use the Frostpunk 2 reference for machinery and civic architecture, Monster Hunter for biologically expressive creature silhouettes, Subnautica for environmental depth cues, and Cities: Skylines for infrastructure legibility—without copying any title's exact visual assets.

### 49.12 Priority order: what to prototype first

| Priority | Prototype | Why it comes first | Acceptance evidence |
|---|---|---|---|
| P0 | One region with authoritative terrain, roads, one settlement, stock/flow economy and one hostile encounter | Proves that the game world, simulation, rendering and package stream agree. | Server and client agree on object identity, stock, damage, state revision and reconnect snapshot. |
| P0 | One complete production chain with transport and storage | Proves that city/tycoon systems are real simulation rather than disconnected menus. | A bottleneck can be induced, diagnosed and fixed; quantities reconcile from source to consumer. |
| P0 | One tower, one mobile unit, three enemy roles and a bounded wave director | Proves combat counters, targeting, telegraphing and pacing. | Repeatable tests demonstrate valid counters, readable enemy intent and deterministic result logs. |
| P1 | One small biome with two ecological species and player evidence/tracking | Proves the Monster Hunter/Subnautica discovery layer with limited scope. | Creature state is stable under streaming; tracks lead to a target; population abstraction survives unload/reload. |
| P1 | One expedition mission with authored topology, optional route and persistent consequences | Proves that exploration and mission structure connect to the settlement. | Objective, evidence, loot and world consequences survive restart and show provenance. |
| P1 | One modular machine assembled from typed components | Proves MachineCraft/Robocraft-style construction under bounded simulation. | Energy, movement and component failures are inspectable; blueprint versioning works. |
| P2 | Creator editor for buildings, gameplay records and UI surfaces | Proves the package ecosystem supports non-core content authors. | A mod adds a building, tower, asset and management panel through the same package/stream. |
| P2 | Co-op encounter and join-in-progress | Proves state replication, group pacing and persistence. | Late joiners reconstruct the live state; server authority remains intact. |
| P3 | Large-scale background faction/ecology simulation | Adds long-horizon depth after local loops are reliable. | Deterministic/replayable regional updates and bounded CPU/memory costs. |
| P3 | Advanced material reactions and large-scale procedural world generation | Expands sandbox depth after performance and migration rules are proven. | Stress tests, save migration, causal diagnostics and simulation conservation tests. |

Do not begin by implementing every system from all references. The first vertical slice should prove the central loop: settlement resources fund an expedition or defence; the expedition reveals or produces something; the resulting material or knowledge improves the settlement; a threat tests the improvement; the after-action report explains the outcome.

### 49.13 Non-transferables and explicit exclusions

The references are not licences to reproduce protected expression. The project should not copy character likenesses, named factions, specific creature designs, distinctive weapon designs, maps, layouts, UI assets, iconography, storylines, dialogue, music, logos or proprietary source code. The Batman/Arkham study is expressly limited to general design techniques. The GTA study is limited to open-world activity structure, mission layering and version/platform differences. The Frostpunk study extracts industrial design language and its relationship to survival mechanics, not its exact world or story.

### 49.14 Final conclusion

The proposed game can be described as a **persistent, moddable industrial settlement simulation with expeditionary ecology and tactical defence**. Its strongest differentiator should be the causal connection among systems: the world supplies resources and knowledge; the settlement converts them into capabilities; the economy moves them; the player authors machines and structures; the threat director tests them; and the persistent simulation records the consequences.

The game should feel like a place because geography, ecology, industry, factions and local history interact. It should feel like a simulation because those interactions have inspectable rules and persistent outcomes. It should feel moddable because all contributions share one safe, versioned package ecosystem and one streamer-first client model. It should feel fair because threats are readable, decisions have traceable consequences and the game can explain its own state.

The best synthesis is not “add everything these games have.” It is: **make every major layer of the world reinforce the others, and make the entire world understandable through one coherent simulation and content architecture.**

---

## Version 9 research and synthesis record

Version 9 preserves the complete Version 8 analysis and adds the reference pass in Sections 21–49. The new pass compares the requested titles by reusable design systems, not by proposing direct reproduction. It expands the prior architecture with:

- Frostpunk/Frostpunk 2: industrial survival, civic tradeoffs and the shift from Victorian-industrial/steam cues toward an oil-powered dieselpunk language.
- GTA Online: free-roam activity layers, authored mission structure, creator tools and major platform/version differences.
- Bloons TD1–6: tower roles, enemy counters, upgrade restrictions, map geometry, modes and meta-progression.
- Nubby's Number Factory: stochastic production and combinatorial item synergies.
- Left 4 Dead 1/2: authored topology with adaptive Director pacing and environmental variation.
- R.E.P.O.: physics-driven co-op communication and extraction risk.
- Robocraft, The Sandbox/Evolution, VRChat and MachineCraft: creator platforms, modular construction, safe scripting and content governance.
- Sim Companies, Cities: Skylines, SimCity 2000 and tycoon games: production graphs, urban networks, traffic, services and market design.
- Subnautica / Below Zero / Subnautica 2: exploration as information progression, environmental risk and base logistics.
- Arkham City GOTY: abstract combat, stealth, traversal, investigation and authored world design only; no DC-specific content.
- Noita: material causality and compositional systems.
- Wildfrost: visible enemy intent, tactical counters and run-level progression.
- Abiotic Factor: institutional environments and survival through repurposing.
- Cyberpunk 2077: vertical city navigation and quest-authored spaces.
- Elden Ring, Fez and Hollow Knight: landmarks, spatial reinterpretation, loops and revisited-world progression.
- Elite Dangerous: deterministic large-scale generation and layered simulation.
- Fallout: New Vegas: factional choice, evidence-aware dialogue and persistent consequences.
- Skyrim: modular production and authored environmental storytelling.
- Monster Hunter: World: extensive ecology, tracking, research, weapon mastery, preparation, crafting, investigations and cooperative hunts.
- No More Room in Hell and 7 Days to Die: scarcity, objective maps, voxel building and cyclic threat preparation.

The synthesis is an architecture and design proposal. It is not a statement that the proposed systems have already been implemented, benchmarked or accepted as final requirements. Existing Version 8 decisions and open questions remain intact unless a future explicit decision revises them.

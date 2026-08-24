# Primeval Works — master game design

Status: pre-production source of truth  
Target: Minecraft Java 26.1.2, NeoForge  
Theme: CurseForge ModJam 2026 — Echoes of the Past

## Product promise

Excavate ancient eggs, hatch genetically unique prehistoric companions, and turn a small camp into a living automated base powered, protected, and operated by dinosaurs.

The mod is not a collection of disconnected dinosaur mobs and machines. Every major system must strengthen at least one of these fantasies:

1. Discovery: finding a new species should feel meaningful.
2. Attachment: individual dinosaurs should be recognizable and worth protecting.
3. Expression: two players should be able to build visibly different working bases.
4. Progress: better organization and stronger relationships should unlock ancient technology.
5. Readability: players should understand why a dinosaur is or is not working.

The shortest complete loop is:

`discover egg → hatch companion → inspect traits → register at Command Table → assign work → route output → improve the base → reach rarer nests and relics`

## Terminology

- **Dinosaur** is the friendly in-game shorthand for every companion.
- **Primeval companion** is the accurate marketing term because Pteranodon is a pterosaur and Dodo is an extinct bird.
- **Base** is one Command Table, its bounded work area, its registered stations, and its roster.
- **Active** means materialized as an entity at a base.
- **Reserve** means safely serialized in the Command Table roster and not present as an entity.
- **Work type** is one of Transport, Cooking, Energy, Crafting, or Gathering.
- **Guardian** is a combat-capable companion assigned to defend a base; combat is not a sixth work type.

## Player progression

### Chapter 1 — First Echo

The player discovers fossil traces in exposed stone, suspicious sand/gravel loot, or a rare fossil nest. A nest visibly communicates the species before hatching.

- Right-clicking an intact wild egg starts a short hatching sequence in place.
- Silk Touch collects the intact egg for controlled incubation.
- A hatchling binds to the interacting player.
- Without a Command Table, the hatchling follows its owner and cannot work.
- With a nearby owned Command Table and a free slot, the hatchling registers and becomes active.
- If the active roster is full, it enters reserve. It must never be discarded or silently fail to hatch.

The first table begins with three active slots. Upgrades increase this to five and then the promised maximum of eight. Reserve capacity begins at twelve and increases to twenty-four.

### Chapter 2 — Living Workshop

The player builds a Feeding Trough, Nesting Mats, one gathering station, and one production station. Dinosaurs perform visible work rather than acting as invisible speed upgrades.

Outputs can be routed to:

- the station's internal output;
- one linked nearby inventory;
- the Command Table pantry, when the output is valid food.

A Signal Baton links stations, inventories, Guard Posts, and the Command Table. The UI remains the authoritative place to inspect and edit those links.

### Chapter 3 — Ancient Industry

Ancient Metal and Sulfur enable powered machines. Water Turbines, Wind Turbines, and Kinetic Dynamos contribute base power. Players choose which machines receive limited power and which dinosaurs specialize in production.

The Processor, Ancient Furnace, Reinforced Piston, Dart Turret, and advanced logistics appear here.

### Chapter 4 — Primordial Mastery

Compressed Ancient Metal, Compressed Cores, species materials, and Primordial Ingots unlock the Prime Incubator and relic equipment. Late-game items should interact with companions rather than merely provide larger damage numbers.

The final rewards are:

- **Primordial Sword** — hitting a target marks it for nearby guardians and builds an Echo charge from coordinated attacks.
- **Ancient Reforged Bayonet** — a late-game precision melee weapon forged from expedition-only Ancient Metal. It rewards holding the target at bayonet distance instead of point-blank sword play.
- **Prime Incubator** — raises the minimum genetic quality and mutation odds, but never guarantees a perfect dinosaur.
- **Sanctuary Beacon** — an expensive powered endgame block that suppresses ordinary hostile spawning inside one base. It does not affect spawners, bosses, raids, or scripted encounters.

## The twelve species

All species can perform all five work types. Primary efficiency is 130%, secondary efficiency is 90%, and every other work type is 45%. Species bonuses improve a strategy; they never gate a mandatory recipe.

| ID | Display name | Primary | Secondary | Guardian | Passive and identity |
|---|---|---:|---:|:---:|---|
| `t_rex` | Tyrannosaurus rex | Energy | Transport | Yes | Apex Guardian: strongest threat generation and Kinetic Dynamo output; very high food cost |
| `triceratops` | Triceratops | Gathering | Transport | Yes | Bulwark: intercepts attacks aimed at non-guardians and harvests several crops in one pass |
| `brachiosaurus` | Brachiosaurus | Transport | Gathering | No | High Reach: largest carry stack and pickup radius; can reach elevated Timber Station tasks |
| `dilophosaurus` | Dilophosaurus | Cooking | Crafting | Yes | Volatile Saliva: improves chemical, sulfur, dart, and heat recipes; ranged poison attack |
| `velociraptor` | Velociraptor | Crafting | Transport | Yes | Quick Hands: shortest station interaction delay and fastest response to new work orders |
| `stegosaurus` | Stegosaurus | Gathering | Crafting | No | Ore Sifter: improves Fire work at the Processor without making it mandatory |
| `parasaurolophus` | Parasaurolophus | Crafting | Transport | No | Resonant Call: slowly raises the mood of nearby companions, with diminishing returns |
| `ankylosaurus` | Ankylosaurus | Gathering | Energy | Yes | Living Hammer: high mining yield and crusher/dynamo efficiency; naturally armored |
| `pteranodon` | Pteranodon | Transport | Energy | No | Air Route: ignores most ground route penalties and improves Wind Turbine output while assigned |
| `dodo` | Dodo | Cooking | Gathering | No | Seed Scatterer: finds seeds and berries and occasionally sheds a feather while content |
| `spinosaurus` | Spinosaurus | Gathering | Fire | Yes | River Hunter: finds fish and defends efficiently in water |
| `pachycephalosaurus` | Pachycephalosaurus | Energy | Gathering | Yes | Impact Drive: excellent Kinetic Dynamo worker whose charge briefly stuns ordinary attackers |

Non-guardians flee toward the Command Table or nearest Guard Post when threatened. They can retaliate weakly if cornered, but they do not seek targets or patrol.

## Individual dinosaur data

Every dinosaur has a stable UUID and an immutable rolled genome. Its name, XP, level, hunger, mood, injury state, current order, and base assignment change over time.

### Genome

Three genes are rolled once at hatching:

- Work aptitude: 0–100
- Combat vitality: 0–100
- Movement aptitude: 0–100

Genes apply bounded multipliers. The intended ordinary range is roughly 0.85–1.15; genetics must matter without making a low roll useless.

Visual scale is derived from average genetic quality plus a small independent jitter:

- Minimum visual scale: 0.90
- Maximum visual scale: 1.10
- The collision box starts from the species-authored dimensions and follows the rolled scale.
- Size is a clue to quality, not a perfect scanner.

Hue variation is deterministic from the genome. Keep it subtle enough that the species palette and UI portrait remain recognizable. The body tint uses a dedicated texture mask so eyes, teeth, claws, markings, and equipment retain their authored colors.

### Mutations

A dinosaur can have neither, either, or both of the two birth mutations:

- **Huge** — +18% model and collision scale, +20% work, damage, movement, and maximum health.
- **Albino** — a shading-preserving white body and red eyes, +40% work, damage, movement, and mount speed, but -20% maximum health.

Wild eggs independently roll 5% Huge and 0.5% Albino. The Premium Egg Incubator raises those independent chances to 25% and 4% while also improving genetic quality. Bred eggs have 9% and 1.2% baseline chances for traits absent from both parents. A trait has a 65% inheritance chance when one parent has it and 88% when both do.

Breeding requires two owned dinosaurs of the same species, two Nesting Treats, sufficient hunger and mood, and a ten-minute parent cooldown. The resulting egg averages its parents' quality and adds a small improvement. All mutation rolls and inheritance boundaries are server-owned and covered by automated tests.

### Level and XP

Levels run from 1–10. Completing useful work, defending the base, and recovering from an injury grant XP. Levels provide small reliability improvements and cosmetic recognition, not exponential power.

- +2% work contribution per level after level 1.
- +2% maximum health per level after level 1.
- Species passive reaches full strength at level 5.
- Level 10 unlocks a species-specific mastery flourish and nameplate border.

The full work-speed result is clamped after all modifiers. No combination of species, genome, mood, level, and mutation may exceed 185% or fall below 25% while the dinosaur is willing to work.

## Needs, schedule, and safety

### Hunger

Hunger is a base fuel economy, not a click-every-minute chore.

- Hunger is stored from 0–100.
- It drains primarily during work, travel, and combat.
- Large species consume more nutrition but should contribute proportionally more value.
- At 60 hunger, a companion schedules a meal when convenient.
- Under 30, food outranks ordinary work.
- At 0, the dinosaur stops working and its mood falls, but it does not take starvation damage.

The Command Table contains a pantry. Transporters can route berries, crops, meat, or prepared rations into it. A visible Feeding Trough is the dining destination; the table may provision several troughs inside the base.

Diet tags:

- Herbivore: berries, crops, Berry Mash.
- Carnivore: small/large dinosaur meat, fish, Meat Ration.
- Omnivore: accepts either with a smaller food-variety bonus.

### Mood

Mood is 0–100 and always shows a breakdown in the UI.

Positive sources include recent meals, food variety, sleeping on a Nesting Mat, finishing work, Parasaurolophus resonance, treats, and time near the owner. Negative sources include hunger, injury, being stuck, working without rest, recent attacks, missing sleep space, and repeatedly cancelled tasks.

| Mood | Label | Work effect | Behavior |
|---|---|---:|---|
| 80–100 | Inspired | up to +15% | rare happy material or flourish chance |
| 50–79 | Content | neutral | ordinary behavior |
| 25–49 | Uneasy | down to -20% | seeks rest more often |
| 1–24 | Miserable | down to -40% | refuses low-priority work |
| 0 | Shut down | no ordinary work | eats, sleeps, flees, or recovers only |

No hidden anger meter exists. Aggression is a species/assignment behavior; mood is the emotional state.

### Night and sleep

- At night, ordinary workers finish their current atomic action, deposit carried items, then seek an assigned or free Nesting Mat.
- Generators continue their environmental base output, but dinosaur contribution pauses while the worker sleeps.
- Guardians sleep when safe and wake immediately when the base threat controller detects a valid attacker.
- A dinosaur unable to find a bed sleeps near the table with a reduced mood recovery rate.
- The player can temporarily mark one essential station as overnight work, but doing so creates a visible mood cost and mandatory later rest.

### Injury instead of permanent death

Owned dinosaurs cannot be permanently killed by ordinary damage.

At zero health:

1. Cancel and roll back the active work reservation.
2. Play the incapacitation animation.
3. Convert the dinosaur to an injured reserve snapshot exactly once.
4. Remove the active entity.
5. Start a recovery timer that consumes food or Recovery Salve.

Injured dinosaurs cannot be activated until recovered. This preserves emotional attachment while allowing attacks to create real economic consequences.

## Work system

### Work types

- **Transport** — moves committed stacks between stations, linked inventories, troughs, and the table pantry.
- **Cooking** — operates food, heat, chemical, sulfur, and furnace-like recipes.
- **Energy** — adds power to environmental or kinetic generators.
- **Crafting** — operates ordinary Crafting Tables and advanced assembly recipes.
- **Gathering** — performs foraging, timber, crop, fishing, excavation, and ore-processing work.

A work station advertises discrete work orders. The base scheduler assigns an eligible dinosaur based on player lock, priority, specialty, distance, current need, mood, and anti-thrashing cost.

The player can choose:

- automatic best worker;
- lock a dinosaur to one station;
- lock a dinosaur to a work type;
- disable a dinosaur from a work type;
- set station priority from 0–5;
- select one output destination;
- set minimum pantry reserves so food is never exported accidentally.

### Visible work

Production only advances while the assigned dinosaur is in the station interaction zone and performing its work animation. Machines may retain partial progress when a worker eats or sleeps. They do not complete work from anywhere in the base.

If pathfinding fails:

- retry with a bounded cooldown;
- expose a blocked-route icon and reason;
- release the reservation after its lease expires;
- only use an anti-stuck teleport to the Command Table after repeated failures and only inside owned base bounds.

## Power

Power is an integer base capacity, not a cable network and not Forge Energy.

- Producers register generation with the Command Table.
- Consumers request capacity only while active.
- The table allocates power by player-set priority and stable tie-breaking.
- UI always shows produced, reserved, used, and blocked demand.
- Environmental validity is rechecked on a slow interval or relevant block update, never by scanning every tick.

Initial values:

| Producer | Base output | Specialist contribution |
|---|---:|---:|
| Water Turbine | 2 | +1 from a strong Energy worker |
| Wind Turbine | 3 | +1, Pteranodon can reach +2 at high level |
| Kinetic Dynamo | 0 | +2–4 depending on assigned dinosaur |

Consumers use the original readable tiers:

- Power 1: utility or logistics.
- Power 2: ordinary processing.
- Power 3: defense or advanced processing.

A Compressed Core Buffer stores a short reserve measured in power-ticks. It bridges brief shortages and overnight worker rest but cannot replace generation.

## Blocks

### Base and care

- **Command Table** — base ownership, active/reserve roster, pantry, assignments, power, alerts, and upgrades.
- **Feeding Trough** — visible eating point supplied by the table pantry.
- **Nesting Mat** — sleep assignment and mood recovery.
- **Guard Post** — patrol center and threat radius for one guardian.
- **Recovery Nest** — improves injured recovery and displays the recovering companion.
- **Cargo Depot** — stable logistics endpoint and overflow buffer.

### Hatching and discovery

- **Fossil Nest** — natural feature containing a species egg and fossil loot.
- **Ancient Egg** — species-specific wild block, right-click hatchable and Silk Touch collectible.
- **Hatchery** — controlled basic hatching with progress indication.
- **Prime Incubator** — endgame genetic-quality floor, increased mutation odds, and a dramatic animated hatch.

### Gathering and production

- **Foraging Post** — berries, seeds, mushrooms, and biome-sensitive finds.
- **Timber Station** — produces Hardwood from nearby valid logs without deleting player builds; it consumes tagged input logs or uses an explicit managed grove.
- **Excavation Pit** — processes fossil-bearing material and mining inputs.
- **Processor** — combines a material, furnace fuel, and a recipe catalyst to refine or compress ancient materials; Fire workers improve throughput but are not required.
- **Ancient Furnace** — powered heat processing, alloying, and prepared ration recipes.
- **Runic Anvil** — final Primordial equipment assembly.

### Power and utility

- **Water Turbine** — generator requiring a valid flowing-water placement.
- **Wind Turbine** — generator requiring sky access and rotor clearance.
- **Kinetic Dynamo** — generator powered entirely by an Energy worker.
- **Compressed Core Buffer** — stores short-term reserve power.
- **Gust Fan** — Power 1 directional push for entities and loose items. This is separate from the Wind Turbine.
- **Ancient Sensor** — Power 1 line sensor that detects block changes up to three blocks away.

### Storage and defense

- **Ancient Vault** — 81 normal 64-stack slots arranged as three UI pages. It integrates with base ownership and power. It does not create illegal 128-count stacks that break hopper and mod compatibility.
- **Dart Turret** — Power 3, consumes dino-crafted darts, supports guardians rather than replacing them.
- **Sanctuary Beacon** — late-game base spawn suppression with explicit exclusions.
- **Magic Turret** — endgame spell-metal defense. It draws heavy base power only while locked onto a hostile and needs an Ancient Spell Stone, a Dart Turret, compressed materials, and refined spell metal to craft.

### Engineering backlog

These remain part of the product plan but must not destabilize the core save/gameplay loop:

- Reinforced Piston — powered heavy-duty movement for automation.
- Reinforced Piston upgrade — may move obsidian but never bedrock, portals, world anchors, or block entities unless specifically supported.
- Remote Vault Terminal — opens linked Ancient Vault pages.

## Items and materials

### Discovery and control

- Fossil Fragment
- Fossil Brush
- Signal Baton — links blocks and issues contextual commands
- Dino Whistle — emergency recall of owned active companions
- Base Upgrade Core I and II
- Recovery Salve
- Berry Mash and Meat Ration
- Name Tag remains the naming method; do not duplicate vanilla behavior

### Natural materials

- Berries
- Hardwood
- Ancient Silk
- Sulfur
- Small Dinosaur Meat
- Large Dinosaur Meat
- Small Dinosaur Bone
- Large Dinosaur Bone
- Pteranodon Wing Fragment
- T. rex Tooth
- Dodo Feather

Personally killing one of your dinosaurs permanently removes it from the roster and drops size-appropriate meat and bone. Pteranodons also drop a Wing Fragment and Tyrannosauruses drop Teeth. Ordinary mob defeats still send owned dinosaurs to recovery, and expeditions never award meat or bone.

### Technology materials

- Raw Ancient Metal
- Ancient Metal Ingot
- Compressed Ancient Metal
- Ancient Core
- Ancient Circuit
- Compressed Core
- Primordial Ingot

There is no Raw Ancient Spell Ingot. Primordial Ingots are created by combining high-tier Ancient Metal with fossil/echo essence in the Ancient Furnace and Runic Anvil chain.

### Equipment

- Primordial Sword
- Ancient Reforged Bayonet
- Crafted Dart variants: stone, sulfur, tranquilizing, and echo
- Optional armor or utility equipment only after the core weapons have unique companion interactions

## Base UI

The Command Table screen has six tabs:

1. **Overview** — power, food, active count, alerts, and base radius.
2. **Roster** — active/reserve cards, mutations, genes, level, mood, hunger, health, and current state.
3. **Work** — station priorities, automatic/manual assignments, block reasons, and progress.
4. **Logistics** — source/destination links, filters, pantry reserves, and overflow warnings.
5. **Power** — producers, consumers, priorities, reserve buffer, and validation failures.
6. **Settings** — access/team policy, overnight work, recall, name, radius visualization, and dismantle safety.

UI principles:

- Every red or yellow state has a plain-language reason and suggested fix.
- Icons are accompanied by tooltips; color is never the only signal.
- Server-confirmed changes animate only after acknowledgement.
- Roster cards show a species portrait generated from authored game art, not AI promotional imagery.
- Common actions take one or two clicks; advanced routing remains available without dominating the first screen.

Overhead indicators are limited to actionable states: hungry, no food, blocked route, under attack, injured, and no bed. Routine numbers remain in the table UI to avoid visual noise.

## Balance guardrails

- Off-specialty efficiency is exactly 45% before genome, mood, and level modifiers.
- No random hatch result blocks progression.
- Premium genetics improve probability, never guarantee a perfect roll.
- A guardian is useful from the first hostile night, but non-guardians are not disposable.
- A turret consumes power and ammunition and cannot cover an entire upgraded base alone.
- Automatic feeding is convenient but consumes meaningful resources.
- Transport distance matters enough to reward layout without making pathfinding the dominant game.
- Environmental generators require honest placement checks and cannot be trivially hidden in a wall.
- Production rates are configured in one balance layer and covered by tests; no magic constants in AI code.

## Release definition of fun

The mod is ready for public release only when a new player can, without commands or documentation:

1. discover and understand an egg;
2. hatch a companion and see its individuality;
3. craft and place a Command Table;
4. feed, rest, assign, and recall the companion;
5. watch it visibly complete a useful job;
6. understand a blocked job from UI feedback;
7. survive a base attack with a guardian or recover an injured companion;
8. route an output into a chest or pantry;
9. generate and spend power;
10. reach one meaningful ancient-tech reward.

Twelve species and dozens of blocks do not compensate for failure of this ten-step experience.

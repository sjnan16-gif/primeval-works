# **PRIMEVAL WORKS**

Primeval Works is a NeoForge mod about building a working base around prehistoric companions.

You find physical eggs in the world, hatch a dinosaur with its own quality and mutations, bring it into your crew, and give it real work. The result is not a row of passive stat bonuses. Dinosaurs walk to machines, carry visible cargo, eat, sleep, defend the base, leave on expeditions, and keep their orders when the world closes.

Built for CurseForge ModJam 2026: Echoes of the Past.

## **THE LOOP**

Find an egg. Hatch a companion. Build a Command Table. Set up food and power. Assign work. Route the output. Upgrade the base. Push into harder expeditions and ancient technology.

The current roster has eight playable species:

- Tyrannosaurus
- Triceratops
- Velociraptor
- Stegosaurus
- Parasaurolophus
- Pteranodon
- Dodo
- Spinosaurus

Each species has its own size, diet, health, movement, work profile, combat behavior, animations, and passive. Individual dinosaurs also roll birth quality, scale, a restrained hue variation, and two possible mutations:

- **Huge** increases physical size and raises work, movement, health, and damage.
- **Albino** preserves the original texture shading while changing body pigment and pupils. It strongly improves work and movement at the cost of health.

Both can appear on the same dinosaur. A companion keeps the same identity everywhere: in the world, the active crew, the depot, an expedition, or recovery.

## **A BASE THAT ACTUALLY RUNS**

The Command Table is the center of one owned base. It manages energy, seven active slots per page, a fourteen-slot upgraded crew, depot storage, recovery, assignments, expeditions, and the upgrade tree.

There are five base specialties:

- **Transport** moves real item stacks between chosen inventories.
- **Fire** tends furnaces, the Ancient Furnace, and the Processor.
- **Energy** works Wind and Water Turbines.
- **Crafting** consumes real ingredients at a selected Crafting Table.
- **Expedition** sends a companion through one of five timed risk and reward tiers.

The systems are designed to connect. A Fire worker does not teleport fuel into a machine. A Transport worker supplies it. A Crafting dinosaur can bring ingredients to its table, but the finished result still needs logistics. Food Boxes can be restocked by the same routing system that feeds the rest of the base.

Transport routes support selected sources and destinations, item filters, exact or tag matching, source reserves, destination targets, batch size, priorities, fallback behavior, repeated routes, double chests, Ancient Barrels, Food Boxes, vanilla furnaces, the Processor, and other compatible inventories. Carried stacks are visible above and on the dinosaur while they are in motion.

Work has physical timing. A companion must reach the station and perform the job. Specialty stars, level, birth quality, mood, mutations, cargo weight, and species passives affect the result. Hunger drain has a persisted cap, so a fast machine cannot accidentally empty a dinosaur every few seconds.

## **FIELD COMMAND**

Home, Stay, and Follow are saved command modes. A player starts with one follower and can unlock up to three.

The Dino Whistle gives compatible followers work that would not make sense inside a fixed base route:

- **Quarry** mines a connected material or a marked three-dimensional region.
- **Lumber** fells one connected tree from the bottom upward.
- **Harvest** finds mature crops, harvests them, and replants them.
- **Collect** retrieves loose items, with an optional creative-style item filter.

Quarry capacity grows with the dinosaur's level. A selection that is too large turns red and states the level it needs. Active marked areas stay visible while the whistle is held, so multiple followers can work without erasing each other's orders.

Field actions are checked again on the server. The whistle cannot break machines, inventories, protected blocks, unbreakable blocks, or unloaded chunks. It respects `mobGriefing` and NeoForge break events, validates ownership and follower limits, and caps every connected or marked search. Client packets request an action; they never decide its result.

## **POWER AND MACHINES**

Primeval Works uses a base-level energy network rather than cables.

- Basic Wind Turbines generate at 60% output and can be upgraded in the Processor with a Pteranodon Wing Fragment.
- Water Turbines need their three bottom cells waterlogged. They produce a small passive current at 20% of normal output and accept an Energy worker for full production.
- A worked Water Turbine can couple its two nearest available submerged turbines at 55% and 35% extra output. Coupling does not recurse.
- The Ancient Furnace trades energy draw for processing speed through its authored throttle.
- The Processor handles ancient refinement, compression, Magic Shards, and turbine upgrades.
- The Premium Egg Incubator improves hatch quality and mutation odds while displaying the real egg inside its glass chamber.
- The Laser Observer watches up to five blocks and emits redstone when it detects an update. Its beam stops at walls.
- The Dart Turret consumes physical darts.
- The late-game Laser Turret tracks visible hostiles and fires a synchronized multi-contact beam. It cannot acquire or damage targets through walls.
- The Ancient Spell Stone suppresses ordinary hostile spawning inside a powered base radius.

Power connections and generation survive reloads. Consumers only request energy while they have actual work, so an empty furnace or idle defense does not quietly drain the buffer.

## **MOUNTS BUILT AS MOVEMENT SYSTEMS**

The Pteranodon is not creative flight with a model attached. It accelerates, banks into turns, transitions between flapping, hovering, and gliding, reacts to dive angle, changes FOV with speed, consumes stamina, and keeps the rider attached to the animated saddle position. High-speed routes also let a Transport Pteranodon use flight when a normal ground path is unreasonable.

The Spinosaurus is its underwater counterpart. It has smooth pitch and bank steering, surface breaches based on speed and angle, reduced rider oxygen drain, a separate land sprint with stamina, terrain-aware body lean, mounted land attacks, and a rider position sampled from the animated model. Unmounted Spinosaurus uses its own surface behavior rather than repeatedly jumping out of the water with vanilla float AI.

Both mounts support Huge scaling without applying the rider offset twice.

## **LIVING COMPANIONS**

Dinosaurs have hunger, mood, health, level, diet, sleep, combat, and recovery. They turn with the head leading the body, keep locomotion beneath compatible action animations, look around when idle, use species-sized workstation reach, and switch to running, flying, or swimming when a follower needs to catch up.

Large companions need stable ground before sleeping. Heavy footsteps use authored animation contacts. Combat damage lands at the matching attack frame. Hostile mobs can target the base crew, and capable companions defend their base or owner.

Ordinary defeat sends an owned dinosaur into the recovery row instead of deleting it. An intentional player kill is permanent and yields the appropriate dinosaur materials. Expedition, depot, recovery, and active-world states are mutually exclusive, so one UUID cannot legitimately exist in two places.

## **PROGRESSION**

Wild Small, Big, and Large eggs use separate weighted species pools and become rarer with size. Right-click hatches an egg in place and awards Fossil Fragments; Silk Touch preserves it for incubation.

Nesting Treats let two healthy dinosaurs of the same species breed. Bred eggs average and improve the parents' quality, slightly improve fresh mutation odds, and strongly favor mutations already present in either parent.

Expeditions supply materials that do not belong in an ordinary crafting recipe: Hardwood, Silk, Sulfur, Fossil Fragments, Ancient Cores, Raw Ancient Metal, Magic Shard Fragments, species trophies, and other tiered rewards. Finished and compressed materials still require their proper machines. Expeditions do not hand out processed metal, meat, or bones as a shortcut around the rest of the mod.

The complete current content and progression rules are listed in [CONTENT_CATALOG.md](docs/CONTENT_CATALOG.md).

## **SAVE AND MULTIPLAYER DESIGN**

Gameplay is server-authoritative. Every companion uses a stable UUID and one saved lifecycle state. Ownership, crew order, needs, genetics, saddle state, carried cargo, work routes, priorities, expeditions, recovery, and field assignments are serialized and reconciled at world and chunk boundaries.

Inventory transfers simulate before committing. Invalid or interrupted transitions return cargo to the base instead of deleting or duplicating it. Assignment payloads recheck ownership, dimension, distance, active status, station type, capacity, and permissions on the server. Nearby bases use a stable nearest-table rule so inventories and defenses cannot leak across ownership boundaries.

One player can own one Command Table, and separate tables require 72 blocks of horizontal space.

## **CONFIGURATION**

`primevalworks-client.toml` controls presentation such as UI sound, heavy footsteps, mount FOV and banking, stamina HUDs, hatch cards, and Pteranodon wind.

`primevalworks-server.toml` controls authoritative balance: needs, recovery, breeding, mutation odds, work rates, transport, energy, expeditions, mounts, combat, defenses, and machine speed and cost. A dedicated server owner edits the server file; connected clients receive the synchronized gameplay values.

## **TECHNICAL TARGET**

- Minecraft 26.1.2
- NeoForge 26.1.2.95
- Java 25
- GeckoLib 5.5.2
- Optional JEI integration

The mod uses one shared dinosaur implementation with data-driven species profiles, bounded world searches, transaction-safe inventories, synchronized render state, and a common authored UI toolkit.

## **BUILD AND TEST**

```powershell
.\gradlew.bat build
```

To launch the development client:

```powershell
.\gradlew.bat runClient --no-configuration-cache
```

The repository also includes a shared IntelliJ **Primeval Works Client** Gradle configuration and `run-client.bat`.

Release changes pass unit and resource contracts, NeoForge GameTests, an integrated client resource/model load, and a dedicated-server startup. Current coverage includes ownership transitions, recovery, expedition reload, work persistence, transport transactions, machine processing, powered defenses, egg behavior, mutations, mounts, follower movement, quarrying, harvesting, collection, and energy assignment.

See [TESTING.md](docs/TESTING.md) for the exact verification workflow and [BACKEND_ARCHITECTURE.md](docs/BACKEND_ARCHITECTURE.md) for the state and networking design.

## **LICENSE**

Source code and original assets are All Rights Reserved. Third-party attributions are kept in [LICENSES](LICENSES). See [LICENSE](LICENSE) for the project license.

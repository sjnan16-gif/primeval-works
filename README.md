# **PRIMEVAL WORKS**

Primeval Works is a NeoForge mod about hatching prehistoric companions and turning an empty camp into a living, automated base.

Every dinosaur is a real creature with its own quality, size, needs, strengths, movement, and personality. They walk to their stations, carry visible items, eat from Food Boxes, sleep, defend the base, leave on expeditions, and remember what they were doing after the world closes.

This is our entry for CurseForge ModJam 2026: Echoes of the Past.

## **CONTENTS**

[The Idea](#the-idea) | [Command Table](#the-command-table) | [Dino Whistle](#the-dino-whistle) | [Base Automation](#a-base-that-actually-runs)

[Energy and Machines](#energy-and-machines) | [Mounts](#flying-and-swimming-mounts) | [Living Companions](#living-companions) | [Progression](#eggs-breeding-and-expeditions)

[Multiplayer and Saves](#multiplayer-and-saves) | [Configuration](#configuration) | [Technical Target](#technical-target)

## **THE IDEA**

The loop is simple to understand. Find an egg in the wild, hatch a companion, build a Command Table, and start giving your crew useful work. From there you can route materials, generate energy, improve machines, unlock more crew slots, and prepare stronger dinosaurs for dangerous expeditions.

The current roster includes Tyrannosaurus, Triceratops, Velociraptor, Stegosaurus, Parasaurolophus, Pteranodon, Dodo, and Spinosaurus.

Each species has its own diet, health, movement, work profile, combat behavior, animations, and passive ability. Individual dinosaurs are born with a quality score, a natural size, and a slight color variation. They may also hatch with Huge, Albino, or both mutations.

**Huge** increases the dinosaur's physical size, health, damage, movement, and work ability. **Albino** keeps the original texture shading while changing the body pigment and pupils, giving a much larger work and movement bonus in exchange for lower health.

The same dinosaur keeps the same identity everywhere. Moving it into the depot, sending it on an expedition, reloading the world, or waiting for it to recover does not create a replacement copy or erase its setup.

## **THE COMMAND TABLE**

The Command Table is the center of your base. It holds the active crew, depot, recovery area, energy reserve, assignments, expeditions, and upgrade tree. It has obsidian level durability because losing the table would break the identity of the base. Ordinary machines use normal pickaxe or axe mining rules and are practical to move in Survival.

A new base begins with seven active companion slots. Upgrades eventually open a second page for fourteen active dinosaurs. Other branches improve the base radius, work speed, needs, energy, expeditions, follower capacity, and related systems. Upgrade costs use real progression materials rather than an abstract point currency.

There are five base jobs. Transport moves physical item stacks between inventories. Fire operates furnaces and processing machines. Energy works turbines. Crafting uses real ingredients at Crafting Tables. Expedition sends a companion away on a timed route with its own danger and reward pool.

These jobs are meant to work together. A Fire worker does not magically create fuel. A Transport worker supplies it. A Crafting dinosaur can bring ingredients to its table, but logistics still handles the finished result. The same transport system can keep Food Boxes stocked while feeding every other production route.

Transport can use chosen sources, destinations, item filters, exact item or tag matching, source reserves, destination targets, batch sizes, priorities, fallback rules, double chests, Ancient Barrels, Food Boxes, vanilla furnaces, the Processor, and the rest of the compatible machines. The item and stack count remain visible while a dinosaur carries them.

## **THE DINO WHISTLE**

The Dino Whistle is the field command system. It is not a decorative menu and it is not a second version of the Command Table. It gives following dinosaurs jobs that only make sense out in the world.

Right clicking the whistle opens its compact setup. The menu lets you choose the field order, its behavior, its working leash, and any item filter that the selected job needs. The search catalogue opens with the same animated inventory language used by the base work planner, so choosing a filter never consumes the item.

**Quarry** has two useful forms. Connected Quarry starts from one marked block and follows matching connected material. Area Quarry uses two marked corners and clears the chosen volume. The first corner remains visible while you choose the second. The complete selection receives a moving green outline, and the compatible follower choices appear directly over the marked area instead of opening another dashboard.

Quarry size scales with the chosen dinosaur's level. A selection beyond its current ability turns red and tells you the level it needs. Once work begins, that region stays visible while the whistle is held. You can mark another site for another follower without losing the first dinosaur's active boundary.

**Lumber** starts from one marked trunk. The dinosaur reaches the lowest safe log it can work, performs the strike, and fells the connected tree upward in a quick sequence.

**Harvest** is automatic. Its assigned follower searches nearby for mature crops, harvests them, and replants them. **Collect** is also automatic and retrieves loose items inside the chosen leash. Collect can be left open to every item or narrowed through its creative style filter.

Only compatible field specialists can accept each order. The server checks ownership, active crew membership, follower limits, distance, loaded chunks, level capacity, block safety, permissions, `mobGriefing`, and NeoForge break events before allowing the action. The whistle cannot be used to mine machines, inventories, protected blocks, or unbreakable blocks, and the client never gets to decide whether a block is valid.

Home, Stay, and Follow are saved companion commands. You begin with one follower slot and can unlock three. Followers keep up using the movement that belongs to their species. Land dinosaurs run when needed, Pteranodon flies, and Spinosaurus swims rather than repeatedly teleporting beside the player.

## **A BASE THAT ACTUALLY RUNS**

Work is physical. A dinosaur must reach the correct block and spend time performing the action. Specialty, level, birth quality, mood, mutations, cargo weight, and species passives all affect the result.

Dinosaurs do not stand inside a machine to work it. Their usable distance accounts for their real size, while pathfinding chooses a nearby stance with enough room for the model and animation. Followers keep a stable route across uneven ground and water instead of constantly choosing a new direction. If navigation genuinely fails, they retry the route, attempt a clear nearby approach, then safely return near their destination as a final fallback.

Cargo transfers are simulated before they are committed. Interrupted routes return the carried stack to the base whenever possible. Depot changes, recovery, expeditions, recalls, chunk reloads, and logout all reconcile the same saved state, preventing the same companion or item from legitimately occupying two lifecycle states at once.

## **ENERGY AND MACHINES**

Primeval Works uses a base energy network without cables.

Basic Wind Turbines produce a reduced output and can be upgraded in the Processor with a Pteranodon Wing Fragment. Water Turbines need their three bottom cells waterlogged. A valid Water Turbine supplies a small passive current equal to twenty percent of normal production, then adds its worked output when an Energy dinosaur is assigned. Generation is deliberately conservative so upgraded bases still need more than one well planned source.

A worked Water Turbine can also turn its two nearest available submerged neighbors. The first linked turbine contributes fifty five percent output and the second contributes thirty five percent. That effect never chains into another set of turbines.

The Processor handles ancient refinement, compression, Magic Shards, and turbine upgrades. The Ancient Furnace trades higher energy draw for faster processing through its physical throttle. The Premium Egg Incubator improves hatch quality and mutation odds while showing the real egg inside its glass chamber.

The Laser Observer watches up to five blocks and sends a redstone pulse when its clear beam detects an update. The Dart Turret uses physical darts. The late game Laser Turret tracks visible hostiles and fires a synchronized beam with several damage contacts. Neither laser can see or continue through a wall.

Machines only request power while they have real work. An empty furnace or idle defense does not quietly drain the reserve.

## **FLYING AND SWIMMING MOUNTS**

Pteranodon flight is a complete movement system. It accelerates, banks into turns, changes between flapping, hovering, and gliding, reacts to dive angle, changes field of view with speed, consumes stamina, and keeps the player attached to the animated saddle position. A Transport Pteranodon can also fly a long cargo route when the ground route is unreasonable.

Spinosaurus is the underwater counterpart. It has smooth pitch and bank steering, clear acceleration, surface breaches based on speed and angle, reduced rider oxygen drain, a separate land sprint with stamina, terrain aware body lean, and mounted land attacks. The player position follows the animated locator in the model instead of using the normal vanilla seat.

Both mounts support the Huge mutation without applying their rider offset twice.

## **LIVING COMPANIONS**

Dinosaurs have hunger, mood, health, level, diet, sleep, combat, and recovery. Their head begins a turn before the body follows. Compatible action animations keep locomotion in the legs. Idle dinosaurs look around, blink, and roam without abandoning the base.

Large companions search for stable ground before sleeping. Heavy footsteps use the authored animation contacts. Combat damage lands at the matching attack moment. Hostile mobs do not naturally choose companions as targets, while capable combat dinosaurs still defend their base or owner when a threat appears.

An ordinary defeat sends an owned dinosaur to recovery. An intentional player kill is permanent and awards the matching materials. Recovery, expedition, depot, and active world states are kept separate.

## **EGGS, BREEDING, AND EXPEDITIONS**

Small, Big, and Large eggs use different weighted species pools. Larger eggs are rarer, and some species can appear in more than one size at different odds. Right clicking hatches the egg and awards a size weighted stack of Fossil Fragments. Silk Touch preserves the block for incubation.

Nesting Treats allow two healthy dinosaurs of the same species to breed. Once both are fed, they walk together, produce hearts, and leave the new egg on the ground between them. The egg averages and slightly improves the parents' quality, receives a small bonus to fresh mutation odds, and strongly favors a mutation carried by either parent.

Expeditions supply materials that do not belong in an ordinary recipe, including Hardwood, Silk, Sulfur, Fossil Fragments, Ancient Cores, Raw Ancient Metal, Magic Shard Fragments, and rare species trophies. Finished and compressed materials still need their proper machines. Expeditions never hand out processed metal, meat, or bones as a shortcut around the rest of the mod.

The complete current content and progression rules are recorded in [CONTENT_CATALOG.md](docs/CONTENT_CATALOG.md).

## **MULTIPLAYER AND SAVES**

Gameplay is authoritative on the server. Each companion has one stable UUID and one saved lifecycle record. Ownership, crew order, needs, genetics, saddle state, cargo, work routes, priorities, expeditions, recovery, command mode, and whistle assignments are serialized and checked again when worlds and chunks load.

Nearby bases use a stable nearest Command Table rule. One base cannot borrow another player's inventory, turbine, worker, defense target, or energy consumer. A player may own one Command Table, and different tables need seventy two blocks of horizontal spacing.

Login restoration first loads the saved companion chunks and allows their entities to enter the world before recreating anything genuinely missing. This prevents an old snapshot from producing a second copy while the real dinosaur is still loading.

## **CONFIGURATION**

`primevalworks-client.toml` controls presentation such as interface sound, heavy footsteps, mount field of view, banking, stamina displays, hatch cards, and Pteranodon wind.

`primevalworks-server.toml` controls authoritative balance such as needs, recovery, breeding, mutation odds, work rates, transport, energy, expeditions, mounts, combat, defenses, machine speed, and machine cost. A dedicated server owner edits this file and connected clients receive the synchronized gameplay values.

## **TECHNICAL TARGET**

Primeval Works targets Minecraft 26.1.2, NeoForge 26.1.2.95, Java 25, and GeckoLib 5.5.2. JEI support is optional.

The project uses one shared dinosaur implementation with species profiles, bounded world searches, safe inventory transactions, synchronized render state, and a shared authored interface toolkit.

Release changes are checked with unit and resource contracts, NeoForge GameTests, an integrated client load, and a dedicated server startup. The final archive is also compared byte for byte with every compiled class and processed resource, then rejected if development models, logs, source files, or workspace folders enter the JAR. Current coverage includes ownership transitions, recovery, expedition reload, work persistence, transport transactions, machine processing, powered defenses, breeding, eggs, mutations, mounts, follower movement, Quarry, Lumber, Harvest, Collect, mining tools, model particles, highlights, and energy assignment.

## **LICENSE**

Source code and original assets are All Rights Reserved. Third party attributions are kept in [LICENSES](LICENSES). See [LICENSE](LICENSE) for the project license.

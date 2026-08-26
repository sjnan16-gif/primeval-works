# Primeval Works - Current Content Catalog

This file describes the build as it exists now. "Placeholder" means the block or item is registered and craftable, but its promised gameplay loop is not complete yet.

## Eggs, hatching, and breeding

Wild generation places three physical egg sizes:

- Small: Field Dodo, Velociraptor, or Pteranodon.
- Big: Parasaurolophus, Stegosaurus, or Triceratops.
- Large: Tyrannosaurus or Spinosaurus.

Right-clicking a wild egg hatches a random species from its size class and gives ownership to that player. The hatchling joins an open Command Table slot, enters the depot if the active crew is full, or waits safely in ownership data if the player has not built a Command Table yet. Silk Touch collects the physical size egg instead.

The Premium Egg Incubator holds one egg without opening a menu. It needs 2 E/S from the base network. Right-click with an egg to start; sneak-right-click with an empty hand to remove it. Small, big, and large eggs start at 20, 40, and 60 seconds, with a modest increase for better genetics and mutations. The incubator locks its selected species and genetics into the egg item, so removing and reinserting it cannot reroll the result.

Nesting Treats provide controlled breeding:

- Two owned, living dinosaurs of the same species each need one treat.
- Both need at least 50% hunger and 55% mood and must be awake, calm, nearby, and off cooldown.
- The first treat marks a waiting parent. Giving the second treat to a matching partner creates a genetic egg.
- A bred egg averages its parents' birth quality, then gains 4-10 points.
- Huge or Albino has a 65% inheritance chance from one matching parent and 88% from two. A missing parent trait can still appear at a slightly improved bred-egg rate: 9% Huge and 1.2% Albino.
- Both parents lose 8 hunger and begin a ten-minute cooldown.
- Nesting Treats craft in pairs from Berries, Silk, Gold, a vanilla Egg, and a Fossil Fragment. They also appear from Deep Wilds and harder expeditions.

Authored entity assets are currently wired for Tyrannosaurus, Field Dodo, Velociraptor, Pteranodon, Stegosaurus, Parasaurolophus, and Spinosaurus. Triceratops is the only active-roster species still using a temporary model; inactive compatibility species remain hidden from normal progression until their art is ready.

## Base and utility blocks

| Content | Status | Current behavior |
|---|---|---|
| Command Table | Working | A single-block base controller with a one-block footprint. It stores up to 500 base energy before upgrades and manages 7-14 active dinosaurs, the depot, work orders, power connections, and the upgrade tree. The starting work radius is 50 blocks. One table is allowed per player and tables need 72 blocks of horizontal separation. |
| Food Box | Working | A 10-slot food-only row joined above the complete player inventory panel. A dinosaur below 50 hunger walks over and eats compatible food until full. Transporters can restock it. |
| Ancient Furnace | Working | Smelts vanilla furnace recipes using base energy only. Its authored throttle ranges from 2.5-10.5 E/S and trades power draw for 0.75x-4.2x processing speed; Fire workers can tend it and Transport workers can supply input or collect output. It releases its energy demand while empty. |
| Wind Turbine | Working | A 3-by-4 multiblock. It needs open air above the rotor and exactly one assigned Energy worker. Zero-to-four Energy stars produce 1.5, 2, 4.5, 7.5, or 11 E/S before level and mutation bonuses. |
| Water Turbine | Working | A 3-by-3, one-block-thick cog whose multiblock cells retain water. Its bottom three cells must be waterlogged, and exactly one Energy worker can be assigned. It generates 1.5 times the worker's normal Energy output. |
| Premium Egg Incubator | Working | One physical egg, visible progress, genetic improvement, persistence, and automatic hatching into the crew/depot. Needs 2 E/S and a Command Table in range. |
| Processor | Working | A four-slot fuel machine with material, fuel, catalyst, and output slots. It refines both ancient ingot chains and compresses Ancient Metal or Cores. Transporters honor per-slot insert/extract controls, and an assigned Fire worker accelerates valid active work. |
| Ancient Barrel | Working | A 54-slot server-authoritative container included in transport routes and base inventory indexing. |
| Reinforced and Sticky Reinforced Pistons | Working | Require 1 E/S plus redstone. Both push Obsidian and Crying Obsidian through the normal piston movement rules; the sticky version also pulls either block back. They still reject bedrock, block entities, and other immovable world anchors. |
| Laser Observer | Working | Requires 1 E/S, detects block updates along its facing direction up to five blocks away, and emits a vanilla-style redstone pulse. Its rendered beam and detection both stop at the first colliding block. |
| Dart Turret | Working | Power-connectable at 3 E/S, automatically tracks the nearest hostile in 18 blocks, consumes Darts from its internal 3x3 magazine, and fires real crossed-sprite projectiles. It requests power only while armed with a live target. |
| Magic Turret | Working | A late-game spell-metal defense upgrade. It consumes 5 E/S only with a visible live target, tracks hostiles within 24 blocks, and deals four 5-damage pulses five ticks apart through a synchronized purple beam with a white core. Walls block acquisition, cancel an interrupted burst, and cut the rendered beam at the collision surface. |
| Ancient Spell Stone | Working | Power-connectable at 4 E/S and suppresses hostile spawn position checks within 48 blocks while powered. |

## Automation that works now

- Transport: selected source containers to selected destination containers, including double chests, Ancient Barrels, Food Boxes, furnaces, and the Processor. Item filters, source reserve, destination target, batch size, block priority, fallback route, exact/tag matching, repeat modes, and real carried stacks are active.
- Fire: vanilla furnaces, Ancient Furnaces, and the Processor. Fire workers accelerate valid active recipes but never silently teleport their inputs or outputs.
- Energy: one Energy dinosaur per valid Wind or Water Turbine, continuous generation, environment validation, stored base energy, consumer assignment, persistence, and per-block demand.
- Crafting: a Crafting dinosaur uses a selected vanilla Crafting Table, checks the loaded base containers for real recipe ingredients, consumes them transactionally, and leaves the result for a Transport worker.
- Expeditions: five authored skill-scaled tiers with fixed weighted pools, risk, persistence, and Transport-compatible return cargo.
- Feeding: Food Boxes automatically serve compatible food at the authored hunger threshold.
- Recovery/depot: active work orders, needs, genetics, cooldowns, expeditions, and carried cargo survive depot storage and world saves. Storing a carrier returns its held stack as base cargo instead of deleting or duplicating it.

## Mounts

- Pteranodon: saddle-gated flight with acceleration, gliding, banking, camera/FOV response, stamina, mounted player posing, and a hard water rejection so it cannot be used as a submarine.
- Spinosaurus: saddle-gated underwater travel with smooth acceleration, pitch and bank steering, no stamina, ten-times-slower rider oxygen loss, Shift+forward land sprinting, land-only mounted left-click attacks, and angle/speed-driven surface breaches that remain in the swim pose until landing or re-entry. Riding does not alter vanilla underwater fog.

## Automation still missing or intentionally manual

- Ancient Furnace has no unique high-tier recipes beyond powered furnace behavior.
- Breeding is intentionally initiated by feeding two treats; it is not a passive breeding pen.
- Egg insertion into the Premium Incubator is intentionally manual. Incubation and crew/depot delivery are automatic afterward.
- Crafting results are dropped at the worktable and require a separately configured Transport dinosaur. This is intentional division of labor.

## Work rules and balance

- Work ratings map to 20%, 20%, 45%, 65%, and 100% action speed for zero through four stars. Off-specialty work never exceeds 45% unless the species is authored with a stronger rating.
- A full-load four-star Transport action takes 1.4 seconds at each container. Heavy cargo slows weak Transport species more than experts.
- Energy output is 2, 2, 5, 8, or 11 E/S by Energy stars.
- Night shift keeps a dinosaur awake but drains mood at 2.3 times the normal duty rate.
- Work pauses for combat, sleep, recovery, starvation at 10 hunger or lower, schedule conflicts, and emergency feeding.
- Work hunger loss uses one persisted species interval no matter how many actions finish during that window.
- Huge increases scale by 18% and work, damage, movement, and health by 20%.
- Albino preserves texture shading through a hue algorithm, raises work/damage/movement and mount speed by 40%, and lowers health by 20%.
- Huge and Albino can occur together. Fossil Fragment restores an Albino dinosaur's original pigment without removing its mutation stats.
- Right-click hatching a wild egg drops one Fossil Fragment stack containing 1-3 fragments. Small/Big/Large eggs use progressively better bonus-fragment odds.

## Expedition materials

Expeditions are the main source of Berries, Dodo Feathers, Fossil Fragments, Hardwood, Sulfur, Silk, Nesting Treats, Pteranodon Wing Fragments, Tyrannosaurus Teeth, Core, Raw Ancient Metal, and Raw Ancient Spell Metal at authored tiers. Only the Primordial Frontier has the very rare direct Compressed Core jackpot.

Expeditions never award dinosaur meat, dinosaur bones, Compressed Ancient Metal, finished Ancient Metal/Spell ingots, weapons, or saddles. Meat, bones, and species trophies come from personally hunting dinosaurs; processed and finished items must go through their crafting or machine progression.

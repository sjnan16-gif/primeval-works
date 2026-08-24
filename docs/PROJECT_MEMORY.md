# Primeval Works project memory

This is the canonical, living contract for the mod. Read it before every edit. Update it whenever a decision changes. The deeper design, backend, art, writing, UI, testing, and detail contracts linked at the end remain binding; this file records the decisions most likely to be forgotten or regressed.

## Project and platform

- Workspace: `C:\Users\Hayro\Downloads\primeval-works`. The old `nnnnn (1)` folder is unrelated and must never receive Primeval Works changes.
- Minecraft 26.1.2, NeoForge 26.1.2, Java 25, GeckoLib 5.5.x.
- Namespace `primevalworks`; Java package root `com.primevalworks`.
- The server owns gameplay state. Clients send intent and render synchronized results.
- One shared dinosaur entity implementation serves all species. Species data supplies balance and visual profiles.
- One Command Table per player. Different players' tables need at least 72 blocks between them.

## Core game

- The fantasy is a smaller, highly controllable dinosaur base-automation game: hatch companions, equip a crew, automate the base, defend it, upgrade it, and explore for progression materials.
- Five specialties: Transport, Fire, Energy, Crafting, Expedition. The older Gathering job is now Expedition.
- Specialty rating is zero to four stars and is primarily species-defined. Every dinosaur can attempt every specialty, but bad off-specialty work is capped at 45% and a zero-star role may be unavailable where the activity needs specialist competence.
- Seven active crew slots are available first. Base upgrades unlock a second page up to fourteen total active slots.
- Hatching before a Command Table is valid: owned dinosaurs remain saved and can join the first claimed base later.
- Dinosaurs have hunger, mood, health, level, birth quality, random scale, and slight procedural hue variation. Hunger, mood, health, jobs, expeditions, cargo, recovery, genetics, saddle state, and ownership must persist through chunk unload, logout, restart, depot moves, and death.
- Food Box: below 50 hunger, an available dinosaur stops safely, eats valid species food from the box, and refills. Diets are species-aware; large carnivores do not eat berries.
- Dinosaurs sleep at night unless assigned to a night shift. Night work drains mood/sanity 130% faster and the assignment UI must warn the player.

## Genetics and mutations

- Only two mutations exist: Huge and Albino. Both may occur on one dinosaur and both icons/names must be shown.
- Huge: 18% larger rendered model and collision, +20% work/movement/combat stats, additional health/damage benefits through the shared mutation multiplier, and mount attachment must scale exactly once.
- Albino: rarer; preserves texture shading while shifting the body toward white and makes the pupils authored red. It buffs work, movement, and mount speed by 40% but lowers health. Albino hatch quality trends only slightly higher.
- Fossil Fragment restores original pigmentation only. It does not remove the Albino mutation or its stats.
- Red pupils are not a global red tint. Every authored species needs an explicit pupil mask/texture treatment so only its pupils become red. Current authored dinosaurs to audit: Dodo, Tyrannosaurus, Pteranodon, Stegosaurus, Parasaurolophus, Spinosaurus.
- Breeding requires two Nesting Treats and two owned dinosaurs of the same species. Bred eggs have slightly stronger quality/mutation odds and inherit parental mutations more often.

## Ownership, depot, expedition, and recovery state machine

Every dinosaur UUID must be in exactly one authoritative state:

1. `ACTIVE_LOADED_OR_VANILLA_SAVED`: selected in an active crew slot; exactly one world entity may exist.
2. `DEPOT`: snapshot only; no world entity.
3. `EXPEDITION`: snapshot/timed state only for UI and rewards; no second active world entity may spawn.
4. `RECOVERING`: deceased-row snapshot with a recovery deadline; not active and no world entity.
5. `PERMANENTLY_REMOVED`: removed only when its owner intentionally kills it by normal player damage; it drops the defined dinosaur materials.

Rules:

- Command/admin `/kill` is a test defeat and must send an owned dinosaur to `RECOVERING`, not leave a roster ghost. Ordinary hostile damage also recovers it.
- A recovering dinosaur must be removed from active IDs before the roster payload is sent. It stays visible only in the four-slot deceased/recovery row until its timer completes.
- Recovery completion restores full health before activation/spawn and clears stale fall velocity/distance.
- Expedition dinosaurs cannot be moved, swapped, recalled, or spawned until their expedition finishes. Rejoin/reload must reconcile an expired expedition once and must never duplicate it.
- Depot/recall/expedition/recovery operations are transactions: capture the newest snapshot, remove the old world authority, update roster and active indexes once, then synchronize.
- Cargo is returned safely before depot, expedition, recovery, or reassignment. Failure to insert drops marked base cargo at the Command Table rather than deleting it.

## Global dinosaur animation and AI contract

- Authored idle, walk, run, work, sleep, swim/fly/glide, hurt, attack, and special clips remain the primary animation. Procedural motion augments them; it must never replace or quantize them.
- Movement animation speed follows real movement speed. Slow walk plays slowly; sprinting and speed mutations play the same authored gait faster without foot sliding.
- Action and locomotion are layered: attacking/working may animate the upper body while moving legs keep the current walk/run cycle. Never freeze moving legs because an upper-body action began.
- Turning is procedural and smooth. Head/neck looks toward the destination first, torso follows with spring-like lag, then hips/body align. Moving dinosaurs keep locomotion; a separate authored turn clip is not used.
- Large dinosaurs turn deliberately but may not moonwalk, spin 360 degrees, walk backward toward a target, or enter a target's body. They maintain mouth/contact distance and face a target before attack contact.
- Attack damage occurs at the authored contact frame, not at animation start. While committed to an attack, the dinosaur looks at its target. Mounted attacks look toward the rider's attacked point.
- Idle dinosaurs roam within their base, observe nearby activity, and occasionally look around. They look directly at a player only within roughly three blocks and not while attacking, working, or sleeping.
- Hitboxes and step height follow the authored true model scale plus genetics. Do not arbitrarily shrink a species to make previews fit.
- Heavy dinosaurs are not pushable and take negligible knockback. Hostile mobs and combat-capable owned dinosaurs target each other within the base.
- Sleeping uses a simple vanilla-style `Z` billboard sequence scaled to the dinosaur.
- Heavy dinosaurs may begin sleeping only when the ground under their footprint has a stable, flat 3x3 support area. Do not let a large sleep pose balance on a single block or ledge.
- Work progress billboards appear above the workstation, not above the dinosaur. Cargo uses the authored hotbar-slot frame above the carrier and the item also appears at the mouth attachment where supported.
- Work approach distance is species-sized and collision-aware. A large dinosaur works from the nearest clear contact distance; it must not force its collision box through the workstation just to satisfy a center-point distance check. Nonlethal owner hits cancel combat and allow assigned work to resume.

## Spinosaurus contract

- Spinosaurus is a top-tier large dinosaur: four-star Energy and Expedition, very high health/damage, long mouth-scale attack reach, strong water travel, and a comparatively fast authored walk. Its land walk clip is normally about 30% slower than the export, then scales continuously with actual land speed.
- It is saddle-gated. The rider must follow the authored `whereplayersits` locator, including its swim offset; never substitute `head2`, the middle of the back, or a static world-space render offset. Runtime applies one three-model-pixel downward support correction after the animated locator, and Huge scales the complete attachment/correction exactly once.
- On land: normal walk plus Shift+forward sprint, clear acceleration, stronger FOV at sprint, two-block mounted step traversal, front/rear terrain-sampled pitch over rises/descents, and a brief strong distance-aware camera impulse at each authored foot contact. The rider stays attached through steering and terrain pitch.
- Land sprint has its own synchronized 100-point stamina pool. Sprint drains it server-side, exhaustion cancels sprint until it has recovered past the restart threshold, and a compact HUD bar appears only while riding on land. Swimming/breaching never consumes this land-sprint stamina.
- Space never makes a land-mounted Spinosaurus jump. Normal block traversal comes from its step height and smoothed terrain pitch.
- In water: smooth acceleration, pitch/yaw/bank steering, no stamina, and rider oxygen drains ten times slower. Do not add Night Vision or custom underwater fog/clarity.
- Unmounted water behavior is separate from mounted swimming: a Spinosaurus uses restrained surface buoyancy and may bob at the waterline, but never inherits vanilla `FloatGoal` jump impulses, launches most of its body into the air, or holds the swim animation after leaving water. This rule must not alter mounted swim/breach controls.
- Underwater mounted attacking is disabled. A land mounted left-click attack uses the authored Spinosaurus attack and turns head/body toward the attacked point; it must never hit its own rider or mount.
- Breach: depends on swim speed and exit angle, stays in swim animation through the arc, turns down naturally, damages contacted external entities, and never damages the Spinosaurus through its own fall/landing.
- Spinosaurus UI previews must fit the exact same rectangle as every other species. Fix preview projection/bounds, never world/model scale.
- The current Spinosaurus export is the authoritative model/animation set and has six authored 512x512 state atlases: open, blink, saddled-ground open/blink, and saddled-aquatic open/blink. Runtime names normalize the source export's `undergroundmount` typo to `spino_saddled_aquatic.png`; do not rename the runtime state or replace these atlases with generated substitutes.

## Command Table block

- The Command Table is exactly one block for placement, collision, selection, ownership, rendering, and item representation. There is no live extension block.
- Its authored block model and `dino_command_table.png` are the source of truth. Never replace it with a generated shape or scale it down/up to solve an inventory-render problem.
- It is as hard as obsidian.
- The screen has seven active slots per page, base actions, energy meter, skill tree, depot, and a deceased/recovery row. Store All must actually move all eligible active dinosaurs to the depot; dragging works both directions.

## Command Table UI coordinate contract

All coordinates are in the authored 427x240 texture. They describe the usable interior; do not expand previews into the outline or the meters. Convert both rectangle edges through the screen transform and retain floating-point bounds until scissor submission.

- Active crew preview 0: `(115, 51)`, size `23x23`.
- Active crew preview `i`: x = `115 + 27*i`, y = `51`, size `23x23`, for seven slots.
- Depot living preview at column 0,row 0: `(311, 66)`, size `23x23`.
- Depot next row in the current authored sprite: `(311, 93)`, size `23x23`.
- Depot next column: `(337, 66)`, size `23x23`; x stride 26, y stride 27.
- Living-depot x stride is 26 and y stride is **27**, not the obsolete 33-pixel stride. The visible regression is diagnostic: row one fits, row two is 6 pixels low, and row three is 12 pixels low.
- Recovery/deceased preview 0: `(311, 155)`, size `23x23`; x stride 26 for four slots. Its interactive outer slot is `(309 + 26*i, 152)`, size `26x27`, and the label is below the row rather than inside a model window.
- Every entity preview is a top-down three-quarter view, fitted from species bounds to the rectangle. It follows the same smooth panel parallax with a slight cursor-reactive rotation.
- Preview models must be clipped to the transformed float rectangle. No part may leak even a fraction of a pixel into an outline, meter, neighbor slot, depot label, or outside panel during parallax, opening, hover, or dragging.
- Do not solve oversized Spinosaurus/Parasaurolophus/Pteranodon previews by changing entity size or hitbox. Correct their preview bounds/camera fit.
- All dinosaur-rendering UI, including the hatch reveal, uses the shared `DinosaurPreviewBounds`; never duplicate a partial species switch that silently treats Spinosaurus like a Dodo.

## Current content decisions

- The active jam roster is exactly eight species for now: Tyrannosaurus, Triceratops, Velociraptor, Stegosaurus, Parasaurolophus, Pteranodon, Dodo, and Spinosaurus. Brachiosaurus, Dilophosaurus, Ankylosaurus, and Pachycephalosaurus remain registry/save-compatibility entries only: do not put them in wild eggs, the creative tab, or normal progression until art is ready.
- Current authored runtime models: Dodo, Tyrannosaurus, Pteranodon, Stegosaurus, Parasaurolophus, Spinosaurus. Triceratops and Velociraptor remain clearly labeled placeholders until authored assets arrive.
- Current specialty stars in Transport/Fire/Energy/Crafting/Expedition order: Tyrannosaurus `1/4/1/1/4`, Triceratops `3/1/1/2/3`, Velociraptor `4/1/1/2/2`, Stegosaurus `1/3/1/1/3`, Parasaurolophus `1/1/3/3/1`, Pteranodon `3/1/2/1/2`, Dodo `2/2/1/2/4`, Spinosaurus `1/2/4/1/4`. The efficiency curve is `20/20/45/65/100%`; mutation and species-passive bonuses are displayed on top and may visibly exceed 100%.
- Species passives are unique and functional: Tyrannosaurus improves fire tending, Triceratops carries extra cargo, Velociraptor moves faster, Stegosaurus accelerates fire work, Parasaurolophus reduces same-owner nearby work-mood drain, Pteranodon improves long-route transport flight, Dodo improves expedition outcomes, and Spinosaurus improves energy generation. A passive aura never applies between ownerless dinosaurs or across owners.
- Endgame weapon is Ancient Reforged Bayonet using the existing spear-style combat foundation; not a bow and not a separate spear.
- Saddles: Pteranodon Saddle and Spinosaurus Saddle. No Tyrannosaurus Saddle.
- Machines/blocks include Command Table, Food Box, Processor, Ancient Furnace, Premium Egg Incubator, Water Turbine, Wind Turbine, Reinforced Piston, Sticky Reinforced Piston, Powered Observer, Dart Turret, Magic Turret, Ancient Spell Stone, Ancient Barrel, and related progression blocks/items in the content catalog.
- Removed/not planned: Dino Crafter, Cooker/Cooking Station, Extended Piston, Sticky Extended Piston, Water Wheel, Ancient Chest, Ancient Chest Opener, Super Chest, energy dust.
- Processor makes reinforced/compressed outputs and requires heavy base power plus its authored three inputs/output workflow. Compressed metal/core is not a normal expedition reward.
- Ancient Furnace uses base energy instead of fuel and has a 2.5 E/s minimum-to-slow setting through a higher-cost maximum producing up to 4.2x speed.
- The authored Ancient Furnace progress fill is cropped from `furnace_fill.png` into `(118,112)`, size `6x21`; it communicates the current smelt cycle without adding a replacement code-drawn meter.
- Water Turbine produces 1.5x the Wind Turbine output only while its 2x2x1 wheel assembly is fully underwater.
- Dart Turret has an internal 3x3, nine-slot dart magazine, consumes real Dart items, requires assigned base energy, automatically locks hostile mobs, and fires the authored Dart as a crossed-sprite arrow-style projectile. Its inventory panel uses the same warm primitive palette as the Food Box. The authored Dart texture is projectile art only; its temporary inventory icon is vanilla until a separate icon is supplied. Magic Turret is the expensive spell-metal defense upgrade: it consumes 5 E/S only while it has a target, needs no ammunition or worker, has no right-click screen, and strikes with direct magic damage. Keep its original compact 16x16-texture turret silhouette; iron, blackstone, and amethyst distinguish the magic conversion without replacing it with an overbuilt custom model. Its synchronized head tracks the nearest hostile and renders nested purple square-prism geometry around a white core. Purple/white particles appear only as a brief muzzle and impact burst at the actual shot; the beam itself remains synchronized geometry. Powered Observer projects a thin, translucent two-color red five-block detection beam with a small red endpoint sparkle and emits observer-style redstone on updates inside that beam.
- Connected active machines request power only while they have real work. An empty Ancient Furnace, invalid/blocked Processor, empty Incubator, or idle turret must not drain the base reserve. A connected turret acquires a hostile before requesting its rated power, then fires after the server ledger grants it; lack of power pauses its cooldown instead of resetting it.
- Wild eggs are world blocks in small, medium, and large pools. Right-click hatches into ownership; Silk Touch collects the block for incubation. Incubation is in-world with a timer, not a menu.
- Expeditions have five authored risk/reward tiers and fixed possible reward pools. They do not award meat/bone. Ancient metal/core/silk/sulfur and other non-craftable progression materials belong in suitable tiers; compressed materials are processor products except an intentionally rare high-tier compressed core reward.

## Recurring regressions: never reintroduce

- Command Table visual replaced, rescaled, made two blocks, or left with a two-block hitbox.
- Command Table custom entity renderer registered over the authored vanilla block model. The current table is a one-block vanilla model using `models/block/command_table_world.json` and `dino_command_table.png`; do not register the old Gecko renderer for it.
- Large species preview leaks out of a UI slot, is made tiny, or changes world hitbox/scale to compensate.
- UI scissor rounds before applying animation/parallax, causing half-pixel leaks.
- Deceased model/label uses living-depot coordinates or an entity remains active after entering recovery.
- Command `/kill` bypasses recovery and leaves a roster entry with no world entity.
- Recovery GameTests spawn a recall entity outside the forced-ticking test region, leaving it at `tickCount=0` and producing a false lifecycle failure. Recovery tests must force-load both the Command Table and dinosaur positions before invoking defeat.
- A saved expedition spawns once from active state and again from its expedition snapshot after login.
- Mutation/hue/saddle/health/work state differs between live entity, depot card, expedition card, and recovery card.
- Full health renders partially filled because the UI compares saved health against a newly recomputed max incorrectly.
- Moving attack/work cancels the legs' locomotion controller.
- Large predator turns snap, twitch, moonwalk, or rotates its body before its head.
- A mount uses a static body offset rather than its authored animated attachment, especially after Huge scaling.
- Spinosaurus logical and rendered rider sockets disagree on the forward-axis sign, use `head2` instead of the authored `whereplayersits`, or let the animated socket expire and briefly fall back to the vanilla middle seat. At yaw zero the logical seat is positive Z; camera/avatar rendering always uses the persistent seat sample or the same forward fallback, never vanilla placement. Ground samples are immediate; only aquatic samples receive a low-latency stability filter.
- Pteranodon ground state accidentally enters flying animation/stamina behavior.
- Spinosaurus breach or landing applies fall damage to itself.
- An unmounted Spinosaurus uses vanilla `FloatGoal`, repeatedly jumps out of deep water, or remains in its swim pose while airborne. Use the species-only surface buoyancy goal; never alter mounted swim physics to solve this regression.
- A small dinosaur can deadlock beside a workstation when center-to-center range is used after navigation stops. Contact checks must intersect the workstation cube against the dinosaur collision box inflated by its species work reach, and pathing should approach the block's ground-level work point.
- A Parasaurolophus mood aura treats two `Optional.empty()` owners as equal and leaks across tests/bases. Require an actual matching owner UUID before applying any companion aura.
- A Pteranodon can have stale `onGround` contact for the first lift frames and immediately cancel a valid takeoff. Keep a short takeoff-only grace window; never use it to mask genuine landing contact later in flight.
- Machine assignment accepts the wrong block/slot, loses assignments on reload, or duplicates a worker reservation.
- Multiple workers at one work target render multiple progress icons instead of one faster combined indicator.
- Transparent placeholder items are too bright or can be grabbed as real items.
- Turret heads stay baked into the world block model while a moving renderer draws a second head, or the Magic Turret beam uses particles/projectiles that can disagree with its server-side hit. World turret models contain only base/pedestal; dynamic heads and the Magic beam use the synchronized turret target, while separate item models retain the complete silhouette. Shot particles are brief feedback only and never define hit position or timing.
- A saved powered consumer silently stops after a chunk/world reload because its transient consumer-to-table binding was lost. Loaded Command Tables are indexed by position; `BaseEnergyRules.isPowered` must self-heal from the table's persisted enabled-consumer set. The powered-turret/base-energy GameTests deliberately clear the transient binding and require immediate recovery.
- Idle connected machines drain the energy buffer, and turrets gate target acquisition on that depleted buffer, creating a permanent no-target/no-power deadlock. Active consumers expose whether they have real work; the ledger sums active demand, while turrets acquire from an enabled connection before checking their granted power. The powered-turret GameTest waits with no targets, requires an unchanged reserve, then independently requires both turrets to acquire and strike.
- A species center-to-center work reach is reused as raw collision-box inflation, leaving large workers several blocks farther away than intended. Convert center reach to a collision-gap allowance by subtracting half the dinosaur width and half the workstation width before inflating.
- A worker path ends against a workstation just outside its interaction threshold and silently holds cargo forever. Its collision-aware approach clearance and its interaction reach must overlap the navigation stopping tolerance; repeat the Processor round-trip test when either value changes.
- A base inventory scan crosses into a second Command Table's storage when tables exist through commands, migration, or test setup. Every indexed container belongs to its nearest loaded Command Table, with a stable position tie-breaker.
- A turret spends power on or attacks a hostile whose nearest Command Table is another player's base. Dart and Laser targeting must remain inside the connected table's radius and nearest-table cell.
- A fluid/entity GameTest spawns outside a forced-ticking chunk and reports frozen spawn velocity as real movement. Force-load every entity and machine position whose ticks are part of the assertion.
- Concurrent combat GameTests let normal hostile acquisition replace the test's intended target. Tests for attack timing/range must keep their own target assigned; separate tests cover autonomous target selection.

## Required verification gate

- Run unit tests and `gradlew.bat build` for every code/resource change.
- Run targeted GameTests for ownership lifecycle, `/kill` recovery, active/depot/recovery exclusivity, expedition reload, mutation/saddle snapshot parity, one-block Command Table, mount self-hit protection, rider attachment scaling, work persistence, energy uniqueness, and machine slot automation.
- Start an integrated client and a dedicated server. A clean startup is not proof of play behavior; manually exercise the changed gameplay before calling it verified.
- Check `logs/latest.log` and crash reports after any reported crash or disconnect.

## Last verified checkpoint

- Command Table world rendering uses the authored one-block model again; its collision/selection is one full block and its hardness remains obsidian-class.
- Active, depot, and recovery previews use the exact authored rectangles above with inward float-to-scissor rounding. Spinosaurus has explicit preview bounds; world scale and collision are never altered to fit UI.
- Owned dinosaurs defeated by hostile damage or `/kill` enter recovery, are removed from active IDs, and do not leave a roster ghost. Intentional normal player kills remain permanent and drop species materials.
- Spinosaurus mounted underwater attacking is disabled. Land mounted attacks aim from the rider look direction, unmounted attacks keep their target in view, the rider follows animated `whereplayersits`, breach landing does not self-damage, terrain pitch is smoothed, and land gait speed follows real movement speed.
- Death recall is a single, brief render: snap down to a small scale, perform one deterministic damped size wobble while fully bright/red, then follow an eased arc toward the Command Table while fading. Never add random per-frame jitter or a second echo/model layer.
- Spinosaurus breach has a client prediction grace state so crossing the waterline cannot briefly revert it to swim/land. Mounted land attacks predict the same aim locally before the authoritative packet arrives. The first-person camera and rendered rider share one persistent animated `whereplayersits` sample with the same forward fallback as the server-side passenger/hitbox anchor; it never expires to the vanilla middle seat.
- Albino pupils use per-species UV masks. Spinosaurus currently has manually masked open-eye pupils until the authored blink texture arrives.
- Automated checkpoint on 2026-08-23 after the compact-depot/breach pass: unit/build passed; all 44 required GameTests passed; the client applied `CameraMixin`, loaded GeckoLib resources, and reached the menu without a fatal/mixin error. Exact rider/camera feel, depot pixels, hatch scale, and death wobble still require the normal human in-game pass.
- Automated checkpoint on 2026-08-23 after adopting the authored Spinosaurus `whereplayersits` locator: pure unit/build passed, all 44 required GameTests passed, and the rebuilt client loaded the updated model/animation resources without a fatal or mixin error. Ground support and aquatic jitter still require the normal human visual pass.
- Automated checkpoint on 2026-08-23 after the Spinosaurus support/surface pass: unit/build passed and all 45 required GameTests passed. The rider attachment is three model pixels lower for Normal and Huge, mounted land step height is 2.05 blocks before genetic scale, terrain pitch samples front/rear support, and only the unmounted AI uses restrained eye-level surface buoyancy. The rebuilt client loaded all ten GeckoLib models/animations without a fatal or mixin error; exact rider height, terrain feel, and waterline appearance still require the normal human in-game pass.
- Automated checkpoint on 2026-08-23 after the active-roster/workstation-defense pass: unit tests and full build passed; all 47 required GameTests passed. Coverage includes flat-area heavy sleeping, worker recovery after a nonlethal owner hit, collision-aware workstation reach, continuous energy work, Processor and chest transport, Ancient Furnace work/progress state, unmounted Spinosaurus surface buoyancy, Pteranodon takeoff/flight, same-owner Parasaurolophus mood support, both turret behaviors, observer/piston energy behavior, and ownership persistence. The rebuilt client applied Primeval Works mixins and loaded all ten GeckoLib models/animations; the dedicated NeoForge server reached `Done` with recipes and advancements loaded and no mod exception. Hatch-card spacing, exact furnace-fill pixels, large-dinosaur work stance, unmounted Spinosaurus waterline appearance, and turret screen feel still require the normal human visual pass.
- Automated checkpoint on 2026-08-23 after the authored Spinosaurus/turret-tracking pass: the new model, six animations, and all six 512x512 visual-state atlases loaded; unit tests and full build passed; all 47 required GameTests passed, including powered turret defense and Processor transport. The integrated client joined `New World`, loaded GeckoLib and JEI, and logged no render/server error or exception. The dedicated server loaded 1,535 recipes and 1,646 advancements and reached `Done`. Exact layered-beam appearance, turret head alignment, revised worker stance, and the restored no-lean Spinosaurus ground rider pose still need the normal human visual pass.
- Automated checkpoint on 2026-08-23 after the turret/stamina pass: unit tests and full build passed; all 47 required GameTests passed on a clean generated test world. Dart and the energy-only beam turret acquisition/fire are exercised independently, and the energy test clears the transient binding to prove reload self-healing. Spinosaurus land sprint has synchronized/persisted stamina and a HUD bar. Exact beam layering, turret aim, Dart palette, and land-sprint feel still require the normal human in-game pass.
- Automated checkpoint on 2026-08-24 after the live turret-energy diagnosis: unit tests/full build passed and all 47 required GameTests passed. The regression test now leaves each connected turret idle for 25 ticks, proves the reserve does not drain, then spawns separate Creepers and requires independent turret acquisition and attack. Ancient Furnace, Processor, and Incubator also release their demand while they have no valid work. A currently running user client still had the previous classes loaded, so the rebuilt behavior requires a client restart and a normal human in-world check before release.
- Automated checkpoint on 2026-08-24 after the workstation/base-isolation pass: small transporters use a collision-clear approach point whose interaction reach overlaps pathfinding tolerance, repeated Processor transport passed 10/10, and base inventory indexing assigns each container to the nearest loaded Command Table. Crafting from base storage passed 10/10 with adjacent test bases, preventing cross-base ingredient theft. The unmounted Spinosaurus surface test now force-loads its pool instead of mistaking an unticked entity's spawn velocity for buoyancy.
- Automated checkpoint on 2026-08-24 after the base-defense boundary pass: both defensive turrets only acquire hostiles inside their connected Command Table's nearest-base cell, so adjacent multiplayer bases cannot steal targets or energy demand from one another. The turret defense test passed 10/10, the complete 47-test GameTest suite passed, and unit tests/full build passed after production screen classes replaced the old prototype filenames. The release JAR contains no docs, Blockbench sources, or prototype screen classes.
- Automated checkpoint on 2026-08-24 after the Magic Turret conversion: the original compact 16x16-texture turret silhouette is retained with iron, blackstone, and amethyst materials. The turret consumes 5 E/S only while targeting, deals 16 direct magic damage at 24-block range, and uses synchronized purple/white beam geometry plus brief shot particles. Registry aliases migrate saved `laser_turret` blocks, items, and block entities to `magic_turret`; the content-registration GameTest proves all three mappings. Unit/full build passed, all 47 required GameTests passed, and the client loaded the restored models and renderer without a missing-model, texture, or mod exception. Exact beam color and turret aim still require the normal human in-world visual pass.

## Detailed companion documents

- `GAME_DESIGN.md` — progression and balance.
- `BACKEND_ARCHITECTURE.md` — authoritative state, reservations, persistence, networking.
- `DETAIL_BIBLE.md` — required polish and feedback loops.
- `ART_ASSET_CONTRACT.md` and `MODELER_BRIEF.md` — model/export/bone/animation contracts.
- `UI_ASSET_MASTER_LIST.md`, `ASSIGN_JOB_UI_PACK.md`, and `UI_STYLE_GUIDE.md` — authored UI and interaction language.
- `CONTENT_CATALOG.md` — registered content and behavior.
- `TESTING.md` — commands and verification workflow.
- `WRITING_STYLE.md` — player-facing copy.

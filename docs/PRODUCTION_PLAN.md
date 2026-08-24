# Primeval Works — 12-Day Production Plan

This plan assumes roughly eight development hours per day and a modeler working in parallel. The goal is a polished, judgeable loop first: find an egg, hatch a distinct dinosaur, assign useful work, improve the base, and defend it. Every later feature must strengthen that loop.

## Release gates

The build is only submission-ready when all of these are true:

- A new player can discover, hatch, name, store, deploy, feed, and assign a dinosaur without outside instructions.
- The Command Table survives saving, reloading, death, dimension travel, and server restart.
- Eight active dinosaurs can work around one base without obvious pathfinding stalls or severe frame loss.
- Every required resource has at least two viable acquisition paths or an intentionally guaranteed path.
- No species is mandatory for the main progression chain.
- Every menu action is validated by the server and behaves correctly in multiplayer.
- The submitted CurseForge build, source repository, description, gallery, and form are finished before the deadline.

## Daily build order

### Day 1 — Foundation and contracts

- Lock Minecraft 26.1.2, NeoForge, Java 25, and GeckoLib.
- Prove block, item, creative tab, dependency loading, local build, and dev client.
- Freeze identifiers, source layout, art contract, gameplay rules, and UI direction.

Exit condition: a clean project builds and opens in Minecraft. This gate is complete.

### Day 2 — Genome and dinosaur core

- Implement the shared `DinoEntity` and data-driven species definition.
- Roll immutable birth genes, scale, hue seed, sex-neutral display identity, and zero-to-two mutations.
- Persist genome, hunger, mood, level, XP, injury, assignment, and owner.
- Add one ugly-but-functional test dinosaur before integrating final art.

Exit condition: a dinosaur survives save/reload with exactly the same identity and statistics.

### Day 3 — Eggs, hatching, and lifecycle

- Add excavated eggs, silk-touch recovery, natural hatching, and the basic incubator.
- Implement stored versus active state as a strict server-owned lifecycle.
- Add the eight-active limit and safe spawn-position search.
- Prevent duplication across chunk unloads, death, storage, and recalls.

Exit condition: eggs hatch, dinosaurs enter the Command Table, and recall/deploy cannot duplicate them.

### Day 4 — Command Table and readable control

- Implement the roster, inspect, deploy, recall, rename, lock, and assignment packets.
- Build the first native Minecraft version of the Stratigraphic Lens UI.
- Show plain-language status before raw numbers.
- Add permissions, distance checks, stale-state rejection, and multiplayer synchronization.

Exit condition: another player can understand why a dinosaur is idle and change its assignment.

### Day 5 — Work scheduler and needs

- Add base-level work orders, reservations, cooldowns, reachability checks, and failure reasons.
- Implement the five specialties with primary, secondary, and 45% off-specialty efficiency.
- Add hunger, food preferences, mood, night sleep, beds, and interrupted-sleep penalties.
- Integrate three representative jobs: haul, gather, and craft.

Exit condition: four dinosaurs can work for a full in-game day without fighting over the same item or starving beside available food.

### Day 6 — Logistics and first progression loop

- Add food storage to the Command Table and configurable input/output routes.
- Add route filters, chest priorities, minimum-stock rules, and overflow behavior.
- Implement hardwood, silk, sulfur, ancient metal, and a small coherent recipe chain.
- Add in-world hints and advancements that teach excavation, hatching, assignment, and feeding.

Exit condition: the base can automatically gather, move, process, and consume one complete resource chain.

### Day 7 — Power and machines

- Implement the base power network as cached producers and consumers, not a world scan.
- Add water turbine, wind turbine, Processor, Ancient Furnace, and one powered utility block.
- Give power shortages a clear priority order and a human-readable UI reason.
- Keep remaining machines data-driven so they are cheap to add after the network is proven.

Exit condition: a visible producer powers two consumers, handles shortage, and resumes cleanly after reload.

### Day 8 — Danger, guarding, and recovery

- Add guard posts, threat rules, retreat thresholds, injury, rest, and revive/recovery behavior.
- Make only combat-capable species actively fight; workers flee or seek a guard.
- Add hostile targeting protection so raids create pressure without random unavoidable losses.
- Tune T. rex, Spinosaurus, Dilophosaurus, and Velociraptor into distinct combat niches.

Exit condition: an attack creates a readable base emergency and recovery cost, not permanent surprise deletion.

### Day 9 — Twelve-species integration

- Integrate all delivered models, textures, eggs, animations, sound placeholders, and species data.
- Validate bone names and animation keys automatically.
- Tune primary and secondary jobs, appetite, movement, carrying capacity, passives, and combat flags.
- Add spawn/discovery tables without making any one egg frustratingly rare.

Exit condition: every species is identifiable, useful, hatchable, animated, and free of missing-resource errors.

### Day 10 — Progression, rewards, and spectacle

- Add Ancient Barrel, Processor, premium incubator, and only the best-performing extra machines.
- Add Ancient Spell Ingot, compressed materials, and the Primordial Sword.
- Make endgame rewards solve real base problems or create memorable spectacle.
- Cut anything that is only a recipe icon with no effect on the main loop.

Exit condition: a fresh survival playthrough has an early goal, a midgame automation payoff, and an endgame aspiration.

### Day 11 — Multiplayer, performance, and polish

- Run two-player ownership, menu, deploy, recall, routing, and chunk-unload tests.
- Profile eight working dinosaurs plus hostile mobs; fix scheduler and pathfinding hot spots.
- Audit tooltips, empty states, error messages, sounds, particles, contrast, and GUI scale.
- Fix all crashes, duplication paths, stuck tasks, and save corruption risks before visual extras.

Exit condition: a 60-minute survival test completes without a crash, duplicate, deadlock, or unexplained idle worker.

### Day 12 — Submission day

- Freeze features and build the release candidate from a clean checkout.
- Test the exact JAR in a normal launcher profile, not only the dev client.
- Check license, dependency declaration, source visibility, credits, and CurseForge metadata.
- Capture gallery images manually in-game; do not use AI-generated gallery or avatar art.
- Upload early enough for moderation, complete the official form, and keep proof of submission.

Exit condition: the project is approved/live and the official entry form is submitted before the deadline.

## Parallel modeler lane

The modeler should work in this order:

1. One medium quadruped test rig to validate scale, bone names, hitbox, animation keys, and export settings.
2. One egg and one machine to validate texture density and block presentation.
3. Shared locomotion sets for related body plans.
4. All twelve base models and idle/walk/run/sleep animations.
5. Work, combat, and personality animations in species-priority order.
6. Final texture variants, tint masks, icons, and presentation renders.

Send the modeler `ART_ASSET_CONTRACT.md` before any export. If an asset cannot meet that contract, change the contract together before code begins depending on the asset.

## Cut order if a gate slips

Cut in this order while preserving the core loop:

1. Extra endgame weapons and decorative material compression tiers.
2. Laser Observer and reinforced piston variants.
3. Ancient Spell Stone and mount equipment.
4. Advanced route conditions beyond filters, priorities, and minimum stock.
5. Secondary special animations that do not communicate work or danger.

Never cut persistence safety, basic multiplayer validation, readable worker status, the eight-active roster, hunger feeding, or the main automation chain.

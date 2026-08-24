# Primeval Works detail bible

Primeval Works should feel like a living base, not a set of machines wearing dinosaur skins. Polish is part of the system design. We are aiming for hundreds of small, coherent responses that make the dinosaurs readable, physical, and worth caring about.

This does not mean adding random particles everywhere. A detail earns its place when it tells the player what a dinosaur intends, what it is touching, how hard it is working, what changed, or how to fix a problem.

## The six-read rule

Every substantial action should answer six questions without requiring a menu:

1. **Intent:** What is the dinosaur trying to do?
2. **Approach:** Where is it going, and why did it choose that route?
3. **Contact:** What exact block, item, target, or creature is it interacting with?
4. **Effort:** What makes this species and individual feel different while doing it?
5. **Result:** What was produced, moved, damaged, powered, or changed?
6. **Recovery:** If it failed, what will it try next and how can the player help?

Gameplay state is server-owned. Rendered props, particles, sound, camera motion, and UI easing are client presentation driven from a small synchronized state. A particle or animation marker must never be the authority that creates an item or deals damage.

## Work should be visible

Every work order has a readable sequence:

```text
notice offer -> reserve -> approach -> face target -> contact -> show progress -> produce or collect -> deliver -> react
```

A transport dinosaur carries a logical manifest on the server. While the manifest is non-empty, the renderer attaches the real item model to `carry_socket`. The visible stack may bob, tilt, or sway with movement, and the count can appear briefly when it changes. It disappears only when the transfer commits, returns to its source, or moves into a validated overflow destination.

Transport details include:

- look at the pickup before collecting it;
- a short pickup motion and item sound at contact;
- the carried item visible at the mouth, harness, or above the head as appropriate for that rig;
- movement adjusted slightly for a large or awkward load;
- a delivery motion aimed at the destination inventory;
- a pleased response after a clean route;
- a puzzled pause, head turn, and plain blocked reason when the destination is full;
- no item popping out of existence during rerouting, chunk unload, or cancellation.

Other jobs follow the same physical logic. A miner strikes at `work_contact`, a cook faces the fire and reacts to heat, a power worker visibly connects to the generator, and a crafter alternates between the correct station contact points. Progress effects happen at contact, not in empty air.

## Dinosaurs should look intelligent

Idle is a behavior, not the absence of behavior. A free dinosaur chooses bounded points of interest inside its base, while respecting beds, work lanes, station approaches, hazards, doors, and other large bodies.

Good roaming behavior:

- pauses before changing direction instead of snapping into a new path;
- looks at nearby players, working companions, dropped food, and unfamiliar sounds;
- uses species-flavored idle interests such as shade, water, height, warm machinery, or berry patches;
- keeps social spacing and avoids bunching in doorways;
- occasionally joins a compatible companion without forming a permanent crowd;
- remembers recent failed destinations for a short cooldown;
- varies idle duration and gaze direction from a stable per-dinosaur seed;
- yields a narrow route to a working or transporting dinosaur when practical;
- returns to a safe home region instead of wandering against the base boundary forever.

Navigation has retry budgets and an unstuck path. Repeated failure should move through: recompute approach, try another valid approach, release the reservation, choose a useful fallback, then show an actionable blocked reason. Teleporting is a last recovery mechanism and must never be the ordinary visual solution.

## Weight, feet, and scale

Animation timeline markers from `ART_ASSET_CONTRACT.md` drive contact presentation. `footstep_left`, `footstep_right`, `land`, `work_contact`, and attack markers must sit on the exact visual frame.

Each footfall can select:

- a species- and mass-appropriate sound;
- a surface-aware dust, splash, leaf, snow, or debris effect;
- a small body response in nearby light props;
- optional local camera impulse based on mass, gait, distance, and occlusion.

Large dinosaurs such as Brachiosaurus and T. rex should have a restrained step impulse. Walking near one should feel heavy; standing thirty blocks away should not shake the screen. Camera motion is cosmetic, distance-capped, rate-limited, disabled in menus, and controlled by an accessibility slider with an off setting. Run, land, roar, and deliberate stomp use separate curves. A combat stomp is still a server-owned gameplay event.

Footstep presentation must not be emitted every entity tick. It comes from the animation contact marker, is culled by distance, and has a per-entity safety cooldown. The stable server hitbox does not grow and shrink with visual breathing or gait motion.

## Needs, mood, and relationships

Hunger and mood need world-readable behavior before they become warnings:

- hungry dinosaurs notice food, sniff, slow down near the pantry, and use a distinct request sound;
- well-fed dinosaurs settle faster and may perform a content idle;
- low mood changes posture, gaze, work-start hesitation, and social interest without making control feel broken;
- sleep has a wind-down, chosen sleeping spot, settle motion, loop, wake reaction, and interruption response;
- Parasaurolophus mood support is visible through a call and nearby companion response, not an invisible percentage aura;
- injury has guarded movement and a clear retreat rather than a normal death animation followed by disappearance.

Reactions are rate-limited and context-sensitive. Eight dinosaurs must not all chirp on the same tick. Stable per-dinosaur offsets keep the base lively without turning it into noise.

## Threats and guardians

Workers notice a threat before fleeing. They turn toward it, call or flinch, choose cover or a guardian, and avoid running through the attacker. Guardians claim targets through the base threat controller so every combat dinosaur does not dogpile the same weak mob unless needed.

Combat details include readable anticipation, contact, recovery, missed-attack recovery, surface impacts, friendly-fire exclusion, a return-to-post moment, and a short all-clear response. Vulnerable dinosaurs should resume interrupted work only after their reservation and route are revalidated.

## World and machine response

Machines communicate through motion, sound, light, and state:

- rotors accelerate and coast instead of jumping between still and full speed;
- gauges and runes reflect the real power allocation;
- a blocked output has a different idle from missing power;
- a crafting completion has a contact beat and output response;
- turrets track with capped speed and settle after losing a target;
- the incubator makes egg movement, warmth, and hatch progress readable from outside its menu;
- nearby dinosaurs orient toward notable machine events without abandoning higher-priority work.

Effects must match the block material and environment. Water-wheel audio changes with flow, furnaces brighten the immediate work area, and wind devices respond to actual validity checks. Visual state may interpolate, but it must converge on the authoritative machine state.

## UI and player control

Every control needs a useful icon, a short label, a hover explanation, a pressed response, and an authoritative success or failure message. Text must fit at supported GUI scales. Important state uses shape and icon as well as color.

Companion screens should show current intent, current target, needs, preferred work, and a plain blocked reason. They should not pretend a local button succeeded while waiting for the server. Pending actions get a short visual state, then resolve to confirmed or rejected.

Animation is restrained: scan lines, gauge easing, signal pulses, selection movement, and entity previews support comprehension. Nothing important depends on noticing a decorative animation.

## Presentation state contract

The shared dinosaur entity will expose a compact synchronized presentation view, derived from authoritative behavior:

- high-level behavior state;
- current work type and station identity when relevant;
- carried display stack and logical count;
- look/contact target when safe to expose;
- sleep, injury, threat, and blocked-reason flags;
- current locomotion mode;
- short-lived presentation event sequence number.

The client maps that view to GeckoLib animation, attached item rendering, sounds, particles, indicators, and camera feedback. One-shot events use sequence numbers or timestamps so packet replay and chunk re-entry do not repeat old roars, impacts, or pickup effects.

## Detail register

Polish discoveries go into a lightweight register during playtesting. Each entry contains:

- feature and player situation;
- missing read or awkward moment;
- proposed visual, audio, motion, UI, or recovery response;
- whether it changes gameplay or presentation only;
- owner and priority;
- performance and accessibility notes;
- verification result.

This lets us accumulate a thousand thoughtful details without creating a thousand unrelated hacks. Reusable systems such as contact markers, carried-stack rendering, surface footsteps, gaze targets, notification reasons, and camera impulses should supply details across all twelve species.

## Feature definition of done

A creature, job, machine, or interaction is not finished until the relevant answers are yes:

- Can the player understand intent and target without opening a debug screen?
- Does animation align with physical contact?
- Do sound and particles match material, mass, and distance?
- Does success visibly change the world or inventory?
- Does failure preserve resources and explain the next step?
- Does the behavior recover from a blocked path, removed block, full inventory, unload, and cancellation?
- Does it remain readable with eight active dinosaurs?
- Are effects bounded by distance, rate, and accessibility settings?
- Is multiplayer state authoritative and reconnect-safe?
- Has it been watched in-game at least once from start to finish?


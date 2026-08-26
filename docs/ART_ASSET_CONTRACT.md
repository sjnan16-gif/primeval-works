# Primeval Works — Blockbench and art asset contract

Give this entire document to the modeler before more assets are finalized. Consistent naming, orientation, pivots, and animation IDs let one shared renderer and AI system drive all twelve species.

## Required tools and project types

Install Blockbench and then install the official **GeckoLib Models & Animations** plugin from `File → Plugins`.

- Dinosaurs, animated eggs, and animated machines: **GeckoLib Animated Model**.
- Ordinary non-animated blocks: **Java Block/Item**.
- Flat inventory icons: normal transparent PNG pixel art.
- Keep every editable `.bbmodel`; exported JSON alone is not a source file.

Do not convert MCreator entity models or paste Java model code into the new project. Export GeckoLib `.geo.json` and `.animation.json` files.

## Naming

Everything uses lowercase `snake_case`. No spaces, uppercase letters, hyphens, version suffixes, or “final/final2”.

Species IDs are fixed:

```text
t_rex
triceratops
brachiosaurus
dilophosaurus
velociraptor
stegosaurus
parasaurolophus
ankylosaurus
pteranodon
dodo
spinosaurus
pachycephalosaurus
```

Editable source:

```text
art/entities/<species>/<species>.bbmodel
art/eggs/ancient_egg.bbmodel
art/blocks/<block_id>/<block_id>.bbmodel
```

Runtime exports:

```text
src/main/resources/assets/primevalworks/geckolib/models/entity/<species>.geo.json
src/main/resources/assets/primevalworks/geckolib/animations/entity/<species>.animation.json
src/main/resources/assets/primevalworks/textures/entity/<species>.png
src/main/resources/assets/primevalworks/textures/entity/<species>_tint.png
```

Animated egg exports:

```text
src/main/resources/assets/primevalworks/geckolib/models/block/ancient_egg.geo.json
src/main/resources/assets/primevalworks/geckolib/animations/block/ancient_egg.animation.json
src/main/resources/assets/primevalworks/textures/block/egg_<species>.png
src/main/resources/assets/primevalworks/textures/block/egg_<species>_cracks.png
```

Never put the mod ID inside an individual filename. The namespace already handles that.

## Coordinate and orientation contract

For entities:

- 16 Blockbench units = 1 Minecraft block.
- Positive Y is up.
- The model looks forward toward Blockbench North / negative Z.
- Center the body horizontally on X = 0.
- The lowest resting foot surface is Y = 0.
- Use a top-level `root` bone with pivot `[0, 24, 0]`.
- Do not rotate `root` to correct a backwards model. Fix the model orientation itself.
- The default pose must be a clean neutral pose with zero rotations on animated bones where possible.
- Entity movement happens in code. Walk/run animations must not translate the root forward through space.

For animated blocks:

- 16 units = one block.
- Center the model on X = 0, Z = 0 with the ground at Y = 0.
- Front faces North / negative Z.
- Use a top-level `root` at `[0, 0, 0]`.
- Decorative geometry may leave the block footprint only when the design sheet explicitly allows it.
- The Command Table is one block: its complete footprint must remain within X -8 to 8 and Z -8 to 8. It is not a two-block structure and must not create an extension block.

For ordinary Java block models:

- The ordinary block volume is X/Y/Z 0–16.
- Front faces North.
- Use axis-aligned cuboids for every part intended to have collision.
- Decorative angled parts are visual-only unless a separate simple collision box is specified.

## Entity scale targets

These are stylized gameplay sizes, not museum-scale recreations. The base must remain navigable with the active crew. Genetic quality scales the visual and physical body together from 0.90 to 1.10; Huge then multiplies that result by 1.18.

| Species | Approx. visual height | Approx. visual length/wingspan | Stable hitbox width × height |
|---|---:|---:|---:|
| T. rex | 3.25 blocks / 52 units | authored model length | 2.03 x 3.25 |
| Triceratops | 2.00 / 32 | 3.2 blocks | 1.45 × 1.80 |
| Brachiosaurus | 4.00 / 64 | 4.2 blocks | 1.50 × 3.20 |
| Dilophosaurus | 1.60 / 26 | 2.4 blocks | 0.80 × 1.40 |
| Velociraptor | 1.55 / 24.75 | 3.47 blocks | 0.77 × 1.55 |
| Stegosaurus | 2.88 / 46 | 5.94 blocks | 1.75 × 2.88 |
| Parasaurolophus | 3.31 / 53 | authored model length | 1.16 x 3.31 |
| Ankylosaurus | 1.45 / 23 | 2.7 blocks | 1.20 × 1.25 |
| Pteranodon | 1.25 / 20 body | 8.75-block wingspan | 1.35 x 1.25 body-only |
| Dodo | 1.56 / 25 | 0.9 block | 0.88 x 1.56 |
| Spinosaurus | 5.05 / 81 | 10.06 blocks | 2.03 x 5.05 |
| Pachycephalosaurus | 1.60 / 26 | 2.1 blocks | 0.80 × 1.40 |

Keep all limbs, tails, crests, wings, and work poses inside the exported visible bounds. Tails and wings do not expand collision; genetic scale applies to both the model and body hitbox so contact remains honest.

## Standard entity rig

Required exact bone names for every species:

```text
root
body
head
jaw
carry_socket
work_socket
nameplate_anchor
```

- `carry_socket` is an empty child bone placed where a carried item should render. For most dinosaurs it sits at the mouth; for transport specialists it may sit at a back harness position.
- `work_socket` is an empty bone at the primary contact point used for particles or held tools.
- `nameplate_anchor` is an empty bone above the highest ordinary point of the neutral model.
- `jaw` may contain no cubes for species without a visibly separate jaw, but the bone must exist so shared animation/render code has a safe target.

Recommended anatomy naming:

```text
neck_01, neck_02, neck_03
tail_01, tail_02, tail_03, tail_04
leg_front_left, leg_front_right
leg_back_left, leg_back_right
foot_front_left, foot_front_right
foot_back_left, foot_back_right
arm_left, arm_right
wing_left_01, wing_left_02
wing_right_01, wing_right_02
```

Always spell out `_left` and `_right`. Mirrored sides must still have independent bones and pivots. Do not leave names such as `bone`, `cube2`, `group`, or `rightlegfinal` in a delivered rig.

Bone rules:

- Pivot each joint where the real rotation should happen.
- Parent from root → body → limb segment; never parent an unrelated limb through another side.
- Do not use negative bone scale or animate a bone through zero scale.
- Avoid permanent non-uniform scale; size cubes correctly.
- Freeze the final rig/bone names before animating. Renaming after animation risks broken channels and code attachments.
- Prefer cubes and zero-thickness planes supported by GeckoLib; avoid unsupported mesh-only features.

## Complexity budgets

Eight dinosaurs may be visible at once, plus wild creatures and animated machines.

| Size class | Maximum cubes/planes | Maximum bones | Suggested texture |
|---|---:|---:|---:|
| Small — Dodo, Raptor, Dilo, Pachy | 90 | 45 | 64×64 or 128×128 |
| Medium — Trike, Stego, Parasaur, Anky, Pteranodon | 130 | 55 | 128×128 |
| Large — T. rex, Spino, Brachiosaurus | 170 | 65 | 128×128 or 256×256 |

These are ceilings, not targets. Spend geometry on silhouette, face, work contact points, and readable motion. Tiny teeth/spikes can use texture planes when individual cubes do not improve the Minecraft-distance silhouette.

## Texture and hue contract

Every species has two textures with identical dimensions and UV layout:

1. `<species>.png` — full authored base texture including all non-variable details.
2. `<species>_tint.png` — transparent everywhere except scales/skin that receive the genetic hue overlay.

Tint-mask pixels should be white or neutral grayscale with authored shading and alpha. The code multiplies them by a subtle genome color and renders them as a layer above the base. Keep these areas transparent in the mask:

- eyes and pupils;
- teeth, beak, tongue, mouth interior;
- claws, horns, plates where their color must stay fixed;
- scars/markings that define the species;
- carried items, harnesses, or equipment.

Texture rules:

- Pixel-art edges only; no antialiased transparent fringe.
- Use nearest-neighbor preview.
- Use one coherent texel density per size class.
- Transparent unused UV space.
- No baked lighting or directional shadows.
- Avoid pure black over large areas; preserve readable shade steps in Minecraft lighting.
- Test the tint mask at several colors before delivery.

Optional emissive textures use `<species>_glow.png` and must be approved per species. Do not make every creature glow.

## Animation naming

Animation identifiers are exact and live inside the species animation file.

Mandatory P0 animations for every dinosaur:

```text
animation.<species>.idle
animation.<species>.walk
animation.<species>.run
animation.<species>.sleep
animation.<species>.eat
animation.<species>.work_generic
animation.<species>.hurt
animation.<species>.incapacitate
animation.<species>.attack
animation.<species>.special
```

Examples:

```text
animation.t_rex.idle
animation.t_rex.run
animation.parasaurolophus.special
```

Movement selection is automatic in code:

- speed near zero → idle;
- ordinary navigation speed → walk;
- sprint/combat/urgent navigation → run;
- Pteranodon airborne state → fly/glide overrides ground movement;
- Spinosaurus swimming or breach state → swim overrides ground movement until water re-entry or landing.
- Rideable models use the locator bone `whereplayersits`. Spinosaurus carries that locator through its swim clip so the rider stays supported in both land and aquatic poses. Mount code applies the current three-model-pixel downward support correction after the animated locator and scales the complete attachment exactly once for genetics and Huge.

Additional required locomotion:

```text
animation.pteranodon.fly
animation.pteranodon.glide
animation.pteranodon.takeoff
animation.pteranodon.land
animation.pteranodon.perch
animation.spinosaurus.swim
```

Species-special intent:

| Species | `special` animation concept |
|---|---|
| T. rex | roar with readable chest/head anticipation |
| Triceratops | charge wind-up and hoof scrape |
| Brachiosaurus | tall reach/neck sweep harvest |
| Dilophosaurus | frill flare and venom spit |
| Velociraptor | pounce or precise claw-work flourish |
| Stegosaurus | heavy tail hammer/sifting motion |
| Parasaurolophus | crest song with breathing resonance |
| Ankylosaurus | tail-club slam |
| Pteranodon | takeoff flourish or air-delivery drop |
| Dodo | excited wing flap and seed scatter |
| Spinosaurus | sail display and water splash |
| Pachycephalosaurus | headbutt wind-up and impact recoil |

We may later add `work_primary` or station-specific animations. `work_generic` must already look acceptable at any station so missing optional animations never block content.

## Animation quality rules

- Idle, walk, run, sleep, eat, and work are loops with seamless first/last poses.
- Hurt, incapacitate, attack, takeoff, land, and special are one-shots unless explicitly agreed.
- Idle target length: 2–5 seconds with subtle breathing/weight shift.
- Walk target cycle: 0.8–1.2 seconds.
- Run target cycle: 0.45–0.8 seconds.
- Work target loop: 1.0–2.0 seconds with a clear contact frame.
- Attack target: readable anticipation → contact → recovery; do not make damage occur on frame zero.
- Keep the root spatially fixed. Fake weight through body/limb motion rather than moving the entire model forward.
- Feet should not visibly skate at the intended movement rate. Provide the walk/run cycle lengths so code speed can be matched.
- Sleeping poses must fit the stable hitbox and avoid sinking far under Y = 0.
- End one-shot animations in a pose that can blend back to idle or the next state.
- Use easing intentionally. Excessive Catmull-Rom overshoot makes tails and jaws clip.

Timeline marker names for code-driven sound/particles:

```text
footstep_left
footstep_right
work_contact
attack_contact
bite
roar
takeoff
land
```

Place markers at the exact visual contact frame. Do not hardcode final sound file paths in the model project; code maps the generic marker to species sounds.

## Egg model contract

Use one shared animated egg geometry for all species. Species identity comes from texture/pattern, which keeps code, collision, and hatching animation consistent.

Geometry:

- roughly 10 units wide and 14 units tall;
- centered on X/Z zero and grounded at Y zero;
- fits inside one block with a simple half-block collision;
- root bone `root`;
- required bones `egg`, `shell_top`, `shell_left`, `shell_right`, and empty `hatch_origin`;
- no visible embryo; silhouettes must remain tasteful and Minecraft-like.

Animations:

```text
animation.ancient_egg.idle
animation.ancient_egg.wiggle
animation.ancient_egg.hatch
```

- Idle is nearly still, perhaps a tiny intermittent pulse.
- Wiggle is a short one-shot used during progress milestones.
- Hatch separates shell pieces without translating the block root.
- Put a `hatch` marker at the moment the creature should materialize.

Create twelve base textures and matching crack overlays. Egg patterns should hint at species—plates, spots, stripes, crest shapes, feather pattern—but must not encode individual genes or mutations, which roll at hatch.

## Static block models

Use Java Block/Item format for non-animated blocks.

Deliver:

```text
art/blocks/<id>/<id>.bbmodel
src/main/resources/assets/primevalworks/models/block/<id>.json
src/main/resources/assets/primevalworks/textures/block/<id>.png
```

The code/data generator will create final blockstates and item model-definition wrappers.

Rules:

- One clearly identified North-facing front.
- Stay inside 0–16 unless the block is explicitly approved as oversized.
- Full opaque faces should use correct cull faces.
- Provide a particle texture.
- Avoid dozens of tiny decorative cuboids on frequently repeated blocks.
- If the collision is not a full cube, include a screenshot showing the intended collision boxes or name collision groups `collision_01`, `collision_02`, etc. Those groups are reference-only and will not render.
- Check inventory/ground/hand display transforms in Blockbench for blocks with a special item silhouette.

## Animated machine models

Use GeckoLib only when motion materially improves the machine: wheel rotation, turbine rotor, hatch chamber, kinetic platform, turret, or Runic Anvil sequence.

Required bones:

```text
root
static
moving
interaction_anchor
```

Optional machine-specific bones use explicit names such as `rotor`, `wheel`, `lid`, `needle`, `turret_yaw`, and `turret_pitch`.

Required animation IDs:

```text
animation.<block_id>.idle
animation.<block_id>.working
```

Additional one-shots such as `open`, `close`, `complete`, `fire`, or `hatch` are approved per block. Animated machines still need a simple static item presentation; do not rely on a world block-entity renderer in inventory UI.

## Item and GUI art

- Ordinary material icons: 16×16 unless detail truly requires 32×32.
- Relic weapons: 32×32 source art, still readable at Minecraft inventory size.
- No antialiased borders or soft drop shadows.
- Keep silhouettes unique before adding color.
- The Command Table UI will be code-rendered from small reusable nine-slice/line assets, not a single giant painted background.
- Modeler provides one authored 48×48 or 64×64 portrait crop per species after entity textures are final. These can be actual renders from the authored model or hand-drawn pixel portraits.
- CurseForge avatar and gallery images must not be AI-generated. Use modeler-authored logo art and real in-game screenshots.

## Delivery checklist per dinosaur

Do not call an asset complete until all boxes pass:

- [ ] Correct species filename and runtime paths
- [ ] Editable `.bbmodel` included
- [ ] Forward is North / negative Z
- [ ] Feet touch Y = 0 and model is centered
- [ ] Required bones exist with exact spelling
- [ ] No placeholder bone names
- [ ] Visible bounds contain every animation
- [ ] Cube/bone budget respected
- [ ] Base and tint-mask textures share dimensions/UVs
- [ ] Tint mask excludes eyes, teeth, claws, and fixed markings
- [ ] All ten mandatory animation IDs exist
- [ ] Loops are seamless
- [ ] Root does not travel during locomotion
- [ ] Contact markers align to visible impacts
- [ ] Model previewed with genetic scale 0.90 and 1.10
- [ ] Screenshot/GIF supplied for neutral, walk, run, sleep, work, attack, and special
- [ ] Exported JSON contains no external texture path or personal filesystem path

## Handoff procedure

1. Modeler places `.bbmodel` and PNG sources under `art/`.
2. Modeler exports runtime files into the exact `src/main/resources/assets/primevalworks/` paths.
3. Run a Gradle build to catch malformed resources.
4. Developer registers the species asset key and tests render orientation, bounds, tint, and every animation in a dedicated animation test screen/world.
5. Modeler fixes the source file, then re-exports. Never patch generated `.geo.json` by hand as the permanent fix.
6. Once code depends on a bone or animation name, changes require an explicit coordinated migration.

This contract lets art and code progress in parallel without making the modeler learn Java or forcing the developer to repair every export manually.

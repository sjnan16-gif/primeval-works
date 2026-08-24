# Quick modeler brief

We're making the dinosaurs in Blockbench as GeckoLib models. Use 16 units as one Minecraft block, face the model toward North/negative Z, keep it centered, and put the feet on Y = 0. Please keep the editable `.bbmodel`; don't only send exported JSON.

Every dinosaur needs a clean `root`, `body`, `head`, `jaw`, `carry_socket`, `work_socket`, and `nameplate_anchor`. Name the other bones clearly too—stuff like `tail_01` or `leg_front_left`, not `bone4` or `group2`. The empty sockets are where code will attach carried items, work particles, and the nameplate.

Each one needs idle, walk, run, sleep, eat, generic work, hurt, incapacitated, attack, and special animations. The model stays in place during walk and run; code handles actual movement. Make the loops seamless, keep the feet from sliding, and give attacks a clear wind-up, hit, and recovery. Pteranodon also needs takeoff, flight, glide, landing, and perching. Spinosaurus needs swimming.

Rideable models should include a locator bone named `whereplayersits`. Put its pivot exactly where the player's feet belong and keep any visible marker cube in the `.bbmodel` authoring file only. Animate the locator only when the seat must move relative to its parent, as Spinosaurus does in its swim clip; never rename it after export. Spinosaurus currently has a three-model-pixel runtime support correction, so do not move the authored pivot to compensate unless code and the asset contract are updated together.

For eggs, we're using one shared animated shape with different species textures. Make it about 10 units wide and 14 tall with `root`, `egg`, `shell_top`, `shell_left`, `shell_right`, and `hatch_origin` bones. It needs subtle idle, wiggle, and hatch animations. Patterns should suggest the species, but genetics and mutations only appear after hatching.

Normal blocks should use regular Minecraft block models. Keep them inside 0–16 unless we've agreed that they're oversized, give them a clear North-facing front, and show us the intended collision if they aren't full cubes.

Animated machines use GeckoLib too. They need `root`, `static`, `moving`, and `interaction_anchor` bones, plus clear names like `wheel`, `rotor`, `lid`, `turret_yaw`, or `turret_pitch`. Every animated block needs `idle` and `working`; special one-shots can be `open`, `close`, `complete`, `fire`, or `hatch`. Don't move the whole root during an animation, and also make a simple static item version because the animated world renderer won't display properly inside inventories.

Put editable files under `art/` and runtime exports under `src/main/resources/assets/primevalworks/`. The full sizes, paths, texture masks, animation IDs, and delivery checklist are in `ART_ASSET_CONTRACT.md`; this message is just the fast version.

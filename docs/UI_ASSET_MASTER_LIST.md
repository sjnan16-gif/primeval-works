# Primeval Works UI asset checklist

This is the current jam-build handoff. It replaces the old 100-screen concept list.
The mod uses a few dynamic screens, not a separate PNG for every state. Code owns all
text, numbers, item icons, dinosaur models, progress fills, hover glows, scrolling,
tooltips, dimming, and animation.

## Export rules

- Draw at native pixel size with nearest-neighbour pixels. Do not upscale before export.
- Export RGBA PNG with no automatic trimming.
- Do not bake names, numbers, item sprites, button words, timers, or status text into art.
- Keep every interactive box aligned to whole pixels.
- A Minecraft item needs a clear 16 x 16 area. A standard framed slot is 18 x 18 or 20 x 20.
- Keep editable sources in `art/ui/`. Runtime PNGs go in
  `src/main/resources/assets/primevalworks/textures/gui/`.
- Keep the existing tan/brown outline palette so `space.png`, `hotbar.png`, and the authored
  full screens can be reused without visible seams.

## Already made — do not redraw

| Asset | Size | What it controls |
|---|---:|---|
| `dino_menu.png` | 427 x 240 | Right-click dinosaur record: name/species, level, two mutations, model, hunger/mood/health, current job and passive |
| `control_table.png` | 427 x 240 | Seven active slots per page, depot, deceased/recovery row, energy meter, base actions |
| `levels_back.png` | 300 x 300 | Draggable and zoomable base-upgrade tree |
| `worksite_planner.png` | 427 x 240 | World-camera work assignment for Transport, Fire, Energy, Crafting and Expedition |
| `energy_top.png` | 427 x 240 | Energy network camera and consumer selection |
| `processor_ui.png` | 427 x 240 | Processor recipe/input/fuel/catalyst/output screen |
| `food_box.png` | 427 x 240 | Source reference; the current compact Food Box is assembled from slots and `space.png` |
| `space.png` | 86 x 14 | Reusable 9-slice text bubble |
| `hotbar.png` | 18 x 18 | Reusable item/action slot |
| hunger/mood/health bars and icons | existing sizes | All condition bars |
| work, energy, mood and sleep indicators | existing sizes | World-space indicators above jobs, machines and dinosaurs |

The existing screens already supply popup, slide, parallax, hover, tooltip and dimming
behaviour. New art only supplies the surfaces below.

## Still required — seven assets

### 1. Machine routing overlay — `machine_routing_overlay.png`

Canvas: **427 x 240**, transparent outside the panel.

This appears inside the Work Planner when the player right-clicks a selected machine.
It never teleports the player or opens the normal machine inventory. It explains and
configures exactly which slots transport dinosaurs may deliver to or collect from.

| Element | X | Y | W | H |
|---|---:|---:|---:|---:|
| Outer panel | 35 | 28 | 357 | 184 |
| Title bubble | 41 | 34 | 329 | 16 |
| Close button | 372 | 34 | 15 | 16 |
| Instruction strip | 42 | 53 | 343 | 13 |
| Card row 1 | 43 | 68 | 341 | 62 |
| Card row 2 | 43 | 134 | 341 | 62 |
| Optional overflow note | 43 | 199 | 341 | 10 |

Each row contains four equal cards: **83 x 62**, separated by 4 pixels. Inside every card
leave an **18 x 18** item slot at top centre, a 10-pixel role-name line below it, and two
small buttons at the bottom for `IN` and `OUT`. Code replaces empty items with a plus icon,
shows `AUTO` for fixed machine roles, dims impossible directions, and adds the live tooltip.

One image handles Processor, furnaces, Food Box, Ancient Barrel, vanilla chests and future
container machines. Do not make one routing PNG per block.

### 2. Powered-machine status — `powered_machine_status.png`

Canvas: **427 x 240**, transparent outside the panel.

This is one reusable status screen for Dart Turret and Ancient Spell Stone. The Magic Turret
intentionally has no right-click screen; its target, powered state, beam and shot feedback are visible in-world.
The block name, energy state, target/range, ammunition and blocked reason are dynamic.

| Element | X | Y | W | H |
|---|---:|---:|---:|---:|
| Outer panel | 85 | 56 | 257 | 128 |
| Title bubble | 97 | 67 | 233 | 16 |
| Block preview | 98 | 89 | 54 | 54 |
| Main status bubble | 158 | 89 | 172 | 18 |
| Detail bubble | 158 | 111 | 172 | 34 |
| Footer/help bubble | 98 | 150 | 232 | 22 |

Leave a small **10 x 10** energy-icon space at the left of the main status bubble. Code
shows no power / powered, nearest target, `18 block` Dart range, base dart stock,
or the Spell Stone's `48 block` ward.

### 3. Ancient Furnace inventory — `ancinet_furnace.png`

Canvas: **427 x 240**. This authored screen is already implemented. It has one input, one
output, a non-interactive energy socket, and an **85 x 7** throttle at **171, 87**. The
separate **9 x 9** handle moves along that bar; the energy readout is dynamic and the player
inventory remains at its normal machine-screen position.

### 4. Ancient Barrel inventory — `ancient_barrel_ui.png`

Canvas: **176 x 222**.

| Element | X | Y | W | H |
|---|---:|---:|---:|---:|
| Barrel title line | 8 | 6 | 160 | 10 |
| 9 x 6 barrel slots | 7 | 17 | 162 | 108 |
| Player-inventory title | 8 | 128 | 160 | 10 |
| Player inventory | 7 | 139 | 162 | 76 |

All slots use a 20-pixel pitch with a 16 x 16 item opening. Counts and tooltips come from
Minecraft. The barrel already works with the vanilla 54-slot screen; this asset is visual
polish, but it is part of the final themed set.

### 5. Hatch reveal card — `hatch_reveal.png`

Canvas: **184 x 66**. No transparent margin.

| Element | X | Y | W | H |
|---|---:|---:|---:|---:|
| Dinosaur viewport | 7 | 7 | 47 | 52 |
| Vertical divider | 59 | 7 | 2 | 52 |
| Header line | 67 | 8 | 109 | 11 |
| Dinosaur name | 67 | 22 | 109 | 12 |
| Mutation line | 67 | 37 | 109 | 11 |
| Quality line | 67 | 50 | 109 | 10 |

The card slides from the top-right, holds, then eases out. Code renders the live newborn
model and supports `No mutation`, `Huge`, `Albino`, or both.

### 6. Pteranodon flight HUD — `pteranodon_flight_hud.png`

Canvas: **96 x 24**, transparent outside the surface.

| Element | X | Y | W | H |
|---|---:|---:|---:|---:|
| Stamina label bubble | 0 | 0 | 96 | 9 |
| Stamina frame | 4 | 10 | 88 | 7 |

Leave the stamina interior transparent. Code draws the green/amber/red fill, exhaustion
state and percentage. It occupies the old controls position at the bottom-right and fades
when dismounted. There is no permanent controls bubble competing with the vanilla HUD.

## No full screen should be made for these

- Wild egg blocks: right-click hatches; Silk Touch picks up.
- Premium Egg Incubator: right-click with an egg inserts it; shift-right-click removes it;
  the egg and timer render in the world.
- Wind and Water Turbines: assigned and inspected in the Energy Map.
- Reinforced and Sticky Reinforced Pistons: redstone blocks controlled by the Energy Map.
- Powered Observer: world/redstone block controlled by the Energy Map.
- Berry Bush: behaves like a crop.
- Swords: item models/tooltips, not screens.
- Breeding: use a Nesting Treat on two identical species; no menu.
- Expeditions: already live inside the Work Planner.
- Deceased/recovery dinosaurs: already live inside the Command Table depot.

## Delivery order

1. `machine_routing_overlay.png`
2. `powered_machine_status.png`
3. `hatch_reveal.png`
4. `pteranodon_flight_hud.png`
5. `ancient_barrel_ui.png`

Send the remaining PNGs plus editable sources in one folder. Do not rename the authored files
that already exist. Once imported, every remaining visual state can be generated by code
without another layout PNG.

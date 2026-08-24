# Primeval Works — UI direction: Stratigraphic Lens

The UI should feel like an ancient machine that learned how to examine living fossils: part excavation table, part brass microscope, part impossible genome reader. It is compact and precise, not a parchment menu and not a reskin of the previous JJK archive.

## Visual promise

When the Command Table opens, the player should immediately see:

1. their active roster;
2. a large, rotatable preview of the selected dinosaur;
3. what it is doing and how it feels;
4. its genome, rare traits, and work strengths;
5. one obvious next action.

The selected creature is the hero. Decoration frames information and never competes with it.

## Material language

- **Basalt glass** — near-black blue stone panels with faint horizontal strata.
- **Aged bronze** — thin mechanical borders, corner joints, needles, and active brackets.
- **Bone ceramic** — primary text and inactive carved glyphs.
- **Amber resin** — selection, genome nodes, mood, and living/organic information.
- **Oxidized teal** — power, routes, scanning lines, and active machine state.
- **Iron-red pigment** — injury, attack, invalid actions, and urgent alerts only.

Working palette for code and texture tests:

```text
basalt void       #080b0d
basalt panel      #101519
raised stone      #171e21
strata line       #273034
bone text         #ddd4bd
muted bone        #938d7d
aged bronze       #8f6a3b
bright bronze     #c49755
amber             #e5ad45
dark amber        #76501f
oxidized teal     #55a392
deep teal         #235d57
iron red          #c55a4b
```

The final colors must be checked against actual Minecraft brightness, GUI scaling, color-blind modes, and the modeler’s textures. Color is always paired with an icon, label, or fill pattern.

## Shape language

- Square/stepped corners, one-pixel cuts, no soft modern rounded cards.
- Thin double rules evoke strata, measuring marks, and machine tracks.
- Active selection uses one bright left bracket plus a dim filled field—not a glowing outline around everything.
- Borders are normally one pixel at the chosen atlas resolution.
- Major divisions use 4–6 pixel mechanical joints instead of thick frames.
- Rune circles and genome diagrams are sparse instruments, not background wallpaper.

## Command Table layout

At ordinary desktop GUI scale, target a 420×240 logical-pixel screen with responsive reduction for smaller windows.

```text
┌ Primeval Works / Base name ─── status ───────── Close ┐
│ Overview  Roster  Work  Routes  Power                 │
├──────────┬──────────────────────────┬──────────────────┤
│ roster   │ specimen lens            │ field notes      │
│ 3/8      │ actual rotating entity   │ status/needs     │
│ cards    │ scan, scale, mutation    │ genome bands     │
│ alerts   │ restrained DNA overlay   │ rare traits      │
├──────────┴──────────────────────────┴──────────────────┤
│ current task / blocked reason             main action │
└───────────────────────────────────────────────────────┘
```

### Header

- Mod/base name at left.
- Compact `7 / 8 active`, `6 / 8 power`, and pantry warning at right; no dashboard-card row.
- Tab strip directly below the header.
- `Esc` closes; do not waste a large button on “Close.”

### Roster rail

- Cards show portrait/silhouette, name, species abbreviation, level, and one status icon.
- Selected card receives the amber bracket.
- Hunger/mood bars stay out of every small card unless warning thresholds are crossed.
- Active and reserve are separate compact filters inside Roster, not permanent screen-wide sections.
- Eight active entries fit without scrolling at the standard size; reserve can scroll.

### Specimen lens

- Render the actual selected model and genetic tint, not a prerendered generic PNG.
- Drag horizontally to rotate; mouse wheel adjusts zoom within safe limits.
- Idle auto-rotation begins only after a short period without input.
- A faint ground grid and scale ticks make size differences readable.
- One slow teal scan band passes over the model.
- Genome nodes orbit or drift at low opacity near the edge; they never cover the face.
- Mutation traits briefly resolve as amber symbols when selection changes.
- Sleep, injury, and work preview poses may be selected from a small state control later; the default remains idle.

### Field notes panel

Information order:

```text
Moss
Parasaurolophus · Level 4
“Moss hums when the workshop gets quiet.”

Taking berries to the pantry
Mood       84  Inspired
Hunger     67  Fed

Work aptitude   78
Vitality         61
Movement         73

Industrious
+12% work contribution
```

The quote is a short authored observation based on real state. It changes occasionally, not every frame.

Genome bands show both number and bar/marker. A subtle double-helix trace links the three bands, making the screen feel like an ancient microscope without pretending to be a modern gene sequencer.

### Footer

- Left: current task, route, or actionable blocked reason.
- Right: one main contextual action such as `Assign work`, `Let rest`, `Activate`, or `Recall`.
- Secondary actions appear as small labeled controls beside it only when relevant.

## Other tabs

### Overview

The specimen lens becomes a compact base relief/map. Show stations, dinosaurs, route lines, and alerts inside the Command Table radius. No fake minimap terrain is needed; use an abstract plan view with real relative positions.

### Work

- Left: work-type filters and worker list.
- Center: station order/priority lanes.
- Right: selected station, assigned dinosaur, progress, inputs, output destination, and blocked reason.
- Drag-and-drop is optional; every drag action needs an equivalent click assignment.

### Routes

Use a clean node-and-line plan view. Source, destination, pantry, overflow, and currently carried manifest are visible. Animate only active route flow, slowly. Never show dozens of decorative particles.

### Power

Use one readable supply rail from producers to prioritized consumers. Environmental validity (“Rotor blocked,” “Water too still”) sits beside the producer. Teal means supplied; dark dashed rail means waiting; iron red means invalid, not merely unpowered.

## Motion

The reference UI works because motion is subtle. Primeval Works uses:

- scan band: 3–4 second pass, low opacity;
- bronze needle settling when a value changes;
- genome nodes drifting by 1–2 pixels;
- selected bracket breathing by a very small brightness change;
- route line stepping only while an item is actually moving;
- machine rune rotating slowly only while the base is powered;
- alert pulse limited to two or three cycles, then static.

No constantly bouncing buttons, random sparkles, large looping glows, screen shake, or full-panel breathing. Add a reduced-motion client setting that freezes decorative loops while retaining meaningful state changes.

## Typography and spacing

- Use Minecraft’s font for final implementation so resource packs and language glyphs behave correctly.
- Pixel headings are short and one weight stronger through color/spacing, not giant size.
- Sentence case in language files; renderer may apply a restrained small-caps appearance to short labels.
- Four-pixel base spacing rhythm at logical resolution: 4, 8, 12, 16.
- Minimum clickable target should remain comfortable at GUI scale; visual glyphs can be smaller inside it.
- Long translated strings wrap or reflow. Nothing depends on exact English width.
- Numbers use stable alignment where values update.

## Implementation approach

- Build with `GuiGraphics` and reusable components, not one huge painted GUI background.
- Use a small pixel atlas for corners, brackets, glyphs, and nine-slice surfaces.
- Draw strata lines, measuring ticks, bars, routes, and genome strands in code.
- Clip every scroll/list/specimen region explicitly.
- Components consume immutable server view data; they do not reach into live block entities.
- Interaction sends validated intent and shows a pending state until the server acknowledges it.
- Entity preview uses a dedicated safe preview render path from the selected snapshot/species data, not a second live world entity with AI.
- Tooltips follow `WRITING_STYLE.md` and always explain blocked states.

## Quality rejects

Reject the UI if it becomes any of these:

- generic brown parchment with runes stamped everywhere;
- a modern sci-fi blue hologram unrelated to ancient materials;
- a direct recolor/layout copy of the JJK archive;
- one giant raster background that cannot resize or localize;
- twelve tiny stat cards competing with the selected dinosaur;
- flavor text that obscures current status;
- constant animation that makes the interface tiring;
- a fake DNA visual with no connection to actual genes;
- cramped text created by preserving decoration.

The screen is successful when a player wants to inspect every hatch because the creature looks alive, its differences are legible, and the interface feels like a treasured piece of recovered machinery.

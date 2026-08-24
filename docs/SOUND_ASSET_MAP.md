# Primeval Works sound asset map

The sound code is wired, but every custom event is deliberately silent until its final `.ogg` is supplied. This keeps temporary vanilla sounds out of the mod.

## Dinosaur folders

Create one folder per species under:

`src/main/resources/assets/primevalworks/sounds/entity/<species>/`

Species folder names:

- `tyrannosaurus`
- `triceratops`
- `brachiosaurus`
- `dilophosaurus`
- `velociraptor`
- `stegosaurus`
- `parasaurolophus`
- `ankylosaurus`
- `pteranodon`
- `field_dodo`
- `spinosaurus`
- `pachycephalosaurus`

Each species supports these cues:

- `ambient`: occasional neutral call
- `alert`: first noticing a hostile target
- `hurt`: taking damage
- `death`: dying
- `attack`: committing to an attack
- `eat`: consuming food
- `step`: ordinary walking contact
- `run_step`: running/heavy contact
- `sleep`: settling into sleep
- `wake`: leaving sleep
- `work`: starting a timed work action

Use mono, 48 kHz Vorbis `.ogg` files. Keep ordinary calls and steps short, remove silence at the start, and leave headroom so several dinosaurs do not clip when heard together. Multiple variants can use `_1`, `_2`, `_3`, and so on.

## Global event groups

The registry also reserves events for eggs, the incubator, Command Table feedback, Food Box, Ancient Furnace, both turbines, energy pulses, the Processor, both turrets, transport/crafting work actions, and UI open/close/hover/click/warning feedback.

When the audio files arrive, place them in `src/main/resources/assets/primevalworks/sounds/` and update `sounds.json` so each existing event lists its matching file variants. The Java hooks and event IDs do not need to change.

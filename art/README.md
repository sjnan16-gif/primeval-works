# Editable Art Sources

Keep editable Blockbench sources here. These files are not included in the released mod JAR.

Use this layout:

- `blockbench/entities/` — one `.bbmodel` per dinosaur
- `blockbench/eggs/` — reusable egg rigs and species variants
- `blockbench/blocks/` — animated or unusually shaped machines
- `textures/source/` — layered source art before PNG export
- `references/` — approved silhouettes, palettes, and scale sheets

Exported runtime files belong under `src/main/resources/assets/primevalworks/`, following `docs/ART_ASSET_CONTRACT.md`. Never treat an exported GeoJSON or PNG as the only editable master.

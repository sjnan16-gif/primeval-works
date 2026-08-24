# Primeval Works

Primeval Works is a Minecraft 26.1.2 NeoForge mod about excavating ancient eggs, hatching genetically unique prehistoric companions, and building a living automated base around their specialties.

The project is a Java Mods entry for CurseForge ModJam 2026: Echoes of the Past.

The source is public so ModJam judges can review it. Primeval Works' original code and assets remain All Rights Reserved; see `LICENSE` and `LICENSES/`.

## Toolchain

- Minecraft 26.1.2
- NeoForge 26.1.2.95
- Java 25
- GeckoLib 5.5.2
- Gradle via the checked-in wrapper
- Blockbench with the GeckoLib Models & Animations plugin

## Run the development client

Double-click `run-client.bat`, or run this from IntelliJ's terminal:

```powershell
.\gradlew.bat runClient --no-configuration-cache
```

In IntelliJ, use the shared **Primeval Works Client** Gradle configuration. It prepares NeoForge's generated launch arguments automatically; the raw generated **Client** application can lose those files after deleting `build/` until Gradle is synced again.

The first launch takes longer while Gradle prepares Minecraft. Later launches are much faster.

## Local build

PowerShell:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Microsoft\jdk-25.0.4.7-hotspot'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat build
```

The finished development JAR is written to `build/libs/`.

## Source layout

- `src/main/java/com/primevalworks/` — Java implementation
- `src/main/resources/assets/primevalworks/` — client assets
- `src/main/resources/data/primevalworks/` — recipes, loot, tags, world data
- `src/generated/resources/` — generated resources
- `art/` — editable Blockbench source files; excluded from the released JAR
- `docs/` — product, architecture, contest, and asset contracts

The Fossil Fragment visual is a deliberate vanilla-texture placeholder used to prove registration and asset loading. It must be replaced before release. The Command Table currently uses a one-block animated placeholder and keeps a one-block footprint until the modeler's final asset replaces it.

## Non-negotiable design constraints

- The server owns all gameplay state.
- A dinosaur's genome is rolled once and never reconstructed from visuals.
- Active and stored are mutually exclusive lifecycle states.
- No mandatory progression may depend on hatching a particular species.
- Work scheduling is base-level and reservation-driven, never a global per-tick scan.
- Every menu action is ownership-, distance-, and payload-validated on the server.

## Working documents

- [Game design and balance](docs/GAME_DESIGN.md)
- [12-day production plan](docs/PRODUCTION_PLAN.md)
- [Current play-test checklist](docs/TESTING.md)
- [Backend architecture](docs/BACKEND_ARCHITECTURE.md)
- [Modeler and animator handoff](docs/ART_ASSET_CONTRACT.md)
- [Short modeler message](docs/MODELER_BRIEF.md)
- [Command Table UI direction](docs/UI_STYLE_GUIDE.md)
- [427 x 240 UI asset master list](docs/UI_ASSET_MASTER_LIST.md)
- [Human writing guide](docs/WRITING_STYLE.md)
- [Living-world detail bible](docs/DETAIL_BIBLE.md)
- [ModJam rules and submission checklist](docs/MODJAM_2026_RULES.md)

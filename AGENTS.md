# Primeval Works repository instructions

These rules apply to every change in this repository.

## Required project memory

- Read `docs/PROJECT_MEMORY.md` completely before changing code, assets, balance, UI, models, or documentation.
- Treat it as the canonical record of decisions and known recurring regressions. If a request changes a decision, update that file in the same patch.
- Do not "fix" a deliberate authored coordinate, model scale, removed feature, or gameplay rule from memory. Verify it against the project memory and the current source asset first.
- When a bug repeats, add its cause, invariant, and regression test to the recurring-regressions section before closing the task.

## Fixed platform

- Target Minecraft 26.1.2 and NeoForge 26.1.2.
- Compile and run with Java 25.
- Use GeckoLib 5.5.x for animated entities and animated block entities.
- Keep `primevalworks` as the namespace and `com.primevalworks` as the Java root.
- Do not introduce Fabric, Architectury, MCreator output, or a second animation library.

## Architecture invariants

- Gameplay is server-authoritative. Client packets express intent only.
- Do not place gameplay behavior in `PrimevalWorks`; use owned subsystems.
- One shared dinosaur entity implementation serves all species.
- Species balance is data-defined; behavioral code is shared.
- Global state stores stable identifiers and indexes, not live entity references.
- Never allow the same dinosaur to exist both as a stored snapshot and an active entity.
- Inventory movement must simulate before commit and must be rollback-safe.
- Work orders require unique IDs, reservations, cancellation, and expiry.
- Avoid world-wide scans and per-tick inventory scans. Use dirty flags, indexes, and bounded base radii.
- All persistent records have an explicit schema version and validation.
- Unknown or invalid saved identifiers degrade safely; they must not crash a world load.

## Gameplay invariants

- Maximum off-specialty work efficiency is 45% of baseline.
- Every dinosaur can attempt all five work types; species change efficiency, not access.
- Mandatory progression cannot require a random species or mutation.
- Dinosaurs become injured and return to reserve instead of permanently dying.
- Random visual scale remains within the collision/pathfinding contract.
- Hunger and mood should create readable decisions, not constant babysitting.
- Every finished action must visibly communicate intent, contact, result, and failure recovery; follow `docs/DETAIL_BIBLE.md`.

## Verification

- Run `gradlew.bat build` for every code or resource change.
- Add pure unit tests for deterministic business rules.
- Add GameTests for world interactions, persistence, block breaking, and multiplayer ownership.
- Test both an integrated client and a dedicated server before a release.
- Do not claim a gameplay feature works until it has been exercised in-game.

## Code and writing quality

- Comments explain a non-obvious reason, invariant, workaround, or failure mode. Do not narrate obvious syntax or leave template/tutorial commentary in production code.
- Prefer names and small methods that make comments unnecessary.
- Public API documentation states a real contract; do not add empty “gets/sets/registers” prose.
- Error messages include what failed, why, and what the player or administrator can do next.
- In-game copy follows `docs/WRITING_STYLE.md`. Avoid robotic system prose, fake epic lore, filler adjectives, and repetitive generated-sounding sentence patterns.
- Prefer reusable presentation hooks for carried items, contact markers, footsteps, gaze, and camera impulses over species-specific one-off effects.

## Assets

- Follow `docs/ART_ASSET_CONTRACT.md` exactly.
- Never rename an exported bone or animation without updating the contract and code together.
- Keep `.bbmodel` sources; do not commit only generated exports.
- Do not use AI-generated CurseForge avatars or gallery images.

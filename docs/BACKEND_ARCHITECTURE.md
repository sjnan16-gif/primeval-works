# Primeval Works — backend architecture

This document defines the persistence, ownership, scheduling, networking, and performance contracts. “Perfect backend” means state cannot duplicate or vanish under ordinary play, UI cannot lie about server state, failure is diagnosable, and adding content does not require copying entire systems.

## Design principles

1. The logical server is authoritative for every gameplay decision.
2. Stable IDs cross save, packet, and subsystem boundaries; Java object references do not.
3. A dinosaur has one canonical lifecycle state and can never be both active and stored.
4. A station exposes bounded work offers; dinosaurs do not scan the world for arbitrary work.
5. Inventory, power, and work completion use validate/reserve/commit semantics.
6. Slow-changing systems are event- or dirty-driven, not recalculated every tick.
7. Content tuning is data-defined; invariants and state transitions remain code-defined.
8. Every persistent type has validation, a schema version, and an explicit recovery path.
9. UI sends intent and receives authoritative results.
10. A dedicated server is a first-class target, not an end-of-project compatibility pass.

## Proposed package ownership

```text
com.primevalworks
├── PrimevalWorks                 loader entry point only
├── registry                     NeoForge registrations
├── base
│   ├── BaseId / BaseRecord
│   ├── PrimevalWorldData        global SavedData and indexes
│   ├── CommandTableBlockEntity
│   └── access                   owner/team permissions
├── dino
│   ├── DinoEntity               one entity implementation
│   ├── DinoId / DinoSnapshot
│   ├── definition               synced data-driven species definitions
│   ├── genome                   immutable genes and mutation rules
│   ├── needs                    hunger, mood, sleep, injury
│   └── ai                       state machine and bounded goals
├── work
│   ├── WorkType
│   ├── WorkOffer / WorkOrder
│   ├── WorkReservation
│   ├── BaseWorkScheduler
│   └── executor                 movement and station interaction
├── logistics                    item routes, filters, transactions
├── power                        producer/consumer ledger and buffer
├── station                      station implementations
├── network
│   ├── payload                  immutable packet records
│   └── validation               common server-side guards
├── menu                         server menus and sync views
├── client                       screens, renderers, indicators
├── config                       bounded server/client settings
└── test                         shared test fixtures and GameTests
```

Only a small, documented surface should live under a future `api` package. Do not build a generic replacement for NeoForge blocks, block entities, menus, or resource handlers. The useful internal API is a dinosaur-workstation contract.

## Stable identity

Use strong wrapper records rather than passing naked UUIDs through business logic:

- `BaseId(UUID value)`
- `DinoId(UUID value)`
- `WorkOrderId(UUID value)` or a base-scoped monotonic long
- `RouteId(UUID value)`

The active entity UUID should equal its `DinoId`. When a stored snapshot is materialized, the entity is recreated with that same UUID. Before materialization, the server checks the global roster index and loaded-entity indexes for an existing instance.

Resource keys identify species, recipes, work types in persistent data, and packets. Never serialize registry ordinals or Java enum ordinals.

## Canonical persistent records

### `DinoGenome`

Immutable and rolled exactly once:

- work gene: unsigned 0–100
- combat/vitality gene: unsigned 0–100
- movement gene: unsigned 0–100
- visual-size jitter seed/value
- hue seed/value
- mutation bit set with maximum cardinality two

Derived attribute multipliers are recalculated from the genome and current balance definition. Do not serialize both the source genes and derived values.

### `DinoSnapshot`

The reserve representation contains:

- schema version
- `DinoId`
- species resource key
- owner UUID
- optional base ID
- player-visible name
- immutable genome
- level and XP
- hunger and mood in fixed-point integer units
- current/max-health relationship needed for recovery
- injury state and remaining recovery time
- learned/cosmetic state
- last safe position for diagnostics only

It does not contain a current Java goal, navigation path, block entity reference, or open menu reference.

### `BaseRecord`

Global server SavedData contains one record per base:

- schema version and `BaseId`
- owner UUID and access policy/team members
- Command Table `GlobalPos`
- active roster: `DinoId` set, maximum configured active count
- reserve roster: `DinoId → DinoSnapshot`
- active UUID index state, including recall-pending markers
- upgrade tier and radius
- stable route and station configuration identifiers
- dismantled/orphaned recovery state

The live Command Table block entity owns local inventory, current links, user-facing settings, and cached subsystem views. The global record protects identity and recovery when chunks are unloaded or the table is broken.

### Command Table item binding

Breaking a Command Table through an authorized action recalls loaded active dinosaurs, marks unloaded ones recall-pending, and drops a bound table/core item containing the `BaseId` as a data component. Replacing that item restores the same base rather than cloning it.

An ordinary crafted table without a `BaseId` creates a new base after server validation. A copied or cheated bound item cannot duplicate a base because the global record accepts only one live table position. Administrators receive a recovery command for orphaned bases.

## Dinosaur lifecycle state machine

Allowed states:

```text
UNBOUND_HATCHLING
    ├── bind with free slot ──> ACTIVE
    └── bind without slot ───> RESERVE

RESERVE ──activate──> MATERIALIZING ──success──> ACTIVE
   ▲                         └──failure────────> RESERVE
   │
ACTIVE ──withdraw──────────────> RESERVE
ACTIVE ──zero health───────────> INJURED_RESERVE
INJURED_RESERVE ──recovered───> RESERVE

Any owned state ──intentional release confirmation──> RELEASED
```

Every transition occurs on the server thread and is implemented as one owned operation with preconditions, state change, and compensating rollback.

### Materialization rules

1. Confirm the dinosaur exists in this base's reserve map.
2. Confirm active capacity, ownership, dimension, safe spawn area, and absence of an active entity with the same UUID.
3. Mark `MATERIALIZING` and persist dirty state.
4. Construct and add the entity with the stable UUID.
5. Remove the stored snapshot only after entity insertion succeeds.
6. On any failure, restore `RESERVE` and retain the snapshot.

### Withdrawal rules

1. Confirm the entity UUID is in the active roster and belongs to the requesting base.
2. Cancel its work order and release every reservation.
3. Create and validate a complete snapshot in memory.
4. Insert the snapshot into reserve and remove the active roster entry in the same server-thread operation.
5. Discard the entity only after the snapshot exists.

Chunk unload is not withdrawal. Vanilla remains responsible for persisting an active entity. The Command Table must never assume an unloaded entity is missing and spawn a replacement. Missing-active recovery requires the base chunks to be fully loaded, a grace period, and an explicit validated recovery path.

## Data-driven species

Register a synced data-pack registry for `DinoSpeciesDefinition` using a Codec/MapCodec. A definition includes:

- display/asset key
- base attributes and dimensions
- diet
- primary and optional secondary work type
- explicit efficiency per work type, validated so non-specialties are 0.45
- guardian profile and attack style
- hunger coefficients
- movement modes
- passive ability identifier and parameters
- sound set
- spawn/nest weights and biome tags

The fixed five `WorkType` values and lifecycle behavior remain code-defined. A malformed definition reports the exact resource and field and fails data reload safely rather than corrupting loaded dinosaurs.

Client assets derive from the species key:

```text
geckolib/models/entity/<species>.geo.json
geckolib/animations/entity/<species>.animation.json
textures/entity/<species>.png
textures/entity/<species>_tint.png
```

Unknown species in an old save create a visible inert “Missing Species” recovery entity/card that can be withdrawn or repaired after the data pack is restored. They must not crash world load.

## Needs simulation

Hunger and mood use fixed-point integers, for example 0–10,000, while UI shows 0–100%. Fixed-point values avoid floating drift and make tests exact.

Needs update at a slow, deterministic cadence, normally once per second. The update receives a context snapshot: activity, distance travelled, recent food, sleep state, injury, threats, work duration, bed quality, and passive auras.

No needs subsystem may directly change navigation or inventories. It emits high-level intents or priority modifiers consumed by the dinosaur state machine.

## Dinosaur behavior state machine

High-level mutually exclusive states:

```text
INJURED / MATERIALIZING
DEFEND or FLEE
SEEK_FOOD → EAT
SEEK_SLEEP → SLEEP
NAVIGATE_TO_WORK → WORK → DELIVER
FOLLOW_OWNER / RECALL
IDLE
```

Priority is centralized. Individual goals must not fight by repeatedly starting and stopping navigation. State changes have minimum residence times where appropriate, and emergency states can preempt ordinary work.

Animation state is a presentation of authoritative behavior, not the behavior source. Missing animation data falls back to a safe idle; it never halts server work.

## Base-level work scheduler

### Why base-level

If eight entities independently scan nearby blocks and inventories, they duplicate work, create unstable assignments, and scale badly. One scheduler per loaded base owns the offer index and reservations.

### Station contract

The internal `DinoWorkstation` contract should conceptually provide:

- stable station identity and `GlobalPos`
- interaction/approach positions
- supported work type
- current health/validity and base binding
- bounded `WorkOffer` collection
- `reserve`, `heartbeat`, `commit`, and `cancel` operations
- power demand and priority
- input/output resource views through NeoForge handlers
- a plain-language blocked reason

Stations register/unregister with the loaded base when placed, removed, loaded, rebound, or moved. The scheduler does not scan every block in the base to rediscover them.

### Work offer and reservation

A `WorkOffer` is immutable and includes:

- unique offer/order ID
- station ID and position
- work type
- priority
- required input/resource predicates
- estimated duration
- approach position
- output destination/route ID
- eligibility flags

Reservation leases expire unless the assigned dinosaur heartbeats while navigating or working. Cancellation is idempotent. Expiration returns reserved resources and station capacity exactly once.

### Assignment score

Use a deterministic score with stable tie-breaking:

- explicit player lock always wins if valid
- station priority
- work efficiency (primary 1.30, secondary 0.90, other 0.45)
- gene, mutation, level, and mood contribution
- travel cost/path history
- current hunger/rest need
- assignment stickiness to prevent thrashing
- species passive suitability

Log the winning score breakdown in debug mode. UI should expose the main reason, such as “best available crafter” or “manually assigned.”

### Tick budget

- Entity movement/navigation: vanilla tick as required.
- Needs and high-level state reevaluation: every 20 ticks, staggered by DinoId.
- Base scheduler: every 10–20 ticks or immediately when marked dirty.
- Power ledger: on topology/validity changes and at most once per second.
- Full station validation: staggered, never all bases on the same tick.

## Logistics and inventory safety

Use NeoForge resource handlers/capabilities for interoperability. Never mutate a neighboring container list directly.

Every item transfer follows:

1. Resolve and validate loaded source/destination handlers.
2. Simulate extraction.
3. Simulate insertion of the exact simulated result.
4. Reserve route capacity and source amount.
5. Let a transport order visibly carry a logical manifest.
6. At delivery, revalidate destination and commit insertion/extraction using a transaction-safe order.
7. If the destination changed, use configured overflow or return to source/depot; never delete the stack.

Do not store a live `IItemHandler`/resource handler beyond the current operation. Store `GlobalPos`, side, slot/filter information, and resolve again.

Routes are deliberately bounded: one configured destination per station plus optional overflow and pantry route. General-purpose arbitrary graph solving is not required for expressive bases and would be difficult to explain.

## Power ledger

Power is base-scoped integer capacity.

```text
environmental generation
+ assigned energy-worker contribution
+ temporary buffer discharge
= available capacity
```

Consumers submit demand and priority. Allocation is deterministic and stable; a consumer does not flicker every scheduler pass when supply is tied. A powered work order retains a short lease, while long idle machines release capacity.

Environmental producers cache validity. Block update events mark them dirty, with a slow safety recheck. Both persistent Wind Turbine phases validate the same sky/rotor clearance; the basic block contributes a 0.6 multiplier and the Processor-built upgraded block contributes 1.0. A Water Turbine retains water in its 3x3 cog structure and validates the three waterlogged cells along its bottom row.

The internal system should not pretend to be Forge Energy. A compatibility bridge can expose/import energy later without changing the base ledger contract.

## Combat and threat control

One `BaseThreatController` indexes recent valid hostile targets inside the base. Guardians query this bounded controller instead of each scanning large entity boxes every tick.

Target scoring considers:

- attacking an owner or companion
- distance to a vulnerable worker
- target damage/threat
- guardian role and current health
- turret/guardian coverage

Friendly fire, owner/team access, tamed animals, and allied companions are excluded server-side. Non-guardians receive flee destinations from the controller.

At zero owned-dinosaur health, normal death drops are suppressed and the injury transition runs once. Wild creatures use normal death/loot behavior.

## Networking and menus

### Synchronized presentation state

The shared dinosaur entity exposes a compact presentation view derived from its authoritative behavior state. It includes locomotion/behavior mode, current work type, carried display stack, sleep/injury/threat flags, and a short blocked reason when relevant. Renderers and screens consume this view; they never infer inventory ownership or work completion from an animation.

Short-lived effects such as pickup, delivery, footfall, roar, work contact, and impact use animation markers plus monotonic event sequence numbers. The client ignores an event sequence it has already shown, preventing chunk re-entry or packet replay from repeating old effects. Carried-item rendering attaches the synchronized display stack to the model's `carry_socket`, while the server retains the full logical manifest and transaction state.

Camera impulses, surface particles, attached items, gaze hints, and interpolated machine gauges remain presentation-only. They are distance-capped, rate-limited, and configurable. See `DETAIL_BIBLE.md` for the player-facing quality contract.

Register custom payloads with explicit codecs. Packets are small immutable records.

Typical client-to-server intents:

- activate/withdraw dinosaur
- assign dinosaur/station/work type
- set priority or overnight toggle
- configure a route/filter
- link with Signal Baton
- recall one/all
- rename base
- acknowledge alert

Every handler validates:

- authenticated sender
- sender owns or has permission for the base
- relevant menu/session or held linking tool
- dimension, loaded position, and interaction distance
- IDs exist and belong to the same base
- enum/resource keys are valid
- numeric bounds and payload size
- rate limit/replay expectations

The client never sends hunger, mood, genes, XP, output items, completed work, power generation, or damage as facts.

On menu open, send one immutable `BaseView`. Thereafter send versioned deltas at a bounded rate, normally no faster than five updates per second. If a delta version is missed, request/resend a full view. Do not send every dinosaur's full serialized snapshot every tick.

## Save versioning and recovery

All records start with `schemaVersion = 1`. NeoForge does not provide custom mod data fixers for every use case, so own explicit migrations between known schema versions.

Rules:

- Decode into a validated intermediate representation.
- Never partially apply a failed migration.
- Preserve unknown optional fields when practical.
- Clamp old out-of-range balance values and log one warning with the affected ID.
- Keep rolling development-world backups before schema changes.
- Include `/primevalworks diagnose base <id>` and admin recovery commands before public beta.

World-load corruption handling prioritizes preserving the save: quarantine a bad dinosaur/base record, show an administrator-facing diagnostic, and allow export/recovery. One malformed record must not prevent unrelated bases from loading.

## Configuration

Server configuration owns balance and resource limits:

- base count/radius and active/reserve caps
- needs rates
- genetics curves and mutation odds
- work and power coefficients
- injury/recovery times
- spawn/nest frequency
- expensive compatibility options

Configuration cannot disable identity checks, permit duplicate lifecycle states, trust client results, or remove packet validation.

Client configuration contains presentation only: indicator density, animation quality, UI scale, color/accessibility choices, and optional debug overlays.

## Performance budgets

- Default one active base per player; architecture supports a configurable higher count.
- Maximum eight active companions per base.
- Bounded base radius; no dimension-wide searches.
- No forced chunk loading by default.
- No per-tick inventory scans.
- No all-entity threat scans from every dinosaur.
- Cache station topology and invalidate it on events.
- Stagger periodic work by stable ID.
- Particle, sound, and network effects have distance and rate limits.
- Rendering scale/hue is client-side presentation; server pathfinding uses stable dimensions.

Profile with several players and the configured maximum active companions before release. Optimize measured hot paths, not guesses, but reject architectures that obviously multiply scans by entities × stations × inventories.

## Test strategy

### Pure deterministic tests

- mutation distribution over fixed seeds and boundary rolls
- gene-to-attribute derivation and clamps
- 130%/90%/45% work efficiency classification
- mood/hunger fixed-point transitions
- power allocation priorities and tie stability
- scheduler scoring and assignment stickiness
- inventory simulation/commit compensation
- codecs, validation, and each schema migration

### GameTests

- hatch with and without table capacity
- activate, withdraw, save, reload, and reactivate with same UUID
- break/re-place a bound Command Table without duplication
- missing/unloaded active entity does not duplicate
- zero health creates exactly one injured snapshot and no owned loot
- station removal cancels and returns reservations
- full/removed destination never deletes transported items
- sleep/wake/guardian threat behavior
- power producer validity after neighbor updates
- owner/team packet rejection

### Manual release matrix

- integrated single-player client
- dedicated NeoForge server with two clients
- reconnect while a dinosaur is working
- server stop during work and after withdrawal
- chunk unload/reload around a base
- death/injury under several damage sources
- table break by owner, teammate, stranger, explosion, and piston attempt
- GeckoLib missing/wrong-version dependency message
- clean installation from CurseForge profile

## Logging and diagnostics

Normal logs report lifecycle failures and recoverable corruption once. Debug logs may include BaseId, DinoId, order ID, state transition, station, and score breakdown. Do not log every AI tick.

UI and commands should surface actionable diagnoses:

- no path to interaction point
- missing food/diet mismatch
- no bed
- station unpowered
- output full or route unloaded
- invalid environment for generator
- no eligible worker
- worker resting/injured
- permissions failure

If a user can report those exact messages and IDs, support becomes dramatically faster than interpreting “it stopped working.”

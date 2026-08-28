# Primeval Works verification checklist

Use this checklist after gameplay, renderer, UI, persistence, or resource changes. A clean compile is necessary, but it does not prove an interaction looks or feels correct.

## Launch and automated checks

From `C:\Users\Hayro\Downloads\primeval-works`:

```powershell
.\gradlew.bat test build
.\gradlew.bat runGameTestServer
.\run-client.bat
```

The IntelliJ run configuration is **Primeval Works Client**. If IntelliJ reports that `build\moddev\clientRunVmArgs.txt` is missing, run `gradlew.bat createMinecraftArtifacts` once or use `run-client.bat`; never point the IDE at the old `nnnnn (1)` workspace.

After a crash or protocol disconnect, inspect `run\logs\latest.log` and the newest file under `run\crash-reports` before changing code.

## Useful developer commands

Both `/pw` and `/primevalworks` are valid aliases. Commands require cheats/operator permission.

```mcfunction
/pw help
/pw hatch spinosaurus
/pw hatch all
/pw mutation huge
/pw mutation albino
/pw mutation both
/pw mutation clear
/pw roster
/pw recall
/pw egg any
/pw egg small
/pw egg big
/pw egg large
/pw insight 100
```

`/pw mutation` changes the nearest owned dinosaur within 16 blocks. To exercise command-defeat recovery directly:

```mcfunction
/kill @e[type=primevalworks:spinosaurus,sort=nearest,limit=1]
```

## Command Table and ownership

1. Place one Command Table and hatch at least eight dinosaurs with `/pw hatch all` or individual hatch commands.
2. Confirm the table is one full block, uses the authored model/texture, has a one-block outline/collision, and is not tiny in hand.
3. Open it and check the seven active windows. Each model must stay inside its `23x23` interior during opening, hover, parallax, and dragging—no fraction of a pixel may enter the bars or neighboring slots.
4. Open the depot. Living cards must use the authored living grid. Recovery cards must sit lower in the separate four-slot recovery row, with the label below them.
5. Drag a dinosaur active-to-depot and depot-to-active. Use **Store All** and verify all eligible dinosaurs actually enter the depot.
6. Send a dinosaur on an expedition. It must be dimmed/locked and impossible to swap, recall, or duplicate until return.
7. Leave the world while one dinosaur is active, one is in the depot, one is on expedition, and one is recovering. Rejoin and verify every UUID exists in exactly one state.
8. Use `/kill` on an owned dinosaur. The single recall animation should squash/shrink, arc to the table, fade red, then remove the world entity. It must appear only in recovery—not in an active slot and not as a ghost roster entry.
9. Wait for recovery or use the relevant test setup. The dinosaur must return with full health, no stored fall distance, and no duplicate entity.

## Exact Command Table preview checks

- Active first interior: `(115,51)`, `23x23`; x stride 27 for seven.
- Depot first interior: `(311,66)`, `23x23`; x stride 26, y stride 27.
- Recovery first interior: `(311,155)`, `23x23`; x stride 26 for four.
- All species use a top-down three-quarter presentation and a species preview profile. Never change world scale or hitbox to fix a card.
- Spinosaurus, Parasaurolophus, and Pteranodon are the mandatory oversized/awkward-shape regression checks.

## Spinosaurus mount

1. Saddle and mount a normal Spinosaurus, then repeat with Huge and Huge+Albino.
2. On land, verify the rider is visibly supported at animated `whereplayersits`, including during idle, walk, sprint, attack, terrain pitch, and animation transitions.
3. Press Space on land. The Spinosaurus must not jump; it crosses normal terrain through step height and terrain pitch.
4. Walk over one-block rises and descents. The model should pitch smoothly and stay grounded rather than lose momentum or detach the rider.
5. Sprint with Shift+forward. Acceleration, FOV, gait speed, and a brief foot-contact camera impulse should clearly communicate the sprint without continuous shaking.
6. Left-click on land. The mount must not hit itself or its rider; it should look toward the attacked point and apply damage at the authored contact frame without a server-correction snap.
7. Enter water. Steering and acceleration should be smooth, oxygen should drain ten times slower, and no Night Vision/custom clarity effect should appear.
8. In first person, confirm the camera follows animated `whereplayersits` instead of the static middle of the body.
9. Left-click underwater. Nothing should attack or apply damage.
10. Breach at several speeds and angles. The swim pose must persist through the arc without briefly snapping backward into swim/land, the return pitch must ease downward, external contacts may deal damage, and the Spinosaurus must never take damage from its own landing.
11. During ground turns, attacks, steep upward swimming, and a breach, watch the rider in third person. It must remain at `whereplayersits` without hovering above the mount, one-frame jumps to the body center, aquatic micro-jitter, or delayed ground catch-up. Re-entering water produces one splash/bubble burst; sustained ascent produces a restrained bubble trail.
12. Dismount beside a deep pool and let the Spinosaurus enter it on its own. It should rise gently until its head reaches the surface, bob there without full jump impulses, and stop using the swim clip as soon as its body leaves water; repeat mounted afterward to confirm breach/steering were unchanged.
13. While mounted on land, walk straight over one- and two-block rises and descents. The body should pitch from front/rear ground support, step the obstacle without jumping, and keep the rider supported for both Normal and Huge.
12. Confirm the walk animation rate follows actual speed, including sprint and Albino/Huge multipliers, without foot sliding.

## Global dinosaur presentation

- While moving and attacking, legs keep the authored walk/run gait; upper-body action does not freeze them.
- Turning leads with head/neck and lets the torso follow. No snap, twitch, moonwalk, backward pursuit, or 360-degree hunting spin.
- Albino textures preserve shading. Only the authored pupil pixels become red; inspect every species rather than trusting a global tint.
- Sleeping uses a simple billboard `Z`; work progress appears over the station; transport cargo uses the hotbar frame and species attachment where available.

## Whistle, follower, and persistence checks

1. Right-click a Dino Whistle in the player inventory or while holding it. Its normal vanilla tooltip must remain visible, and the authored setup panel should open immediately without a hold timer or permanent sidebar.
2. Configure Connected Quarry and left-click one block; then configure Area Quarry and left-click two opposite corners. Both must open the follower picker over the mark, assign without a false `mark expired` message, execute only the bounded order, and clear when finished. An assigned Area keeps moving edge dashes plus diagonal lines travelling across all four vertical faces while the Whistle is held. Lumber uses one left-clicked log; Harvest and Collect do not consume attack input.
3. Open Collect's plus slot. The setup card must slide left while the searchable creative-style item catalogue slides right. Search, scroll, click, and drag must all select a filter without consuming an item.
4. Assign a passive Harvest or Collect order, switch the dinosaur to Home and Stay, then back to Follow. The same order, range, filter, and marked settings must resume rather than reset.
5. Repeat the previous check across a chunk unload, logout/rejoin, full client restart, depot storage/reactivation, and dedicated-server restart. The dinosaur UUID and field order must remain unique and server-authoritative.
6. Lead a normal follower across open ground, around walls, through a doorway, and more than 30 blocks away. It should repath without zigzagging; after a genuine stall it may recover near the owner, but it must never teleport during ordinary close movement.
7. Lead an unmounted Spinosaurus through water. It must swim toward the owner instead of its idle buoyancy goal cancelling navigation. Dismount during a breach and confirm it returns to the correct land/water pose without a latched swim animation.
8. Lead a Pteranodon far enough behind to require flight. It should enter powered catch-up flight, update toward the moving owner without restarting its flap cycle every few ticks, and land/return to normal navigation when close.
9. Open a companion screen while both player and dinosaur are outside the base. **Jobs** must be replaced by a readable **Call Back** action; base work cannot be opened remotely.
10. With two real players, verify ownership and follower caps independently. One player must not command, overwrite, teleport, or resume another player's dinosaur or saved order.

## Release gate

- `test build` passes.
- All required GameTests pass on the dedicated GameTest server.
- Integrated client enters a world without a fatal error, resource failure, or network protocol disconnect.
- Changed interactions receive a hands-on visual/gameplay pass.
- Multiplayer ownership/persistence is tested with two players before a release candidate.

# Assign Job UI Pack

Make exactly **20 UI artboards** for the complete assignment system. Transport, Fire, Energy, Crafting, and Gathering reuse the same planner shell; their icons, steps, colors, and text change in code.

Do not bake names, numbers, item names, coordinates, or descriptions into the art. Leave those areas blank. Buttons can have embedded symbols, but their words should be drawn by the game so translations and changing states fit.

## Master placement map

Use the whole screen as the canvas, but keep the middle **completely clear** because that is where the player sees and clicks the base.

```text
          [ THIN TITLE / CONTROL BAR ]

[ WORKFLOW ]       OPEN WORLD VIEW       [ RULE DRAWER ]
[   RAIL   ]       + BLOCK CURSOR         [ only one tab ]

                  [ HOVER CARD ]

          [ MESSAGE | CANCEL | SAVE ]
```

- Top bar: centered at the top, roughly 65–72% of the screen width and only 6–8% of its height.
- Workflow rail: top-left, around 18–21% of the screen width. Its height ends immediately after its steps; it must not run to the bottom.
- Rule drawer: top-right, around 22–25% of the screen width and no more than 40% of the screen height. Only one drawer/tab is visible at once.
- Bottom bar: centered at the bottom, the same width as the top bar and only 6–8% of the screen height.
- Hover card: close to the cursor, but automatically flips sides so it never sits under the cursor.
- Large pickers/editors: slide over the right half or appear as a centered popup. They temporarily replace the small rule drawer; they do not stack on top of it.
- Warnings and confirmation sheets: centered, approximately 38–48% of screen width. Everything behind them stays visible.

1. **Planner HUD shell**  
   **Place:** thin centered top bar plus thin centered bottom bar; the middle is transparent. **Contains:** title at top-left, dino/job below it, base name and orbit/zoom controls at top-right, feedback text at bottom-left, Cancel and Save at bottom-right.

2. **Workflow rail**  
   **Place:** upper-left beneath the top bar. **Contains:** heading, one short job description, up to five step-card sockets, and one current-instruction line. End the panel directly after the instruction.

3. **Workflow step card states**  
   **Place:** stacked vertically inside the Workflow Rail. **Contains:** icon socket on the left, role name upper-right, block name or coordinates lower-right, and a thin colored state strip. Make idle, hovered, active, completed, optional, and invalid states.

4. **World block inspector**  
   **Place:** beside the mouse cursor; code flips it left/right and above/below near screen edges. **Contains:** current role, block icon, block name, coordinates, and one short valid/invalid reason.

5. **Route rules drawer**  
   **Place:** upper-right beneath the top bar. **Contains:** Route and Stock tabs at the top, then five rows for Priority, Schedule, Repeat, Route Policy, and Safety. Each row has label left and current value right.

6. **Stock and items drawer**  
   **Place:** exactly where Route Rules sits; it replaces that tab instead of appearing beside it. **Contains:** Batch Size, Source Reserve, Target Stock, Match Mode, filter name, Any button, and one horizontal item-slot row.

7. **Expanded item picker**  
   **Place:** slide in over the right 38–44% of the screen and hide the small rule drawer. **Contains:** search field at top, Any Item and category controls below, scrollable item grid in the middle, selected preview near the bottom, match toggle, Back and Confirm at the bottom.

8. **Item tag/category picker**  
   **Place:** centered over the Expanded Item Picker, not over the world center. **Contains:** scrollable category list on the left and the selected category's icon, name, examples, and explanation on the right.

9. **Recipe/task picker**  
   **Place:** large right-side drawer replacing all other right drawers. **Contains:** recipe list left, selected recipe preview right, input/output slots, time, power, machine, quantity control, Back and Queue buttons.

10. **Gathering area editor**  
    **Place:** narrow lower-left palette beneath the Workflow Rail; the 3D area remains visible in the center. **Contains:** Corner A/B, Move, Height Up/Down, Clear, Include Drops, Break Blocks, Harvest Crops, and Finish.

11. **Preferred zone editor**  
    **Place:** narrow lower-left palette, replacing the Gathering palette. **Contains:** zone type, brush size, paint, erase, clear, and preference strength; show the current zone color at the top.

12. **Avoid zone editor**  
    **Place:** same lower-left palette position as Preferred Zones. **Contains:** hazard type, brush size, paint, erase, clear, and Allow During Emergency.

13. **Schedule editor**  
    **Place:** centered popup while the world stays visible. **Contains:** day/night timeline across the top, draggable start/end markers, Always/Day/Night presets, sleep protection, break frequency, overtime, Cancel and Apply.

14. **Priority editor**  
    **Place:** centered popup. **Contains:** reorderable task list on the left, selected-task priority and interrupt rules on the right, Resume Previous Task, Cancel and Apply.

15. **Fallback chain editor**  
    **Place:** right-side drawer replacing Route/Stock. **Contains:** five ordered fallback slots, drag handles, validity lights, add/remove buttons, and final behavior when every choice fails.

16. **Route review sheet**  
    **Place:** centered confirmation sheet. **Contains:** compact route flow across the top, all selected rules in two columns, estimated distance/time, warning strip, Back and Save Assignment.

17. **Conflict fixer**  
    **Place:** centered warning sheet above everything. **Contains:** warning icon and problem at top, affected step/block in the middle, suggested fix below, Locate/Edit, Ignore if Safe, and Cancel.

18. **Dino unavailable warning**  
    **Place:** small centered sheet or upper-center banner; never full-screen. **Contains:** dino portrait/icon, reason, expected return, Locate Dino, Fix Problem if possible, and Dismiss.

19. **Saved/success toast**  
    **Place:** upper-right beneath the top bar, sliding in and then leaving automatically. **Contains:** success icon, one-line result, optional warning count, and Undo.

20. **Quick-help overlay**  
    **Place:** bottom-center just above the bottom bar. **Contains:** compact mouse/key icons for orbit, zoom, select, remove, see-through inspect, recenter, and exit. It disappears automatically.

## Shared export pieces

Each artboard may contain its own component sheet, but keep these shapes visually identical everywhere: normal/hovered/pressed button, selected/unselected tab, item slot, icon socket, divider, scrollbar, checkbox, toggle, warning stripe, tooltip pointer, and paper-panel corners.

The world markers are rendered by code rather than full UI images: source outline, workstation outline, destination outline, fallback outline, area volume, preferred-zone fill, avoid-zone fill, route arrows, and invalid-block cross. The art team only needs to design their colors and small symbols.

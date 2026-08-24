# Primeval Works — in-game writing style

Primeval Works sounds like a field researcher who has spent enough time with the animals to stop treating them like specimens. The voice is observant, practical, warm, and occasionally dry. It is not a corporate dashboard, a tutorial bot, or a wall of fake ancient prophecy.

## Core voice

- Say what is happening before adding flavor.
- Prefer one specific observation over three broad adjectives.
- Let the dinosaurs have habits, not human dialogue.
- Ancient technology is strange but physical: worn contacts, humming stone, warm metal, hairline cracks, dust caught in grooves.
- Humor is quiet and rare. Never turn every tooltip into a joke.
- Trust the player. Do not explain Minecraft controls they already understand unless ours differ.

Examples:

| Avoid | Use |
|---|---|
| “Allows the player to efficiently transport a variety of items.” | “Carries finished goods to a linked chest.” |
| “This mysterious ancient artifact pulses with untold primordial power.” | “The metal is warm. Something inside is still turning.” |
| “The dinosaur is unable to complete its currently assigned task.” | “Moss can’t reach the furnace.” |
| “Insufficient nutritional resources detected.” | “No suitable food in the pantry.” |
| “Click here to assign a work suitability.” | “Assign work” |
| “Your prehistoric companion is in a negative mood state.” | “Juniper needs rest.” |
| “Unleash the awesome power of this legendary weapon!” | “Marked targets draw your guardians’ attention.” |

## Information order

For tooltips and status details:

1. Current fact
2. Cause
3. Fix, if the player can act
4. Optional observation/flavor

Example:

```text
Route blocked
The output chest is full.
Choose another destination or clear some space.
```

Do not show all four lines when one is enough:

```text
Taking berries to the pantry
```

## Dinosaur cards

A roster card uses concise facts:

```text
Moss
Parasaurolophus · Level 4
Taking berries to the pantry
```

The detailed view may add one changing observation chosen from authored lines:

- “Moss hums when the workshop gets quiet.”
- “She keeps checking the empty trough.”
- “Fresh mud still clings to his crest.”
- “They settle faster when Bramble sleeps nearby.”

Observations must follow real state. Never claim a behavior the player cannot see or that the simulation does not track.

Use player-selected pronouns only if that feature is implemented. Otherwise use the dinosaur’s name or singular “they”; do not guess from species or texture.

## Block and item tooltips

Names stay short. The first tooltip line explains use in ordinary language. A second muted line may carry flavor when it adds character.

```text
Signal Baton
Links stations, storage, and guard posts.
The bronze tip twitches near an active route.
```

```text
Compressed Core Buffer
Keeps machines running through short power shortages.
Stored: 84 / 120 power-ticks
```

Avoid starting every tooltip with “Used to,” “Allows you to,” or “This item.”

## Alerts and blocked reasons

Every alert names the affected dinosaur/station when space allows:

- “Moss can’t reach the Ancient Furnace.”
- “The east trough is empty.”
- “Wind Turbine: rotor blocked above.”
- “Processor is waiting for power.”
- “Bramble has nowhere to sleep.”
- “Cargo Depot is full; 12 Hardwood returned to the Timber Station.”

Errors never blame the player and never hide the outcome. If items were safely returned, say so. If an action was rejected by permissions, say who owns the base.

## Genome language

The microscope/genome screen is scientific enough to feel precise but not modern-laboratory sterile.

Labels:

- Work aptitude
- Vitality
- Movement
- Body variation
- Rare traits
- Genome stability

Use “Not observed” instead of question-mark spam. Use “No rare traits” instead of “Mutation: NULL.”

Quality descriptions are restrained:

- Fragile
- Typical
- Strong
- Exceptional

The actual numeric genes remain visible for players who want them. Descriptions do not replace data.

## Quest and advancement text

Advancement titles are short and memorable. Descriptions state the accomplished act, not a command to do it.

```text
First Echo
Hatch a creature from an ancient egg.
```

```text
Eight Sets of Footprints
Keep a full active roster fed and content.
```

```text
Still Turning
Bring an ancient machine back to life.
```

Avoid generic titles such as “Dinosaur Master,” “Automation Expert,” or “Ancient Beginnings” unless the wording connects to a specific in-game moment.

## Lore

Lore arrives as short field notes, fossil inscriptions, machine wear, and player inference. Do not deliver the whole backstory in an opening monologue.

A good lore entry contains something concrete:

> Grooves circle the inside of the alloy ring. They are deepest where a claw would rest.

A weak lore entry only claims importance:

> This legendary device was used by an incredibly powerful forgotten civilization long ago.

We do not name or fully explain the ancient builders until the gameplay has evidence worth discovering.

## UI labels

- Sentence case for tabs, buttons, headings, and statuses.
- Use verbs for actions: “Assign,” “Recall,” “Link storage,” “Let rest.”
- Use nouns for destinations: “Roster,” “Work,” “Routes,” “Power.”
- Avoid ALL CAPS in localized strings. The renderer may apply a visual small-caps style without changing the source copy.
- Avoid punctuation in short buttons unless clarity requires it.
- Numbers include units where ambiguity exists.
- Color never carries the whole meaning; include an icon, label, or pattern.

## Localization and implementation

- Every player-visible string lives in language files.
- Never build sentences by concatenating translated fragments.
- Use translatable parameters for names, counts, and values.
- Provide singular/plural-aware strings where counts read naturally.
- Keep line lengths flexible; screens must tolerate longer translations.
- Do not encode layout spaces or manual line breaks into ordinary translations.
- Developer logs may be technical. Player copy should not leak class names, registry IDs, UUIDs, or stack traces unless a copyable diagnostics view is explicitly opened.

## Final human pass

Before release, read every new string aloud and ask:

1. Would a person say this once, in this situation?
2. Does it reveal a real fact?
3. Is it shorter without losing the useful part?
4. Does it repeat a nearby label?
5. Does it sound like five other tooltips because they share a template?

If it feels generated, replace the general sentence with a specific observation from the actual mechanic.

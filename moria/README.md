# Moria — a Dungeon Crawler in simple new video-game theatrics

> For the very willing, the very picky, the most excellent — the Adventurers in
> the Careful Years of Time.

A small, deterministic **Moria dungeon crawler** rendered through a **GUI-driven
text pane / canvas** with excellent text features. A lone hero descends the
Mines of Moria one level at a time, moving by torchlight through rough-hewn
halls, past the orc and the troll, down toward the **Balrog on level eight** —
and, if resolve and luck hold, up and out into distant daylight.

All game logic is SLeeLa source. The GUI is **SleelaUI™** (SLeeLa's own
cross-platform native toolkit), painting the dungeon into a text canvas. The
game is fully playable **headless** too — the same text read drives both.

## What's here

| Path | Contents |
|---|---|
| [`game/MoriaDungeon.sleela`](game/MoriaDungeon.sleela) | The crawler core: map generation, the hero, monsters, combat, descent, the scrollback chronicle, the authoritative text rendering, and the **figuring pieces** (the Original Characters that roam the board). A single `#sleela 1.6` game Wrapper. |
| [`game/CityLights.sleela`](game/CityLights.sleela) | The **scoreboard lighting** layer: lights that come out of the game's text, coloured by a city's Providence (learned from trusted sources) and sized 2–4 mm. |
| [`game/MoriaStats.sleela`](game/MoriaStats.sleela) | **The six ability scores** — Strength, Dexterity, **Constitution**, Intelligence, Wisdom, and **Character (the Number)**. Owns the modifiers and the Character-Number *truth model*. |
| [`game/MoriaSpells.sleela`](game/MoriaSpells.sleela) | **The Spellbook** — a name-forge of **18,432** dressed-up spells, and the signature **Fireball = 1d28 + 6 per level**. |
| [`game/MoriaWeapons.sleela`](game/MoriaWeapons.sleela) | **The Armoury** — a name-forge of **13,440** dressed-up melee weapons (**many +4 or greater**) **plus crossbows and bows**: ranged arms that reach **≥ 8 squares**, enchant **+3 to +28**, and strike for **2d12 + 8**. |
| [`game/MoriaSave.sleela`](game/MoriaSave.sleela) | **Save / Load / Alter** for creatures and character stats, plus the **saved previous leveling work** (a persisted level-up ledger). |
| [`game/MoriaTest.sleela`](game/MoriaTest.sleela) | A deterministic self-check: map geometry, in-bounds invariants, chronicle growth, and same-seed reproducibility. |
| [`ui-sleela/MoriaUI.sleela`](ui-sleela/MoriaUI.sleela) | The SleelaUI **text-pane front-end**: a real native window presenting the map pane, the HUD, the glyph legend, and the chronicle, on a slick-black / torch-amber theme. |
| [`Makefile`](Makefile) | Build dispatcher (`game` / `run` / `ui` / `test`). |

The SleelaUI widget toolkit is **reused** from the Airport Tycoon Edition 1
vendor drop at [`../1/sources/user-interface`](../1/sources/user-interface)
(byte-for-byte the upstream `mearvk/SLeeLa` `lib/user-interface`), so there is
one authoritative copy of the widget vocabulary rather than a duplicate.

## The excellent text features

The text pane is built from the toolkit's own widgets — no new native
primitives — and layers the "excellent text" on top in SLeeLa:

- **A monospaced map canvas.** Each dungeon row is one left-aligned text run
  inside a dark surface card, under a monospaced font request, so every column
  lands under the one above it — the grid reads as a canvas.
- **Moving torchlight (fog of war).** Cells beyond the torch radius read as
  dark; remembered walls stay faintly drawn. The lit circle follows the hero.
- **A HUD status strip.** Hero, depth (`/8`), HP, gold, kills, torch radius, and
  turn count, in one themed line.
- **A glyph legend.** Every theatrical glyph with its meaning, each as a badge +
  label row.
- **A scrollback chronicle.** A message history (newest last) with a scrollbar,
  so a picky Adventurer can read the whole descent.
- **A themed result banner.** Victory, a fall in the dark, or an ongoing
  descent, as an inline notice.

## Scoreboard lighting — lights out of the text

The scoreboard's Font instances **radiate light**, like torches struck behind
the letters. The model ([`game/CityLights.sleela`](game/CityLights.sleela)) is
faithful to SLeeLa's real font-effect vocabulary (`lib/user-interface/SLFontEffect`:
a `GLOW` / `LIGHT` / `EMITTER` with a packed `0xRRGGBBAA` colour, an emitter
radius, and an intensity), so a full SleelaUI build can bind these descriptors
straight onto an `SLFont`; here they also render as text so the game reads with
no display.

**Colour by Providence and by who's playing:**

| What | Light |
|---|---|
| A **Great** city | **orange** |
| An **Excellent** city | **green** |
| The **Trusted Player** | **purple** |
| Anything ordinary | dim white |

**GeoLocation & City Providence.** Each city carries its geolocation
(latitude/longitude) and a Providence tier. A city's tier is **not guessed** —
it is **learned by reaching out to a Source of Information**: a rated website or
a trusted search result. Only a source that clears the trust threshold (≥ 70)
may set a city to Great or Excellent; a weak source (e.g. a rumour blog) leaves
the city ordinary and its light dim. So the lights on the board reflect
*verified* standing, not hearsay.

**Size — 2 to 4 mm.** Every scoreboard light is sized in millimetres, clamped to
the board's **2–4 mm** band (ordinary 2 mm, great 3 mm, excellent and the
player's beacon 4 mm), then converted to an emitter radius in px for the font
effect.

```
Scoreboard — lights out of the text (2..4 mm):
  Moria Gate     [Excellent]  37.774N, 122.419W  light=green  4mm/16px  src=survey.example (rated 4.6) (trust 88)
  Bree           [Ordinary]   48.856N, 2.352E     light=dim    2mm/8px   src=unverified
  Minas Tirith   [Great]      51.507N, 0.127W     light=orange 3mm/12px  src=trusted-search:gondor (trust 81)
  Rivendell      [Excellent]  40.713N, 74.006W    light=green  4mm/16px  src=atlas.example (rated 4.8) (trust 92)
  Avery          [TRUSTED Player]                  light=purple 4mm/16px
```

## The figuring pieces — Original Characters that move around the UI

Beyond the lone `@`, the board now carries a **one-off set of figuring pieces**:
the **Original Characters** of the tale, each a distinct token that **moves
around the dungeon pane** on its own. They are mustered at the start and
**follow the hero down** each level, so the UI reads like a Fellowship picking
its way through the Mines rather than a single dot.

| Glyph | Figure | Behaviour |
|---|---|---|
| `G` | **Gandalf** — the Guide | Companion; keeps formation near the hero |
| `F` | **Frodo** — the Bearer | Companion; keeps formation near the hero |
| `A` | **Aragorn** — the Ranger | Companion; keeps formation near the hero |
| `L` | **Legolas** — the Elf | Companion; keeps formation near the hero |
| `D` | **Gimli** — the Dwarf | Companion; keeps formation near the hero |
| `S` | **Sauron's Eye** — the Stalker | A named foe; haunts the deeper halls (depth 4+) |

Each piece is a `Figure` struct node in an intrusive linked list (the same
array-free pattern as the chronicle and the cell mutations). Every turn the
companions **drift toward the hero** with a small seeded wobble (so the
formation looks alive but never marches in a dead-straight line), while the
stalker closes in with intent. Movement is clamped to open floor — pieces never
overlap, never clip a wall, and never leave the map — and it is **fully
deterministic**: the same seed replays the same wandering, step for step. The
GUI gets a dedicated **"The Fellowship — Figuring Pieces"** panel (glyph badge +
name + role + live position); the headless read prints the same roster. Run
`make figures` (or `make run`) to see it.

## The RPG layer — stats, spells, weapons, and the saved book

The Adventurer (and the creatures of the Mines) now carry a full character
system, composed into the crawler core and persisted through a save book.

### The six ability scores — with Constitution and the Character Number

Every sheet carries **Strength, Dexterity, Constitution, Intelligence, Wisdom,**
and **Character (CHR#) — the Number** ([`game/MoriaStats.sleela`](game/MoriaStats.sleela)).
Constitution drives hit points (a base plus CON per level, so the body endures
the long descent). The **Character Number is not vanity — it is how TRUE the
rest of the numbers are:** at a high Character Number the sheet reads exactly as
written, but a low Character Number *blurs* the apparent scores away from their
true values and reads a life and its choices less honestly. **The higher the
Character the Number, the truer his numbers will be about his life and his
actual choices.** The sheet shows a live *truth %*.

### The Fireball — 1d28 + 6 per level

The signature spell, by decree: **Fireball = 1d28 + 6 per level** (plus the
caster's Intelligence power). At level 5 with a die of 19 and +2 INT that is
`19 + 30 + 2 = 51` damage. See `castFireball()` in the dungeon core.

### Thousands of spells, thousands of weapons — dressed up

- The **Spellbook** forges **18,432** distinct spell names from a deterministic
  grammar `[Adjunct] [Root] of [Epithet]` across eight schools and nine tiers —
  *Greater Fireball of Khazad-dûm*, *Abyssal Scourge of Durin's Bane*, and so on.
- **Crossbows and bows** — the ranged arms. Every bow or crossbow reaches **at
  least 8 squares** (crossbows a little farther), carries a rich **+3 to +28**
  enchantment, and strikes for **2d12 + 8** — an average set of 2·6 + 8 = **20**
  before the bonus (and `fireAt(...)` looses one down the hall when a monster
  stands within range). The hero slings one as a sidearm from the first step
  into the Mines, and finer ones turn up on the deeper levels.
- The **Armoury** forges **13,440** distinct melee weapon names from `[Material]
  [Form] of [Legend]`, each with a **+N enchantment**. **Many are +4 or
  greater**, and the deeper levels of Moria mint the legendary (up to +9) —
  *Mithril Greatsword of the Balrog-slayer +7*.

Every name is **stable for its index**, so a given find always reads the same
(determinism — the Careful Years of Time).

### Save / Load / Alter — and the saved leveling work

[`game/MoriaSave.sleela`](game/MoriaSave.sleela) persists the dungeon's
**creatures** and the Adventurer's **overall stats** to a compact, line-oriented
save blob, restores from it, and lets you **alter** a creature's position, hit
points, or whole stat block in place. It also keeps a **ledger of previous
leveling work** — every level-up records the HP it granted and the stat it
raised, so a character's history survives a save and reads back on the sheet.
Run the demos with `make stats`, `make spells`, `make weapons`, and `make save`.

## Theatrics — the glyph alphabet

```
@  the Adventurer      #  rough dwarf-stone wall   .  lit floor
>  the stair down      $  a glint of treasure      +  a door
o  an orc              T  a troll (depth 3+)        B  the Balrog (depth 8)
G  Gandalf   F  Frodo   A  Aragorn   L  Legolas   D  Gimli   S  Sauron's Eye
   (space)             the unseen dark beyond the torch
```

## Determinism — the Careful Years of Time

Every roll — level layout, treasure, monsters, combat — comes from a seeded
linear-congruential stream. **A given seed replays the same descent, step for
step.** The self-check asserts this: two runs on the same seed reach the same
final depth, the same alive state, and the same gold.

## Build & run

```sh
make game    # type/parse-check the Wrappers with Sleelvac (if installed)
make run     # headless self-descent: prints the map, HUD, roster, and chronicle
make lights  # the scoreboard city-lights demo (orange/green/purple, 2-4 mm)
make figures # the figuring pieces: Original Characters roaming the board
make ui      # open the native SleelaUI text-pane window (X11 / Cocoa / Win32)
make test    # run the deterministic self-check
```

If the SLeeLa toolchain isn't on the path, `make game` skips cleanly; set
`SLEELA=/path/to/sleela` to enable it. With no display, `make ui` falls back to
the same text read as `make run`, so the game always reads.

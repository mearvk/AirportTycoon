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
| [`game/MoriaBestiary.sleela`](game/MoriaBestiary.sleela) | **The Legends Bestiary** — **24 named legends** (Tiamat, Vecna, Strahd, Drizzt, Elminster, …) keyed to **dungeon depth**: deeper is more experienced. Each is depth/tier-scaled into a full stat block. |
| [`game/MoriaGrimoire.sleela`](game/MoriaGrimoire.sleela) | **The Grimoire of Castings** — every spell usable in Moria, by class (Mage/Cleric/Wizard; **Fighters get swords**), plus the **Lich obedience pact** (You must Agree / Obey — it **pauses** your character) resolved by the **Spelling Duo**. |
| [`game/MoriaCreator.sleela`](game/MoriaCreator.sleela) | **The character-creation generator** — base-stat allowances (**Rolled**, **Point-set**, **Old Wisdoms**) and **lifetime feats** (the *snuff*): **Law Degree, Trusts of Universities, Great Wealth, Exception Institute**, Knighthood, Endowed Chair. |
| [`game/MoriaTest.sleela`](game/MoriaTest.sleela) | A deterministic self-check: map geometry, in-bounds invariants, chronicle growth, and same-seed reproducibility. |
| [`ui-sleela/MoriaUI.sleela`](ui-sleela/MoriaUI.sleela) | The SleelaUI **text-pane front-end**: a real native window presenting the map pane, the HUD, the glyph legend, and the chronicle, on a slick-black / torch-amber theme. |
| [`ui-sleela/SLImageFile.sleela`](ui-sleela/SLImageFile.sleela) | The **file loader**: a SleelaUI image widget that loads a real image **file** from a path (preserving its alpha), scaled to a box while keeping aspect — the loader behind the title logo. Bottoms out in the `uiImageFile` bridge. |
| [`ui-sleela/SLFontEffect.sleela`](ui-sleela/SLFontEffect.sleela) | The **light emitter descriptor**: a GLOW/LIGHT/EMITTER with a packed `0xRRGGBBAA` colour, an emitter radius, an intensity, and a **direction** (so an emitter can cast its light one way only — e.g. straight down). Binds through the `uiFontEffect*` bridge. |
| [`ui-sleela/MoriaTitleLogo.sleela`](ui-sleela/MoriaTitleLogo.sleela) | The **title-bar logo** widget: paints the D&D mark ([`images/D&D-logo-title.png`](images/D&D-logo-title.png) — trimmed to the mark, transparent background) as the window's cool title logo via `SLImageFile`, with the text heading as a graceful fallback. |
| [`ui-sleela/MoriaThrobber.sleela`](ui-sleela/MoriaThrobber.sleela) | The **title throbber**: a thin (2–4 px), full-width, resizing light strip directly under the title that casts a radiant **white-and-yellow** light **downward only** onto the Descriptive Canvas, with a constant 3D light-ebb animation that runs until the program ends. |
| [`ui-sleela/MoriaCharacterSelect.sleela`](ui-sleela/MoriaCharacterSelect.sleela) | The **character-select startup**: choose a ready-made hero from a small roster, or **forge a new one** with the creation generator — the chosen `Creature0` is adopted by `beginFromCharacter`. |
| [`ui-sleela/BRIDGE.md`](ui-sleela/BRIDGE.md) | The **SLVM bridge manifest**: the new `ui*` built-ins the Moria UI helpers introduce (`uiImageFile`, `uiFontEffectBind/Update`, `uiSleepMillis`), with signatures and the C ABI each should call. |
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

## The title logo, the throbber, and character select

- **A cool title logo.** The window title is the **D&D mark**, loaded from a
  real image file ([`images/D&D-logo-title.png`](images/D&D-logo-title.png)) by
  the `SLImageFile` loader. The asset is trimmed to the mark with a transparent
  background, so only the red dragon-ampersand reads over the slick-black stone.
  If the image can't be bound, the title falls back to a text heading.
- **A radiant throbber under the title.** Directly beneath the title sits a thin
  **2–4 px, full-width** light strip (it resizes with the window). It casts a
  **radiant white-and-yellow** light **downward only** — out of its bottom edge,
  onto and into the Descriptive Canvas the board is drawn on — via an
  `SLFontEffect` emitter masked to `DIR_DOWN`. A travelling bright spot sweeps
  across it while a slower breath pulses the intensity and warms the colour
  between white and yellow, so the band reads as light **moving in 3D** as time
  ebbs and fades. It is **quite visible and never stops**: the UI drives its own
  frame loop (`pump` + `requestRedraw`), ticking the light every frame until the
  user ends the program.
- **Choose or create your Adventurer.** On startup the GUI shows a neat
  **"Choose your Adventurer"** panel: pick a ready-made hero from a small roster
  (each a full `Creature0` from the generator) or **forge a new one** with the
  allowances and lifetime feats. The chosen hero is adopted via
  `beginFromCharacter`, so it walks into the Mines with its own stats, gold,
  standing, and feats. With no display (or no pick) a sensible, distinguished
  default is used so the game always begins.

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

### The Mace of the Deep — and the mutual plus

The signature mace does, on average, **1d48 + 26 hit points** (a d48 averages
24.5, so the Mace averages ~50 before Strength and the enchantment). Its **"+1
to hit or damage is mutual"**: the `+N` enchantment adds to **both** the attack
roll and the damage. That is a setting (assumed on) —
[`config/moria.conf`](config/moria.conf) `weapons.mutual_plus=1`, backed by
`MoriaWeapons.MUTUAL_PLUS`.

### The enchantment incurrence-radius / depth model

A `+N` weapon carries an **incurrence radius** of extra-damage potential that
scales with the enchantment and the dungeon depth:

```
radiusBase(bonus)      = 2^(bonus + 3)              +5 => 256
depthMultiplier(depth) = 16 / depth  (clamped >= 1) depth 16 => x1
radiusLevels           = radiusBase * depthMultiplier
extraPotential         = radiusLevels * 4
```

**Anchor (by decree):** a **+5** weapon at **Dungeon Depth 16** has about **256
radius levels** — roughly **1024 extra potential damage**. The falloff is
geometric, and **lower depths are more generous** (bigger rolls):

| Enchant | Radius @ depth 16 | Extra potential |
|---|---|---|
| **+5** | **256** | **1024** |
| +4 | 128 | 512 |
| +3 | 64 | 256 |
| +2 | 32 | 128 |
| +1 | 16 | 64 |

| A +5 weapon at depth… | Radius | Extra potential |
|---|---|---|
| 16 | 256 | 1024 |
| 8 | 512 | 2048 |
| 4 | 1024 | 4096 |
| 1 | 4096 | 16384 |

On each strike the weapon incurs a seeded extra-damage roll in
`[0, extraPotential]` on top of the base hit — so a `+5` Mace deep in the Mines
is genuinely devastating, and shallower levels roll even bigger. Tunable under
the `weapons.radius_*` keys in [`config/moria.conf`](config/moria.conf).

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

### The Legends Bestiary — named foes, keyed to depth

Twenty-four **named legends** may be met in the Mines
([`game/MoriaBestiary.sleela`](game/MoriaBestiary.sleela)), each keyed to a
**dungeon depth** on the simple, faithful rule that **deeper is usually more
experienced**: mortal adventurers and explorers haunt the upper halls, famed
wizards and heroes the middle depths, darklords and liches below that, and the
demon princes, archdevils, and draconic deities wait at the very bottom.

| Depth band | Who holds those halls |
|---|---|
| **1–3** | Volothamp Geddarm · Minsc · Drizzt Do'Urden |
| **4–6** | Jarlaxle Baenre · Bigby · Tenser · Elminster Aumar |
| **7–9** | Halaster Blackcloak · Mordenkainen · Tasha (Iggwilv) · Raistlin Majere · Xanathar |
| **10–12** | Kas the Betrayer · Lord Soth · Acererak · Strahd von Zarovich |
| **13–14** | Zariel · Miska the Wolf-Spider · Orcus · Demogorgon |
| **15–16** | Vecna · Asmodeus · Bahamut · **Tiamat** |

Each legend carries its **Name, Type/Species, Primary Realm/Setting, and Notable
Role/Title**, plus a **native depth** (1–16) and a **power tier** (1–10, deities
10, mortals ~2–4). From those, a legend is **materialised into a full stat
block** (STR/DEX/CON/INT/WIS/CHR#, HP, and a challenge rating) via
[`MoriaStats`](game/MoriaStats.sleela): deeper native depth and higher tier mean
mightier numbers, and a **deity reads with a true Character Number** (its numbers
do not lie). A legend met **at or below its home depth** fights at full strength;
met as a rare **stray far above its home**, it is weakened — a shadow of itself.
The dungeon names the **lord of the hero's current depth** on entry and on each
descent. Run the roster demo with **`make bestiary`**.

### The Grimoire of Castings — spells by class, lich pacts, and the Spelling Duo

Every spell that can be **used in Moria** lives in the Grimoire
([`game/MoriaGrimoire.sleela`](game/MoriaGrimoire.sleela)), with the classes
that may cast each. The specially-named castings are all here —

| Casting | Effect |
|---|---|
| **Heal** | restore hit points |
| **Teleport** | move across the hall |
| **Make Short Change** | alter small coin |
| **Cast for Better Wood** | mend a haft or door |
| **Heal Water** | purify a pool |
| **Demystify Sound** | reveal a true noise |
| **Call for Collegiance** | summon allies |

— alongside the **standard spells** for the casters (Ward, Arcane Bolt, Conjure
Light, Bless, Detect Evil, Identify, **Magic Missile**, **Trick**). The decreed
spell numbers:

- **Heal** — restores **1d40 + 6 per caster level**.
- **Heal Water** — recalls all spent mana to its correct natural place and time,
  **heals the Universe**, turns your morals and weathering to **Trust**, and
  **rejuvenates Base Constitution at 99.6%** (99.6% of the way to its true max).
- **Magic Missile** — three reliable darts, **3 × (1d4 + 1)**.
- **Trick** — follows **Magic Missile only** (MM first, then Trick — never the
  other order): adds a wound of **1d6 + 6, plus 28 HP per caster level**, on top
  of the missile.
- **Fireball past caster level 6 assumes Trick** — a steady **+42 HP** added to
  the blast.

Each class knows a different set:
**Fighters get their swords for now (no castings)**; **Mages** cast the arcane
strikers, **Clerics** the divine heals and wards, and **Wizards** — the full
scholars — command the widest book. Run `make grimoire` for the list and the
per-class counts.

**Lich obedience pacts.** A Lich (any lich/undead lord of a depth) may **request
an obedience pact**. *You must Agree. You must Obey.* The **Obey pauses your
character** — control is held, and you cannot act — until the pact is answered.
To be freed you are prompted for a **Spelling Duo**: your character must usually
be **right** — a pair of real-life events that **must have happened in the
correct order**. Answer the order rightly and the pause lifts and you are freed;
answer wrongly and the Lich keeps its hold (you stay Obey-bound until a later,
right answer). In the dungeon, `characterPaused()` actually blocks movement
while the pact is unresolved.

## Character creation — allowances and the snuff

Make an Adventurer with [`game/MoriaCreator.sleela`](game/MoriaCreator.sleela).
It offers three **allowances** for the base ability scores:

- **Rolled** — fresh scores (4d6-drop-low feel) from the seeded stream.
- **Point-set** — you supply the six base scores directly (a chosen build).
- **Old Wisdoms** — carry forward a seasoned baseline: the accumulated life
  favours **Wisdom** and the **Character Number**, so a veteran soul starts
  wiser and reads **truer** than a fresh recruit (and more years deepen both).

On top of the base, the **snuff** — the great feats of a lifetime, several of
them **won by law too** — grant creation bonuses. Each is applied once:

| Lifetime feat | What it grants |
|---|---|
| **Law Degree** (won by law) | +INT, +WIS, +standing, a purse |
| **Trusts of Universities** | +INT, +WIS, a scholarly **endowment**, and your forward **Democrat & Socialist** track moves **+5 levels** (1d0 + 5) |
| **Great Wealth** | summons **1000× your estimable wealth** from a known relation to a Kingdom/Great Kingdom — **≥ 20,000 gold coins** — plus +Character/standing |
| **Exception Institute** | **+1000 INT for 5 hours**, **2 Kingdoms** and **2 followers** (level-36 NPCs that may or may not engage, attack, or eat; held **28 rounds per summoner level ≈ 28 min/level**), plus +Character# and +WIS — the exception that makes the whole sheet **truer** |
| **Knighthood** (by Law) | +STR, +Character, +standing |
| **Endowed Chair** | **+8000 permanent INT** (a crumble pool that erodes over ~1000 moves as some fail; once crumbled the boost fades in **1–2 minutes**), plus +WIS and a stipend |

A built character carries its final six scores, its **starting gold**, a civic
**standing**, and the feats it holds; the dungeon can **adopt** it with
`beginFromCharacter(...)`, so a distinguished, well-endowed soul walks into the
Mines already mighty. Run the generator demo with **`make creator`**.

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

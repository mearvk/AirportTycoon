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
| [`game/MoriaDungeon.sleela`](game/MoriaDungeon.sleela) | The crawler core: map generation, the hero, monsters, combat, descent, the scrollback chronicle, and the authoritative text rendering. A single `#sleela 1.6` game Wrapper. |
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

## Theatrics — the glyph alphabet

```
@  the Adventurer      #  rough dwarf-stone wall   .  lit floor
>  the stair down      $  a glint of treasure      +  a door
o  an orc              T  a troll (depth 3+)        B  the Balrog (depth 8)
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
make run     # headless self-descent: prints the final map, HUD, and chronicle
make ui      # open the native SleelaUI text-pane window (X11 / Cocoa / Win32)
make test    # run the deterministic self-check
```

If the SLeeLa toolchain isn't on the path, `make game` skips cleanly; set
`SLEELA=/path/to/sleela` to enable it. With no display, `make ui` falls back to
the same text read as `make run`, so the game always reads.

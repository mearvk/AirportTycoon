# Airport Tycoon — Business Edition

A fun, fast-paced air-traffic game built to run on
**[SLeeLa](https://github.com/mearvk/SLeeLa)** and her JavaFX UI/animation layer.
All game logic is SLeeLa source; the JavaFX layer is the presentation/animation
substrate.

This repository holds two SLeeLa games side by side:

- **Airport Tycoon** — the versioned business/air-traffic game, in the numbered
  folders [`1/`](1/) and [`2/`](2/) (see the version index below).
- **Moria** — a deterministic dungeon crawler rendered through a native
  **SleelaUI** text pane, in [`moria/`](moria/). It is self-contained and
  developed on its own line; start at [`moria/README.md`](moria/README.md).

## Versioning

This repository is organized into **numbered version folders** at the root —
`1/`, `2/`, `3/`, … Each folder is a **complete, self-contained snapshot** of
the game's source for that version: its SLeeLa Wrappers, imported `/sources`,
the Sleela UI, config, and build dispatcher all live inside the version folder.
Nothing outside the version folders is required to build or run a version.

To cut a new version, copy the latest version folder to the next number and
evolve it there; older versions remain frozen for reference and comparison.

Every Edition also carries a **superb name** drawn from a wealth of vocabulary,
recorded in a `VERSION.NAME.md` inside its folder.

| Version | Name | Path | Contents |
|---|---|---|---|
| **1** | *Keystone Meridian* | [`1/`](1/) | The base game: the tower game, the business/life layer (Character + Citizen), the win condition (> $240,000 in a Major Eastern Region, rising month-over-month), and the Author Path that wins in exactly 1001 moves. See [`1/README.md`](1/README.md) · [`1/VERSION.NAME.md`](1/VERSION.NAME.md). |
| **2** | *Golden Assimilation* | [`2/`](2/) | **Latest.** The improvement line: the reputation-driven service premium, the on-time combo, and the Business Desk whose Level-5 **Great Assimilation** rakes in Major-Eastern money. See [`2/README.md`](2/README.md) · [`2/VERSION.NAME.md`](2/VERSION.NAME.md). |

> The latest version is the highest-numbered folder. Start at
> [`2/README.md`](2/README.md) for the current edition, or
> [`1/README.md`](1/README.md) for the original base game overview,
> architecture, and build/run instructions.

## Moria — the dungeon crawler

Alongside the versioned Airport Tycoon game, [`moria/`](moria/) is a small,
deterministic **Moria dungeon crawler**: all logic is SLeeLa source, presented
through **SleelaUI** (SLeeLa's own cross-platform native toolkit) as a text
pane / canvas, and fully playable **headless** too. It reuses the Airport Tycoon
Edition 1 SleelaUI widget vocabulary ([`1/sources/user-interface`](1/sources/user-interface))
rather than duplicating it. Highlights:

- The Adventurer starts on **Level 1, the planet surface**, then descends the
  **persistent** Mines of Moria — a level is maintained, so taking a stair down
  and back up returns you to the same hall; creatures grow **harder with depth**.
- Choose or **forge a character** at startup; a **title logo**, a radiant
  downward **throbber**, and the scoreboard **city-lights** dress the GUI.
- A full RPG layer — six ability scores, thousands of spells and weapons, the
  Legends Bestiary, the Grimoire, and a save/leveling ledger.
- **Walk-animation sprites** for the cast — `adventurer`, `warden`, `gandalf`,
  `gimli`, `frodo`, `aragorn`, and `moria-goblin` — each with a four-direction
  (`top`/`down`/`left`/`right`) × three-action (`start`/`mid`/`end`) sprite set,
  in both a flat layout and a nested `<direction>/<action>/` folder tree.

See [`moria/README.md`](moria/README.md) for the full tour and build/run targets
(`make run` / `make ui` / `make test`, and the sprite tools under `moria/tools/`).

## Layout

```
AirportTycoon/
├── README.md        ← this file (the repo index: Airport Tycoon versions + Moria)
├── LICENSE
├── .gitignore
├── 1/               ← Airport Tycoon version 1 — full, self-contained snapshot
│   ├── README.md    ← version 1 overview + how to build/run
│   ├── Makefile     ← version 1 build dispatcher
│   ├── config/
│   ├── game/        ← the .sleela Wrappers
│   ├── sources/     ← imported SLeeLa library sources (character, citizen,
│   │                   user-interface — the SleelaUI widgets Moria also reuses)
│   └── ui/          ← the JavaFX Sleela UI
├── 2/               ← Airport Tycoon version 2 (latest) — evolves from v1
│   ├── README.md    ← version 2 overview + how to build/run
│   ├── Makefile     ← version 2 build dispatcher
│   ├── config/
│   ├── game/        ← the .sleela Wrappers
│   ├── sources/     ← imported SLeeLa library sources (character, citizen)
│   └── ui/          ← the JavaFX Sleela UI
└── moria/           ← Moria — the SleelaUI dungeon crawler (self-contained)
    ├── README.md    ← Moria overview + build/run
    ├── Makefile     ← Moria build dispatcher (run / ui / test / …)
    ├── config/
    ├── game/        ← the .sleela game Wrappers (dungeon, bestiary, animation, …)
    ├── ui-sleela/   ← the SleelaUI front-end + presentation helpers
    ├── images/      ← per-character walk-animation sprites + the D&D title logo
    └── tools/       ← pure-stdlib sprite tools (autocrop / generate / folderize)
```

## Build & run a version

Each version is built from inside its own folder, e.g. for the latest (version 2):

```sh
cd 2
make check     # game-logic + economy + author-path tests (no JavaFX needed)
make ui-run    # launch the animated Sleela UI (Maven + JavaFX 21)
make author    # prove the Author Path wins in 1001 moves (needs SLeeLa toolchain)
```

See the per-version README (e.g. [`2/README.md`](2/README.md)) for the full
target list and toolchain notes.

Moria is built the same way, from inside its own folder:

```sh
cd moria
make run    # headless self-descent: map, HUD, roster, chronicle
make ui     # open the native SleelaUI text-pane window (falls back to the read)
make test   # the deterministic self-check
```

See [`moria/README.md`](moria/README.md) for the full target list (`lights`,
`figures`, `animation`, `catalog`, `sprites`, and the RPG-layer demos).

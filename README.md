# Airport Tycoon — Business Edition

A fun, fast-paced air-traffic game built to run on
**[SLeeLa](https://github.com/mearvk/SLeeLa)** and her JavaFX UI/animation layer.
All game logic is SLeeLa source; the JavaFX layer is the presentation/animation
substrate.

## Versioning

This repository is organized into **numbered version folders** at the root —
`1/`, `2/`, `3/`, … Each folder is a **complete, self-contained snapshot** of
the game's source for that version: its SLeeLa Wrappers, imported `/sources`,
the Sleela UI, config, and build dispatcher all live inside the version folder.
Nothing outside the version folders is required to build or run a version.

To cut a new version, copy the latest version folder to the next number and
evolve it there; older versions remain frozen for reference and comparison.

| Version | Path | Contents |
|---|---|---|
| **1** | [`1/`](1/) | The current game: the tower game, the business/life layer (Character + Citizen), the win condition (> $240,000 in a Major Eastern Region, rising month-over-month), and the Author Path that wins in exactly 1001 moves. See [`1/README.md`](1/README.md). |

> The latest version is the highest-numbered folder. Start at
> [`1/README.md`](1/README.md) for the full game overview, architecture, and
> build/run instructions.

## Layout

```
AirportTycoon/
├── README.md        ← this file (the version index)
├── LICENSE
├── .gitignore
└── 1/               ← version 1 — full, self-contained source snapshot
    ├── README.md    ← version 1 overview + how to build/run
    ├── Makefile     ← version 1 build dispatcher
    ├── config/
    ├── game/        ← the .sleela Wrappers
    ├── sources/     ← imported SLeeLa library sources (character, citizen)
    └── ui/          ← the JavaFX Sleela UI
```

## Build & run a version

Each version is built from inside its own folder, e.g. for version 1:

```sh
cd 1
make check     # game-logic + economy + author-path tests (no JavaFX needed)
make ui-run    # launch the animated Sleela UI (Maven + JavaFX 21)
make author    # prove the Author Path wins in 1001 moves (needs SLeeLa toolchain)
```

See the per-version README (e.g. [`1/README.md`](1/README.md)) for the full
target list and toolchain notes.

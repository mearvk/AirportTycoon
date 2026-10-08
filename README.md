<img src="images/debian-vs-ubuntu.jpeg" alt="Debian vs Ubuntu" width="180">

# Airport Tycoon — Business Edition

A fun, fast-paced air-traffic game built to run on
**[SLeeLa](https://github.com/mearvk/SLeeLa)** and her JavaFX UI/animation layer.
All game logic is SLeeLa source; the JavaFX layer is the presentation/animation
substrate.

This repository holds two SLeeLa games side by side:

- **Airport Tycoon** — the versioned business/air-traffic game, in the numbered
  folders [`1/`](1/) through [`8/`](8/) (see the version index below).
- **Moria** — a deterministic dungeon crawler rendered through a native
  **SleelaUI** text pane, in [`moria/`](moria/). It is self-contained and
  developed on its own line; start at [`moria/README.md`](moria/README.md).

## Versioning

This repository is organized into **numbered version folders** at the root —
`1/`, `2/`, `3/`, `4/`, `5/`, `6/`, `7/`, `8/`, … Each folder is a **complete, self-contained snapshot** of
the game's source for that version: its SLeeLa Wrappers, imported `/sources`,
the Sleela UI, config, and build dispatcher all live inside the version folder.
Nothing outside the version folders is required to build or run a version.

To cut a new version, copy the latest version folder to the next number and
evolve it there; older versions remain frozen for reference and comparison.

Every Edition also carries a **superb name** drawn from a wealth of vocabulary,
recorded in a `VERSION.NAME.md` inside its folder.

| Version | Name | Epithet | Path | Contents |
|---|---|---|---|---|
| **1** | *Keystone Meridian* | *The Foundational Shift* | [`1/`](1/) | The base game: the tower game, the business/life layer (Character + Citizen), the win condition (> $240,000 in a Major Eastern Region, rising month-over-month), and the Author Path that wins in exactly 1001 moves. See [`1/README.md`](1/README.md) · [`1/VERSION.NAME.md`](1/VERSION.NAME.md). |
| **2** | *Golden Assimilation* | *The Assimilation Era* | [`2/`](2/) | The improvement line: the reputation-driven service premium, the on-time combo, and the Business Desk whose Level-5 **Great Assimilation** rakes in Major-Eastern money. See [`2/README.md`](2/README.md) · [`2/VERSION.NAME.md`](2/VERSION.NAME.md). |
| **3** | *Platinum Ascension* | *The Prosperity Era* | [`3/`](3/) | Edition 3 common base, carrying the Golden Assimilation foundation forward for its own evolution. See [`3/README.md`](3/README.md) · [`3/VERSION.NAME.md`](3/VERSION.NAME.md). |
| **4** | *Sovereign Skies* | *The Dominion Era* | [`4/`](4/) | Edition 4 common base, carrying the shared foundation forward for its own evolution. See [`4/README.md`](4/README.md) · [`4/VERSION.NAME.md`](4/VERSION.NAME.md). |
| **5** | *Imperial Horizons* | *The Grand Strategy Era* | [`5/`](5/) | Edition 5 common base, carrying the shared foundation forward for its own evolution. See [`5/README.md`](5/README.md) · [`5/VERSION.NAME.md`](5/VERSION.NAME.md). |
| **6** | *Grand Meridian* | *The Continental Era* | [`6/`](6/) | Edition 6 common base, carrying the shared foundation forward for its own evolution. See [`6/README.md`](6/README.md) · [`6/VERSION.NAME.md`](6/VERSION.NAME.md). |
| **7** | *Celestial Concourse* | *The Zenith Era* | [`7/`](7/) | Edition 7 common base, carrying the shared foundation forward for its own evolution. See [`7/README.md`](7/README.md) · [`7/VERSION.NAME.md`](7/VERSION.NAME.md). |
| **8** | *Apex Dominion* | *The Ultimate Era* | [`8/`](8/) | Edition 8 common base, carrying the shared foundation forward for its own evolution. See [`8/README.md`](8/README.md) · [`8/VERSION.NAME.md`](8/VERSION.NAME.md). |

> The latest version is the highest-numbered folder. Start at
> [`8/README.md`](8/README.md) for the current edition, or
> [`1/README.md`](1/README.md) for the original base game overview,
> architecture, and build/run instructions.


## Gameplay & Goals

### Airport Tycoon — what you are trying to do

You are the **airport tower and owner**. The core loop is simple: receive incoming aircraft, land them safely on available runways, move them to gates, service them, and send them back out before their patience expires.

The primary business goal is to build a healthy, growing airport:

1. **Keep aircraft moving.** Land inbound planes, assign gates, service them, and depart them promptly.
2. **Protect reputation.** A plane that runs out of patience and leaves angry hurts reputation. If reputation reaches **0**, the airport closes.
3. **Make money.** Successful departures produce fare revenue. The business/life layer tracks the owner's Character and the Citizens who travel through the airport.
4. **Reach the win condition.** Build **Major-Eastern revenue above $240,000 while that revenue is rising month-over-month**.
5. **Master the Author Path.** The repository also contains a deterministic **1001-move Author Path** that is guaranteed to win. It is available as a strategy/proof path rather than being required for ordinary play.

### Practical tower strategy

- Watch **patience** as closely as runway and gate capacity.
- Do not let planes sit idle when another aircraft needs the gate or runway.
- Prefer clean, on-time departures: in Edition 2, every **five consecutive clean departures** earns a **$150** combo tip.
- Edition 2 reputation directly affects fares: at reputation 60 the fare is neutral; higher reputation increases the service premium, while very low reputation forces discounts.
- Edition 2 **Auto-assist** prioritizes urgent aircraft while giving a small preference to heavier, higher-value aircraft when urgency is otherwise comparable.
- Edition 2's **Business Desk** runs alongside the tower game. Casino Management is higher variance; Investment Management is steadier and compounding. Sustained profitable weeks build IQ toward the **Level-5 Great Assimilation**, which temporarily boosts Major-Eastern revenue.

For exact mechanics of a particular edition, use that edition's README: [`1/README.md`](1/README.md) through [`8/README.md`](8/README.md). Edition 8 is the current versioned edition.

### Starting a game

For the current edition:

```sh
cd 8
make check
make ui-run
```

For a deterministic/headless run, use the version's `make run` target. The configured RNG seed makes the same seed replayable, which is useful for testing and learning the game.

### Fleet management, maintenance, and executive operations

Editions **1–8** now share a standard fleet-management layer alongside the route economy and pacing systems. Each edition performs a **one-time fleet roll** stored in its SLeeLa source/configuration so the starting management state is stable and reproducible.

- **Fleet size:** each edition manages a fleet within the standard **80–320 plane** range. The version-level target increases by **50 planes per edition level**, subject to the 320-plane operating cap.
- **Use rating:** every fleet receives a **0–100 use rating**. When the rating falls **below 85**, the management system flags the fleet for a **Maintenance Engineer** review.
- **Employees and results:** better employees improve operating results, but higher-quality staffing costs more. The management objective is greater long-term value rather than simply minimizing expense.
- **Global maintenance:** the shared registry covers **23 named global fleets**, individually scheduled aircraft, and **724 named game planets (PL-1 through PL-724)**. Maintenance intervals and costs are calculated per aircraft.
- **Executive management:** the **Fleet Management Desk** provides Text Area Input/Output controls for fleet size, use rating, employee quality, maintenance status, results, and operating cost. Adjustments can be discussed with **Top Brass** during operations or made during the **End-of-Day Review**.
- **Pacing:** the authoritative game-management clock runs at **8 Hz**, with a reference fleet event approximately every **22 seconds**. Game days are configurable from roughly **2–20 minutes**, with longer days permitted.
- **Route economics:** aircraft generate and consume airline money at the **Pickup, Flight, and Dropoff** stages. Route and fleet decisions therefore feed directly into the business result.

The management layer is deliberately an executive **decision-support system**: it presents conditions, costs, results, and recommended actions so the player can make the management decision rather than having the game irreversibly make personnel decisions on the player's behalf.

### Edition fleet-management rolls

The current one-time source/configuration rolls are:

| Edition | Rolled planes | Use rating | Engineer review? |
|---|---:|---:|---|
| **1** | 147 | 91 | No |
| **2** | 198 | 76 | Yes |
| **3** | 231 | 88 | No |
| **4** | 264 | 83 | Yes |
| **5** | 287 | 95 | No |
| **6** | 305 | 79 | Yes |
| **7** | 320 | 93 | No |
| **8** | 320 | 86 | No |

Edition 7 is intentionally stronger than Editions 1 and 3. Editions 7 and 8 reach the current 320-plane cap; the source records the roll so it does not silently change between runs.

### Reservations and airline growth

Reservations are part of the business-growth model: more successful reservations create more activity, more passengers, more relationships, and more opportunity for the airline. **Edition 7 uses 2,000 reservations per month as an exemplary management target**, giving the executive layer a concrete measure of airline scale and customer activity.

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
├── 2/               ← Airport Tycoon version 2 — Golden Assimilation
│   ├── README.md    ← version 2 overview + how to build/run
│   ├── Makefile     ← version 2 build dispatcher
│   ├── config/
│   ├── game/        ← the .sleela Wrappers
│   ├── sources/     ← imported SLeeLa library sources (character, citizen)
│   └── ui/          ← the JavaFX Sleela UI
├── 3/               ← Platinum Ascension — The Prosperity Era
├── 4/               ← Sovereign Skies — The Dominion Era
├── 5/               ← Imperial Horizons — The Grand Strategy Era
├── 6/               ← Grand Meridian — The Continental Era
├── 7/               ← Celestial Concourse — The Zenith Era
├── 8/               ← Apex Dominion — The Ultimate Era
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

Each version is built from inside its own folder, e.g. for the latest (version 8):

```sh
cd 8
make check
make ui-run
```

See the per-version README (e.g. [`8/README.md`](8/README.md)) for the full
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

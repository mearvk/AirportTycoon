# Airport Tycoon — Business Edition

A fun, fast-paced air-traffic game built to run on **[SLeeLa](https://github.com/mearvk/SLeeLa)**
and her JavaFX UI/animation layer. You are the tower: planes arrive on an
ever-shrinking timer, and you have a bounded number of **gates** and **runways**
to land them, service them, and send them off before they run out of patience.
Keep the airport profitable and your reputation up — hit zero reputation and the
airport closes.

```
            inbound ───▶ land (runway) ───▶ taxi (gate) ───▶ service ───▶ depart (runway) ───▶ $$$
                │                                                              ▲
                └────────────── patience runs out ───▶ plane leaves angry ──▶ reputation ▼
```

## How it's built — SLeeLa source + a Sleela UI

The game follows SLeeLa's recommended split (see SLeeLa's
[`gui/INTEGRATION.md`](https://github.com/mearvk/SLeeLa/blob/master/gui/INTEGRATION.md),
**Path 2 — SLeeLa intent → Java GUI**): **all game logic lives in `.sleela`
source**, and the JavaFX layer is purely a presentation/animation substrate.

```
  game/AirportTycoon.sleela      ← business logic: state, rules, the tick (SLeeLa Wrapper™)
          │  snapshot() / named intents
          ▼
  ui/ … SleelaRuntime            ← the intent boundary (named operations)
          │
          ▼
  JavaFX canvas + AnimationTimer ← window, sprites, smooth animation, HUD, controls
```

## Repository layout (root-organized)

| Path | What it is |
|------|------------|
| [`game/AirportTycoon.sleela`](game/AirportTycoon.sleela) | **The tower game.** Pure SLeeLa 1.6 Wrapper™ — owns every piece of state and the simulation `step()`. Also runs headless via its `main()` self-play demo. |
| [`game/AirportTycoonLife.sleela`](game/AirportTycoonLife.sleela) | **The business/life layer.** Connects the game to the SLeeLa economy: the airport owner is a `Character` with a `BusinessModel`; passengers are `Citizen`s who earn, are taxed, and buy tickets. Owns the **win condition**. |
| [`game/AuthorPath.sleela`](game/AuthorPath.sleela) | **The Author Path** — a guaranteed-winning strategy of exactly **1001 moves** that wins in every case. |
| [`game/AirportTycoonTest.sleela`](game/AirportTycoonTest.sleela) | A SLeeLa self-check Wrapper (happy path + abandonment). |
| [`sources/`](sources/) | **Imported SLeeLa library sources** (`character/` + `citizen/`), vendored verbatim from `mearvk/SLeeLa`. See [`sources/README.md`](sources/README.md). |
| [`ui/`](ui/) | The **Sleela UI** — a JavaFX 21 host (`AirportTycoonApp`) that renders the Wrapper's state, animates planes, and shows the live business/traveler economy panel. Maven project, mirroring SLeeLa's Audio GUI `pom`. |
| [`config/airport.conf`](config/airport.conf) | SLeeLa VM settings + gameplay tuning (economy, capacity, pace, RNG seed). |
| [`Makefile`](Makefile) | Top-level dispatcher (`game`, `run`, `run-life`, `test`, `ui`, `ui-run`, `check`). |

### Inside the SLeeLa Wrapper

Written to the normative SLeeLa 1.6 grammar
([`SLEELA.syntax`](https://github.com/mearvk/SLeeLa/blob/master/SLEELA.syntax)):

- **`struct Plane` / `Gate` / `Runway`** — syntax-1.2 named aggregates (reference
  types). Because **SLeeLa 1.6 has no array type**, collections are modelled as
  intrusive **singly-linked lists** of struct nodes (a `next` field), walked with
  `while` loops. This stays inside the grammar and respects the 4096-live-instance
  budget.
- **Deterministic PRNG** so a given `rng_seed` always plays the same shift
  (handy for replay and the test Wrapper).
- **`lock(0)` / `unlock(0)`** guard the shared field state in `step()` using
  SLeeLa's bounded threading built-ins.
- **Conducted methods** (`sysdepth()`, `degreemax()`, `role("Pipeline")`) are
  used for flavor and to honor the Constitution invariants a SLeeLa program runs
  under.
- **`structPack(plane)`** produces the per-plane wire form the UI parses each
  frame; the header line is pipe-delimited (`ATC|cash=…|rep=…`).

### The Sleela UI / animation

- `AirportTycoonApp` (JavaFX `Application`) drives a 10 Hz `AnimationTimer`:
  it calls the runtime's `step()`, reads `snapshot()`, **interpolates** each
  plane sprite toward its reported position for smooth motion, and redraws the
  canvas (sky, tarmac, runways with centrelines, gates, plane glyphs with
  per-plane patience bars that redden under pressure).
- `SleelaProcessRuntime` is the production boundary. It probes for the SLeeLa
  toolchain (`-Dsleela.home` / `SLEELA_HOME`); when present it drives the real
  Wrapper over the native bridge (`slcore_exchange`, Path 3), and when absent it
  transparently falls back to `LocalGameModel`.
- `LocalGameModel` is a **faithful Java mirror of the exact rules** in the
  `.sleela` Wrapper, so the game is playable and animated on any machine even
  before the SLeeLa compiler is installed — and a reviewer can diff the two to
  confirm they match.

## Both ends of the economy — Character & Citizen

Beyond the fast tower game, Airport Tycoon models the airport from **both ends**
using SLeeLa's economy Master Classes, imported into [`sources/`](sources/):

- **The owner's end — `Character`.** The airport operator is a SLeeLa
  `Character` who adopts one `BusinessModel` (here a `SubscriptionModel` for
  recurring gate leases). Each in-game month the owner realizes the model's
  profit, which moves their **cash, reputation, and grit** — a losing month
  really takes it out of you.
- **The traveler's end — `Citizen`.** Passengers are SLeeLa `Citizen`s: able,
  employed in an `Industry`, paid into a `BankAccount`, taxed by their
  `FederalReserveID` district, and — on a good month — willing to
  `treatYourself(...)` to a ticket. A Citizen only flies when `isAble()` and
  solvent.

The two ends balance: raise ticket prices or taxes and travelers stop flying;
neglect the business model and the owner goes under. The tower game's fare
income is folded onto the owner's book each month. In the Sleela UI this is the
right-hand **"The Business & The Travelers"** panel, with live ticket-price and
tax levers; `game/AirportTycoonLife.sleela` is the authoritative Wrapper and
`LifeEconomy` is its faithful Java mirror for the UI.

> **On "both ends of Adult Fantasy".** This is the mature business/life-sim
> layer — the game is played simultaneously from the proprietor's balance sheet
> and the traveler's wallet. It is a civic/economic simulation built directly on
> the shipped SLeeLa `Character` and `Citizen` classes.

## Winning & the Author Path

**A winning month** requires **all three** conditions, evaluated in
`AirportTycoonLife.winningMonth()`:

1. **More than $240,000** of revenue in a **Major Eastern Region**;
2. an **increase over the previous month's overall revenue**; and
3. an **increase over the previous month's Major-Eastern revenue**.

The **Major Eastern Region** is defined against the Federal Reserve districts in
the imported `FederalReserveID`: the major eastern money centers — **Boston,
New York, Philadelphia, Richmond, Atlanta**. Each traveler is tagged with a
district, and ticket revenue is attributed to its region; the tower game's fare
income lands at the airport's own Major-Eastern hub (New York) and so counts
toward both the Eastern and overall totals. The UI's economy panel shows the
East/total revenue, the previous month's figures, the win bar, and a
**`>> WINNING MONTH <<`** banner.

### The Author Path — wins in 1001 moves, every time

Because the game is **deterministic**, "the author knew the clues": the whole
future is knowable in advance. [`game/AuthorPath.sleela`](game/AuthorPath.sleela)
(mirrored by `AuthorPath.java`) encodes a guaranteed-winning strategy of
**exactly 1001 moves** that wins in **every case**:

- each in-game month the author directs a **strictly increasing** Eastern fare
  income that is always **above the $240,000 threshold**, so all three win
  clauses hold every month, monotonically, independent of the seed;
- the plan is padded with no-op **hold** moves so its length is **exactly 1001**.

`make author` runs the SLeeLa proof; the Java test suite verifies the path wins
in exactly 1001 moves across every starting state it sweeps.

## Controls

| Key / Button | Action |
|---|---|
| **A** / Auto-assist | Make the smart move for the most urgent plane |
| **Space** / Pause | Pause / resume the shift |
| **G** / Buy gate | Open the next gate (costs cash) |
| **R** / Buy runway | Open the next runway (costs cash) |
| Restart | Begin a fresh shift (resets the economy too) |
| Ticket ± / Tax ± (panel) | Set the traveler ticket price and civic tax rate between months |

## Build & run

```sh
# Game-logic unit tests (no JavaFX download needed):
make check

# Launch the animated Sleela UI (needs Maven + JavaFX 21):
make ui-run

# With the real SLeeLa toolchain installed, run the Wrapper headless:
make run                       # or: sleela run game/AirportTycoon.sleela
SLEELA_HOME=/path/to/SLeeLa make ui-run   # UI drives the real .sleela logic
```

> **Toolchain note.** SLeeLa's own compiler (`Sleelvac`) is required to compile
> and execute the `.sleela` Wrappers; `make game` / `make run` detect it and
> skip gracefully if it isn't on `PATH`. The JavaFX UI needs JDK 21 and the
> JavaFX 21 artifacts (pulled by Maven). The pure-Java `make check` target runs
> everywhere and validates the shared game rules.

## Tuning

Edit [`config/airport.conf`](config/airport.conf) to retune the economy,
capacity, pace, and RNG seed. The matching `static` fields at the top of
`AirportTycoon.sleela` are the authoritative defaults.

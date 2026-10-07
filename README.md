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
| [`game/AirportTycoon.sleela`](game/AirportTycoon.sleela) | **The game.** Pure SLeeLa 1.6 Wrapper™ — owns every piece of state and the simulation `step()`. Also runs headless via its `main()` self-play demo. |
| [`game/AirportTycoonTest.sleela`](game/AirportTycoonTest.sleela) | A SLeeLa self-check Wrapper (happy path + abandonment). |
| [`ui/`](ui/) | The **Sleela UI** — a JavaFX 21 host (`AirportTycoonApp`) that renders the Wrapper's state and animates it. Maven project, mirroring SLeeLa's Audio GUI `pom`. |
| [`config/airport.conf`](config/airport.conf) | SLeeLa VM settings + gameplay tuning (economy, capacity, pace, RNG seed). |
| [`Makefile`](Makefile) | Top-level dispatcher (`game`, `run`, `test`, `ui`, `ui-run`, `check`). |

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

## Controls

| Key / Button | Action |
|---|---|
| **A** / Auto-assist | Make the smart move for the most urgent plane |
| **Space** / Pause | Pause / resume the shift |
| **G** / Buy gate | Open the next gate (costs cash) |
| **R** / Buy runway | Open the next runway (costs cash) |
| Restart | Begin a fresh shift |

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

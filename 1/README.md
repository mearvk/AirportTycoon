<p align="right"><img src="../images/debian-vs-ubuntu.jpeg" alt="Debian vs Ubuntu" width="180"></p>

# Airport Tycoon — Business Edition (Edition 1) — *Keystone Meridian*

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
| [`ui-sleela/AirportTycoonUI.sleela`](ui-sleela/AirportTycoonUI.sleela) | **The native SleelaUI front-end.** Opens a real Slick Black window (X11/Cocoa/Win32) with SleelaUI widgets and presents the whole program at a glance — owner books, win status, travelers, and the Author Path proof. |
| [`terminal/AuthorTerminal.sleela`](terminal/AuthorTerminal.sleela) | **The Author Terminal.** A text-input terminal with basic OS functionality (via `lib/os`): it asks whether you're the Author, otherwise asks your Number, responds appropriately, then runs a small OS shell. Interactive host: `AuthorTerminal.java`. |
| [`media/EventsCenter.sleela`](media/EventsCenter.sleela) · [`media/Reporter.sleela`](media/Reporter.sleela) | **The media / events center** (the mid :: center area). A 300-rated events engine driving the focus routes (Asia + Euro-American), with 6 real price tiers and a soft economic pad; and the 220-rated **Reporter** (your Boss) who fuses the Author viewpoint with **East 5.0** (Asia). Java mirror: `MediaCenter.java`. |
| [`media/LightingEffects.sleela`](media/LightingEffects.sleela) · [`media/LightingLoader.sleela`](media/LightingLoader.sleela) | **Lighting Effects — a loadable module + loader.** Optional **light effects on your text/words** (glow, highlight, pulse, spotlight, dim) via true ANSI light and a GUI `[[fx:…]]` markup. The **Loader** finds the module, verifies the module contract, and only then enables lighting; its headline trick is to **see your Mayor's clues/excellents** — a `*`-starred word glows, an `!`-ended word is spotlit. Java mirror: `LightingEffects.java`; self-demo: `make lights`. |
| [`sources/`](sources/) | **Imported SLeeLa library sources** — `character/` + `citizen/` (the economy), `user-interface/` (the SleelaUI toolkit), and `os/` (host OS surface), vendored verbatim from `mearvk/SLeeLa`. See [`sources/README.md`](sources/README.md). |
| [`ui/`](ui/) | The **JavaFX Sleela UI** — a JavaFX 21 host (`AirportTycoonApp`) that renders the Wrapper's state, animates planes, and shows the live business/traveler economy panel. (A playable stand-in; the native front-end is `ui-sleela/`.) |
| [`config/airport.conf`](config/airport.conf) | SLeeLa VM settings + gameplay tuning (economy, capacity, pace, RNG seed). |
| [`Makefile`](Makefile) | Top-level dispatcher (`game`, `run`, `run-life`, `author`, `ui-sleela`, `test`, `ui`, `ui-run`, `check`). |

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

### The native SleelaUI front-end

[`ui-sleela/AirportTycoonUI.sleela`](ui-sleela/AirportTycoonUI.sleela) is the
program's **native** interface, built with **SleelaUI™** — SLeeLa's own
cross-platform widget toolkit ([`lib/user-interface`](https://github.com/mearvk/SLeeLa/tree/master/lib/user-interface),
vendored under [`sources/user-interface/`](sources/user-interface/)). It opens a
**real native window** through the `ui*` VM bridge (X11 on Linux, Cocoa on
macOS, Win32 on Windows) and paints the signature **Slick Black** look with the
toolkit's own rasterizer, identical on all three desktops.

The window presents the whole program readably, using the real widgets:

- an `SLHeading` title and a backend/version `SLLabel`;
- an `SLCard` with the **owner's books** — cash, plus reputation and grit as
  `SLLevelBar`s;
- an `SLInfoBar` showing the **win status** (Major-Eastern revenue above
  $240,000, rising);
- an `SLCard` **media center / route board** (the mid :: center area) — each
  Asia and Euro-American focus route's region `SLBadge`, luxury tier, fare, and
  on-time %, with the Boss's (Reporter, IQ 220) `SLInfoBar` headline;
- an `SLCard` of **travelers** (`SLLabel` + `SLBadge` home-district pills);
- an `SLCard` for the **Author Path**, with the one accent `SLButton`
  (`setSuggested(true)`) — *wins in 1001 moves, every time*;
- an `SLStatusBar` footer.

Run it with `make ui-sleela` (needs the SLeeLa toolchain). With no display it
falls back to a text read so the program still reports itself.

### Lighting Effects — a loadable module with a Loader

Edition 1.0 may now **use light effects** on your text/words. Lighting is an
optional, loadable **module** ([`media/LightingEffects.sleela`](media/LightingEffects.sleela))
that shares SLeeLa's common module contract (`load` / `loaded` / `moduleName` /
`describe`), exactly like `lib/gameplay/GameMove`. It is brought online by a
**Loader** ([`media/LightingLoader.sleela`](media/LightingLoader.sleela)) that
**finds the module, verifies the contract, and only then enables lighting** —
the same discovery/verify pattern the SLeeLa compiler, Loader, and Nordshrift
use for a package facade. If verification fails the Loader stays disabled and
passes text through unlit, so a missing or broken module can never crash the
program.

The module casts five light effects on a word — **glow**, **highlight**,
**pulse**, **spotlight**, and **dim** — each with a packed `0xRRGGBBAA` colour
(the same packing as [`SLColor`](sources/user-interface/SLColor.sleela)). On a
real terminal the effects are true **ANSI light** (bold, colour, blink,
reverse-video spotlight); a GUI can instead read the structured
`[[fx:name:#RRGGBB|word]]` markup and render the same glow.

Its headline feature: **you can see your Mayor's clues/excellents.** Pass a line
of text and the module lights the important words — a `*`-starred word (a
Mayor's clue) **glows**, and an `!`-ended word (an *excellent*) is **spotlit**:

```
plain : Mayor says: *land the heavies first, keep rep high. Excellent!
lit   : Mayor says: ⟨glow⟩*land⟨/⟩ the heavies first, keep rep high. ⟨spotlight⟩Excellent!⟨/⟩
```

Run the self-demo with **`make lights`** — it drives the real SLeeLa Loader when
the toolchain is present, and otherwise runs the faithful Java mirror
(`LightingEffects.java`), whose behaviour is locked in by the `make check`
test suite.

### The Author Terminal (text input + basic OS)

[`terminal/AuthorTerminal.sleela`](terminal/AuthorTerminal.sleela) is a
text-input terminal with **basic OS functionality**, built on SLeeLa's real
[`lib/os`](https://github.com/mearvk/SLeeLa/tree/master/lib/os) surface
(`osPlatform`, `osHostName`, `osUserName`, `osCurrentDir`, …), vendored under
[`sources/os/`](sources/os/). It:

1. greets you with live OS facts (platform, host, user, cwd);
2. asks **"Are you the Author? (yes/no)"**
   - **yes** → greets you as the Author who knew the clues and wins in **1001**
     moves, every time;
   - **otherwise** → asks **"What is your Number?"** and responds to it:
     - **1001** → the author's number — *"you are the Author after all"*;
     - **1–12** → a Federal Reserve District (1, 2, 3, 5, 6 are Major Eastern) —
       a civic traveler greeting;
     - anything else → a courteous default;
3. drops you into a small OS shell: `whoami`, `host`, `pwd`, `platform`, `pid`,
   `temp`, `author?`, `whoami?`, `help`, `exit`.

Because SLeeLa's surface has `print` but no stdin primitive (its `lib/io`
console classes are stubs), the Wrapper exposes a **pure decision core**
(`handleLine()`), and the interactive host [`AuthorTerminal.java`](ui/src/main/java/com/mearvk/sleela/airport/AuthorTerminal.java)
reads real `stdin` and prints what it returns — the same split used throughout
this project. Run the interactive terminal with **`make terminal-run`**, or the
SLeeLa self-demo with **`make terminal`**.

### The media / events center (the mid :: center area)

The center of the program is a background **events engine** rated at a **300**
model — the sharpest in the program ([`media/EventsCenter.sleela`](media/EventsCenter.sleela)).
Above it sits the **Reporter — your Boss** — rated at a **220** model
([`media/Reporter.sleela`](media/Reporter.sleela)), who interprets the center,
fusing **your ways and ideas (the Author viewpoint)** with **Asia, modelled as
`East 5.0`**. Java mirror for the UI/tests: `MediaCenter.java`.

**First Edition focus — real routes, real values:**

- **Asia routes** (the Great area the game focuses on): `Shanghai ↔ Tokyo`.
  East 5.0 leads the Boss's read here.
- **Euro-American routes** — Northern Europe + Chinese-influence hubs near the
  **UK** and **France**, feeding the **Americas (AM)**:
  `London → Santiago`, `Paris → Santiago`, `Shanghai → London`,
  `Shanghai → Paris`. The Author viewpoint leads the Boss's read here.

**The real model values:**

- **Countries, each contained by exactly 3 national theories** — an *On-Time
  Doctrine* (punctuality ceiling), a *Containment Theory* (delay damping), and a
  *Market Theory* (baseline luxury tier). China, Japan, the UK, France, and a
  Chile/Americas gateway.
- **Six real price tiers, 1.0 – 6.0** (6 = most luxurious): fares
  `$120 · $200 · $320 · $520 · $840 · $1360`.
- **Flights are mainly on time** (≈ 92–99% raw), then **measured and contained**
  by each origin country's theories — capped at its ceiling and damped toward it.
- **A soft economic pad** (35%) blends every figure gently toward its target, so
  prices, demand, and the headline fare index move calmly — the cushioning that
  keeps the First Edition enjoyable.

Run the self-demos with **`make media`**. In the native SleelaUI window the
center area shows a **live route board** — each focus route's region, luxury
tier, fare, and on-time % — with the **Boss's (Reporter) headline** below it.

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


## Timeless Route Profitability Model

Edition 1 now loads `game/RouteProfitabilityModel.sleela` as a common management model for routes and airline ownership. Planes and the airline are treated as the player's investment: normal routes begin from the assumption that aircraft operations are generally profitable, while the game asks the player to prove that a route is sound, secure, and sustainable.

Every completed flight has **three economic moments**:

1. **Pickup** — passenger demand and ticket value post revenue.
2. **Flight** — the aircraft's movement and route operation post the flight's economic value.
3. **Dropoff** — passenger completion and destination service post the final flight revenue.

The model keeps a running airline balance and route observations. Its management clock is **timeless**: the management intelligence observes its own decision horizon while also observing the game world and the global economy as separate frames of time. This lets the player invest in routes that look as strong as established routes without requiring every decision to be made on the tower's immediate clock.

The **MoreStandard intelligence** layer is intentionally exploratory. It can look beyond ordinary route profitability for additional conditions — demand, reliability, security, capacity, timing, economic climate, and other factors that make a system work. The model is therefore not just a fare calculator; it is an executive-level route-investment observer.

Run the standalone model with `make route-economy`.

The existing game remains authoritative; this model is an additional loaded observation/economic layer and does not make irreversible decisions for the player.


## Timeless Route Profitability Model

Edition 1 now loads `game/RouteProfitabilityModel.sleela` as a common management model for routes and airline ownership. Planes and the airline are treated as the player's investment: normal routes begin from the assumption that aircraft operations are generally profitable, while the game asks the player to prove that a route is sound, secure, and sustainable.

Every completed flight has **three economic moments**: **Pickup** posts passenger/ticket revenue; **Flight** posts the aircraft and route operating value; **Dropoff** posts the destination/service value. The airline balance updates at each of these three moments, rather than only once at the end of a flight.

The management model is **timeless**. It observes its own decision horizon while also observing the game world and the global economy as separate frames of time. This gives the player an executive-level investment view: establish that a new route can perform as well as existing routes, then invest when the evidence supports it.

The **MoreStandard intelligence** layer searches beyond ordinary route profitability for additional conditions that make the system work — demand, reliability, security, capacity, timing, economic climate, and other observable factors. It is an exploratory intelligence layer, not an automatic irreversible decision-maker.

Run the standalone loaded model with `make route-economy`.

## Pacing, Executive Desk, and Live Fleet

This edition uses `game/PacingEngine.sleela` at **8 Hz** for authoritative action iteration and full Game Engine/Logic appraisals. The reference airline has **223 planes** and receives a fleet-level Event Action about every **22 seconds**; supported active fleets range from **180 to 350 planes**. A game day is configurable from **2 to 20 minutes**, with longer days allowed and an **8-minute reference pace**.

The player can stay at the **Executive Desk** and receive live income/expense events and an end-of-day review, or switch to **Live Fleet** to observe aircraft operating in their route/resort theatre. `ui-sleela/AirportTycoonPacingUI.sleela` defines a GUI-friendly input/output Text Area contract for commands, live event streams, appraisals, money postings, and reviews. These are presentation views over the same simulation state.

Run the pacing model with `make pacing`.
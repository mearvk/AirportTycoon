# Airport Tycoon — Business Edition (Edition 4) — *Sovereign Skies*

> **Edition 4 — Sovereign Skies, the Dominion Era.
> **Edition 1 Base Concepts** (see [`../1/README.md`](../1/README.md)): the
> tower game, the business/life layer (`Character` + `Citizen`), the
> media/events center, the Author Terminal, the native SleelaUI front-end, the
> win condition, and the Author Path that wins in exactly 1001 moves — all
> carried forward intact.
>
> ### What's new in Edition 2 — the economic feedback loop
>
> Edition 1's fares were flat and reputation was mostly cosmetic. Edition 2
> makes a well-run tower *pay*, with three faithful changes applied identically
> in the SLeeLa Wrapper ([`game/AirportTycoon.sleela`](game/AirportTycoon.sleela))
> and its Java mirror (`ui/.../LocalGameModel.java`):
>
> 1. **Service premium.** Every fare is scaled by a reputation-driven premium:
>    `premium% = 100 + (reputation − 60)`, clamped to **[70%, 150%]**. At the
>    reference reputation (60) the premium is a neutral 100%, so Edition 1's
>    fares are the exact midpoint — a thriving airport (rep 100) now earns a
>    **+40%** tip on every departure, while a failing one (rep 0) is forced to
>    **−30%** discounts. Reputation finally hits the bottom line.
> 2. **On-time combo.** Every **5** consecutive clean departures pays a flat
>    **$150** combo tip; a single plane leaving angry resets the streak to zero.
>    This rewards sustained, careful tower management, not just volume.
> 3. **Smarter auto-assist.** The "A" / Auto-assist helper now ranks planes by a
>    priority score, `patience × 4 − sizeClass`, so when two planes are equally
>    close to timing out it protects the **heavier, higher-value** one — while a
>    genuinely more-urgent small plane still wins. The tie-break is sub-one
>    patience point, so urgency always dominates value.
>
> The snapshot wire format gains two backward-compatible fields (`streak`,
> `premium`) that the HUD now shows as **`FARE n%`** and **`STREAK n`**. All
> three changes are tunable in [`config/airport.conf`](config/airport.conf) under
> the *Edition 2* keys, and are locked in by new assertions in the game-logic
> test suite (`make check`).
>
> ### What's new in Edition 2 — the Business Desk
>
> Edition 2 adds a **Business Desk** — the place the player steps up to to make
> a **move** or **transaction** while the tower game runs
> ([`game/BusinessDesk.sleela`](game/BusinessDesk.sleela), mirrored by
> `ui/.../BusinessDesk.java`). The desk starts **empty** — a player who has only
> *tried on their genius* has opened nothing yet. From the desk you run **side
> games in parallel** with flying planes, modelled as **Chemistry**:
>
> - **Casino Management** — an **exothermic** reaction: high variance, a small
>   positive edge, big swings week to week.
> - **Investment Management** — a **titration** reaction: a steady, compounding
>   weekly yield with the occasional drawdown.
>
> Each venture settles a weekly transaction onto the owner's book and accrues
> **IQ** from sustained profitable weeks. When a venture reaches **IQ level 5**
> it fires a **Major Level-5 move: the Great Assimilation** — the *"IQ reordering
> of the Orient by assimilation of US capitalist interests."* For a run of weeks
> it multiplies **Major-Eastern** revenue **3×** — the stretch that *rakes in the
> money* — feeding straight into the East&nbsp;5.0 / Major-Eastern win economy
> (`applyAssimilation()` scales [`LifeEconomy`](#) revenue while it runs). The
> whole desk is **deterministic** from a seed, like the rest of the game.
>
> Run the self-demo with **`make desk`** (it falls back to the Java mirror when
> the SLeeLa toolchain is absent). The desk's knobs — the major level, the
> assimilation length, and its 3× Eastern multiplier — are tunable under the
> *Business Desk* keys in [`config/airport.conf`](config/airport.conf), and the
> mechanics are covered by new `make check` assertions. Everything else is
> inherited from Edition 1 verbatim.


### What's new in Edition 3 — Prosperity Contracts

Edition 3 adds a strategic operating layer on top of Edition 2's economic
feedback loop. The airport carries one deterministic **Prosperity Contract**
at a time: serve **5 aircraft of the displayed size class** before the
**240-tick** contract clock expires. Matching departures advance the contract;
all other departures remain fully profitable.

Completing a contract pays a **$500 prosperity bonus**, restores up to **2
reputation**, records the completion, and rotates the target **Small → Medium →
Heavy → Small**. Finish it with at least **120 ticks still remaining** and the
airport earns an additional **$250 quality bonus** and **+1 reputation**.
Missing the clock simply resets progress and rotates the target; there is **no
additional reputation penalty**. The rule therefore rewards planning and
prioritization without creating a second hidden game-over condition.

The SLeeLa Wrapper and Java mirror expose the same deterministic contract
readouts in the snapshot: `contractTarget`, `contractProgress`,
`contractGoal`, `contractLeft`, `contracts`, `contractEarlyWindow`, and `contractEarlyBonus`. Edition 2's service
premium, on-time combo, smarter auto-assist, Business Desk, deterministic seed,
and Author Path remain intact.

### What's new in Edition 4 — Prosperity Ladder

Edition 4 carries Edition 3's Prosperity Contracts forward and turns repeated
success into a **Prosperity Ladder**. A completed contract advances a
consecutive-completion chain. The next contract earns an additional **$100 per
completed contract in the chain**, capped at **+$400** on top of the $500 base.
The Edition 3 early-quality bonus remains available. Letting a contract expire
breaks the ladder back to zero, while ordinary fares and the airport's core
reputation rules remain unchanged.

The snapshot exposes `contractChain`, `contractLadderStep`, and
`contractLadderCap`, so the UI can make the escalating reward visible. Edition
4 rewards not only solving the current traffic problem, but building a sustained
operating record without turning the ladder into a second mandatory win condition.

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
| [`game/BusinessDesk.sleela`](game/BusinessDesk.sleela) | **The Business Desk (Edition 2 foundation carried into Edition 3).** Where the player makes a move/transaction while flying planes. Starts empty; opens parallel **Casino** and **Investment** side games (modelled as Chemistry), accrues **IQ**, and fires the **Level-5 Great Assimilation** that rakes in Major-Eastern money. Java mirror: `BusinessDesk.java`; self-demo: `make desk`. |
| [`game/AirportTycoonTest.sleela`](game/AirportTycoonTest.sleela) | A SLeeLa self-check Wrapper (happy path + abandonment). |
| [`ui-sleela/AirportTycoonUI.sleela`](ui-sleela/AirportTycoonUI.sleela) | **The native SleelaUI front-end.** Opens a real Slick Black window (X11/Cocoa/Win32) with SleelaUI widgets and presents the whole program at a glance — owner books, win status, travelers, and the Author Path proof. |
| [`terminal/AuthorTerminal.sleela`](terminal/AuthorTerminal.sleela) | **The Author Terminal.** A text-input terminal with basic OS functionality (via `lib/os`): it asks whether you're the Author, otherwise asks your Number, responds appropriately, then runs a small OS shell. Interactive host: `AuthorTerminal.java`. |
| [`media/EventsCenter.sleela`](media/EventsCenter.sleela) · [`media/Reporter.sleela`](media/Reporter.sleela) | **The media / events center** (the mid :: center area). A 300-rated events engine driving the focus routes (Asia + Euro-American), with 6 real price tiers and a soft economic pad; and the 220-rated **Reporter** (your Boss) who fuses the Author viewpoint with **East 5.0** (Asia). Java mirror: `MediaCenter.java`. |
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

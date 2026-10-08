<p align="right"><img src="../../images/debian-vs-ubuntu.jpeg" alt="Debian vs Ubuntu" width="180"></p>

# Imported SLeeLa sources (`/sources`)

Vendored SLeeLa standard-library packages that Airport Tycoon builds on. These
files are **imported verbatim** from
[`mearvk/SLeeLa`](https://github.com/mearvk/SLeeLa) (branch `master`) and are
byte-for-byte identical to upstream (verified by git blob SHA at import time).
They are kept here so the game resolves its `Character`, `Citizen`, and SleelaUI
widget vocabulary without requiring a full SLeeLa checkout on the build path.

| Local path | Upstream path |
|---|---|
| `sources/character/` | `lib/character/` |
| `sources/citizen/` | `lib/citizen/` |
| `sources/gameplay/` | `lib/gameplay/` |
| `sources/user-interface/` | `lib/user-interface/` |
| `sources/os/` | `lib/os/` |

## `user-interface/` — the SleelaUI widget toolkit

The SLeeLa-source surface of **SleelaUI™**, SLeeLa's own cross-platform native
UI toolkit (not GTK/Qt). Opening a window and adding widgets bottoms out in the
`ui*` VM built-ins, which call the genuine SleelaUI C ABI and drive the real
host window system (X11 / Cocoa / Win32). The front-end
[`ui-sleela/AirportTycoonUI.sleela`](../ui-sleela/AirportTycoonUI.sleela) builds
on these classes:

- `SLUserInterface` (open the app, report backend, run the loop), `SLWindow`,
  `SLWidget` (the base: margins, alignment, accents).
- Containers/content: `SLBox`, `SLCard`, `SLGrid`, `SLHeaderBar`, `SLStatusBar`,
  `SLSeparator`, `SLHeading`, `SLLabel`, `SLButton`, `SLBadge`, `SLInfoBar`,
  `SLLevelBar`, `SLProgressBar`, and the rest of the catalogue.
- Theming: `SLTheme` (`slickBlack()` + role colours, radius, font) and `SLColor`.

## `os/` — the host operating-system surface

The SLeeLa-source surface of `lib/os`: a single portable API over the real host
OS (Windows / Linux / macOS). Calls bottom out in the `os*` VM built-ins
(`osPlatform`, `osHostName`, `osUserName`, `osCurrentDir`, `osGetEnv`, `osRun`,
`osSpawn`, …). The **Author Terminal** ([`terminal/AuthorTerminal.sleela`](../terminal/AuthorTerminal.sleela))
uses these for its basic OS functionality. Classes: `SLOperatingSystem`,
`SLEnvironment`, `SLProcess`, `SLFileSystem`, `SLFile`, `SLDirectory`, `SLPath`,
`SLPermissions`, `SLClock`, `SLEventSignal`, and the per-OS flavors
`SLLinuxOS` / `SLMacOS` / `SLWindowsOS`.

## `character/` — the business-owner vocabulary

A `Character` is a named actor in the Sleela economy who adopts exactly one
`BusinessModel` and lives or dies by its monthly profit. The package is the
classic base/subclass polymorphism pattern:

- `Character.sleela` — the Master Class (name, grit, reputation, cash, a
  base-typed `BusinessModel model`).
- `BusinessModel.sleela` — the base contract (`monthlyRevenue()`/`monthlyCost()`
  → `monthlyProfit()`), with `KIND_*` archetypes.
- Twelve concrete models: `SubscriptionModel`, `FreemiumModel`,
  `MarketplaceModel`, `AdvertisingModel`, `RetailModel`, `WholesaleModel`,
  `FranchiseModel`, `LicensingModel`, `SaaSModel`, `ConsultingModel`,
  `ManufacturingModel`, `BrokerageModel`.
- `SLPackage.sleela` — the package facade.

## `citizen/` — the person/traveler vocabulary

A `Citizen` is the civic Master Class: one able, taxpaying person with a life
cycle (born → working → retired), composing three sidecars:

- `Citizen.sleela` — stages, mood, and the `isAble()` contract.
- `Industry.sleela` — employment sector + wage.
- `BankAccount.sleela` — balance in cents, deposits/withdrawals, Fed district.
- `FederalReserveID.sleela` — synthetic clearing ID + the twelve Fed districts.
- `SLPackage.sleela` — the package facade.

## `gameplay/` — the three-move turn system

A player takes a turn and makes exactly one of three moves, each resolved by
**deterministic, seeded rolls** so a replayed game (luck included) repeats. The
package is the same base/subclass polymorphism pattern as `character/`:

- `DiceRoll.sleela` — a seeded MINSTD roller: `rollUnder(pct)`, `coinFlip()`,
  `between(lo,hi)` — the 80/20 and 50/50 behind every move.
- `Player.sleela` — experience, a Person/Citizen `quality` score (which sets the
  price of information), cash, boss understanding, and pass-throughs to the
  courtroom.
- `ManagementCourtroom.sleela` — banked `file` / `tact` / `rolls` and a derived
  `standing`.
- `MoveOutcome.sleela` — the result record a move fills and `applyTo(Player)`.
- `GameMove.sleela` — the base move contract (`resolve(Player) → MoveOutcome`),
  with `CHOICE_*` for the three moves.
- Three concrete moves: `HelpIndexMove` (a — create a help index/friend: an 80%
  roll then a 20% roll, with the better index + boss understanding as live
  subevents), `AskForHelpMove` (b — ask for help/new routes: a 50/50 that either
  shows your `ExpectedFuture` or buys the boss's `DetailedReport`), and
  `NewDesireMove` (c — create a new desire: review `PortfolioDecision`s, gain
  courtroom standing, and back a bet for profit + a futures read).
- Move (b) brain: `InferenceEngine.sleela` → `ExpectedFuture.sleela` (the
  on-schedule projection on a winning 50/50) and `DetailedReport.sleela` (the
  boss's pre-decided Game-Engineering moves on a losing 50/50, priced by player
  quality).
- Move (c) substance: `PortfolioDecision.sleela` — a medium bet on a realistic
  industry; a found **BIG** bet always wins (bet big where you find it).
- `GameTurn.sleela` — the orchestrator (bind → resolve → apply, per turn).
- `SLPackage.sleela` — the package facade.

## How the game uses them

`game/AirportTycoonLife.sleela` is the integration Wrapper. It models the airport
across **both ends of the economy**:

- **Owner end** — the airport operator is a `Character` who adopts a
  `BusinessModel` (gate leases as a `SubscriptionModel`); each in-game month the
  Character realizes the model's profit, moving its cash, grit, and reputation.
- **Traveler end** — passengers are `Citizen`s who are employed in an
  `Industry`, get paid into a `BankAccount`, pay taxes, and `treatYourself(...)`
  on a ticket. A citizen only flies when `isAble()` and solvent.

> **Fidelity note.** These sources are `#sleela 1.3` and use `extends`
> (inheritance) and `static final`, which the single-class `#sleela 1.6` game
> Wrapper does not. They are resolved as separate one-class-per-file library
> units by the SLeeLa compiler's library Index, exactly as upstream intends —
> they are not merged into the game's single class.

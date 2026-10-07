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
| `sources/user-interface/` | `lib/user-interface/` |

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

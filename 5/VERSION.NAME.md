<img src="../images/debian-vs-ubuntu.jpeg" alt="Debian vs Ubuntu" width="180">

# Edition 5 — *Imperial Horizons*

> **Version:** 5
> **Name:** Imperial Horizons
> **Epithet:** *The Grand Strategy Era*
> **Status:** Latest — the grand strategy line, evolving from the Edition 4 dominion line.

## Why the name

**Imperial Horizons** — Edition 5 expands the airport's strategy beyond the immediate traffic horizon. The player must balance aircraft flow, contract prosperity, and the capital needed to keep the airport resilient.

The **Imperial Reserve** rewards maintaining at least $3,000 for 180 consecutive ticks with a $750 treasury award and up to +2 reputation. Falling below the floor resets only qualification progress; it never closes the airport.

> *Imperial Horizons* — the Edition where prosperity becomes long-range strategy.

## What's new since *Sovereign Skies* (Edition 4)

- **Imperial Reserve** — cash-stability objective alongside the Prosperity Ladder.
- **$3,000 reserve floor** — capital must remain at or above the floor.
- **180-tick qualification** — stability must be maintained continuously.
- **$750 treasury award** — successful qualification adds working capital.
- **Up to +2 reputation** — financial discipline reinforces airport quality.
- **Reset without punishment** — spending below the floor resets progress without a new failure condition.
- **UI-visible finance state** — reserve floor, progress, goal, reward, and award count are in snapshots.
- **1–4 player multiplayer** — a dependency-free authoritative TCP server supports a shared airport or private airports.
- **Global player chat** — shared chat works in both multiplayer modes.

Editions 1–4 remain intact: tower operations, economic feedback, Prosperity Contracts, the early-quality bonus, and the Prosperity Ladder all continue forward.

The multiplayer layer is designed for trusted LAN/private networks in this edition; it does not yet provide internet-grade authentication or TLS.

See README.md for the full overview and Edition 5 notes.

- **CCP/1 client communications** — presence, peer discovery, broadcast/direct messages, ACKs, and heartbeats are relayed through the authoritative server.

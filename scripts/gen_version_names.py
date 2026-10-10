#!/usr/bin/env python3
"""Rewrite each edition's VERSION.NAME.md to describe its real, implemented
gimmick on the unified 1->8 mastery ladder, plus the shared phosphor look.

Each file is regenerated so the narrative matches EditionGimmicks exactly:
Edition N carries systems 1..N and introduces exactly one new one.
"""
import pathlib

ROOT = pathlib.Path(__file__).resolve().parent.parent

META = [
    ("Keystone Meridian", "The Foundational Shift"),
    ("Golden Assimilation", "The Assimilation Era"),
    ("Platinum Ascension", "The Prosperity Era"),
    ("Sovereign Skies", "The Dominion Era"),
    ("Imperial Horizons", "The Grand Strategy Era"),
    ("Grand Meridian", "The Continental Era"),
    ("Celestial Concourse", "The Zenith Era"),
    ("Apex Dominion", "The Ultimate Era"),
]

# system: (title, mastery, one-paragraph "why", bullet list of what's new)
SYSTEMS = [
    ("Tower Operations", "Operate",
     "The base game: land inbound aircraft, assign gates, service them, and "
     "depart them before their patience runs out. Reputation gates survival; "
     "cash gates growth.",
     ["The land → gate → service → depart loop",
      "Patience, reputation and cash as the three core signals",
      "Deterministic PRNG so a seed replays identically",
      "The unified green-phosphor look every later edition inherits"]),
    ("Service Premium & On-Time Combo", "Reward quality",
     "Quality gets a legible price. Reputation now scales every fare through a "
     "service premium, and five clean departures in a row pay a combo tip.",
     ["Reputation-driven service premium (70%–150% of base fare)",
      "On-time combo: a flat tip every five clean departures",
      "Value-aware auto-assist that protects heavies on a near-tie",
      "The Business Desk side ventures and the Level-5 Great Assimilation"]),
    ("Weather & Runway Conditions", "Read the environment",
     "The sky becomes a system to read. Fronts drift across the field and ice "
     "the runways; you must de-ice the right runway and land into the gap.",
     ["Clear → Front → Storm weather phases on a deterministic cadence",
      "Storms ice every runway; iced runways refuse landings",
      "A de-ice action the player (or a later policy) drives",
      "Auto-assist de-ices a blocking runway for the most urgent inbound"]),
    ("Fleet Health & Maintenance", "Sustain the machine",
     "The machine must be sustained. A fleet-wide use-rating decays as aircraft "
     "cycle; below 85 the Maintenance Engineer flags it and fares are reduced "
     "until you service it.",
     ["A 0–100 fleet use-rating that wears with departures",
      "A sub-85 engineer-review flag that cuts fares to 75%",
      "A scheduled-maintenance action whose cost grows with neglect",
      "Full fares restored the moment the fleet is serviced"]),
    ("Route Network & Reservations", "Plan ahead",
     "Arrivals become a plan rather than a surprise. You book routes and "
     "reservations ahead; a full book smooths traffic and pays a steady bonus, "
     "an empty one starves the gates.",
     ["A forward reservation book with a full-book target of 20",
      "A steady per-departure bonus while the book is well-stocked",
      "Departures draw the book down and count as fulfilled",
      "Booking you drive directly (or delegate in Edition 7)"]),
    ("Multi-Terminal Expansion", "Scale out",
     "The hub scales out across the map. You unlock continental terminals and "
     "balance load across them; the hub rewards keeping them level rather than "
     "overloading one.",
     ["Up to four continental terminals you unlock in turn",
      "Departures routed to the least-loaded open terminal",
      "An imbalance readout that rewards level operations",
      "Each terminal behaves as its own small airport"]),
    ("Automation & Policy", "Delegate control",
     "You stop pressing AUTO and start programming it. A configurable policy "
     "lets the tower run landing, de-icing, maintenance and booking on its own "
     "while you supervise the exceptions.",
     ["A four-switch policy: auto-land, auto-de-ice, auto-maintain, auto-book",
      "The tower executes the policy every tick",
      "A delegated-count readout of how much is on autopilot",
      "Delegation of every earlier system, not just per-plane clicks"]),
    ("Live Competitive Economy", "Compete",
     "Everything runs at once and the result is published. One authoritative "
     "live score, derived from the whole stack, goes to the server so you can "
     "compete. Mastery is holding all eight systems green.",
     ["A read-only live score derived from cash, reputation, served and streak",
      "A published LIVE line for a competitive server to broadcast",
      "Every earlier system — weather, fleet, network, terminals, policy — live at once",
      "The culmination of the ordered 1→8 mastery ladder"]),
]


def doc(n: int) -> str:
    codename, epithet = META[n - 1]
    title, mastery, why, news = SYSTEMS[n - 1]
    status = ("Latest — the current edition, holding all eight systems."
              if n == 8 else
              f"A complete snapshot carrying systems 1..{n} of the mastery ladder.")
    carried = ""
    if n > 1:
        prev_titles = ", ".join(SYSTEMS[i][0] for i in range(n - 1))
        carried = (
            f"\n## Carried forward from Edition {n - 1}\n\n"
            f"Every earlier system remains part of Edition {n}: {prev_titles}. "
            f"Edition {n} adds exactly one new system on top, so the player now "
            f"holds **{n}** systems at once.\n")
    news_md = "\n".join(f"- {b}" for b in news)
    base_or_new = "The base system" if n == 1 else f"New in Edition {n}"
    return f"""<p align="right"><img src="../images/debian-vs-ubuntu.jpeg" alt="Debian vs Ubuntu" width="180"></p>

# Edition {n} — *{codename}*

> **Version:** {n}
> **Name:** {codename}
> **Epithet:** *{epithet}*
> **Mastery theme:** {mastery}
> **Status:** {status}

## The mastery ladder

Airport Tycoon is one game played at eight depths. Edition **{n}** sits at depth
**{n}** of the ordered 1→8 ladder and introduces **{title}** (*{mastery}*). Each
edition keeps every earlier system and lights exactly one new one, so interest
multiplies as you climb. The whole series shares one **green-phosphor** look —
a monochrome 8-bit CRT palette with orthogonal, grid-aligned objects — so 1→8
read as a single machine; only the number of lit systems changes.

## {base_or_new} — {title}

{why}

## What this edition adds

{news_md}
{carried}
See [README.md](README.md) for the full overview and build/run instructions, and
the repository README for the complete 1→8 ladder.
"""


def main():
    for n in range(1, 9):
        (ROOT / str(n) / "VERSION.NAME.md").write_text(doc(n))
        print(f"ed {n}: VERSION.NAME.md rewritten")
    print("done")


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""Regenerate each edition's SleelaUI front-end (AirportTycoonUI.sleela) with
the unified green-phosphor theme and that edition's own ordered mastery ladder.

Edition N shows systems 1..N, with the Nth marked NEW THIS EDITION, mirroring
EditionGimmicks. The whole series shares one phosphor palette so 1..8 look like
one machine; only the number of lit ladder rows changes.
"""
import pathlib

ROOT = pathlib.Path(__file__).resolve().parent.parent

CODENAMES = [
    "Keystone Meridian", "Golden Assimilation", "Platinum Ascension",
    "Sovereign Skies", "Imperial Horizons", "Grand Meridian",
    "Celestial Concourse", "Apex Dominion",
]

# (title, mastery verb) in ladder order; index 0 == system introduced at ed 1.
SYSTEMS = [
    ("Tower Operations", "Operate"),
    ("Service Premium & On-Time Combo", "Reward quality"),
    ("Weather & Runway Conditions", "Read the environment"),
    ("Fleet Health & Maintenance", "Sustain the machine"),
    ("Route Network & Reservations", "Plan ahead"),
    ("Multi-Terminal Expansion", "Scale out"),
    ("Automation & Policy", "Delegate control"),
    ("Live Competitive Economy", "Compete"),
]


def ladder_builder(edition: int) -> str:
    """The buildMasteryLadder() method body for this edition."""
    codename = CODENAMES[edition - 1]
    rows = []
    # carried + base systems (all but the newest) rendered as ladder rows
    for i in range(edition - 1):
        title, mastery = SYSTEMS[i]
        rows.append(
            f'        ladderRow(box, "{i + 1}", "{title}", "{mastery}");')
    rows_block = "\n".join(rows)
    new_title, new_mastery = SYSTEMS[edition - 1]
    if edition == 1:
        # Edition 1 has only the base system; present it as the new/only one.
        body_rows = ""
        new_bar = (
            f'        SLInfoBar top = new SLInfoBar();\n'
            f'        top.info(box, "THE BASE GAME — 1. {new_title} '
            f'({new_mastery}): land, gate, service and depart planes before '
            f'patience runs out. Every later edition builds on this loop.");'
        )
    else:
        body_rows = rows_block + "\n\n"
        new_bar = (
            f'        SLInfoBar top = new SLInfoBar();\n'
            f'        top.info(box, "NEW THIS EDITION — {edition}. {new_title} '
            f'({new_mastery}). It layers on top of every system above, so you '
            f'now hold {edition} systems at once.");'
        )
    return (
        "    // The ordered 1->8 mastery ladder for this edition: Edition "
        f"{edition}\n"
        f"    // holds systems 1..{edition}, each layered on the last, newest marked NEW.\n"
        "    // Mirrors EditionGimmicks (ui/.../EditionGimmicks.java).\n"
        "    void buildMasteryLadder(SLBox parent) {\n"
        "        SLCard card = new SLCard();\n"
        "        card.createIn(parent);\n"
        "        SLBox box = new SLBox();\n"
        "        box.vertical(card, 6);\n"
        "        box.setMarginAll(14);\n\n"
        "        SLHeading h = new SLHeading();\n"
        f'        h.createIn(box, "Mastery Ladder — Edition {edition} ({codename})", 16);\n\n'
        f"{body_rows}"
        f"{new_bar}\n"
        "    }\n\n"
        "    // One ladder row: level number badge + system title + mastery verb.\n"
        "    void ladderRow(SLBox parent, String level, String title, String mastery) {\n"
        "        SLBox row = new SLBox();\n"
        "        row.horizontal(parent, 10);\n\n"
        "        SLBadge n = new SLBadge();\n"
        "        n.createIn(row, level);\n\n"
        "        SLLabel t = new SLLabel();\n"
        "        t.createIn(row, title);\n\n"
        "        SLLabel m = new SLLabel();\n"
        '        m.createIn(row, "— " + mastery);\n'
        "    }\n\n"
    )


def text_read_block(edition: int) -> str:
    lines = []
    lines.append('        print("Airport Tycoon — Business Edition (SleelaUI, headless)");')
    lines.append(
        f'        print("Mastery Ladder (Edition {edition}, {CODENAMES[edition - 1]}):");')
    for i in range(edition):
        title, mastery = SYSTEMS[i]
        tag = "  (NEW)" if i == edition - 1 and edition > 1 else ""
        label = f"  {i + 1} {title}"
        lines.append(f'        print("{label[:40]:<40}— {mastery}{tag}");')
    lines.append(
        '        print("Owner: Avery Sloane (Subscription) — cash climbing, profitable.");')
    return "\n".join(lines)


def main():
    canon = (ROOT / "8" / "ui-sleela" / "AirportTycoonUI.sleela").read_text()
    for n in range(1, 9):
        text = canon

        # 1) Replace the ladder builder + ladderRow methods block.
        start = text.index("    // The ordered 1->8 mastery ladder")
        end = text.index("    // The owner's business at a glance:")
        text = text[:start] + ladder_builder(n) + text[end:]

        # 2) Replace the headless text-read ladder block.
        tstart = text.index(
            '        print("Airport Tycoon — Business Edition (SleelaUI, headless)");')
        tend = text.index(
            '        print("WIN: Major Eastern revenue')
        text = text[:tstart] + text_read_block(n) + "\n" + text[tend:]

        # 3) Fix the window title / headings that name edition 8.
        text = text.replace(
            'ui.openWindow("Airport Tycoon — Business Edition"',
            f'ui.openWindow("Airport Tycoon — Edition {n} ({CODENAMES[n - 1]})"')

        (ROOT / str(n) / "ui-sleela" / "AirportTycoonUI.sleela").write_text(text)
        print(f"ed {n}: SleelaUI regenerated ({n} ladder rows)")
    print("done")


if __name__ == "__main__":
    main()

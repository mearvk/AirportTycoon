#!/usr/bin/env python3
"""Second-pass corrections so every edition builds against the canonical,
edition-parameterized gameplay code.

Context: editions 3-7 did not build even before this work (divergent
GameSnapshot, a corrupted ed7 SleelaRuntime, and broken ed5/6 multiplayer
files). The canonical approach makes the gameplay edition-agnostic, so we also
make the small edition-agnostic support files canonical and give editions that
carry a BusinessDesk (2-8) the canonical test.
"""
import pathlib

ROOT = pathlib.Path(__file__).resolve().parent.parent
SRC = "ui/src/main/java/com/mearvk/sleela/airport"
TST = "ui/src/test/java/com/mearvk/sleela/airport"
CANON = ROOT / "8"

# Edition-agnostic support files that must be identical everywhere.
AGNOSTIC = [
    f"{SRC}/GameSnapshot.java",
    f"{SRC}/SleelaRuntime.java",
]

# Editions that carry a BusinessDesk can use the canonical ed8 test verbatim.
HAVE_BUSINESS_DESK = [2, 3, 4, 5, 6, 7]

canon_test = (CANON / TST / "LocalGameModelTest.java").read_text()
agnostic_contents = {rel: (CANON / rel).read_text() for rel in AGNOSTIC}

for n in range(1, 8):
    edir = ROOT / str(n)
    for rel, content in agnostic_contents.items():
        (edir / rel).write_text(content)
    if n in HAVE_BUSINESS_DESK:
        (edir / TST / "LocalGameModelTest.java").write_text(canon_test)
    print(f"ed {n}: agnostic files synced"
          + ("; canonical test installed" if n in HAVE_BUSINESS_DESK else "; kept own test"))
print("done")

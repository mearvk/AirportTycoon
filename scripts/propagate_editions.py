#!/usr/bin/env python3
"""Propagate the unified phosphor UI + ordered 1->8 mastery ladder to every
edition folder.

Edition 8 is the canonical, edition-parameterized implementation. The gameplay
is driven by a single edition number (EditionConfig.EDITION), so the big shared
Java files are byte-identical across editions and only EditionConfig changes.
Each edition's existing test file keeps its own coverage; this script appends
the shared ladder/gimmick tests (which never reference edition-gated classes
like BusinessDesk) and registers them.
"""
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
SRC = "ui/src/main/java/com/mearvk/sleela/airport"
TST = "ui/src/test/java/com/mearvk/sleela/airport"
CANON = ROOT / "8"

# Files copied verbatim from edition 8 to every edition (edition-agnostic).
SHARED = [
    f"{SRC}/Phosphor.java",
    f"{SRC}/EditionGimmicks.java",
    f"{SRC}/GimmickSystems.java",
    f"{SRC}/LocalGameModel.java",
    f"{SRC}/AirportTycoonApp.java",
]

# The ladder test registration calls (added to each test's main()).
REGISTER = """        // --- The ordered 1->8 mastery ladder (shared across editions) ---
        testLadderIsCumulative();
        testEachEditionAddsExactlyOneSystem();
        testEditionBoundsRejected();
        testPhosphorPaletteIsOrderedAndMonochrome();
        testPhosphorHealthTintFadesToAmber();
        testWeatherGatesLandingUntilDeIced();
        testFleetReviewReducesFaresUntilServiced();
        testNetworkBookPaysSteadyBonus();
        testTerminalsBalanceLoad();
        testAutomationDelegatesPolicies();
        testLiveScoreMonotoneInGoodState();
        testEditionOneHasOnlyTheTowerSystem();
        testEditionEightRunsEverySystem();
        testSystemsStatusListsLiveSystems();
        testHigherEditionsStayDeterministic();
"""


def extract_ladder_methods(canon_test: str) -> str:
    """Grab the ladder test method block from the edition-8 test file."""
    start = canon_test.index("    private static void testLadderIsCumulative()")
    # The block runs to the final 'same edition+seed => same cash' assertion's
    # closing brace, just before the class-closing brace.
    end_marker = 'check(a.cash() == b.cash(), "same edition+seed => same cash");'
    end = canon_test.index(end_marker)
    # find the method-closing brace after end_marker
    close = canon_test.index("\n    }\n", end) + len("\n    }\n")
    return canon_test[start:close]


def edition_config(n: int) -> str:
    return f"""package com.mearvk.sleela.airport;

/**
 * EditionConfig &mdash; the one place that records which edition this folder is.
 *
 * <p>The gameplay code ({{@link LocalGameModel}}, {{@link AirportTycoonApp}}) is
 * edition-agnostic: the ordered 1&rarr;8 mastery ladder ({{@link EditionGimmicks}})
 * is driven entirely by this single edition number, so the <em>same</em> Java is
 * copied into every numbered edition folder and only this constant changes.
 * Edition&nbsp;N therefore lights exactly systems&nbsp;1..N from identical code.
 *
 * <p>Override at launch with {{@code -Dat.edition=N}} to compare editions.
 */
public final class EditionConfig {{

    private EditionConfig() {{
    }}

    /** This folder's edition (1..8). */
    public static final int EDITION = {n};
}}
"""


def main():
    canon_test_path = CANON / TST / "LocalGameModelTest.java"
    canon_test = canon_test_path.read_text()
    ladder_methods = extract_ladder_methods(canon_test)

    shared_contents = {}
    for rel in SHARED:
        shared_contents[rel] = (CANON / rel).read_text()

    for n in range(1, 8):  # editions 1..7 (8 is the canonical source)
        edir = ROOT / str(n)
        if not edir.is_dir():
            print(f"skip: edition {n} missing")
            continue

        # 1) copy shared edition-agnostic files
        for rel, content in shared_contents.items():
            (edir / rel).write_text(content)

        # 2) write per-edition EditionConfig
        (edir / SRC / "EditionConfig.java").write_text(edition_config(n))

        # 3) append ladder tests + register them in this edition's test
        tpath = edir / TST / "LocalGameModelTest.java"
        text = tpath.read_text()
        if "testLadderIsCumulative" not in text:
            # register calls: insert just before the "if (failures == 0)" in main()
            idx = text.index("        if (failures == 0) {")
            text = text[:idx] + REGISTER + text[idx:]
            # append the method bodies just before the final class-closing brace
            last_brace = text.rstrip().rfind("}")
            text = text[:last_brace] + "\n" + ladder_methods + "\n}\n"
            tpath.write_text(text)

        print(f"ok: edition {n} updated")

    print("done")


if __name__ == "__main__":
    sys.exit(main())

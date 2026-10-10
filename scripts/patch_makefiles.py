#!/usr/bin/env python3
"""Insert the new shared files into each edition Makefile's javac fallback list."""
import pathlib

ROOT = pathlib.Path(__file__).resolve().parent.parent

ANCHOR = "\t    src/main/java/com/mearvk/sleela/airport/SleelaRuntime.java \\\n"
INSERT = (
    "\t    src/main/java/com/mearvk/sleela/airport/EditionConfig.java \\\n"
    "\t    src/main/java/com/mearvk/sleela/airport/Phosphor.java \\\n"
    "\t    src/main/java/com/mearvk/sleela/airport/EditionGimmicks.java \\\n"
    "\t    src/main/java/com/mearvk/sleela/airport/GimmickSystems.java \\\n"
)

for n in range(1, 9):
    mk = ROOT / str(n) / "Makefile"
    text = mk.read_text()
    if "EditionGimmicks.java" in text:
        print(f"ed {n}: already patched")
        continue
    if ANCHOR not in text:
        print(f"ed {n}: ANCHOR not found — skipping")
        continue
    text = text.replace(ANCHOR, ANCHOR + INSERT, 1)
    mk.write_text(text)
    print(f"ed {n}: patched")
print("done")

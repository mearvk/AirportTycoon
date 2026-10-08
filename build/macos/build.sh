#!/bin/sh
# Airport Tycoon — macOS 11+ build
# Requires Java 21 and Maven 3.9+.

set -eu
ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"

for edition in 1 2 3 4 5 6 7 8; do
    mkdir -p "$ROOT/build/$edition"
    echo "== Airport Tycoon Edition $edition / macOS =="
    (cd "$ROOT/$edition/ui" && mvn -q -DskipTests=false test)
done
mkdir -p "$ROOT/build/dungeons-of-moria"
echo "macOS build/test complete."

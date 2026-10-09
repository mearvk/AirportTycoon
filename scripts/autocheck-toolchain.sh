#!/bin/sh
# Airport Tycoon autocheck for SLeeLa verifier-discovery failures.
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$ROOT"
fail=0
echo "== Airport Tycoon autocheck: SLeeLa verifier assets =="
if [ -n "${SLEELA_HOME:-}" ]; then
  echo "INFO: SLEELA_HOME=$SLEELA_HOME"
  for p in tools/verify-before-execution.py security/sha256-manifest.json; do
    if [ -f "$SLEELA_HOME/$p" ]; then echo "PASS: SLEELA_HOME/$p"; else echo "WARN: SLEELA_HOME missing $p"; fi
  done
else
  echo "INFO: SLEELA_HOME unset; Makefile discovery must select a valid root"
fi
echo "== Search: verifier path assumptions across editions =="
if grep -R -n --include='Makefile' -E 'verification tool not found|searched upward from|SLEELA_ENV|SLEELA \?=' 1 2 3 4 5 6 7 8; then echo "INFO: reviewed edition Makefiles"; else echo "FAIL: no edition toolchain configuration found"; fail=1; fi
for n in 1 2 3 4 5 6 7 8; do
 if grep -q '$(if $(wildcard $(CURDIR)/../../SLeeLa/impl/build/sleela)' "$n/Makefile"; then echo "PASS: edition $n prefers local SLeeLa build"; else echo "WARN: edition $n may select an older PATH executable"; fi
done
exit "$fail"

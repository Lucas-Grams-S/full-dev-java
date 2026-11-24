#!/usr/bin/env bash
# S4.02: every alert needs a runbook: finds the `runbook:` path of each alert in the given rules files and checks that the file exists
# under DOCS_ROOT and passes DocLint. Usage: ./check-runbooks.sh DOCS_ROOT rules1.yml [rules2.yml ...]
set -u
root="$1"; shift
here="$(cd "$(dirname "$0")" && pwd)"
missing=0; bad=0; total=0
for p in $(grep -hoE 'runbook: *"[^"]+"' "$@" | sed -E 's/runbook: *"([^"]+)"/\1/' | sort -u); do
  total=$((total+1))
  if [ ! -f "$root/$p" ]; then echo "MISSING  $p"; missing=$((missing+1)); continue; fi
  if java "$here/DocLint.java" runbook "$root/$p" >/dev/null; then echo "OK       $p"; else echo "LINT     $p"; bad=$((bad+1)); fi
done
echo "runbooks referenced: $total | missing: $missing | failing the lint: $bad"
[ $((missing+bad)) -eq 0 ]

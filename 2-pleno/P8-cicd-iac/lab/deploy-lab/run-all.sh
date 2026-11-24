#!/usr/bin/env bash
# Runs the four experiments and rewrites out-*.txt (the lessons quote these files).
# Needs Docker and the image nginx:1.29-alpine. Takes ~2 minutes. Cleans up after itself.
set -Eeuo pipefail
cd "$(dirname "$0")"
export STARTUP=5
bash exp1-recreate.sh                      > out-1-recreate.txt 2>&1
bash exp2-rolling.sh                       > out-2-rolling.txt 2>&1
BAD_ROLLOUT=1 bash exp2-rolling.sh         > out-2b-rolling-flaky.txt 2>&1
BAD_ROLLOUT=2 bash exp2-rolling.sh         > out-2c-rolling-dead.txt 2>&1
bash exp3-bluegreen.sh                     > out-3-bluegreen.txt 2>&1
bash exp4-canary.sh                        > out-4a-canary-good.txt 2>&1
BAD=1 bash exp4-canary.sh                  > out-4b-canary-bad.txt 2>&1 || true
echo "done; leftovers:"; docker ps -a --format '{{.Names}}' | grep '^lab-p8-' || echo "  none"

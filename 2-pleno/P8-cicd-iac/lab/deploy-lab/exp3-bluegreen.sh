#!/usr/bin/env bash
# Experiment 3: BLUE/GREEN. Switch under load, then ROLLBACK, measuring time and errors.
set -Eeuo pipefail
source "$(dirname "$0")/lib.sh"; trap cleanup EXIT; net_up
export WORK
app lab-p8-app-blue v1;  ready lab-p8-app-blue
app lab-p8-app-green v2; ready lab-p8-app-green
set_pool lab-p8-app-blue
echo ">> 3000 requests; green is up and warm; at t=1s we switch to green, at t=4s we ROLL BACK to blue"
( sleep 4; bash "$HERE/step.sh" bg_to lab-p8-app-blue ) &
python3 "$HERE/load.py" "http://127.0.0.1:$PORT/" 3000 4 --at 1 "bash $HERE/step.sh bg_to lab-p8-app-green"
wait
echo ">> smoke test BEFORE switching: a DEAD green (every /health fails)"
app lab-p8-app-green v2 2; ready lab-p8-app-green || echo "  green failed its readiness -> the switch is NEVER made"
echo ">> smoke test BEFORE switching: a FLAKY green (30% fail), 10 rounds (ready = 3 consecutive OKs, retried up to 30 times, like a probe that keeps trying)"
pass=0; for r in $(seq 1 10); do app lab-p8-app-green v2 1; ready lab-p8-app-green && pass=$((pass+1)); done
echo "  the flaky green passed the 3-probe smoke test in $pass of 10 rounds: a probe that keeps trying only needs 3 OKs in a row ONCE"

#!/usr/bin/env bash
# Experiment 2: ROLLING over 4 instances. BAD_ROLLOUT=1 shows the readiness gate stopping a bad version.
set -Eeuo pipefail
source "$(dirname "$0")/lib.sh"; trap cleanup EXIT; net_up
export WORK STARTUP BAD_ROLLOUT="${BAD_ROLLOUT:-0}"
for i in 1 2 3 4; do app "lab-p8-app-$i" v1; ready "lab-p8-app-$i"; done
set_pool lab-p8-app-1 lab-p8-app-2 lab-p8-app-3 lab-p8-app-4
echo ">> 4000 requests / 4 threads; at t=1s the rolling update starts (BAD_ROLLOUT=$BAD_ROLLOUT)"
python3 "$HERE/load.py" "http://127.0.0.1:$PORT/" 4000 4 --at 1 "bash $HERE/step.sh rolling"

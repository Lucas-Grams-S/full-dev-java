#!/usr/bin/env bash
# Experiment 1: RECREATE (stop v1, start v2). Measures the downtime a user sees.
set -Eeuo pipefail
source "$(dirname "$0")/lib.sh"; trap cleanup EXIT; net_up
export WORK STARTUP
app lab-p8-app-1 v1; ready lab-p8-app-1
cat >"$WORK/r.conf" <<CONF
upstream app { server lab-p8-app-1:80; }
server { listen 80; location / { proxy_pass http://app; proxy_connect_timeout 1s; } }
CONF
router_up "$WORK/r.conf"
echo ">> 3000 requests / 4 threads; at t=1s v1 is removed and v2 starts (simulated startup: ${STARTUP}s)"
python3 "$HERE/load.py" "http://127.0.0.1:$PORT/" 3000 4 --at 1 "bash $HERE/step.sh recreate_v2"

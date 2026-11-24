#!/usr/bin/env bash
# Experiment 4: CANARY by percentage, with an automatic decision from the error ratio of each version.
set -Eeuo pipefail
source "$(dirname "$0")/lib.sh"; trap cleanup EXIT; net_up
export WORK
BAD="${BAD:-0}"
app lab-p8-app-stable v1; ready lab-p8-app-stable
app lab-p8-app-canary v2 "$BAD"
canary_pct() {  # $1 = percentage sent to the canary (nginx rejects "0%": 0 means "no split at all")
  if [[ "$1" -eq 0 ]]; then
    cat >"$WORK/r.conf" <<CONF
upstream stable { server lab-p8-app-stable:80; }
server { listen 80; location / { proxy_pass http://stable; } }
CONF
    router_up "$WORK/r.conf"; return 0
  fi
  cat >"$WORK/r.conf" <<CONF
upstream stable { server lab-p8-app-stable:80; }
upstream canary { server lab-p8-app-canary:80; }
split_clients "\${request_id}" \$pool { $1% canary; * stable; }
server { listen 80; location / { proxy_pass http://\$pool; } }
CONF
  router_up "$WORK/r.conf"
}
for pct in 5 25; do
  canary_pct "$pct"
  echo ">> canary at ${pct}% (BAD=$BAD): 4000 requests"
  python3 "$HERE/load.py" "http://127.0.0.1:$PORT/" 4000 4 | tee "$WORK/out.txt"
  # decision rule: error ratio OF THE CANARY (not the total) above 2% -> abort
  rc=0
  python3 - "$WORK/out.txt" <<'PY' || rc=$?
import re, sys
ok = bad = 0
for line in open(sys.argv[1]):
    m = re.match(r"\s+version=v2\s+status=(\S+)\s+(\d+)", line)
    if m:
        n = int(m.group(2))
        if m.group(1).isdigit() and int(m.group(1)) < 500: ok += n
        else: bad += n
tot = ok + bad
ratio = bad / tot if tot else 0
print(f"  canary: {tot} requests, {bad} errors -> {100*ratio:.1f}% ; rule: abort above 2%")
if ratio > 0.02:
    print("  DECISION: ABORT (canary back to 0%)"); sys.exit(3)
print("  DECISION: promote to the next step")
PY
  if [[ $rc -eq 3 ]]; then canary_pct 0; echo "  canary back to 0%; checking 500 requests:"; python3 "$HERE/load.py" "http://127.0.0.1:$PORT/" 500 4 | sed 's/^/    /'; break; fi
done

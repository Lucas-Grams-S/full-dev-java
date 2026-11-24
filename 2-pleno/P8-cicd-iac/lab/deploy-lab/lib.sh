# shellcheck shell=bash
# Helpers shared by the experiments. Everything lives in the docker network "lab-p8-net".
NET=lab-p8-net
ROUTER=lab-p8-router
PORT=5090
IMG=nginx:1.29-alpine
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WORK="${WORK:-$(mktemp -d)}"
chmod 755 "$WORK"

net_up() { docker network inspect "$NET" >/dev/null 2>&1 || docker network create "$NET" >/dev/null; }

# app NAME VERSION [BAD]: nginx that answers its version; BAD=1: 30% of requests (and of /health) fail; BAD=2: all fail
app() {
  local name=$1 ver=$2 bad=${3:-0} conf="$WORK/$1.conf"
  if [[ "$bad" != 0 ]]; then
    local pct=30; [[ "$bad" == 2 ]] && pct=100
    cat >"$conf" <<CONF
split_clients "\${request_id}" \$fail { ${pct}% 1; * 0; }
server {
  listen 80;
  add_header X-Version $ver always;
  location /health { if (\$fail) { return 500; } return 200 "ok\n"; }
  location / { if (\$fail) { return 500 "boom\n"; } return 200 "$ver\n"; }
}
CONF
  else
    cat >"$conf" <<CONF
server {
  listen 80;
  add_header X-Version $ver always;
  location /health { return 200 "ok\n"; }
  location / { return 200 "$ver\n"; }
}
CONF
  fi
  docker rm -f "$name" >/dev/null 2>&1 || true
  docker run -d --name "$name" --network "$NET" -v "$conf":/etc/nginx/conf.d/default.conf:ro "$IMG" >/dev/null
}

# ready NAME: a "readiness probe": 3 consecutive 200s on /health (wget inside the container)
ready() {
  local name=$1 ok=0
  for _ in $(seq 1 30); do
    if docker exec "$name" wget -q -O /dev/null "http://127.0.0.1/health" 2>/dev/null; then
      ok=$((ok+1)); [[ $ok -ge 3 ]] && return 0
    else ok=0; fi
    sleep 0.3
  done
  return 1
}

# router CONFFILE: (re)creates the router with the given server config
router_up() {
  cp "$1" "$WORK/router.conf"
  if docker ps --format '{{.Names}}' | grep -qx "$ROUTER"; then
    docker exec "$ROUTER" nginx -s reload >/dev/null 2>&1
  else
    docker rm -f "$ROUTER" >/dev/null 2>&1 || true
    docker run -d --name "$ROUTER" --network "$NET" -p 127.0.0.1:$PORT:80 \
      -v "$WORK/router.conf":/etc/nginx/conf.d/default.conf:ro "$IMG" >/dev/null
  fi
  for _ in $(seq 1 30); do curl -s -o /dev/null "http://127.0.0.1:$PORT/" && return 0; sleep 0.2; done
}

cleanup() {
  docker rm -f "$ROUTER" $(docker ps -aq --filter "name=lab-p8-app-") >/dev/null 2>&1 || true
  docker network rm "$NET" >/dev/null 2>&1 || true
}

# --- steps that run in the MIDDLE of a load test (called through step.sh) -------------------
# A real Spring Boot start takes seconds; STARTUP simulates it (we measure the rest for real).
STARTUP="${STARTUP:-5}"
recreate_v2() { docker rm -f lab-p8-app-1 >/dev/null; sleep "$STARTUP"; app lab-p8-app-1 v2; ready lab-p8-app-1; }

# upstream with the given container names (only the live ones), then reload the router
set_pool() {
  { echo "upstream app {"; for n in "$@"; do echo "  server $n:80;"; done; echo "}"
    echo 'server { listen 80; location / { proxy_pass http://app; proxy_connect_timeout 1s; proxy_next_upstream error timeout http_502; } }'; } >"$WORK/r.conf"
  router_up "$WORK/r.conf"
}

# ROLLING: replace the 4 pods one at a time; the new one only enters the pool after 'ready'.
# BAD_ROLLOUT=2: v2 is dead (every /health fails) -> the readiness gate stops the rollout.
# BAD_ROLLOUT=1: v2 is FLAKY (30% of /health fail) -> 3 consecutive OKs pass 34% of the time!
rolling() {
  local all=(lab-p8-app-1 lab-p8-app-2 lab-p8-app-3 lab-p8-app-4)
  for i in 0 1 2 3; do
    local old=${all[$i]} new="lab-p8-app-new$((i+1))"
    app "$new" v2 "${BAD_ROLLOUT:-0}"            # surge: the new pod exists before the old one leaves
    if ! ready "$new"; then echo "  [rollout] $new NEVER became ready -> rollout stopped at step $((i+1))/4"; docker rm -f "$new" >/dev/null; return 0; fi
    local pool=(); for j in 0 1 2 3; do
      if [[ $j -gt $i ]]; then pool+=("${all[$j]}"); elif [[ $j -eq $i ]]; then :; else pool+=("lab-p8-app-new$((j+1))"); fi
    done
    pool+=("$new"); set_pool "${pool[@]}"       # new in, old out (old no longer receives requests)
    docker rm -f "$old" >/dev/null
    echo "  [rollout] step $((i+1))/4 done: pool = ${pool[*]}"
    sleep 1
  done
}

# BLUE/GREEN: both exist; the router switches in one reload
bg_to() { set_pool "$1"; echo "  [switch] router -> $1"; }

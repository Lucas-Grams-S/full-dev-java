#!/usr/bin/env bash
# Desliga e LIMPA tudo que run-stack.sh criou.
cd "$(dirname "$0")"
[ -f target/stack/app.pid ] && kill "$(cat target/stack/app.pid)" 2>/dev/null
kill "$(lsof -t -i:8080 2>/dev/null)" 2>/dev/null   # (nunca pkill -f: mata o próprio shell)
docker rm -f lab-p6-prom lab-p6-grafana lab-p6-jaeger >/dev/null 2>&1
rm -rf target/stack
echo "limpo"

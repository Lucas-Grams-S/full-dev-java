#!/usr/bin/env bash
# Gera tráfego sintético contra a aplicação do laboratório (porta 8080).
# uso: ./traffic.sh <quantidade> [pausa_em_segundos] [simulate_ou_vazio]
set -u
N="${1:-60}"; PAUSE="${2:-0.1}"; FORCE="${3:-}"
for i in $(seq 1 "$N"); do
  acc="acc-$(( (i % 9) + 1 ))"
  if [ -n "$FORCE" ]; then sim="$FORCE"
  else r=$(( RANDOM % 100 ))
    if   [ "$r" -lt 8 ]; then sim="insufficient"
    elif [ "$r" -lt 10 ]; then sim="error"
    else sim=""
    fi
  fi
  body="{\"from\":\"$acc\",\"to\":\"acc-x\",\"amount\":\"10.00\",\"simulate\":$( [ -z "$sim" ] && echo null || echo "\"$sim\"" )}"
  curl -s -o /dev/null -X POST localhost:8080/api/v1/transfers -H 'Content-Type: application/json' -H "X-Request-Id: traffic-$i-$RANDOM$RANDOM" -d "$body"
  sleep "$PAUSE"
done

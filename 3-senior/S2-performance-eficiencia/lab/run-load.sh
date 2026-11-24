#!/usr/bin/env bash
# Regrava out-s206-*.txt: curva de saturacao, omissao coordenada (Java puro) e k6 em Docker (imagem grafana/k6:1.8.3).
# Uso: ./run-load.sh   (~3 min; nao rode outra coisa junto)
set -u
cd "$(dirname "$0")"
LAB="$PWD"
JH="${JAVA_HOME:-/usr/lib/jvm/java-25-openjdk-amd64}"
timeout 110 "$JH/bin/java" LoadLab.java saturation > out-s206-saturation.txt 2>&1
timeout 110 "$JH/bin/java" LoadLab.java omission   > out-s206-omission.txt 2>&1

# k6 contra o servidor do laboratorio (porta 18080; o k6 usa a rede do host)
LOG="$(mktemp)"
"$JH/bin/java" LoadLab.java server 18080 > "$LOG" 2>&1 &
until grep -q 'servidor em' "$LOG"; do sleep 1; done
filtra() { grep -E "^\s+(✓|✗)|http_req_duration\.\.|http_reqs\.|dropped_iterations|vus_max|Insufficient|thresholds on" | sed -E 's/time="[^"]*" level=[a-z]+ msg=/aviso: /; s/ executor=.*//' | cut -c1-140; }
for RATE in 400 900; do
  docker run --rm --network host -i -e RATE=$RATE grafana/k6:1.8.3 run --quiet - < k6-open.js > "$LOG.k6" 2>&1
  echo "exit=$?" > "out-s206-k6-$RATE.txt"
  filtra < "$LOG.k6" >> "out-s206-k6-$RATE.txt"
done
kill "$(lsof -t -i:18080)" 2>/dev/null
rm -f "$LOG" "$LOG.k6"
echo ok

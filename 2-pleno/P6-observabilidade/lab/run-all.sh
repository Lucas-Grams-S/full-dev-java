#!/usr/bin/env bash
# Roda TODO o laboratório do P6 e regrava as saídas (out-*.txt) que as aulas mostram.
# Requer JDK 25, Maven, Docker (imagens prom/prometheus, grafana/grafana, jaegertracing/jaeger). Leva ~6 minutos.
set -uo pipefail
cd "$(dirname "$0")"
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-25-openjdk-amd64}"
OBS=observability-lab
echo "== 1/5 testes da aplicação (sem Docker)"
( cd $OBS && mvn -B test > out-suite.raw 2>&1; grep -E 'Tests run:.*Fail|BUILD' out-suite.raw | grep -v 'Time elapsed' > out-suite-resumo.txt
  for t in T1 T2 T3 T4; do grep -hE "^$t " out-suite.raw | sort -u > out-$t.txt; done )
echo "== 2/5 alertas: promtool check + test (e o teste que PROVA que a regra quebrada reprova)"
( cd $OBS/prometheus && {
    docker run --rm -v "$PWD":/p --entrypoint promtool prom/prometheus:latest check rules /p/alerts.yml 2>&1 | sed 's/^/T8 /'
    docker run --rm -v "$PWD":/p -w /p --entrypoint promtool prom/prometheus:latest test rules alerts.test.yml 2>&1 | sed 's/^/T8 /'
    docker run --rm -v "$PWD":/p -w /p --entrypoint promtool prom/prometheus:latest test rules histogram-pause.test.yml 2>&1 | sed 's/^/T8 pausa de lógica: /'
    W=$(mktemp -d); chmod 755 "$W"; sed 's/status=~"5.."/status=~"4.."/' alerts.yml > "$W/alerts.yml"; cp alerts.test.yml "$W/"; chmod a+r "$W"/*
    echo "T8 --- regra QUEBRADA de propósito (5xx trocado por 4xx): o teste precisa reprovar"
    docker run --rm -v "$W":/p -w /p --entrypoint promtool prom/prometheus:latest test rules alerts.test.yml 2>&1 | grep -E 'SUCCESS|FAILED|name:' | sed 's/^/T8 /'
    rm -rf "$W"
  } > ../out-T8.txt )
echo "== 3/5 pilha real (Prometheus, Grafana, Jaeger) + incidente simulado"
( cd $OBS && ./run-stack.sh && ./incident.sh > out-T5.txt 2>&1; ./check-stack.sh > out-T6.txt 2>&1; ./stop-stack.sh )
echo "== 3b: desligamento gracioso (SIGTERM)"
( cd $OBS && ./shutdown-check.sh > out-T9.txt 2>&1 )
echo "== 4/5 Spring Security na porta de gestão"
( cd mgmt-security-lab && mvn -B -q test 2>&1 | grep -E '^T7' > out-T7.txt; rm -rf target )
echo "== 5/5 limpeza"
( cd $OBS && rm -rf target out-suite.raw )
docker ps -a --format '{{.Names}}' | grep lab-p6 || echo "nenhum container lab-p6-* restante"

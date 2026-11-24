#!/usr/bin/env bash
# Desligamento gracioso (decisão 73): o que o /actuator/health/readiness e a requisição em voo fazem quando chega o SIGTERM.
# Imprime linhas "T9 ...". Requer o JAR (mvn package) e as portas 8080/8081 livres.
set -uo pipefail
cd "$(dirname "$0")"
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-25-openjdk-amd64}"
WORK=target/shutdown; mkdir -p $WORK
"$JAVA_HOME/bin/java" -jar target/observability-lab-0.0.1.jar > $WORK/app.log 2>&1 &
PID=$!
for i in $(seq 1 40); do curl -s -o /dev/null -w '%{http_code}' localhost:8081/actuator/health/readiness | grep -q 200 && break; sleep 0.5; done
curl -s -X POST 'localhost:8080/lab/risk-latency?account=acc-7&ms=1500' >/dev/null
# requisição lenta (1,5 s) que estará EM VOO quando o sinal chegar
( curl -s -o /dev/null -w 'T9 requisição em voo: HTTP %{http_code} em %{time_total}s\n' -X POST localhost:8080/api/v1/transfers -H 'Content-Type: application/json' -d '{"from":"acc-7","to":"x","amount":"1.00","simulate":null}' > $WORK/inflight.txt ) &
sleep 0.3
T0=$(date +%s.%N); kill -TERM $PID
echo "T9 SIGTERM enviado (JVM pid $PID)"
last=""
for i in $(seq 1 40); do
  r=$(curl -s -m 1 -o /dev/null -w '%{http_code}' localhost:8081/actuator/health/readiness); l=$(curl -s -m 1 -o /dev/null -w '%{http_code}' localhost:8081/actuator/health/liveness)
  n=$(curl -s -m 1 -o /dev/null -w '%{http_code}' -X POST localhost:8080/api/v1/transfers -H 'Content-Type: application/json' -d '{"from":"acc-1","to":"x","amount":"1.00","simulate":null}')
  cur="readiness=$r liveness=$l novaRequisicao=$n"
  if [ "$cur" != "$last" ]; then printf 'T9 t=%.1fs %s\n' "$(echo "$(date +%s.%N) - $T0" | bc)" "$cur"; last="$cur"; fi
  kill -0 $PID 2>/dev/null || break
  sleep 0.1
done
wait $PID 2>/dev/null; echo "T9 JVM saiu com código $?"
wait; cat $WORK/inflight.txt
grep -E 'Commencing graceful shutdown|Graceful shutdown complete|Graceful shutdown aborted' $WORK/app.log | sed -E 's/.* : //' | cut -c1-120 | sed 's/^/T9 log: /'
rm -rf $WORK

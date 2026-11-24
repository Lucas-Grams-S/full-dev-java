#!/usr/bin/env bash
# Sobe a pilha de observabilidade do laboratório: aplicação (8080/8081), Prometheus (9090), Grafana (3000), Jaeger (16686/4318).
# Requer JDK 25, Maven e Docker. Containers em rede "host", nome lab-p6-*. Desligue com ./stop-stack.sh
set -euo pipefail
cd "$(dirname "$0")"
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-25-openjdk-amd64}"
WORK="${WORK:-target/stack}"; mkdir -p "$WORK"
mvn -B -q -DskipTests package 2>&1 | grep -vE '^WARNING' || true
# regras com "for" curto SÓ para a demonstração (o arquivo de verdade usa 1 a 10 minutos e é testado com promtool)
sed -E 's/for: [0-9]+m/for: 10s/' prometheus/alerts.yml > "$WORK/alerts.yml"
cp prometheus/prometheus.yml "$WORK/prometheus.yml"
docker rm -f lab-p6-prom lab-p6-grafana lab-p6-jaeger >/dev/null 2>&1 || true
docker run -d --name lab-p6-jaeger --network host jaegertracing/jaeger:latest >/dev/null
docker run -d --name lab-p6-prom --network host \
  -v "$PWD/$WORK/prometheus.yml":/etc/prometheus/prometheus.yml:ro -v "$PWD/$WORK/alerts.yml":/etc/prometheus/alerts.yml:ro \
  prom/prometheus:latest --config.file=/etc/prometheus/prometheus.yml --web.listen-address=:9090 >/dev/null
# senha só de laboratório; Grafana escuta só em 127.0.0.1
docker run -d --name lab-p6-grafana --network host -e GF_SECURITY_ADMIN_PASSWORD=lab-only-admin -e GF_SERVER_HTTP_ADDR=127.0.0.1 \
  -v "$PWD/grafana/provisioning":/etc/grafana/provisioning:ro -v "$PWD/grafana/dashboards":/var/lib/grafana/dashboards:ro grafana/grafana:latest >/dev/null
nohup env LOGGING_STRUCTURED_FORMAT_CONSOLE=ecs "$JAVA_HOME/bin/java" -jar target/observability-lab-0.0.1.jar > "$WORK/app.log" 2>&1 &
echo $! > "$WORK/app.pid"
for i in $(seq 1 40); do curl -s -o /dev/null -w '%{http_code}' localhost:8081/actuator/health/readiness | grep -q 200 && break; sleep 1; done
for i in $(seq 1 40); do curl -s -o /dev/null localhost:9090/-/ready && curl -s -o /dev/null localhost:3000/api/health && break; sleep 1; done
echo "pronto: app :8080/:8081, prometheus :9090, grafana :3000 (admin / lab-only-admin), jaeger :16686"

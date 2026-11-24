#!/usr/bin/env bash
# Regrava todos os out-*.txt do laboratorio S1 (Java puro, sem Docker). Uso: JAVA_HOME=<jdk25> ./run-all.sh
set -u
cd "$(dirname "$0")"
JAVA="${JAVA_HOME:-/usr/lib/jvm/java-25-openjdk-amd64}/bin/java"
run() { timeout 300 "$JAVA" "$1.java" > "out-$2.txt" 2>&1 || echo "FALHOU: $1"; }
run LoadBalancerLab s101
run ConsistencyLab s102
run ShardingLab s103
run CacheLab s104
run CqrsLab s105
run EventSourcingLab s106
run SagaLab s107
run IdempotencyLab s108
run RateLimitLab s109
run ClockLab s110
run ShortenerLab s111
run ReconciliationLab s112
run FeedLab s113
echo "ok: $(ls out-*.txt | wc -l) saidas"

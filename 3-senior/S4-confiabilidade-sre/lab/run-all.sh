#!/usr/bin/env bash
# Rewrites every out-*.txt of the S4 lab (Java only, no Docker, no Maven). Needs JDK 25: JAVA_HOME or `java` on PATH.
# Run ONE module at a time on a shared machine: ChaosLab and CapacityLab measure time.
set -u
cd "$(dirname "$0")"
JAVA="${JAVA_HOME:+$JAVA_HOME/bin/}java"
s() { [ -z "${ONLY:-}" ] || [ "$ONLY" = "$1" ]; }
s s1 && { $JAVA SloLab.java budget > out-s401-budget.txt; timeout 300 $JAVA SloLab.java alerts > out-s401-alerts.txt; }
s s2 && { $JAVA DocLint.java selftest samples > out-s402-lint.txt; ./check-runbooks.sh samples/docs ../../../2-pleno/P6-observabilidade/lab/observability-lab/prometheus/alerts.yml prometheus/slo-rules.yml > out-s402-runbooks.txt || true; }
s s4 && for v in no-timeout timeout timeout-async; do timeout 120 $JAVA ChaosLab.java $v > out-s404-$v.txt 2>&1; done
s s4 && { timeout 120 $JAVA ChaosLab.java timeout 100 > out-s404-timeout-100rps.txt 2>&1; timeout 120 $JAVA ChaosLab.java timeout-async 100 > out-s404-timeout-async-100rps.txt 2>&1; }
s s5 && { $JAVA RpoRtoLab.java rpo > out-s405-rpo.txt; $JAVA RpoRtoLab.java rto > out-s405-rto.txt; }
s s6 && { $JAVA AzLab.java capacity > out-s406-capacity.txt; $JAVA AzLab.java chain > out-s406-chain.txt; }
s s7 && { timeout 200 $JAVA CapacityLab.java queue > out-s407-queue.txt; $JAVA CapacityLab.java little > out-s407-little.txt; $JAVA CapacityLab.java forecast > out-s407-forecast.txt; }
s pt && ./run-promtool.sh --write
echo "done"

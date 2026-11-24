#!/usr/bin/env bash
# Roda a suíte e grava as saídas que as aulas mostram. Requer JDK 25 e Maven; NÃO usa Docker.
set -euo pipefail
cd "$(dirname "$0")"
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-25-openjdk-amd64}"
mvn -B test 2>&1 | grep -vE '^WARNING' > out-suite.raw || true
grep -E 'Tests run:|BUILD' out-suite.raw > out-suite-resumo.txt
awk '/^MODULES = /,/^$/' out-suite.raw | head -3 > out-modules.txt || true
grep -E "^RULE|^LAYERED|^SQL RULE|^   (Class|Field|Method|Constructor|Layer)" out-suite.raw | sed -E 's/ in \([A-Za-z]+\.java:[0-9]+\)//' > out-archunit-bad.txt || true
awk '/^# (Account|Transfer|Antifraud|Notification)/,/^$/' out-suite.raw > out-module-model.txt || true
grep -E "^(SYNC|ASYNC) LISTENER" out-suite.raw > out-event-semantics.txt || true
grep -E "^TYPO RULE" out-suite.raw >> out-archunit-bad.txt || true
awk '/^LAYERED/{f=1} /^SQL RULE/{f=0} f' out-archunit-bad.txt > out-layered.txt || true
grep -E "^INCIDENT" out-suite.raw > out-incident.txt || true
awk '/^MODBAD VERIFY/,/^$/' out-suite.raw | head -4 > out-modulith-rules.txt || true
cp target/spring-modulith-docs/components.puml out-components.puml
awk '/^BAD MODULES VERIFY/,/non-exposed type/' out-suite.raw > out-modulith-bad.txt || true
ls target/spring-modulith-docs > out-docs.txt
cat out-suite-resumo.txt

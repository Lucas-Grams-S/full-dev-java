#!/usr/bin/env bash
# Roda a suíte do P5 e grava as saídas que as aulas mostram. Requer JDK 25 e Maven; NÃO usa Docker nem rede (só o Maven Central na 1ª vez).
set -uo pipefail
cd "$(dirname "$0")"
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-25-openjdk-amd64}"
mvn -B test > out-suite.raw 2>&1 || true
grep -E 'Tests run:.*Fail|Reactor Summary|SUCCESS|FAILURE|BUILD' out-suite.raw | grep -vE 'Time elapsed' > out-suite-resumo.txt || true
for t in T1 T2 T3 T4 T5 T6 T7 T10 T3G T4G; do
  grep -hE "^$t " out-suite.raw | sort -u > "out-$t.txt" || true
done
# Verificação de contrato com o provedor "quebrado" de propósito (3 modos)
: > out-contract-break.txt
for mode in additive rename type; do
  echo "### lab.break=$mode" >> out-contract-break.txt
  mvn -B -q -pl contract-lab test -Dtest=ProviderPactVerificationTest -Dlab.break=$mode 2>&1 \
    | grep -E 'has a matching body|body: |Tests run:.*Fail.*ProviderPact|Type mismatch|missing the following' | sed -E 's/ -- Time elapsed.*//' | sort -u >> out-contract-break.txt
done
cp contract-lab/src/test/resources/pacts/payflow-monolith-notifications.json out-pact.json 2>/dev/null || true
cat out-suite-resumo.txt

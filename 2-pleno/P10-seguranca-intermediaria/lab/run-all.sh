#!/usr/bin/env bash
# Regrava os out-*.txt do laboratório da P10 (Keycloak 26.8.2 + PostgreSQL 18 + Trivy/cosign em contêineres).
# Uso: ./run-all.sh            (tudo)   |   ONLY=tests|mutants|supply|outage|threat  (uma parte)
# Limpa ao fim: contêineres lab-p10-*, target/ e logs brutos.
set -uo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64
RAW="$(mktemp -d)"
want() { [[ -z "${ONLY:-}" || "${ONLY:-}" == "$1" ]]; }

cleanup() {
  "$HERE/keycloak-lab/kc-down.sh"; "$HERE/secrets-lab/pg-down.sh"
  rm -rf "$HERE/identity-lab/target" "${RAW:?}"
}
trap cleanup EXIT

if want tests; then
  "$HERE/keycloak-lab/kc-up.sh" > "$HERE/keycloak-lab/out-setup.txt" 2>&1
  "$HERE/secrets-lab/pg-up.sh" > /dev/null 2>&1
  ( cd "$HERE/identity-lab" && mvn -B -ntp test -Dtest='ResourceServerTest,*IT' > "$RAW/suite.raw" 2>&1 )
  grep -E "Tests run:.*in com.payflow|^\[(INFO|ERROR|WARNING)\] Tests run: [0-9]+, F.*[0-9]$" "$RAW/suite.raw" | sed -E 's/^\[[A-Z]+\] //; s/ -- in com\.payflow\./ — /' > "$HERE/identity-lab/out-suite.txt"
  for t in 01 02 03 04 05 06 07; do
    grep "^T10\.$t" "$RAW/suite.raw" | sed "s/^T10\.$t //" | cut -c1-600 > "$HERE/identity-lab/out-T$t.txt"
  done
fi
want mutants && python3 "$HERE/run-mutants.py" > "$HERE/identity-lab/out-mutants.txt" 2>&1
want supply && ONLY= "$HERE/supply-chain-lab/run-supply-chain.sh" > /dev/null 2>&1
want supply && "$HERE/supply-chain-lab/repro-check.sh"
want outage && "$HERE/identity-lab/outage.sh"
want threat && python3 "$HERE/threat-model/check-threat-model.py" --self-test > "$HERE/threat-model/out-threat-model.txt" 2>&1
echo "run-all done"

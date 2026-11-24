#!/usr/bin/env bash
# Rewrites every out-*.txt of the S5 lab. Java only (JDK 25 + keytool) for the programs; Docker only for run-trivy.sh.
# Run ONE section at a time on a shared machine: MtlsLab cost and CryptoLab cost measure time.
set -u
cd "$(dirname "$0")"
JAVA="${JAVA_HOME:+$JAVA_HOME/bin/}java"
s() { [ -z "${ONLY:-}" ] || [ "$ONLY" = "$1" ]; }
s s1 && { $JAVA DefenseLab.java layers > out-s501-layers.txt; $JAVA DefenseLab.java policy > out-s501-policy.txt; }
s s2 && { timeout 200 $JAVA MtlsLab.java cases > out-s502-cases.txt 2>&1; timeout 200 $JAVA MtlsLab.java cost > out-s502-cost.txt 2>&1; }
s s3 && for m in envelope nonce blind cost; do $JAVA CryptoLab.java $m > out-s503-$m.txt; done
s s4 && for m in sast dast sca policy; do $JAVA PipelineLab.java $m > out-s504-$m.txt; done
s s5 && for m in luhn scrub cpf inventory; do $JAVA PiiLab.java $m > out-s505-$m.txt; done
s s6 && for m in chain lint; do $JAVA AuditLab.java $m > out-s506-$m.txt; done
s s7 && for m in score lint coverage; do $JAVA ReviewLab.java $m > out-s507-$m.txt; done
s trivy && ONLY= ./run-trivy.sh
echo "done"

#!/usr/bin/env bash
# Regrava os out-s202-*.txt e out-s203-*.txt (JFR; Java puro; sem Maven e sem Docker).
# Uso: ./run-profile.sh   (JAVA_HOME opcional; padrao: JDK 25)
set -u
cd "$(dirname "$0")"
LAB="$PWD"
JH="${JAVA_HOME:-/usr/lib/jvm/java-25-openjdk-amd64}"
W="$(mktemp -d)"; chmod 755 "$W"; mkdir "$W/cls" "$W/rec"
"$JH/bin/javac" -d "$W/cls" ProfileApp.java BiasLab.java FlameFold.java CustomEventDemo.java || exit 1
cd "$W"
EV="jdk.JavaMonitorEnter#threshold=1ms"

# ---- S2.02: a mesma aplicacao, versao com problemas e versao corrigida
"$JH/bin/java" -cp cls -XX:StartFlightRecording:filename=rec/bad.jfr,settings=profile,$EV ProfileApp bad 12 2>&1 | grep -E '^(BAD|FIXED)' > "$LAB/out-s202-ops.txt"
"$JH/bin/java" -cp cls -XX:StartFlightRecording:filename=rec/fixed.jfr,settings=profile,$EV ProfileApp fixed 12 2>&1 | grep -E '^(BAD|FIXED)' >> "$LAB/out-s202-ops.txt"
"$JH/bin/jfr" summary rec/bad.jfr | grep -E 'Duration|Event Type|jdk.ExecutionSample|jdk.JavaMonitorEnter|jdk.ObjectAllocationSample|jdk.GarbageCollection ' > "$LAB/out-s202-summary.txt"
"$JH/bin/jfr" view hot-methods rec/bad.jfr   | sed -n 1,14p  > "$LAB/out-s202-hot-bad.txt"
"$JH/bin/jfr" view hot-methods rec/fixed.jfr | sed -n 1,12p  > "$LAB/out-s202-hot-fixed.txt"
"$JH/bin/jfr" view allocation-by-site rec/bad.jfr | sed -n 1,10p > "$LAB/out-s202-alloc-bad.txt"
"$JH/bin/jfr" view allocation-by-site rec/fixed.jfr | sed -n 1,10p > "$LAB/out-s202-alloc-fixed.txt"
"$JH/bin/jfr" view contention-by-site rec/bad.jfr | sed -n 1,8p > "$LAB/out-s202-contention-bad.txt"
{ echo "--- BAD"; "$JH/bin/jfr" view gc-pauses rec/bad.jfr | grep -E 'Total Pause|Number of Pauses|Longest Pause|Average Pause'
  echo "--- FIXED"; "$JH/bin/jfr" view gc-pauses rec/fixed.jfr | grep -E 'Total Pause|Number of Pauses|Longest Pause|Average Pause'; } > "$LAB/out-s202-gc.txt"

# ---- S2.02: evento customizado
"$JH/bin/java" -cp cls -XX:StartFlightRecording:filename=rec/c.jfr CustomEventDemo 2>&1 | grep -v 'jfr,startup' > "$LAB/out-s202-custom.txt"
"$JH/bin/jfr" print --events payflow.Transfer rec/c.jfr | grep -E 'payflow|duration|cents|result|^}' >> "$LAB/out-s202-custom.txt"

# ---- S2.03: viés de amostragem e arvore de chamadas
{
  echo "### padrao (JDK 25)"
  "$JH/bin/java" -cp cls -XX:StartFlightRecording:filename=rec/b1.jfr,settings=profile BiasLab 10 2>&1 | grep -v 'jfr,startup'
  echo "JFR:"; "$JH/bin/jfr" view hot-methods rec/b1.jfr | sed -n 4,7p
  echo; echo "### sem pontos de seguranca em lacos contados (-XX:-UseCountedLoopSafepoints)"
  "$JH/bin/java" -cp cls -XX:-UseCountedLoopSafepoints -XX:StartFlightRecording:filename=rec/b2.jfr,settings=profile BiasLab 10 2>&1 | grep -v 'jfr,startup'
  echo "JFR:"; "$JH/bin/jfr" view hot-methods rec/b2.jfr | sed -n 4,7p
  echo; echo "### idem + -XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints"
  "$JH/bin/java" -cp cls -XX:-UseCountedLoopSafepoints -XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints -XX:StartFlightRecording:filename=rec/b3.jfr,settings=profile BiasLab 10 2>&1 | grep -v 'jfr,startup'
  echo "JFR:"; "$JH/bin/jfr" view hot-methods rec/b3.jfr | sed -n 4,7p
} > "$LAB/out-s203-bias.txt"
"$JH/bin/jfr" print --events jdk.ExecutionSample --stack-depth 64 rec/bad.jfr > rec/bad-samples.txt
"$JH/bin/java" -cp cls FlameFold rec/bad-samples.txt 5 3 > "$LAB/out-s203-flame.txt"
cd "$LAB" && rm -rf "${W:?}"
echo "ok"

#!/usr/bin/env bash
# Regrava out-s205.txt: o mesmo servico sob 7 configuracoes de GC/heap, 2 repeticoes cada (~5 min).
# Conjunto vivo = 600 MB, 3000 req/s, 16 s por execucao (6 s de aquecimento descartados). Java puro.
set -u
cd "$(dirname "$0")"
LAB="$PWD"
J="${JAVA_HOME:-/usr/lib/jvm/java-25-openjdk-amd64}/bin"
W="$(mktemp -d)"; chmod 755 "$W"
"$J/javac" -d "$W" GcLab.java || exit 1
cd "$W"
: > "$LAB/out-s205.txt"
run() {
  local nome="$1"; shift
  for rep in 1 2; do
    echo "### $nome (execucao $rep)" >> "$LAB/out-s205.txt"
    "$J/java" -cp . "$@" -Dchurn=5 -XX:StartFlightRecording:filename=g.jfr GcLab 16 3000 600 6 > run.txt 2>&1
    grep -E '^(req=|heap max=)' run.txt >> "$LAB/out-s205.txt"
    local oom; oom=$(grep -c 'OutOfMemoryError' run.txt)
    [ "$oom" -gt 0 ] && echo "OutOfMemoryError registrado $oom vez(es) na saida (threads de servico morreram)" >> "$LAB/out-s205.txt"
    "$J/jfr" view gc-pauses g.jfr | grep -E 'Total Pause|Number of Pauses|Maximum Pause' | tr '\n' ' ' >> "$LAB/out-s205.txt"
    echo >> "$LAB/out-s205.txt"
  done
}
run "G1 -Xmx1g"        -XX:+UseG1GC -Xmx1g
run "G1 -Xmx700m"      -XX:+UseG1GC -Xmx700m
run "G1 -Xmx1536m"     -XX:+UseG1GC -Xmx1536m
run "Parallel -Xmx1g"  -XX:+UseParallelGC -Xmx1g
run "Serial -Xmx1g"    -XX:+UseSerialGC -Xmx1g
run "ZGC -Xmx1g"       -XX:+UseZGC -Xmx1g
run "ZGC -Xmx700m"     -XX:+UseZGC -Xmx700m
cd "$LAB" && rm -rf "${W:?}"
echo ok

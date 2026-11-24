#!/usr/bin/env bash
# Regrava os out-s204-*.txt (vazamento, heap dump, histograma, OldObjectSample). Java puro.
set -u
cd "$(dirname "$0")"
LAB="$PWD"
JH="${JAVA_HOME:-/usr/lib/jvm/java-25-openjdk-amd64}"
J="$JH/bin"
W="$(mktemp -d)"; chmod 755 "$W"
"$J/javac" -d "$W" LeakLab.java HprofHisto.java || exit 1
cd "$W"

# 1) heap pequeno: o vazamento derruba a JVM e ela deixa o dump
"$J/java" -cp . -Xmx64m -XX:+HeapDumpOnOutOfMemoryError -XX:+ExitOnOutOfMemoryError -XX:HeapDumpPath="$W/oom.hprof" LeakLab leak 0 60 > oom-run.txt 2>&1
grep -E 'OutOfMemoryError|Dumping heap|Heap dump file|Terminating' oom-run.txt | sed "s#$W#<dir>#" | sed -E 's/ in [0-9.]+ secs//' > "$LAB/out-s204-oom.txt"
"$J/java" -cp . HprofHisto "$W/oom.hprof" 8 > "$LAB/out-s204-histo.txt"

# 2) heap maior e ritmo fixo: dois histogramas ao vivo (jcmd) e o JFR com caminho ate a raiz
"$J/java" -cp . -Xmx256m -XX:StartFlightRecording:filename=leak.jfr,settings=profile,dumponexit=true,path-to-gc-roots=true LeakLab leak 3000 16 > slow-run.txt 2>&1 &
PID=$!
sleep 5
{ echo "### t = 5 s"; "$J/jcmd" "$PID" GC.class_histogram | sed -n 2,7p | sed -E 's/ \(java.base@[^)]*\)//'; } > "$LAB/out-s204-jcmd.txt"
sleep 7
{ echo "### t = 12 s"; "$J/jcmd" "$PID" GC.class_histogram | sed -n 2,7p | sed -E 's/ \(java.base@[^)]*\)//'; } >> "$LAB/out-s204-jcmd.txt"
{ "$J/jcmd" "$PID" help GC.heap_dump | sed -n 1,16p; echo ...; "$J/jcmd" "$PID" help GC.class_histogram | sed -n 1,12p; } > "$LAB/out-s204-help.txt"
"$J/jcmd" "$PID" GC.heap_dump "$W/live.hprof" | sed "s#$W#<dir>#" > "$LAB/out-s204-dump.txt"
wait "$PID"
"$J/java" -cp . HprofHisto "$W/live.hprof" 5 > "$LAB/out-s204-histo-live.txt"
grep -E '^req=|^fim' slow-run.txt | tail -2 > "$LAB/out-s204-slow.txt"
"$J/jfr" view memory-leaks-by-site leak.jfr | sed -n 1,10p > "$LAB/out-s204-leakview.txt"
"$J/jfr" print --events jdk.OldObjectSample --stack-depth 3 leak.jfr > old.txt
{ echo "eventos OldObjectSample: $(grep -c '^jdk.OldObjectSample' old.txt)"
  awk 'BEGIN{RS="";ORS="\n\n"} /payload : LeakLab.Session/ {print; exit}' old.txt | sed -n '/object =/,/arrayElements/p;/stackTrace/,/^  \]/p'; } > "$LAB/out-s204-oldobj.txt"

# 3) mesma carga, cache limitado
"$J/java" -cp . -Xmx64m LeakLab fixed 0 8 2>&1 | tail -2 > "$LAB/out-s204-fixed.txt"
cd "$LAB" && rm -rf "${W:?}"
echo ok

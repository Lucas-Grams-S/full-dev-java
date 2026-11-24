#!/usr/bin/env bash
# S2.08: compila o jmh-lab (Maven, JDK 25, JMH 1.37) e regrava out-s208-*.txt. Uso: ./run-jmh.sh (~5 min; nao rode outra coisa junto)
set -u
cd "$(dirname "$0")"
LAB="$PWD"
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-25-openjdk-amd64}"
cd jmh-lab
mvn -q -B package -DskipTests > /dev/null 2>&1 || { echo "mvn falhou"; exit 1; }
OPTS="-f 1 -wi 3 -w 1s -i 5 -r 1s"       # 1 fork, 3 aquecimentos, 5 medicoes de 1 s (curto, para a aula)
tabela() { sed -n '/^Benchmark /,$p' | grep -v '^$'; }
"$JAVA_HOME/bin/java" -jar target/benchmarks.jar PitfallsBench   $OPTS 2>&1 | tabela > "$LAB/out-s208-pitfalls.txt"
"$JAVA_HOME/bin/java" -jar target/benchmarks.jar MoneySumBench   $OPTS 2>&1 | tabela > "$LAB/out-s208-money.txt"
"$JAVA_HOME/bin/java" -jar target/benchmarks.jar MoneySumBench   $OPTS -prof gc 2>&1 | tabela | grep -E '^Benchmark|alloc.rate.norm' > "$LAB/out-s208-money-gc.txt"
"$JAVA_HOME/bin/java" -jar target/benchmarks.jar TextBench       $OPTS 2>&1 | tabela > "$LAB/out-s208-text.txt"
"$JAVA_HOME/bin/java" -jar target/benchmarks.jar StreamBench     $OPTS 2>&1 | tabela > "$LAB/out-s208-stream.txt"
"$JAVA_HOME/bin/java" -version 2>&1 | head -1 > "$LAB/out-s208-env.txt"
cd "$LAB" && rm -rf jmh-lab/target
echo ok

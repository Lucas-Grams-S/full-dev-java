#!/usr/bin/env bash
# Laboratório da P7.02/P7.09: a JVM dentro de um contêiner com limites (Docker, sem Kubernetes).
# Requer JDK 25 (javac) e Docker; a imagem eclipse-temurin:25-jre. Saída em out-jvm.txt.
set -uo pipefail
cd "$(dirname "$0")"
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-25-openjdk-amd64}"
W="$(mktemp -d)"; trap 'rm -rf "$W"' EXIT
"$JAVA_HOME/bin/javac" -d "$W" Probe.java && chmod -R a+rX "$W"
IMG=eclipse-temurin:25-jre
info(){ echo "-- $*"; docker run --rm "${@:1:$#-0}" -v "$W":/p:ro $IMG java -cp /p Probe info 2>&1 | grep -v '^Picked up'; }
{
echo "## 1. heap e coletor por limite de memória (--cpus 2, padrão da JVM)"
for m in 256m 512m 768m 1g 2g; do echo -n "--memory $m: "; docker run --rm --memory $m --cpus 2 -v "$W":/p:ro $IMG java -cp /p Probe info; done
echo "## 2. MaxRAMPercentage=75"
for m in 768m 2g; do echo -n "--memory $m: "; docker run --rm --memory $m --cpus 2 -e JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=75 -v "$W":/p:ro $IMG java -cp /p Probe info 2>&1 | grep -v '^Picked up'; done
echo "## 3. CPUs vistas pela JVM (--memory 2g)"
for c in 0.5 1 1.5 2 4; do echo -n "--cpus $c: "; docker run --rm --memory 2g --cpus $c -v "$W":/p:ro $IMG java -cp /p Probe info; done
echo -n "sem limite nenhum: "; docker run --rm -v "$W":/p:ro $IMG java -cp /p Probe info
echo -n "--cpu-shares 256 (equivale à requisição 250m), sem --cpus: "; docker run --rm --cpu-shares 256 --memory 2g -v "$W":/p:ro $IMG java -cp /p Probe info
echo -n "768m, 1 cpu, G1 explícito: "; docker run --rm --memory 768m --cpus 1 -v "$W":/p:ro $IMG java -XX:+UseG1GC -XX:MaxRAMPercentage=70 -cp /p Probe info
echo "## 4. estourar a memória de quatro jeitos (--memory 256m)"
t(){ name=$1; shift; docker rm -f p7-$name >/dev/null 2>&1; docker run --name p7-$name -v "$W":/p:ro "$@" >"$W/$name.out" 2>&1; code=$?; oom=$(docker inspect p7-$name --format '{{.State.OOMKilled}}'); echo "[$name] exit=$code OOMKilled=$oom :: $(tail -c 160 "$W/$name.out" | tr '\n' ' ')"; docker rm -f p7-$name >/dev/null; }
t A-heap-dentro-do-limite --memory 256m $IMG java -XX:MaxRAMPercentage=75 -cp /p Probe heap
t B-Xmx-maior-que-o-limite --memory 256m $IMG java -Xmx600m -cp /p Probe heap
t C-direct-com-teto-padrao --memory 256m $IMG java -XX:MaxRAMPercentage=50 -cp /p Probe direct
t D-nativa-sem-teto --memory 256m $IMG java -XX:MaxRAMPercentage=50 -XX:MaxDirectMemorySize=1g -cp /p Probe native
t E-ExitOnOutOfMemoryError --memory 256m $IMG java -XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError -cp /p Probe heap
} 2>&1 | tee out-jvm.txt

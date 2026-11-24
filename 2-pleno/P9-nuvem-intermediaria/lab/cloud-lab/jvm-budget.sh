#!/usr/bin/env bash
# P9.02: what the JVM does inside a Fargate-sized container (task cpu 512 = 0.5 vCPU, memory 1024 MiB), measured with Docker cgroup limits.
# Docker emulates the LIMITS, not Fargate itself. Needs Docker and eclipse-temurin:25-jre (the J9 base image).
set -Eeuo pipefail
IMG=eclipse-temurin:25-jre
flags() { docker run --rm --memory="$1" --cpus="$2" "$IMG" java "${@:3}" -XX:+PrintFlagsFinal -version 2>&1 | grep -E "MaxHeapSize|UseG1GC |UseSerialGC " | sed -E 's/ +/ /g; s/\{[^}]*\}//g'; }
mib() { awk -v b="$1" 'BEGIN { printf "%.0f MiB", b/1048576 }'; }
echo '$ docker run --memory=1024m --cpus=0.5 java -XX:MaxRAMPercentage=70            (NO explicit collector)'
flags 1024m 0.5 -XX:MaxRAMPercentage=70
echo '$ docker run --memory=1024m --cpus=0.5 java -XX:+UseG1GC -XX:MaxRAMPercentage=70   (what the task definition sets)'
flags 1024m 0.5 -XX:+UseG1GC -XX:MaxRAMPercentage=70
H="$(docker run --rm --memory=1024m --cpus=0.5 "$IMG" java -XX:+UseG1GC -XX:MaxRAMPercentage=70 -XX:+PrintFlagsFinal -version 2>&1 | awk '/ MaxHeapSize /{print $4}')"
echo "max heap = $(mib "$H") of 1024 MiB -> $(awk -v h="$H" 'BEGIN { printf "%.0f", 1024 - h/1048576 }') MiB left for metaspace, code cache, thread stacks, direct buffers and the OS"
echo '$ the same with the 768 MiB limit of the Kubernetes manifests (P7.02), 1 CPU'
H2="$(docker run --rm --memory=768m --cpus=1 "$IMG" java -XX:+UseG1GC -XX:MaxRAMPercentage=70 -XX:+PrintFlagsFinal -version 2>&1 | awk '/ MaxHeapSize /{print $4}')"
echo "max heap = $(mib "$H2") of 768 MiB -> $(awk -v h="$H2" 'BEGIN { printf "%.0f", 768 - h/1048576 }') MiB left"

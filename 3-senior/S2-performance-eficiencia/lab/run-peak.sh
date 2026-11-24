#!/usr/bin/env bash
# S2.07: desempenho de regime, JVM x native image (PeakApp: 12 lotes de CPU). Regrava out-s207-peak.txt.
set -u
cd "$(dirname "$0")"
LAB="$PWD"
J="${JAVA_HOME:-/usr/lib/jvm/java-25-openjdk-amd64}/bin"
W="$(mktemp -d)"; chmod 755 "$W"
"$J/javac" -d "$W" PeakApp.java || exit 1
( cd "$W" && "$J/jar" cf peak.jar PeakApp.class )
docker run --rm --user "$(id -u):$(id -g)" -v "$W:/work" -w /work ghcr.io/graalvm/native-image-community:25 \
   -cp peak.jar PeakApp -o peak --no-fallback > "$W/build.log" 2>&1
{
  for rep in 1 2; do
    echo "JVM (G1, JIT)        #$rep: $("$J/java" -cp "$W/peak.jar" PeakApp)"
    echo "native image         #$rep: $("$W/peak")"
  done
} > "$LAB/out-s207-peak.txt"
cd "$LAB" && rm -rf "${W:?}"
echo ok

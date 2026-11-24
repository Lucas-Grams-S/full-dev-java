#!/usr/bin/env bash
# O JAR é reprodutível? Constrói duas vezes (numa cópia temporária) com e sem project.build.outputTimestamp e compara o SHA-256.
set -uo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64
T="$(mktemp -d)"; trap 'rm -rf "${T:?}"' EXIT
cp -r "$HERE/../identity-lab" "$T/a"; rm -rf "$T/a/target"
build() { ( cd "$T/a" && mvn -B -ntp -q clean package -DskipTests >/dev/null 2>&1 ); sha256sum "$T/a/target/identity-lab-0.1.0-SNAPSHOT.jar" | cut -c1-16; }
{
  echo "sem project.build.outputTimestamp:"; echo "  build 1: $(build)"; sleep 2; echo "  build 2: $(build)"
  sed -i 's#<java.version>25</java.version>#<java.version>25</java.version>\n    <project.build.outputTimestamp>2026-11-14T00:00:00Z</project.build.outputTimestamp>#' "$T/a/pom.xml"
  echo "com project.build.outputTimestamp:"; echo "  build 1: $(build)"; sleep 2; echo "  build 2: $(build)"
} > "$HERE/out-repro.txt" 2>&1

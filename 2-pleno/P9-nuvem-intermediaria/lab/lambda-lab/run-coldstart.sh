#!/usr/bin/env bash
# P9.05: what a Java cold start is made of, measured LOCALLY (not on Lambda) against moto.
# Needs: JDK 25, Maven (dependency jars come from ../aws-sdk-lab), a running moto (AWS_ENDPOINT_URL).
set -Eeuo pipefail
cd "$(dirname "$0")"
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-25-openjdk-amd64}"; JAVA="$JAVA_HOME/bin/java"
ENDPOINT="${AWS_ENDPOINT_URL:?set AWS_ENDPOINT_URL=http://localhost:<moto-port>}"
N="${N:-7}"
WORK="$(mktemp -d)"; trap 'rm -rf "$WORK"' EXIT
( cd ../aws-sdk-lab && mvn -q -B dependency:build-classpath -Dmdep.outputFile="$WORK/cp.txt" >/dev/null )
CP="$(cat "$WORK/cp.txt")"
"$JAVA_HOME/bin/javac" -cp "$CP" -d "$WORK/classes" ColdStartHandler.java
# CDS/AOT archives cannot contain a classpath DIRECTORY: package the handler as a JAR (like a Lambda deployment package)
"$JAVA_HOME/bin/jar" cf "$WORK/handler.jar" -C "$WORK/classes" .
CPALL="$WORK/handler.jar:$CP"

median() { sort -n | awk '{a[NR]=$1} END {print a[int((NR+1)/2)]}'; }
measure() {            # label, extra java flags...
  local label="$1"; shift
  local lines; lines="$(for _ in $(seq 1 "$N"); do "$JAVA" "$@" -cp "$CPALL" ColdStartHandler "$ENDPOINT" 2>/dev/null; done)"
  local total; total="$(echo "$lines" | sed -E 's/.*total-to-first-response=([0-9]+)ms.*/\1/' | median)"
  local init;  init="$(echo "$lines"  | sed -E 's/.*sdk-client-init=([0-9]+)ms.*/\1/' | median)"
  local first; first="$(echo "$lines" | sed -E 's/.*first-call=([0-9]+)ms.*/\1/' | median)"
  printf '%-44s init=%4sms  first-call=%4sms  total=%5sms  (median of %s)\n' "$label" "$init" "$first" "$total" "$N"
}

echo "# JDK: $("$JAVA" -version 2>&1 | head -1)   cores: $(nproc)   load: $(cut -d' ' -f1 /proc/loadavg)"
echo "# one warm call, for contrast:"; "$JAVA" -cp "$CPALL" ColdStartHandler "$ENDPOINT" 50
echo "# cold start variants ($N runs each, median):"
measure "default (-Xshare:auto, tiered)"
measure "-XX:TieredStopAtLevel=1 (C1 only)" -XX:TieredStopAtLevel=1
measure "-Xshare:off (no class-data sharing at all)" -Xshare:off

# AppCDS: archive the classes of THIS application (dynamic archive at exit), then reuse it.
"$JAVA" -XX:ArchiveClassesAtExit="$WORK/app.jsa" -cp "$CPALL" ColdStartHandler "$ENDPOINT" >/dev/null 2>&1 || echo "# AppCDS archive creation failed"
echo "# AppCDS archive size: $(du -k "$WORK/app.jsa" | cut -f1) KiB"
measure "AppCDS (-XX:SharedArchiveFile)" -XX:SharedArchiveFile="$WORK/app.jsa"
measure "AppCDS + C1 only" -XX:SharedArchiveFile="$WORK/app.jsa" -XX:TieredStopAtLevel=1

# JDK 24+ ahead-of-time cache (JEP 483 class loading+linking; JEP 514/515 make it simpler/profile-aware in 25)
"$JAVA" -XX:AOTCacheOutput="$WORK/app.aot" -cp "$CPALL" ColdStartHandler "$ENDPOINT" >/dev/null 2>&1 || echo "# AOT cache creation failed"
if [ -f "$WORK/app.aot" ]; then
  echo "# AOT cache size: $(du -k "$WORK/app.aot" | cut -f1) KiB"
  measure "AOT cache (-XX:AOTCache)" -XX:AOTCache="$WORK/app.aot"
fi

#!/usr/bin/env bash
# P9.08: how long does the JVM of the PayFlow image keep a stale DNS answer after an RDS failover? Measured with /etc/hosts as the "DNS server".
# Needs Docker, eclipse-temurin:25-jre and a JDK 25 on the host (to compile). Takes ~45 s.
set -Eeuo pipefail
cd "$(dirname "$0")"
JAVAC="${JAVA_HOME:-/usr/lib/jvm/java-25-openjdk-amd64}/bin/javac"
W="$(mktemp -d)"; trap 'rm -rf "${W:?}"' EXIT
"$JAVAC" -d "$W" DnsTtl.java
printf 'networkaddress.cache.ttl=5\n' > "$W/ttl.props"
echo '# A) the default of the PayFlow image (eclipse-temurin:25-jre): no networkaddress.cache.ttl set'
docker run --rm --user root -v "$W:/w:ro" eclipse-temurin:25-jre java -cp /w DnsTtl 60
echo; echo '# B) with -Djava.security.properties=ttl.props (networkaddress.cache.ttl=5; AWS recommends <= 60 s in production)'
docker run --rm --user root -v "$W:/w:ro" eclipse-temurin:25-jre java -Djava.security.properties=/w/ttl.props -cp /w DnsTtl 60

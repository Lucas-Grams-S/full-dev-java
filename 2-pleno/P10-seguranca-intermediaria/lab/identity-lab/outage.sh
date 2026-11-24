#!/usr/bin/env bash
# O que a API faz quando o Keycloak NÃO está disponível: (1) fora do ar desde a subida, (2) volta, (3) cai com o JWKS já em cache.
# Regrava out-outage.txt. Usa o JAR da aplicação (porta 18085) e o Keycloak do laboratório.
set -uo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64
PORT=18085; URL="http://127.0.0.1:$PORT/api/v1/accounts"; LOG="$(mktemp)"
"$HERE/../keycloak-lab/kc-down.sh"
( cd "$HERE" && mvn -B -ntp -q package -DskipTests >/dev/null 2>&1 )
"$JAVA_HOME/bin/java" -jar "$HERE"/target/identity-lab-0.1.0-SNAPSHOT.jar --server.port=$PORT > "$LOG" 2>&1 &
APP=$!
cleanup() { kill "$APP" 2>/dev/null; "$HERE/../keycloak-lab/kc-down.sh"; rm -f "$LOG"; }
trap cleanup EXIT
for i in $(seq 1 30); do grep -q "Started LabApplication" "$LOG" && break; sleep 1; done
token() { curl -s http://127.0.0.1:8180/realms/payflow/protocol/openid-connect/token -d grant_type=password -d client_id=payflow-lab-direct \
            -d username=alice@example.com -d password=alice-lab-pass-1 -d scope="openid transfers:read" | python3 -c "import sys,json;print(json.load(sys.stdin)['access_token'])"; }
code() { curl -s -o /dev/null -w "%{http_code}" "$@"; }
{
  echo "1) Keycloak FORA DO AR desde antes da subida"
  grep -o "Started LabApplication in [0-9.]* seconds" "$LOG"
  echo "   sem token                 -> $(code "$URL")"
  echo "   token qualquer (lixo)     -> $(code -H 'Authorization: Bearer abc.def.ghi' "$URL")   (corpo vazio)"
  echo "   linhas ERROR no log       -> $(grep -c 'JwtDecoderInitializationException' "$LOG") com JwtDecoderInitializationException: $(grep -m1 -o 'Unable to resolve the Configuration with the provided Issuer of "[^"]*"' "$LOG")"
  "$HERE/../keycloak-lab/kc-up.sh" >/dev/null 2>&1
  TOK="$(token)"
  echo "2) Keycloak VOLTA (sem reiniciar a API)"
  echo "   token real                -> $(code -H "Authorization: Bearer $TOK" "$URL")"
  echo "   token qualquer (lixo)     -> $(code -H 'Authorization: Bearer abc.def.ghi' "$URL")"
  docker stop lab-p10-keycloak >/dev/null 2>&1
  echo "3) Keycloak CAI de novo (o JWKS já está em cache)"
  echo "   mesmo token real          -> $(code -H "Authorization: Bearer $TOK" "$URL")"
} > "$HERE/out-outage.txt" 2>&1
rm -rf "${HERE:?}/target"

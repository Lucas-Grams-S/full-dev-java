#!/usr/bin/env bash
# P9.04/P9.06: the SNS/SQS/Secrets Manager adapters against moto, plus two mutants that must make the tests FAIL.
# Needs: Docker (motoserver/moto), JDK 25, Maven. Writes out-sdk.txt. Takes ~2 minutes.
set -Eeuo pipefail
cd "$(dirname "$0")"
OUT="$PWD/out-sdk.txt"; PORT=5090
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-25-openjdk-amd64}" AWS_ACCESS_KEY_ID=test AWS_SECRET_ACCESS_KEY=test AWS_REGION=us-east-1 AWS_ENDPOINT_URL=http://localhost:$PORT AWS_PAGER=
TMP="$(mktemp -d)"
cleanup() { docker rm -f lab-p9-moto-sdk >/dev/null 2>&1 || true; rm -rf "${TMP:?}"; rm -rf "${PWD:?}/target"; }
trap cleanup EXIT
docker rm -f lab-p9-moto-sdk >/dev/null 2>&1 || true
docker run -d --name lab-p9-moto-sdk -p 127.0.0.1:$PORT:5000 motoserver/moto:latest >/dev/null
for _ in $(seq 1 40); do curl -s -o /dev/null http://localhost:$PORT/moto-api/ && break; sleep 0.5; done
{
  echo '$ mvn test   (5 test classes against moto; the clients have NO endpoint/credentials in code: AWS_ENDPOINT_URL does it)'
  mvn -B test -Dtest='*Test' 2>&1 | grep -E '^(T9\.|handler failed)|Tests run:.*(Fail|in com)|BUILD' | sed -E 's/, Time elapsed: [0-9.]+ s//; s/^\[INFO\] //; s/^\[ERROR\] //' | grep -v '^Tests run: [0-9]*, Failures: 0, Errors: 0, Skipped: 0$' > "$TMP/main.txt" || true
  grep -E '^(T9\.)' "$TMP/main.txt" | sort -s -t' ' -k1,1; grep -E '^handler failed' "$TMP/main.txt"; grep -E '^Tests run' "$TMP/main.txt"; grep -E '^BUILD' "$TMP/main.txt"
  echo
  echo '# MUTANT 1: the handler forgets to remember message ids (no idempotency) -> VisibilityTimeoutTest must FAIL'
  rm -rf "${TMP:?}/m1"; mkdir -p "$TMP/m1"; cp -r pom.xml src "$TMP/m1/"
  sed -i 's/if (!processed.add(messageKey))/if (false)/' "$TMP/m1/src/main/java/com/payflow/cloud/IdempotentHandler.java"
  ( cd "$TMP/m1" && mvn -B test -Dtest='VisibilityTimeoutTest' 2>&1 | grep -E 'Tests run:.*Fail|expected|Expecting|BUILD|containsExactly' | head -5 ) || true
  echo
  echo '# MUTANT 2: the worker DELETES the message even when the handler failed -> PoisonMessageTest must FAIL (the poison never reaches the DLQ)'
  rm -rf "${TMP:?}/m2"; mkdir -p "$TMP/m2"; cp -r pom.xml src "$TMP/m2/"
  python3 - "$TMP/m2/src/main/java/com/payflow/cloud/QueueWorker.java" <<'PY'
import sys
p = sys.argv[1]
t = open(p).read()
old = "// keep the message: no delete."
assert old in t
open(p, "w").write(t.replace(old, "sqs.deleteMessage(DeleteMessageRequest.builder().queueUrl(queueUrl).receiptHandle(m.receiptHandle()).build()); //"))
PY
  ( cd "$TMP/m2" && mvn -B test -Dtest='PoisonMessageTest' 2>&1 | grep -E 'Tests run:.*Fail|expected|Expecting|BUILD|containsExactly' | head -5 ) || true
} > "$OUT" 2>&1
echo "wrote $OUT"

#!/usr/bin/env bash
# Reruns EVERY laboratory of P9 and rewrites the out-*.txt files that the lessons quote.
# Needs: Docker (moto, postgres:18, eclipse-temurin:25-jre, trivy, actionlint, shellcheck), Terraform >= 1.14, AWS CLI, JDK 25, Maven.
# Takes ~15 minutes. Run it on a quiet machine (the cold-start and replication numbers depend on it).
set -Eeuo pipefail
cd "$(dirname "$0")"
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-25-openjdk-amd64}"
echo "== cidr-lab";     "$JAVA_HOME/bin/java" cidr-lab/CidrLab.java > cidr-lab/out-cidr.txt
echo "== jvm-budget";   cloud-lab/jvm-budget.sh > cloud-lab/out-jvm.txt
echo "== cloud-lab";    cloud-lab/run-cloud.sh
echo "== aws-sdk-lab";  aws-sdk-lab/run-sdk.sh
echo "== replica-lab";  replica-lab/run-replica.sh > replica-lab/out-replica.txt 2>&1
echo "== dns";          replica-lab/run-dns.sh > replica-lab/out-dns.txt 2>&1
echo "== lambda-lab (cold start)"
docker rm -f lab-p9-moto-cs >/dev/null 2>&1 || true
docker run -d --name lab-p9-moto-cs -p 127.0.0.1:5091:5000 motoserver/moto:latest >/dev/null
for _ in $(seq 1 40); do curl -s -o /dev/null http://localhost:5091/moto-api/ && break; sleep 0.5; done
AWS_ENDPOINT_URL=http://localhost:5091 AWS_ACCESS_KEY_ID=test AWS_SECRET_ACCESS_KEY=test AWS_REGION=us-east-1 lambda-lab/run-coldstart.sh 2>&1 | grep -vE "WARNING|SLF4J" > lambda-lab/out-coldstart.txt
docker rm -f lab-p9-moto-cs >/dev/null 2>&1 || true
echo "== pipeline-lab (actionlint + shellcheck)"
{
  echo '$ actionlint .github/workflows/deploy-ecs.yml'
  docker run --rm -v "$PWD/pipeline-lab:/repo:ro" -w /repo rhysd/actionlint:1.7.12 .github/workflows/deploy-ecs.yml && echo "(clean)"
  echo '$ shellcheck scripts/ci/ecs-deploy.sh scripts/ci/smoke-test.sh'
  docker run --rm -v "$PWD/pipeline-lab:/repo:ro" -w /repo koalaman/shellcheck:stable scripts/ci/ecs-deploy.sh scripts/ci/smoke-test.sh && echo "(clean)"
  echo '# MUTANT: an unquoted variable and a missing needs target must be caught'
  tmp="$(mktemp -d)"; cp -r pipeline-lab/. "$tmp/"; chmod -R a+rX "$tmp"
  sed -i 's/needs: staging/needs: stagng/' "$tmp/.github/workflows/deploy-ecs.yml"
  sed -i 's|> "$tmp/current.json"|> $tmp/current.json|' "$tmp/scripts/ci/ecs-deploy.sh"
  docker run --rm -v "$tmp:/repo:ro" -w /repo rhysd/actionlint:1.7.12 .github/workflows/deploy-ecs.yml | head -3 || true
  docker run --rm -v "$tmp:/repo:ro" -w /repo koalaman/shellcheck:stable scripts/ci/ecs-deploy.sh | grep -E "^In |SC[0-9]+" | head -4 || true
  rm -rf "${tmp:?}"
} > pipeline-lab/out-lint.txt 2>&1
echo "done"

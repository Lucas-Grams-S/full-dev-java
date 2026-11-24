#!/usr/bin/env bash
# Roda NA INSTANCIA (como root, via SSM Run Command). Uso: deploy.sh <tag-da-imagem>
# Variaveis para teste local: PAYFLOW_DIR, AWS_REGION, REGISTRY, SKIP_ECR_LOGIN=1, PULL_POLICY=never, COMPOSE_EXTRA="-f arquivo"
set -Eeuo pipefail

TAG="${1:?usage: deploy.sh <image-tag>}"
APP_DIR="${PAYFLOW_DIR:-/opt/payflow}"
cd "$APP_DIR"

imds() {   # IMDSv2: pega um token de sessao e so entao le o metadado
  local token
  token="$(curl -fsS -X PUT http://169.254.169.254/latest/api/token -H 'X-aws-ec2-metadata-token-ttl-seconds: 60')"
  curl -fsS -H "X-aws-ec2-metadata-token: ${token}" "http://169.254.169.254/latest/meta-data/$1"
}
AWS_REGION="${AWS_REGION:-$(imds placement/region)}"
export AWS_REGION AWS_DEFAULT_REGION="$AWS_REGION"
REGISTRY="${REGISTRY:-$(aws sts get-caller-identity --query Account --output text).dkr.ecr.${AWS_REGION}.amazonaws.com}"

param() { aws ssm get-parameter --name "/payflow/$1" --with-decryption --query Parameter.Value --output text; }

echo ">> secrets"
umask 077
mkdir -p secrets
param db_password > secrets/db_password
param jwt_secret  > secrets/jwt_secret
DB_HOST="$(param db_host)"
# arquivo .pgpass (usado so pelo bootstrap-admin.sh, que roda psql): host:porta:banco:usuario:senha
printf '%s:5432:payflow:payflow:%s\n' "$DB_HOST" "$(cat secrets/db_password)" > secrets/pgpass
# o container roda como uid 10001 (J9.02): o arquivo precisa ser DELE e so dele ler
chown 10001:10001 secrets/db_password secrets/jwt_secret
chmod 400 secrets/db_password secrets/jwt_secret secrets/pgpass

echo ">> image ${REGISTRY}/payflow:${TAG}"
if [[ "${SKIP_ECR_LOGIN:-0}" != "1" ]]; then
  aws ecr get-login-password | docker login --username AWS --password-stdin "$REGISTRY"
fi
[[ -f .env ]] && cp .env .env.previous
printf 'PAYFLOW_IMAGE=%s/payflow:%s\nDB_HOST=%s\n' "$REGISTRY" "$TAG" "$DB_HOST" > .env

# COMPOSE_EXTRA e palavra-solta de proposito (varios argumentos)
# shellcheck disable=SC2086
compose() { docker compose --env-file .env -f compose.prod.yaml ${COMPOSE_EXTRA:-} "$@"; }

echo ">> up"
if compose up -d --pull "${PULL_POLICY:-always}" --wait --wait-timeout 120; then
  echo ">> deployed ${TAG}"
else
  echo "!! deploy failed; logs:" >&2
  compose logs --tail 80 app >&2 || true
  if [[ -f .env.previous ]]; then
    echo "!! rolling back to the previous image" >&2
    cp .env.previous .env
    compose up -d --wait --wait-timeout 120 || true
  fi
  exit 1
fi

#!/usr/bin/env bash
# Gera os segredos de DESENVOLVIMENTO do Compose em ./secrets (fora do Git e fora do contexto de build).
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p secrets
[ -f secrets/db_password ] || printf 'db-%s' "$(head -c 18 /dev/urandom | base64 | tr -dc 'A-Za-z0-9' | head -c 24)" > secrets/db_password
[ -f secrets/jwt_secret ]  || head -c 48 /dev/urandom | base64 | tr -d '\n' > secrets/jwt_secret
# formato do .pgpass: host:porta:banco:usuario:senha
printf 'db:5432:payflow:payflow:%s\n' "$(cat secrets/db_password)" > secrets/pgpass
# O Compose monta cada arquivo como bind mount (uid e mode do compose.yaml sao ignorados) e o processo da
# aplicacao roda com o uid 10001: o arquivo precisa ser legivel por "outros". So vale para dev local.
chmod 644 secrets/db_password secrets/jwt_secret
chmod 600 secrets/pgpass
echo "segredos de desenvolvimento em ./secrets (ignorados pelo Git e pelo .dockerignore)"

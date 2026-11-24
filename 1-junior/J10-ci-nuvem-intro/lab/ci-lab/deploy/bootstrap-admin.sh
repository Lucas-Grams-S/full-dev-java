#!/usr/bin/env bash
# Roda NA INSTANCIA (via sessao SSM). Uso: sudo bash bootstrap-admin.sh <email> "<nome>" '{bcrypt}$2a$10$...'
# O hash e gerado no SEU computador (J10.06); a senha em texto nunca chega ao servidor.
set -Eeuo pipefail
EMAIL="${1:?email}" FULL_NAME="${2:?name}" HASH="${3:?bcrypt hash with the {bcrypt} prefix}"
cd "${PAYFLOW_DIR:-/opt/payflow}"
set -a; . ./.env; set +a
docker run --rm -i \
  -e PGHOST="$DB_HOST" -e PGUSER=payflow -e PGDATABASE=payflow -e PGSSLMODE=verify-full \
  -e PGSSLROOTCERT=/certs/global-bundle.pem -e PGPASSFILE=/run/secrets/pgpass \
  -v "$PWD/certs/global-bundle.pem:/certs/global-bundle.pem:ro" \
  -v "$PWD/secrets/pgpass:/run/secrets/pgpass:ro" \
  postgres:18 psql -v email="$EMAIL" -v full_name="$FULL_NAME" -v hash="$HASH" -f - < "$(dirname "$0")/bootstrap-admin.sql"

#!/usr/bin/env bash
# Helpers dos experimentos: banco 'payflow' no container lab-p2-pg (postgres:18).
# Uso: source run.sh ; reset ; psql_a <<< "..." ; psql_b <<< "..."
PSQL="docker exec -i lab-p2-pg psql -U postgres -d payflow -X -q -v ON_ERROR_STOP=0"
reset() {
  docker exec -i lab-p2-pg psql -U postgres -d postgres -X -q -c "DROP DATABASE IF EXISTS payflow WITH (FORCE)" -c "CREATE DATABASE payflow" >/dev/null
  $PSQL -f - < "$(dirname "${BASH_SOURCE[0]}")/00-schema.sql" >/dev/null
  $PSQL -f - < "$(dirname "${BASH_SOURCE[0]}")/01-seed.sql" >/dev/null
}
psql_a() { $PSQL "$@" 2>&1 | sed 's/^/[A] /'; }
psql_b() { $PSQL "$@" 2>&1 | sed 's/^/[B] /'; }

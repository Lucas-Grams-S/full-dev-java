#!/usr/bin/env bash
# PostgreSQL 18 só para o laboratório de rotação (porta 55432). Remove com pg-down.sh.
set -euo pipefail
docker rm -f lab-p10-pg >/dev/null 2>&1 || true
docker run -d --name lab-p10-pg -p 127.0.0.1:55432:5432 -e POSTGRES_PASSWORD=lab-pg-admin postgres:18 >/dev/null
for i in $(seq 1 40); do
  if docker exec lab-p10-pg pg_isready -U postgres >/dev/null 2>&1; then sleep 2; echo "postgres ready"; exit 0; fi
  sleep 1
done
echo "postgres did not become ready" >&2; exit 1

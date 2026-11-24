#!/usr/bin/env bash
# S3.09 (alternative D): a GENERATED column instead of trigger + backfill, on PostgreSQL 18 (220k users).
set -Eeuo pipefail; cd "$(dirname "$0")"; . ./lib.sh
OUT=${OUT:-.}
docker exec "$LAB_PG" bash -c 'pkill pgbench >/dev/null 2>&1 || true'
reset_db
psqlx < V18__expand_users_phone_e164.sql
{
echo '## F) the alternative that looks easier: a GENERATED column (PostgreSQL 18)'
psqlx <<'SQL'
\echo '-- VIRTUAL (computed on read, no table rewrite) with the user-defined normalize_phone_br:'
\set ON_ERROR_STOP off
ALTER TABLE users ADD COLUMN phone_virtual text GENERATED ALWAYS AS (normalize_phone_br(phone)) VIRTUAL;
\set ON_ERROR_STOP on
\echo '-- STORED (computed on write): does the table file change, and how long does ACCESS EXCLUSIVE last?'
SELECT pg_relation_filenode('users') AS f0 \gset
\timing on
ALTER TABLE users ADD COLUMN phone_stored text GENERATED ALWAYS AS (normalize_phone_br(phone)) STORED;
\timing off
SELECT 'table file replaced (rewrite): ' || (pg_relation_filenode('users') <> :'f0');
\echo '-- a generated column cannot be written by the application:'
\set ON_ERROR_STOP off
UPDATE users SET phone_stored = '+5511999999999' WHERE id = (SELECT id FROM users LIMIT 1);
SQL
} > "$OUT/out-s309-generated.txt" 2>&1
cat "$OUT/out-s309-generated.txt"

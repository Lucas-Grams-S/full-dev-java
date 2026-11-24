#!/usr/bin/env bash
# S3.07: the one measurement that belongs to "pick the right store": a BRIN index on an append-only time column versus a B-tree (3M-row ledger).
set -Eeuo pipefail; cd "$(dirname "$0")"; . ./lib.sh
OUT=${OUT:-.}
docker exec "$LAB_PG" bash -c 'pkill pgbench >/dev/null 2>&1 || true'
reset_db
{
psqlx <<'SQL'
\pset pager off
\echo '## A) physical correlation: BRIN only works if the column follows the physical order of the table'
SELECT 'ledger_entries.created_at correlation: ' || round(correlation::numeric, 4) FROM pg_stats WHERE tablename = 'ledger_entries' AND attname = 'created_at';
SELECT 'ledger_entries.account_id correlation: ' || round(correlation::numeric, 4) FROM pg_stats WHERE tablename = 'ledger_entries' AND attname = 'account_id';
\echo
\echo '## B) size and build time: B-tree x BRIN on created_at'
\timing on
CREATE INDEX idx_ledger_created_btree ON ledger_entries (created_at);
CREATE INDEX idx_ledger_created_brin ON ledger_entries USING brin (created_at) WITH (pages_per_range = 32);
\timing off
SELECT 'B-tree: ' || pg_size_pretty(pg_relation_size('idx_ledger_created_btree')) || ' | BRIN: ' || pg_size_pretty(pg_relation_size('idx_ledger_created_brin')) || ' | table: ' || pg_size_pretty(pg_relation_size('ledger_entries'));
\echo
\echo '## B2) the same rows physically sorted by created_at (what a long-lived append-only table looks like)'
CREATE TABLE ledger_sorted AS SELECT * FROM ledger_entries ORDER BY created_at;
ANALYZE ledger_sorted;
SELECT 'ledger_sorted.created_at correlation: ' || round(correlation::numeric, 4) FROM pg_stats WHERE tablename = 'ledger_sorted' AND attname = 'created_at';
CREATE INDEX idx_sorted_brin ON ledger_sorted USING brin (created_at) WITH (pages_per_range = 32);
SELECT 'BRIN on the sorted table: ' || pg_size_pretty(pg_relation_size('idx_sorted_brin')) || ' | table: ' || pg_size_pretty(pg_relation_size('ledger_sorted'));
EXPLAIN (ANALYZE, COSTS off, TIMING off, BUFFERS off, SUMMARY on) SELECT count(*), sum(amount) FROM ledger_sorted WHERE created_at >= '2026-09-10' AND created_at < '2026-09-11';
\echo
\echo '## C) a one-day range query (about 4,100 rows)'
SET enable_indexscan = off; SET enable_bitmapscan = on; SET max_parallel_workers_per_gather = 0;
\echo '-- B-tree only (BRIN hidden)'
BEGIN; DROP INDEX idx_ledger_created_brin;
EXPLAIN (ANALYZE, COSTS off, TIMING off, BUFFERS off, SUMMARY on) SELECT count(*), sum(amount) FROM ledger_entries WHERE created_at >= '2026-09-10' AND created_at < '2026-09-11';
ROLLBACK;
\echo '-- BRIN only (B-tree hidden)'
BEGIN; DROP INDEX idx_ledger_created_btree;
EXPLAIN (ANALYZE, COSTS off, TIMING off, BUFFERS off, SUMMARY on) SELECT count(*), sum(amount) FROM ledger_entries WHERE created_at >= '2026-09-10' AND created_at < '2026-09-11';
ROLLBACK;
\echo '-- no index at all'
BEGIN; DROP INDEX idx_ledger_created_btree; DROP INDEX idx_ledger_created_brin;
EXPLAIN (ANALYZE, COSTS off, TIMING off, BUFFERS off, SUMMARY on) SELECT count(*), sum(amount) FROM ledger_entries WHERE created_at >= '2026-09-10' AND created_at < '2026-09-11';
ROLLBACK;
\echo
\echo '## D) BRIN on a column that does NOT follow the physical order (account_id): useless'
CREATE INDEX idx_ledger_account_brin ON ledger_entries USING brin (account_id);
SELECT account_id AS acc FROM ledger_entries OFFSET 5000 LIMIT 1 \gset
EXPLAIN (ANALYZE, COSTS off, TIMING off, BUFFERS off, SUMMARY on) SELECT count(*) FROM ledger_entries WHERE account_id = :'acc';
SQL
} > "$OUT/out-s307-brin.txt" 2>&1
cat "$OUT/out-s307-brin.txt" | cut -c1-200

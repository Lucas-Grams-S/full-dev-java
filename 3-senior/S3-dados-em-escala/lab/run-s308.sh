#!/usr/bin/env bash
# S3.08: analytics on top of OLTP data: aggregate on the ledger x materialized view x incremental summary table, and the late-data trap.
set -Eeuo pipefail; cd "$(dirname "$0")"; . ./lib.sh
OUT=${OUT:-.}
docker exec "$LAB_PG" bash -c 'pkill pgbench >/dev/null 2>&1 || true'
reset_db
{
psqlx <<'SQL'
\pset pager off
\echo '## A) "volume per day and type" straight on the 3M-row ledger (the OLTP table)'
SET max_parallel_workers_per_gather = 2;
EXPLAIN (ANALYZE, COSTS off, TIMING off, BUFFERS off, SUMMARY on) SELECT created_at::date AS day, type, count(*), sum(amount) FROM ledger_entries GROUP BY 1, 2;

\echo
\echo '## B) materialized view: build, refresh and read'
\timing on
CREATE MATERIALIZED VIEW mv_daily_volume AS SELECT created_at::date AS day, type, count(*) AS entries, sum(amount) AS amount FROM ledger_entries GROUP BY 1, 2;
CREATE UNIQUE INDEX uk_mv_daily_volume ON mv_daily_volume (day, type);
\echo 'REFRESH (blocks readers while it runs):'
REFRESH MATERIALIZED VIEW mv_daily_volume;
\echo 'REFRESH ... CONCURRENTLY (readers keep reading the old copy; needs the unique index):'
REFRESH MATERIALIZED VIEW CONCURRENTLY mv_daily_volume;
\echo 'read one month of the view:'
SELECT day, type, entries, amount FROM mv_daily_volume WHERE day >= '2026-10-01' AND day < '2026-10-03' ORDER BY day, type;
\timing off
SELECT 'view: ' || (SELECT count(*) FROM mv_daily_volume) || ' rows, ' || pg_size_pretty(pg_total_relation_size('mv_daily_volume'));

\echo
\echo '## C) summary table fed by an incremental job and the late-data trap'
CREATE TABLE daily_volume (day date NOT NULL, type text NOT NULL, entries bigint NOT NULL, amount numeric(19,2) NOT NULL, PRIMARY KEY (day, type));
INSERT INTO daily_volume SELECT created_at::date, type, count(*), sum(amount) FROM ledger_entries GROUP BY 1, 2;
SELECT 'after the first full load: summary = ' || sum(entries) || ' entries, ledger = ' || (SELECT count(*) FROM ledger_entries) FROM daily_volume;
SELECT max(created_at) AS wm FROM ledger_entries \gset
\echo '-- 100 on-time entries arrive (2026-11-01 08:00) ...'
INSERT INTO ledger_entries (account_id, type, amount, created_at) SELECT id, 'CREDIT', 1.00, timestamptz '2026-11-01 08:00' FROM accounts LIMIT 100;
\echo '-- 500 entries commit LATE: created_at is yesterday-ish (2026-10-30) but they arrive after the job ran; 1 entry is MUCH later (2026-09-01)'
INSERT INTO ledger_entries (account_id, type, amount, created_at) SELECT id, 'CREDIT', 1.00, timestamptz '2026-10-30 12:00' FROM accounts LIMIT 500;
INSERT INTO ledger_entries (account_id, type, amount, created_at) SELECT id, 'CREDIT', 1.00, timestamptz '2026-09-01 12:00' FROM accounts LIMIT 1;
\echo '-- job v1: "add everything with created_at > watermark (the max created_at of the last run)" (+= semantics)'
INSERT INTO daily_volume SELECT created_at::date, type, count(*), sum(amount) FROM ledger_entries WHERE created_at > :'wm' GROUP BY 1, 2
  ON CONFLICT (day, type) DO UPDATE SET entries = daily_volume.entries + EXCLUDED.entries, amount = daily_volume.amount + EXCLUDED.amount;
SELECT 'job v1: summary = ' || sum(entries) || ', ledger = ' || (SELECT count(*) FROM ledger_entries) || ', MISSING ' || ((SELECT count(*) FROM ledger_entries) - sum(entries)) FROM daily_volume;
\echo '-- job v2: recompute the last 2 days from scratch (idempotent upsert with =, not +=)'
INSERT INTO daily_volume SELECT created_at::date, type, count(*), sum(amount) FROM ledger_entries WHERE created_at >= (:'wm'::timestamptz)::date - 2 GROUP BY 1, 2
  ON CONFLICT (day, type) DO UPDATE SET entries = EXCLUDED.entries, amount = EXCLUDED.amount;
SELECT 'job v2: summary = ' || sum(entries) || ', ledger = ' || (SELECT count(*) FROM ledger_entries) || ', MISSING ' || ((SELECT count(*) FROM ledger_entries) - sum(entries)) FROM daily_volume;
\echo '-- the 1 entry outside the window is caught only by the reconciliation (full compare, day by day):'
SELECT l.day, l.type, l.entries AS ledger_entries, d.entries AS summary_entries
FROM (SELECT created_at::date AS day, type, count(*) AS entries FROM ledger_entries GROUP BY 1, 2) l
JOIN daily_volume d USING (day, type) WHERE l.entries <> d.entries;
SQL
} > "$OUT/out-s308-analytics.txt" 2>&1
cat "$OUT/out-s308-analytics.txt" | cut -c1-200

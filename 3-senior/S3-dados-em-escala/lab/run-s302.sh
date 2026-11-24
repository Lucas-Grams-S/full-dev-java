#!/usr/bin/env bash
# S3.02: native partitioning on PostgreSQL 18, measured on the 3M-row ledger (24 months).
set -Eeuo pipefail; cd "$(dirname "$0")"; . ./lib.sh
OUT=${OUT:-.}
reset_db
{
psqlx <<'SQL'
\pset pager off
\echo '## A) the partitioned copy of the ledger: RANGE (created_at), one partition per month + DEFAULT'
CREATE TABLE ledger_plain (LIKE ledger_entries INCLUDING DEFAULTS INCLUDING CONSTRAINTS INCLUDING INDEXES);
INSERT INTO ledger_plain SELECT * FROM ledger_entries;

CREATE TABLE ledger_p (LIKE ledger_entries INCLUDING DEFAULTS INCLUDING CONSTRAINTS) PARTITION BY RANGE (created_at);
CREATE FUNCTION create_month_partitions(parent regclass, first_month date, n int) RETURNS int LANGUAGE plpgsql AS $$
DECLARE m date; made int := 0; BEGIN
  FOR i IN 0 .. n - 1 LOOP
    m := (first_month + make_interval(months => i))::date;
    EXECUTE format('CREATE TABLE IF NOT EXISTS %I PARTITION OF %s FOR VALUES FROM (%L) TO (%L)',
                   replace(parent::text, '.', '_') || '_' || to_char(m, 'YYYY_MM'), parent, m, (m + interval '1 month')::date);
    made := made + 1;
  END LOOP; RETURN made; END $$;
SELECT create_month_partitions('ledger_p', date '2024-11-01', 25) AS partitions_created;
CREATE TABLE ledger_p_default PARTITION OF ledger_p DEFAULT;
\echo '-- the primary key MUST contain the partition key:'
DO $$ BEGIN ALTER TABLE ledger_p ADD CONSTRAINT pk_ledger_p PRIMARY KEY (id); EXCEPTION WHEN OTHERS THEN RAISE NOTICE '%', SQLERRM; END $$;
ALTER TABLE ledger_p ADD CONSTRAINT pk_ledger_p PRIMARY KEY (id, created_at);
CREATE INDEX idx_ledger_p_account_created ON ledger_p (account_id, created_at DESC);
\timing on
INSERT INTO ledger_p SELECT * FROM ledger_entries;
\timing off
ANALYZE ledger_p; ANALYZE ledger_plain;
SELECT 'rows in default partition: ' || count(*) FROM ledger_p_default;
SELECT 'plain table+indexes: ' || pg_size_pretty(pg_total_relation_size('ledger_plain')) || ' | partitioned total: ' ||
       pg_size_pretty(sum(pg_total_relation_size(c.oid))) FROM pg_class c JOIN pg_inherits i ON i.inhrelid = c.oid WHERE i.inhparent = 'ledger_p'::regclass;

\echo
\echo '## B) pruning: a month-sized aggregate (no useful index on created_at alone in the plain table)'
\echo '-- plain table'
EXPLAIN (ANALYZE, SUMMARY, COSTS off, TIMING off, BUFFERS off) SELECT count(*), sum(amount) FROM ledger_plain WHERE created_at >= '2026-09-01' AND created_at < '2026-10-01';
\echo '-- partitioned table'
EXPLAIN (ANALYZE, SUMMARY, COSTS off, TIMING off, BUFFERS off) SELECT count(*), sum(amount) FROM ledger_p WHERE created_at >= '2026-09-01' AND created_at < '2026-10-01';
\echo
\echo '-- the statement of one account, last 3 months (the PayFlow query): partitioned, WITH and WITHOUT the date filter'
SELECT account_id AS acc FROM ledger_p WHERE created_at >= '2026-08-01' LIMIT 1 OFFSET 1000 \gset
\echo '(plans summarized below by the shell: how many partitions each one touched)'
SQL
} > "$OUT/out-s302-partitioning.txt" 2>&1
ACC=$(psqlx -Atc "select account_id from ledger_p where created_at >= '2026-08-01' limit 1 offset 1000")
summ() { psqlx -At -c "EXPLAIN (ANALYZE, SUMMARY, COSTS off, TIMING off, BUFFERS off) $1" |
  awk '/Scan/ && !/Bitmap Index Scan/ { if (match($0, / on [a-z_0-9]+/)) { t = substr($0, RSTART + 4, RLENGTH - 4); seen[t] = 1 } }
       /Planning Time/ { p = $3 } /Execution Time/ { e = $3 }
       END { n = 0; for (k in seen) n++; printf "partitions read: %d | planning %s ms | execution %s ms\n", n, p, e }'; }
{
echo '-- statement of ONE account WITH an open-ended date filter (created_at >= 2026-08-01):'
summ "SELECT * FROM ledger_p WHERE account_id = '$ACC' AND created_at >= '2026-08-01' ORDER BY created_at DESC LIMIT 50"
echo '-- ... with a CLOSED range (2026-08-01 <= created_at < 2026-11-01):'
summ "SELECT * FROM ledger_p WHERE account_id = '$ACC' AND created_at >= '2026-08-01' AND created_at < '2026-11-01' ORDER BY created_at DESC LIMIT 50"
echo '-- ... WITHOUT any date filter:'
summ "SELECT * FROM ledger_p WHERE account_id = '$ACC' ORDER BY created_at DESC LIMIT 50"
echo '-- the same query on the plain (non-partitioned) table:'
summ "SELECT * FROM ledger_plain WHERE account_id = '$ACC' ORDER BY created_at DESC LIMIT 50"
} >> "$OUT/out-s302-partitioning.txt" 2>&1
{
psqlx <<'SQL'
\echo
\echo
\echo '## C) a unique constraint that does not include the partition key is refused'
DO $$ BEGIN CREATE UNIQUE INDEX uk_ledger_p_transfer ON ledger_p (transfer_id, type); EXCEPTION WHEN OTHERS THEN RAISE NOTICE '%', SQLERRM; END $$;

\echo
\echo '## D) retention: remove the 8 oldest months'
\echo '-- plain table: DELETE in one statement'
SELECT pg_current_wal_lsn() AS l0 \gset
\timing on
DELETE FROM ledger_plain WHERE created_at < '2025-07-01';
\timing off
SELECT 'WAL for the DELETE: ' || pg_size_pretty(pg_current_wal_lsn() - :'l0'::pg_lsn) AS delete_wal \gset
\echo :delete_wal
\echo '-- partitioned table: DETACH CONCURRENTLY with a DEFAULT partition present'
\set ON_ERROR_STOP off
ALTER TABLE ledger_p DETACH PARTITION ledger_p_2024_11 CONCURRENTLY;
\set ON_ERROR_STOP on
\echo '-- the default partition is empty (measured above): drop it, then DETACH CONCURRENTLY works'
DROP TABLE ledger_p_default;
SELECT pg_current_wal_lsn() AS l1 \gset
\timing on
ALTER TABLE ledger_p DETACH PARTITION ledger_p_2024_11 CONCURRENTLY;
DROP TABLE ledger_p_2024_11;
DO $$ DECLARE r record; BEGIN FOR r IN SELECT c.relname FROM pg_inherits i JOIN pg_class c ON c.oid = i.inhrelid
   WHERE i.inhparent = 'ledger_p'::regclass AND c.relname < 'ledger_p_2025_07' AND c.relname ~ '20(24|25)_' ORDER BY 1 LOOP
   EXECUTE format('ALTER TABLE ledger_p DETACH PARTITION %I', r.relname); EXECUTE format('DROP TABLE %I', r.relname); END LOOP; END $$;
\timing off
SELECT 'WAL for the DETACH/DROP: ' || pg_size_pretty(pg_current_wal_lsn() - :'l1'::pg_lsn);
\echo
\echo '## H) with NO default partition, a row outside every range is REFUSED (the monitoring must create partitions ahead of time)'
DO $$ BEGIN INSERT INTO ledger_p (account_id, type, amount, created_at) SELECT id, 'CREDIT', 1, timestamptz '2030-01-15' FROM accounts LIMIT 1;
  EXCEPTION WHEN OTHERS THEN RAISE NOTICE '%', SQLERRM; END $$;
SELECT 'partitions left: ' || count(*) FROM pg_inherits WHERE inhparent = 'ledger_p'::regclass;
SELECT 'rows left: ' || count(*) FROM ledger_p;
SQL
} >> "$OUT/out-s302-partitioning.txt" 2>&1

reset_db
{
psqlx <<'SQL'
\pset pager off
\echo '## E) migrating the EXISTING table without a rewrite: ATTACH PARTITION, with and without a validated CHECK (1 year = ~1.4M rows)'
CREATE TABLE ledger_p (LIKE ledger_entries INCLUDING DEFAULTS INCLUDING CONSTRAINTS) PARTITION BY RANGE (created_at);
ALTER TABLE ledger_p ADD PRIMARY KEY (id, created_at);
\echo '-- E1) ATTACH without a CHECK: PostgreSQL scans the table to prove every row fits the bound'
CREATE TABLE ledger_y1 (LIKE ledger_p INCLUDING DEFAULTS INCLUDING CONSTRAINTS);
INSERT INTO ledger_y1 SELECT * FROM ledger_entries WHERE created_at >= '2025-01-01' AND created_at < '2026-01-01';
ALTER TABLE ledger_y1 ADD PRIMARY KEY (id, created_at);
BEGIN;
\timing on
ALTER TABLE ledger_p ATTACH PARTITION ledger_y1 FOR VALUES FROM ('2025-01-01') TO ('2026-01-01');
\timing off
SELECT 'E1 locks: ' || string_agg(c.relname || '=' || l.mode, ', ' ORDER BY 1) FROM pg_locks l JOIN pg_class c ON c.oid = l.relation WHERE c.relname IN ('ledger_p','ledger_y1') AND l.pid = pg_backend_pid() AND l.granted AND l.mode NOT LIKE 'Access%Share';
COMMIT;
ALTER TABLE ledger_p DETACH PARTITION ledger_y1;
\echo '-- E2) with a validated CHECK equal to the bound: the scan is skipped'
ALTER TABLE ledger_y1 ADD CONSTRAINT ck_y1_bound CHECK (created_at >= '2025-01-01' AND created_at < '2026-01-01') NOT VALID;
ALTER TABLE ledger_y1 VALIDATE CONSTRAINT ck_y1_bound;
\timing on
ALTER TABLE ledger_p ATTACH PARTITION ledger_y1 FOR VALUES FROM ('2025-01-01') TO ('2026-01-01');
\timing off
ALTER TABLE ledger_y1 DROP CONSTRAINT ck_y1_bound;
SELECT 'attached rows: ' || count(*) FROM ledger_p;
\echo
\echo '## F) hash partitioning by account_id (16 partitions): the statement of ONE account WITHOUT a date'
CREATE TABLE ledger_h (LIKE ledger_entries INCLUDING DEFAULTS INCLUDING CONSTRAINTS) PARTITION BY HASH (account_id);
SELECT format('CREATE TABLE ledger_h_%s PARTITION OF ledger_h FOR VALUES WITH (MODULUS 16, REMAINDER %s)', i, i) FROM generate_series(0, 15) i \gexec
ALTER TABLE ledger_h ADD PRIMARY KEY (id, account_id);
CREATE INDEX ON ledger_h (account_id, created_at DESC);
INSERT INTO ledger_h SELECT * FROM ledger_entries;
ANALYZE ledger_h;
SQL
} > "$OUT/out-s302-attach-hash.txt" 2>&1
ACC=$(psqlx -Atc "select account_id from ledger_h limit 1 offset 777")
{
echo "-- ONE account, no date, hash by account_id:"
summ "SELECT * FROM ledger_h WHERE account_id = '$ACC' ORDER BY created_at DESC LIMIT 50"
echo "-- the month-sized aggregate by DATE on the hash table:"
summ "SELECT count(*), sum(amount) FROM ledger_h WHERE created_at >= '2026-09-01' AND created_at < '2026-10-01'"
} >> "$OUT/out-s302-attach-hash.txt" 2>&1
cat "$OUT"/out-s302-*.txt

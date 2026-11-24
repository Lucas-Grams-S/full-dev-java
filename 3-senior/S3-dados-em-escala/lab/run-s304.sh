#!/usr/bin/env bash
# S3.04: archiving a ledger partition: verify, export, restore-test, drop; and who is allowed to do it.
set -Eeuo pipefail; cd "$(dirname "$0")"; . ./lib.sh
OUT=${OUT:-.}
reset_db
psqlx < setup-partitioned.sql >/dev/null
docker exec "$LAB_PG" bash -c 'mkdir -p /tmp/arch && chown postgres /tmp/arch && rm -f /tmp/arch/*'
{
echo '## A) who may remove data: the application role (not the owner) tries DELETE, DETACH and DROP'
psqlx <<'SQL'
CREATE ROLE payflow_app LOGIN PASSWORD 'app';
GRANT SELECT, INSERT ON ledger_p TO payflow_app;
SET ROLE payflow_app;
\set ON_ERROR_STOP off
DELETE FROM ledger_p WHERE created_at < '2024-12-01';
ALTER TABLE ledger_p DETACH PARTITION ledger_p_2024_11;
DROP TABLE ledger_p_2024_11;
RESET ROLE;
SQL
echo
echo '## B) archive the oldest month (ledger_p_2024_11) in 5 verified steps'
T0=$(now_ms)
echo '-- 1. fingerprint BEFORE (row count, sum, md5 of every row in id order)'
psqlx -At -F' | ' -c "select count(*), sum(amount), md5(string_agg(id::text || '|' || type || '|' || amount::text || '|' || created_at::text, ',' ORDER BY id)) from ledger_p_2024_11" | tee /tmp/fp_before.$$
echo '-- 1b. closing snapshot per account for that month: "balance = sum of the ledger" must survive the archive'
NETSQL="sum(CASE type WHEN 'CREDIT' THEN amount ELSE -amount END)"
psqlx -c "CREATE TABLE period_balances (account_id uuid NOT NULL, period date NOT NULL, net numeric(19,2) NOT NULL, PRIMARY KEY (account_id, period))" \
      -c "INSERT INTO period_balances SELECT account_id, date '2024-11-01', $NETSQL FROM ledger_p_2024_11 GROUP BY account_id"
TOTAL_BEFORE=$(psqlx -Atc "select $NETSQL from ledger_p")
echo "   net total of the WHOLE ledger before the archive: $TOTAL_BEFORE"
echo '-- 2. DETACH PARTITION CONCURRENTLY (queries on the ledger keep working; the month leaves the parent)'
psqlx -c "ALTER TABLE ledger_p DETACH PARTITION ledger_p_2024_11 CONCURRENTLY"
echo "   rows of that month still reachable through ledger_p: $(psqlx -Atc "select count(*) from ledger_p where created_at < '2024-12-01'")"
echo '-- 3. export to compressed CSV + sha256'
T1=$(now_ms)
psqlx -c "COPY ledger_p_2024_11 TO PROGRAM 'gzip -c > /tmp/arch/ledger_2024_11.csv.gz' CSV HEADER"
docker exec "$LAB_PG" bash -c 'cd /tmp/arch && sha256sum ledger_2024_11.csv.gz > ledger_2024_11.sha256 && cat ledger_2024_11.sha256 && ls -l --block-size=K ledger_2024_11.csv.gz | awk "{print \"   csv.gz size: \" \$5}"'
echo "   table size: $(psqlx -Atc "select pg_size_pretty(pg_total_relation_size('ledger_p_2024_11'))")"
echo "   export took $(( $(now_ms) - T1 )) ms"
echo '-- 4. restore test: load the file into a scratch table and compare the fingerprint'
T2=$(now_ms)
psqlx -c "CREATE TABLE ledger_restore (LIKE ledger_entries)" -c "COPY ledger_restore FROM PROGRAM 'gunzip -c /tmp/arch/ledger_2024_11.csv.gz' CSV HEADER"
psqlx -At -F' | ' -c "select count(*), sum(amount), md5(string_agg(id::text || '|' || type || '|' || amount::text || '|' || created_at::text, ',' ORDER BY id)) from ledger_restore" > /tmp/fp_after.$$
echo "   restore took $(( $(now_ms) - T2 )) ms"
cat /tmp/fp_after.$$
if cmp -s /tmp/fp_before.$$ /tmp/fp_after.$$; then echo '   fingerprints are IDENTICAL: safe to drop'; else echo '   fingerprints DIFFER: do NOT drop'; fi
echo '-- 5. manifest row, then DROP'
psqlx <<'SQL'
CREATE TABLE archive_manifest (partition_name text PRIMARY KEY, range_from date NOT NULL, range_to date NOT NULL, row_count bigint NOT NULL,
  amount_sum numeric(19,2) NOT NULL, sha256 text NOT NULL, location text NOT NULL, archived_at timestamptz NOT NULL DEFAULT now());
SQL
SHA=$(docker exec "$LAB_PG" cut -d' ' -f1 /tmp/arch/ledger_2024_11.sha256)
psqlx -c "INSERT INTO archive_manifest(partition_name, range_from, range_to, row_count, amount_sum, sha256, location)
          SELECT 'ledger_p_2024_11', '2024-11-01', '2024-12-01', count(*), sum(amount), '$SHA', 's3://payflow-ledger-archive/2024/11/ledger_2024_11.csv.gz' FROM ledger_restore"
psqlx -c "DROP TABLE ledger_p_2024_11" -c "DROP TABLE ledger_restore"
psqlx -x -c "select partition_name, row_count, amount_sum, left(sha256, 16) || '...' as sha256, location from archive_manifest"
echo '-- 6. the invariant after the archive (sum of the ledger x opening snapshots)'
echo "   whole ledger BEFORE:            $TOTAL_BEFORE"
echo "   live ledger only, AFTER drop:   $(psqlx -Atc "select $NETSQL from ledger_p")   <- no longer equals: the invariant would fire a false alarm"
echo "   live ledger + period_balances:  $(psqlx -Atc "select (select $NETSQL from ledger_p) + (select sum(net) from period_balances)")   <- equal again"
echo "total time of the 5 steps: $(( $(now_ms) - T0 )) ms (3M-row ledger; this month = $(cut -d' ' -f1 /tmp/fp_before.$$) rows)"
echo
echo '## C) pg_dump (custom format, -Fc) of the NEXT month, for size comparison with the CSV above'
docker exec "$LAB_PG" bash -c 'pg_dump -U postgres -Fc -t ledger_p_2024_12 payflow | wc -c | awk "{printf \"pg_dump -Fc ledger_p_2024_12: %.1f MB\\n\", \$1/1048576}"'
psqlx -Atc "select 'table ledger_p_2024_12 (heap + indexes): ' || pg_size_pretty(pg_total_relation_size('ledger_p_2024_12'))"
} > "$OUT/out-s304-archive.txt" 2>&1
rm -f /tmp/fp_before.$$ /tmp/fp_after.$$
cat "$OUT/out-s304-archive.txt"

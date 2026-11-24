#!/usr/bin/env bash
# S3.01: expand/contract mechanics measured on PostgreSQL 18 (3M ledger rows, 1M transfers, 200k users). Needs the lab container (see run-all.sh).
set -Eeuo pipefail; cd "$(dirname "$0")"; . ./lib.sh
OUT=${OUT:-.}

docker exec "$LAB_PG" bash -c 'pkill pgbench >/dev/null 2>&1 || true'
reset_db
{
echo '## A) ADD COLUMN: constant default x volatile default (3M rows)'
psqlx <<'SQL'
\timing on
select pg_relation_filenode('ledger_entries') as f0 \gset
ALTER TABLE ledger_entries ADD COLUMN source_kind VARCHAR(10) NOT NULL DEFAULT 'API';
select pg_relation_filenode('ledger_entries') = :'f0' as same_file_after_constant_default;
ALTER TABLE ledger_entries ADD COLUMN batch_no DOUBLE PRECISION NOT NULL DEFAULT random();
select pg_relation_filenode('ledger_entries') = :'f0' as same_file_after_volatile_default;
SQL
} > "$OUT/out-s301-add-column.txt" 2>&1

reset_db
{
echo '## B) the lock queue: an ALTER waits behind a long reader and BLOCKS every new reader'
for LT in 0 1s; do
  echo "--- ALTER with lock_timeout=$LT (0 = wait forever)"
  ( psqlx -c "begin; select count(*) from users; select pg_sleep(6); commit;" >/dev/null ) &
  sleep 1
  ( DB=payflow psqlx -c "set lock_timeout='$LT'; alter table users add column probe_$RANDOM int;" 2>&1 | sed 's/^/  [B ALTER] /'; echo "  [B ALTER] finished" ) &
  sleep 0.5
  ( T0=$(now_ms); psqlx -Atc "select 'reader C got its answer' from users limit 1" | sed 's/^/  [C SELECT] /'; T1=$(now_ms)
    echo "  [C SELECT] waited $((T1-T0)) ms (the long reader A holds its lock for ~6 s)" ) &
  sleep 1
  if [ "$LT" = 0 ]; then
    echo "  -- what pg_stat_activity shows while the three sessions wait (A = idle reader holding the lock, B = ALTER, C = innocent SELECT):"
    psqlx -c "select pid, state, wait_event_type || ':' || wait_event as waiting_on, pg_blocking_pids(pid) as blocked_by, left(query, 44) as query from pg_stat_activity where datname = 'payflow' and pid <> pg_backend_pid() and state <> 'idle' order by pid" | sed 's/^/  /'
  fi
  wait
done
} > "$OUT/out-s301-lock-queue.txt" 2>&1

reset_db
{
echo '## C) NOT NULL and FOREIGN KEY: the lock each step takes (pg_locks inside the transaction) and its time (1M rows)'
psqlx <<'SQL'
\echo '-- C0) the append-only trigger of the ledger (V5) also blocks a BACKFILL of ledger_entries'
DO $$ BEGIN UPDATE ledger_entries SET created_at = created_at WHERE id = (SELECT id FROM ledger_entries LIMIT 1);
  EXCEPTION WHEN OTHERS THEN RAISE NOTICE 'ledger UPDATE failed: %', SQLERRM; END $$;
ALTER TABLE transfers ADD COLUMN channel VARCHAR(10);
UPDATE transfers SET channel = 'API';
\echo '-- C1) direct SET NOT NULL: scans the whole table while holding the strongest lock'
\timing on
BEGIN;
ALTER TABLE transfers ALTER COLUMN channel SET NOT NULL;
\timing off
SELECT 'C1 lock held: ' || mode FROM pg_locks WHERE relation = 'transfers'::regclass AND pid = pg_backend_pid() AND granted AND mode LIKE 'Access%Exclusive%';
COMMIT;
\echo '-- C2) CHECK (channel IS NOT NULL) NOT VALID, VALIDATE, then SET NOT NULL'
ALTER TABLE transfers ALTER COLUMN channel DROP NOT NULL;
BEGIN;
ALTER TABLE transfers ADD CONSTRAINT ck_channel_nn CHECK (channel IS NOT NULL) NOT VALID;
SELECT 'C2a ADD ... NOT VALID holds: ' || string_agg(mode, ', ') FROM pg_locks WHERE relation = 'transfers'::regclass AND pid = pg_backend_pid() AND granted AND mode NOT LIKE 'Access%Share%';
COMMIT;
\timing on
BEGIN;
ALTER TABLE transfers VALIDATE CONSTRAINT ck_channel_nn;
\timing off
SELECT 'C2b VALIDATE holds: ' || string_agg(mode, ', ') FROM pg_locks WHERE relation = 'transfers'::regclass AND pid = pg_backend_pid() AND granted AND mode NOT LIKE 'Access%Share%';
COMMIT;
\echo 'C2c SET NOT NULL with the validated CHECK in place (no scan):'
\timing on
ALTER TABLE transfers ALTER COLUMN channel SET NOT NULL;
\timing off
ALTER TABLE transfers DROP CONSTRAINT ck_channel_nn;
\echo '-- C2d) PostgreSQL 18: the NOT NULL constraint itself can be NOT VALID'
ALTER TABLE transfers ALTER COLUMN channel DROP NOT NULL;
BEGIN;
ALTER TABLE transfers ADD CONSTRAINT nn_channel NOT NULL channel NOT VALID;
SELECT 'C2d ADD NOT NULL ... NOT VALID holds: ' || string_agg(mode, ', ') FROM pg_locks WHERE relation = 'transfers'::regclass AND pid = pg_backend_pid() AND granted AND mode NOT LIKE 'Access%Share%';
COMMIT;
\echo 'a NEW row with channel NULL is refused at once, even though the constraint is not validated yet:'
\set ON_ERROR_STOP off
INSERT INTO transfers (source_account_id, target_account_id, amount, status, idempotency_key) VALUES (gen_random_uuid(), gen_random_uuid(), 1, 'NEW', 'k-null-test');
\set ON_ERROR_STOP on
\timing on
BEGIN;
ALTER TABLE transfers VALIDATE CONSTRAINT nn_channel;
\timing off
SELECT 'C2d VALIDATE holds: ' || string_agg(mode, ', ') FROM pg_locks WHERE relation = 'transfers'::regclass AND pid = pg_backend_pid() AND granted AND mode NOT LIKE 'Access%Share%';
COMMIT;
\echo '-- C3) FOREIGN KEY transfers -> accounts: plain x NOT VALID + VALIDATE'
\timing on
BEGIN;
ALTER TABLE transfers ADD CONSTRAINT fk_t_src_plain FOREIGN KEY (source_account_id) REFERENCES accounts (id);
\timing off
SELECT 'C3a plain FK holds: ' || string_agg(c.relname || '=' || l.mode, ', ' ORDER BY c.relname, l.mode) FROM pg_locks l JOIN pg_class c ON c.oid = l.relation WHERE c.relname IN ('transfers','accounts') AND l.pid = pg_backend_pid() AND l.granted AND l.mode NOT IN ('AccessShareLock','RowShareLock');
COMMIT;
ALTER TABLE transfers DROP CONSTRAINT fk_t_src_plain;
BEGIN;
ALTER TABLE transfers ADD CONSTRAINT fk_t_src FOREIGN KEY (source_account_id) REFERENCES accounts (id) NOT VALID;
SELECT 'C3b FK ... NOT VALID holds: ' || string_agg(c.relname || '=' || l.mode, ', ' ORDER BY c.relname, l.mode) FROM pg_locks l JOIN pg_class c ON c.oid = l.relation WHERE c.relname IN ('transfers','accounts') AND l.pid = pg_backend_pid() AND l.granted AND l.mode NOT IN ('AccessShareLock','RowShareLock');
COMMIT;
\timing on
BEGIN;
ALTER TABLE transfers VALIDATE CONSTRAINT fk_t_src;
\timing off
SELECT 'C3c VALIDATE holds: ' || string_agg(c.relname || '=' || l.mode, ', ' ORDER BY c.relname, l.mode) FROM pg_locks l JOIN pg_class c ON c.oid = l.relation WHERE c.relname IN ('transfers','accounts') AND l.pid = pg_backend_pid() AND l.granted AND l.mode NOT IN ('AccessShareLock','RowShareLock');
COMMIT;
SQL
} > "$OUT/out-s301-not-null-fk.txt" 2>&1

reset_db
{
echo '## D) backfill of 1M rows: one UPDATE x batches of 20k with COMMIT; point updates (pgbench, 4 clients) running meanwhile'
psqlx -c "alter table transfers add column status_a varchar(12), add column status_b varchar(12)"
docker exec "$LAB_PG" bash -c "cat > /tmp/point.sql <<'P'
\set rid random(1,1000000)
UPDATE transfers SET created_at = created_at WHERE idempotency_key = 'k-' || :rid;
P"
run_case() { # name sql-file-content
  local name="$1" col="$2"
  echo "--- $name"
  psqlx -Atc "select pg_current_wal_lsn()" > /tmp/lsn0.$$
  docker exec "$LAB_PG" bash -c "rm -f /tmp/pgb*; pgbench -U postgres -n -f /tmp/point.sql -c 4 -T 40 -l --log-prefix=/tmp/pgb payflow >/tmp/pgb.out 2>&1" &
  PG=$!
  sleep 2
  local T0 T1; T0=$(now_ms)
  if [ "$name" = single ]; then
    psqlx -c "UPDATE transfers SET $col = lower(status)"
  else
    psqlx -c "DO \$\$ DECLARE last uuid := '00000000-0000-0000-0000-000000000000'; n int; b int := 0; BEGIN LOOP
        WITH k AS (SELECT id FROM transfers WHERE id > last ORDER BY id LIMIT 20000),
             u AS (UPDATE transfers t SET $col = lower(status) FROM k WHERE t.id = k.id RETURNING t.id)
        SELECT (array_agg(id ORDER BY id DESC))[1], count(*) INTO last, n FROM u;
        EXIT WHEN n = 0; b := b + 1; COMMIT; END LOOP; RAISE NOTICE 'batches: %', b; END \$\$" 2>&1
  fi
  T1=$(now_ms)
  echo "backfill took $((T1-T0)) ms"
  docker exec "$LAB_PG" bash -c 'pkill pgbench >/dev/null 2>&1 || true' >/dev/null 2>&1; wait $PG 2>/dev/null || true
  psqlx -Atc "select 'WAL written: ' || pg_size_pretty(pg_current_wal_lsn() - '$(cat /tmp/lsn0.$$)'::pg_lsn)"
  # pgbench -l columns: client tx_no latency_us ... ; keep only transactions that finished during the backfill window
  docker exec "$LAB_PG" bash -c "cat /tmp/pgb.[0-9]* 2>/dev/null | awk '{ if (\$3>m) m=\$3; if (\$3>1000000) slow++; n++; s+=\$3 } END { printf \"point updates: %d, average %.1f ms, MAX %.1f ms, slower than 1 s: %d\\n\", n, s/n/1000, m/1000, slow }'"
  rm -f /tmp/lsn0.$$
}
run_case single status_a
psqlx -c "vacuum transfers" ; 
run_case batches status_b
psqlx -Atc "select 'dead tuples now: ' || n_dead_tup || ', table size: ' || pg_size_pretty(pg_relation_size('transfers')) from pg_stat_user_tables where relname='transfers'"
} > "$OUT/out-s301-backfill.txt" 2>&1
sed -i "/^Terminated\$/d" "$OUT"/out-s301-*.txt
cat "$OUT"/out-s301-*.txt

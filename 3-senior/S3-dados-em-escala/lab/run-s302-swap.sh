#!/usr/bin/env bash
# S3.02 (part 2): turn the EXISTING ledger_entries into a partitioned table while a read+write load runs (rename + attach the old table as a partition).
set -Eeuo pipefail; cd "$(dirname "$0")"; . ./lib.sh
OUT=${OUT:-.}
docker exec "$LAB_PG" bash -c 'pkill pgbench >/dev/null 2>&1 || true'
reset_db
{
echo '## G) swap: legacy table becomes the first partition of a new partitioned ledger_entries'
docker exec "$LAB_PG" bash -c "cat > /tmp/read.sql <<'P'
\set rid random(1,200000)
SELECT count(*), coalesce(sum(amount),0) FROM ledger_entries WHERE account_id = (SELECT id FROM accounts OFFSET :rid LIMIT 1) AND created_at >= '2026-01-01';
P
cat > /tmp/write.sql <<'P'
\set rid random(1,200000)
INSERT INTO ledger_entries (account_id, type, amount, created_at) SELECT id, 'CREDIT', 1.00, timestamptz '2026-11-01' + random() * interval '20 hours' FROM accounts OFFSET :rid LIMIT 1;
P"
echo '-- 1. online preparation (no long lock): unique index including the future partition key, and a validated CHECK equal to the bound'
T0=$(now_ms)
psqlx -c "CREATE UNIQUE INDEX CONCURRENTLY uk_ledger_id_created ON ledger_entries (id, created_at)"
psqlx -c "ALTER TABLE ledger_entries ADD CONSTRAINT ck_ledger_range CHECK (created_at >= '2024-11-01' AND created_at < '2026-11-02') NOT VALID" \
      -c "ALTER TABLE ledger_entries VALIDATE CONSTRAINT ck_ledger_range"
echo "   preparation took $(( $(now_ms) - T0 )) ms"
echo '-- 2. start the load (reads and inserts, 4 clients each, 12 s) and swap in the middle'
docker exec "$LAB_PG" bash -c "rm -f /tmp/sw.*; (pgbench -U postgres -n -f /tmp/read.sql -c 4 -T 12 -l --log-prefix=/tmp/sw.r payflow > /tmp/sw.r.out 2>&1 &) ; (pgbench -U postgres -n -f /tmp/write.sql -c 4 -T 12 -l --log-prefix=/tmp/sw.w payflow > /tmp/sw.w.out 2>&1 &)"
sleep 4
T1=$(now_ms)
psqlx <<'SQL'
BEGIN;
SET LOCAL lock_timeout = '2s';
ALTER TABLE ledger_entries RENAME TO ledger_entries_legacy;
ALTER TABLE ledger_entries_legacy DROP CONSTRAINT pk_ledger_entries, ADD CONSTRAINT pk_ledger_entries_legacy PRIMARY KEY USING INDEX uk_ledger_id_created;
ALTER INDEX idx_ledger_entries_account_created RENAME TO idx_ledger_legacy_account_created;
CREATE TABLE ledger_entries (
    id UUID NOT NULL DEFAULT uuidv7(), account_id UUID NOT NULL, transfer_id UUID, type VARCHAR(10) NOT NULL,
    amount NUMERIC(19,2) NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_ledger_entries PRIMARY KEY (id, created_at),
    CONSTRAINT ck_ledger_type CHECK (type IN ('DEBIT','CREDIT')), CONSTRAINT ck_ledger_amount CHECK (amount > 0)
) PARTITION BY RANGE (created_at);
CREATE INDEX idx_ledger_entries_account_created ON ledger_entries (account_id, created_at DESC);
ALTER TABLE ledger_entries ATTACH PARTITION ledger_entries_legacy FOR VALUES FROM ('2024-11-01') TO ('2026-11-02');
CREATE TABLE ledger_entries_2026_11 PARTITION OF ledger_entries FOR VALUES FROM ('2026-11-02') TO ('2026-12-01');
CREATE TABLE ledger_entries_2026_12 PARTITION OF ledger_entries FOR VALUES FROM ('2026-12-01') TO ('2027-01-01');
DROP TRIGGER trg_ledger_append_only ON ledger_entries_legacy;   -- the parent trigger is cloned into every partition, legacy included
CREATE TRIGGER trg_ledger_append_only BEFORE UPDATE OR DELETE ON ledger_entries FOR EACH ROW EXECUTE FUNCTION ledger_append_only();
COMMIT;
SQL
T2=$(now_ms)
echo "   the swap transaction took $((T2-T1)) ms (rename + create + attach + partitions + trigger)"
sleep 9
docker exec "$LAB_PG" bash -c 'for k in r w; do echo "   pgbench $k:"; grep -E "number of transactions actually processed|number of failed transactions|latency average" /tmp/sw.$k.out | sed "s/^/     /"; cat /tmp/sw.$k.[0-9]* 2>/dev/null | awk "{ if (\$3>m) m=\$3; n++ } END { printf \"     max latency %.1f ms over %d transactions\\n\", m/1000, n }" ; done'
echo '-- 3. result'
psqlx <<'SQL'
INSERT INTO ledger_entries (account_id, type, amount) SELECT id, 'CREDIT', 5.00 FROM accounts LIMIT 1;   -- created_at = now(): goes to the NEW partition
SELECT 'partitions: ' || string_agg(c.relname, ', ' ORDER BY c.relname) FROM pg_inherits i JOIN pg_class c ON c.oid = i.inhrelid WHERE i.inhparent = 'ledger_entries'::regclass;
SELECT 'rows per partition: ' || string_agg(tableoid::regclass || '=' || n, ', ' ORDER BY tableoid::regclass::text) FROM (SELECT tableoid, count(*) n FROM ledger_entries GROUP BY tableoid) x;
\set ON_ERROR_STOP off
UPDATE ledger_entries SET amount = amount WHERE id = (SELECT id FROM ledger_entries_2026_11 LIMIT 1);
UPDATE ledger_entries SET amount = amount WHERE id = (SELECT id FROM ledger_entries_legacy LIMIT 1);
\set ON_ERROR_STOP on
\echo '-- the append-only trigger does NOT stop the removal of a whole partition (here: the one that holds the row inserted above):'
ALTER TABLE ledger_entries DETACH PARTITION ledger_entries_2026_11;
DROP TABLE ledger_entries_2026_11;
SELECT 'partition dropped with the trigger in place; rows left in ledger_entries: ' || count(*) FROM ledger_entries;
SQL
} > "$OUT/out-s302-swap.txt" 2>&1
sed -i '/^Terminated$/d' "$OUT/out-s302-swap.txt"
cat "$OUT/out-s302-swap.txt"

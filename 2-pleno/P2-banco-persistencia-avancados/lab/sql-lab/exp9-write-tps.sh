#!/usr/bin/env bash
# P2.11: custo de escrita do índice coberto na transferência (pgbench, 8 clientes, 4 s, 3 rodadas por estado). NÃO chama reset.
cd "$(dirname "$0")"; source ./run.sh
$PSQL -c "DROP TABLE IF EXISTS lab_acc" -c "CREATE TABLE lab_acc AS SELECT row_number() OVER (ORDER BY created_at, id)::int AS n, id FROM accounts" -c "ALTER TABLE lab_acc ADD PRIMARY KEY (n)" >/dev/null
docker exec -i lab-p2-pg sh -c "cat > /tmp/transfer.sql" < transfer.pgbench
run() { for i in 1 2 3; do docker exec lab-p2-pg pgbench -U postgres -d payflow -n -f /tmp/transfer.sql -c 8 -j 4 -T 4 2>&1 | awk '/tps =/ {printf "%s ", $3}'; done; echo; }
$PSQL -c "CREATE INDEX IF NOT EXISTS idx_ledger_entries_account_created ON ledger_entries (account_id, created_at DESC)" -c "DROP INDEX IF EXISTS idx_ledger_entries_account_created_cov" -c "DROP INDEX IF EXISTS idx_transfers_pending" >/dev/null
echo -n "S0 (índice antigo)        tps: "; run
$PSQL -c "CREATE INDEX CONCURRENTLY idx_ledger_entries_account_created_cov ON ledger_entries (account_id, created_at DESC) INCLUDE (type, amount)" >/dev/null
echo -n "S1 (antigo + coberto)     tps: "; run
$PSQL -c "DROP INDEX CONCURRENTLY idx_ledger_entries_account_created" >/dev/null
echo -n "S2 (só o coberto)         tps: "; run
$PSQL -c "DELETE FROM ledger_entries WHERE created_at > timestamptz '2026-11-14 00:00:00+00'" -c "VACUUM (ANALYZE) ledger_entries" >/dev/null

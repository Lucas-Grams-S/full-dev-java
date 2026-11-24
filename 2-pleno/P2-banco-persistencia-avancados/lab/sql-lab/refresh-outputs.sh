#!/usr/bin/env bash
# Regera os out-*.txt dos experimentos de plano/índice sobre o volume (NÃO chama reset: preserva os 5 milhões de linhas).
cd "$(dirname "$0")"; source ./run.sh
$PSQL -c "CREATE INDEX IF NOT EXISTS idx_ledger_entries_account_created ON ledger_entries (account_id, created_at DESC)" -c "DROP INDEX IF EXISTS idx_ledger_entries_account_created_cov" -c "DROP INDEX IF EXISTS idx_transfers_pending" -c "VACUUM (ANALYZE) ledger_entries" -c "CREATE EXTENSION IF NOT EXISTS pg_stat_statements" >/dev/null
$PSQL -f - < 20-explain.sql > out-explain-1.txt 2>&1
$PSQL -f - < 21-indexes.sql > out-indexes-1.txt 2>&1
$PSQL -f - < 22-indexes-2.sql > out-indexes-2.txt 2>&1
$PSQL -f - < 23-write-cost.sql > out-write-cost.txt 2>&1
./exp6-create-index.sh > out-exp6.txt 2>&1
$PSQL -f - < 24-read-model.sql > out-read-model.txt 2>&1
$PSQL -f - < 30-stats-and-statements.sql > out-stats.txt 2>&1
$PSQL -c "DELETE FROM ledger_entries WHERE created_at > timestamptz '2026-11-14 00:00:00+00'" -c "VACUUM (ANALYZE) ledger_entries" >/dev/null
echo finished

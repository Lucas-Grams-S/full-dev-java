#!/usr/bin/env bash
# P2.05: CREATE INDEX bloqueia escrita; CONCURRENTLY não (e falha deixando índice INVALID)
source "$(dirname "$0")/run.sh"
ACC=$($PSQL -Atc "SELECT account_id FROM ledger_entries LIMIT 1")
ins() { echo "\\timing on
INSERT INTO ledger_entries (account_id, type, amount) VALUES ('$ACC','CREDIT',1);
DELETE FROM ledger_entries WHERE created_at > timestamptz '2026-11-14 00:00:00+00';"; }
for MODE in "" "CONCURRENTLY"; do
  echo "=== CREATE INDEX $MODE idx_tmp ON ledger_entries (amount) com um INSERT chegando 1 s depois ==="
  ( $PSQL -c "\\timing on" -c "CREATE INDEX $MODE idx_tmp ON ledger_entries (amount)" 2>&1 | sed 's/^/[A] /' ) &
  sleep 1
  ins | $PSQL 2>&1 | grep Time | head -1 | sed 's/^/[B] INSERT: /'
  wait
  $PSQL -c "DROP INDEX idx_tmp" >/dev/null
done
echo "=== CONCURRENTLY dentro de transação não é permitido ==="
$PSQL -c "BEGIN; CREATE INDEX CONCURRENTLY idx_tmp2 ON ledger_entries (amount); ROLLBACK;" 2>&1
echo "=== CONCURRENTLY que falha deixa índice INVALID: índice único sobre coluna com duplicatas ==="
$PSQL -c "CREATE UNIQUE INDEX CONCURRENTLY idx_tmp3 ON ledger_entries (amount)" 2>&1
$PSQL -c "SELECT indexrelid::regclass, indisvalid FROM pg_index WHERE indexrelid::regclass::text = 'idx_tmp3'"
$PSQL -c "DROP INDEX CONCURRENTLY idx_tmp3"

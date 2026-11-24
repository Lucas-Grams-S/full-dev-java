#!/usr/bin/env bash
# P2.01: relatório de conciliação (regra 3) com DOIS comandos: READ COMMITTED enxerga mundos diferentes; REPEATABLE READ, um só.
source "$(dirname "$0")/run.sh"
A=20000000-0000-0000-0000-000000000001
for LEVEL in "READ COMMITTED" "REPEATABLE READ"; do
  reset
  $PSQL -c "INSERT INTO ledger_entries (account_id, type, amount) SELECT id, 'CREDIT', 1000 FROM accounts" >/dev/null
  echo "=== conciliação em $LEVEL (um depósito de 100 confirma entre os dois comandos) ==="
  psql_a <<SQL &
BEGIN ISOLATION LEVEL $LEVEL READ ONLY;
SELECT 'soma dos saldos' AS q, sum(balance) FROM accounts;
SELECT pg_sleep(2);
SELECT 'soma dos lançamentos' AS q, sum(CASE type WHEN 'CREDIT' THEN amount ELSE -amount END) FROM ledger_entries;
COMMIT;
SQL
  sleep 0.8
  psql_b <<SQL
BEGIN; UPDATE accounts SET balance = balance + 100 WHERE id='$A'; INSERT INTO ledger_entries (account_id, type, amount) VALUES ('$A','CREDIT',100); COMMIT;
SQL
  wait
done

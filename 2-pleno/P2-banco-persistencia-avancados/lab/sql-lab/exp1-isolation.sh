#!/usr/bin/env bash
# P2.01 — anomalias e níveis de isolamento (duas sessões). Uso: ./exp1-isolation.sh
source "$(dirname "$0")/run.sh"
ACC1=20000000-0000-0000-0000-000000000001

for LEVEL in "READ COMMITTED" "REPEATABLE READ"; do
  reset
  echo "=== Leitura não repetível e fantasma em $LEVEL ==="
  psql_a <<SQL &
BEGIN ISOLATION LEVEL $LEVEL;
SELECT 'A 1a leitura' AS q, balance, (SELECT count(*) FROM ledger_entries WHERE account_id='$ACC1') AS lancamentos FROM accounts WHERE id='$ACC1';
SELECT pg_sleep(3);
SELECT 'A 2a leitura' AS q, balance, (SELECT count(*) FROM ledger_entries WHERE account_id='$ACC1') AS lancamentos FROM accounts WHERE id='$ACC1';
COMMIT;
SQL
  sleep 1
  psql_b <<SQL
UPDATE accounts SET balance = balance + 100 WHERE id='$ACC1';
INSERT INTO ledger_entries (account_id, type, amount) VALUES ('$ACC1','CREDIT',100);
SELECT 'B confirmou (autocommit)';
SQL
  wait
done

reset
echo "=== READ UNCOMMITTED se comporta como READ COMMITTED (leitura suja?) ==="
psql_a <<SQL &
BEGIN;
UPDATE accounts SET balance = 0 WHERE id='$ACC1';
SELECT pg_sleep(3);
ROLLBACK;
SQL
sleep 1
psql_b <<SQL
BEGIN ISOLATION LEVEL READ UNCOMMITTED;
SHOW transaction_isolation;
SELECT 'B lê durante a tx de A' AS q, balance FROM accounts WHERE id='$ACC1';
COMMIT;
SQL
wait

# Write skew: regra "no máximo 3 contas por usuário" (conta, depois insere)
for LEVEL in "READ COMMITTED" "REPEATABLE READ" "SERIALIZABLE"; do
  reset
  echo "=== Write skew (máx. 3 contas por usuário) em $LEVEL ==="
  for who in a b; do
    f=psql_$who
    ( $f <<SQL
BEGIN ISOLATION LEVEL $LEVEL;
SELECT '$who conta' AS q, count(*) FROM accounts WHERE owner_id='10000000-0000-0000-0000-000000000001';
SELECT pg_sleep(2);
INSERT INTO accounts (owner_id) VALUES ('10000000-0000-0000-0000-000000000001');
COMMIT;
SQL
    ) &
  done
  wait
  $PSQL -Atc "SELECT 'contas de Ana no fim: ' || count(*) FROM accounts WHERE owner_id='10000000-0000-0000-0000-000000000001'"
done

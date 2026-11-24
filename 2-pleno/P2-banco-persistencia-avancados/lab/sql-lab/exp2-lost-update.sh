#!/usr/bin/env bash
# P2.02 — lost update, @Version (UPDATE ... WHERE version = ?), UPDATE condicional, FOR UPDATE, NOWAIT, SKIP LOCKED, lock_timeout
source "$(dirname "$0")/run.sh"
ACC=20000000-0000-0000-0000-000000000001
show() { $PSQL -Atc "SELECT 'saldo final: ' || balance || ' | version: ' || version FROM accounts WHERE id='$ACC'"; }

reset; echo "=== 1. Lost update: ler (app), somar na app, gravar o valor (READ COMMITTED) ==="
# A lê 1000 e grava 1100 (+100); B lê 1000 e grava 1050 (+50). Esperado: 1150.
psql_a <<SQL &
BEGIN; SELECT balance FROM accounts WHERE id='$ACC'; SELECT pg_sleep(2);
UPDATE accounts SET balance = 1100 WHERE id='$ACC'; COMMIT;
SQL
sleep 0.5
psql_b <<SQL
BEGIN; SELECT balance FROM accounts WHERE id='$ACC'; SELECT pg_sleep(2);
UPDATE accounts SET balance = 1050 WHERE id='$ACC'; COMMIT;
SQL
wait; show

reset; echo "=== 2. O mesmo em REPEATABLE READ ==="
psql_a <<SQL &
BEGIN ISOLATION LEVEL REPEATABLE READ; SELECT balance FROM accounts WHERE id='$ACC'; SELECT pg_sleep(2);
UPDATE accounts SET balance = 1100 WHERE id='$ACC'; COMMIT;
SQL
sleep 0.5
psql_b <<SQL
BEGIN ISOLATION LEVEL REPEATABLE READ; SELECT balance FROM accounts WHERE id='$ACC'; SELECT pg_sleep(2);
UPDATE accounts SET balance = 1050 WHERE id='$ACC'; COMMIT;
SQL
wait; show

reset; echo "=== 3. @Version na mão: UPDATE ... SET version = version + 1 WHERE id = ? AND version = ? ==="
psql_a <<SQL &
BEGIN; SELECT balance, version FROM accounts WHERE id='$ACC'; SELECT pg_sleep(2);
UPDATE accounts SET balance = 1100, version = version + 1 WHERE id='$ACC' AND version = 0 RETURNING 'atualizou 1 linha' AS resultado; COMMIT;
SQL
sleep 0.5
psql_b <<SQL
BEGIN; SELECT balance, version FROM accounts WHERE id='$ACC'; SELECT pg_sleep(2);
UPDATE accounts SET balance = 1050, version = version + 1 WHERE id='$ACC' AND version = 0 RETURNING 'atualizou 1 linha' AS resultado; COMMIT;
SQL
wait; show

reset; echo "=== 4. UPDATE atômico: balance = balance + ? ==="
psql_a <<SQL &
BEGIN; UPDATE accounts SET balance = balance + 100 WHERE id='$ACC'; SELECT pg_sleep(2); COMMIT;
SQL
sleep 0.5
psql_b <<SQL
\timing on
UPDATE accounts SET balance = balance + 50 WHERE id='$ACC';
SQL
wait; show

reset; echo "=== 5. Saque condicional (WHERE balance >= ?): dois saques de 800 de uma conta de 1000 ==="
psql_a <<SQL &
BEGIN; UPDATE accounts SET balance = balance - 800 WHERE id='$ACC' AND balance >= 800 RETURNING 'A: saque feito' AS resultado; SELECT pg_sleep(2); COMMIT;
SQL
sleep 0.5
psql_b <<SQL
UPDATE accounts SET balance = balance - 800 WHERE id='$ACC' AND balance >= 800 RETURNING 'B: saque feito' AS resultado;
SQL
wait; show

reset; echo "=== 6. SELECT ... FOR UPDATE: B espera e lê o valor novo ==="
psql_a <<SQL &
BEGIN; SELECT balance FROM accounts WHERE id='$ACC' FOR UPDATE; SELECT pg_sleep(2);
UPDATE accounts SET balance = balance + 100 WHERE id='$ACC'; COMMIT;
SQL
sleep 0.5
psql_b <<SQL
\timing on
BEGIN; SELECT balance FROM accounts WHERE id='$ACC' FOR UPDATE;
UPDATE accounts SET balance = balance + 50 WHERE id='$ACC'; COMMIT;
SQL
wait; show

reset; echo "=== 7. NOWAIT, SKIP LOCKED e lock_timeout ==="
psql_a <<SQL &
BEGIN; SELECT id FROM accounts WHERE id='$ACC' FOR UPDATE; SELECT pg_sleep(4); COMMIT;
SQL
sleep 0.7
psql_b <<SQL
BEGIN; SELECT 'NOWAIT' q; SELECT id FROM accounts WHERE id='$ACC' FOR UPDATE NOWAIT; ROLLBACK;
BEGIN; SELECT 'SKIP LOCKED (4 contas, 1 travada)' q; SELECT substr(id::text,34) AS id_fim FROM accounts ORDER BY id FOR UPDATE SKIP LOCKED; ROLLBACK;
BEGIN; SET LOCAL lock_timeout = '500ms'; SELECT 'lock_timeout 500ms' q; SELECT id FROM accounts WHERE id='$ACC' FOR UPDATE; ROLLBACK;
SQL
wait

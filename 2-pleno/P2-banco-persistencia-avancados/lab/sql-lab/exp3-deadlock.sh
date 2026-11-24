#!/usr/bin/env bash
# P2.03 — deadlock no banco: transferências em sentidos opostos; correção por ordem de id; deadlock via FK/índice único
source "$(dirname "$0")/run.sh"
A=20000000-0000-0000-0000-000000000001
B=20000000-0000-0000-0000-000000000002

reset; echo "=== 1. Ana->Bruno e Bruno->Ana, cada uma trava a origem primeiro ==="
psql_a <<SQL &
BEGIN; UPDATE accounts SET balance = balance - 10 WHERE id='$A'; SELECT pg_sleep(1.5);
UPDATE accounts SET balance = balance + 10 WHERE id='$B'; COMMIT;
SQL
sleep 0.3
psql_b <<SQL
BEGIN; UPDATE accounts SET balance = balance - 20 WHERE id='$B'; SELECT pg_sleep(1.5);
UPDATE accounts SET balance = balance + 20 WHERE id='$A'; COMMIT;
SQL
wait
$PSQL -Atc "SELECT 'saldos: ' || string_agg(balance::text, ' / ' ORDER BY id) FROM accounts WHERE id IN ('$A','$B')"

reset; echo "=== 2. Correção: travar SEMPRE em ordem de id (SELECT ... FOR UPDATE ORDER BY id) ==="
lock_and_move() { # $1=origem $2=destino $3=valor
cat <<SQL
BEGIN;
SELECT id FROM accounts WHERE id IN ('$1','$2') ORDER BY id FOR UPDATE;
SELECT pg_sleep(1.5);
UPDATE accounts SET balance = balance - $3 WHERE id='$1';
UPDATE accounts SET balance = balance + $3 WHERE id='$2';
COMMIT;
SQL
}
psql_a <<< "$(lock_and_move $A $B 10)" &
sleep 0.3
psql_b <<< "$(lock_and_move $B $A 20)"
wait
$PSQL -Atc "SELECT 'saldos: ' || string_agg(balance::text, ' / ' ORDER BY id) FROM accounts WHERE id IN ('$A','$B')"

reset; echo "=== 3. Diagnóstico: quem espera quem (pg_locks / pg_blocking_pids) ==="
psql_a <<SQL &
BEGIN; UPDATE accounts SET balance = balance - 10 WHERE id='$A'; SELECT pg_sleep(4); COMMIT;
SQL
sleep 0.4
psql_b <<SQL &
UPDATE accounts SET balance = balance + 1 WHERE id='$A';
SQL
sleep 1
$PSQL <<SQL
SELECT pid, state, wait_event_type, wait_event, pg_blocking_pids(pid) AS bloqueado_por, left(query, 60) AS query
FROM pg_stat_activity WHERE datname='payflow' AND pid <> pg_backend_pid() AND state <> 'idle' ORDER BY pid;
SQL
wait

reset; echo "=== 4. Deadlock sem UPDATE de saldo: duas inserções com a mesma idempotency_key em sentido cruzado ==="
psql_a <<SQL &
BEGIN; INSERT INTO transfers (source_account_id,target_account_id,amount,idempotency_key) VALUES ('$A','$B',1,'k-1'); SELECT pg_sleep(1.5);
INSERT INTO transfers (source_account_id,target_account_id,amount,idempotency_key) VALUES ('$A','$B',1,'k-2'); COMMIT;
SQL
sleep 0.3
psql_b <<SQL
BEGIN; INSERT INTO transfers (source_account_id,target_account_id,amount,idempotency_key) VALUES ('$A','$B',1,'k-2'); SELECT pg_sleep(1.5);
INSERT INTO transfers (source_account_id,target_account_id,amount,idempotency_key) VALUES ('$A','$B',1,'k-1'); COMMIT;
SQL
wait

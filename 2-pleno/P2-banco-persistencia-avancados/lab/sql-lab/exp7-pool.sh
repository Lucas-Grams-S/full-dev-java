#!/usr/bin/env bash
# P2.06: vazão (TPS) × nº de conexões simultâneas. pgbench dentro do container do banco (compete pela CPU: ver aula).
source "$(dirname "$0")/run.sh"
$PSQL -c "DROP TABLE IF EXISTS lab_acc" -c "CREATE TABLE lab_acc AS SELECT row_number() OVER (ORDER BY created_at, id)::int AS n, id FROM accounts" -c "ALTER TABLE lab_acc ADD PRIMARY KEY (n)" >/dev/null
for f in transfer transfer-with-wait; do
  docker exec -i lab-p2-pg sh -c "cat > /tmp/$f.sql" < "$(dirname "$0")/$f.pgbench"
  echo "=== $f.pgbench ==="
  for c in 1 2 4 8 16 32 64 128; do
    j=$(( c < 4 ? c : 4 ))
    docker exec lab-p2-pg pgbench -U postgres -d payflow -n -f /tmp/$f.sql -c $c -j $j -T 8 2>&1 | awk -v c=$c '/tps =/ {t=$3} /latency average/ {l=$4} /number of failed/ {fl=$0} END {printf "clientes=%-4d tps=%-8s latência média=%s ms\n", c, t, l}'
  done
done

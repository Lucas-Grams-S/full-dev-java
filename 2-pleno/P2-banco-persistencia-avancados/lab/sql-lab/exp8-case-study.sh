#!/usr/bin/env bash
# P2.11: o extrato lento. Latência média (pgbench, 1 cliente, 4 s por consulta) antes/depois dos índices e das reescritas.
source "$(dirname "$0")/run.sh"
HOT=$($PSQL -Atc "SELECT account_id FROM ledger_entries GROUP BY account_id ORDER BY count(*) DESC LIMIT 1")
LAST_TS_ID=$($PSQL -At -F'|' -c "SELECT created_at, id FROM ledger_entries WHERE account_id='$HOT' ORDER BY created_at DESC, id DESC OFFSET 400000 LIMIT 1")
TS="${LAST_TS_ID%%|*}"; ID="${LAST_TS_ID##*|}"
declare -A Q
Q[1_primeira_pagina]="SELECT * FROM ledger_entries WHERE account_id='$HOT' ORDER BY created_at DESC, id DESC LIMIT 20"
Q[2_pagina_20001_offset]="SELECT * FROM ledger_entries WHERE account_id='$HOT' ORDER BY created_at DESC, id DESC OFFSET 400000 LIMIT 20"
Q[3_pagina_20001_keyset]="SELECT * FROM ledger_entries WHERE account_id='$HOT' AND (created_at, id) < ('$TS', '$ID') ORDER BY created_at DESC, id DESC LIMIT 20"
Q[4_total_de_linhas]="SELECT count(*) FROM ledger_entries WHERE account_id='$HOT'"
Q[5_resumo_30_dias]="SELECT type, count(*), sum(amount) FROM ledger_entries WHERE account_id='$HOT' AND created_at >= timestamptz '2026-10-14' GROUP BY type"
Q[6_resumo_2_anos_por_mes]="SELECT date_trunc('month', created_at) m, type, sum(amount) FROM ledger_entries WHERE account_id='$HOT' GROUP BY 1,2 ORDER BY 1"
Q[7_dia_com_date_cast]="SELECT count(*) FROM ledger_entries WHERE account_id='$HOT' AND created_at::date = date '2026-10-01'"
Q[8_dia_com_intervalo]="SELECT count(*) FROM ledger_entries WHERE account_id='$HOT' AND created_at >= timestamptz '2026-10-01 00:00:00+00' AND created_at < timestamptz '2026-10-02 00:00:00+00'"
measure() {
  echo "--- $1 ---"
  $PSQL -c "VACUUM (ANALYZE) ledger_entries" >/dev/null
  for k in $(printf '%s\n' "${!Q[@]}" | sort); do
    echo "${Q[$k]};" | docker exec -i lab-p2-pg sh -c 'cat > /tmp/q.sql'
    ms=$(docker exec lab-p2-pg pgbench -U postgres -d payflow -n -f /tmp/q.sql -c 1 -T 4 2>&1 | awk '/latency average/ {print $4}')
    printf '%-28s %10s ms\n' "$k" "$ms"
  done
  $PSQL -Atc "SELECT 'tamanho dos índices de ledger_entries: ' || pg_size_pretty(pg_indexes_size('ledger_entries'))"
}
$PSQL -c "DROP INDEX IF EXISTS idx_ledger_entries_account_created_cov" -c "DROP INDEX IF EXISTS idx_transfers_pending" >/dev/null
measure "S0: índices da V4 apenas"
echo ">>> CREATE INDEX CONCURRENTLY (covering)"; $PSQL -c "\\timing on" -c "CREATE INDEX CONCURRENTLY idx_ledger_entries_account_created_cov ON ledger_entries (account_id, created_at DESC) INCLUDE (type, amount)" | grep Time
measure "S1: V4 + índice coberto (INCLUDE type, amount)"
$PSQL -c "DROP INDEX CONCURRENTLY idx_ledger_entries_account_created" >/dev/null
measure "S2: só o índice coberto (o antigo foi removido)"

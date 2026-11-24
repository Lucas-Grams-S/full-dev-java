-- P2.11: o resumo de 2 anos como MODELO DE LEITURA (visão materializada): troca frescor por velocidade.
\set QUIET on
SELECT account_id AS hot FROM ledger_entries GROUP BY account_id ORDER BY count(*) DESC LIMIT 1 \gset
\timing on
CREATE MATERIALIZED VIEW ledger_monthly AS
  SELECT account_id, date_trunc('month', created_at) AS month, type, count(*) AS entries, sum(amount) AS total
  FROM ledger_entries GROUP BY 1, 2, 3;
CREATE UNIQUE INDEX ON ledger_monthly (account_id, month, type);
\timing off
ANALYZE ledger_monthly;
SELECT (SELECT count(*) FROM ledger_monthly) AS linhas, pg_size_pretty(pg_total_relation_size('ledger_monthly')) AS tamanho;
EXPLAIN (ANALYZE, BUFFERS, COSTS OFF) SELECT month, type, entries, total FROM ledger_monthly WHERE account_id = :'hot' ORDER BY month;
\timing on
REFRESH MATERIALIZED VIEW CONCURRENTLY ledger_monthly;
\timing off
DROP MATERIALIZED VIEW ledger_monthly;

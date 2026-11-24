-- P2.04: planos de execução (índices da V4 apenas). \gset guarda ids em variáveis do psql.
\set QUIET on
SELECT id AS hot FROM accounts WHERE id = (SELECT account_id FROM ledger_entries GROUP BY account_id ORDER BY count(*) DESC LIMIT 1) \gset
SELECT id AS normal FROM accounts a WHERE (SELECT count(*) FROM ledger_entries l WHERE l.account_id = a.id) BETWEEN 200 AND 260 LIMIT 1 \gset
SELECT 'hot: ' || :'hot' || ' = ' || (SELECT count(*) FROM ledger_entries WHERE account_id = :'hot') || ' lançamentos; normal: ' || :'normal' || ' = ' || (SELECT count(*) FROM ledger_entries WHERE account_id = :'normal') AS info;
\echo
\echo '### Q1 extrato da conta normal: 20 mais recentes'
EXPLAIN (ANALYZE, BUFFERS, COSTS ON) SELECT * FROM ledger_entries WHERE account_id = :'normal' ORDER BY created_at DESC LIMIT 20;
\echo '### Q2 extrato da conta quente: 20 mais recentes'
EXPLAIN (ANALYZE, BUFFERS) SELECT * FROM ledger_entries WHERE account_id = :'hot' ORDER BY created_at DESC LIMIT 20;
\echo '### Q3 página 5 000 com OFFSET (conta quente)'
EXPLAIN (ANALYZE, BUFFERS) SELECT * FROM ledger_entries WHERE account_id = :'hot' ORDER BY created_at DESC LIMIT 20 OFFSET 100000;
\echo '### Q3b mesma página com keyset (created_at, id) < último visto'
SELECT created_at AS last_ts, id AS last_id FROM ledger_entries WHERE account_id = :'hot' ORDER BY created_at DESC, id DESC LIMIT 1 OFFSET 100000 \gset
EXPLAIN (ANALYZE, BUFFERS) SELECT * FROM ledger_entries WHERE account_id = :'hot' AND (created_at, id) < (:'last_ts', :'last_id') ORDER BY created_at DESC, id DESC LIMIT 20;
\echo '### Q4 resumo mensal da conta quente (soma por tipo, 30 dias)'
EXPLAIN (ANALYZE, BUFFERS) SELECT type, count(*), sum(amount) FROM ledger_entries WHERE account_id = :'hot' AND created_at >= timestamptz '2026-10-14' GROUP BY type;
\echo '### Q4b resumo de 2 anos da conta quente'
EXPLAIN (ANALYZE, BUFFERS) SELECT date_trunc('month', created_at) AS mes, type, sum(amount) FROM ledger_entries WHERE account_id = :'hot' GROUP BY 1, 2 ORDER BY 1;
\echo '### Q5 função na coluna: date(created_at) = dia'
EXPLAIN (ANALYZE, BUFFERS) SELECT count(*) FROM ledger_entries WHERE account_id = :'hot' AND created_at::date = date '2026-10-01';
\echo '### Q5b intervalo semiaberto equivalente'
EXPLAIN (ANALYZE, BUFFERS) SELECT count(*) FROM ledger_entries WHERE account_id = :'hot' AND created_at >= timestamptz '2026-10-01 00:00:00+00' AND created_at < timestamptz '2026-10-02 00:00:00+00';
\echo '### Q6 transferências PENDING'
EXPLAIN (ANALYZE, BUFFERS) SELECT id, created_at FROM transfers WHERE status = 'PENDING' ORDER BY created_at LIMIT 50;
\echo '### Q7 conciliação (regra 3) de UMA conta'
EXPLAIN (ANALYZE, BUFFERS) SELECT sum(CASE type WHEN 'CREDIT' THEN amount ELSE -amount END) FROM ledger_entries WHERE account_id = :'hot';

-- P2.05: estratégia de índices (sobre o volume de 10-big-data.sql)
\set QUIET on
\timing off
SELECT id AS hot FROM accounts WHERE id = (SELECT account_id FROM ledger_entries GROUP BY account_id ORDER BY count(*) DESC LIMIT 1) \gset
\echo '### A. Cobertura: INCLUDE (type, amount) permite Index Only Scan na soma por conta quente'
\timing on
CREATE INDEX idx_ledger_entries_account_created_cov ON ledger_entries (account_id, created_at DESC) INCLUDE (type, amount);
\timing off
SELECT relname, pg_size_pretty(pg_relation_size(oid)) AS tamanho FROM pg_class WHERE relname IN ('idx_ledger_entries_account_created','idx_ledger_entries_account_created_cov');
EXPLAIN (ANALYZE, BUFFERS, COSTS OFF) SELECT sum(CASE type WHEN 'CREDIT' THEN amount ELSE -amount END) FROM ledger_entries WHERE account_id = :'hot';
EXPLAIN (ANALYZE, BUFFERS, COSTS OFF) SELECT type, count(*), sum(amount) FROM ledger_entries WHERE account_id = :'hot' AND created_at >= timestamptz '2026-10-14' GROUP BY type;
\echo '### A2. O extrato (SELECT *) continua pedindo o heap: o coberto não ajuda quando se pede coluna fora do INCLUDE'
EXPLAIN (ANALYZE, BUFFERS, COSTS OFF) SELECT * FROM ledger_entries WHERE account_id = :'hot' ORDER BY created_at DESC LIMIT 20;
\echo '### B. Parcial: só as PENDING'
\timing on
CREATE INDEX idx_transfers_pending ON transfers (created_at) WHERE status = 'PENDING';
\timing off
CREATE INDEX idx_transfers_status_created ON transfers (status, created_at);
SELECT relname, pg_size_pretty(pg_relation_size(oid)) AS tamanho FROM pg_class WHERE relname IN ('idx_transfers_pending','idx_transfers_status_created','idx_transfers_source_account_id');
EXPLAIN (ANALYZE, BUFFERS, COSTS OFF) SELECT id, created_at FROM transfers WHERE status = 'PENDING' ORDER BY created_at LIMIT 50;
DROP INDEX idx_transfers_status_created;
\echo '### B2. Armadilha: parâmetro ($1) + plano genérico não usa o índice parcial'
PREPARE pend(text) AS SELECT id FROM transfers WHERE status = $1 ORDER BY created_at LIMIT 50;
SET plan_cache_mode = force_generic_plan;
EXPLAIN (COSTS OFF) EXECUTE pend('PENDING');
SET plan_cache_mode = force_custom_plan;
EXPLAIN (COSTS OFF) EXECUTE pend('PENDING');
RESET plan_cache_mode;
DEALLOCATE pend;
\echo '### C. Ordem das colunas: (created_at, account_id) em vez de (account_id, created_at)'
CREATE INDEX idx_wrong_order ON ledger_entries (created_at DESC, account_id);
SET enable_indexscan = on;
BEGIN;
DROP INDEX idx_ledger_entries_account_created, idx_ledger_entries_account_created_cov;   -- só nesta transação
EXPLAIN (ANALYZE, BUFFERS, COSTS OFF) SELECT * FROM ledger_entries WHERE account_id = :'hot' ORDER BY created_at DESC LIMIT 20;
ROLLBACK;
DROP INDEX idx_wrong_order;

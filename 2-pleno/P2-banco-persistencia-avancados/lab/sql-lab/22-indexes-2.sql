\set QUIET on
SELECT id AS hot FROM accounts WHERE id = (SELECT account_id FROM ledger_entries GROUP BY account_id ORDER BY count(*) DESC LIMIT 1) \gset
SELECT id AS normal FROM accounts a WHERE (SELECT count(*) FROM ledger_entries l WHERE l.account_id = a.id) BETWEEN 200 AND 260 LIMIT 1 \gset
\echo '### C2. Ordem errada (created_at, account_id) com conta NORMAL (≈250 lançamentos em 5 milhões)'
CREATE INDEX idx_wrong_order ON ledger_entries (created_at DESC, account_id);
BEGIN;
DROP INDEX idx_ledger_entries_account_created, idx_ledger_entries_account_created_cov;
EXPLAIN (ANALYZE, BUFFERS, COSTS OFF) SELECT * FROM ledger_entries WHERE account_id = :'normal' ORDER BY created_at DESC LIMIT 20;
ROLLBACK;
DROP INDEX idx_wrong_order;
\echo '### D. PostgreSQL 18 skip scan: índice (type, account_id, created_at) e consulta SEM type'
CREATE INDEX idx_skip ON ledger_entries (type, account_id, created_at);
BEGIN;
DROP INDEX idx_ledger_entries_account_created, idx_ledger_entries_account_created_cov;
EXPLAIN (ANALYZE, BUFFERS, COSTS OFF) SELECT count(*) FROM ledger_entries WHERE account_id = :'normal';
ROLLBACK;
DROP INDEX idx_skip;

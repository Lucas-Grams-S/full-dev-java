\set QUIET on
DELETE FROM ledger_entries WHERE created_at > timestamptz '2026-11-14 00:00:00+00';
VACUUM (ANALYZE) ledger_entries;
CREATE TEMP TABLE batch AS SELECT account_id, type, amount FROM ledger_entries LIMIT 200000;
\echo '### E. 200 000 INSERTs (uma transação, ROLLBACK ao fim): custo por quantidade de índices'
\timing on
\echo '--- 4 índices (pk, account_created, account_created_cov, transfer_id)'
BEGIN; INSERT INTO ledger_entries (account_id, type, amount) SELECT account_id, type, amount FROM batch; ROLLBACK;
BEGIN; INSERT INTO ledger_entries (account_id, type, amount) SELECT account_id, type, amount FROM batch; ROLLBACK;
\echo '--- 3 índices (sem o coberto)'
BEGIN; DROP INDEX idx_ledger_entries_account_created_cov; INSERT INTO ledger_entries (account_id, type, amount) SELECT account_id, type, amount FROM batch; ROLLBACK;
BEGIN; DROP INDEX idx_ledger_entries_account_created_cov; INSERT INTO ledger_entries (account_id, type, amount) SELECT account_id, type, amount FROM batch; ROLLBACK;
\echo '--- 1 índice (só a PK)'
BEGIN; DROP INDEX idx_ledger_entries_account_created_cov, idx_ledger_entries_account_created, idx_ledger_entries_transfer_id; INSERT INTO ledger_entries (account_id, type, amount) SELECT account_id, type, amount FROM batch; ROLLBACK;
BEGIN; DROP INDEX idx_ledger_entries_account_created_cov, idx_ledger_entries_account_created, idx_ledger_entries_transfer_id; INSERT INTO ledger_entries (account_id, type, amount) SELECT account_id, type, amount FROM batch; ROLLBACK;
\timing off
SELECT count(*) AS lancamentos_apos_rollback FROM ledger_entries;

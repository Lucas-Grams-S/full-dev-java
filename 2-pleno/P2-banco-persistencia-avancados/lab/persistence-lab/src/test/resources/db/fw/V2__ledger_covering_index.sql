CREATE INDEX CONCURRENTLY idx_ledger_entries_account_created_cov
    ON ledger_entries (account_id, created_at DESC) INCLUDE (type, amount);

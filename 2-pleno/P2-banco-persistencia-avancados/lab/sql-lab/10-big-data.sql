-- Volume para P2.04–P2.06 e P2.11: 1 000 usuários, 20 000 contas (a conta n=1 é "quente": 10% dos lançamentos),
-- 5 000 000 lançamentos em ordem cronológica (como um livro-razão real: só INSERT, tempo crescente), 1 000 000 transferências.
-- Esquema = V1..V3 (00-schema.sql). Os índices secundários da V4 são criados DEPOIS da carga (mais rápido).
\timing on
SET work_mem = '256MB';
SET maintenance_work_mem = '256MB';
SET synchronous_commit = off;
-- carga primeiro, índices depois (a V4 já existe no esquema: derrubamos e recriamos no fim)
DROP INDEX idx_accounts_owner_id, idx_transfers_source_account_id, idx_transfers_target_account_id, idx_ledger_entries_account_created, idx_ledger_entries_transfer_id;

INSERT INTO users (email, full_name, password_hash)
SELECT 'user' || g || '@example.com', 'Pessoa ' || g, 'x' FROM generate_series(1, 1000) g;

CREATE TEMP TABLE u AS SELECT row_number() OVER (ORDER BY id) AS n, id FROM users;
INSERT INTO accounts (owner_id, balance, created_at)
SELECT u.id, 1000, now() - interval '800 days' FROM generate_series(1, 20000) g JOIN u ON u.n = 1 + (g % 1000);

CREATE TEMP TABLE acc AS SELECT row_number() OVER (ORDER BY created_at, id) AS n, id FROM accounts;
CREATE UNIQUE INDEX ON acc (n);
ANALYZE acc;

INSERT INTO ledger_entries (account_id, type, amount, created_at)
SELECT acc.id, CASE WHEN r.t < 0.5 THEN 'DEBIT' ELSE 'CREDIT' END, round((1 + r.a * 499)::numeric, 2), r.ts
FROM (SELECT CASE WHEN random() < 0.10 THEN 1 ELSE 2 + floor(random() * 19999)::int END AS n,
             random() AS t, random() AS a,
             timestamptz '2026-11-14 00:00:00+00' - make_interval(secs => g * 730.0 * 86400 / 5000000) AS ts
      FROM generate_series(1, 5000000) g) r
JOIN acc ON acc.n = r.n
ORDER BY r.ts;

INSERT INTO transfers (source_account_id, target_account_id, amount, status, idempotency_key, created_at)
SELECT s.id, t.id, round((1 + random() * 499)::numeric, 2),
       CASE WHEN g % 1000 = 0 THEN 'PENDING' WHEN g % 50 = 0 THEN 'FAILED' ELSE 'COMPLETED' END,
       'k-' || g, timestamptz '2026-11-14 00:00:00+00' - make_interval(secs => g * 730.0 * 86400 / 1000000)
FROM generate_series(1, 1000000) g
JOIN acc s ON s.n = 1 + (g::bigint * 7919) % 20000
JOIN acc t ON t.n = 1 + (g::bigint * 104729 + 1) % 20000
WHERE s.id <> t.id;

-- Índices da V4 (J3.12): a "base" do PayFlow ao fim da J10/P1
CREATE INDEX idx_accounts_owner_id ON accounts (owner_id);
CREATE INDEX idx_transfers_source_account_id ON transfers (source_account_id);
CREATE INDEX idx_transfers_target_account_id ON transfers (target_account_id);
CREATE INDEX idx_ledger_entries_account_created ON ledger_entries (account_id, created_at DESC);
CREATE INDEX idx_ledger_entries_transfer_id ON ledger_entries (transfer_id);
VACUUM (ANALYZE);
SELECT 'ledger_entries' t, count(*), pg_size_pretty(pg_table_size('ledger_entries')) heap, pg_size_pretty(pg_indexes_size('ledger_entries')) indices FROM ledger_entries
UNION ALL SELECT 'transfers', count(*), pg_size_pretty(pg_table_size('transfers')), pg_size_pretty(pg_indexes_size('transfers')) FROM transfers
UNION ALL SELECT 'accounts', count(*), pg_size_pretty(pg_table_size('accounts')), pg_size_pretty(pg_indexes_size('accounts')) FROM accounts;

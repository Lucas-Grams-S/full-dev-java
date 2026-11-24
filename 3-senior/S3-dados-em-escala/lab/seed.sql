-- 200k users (phone in 4 messy formats), 200k accounts, 3M ledger entries over 24 months (2024-11..2026-10).
INSERT INTO users (email, full_name, phone, created_at)
SELECT 'user' || g || '@example.test', 'User ' || g,
       CASE g % 5 WHEN 0 THEN NULL
                  WHEN 1 THEN '(11) 9' || lpad((g % 10000000)::text, 4, '0') || '-' || lpad(((g*7) % 10000)::text, 4, '0')
                  WHEN 2 THEN '119' || lpad(((g*13) % 100000000)::text, 8, '0')
                  WHEN 3 THEN '+55 21 9' || lpad(((g*17) % 100000000)::text, 8, '0')
                  ELSE '021-9' || lpad(((g*19) % 100000000)::text, 8, '0') END,
       timestamptz '2024-11-01' + (g % 700) * interval '1 day'
FROM generate_series(1::bigint, 200000) g;
INSERT INTO accounts (owner_id, balance) SELECT id, 0 FROM users;
CREATE TEMP TABLE acc_n AS SELECT row_number() OVER (ORDER BY id) AS rn, id FROM accounts;
CREATE UNIQUE INDEX ON acc_n (rn);
INSERT INTO ledger_entries (account_id, type, amount, created_at)
SELECT a.id, CASE WHEN g % 2 = 0 THEN 'CREDIT' ELSE 'DEBIT' END, 1 + (g % 5000) / 100.0,
       timestamptz '2024-11-01' + g * interval '21 seconds' + random() * interval '20 seconds'
FROM generate_series(1::bigint, 3000000) g
JOIN acc_n a ON a.rn = 1 + (g * 7919) % 200000;
INSERT INTO transfers (source_account_id, target_account_id, amount, status, idempotency_key, created_at)
SELECT s.id, t.id, 10 + g % 90, CASE WHEN g % 50 = 0 THEN 'FAILED' ELSE 'COMPLETED' END, 'k-' || g,
       timestamptz '2024-11-01' + g * interval '63 seconds'
FROM generate_series(1::bigint, 1000000) g
JOIN acc_n s ON s.rn = 1 + (g * 31) % 200000 JOIN acc_n t ON t.rn = 1 + (g * 37 + 1) % 200000;
ANALYZE;

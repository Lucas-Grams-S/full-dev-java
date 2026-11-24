#!/usr/bin/env bash
# S3.06: what a CDC tool (Debezium) reads: PostgreSQL logical decoding with test_decoding (human-readable). Debezium and Kafka Connect NOT executed.
set -Eeuo pipefail; cd "$(dirname "$0")"; . ./lib.sh
OUT=${OUT:-.}
reset_db
{
psqlx <<'SQL'
\pset pager off
SELECT 'wal_level = ' || current_setting('wal_level');
CREATE TABLE cdc_users (id bigint PRIMARY KEY, email text NOT NULL, bio text);
SELECT 'slot created: ' || slot_name FROM pg_create_logical_replication_slot('s306', 'test_decoding');
\echo
\echo '## A) INSERT / UPDATE / DELETE as the slot delivers them (default REPLICA IDENTITY = primary key)'
INSERT INTO cdc_users VALUES (1, 'ana@example.test', 'short bio');
UPDATE cdc_users SET email = 'ana.silva@example.test' WHERE id = 1;
DELETE FROM cdc_users WHERE id = 1;
SELECT lsn, xid, data FROM pg_logical_slot_get_changes('s306', NULL, NULL);
\echo
\echo '## B) the same UPDATE with REPLICA IDENTITY FULL (old row values travel too)'
ALTER TABLE cdc_users REPLICA IDENTITY FULL;
INSERT INTO cdc_users VALUES (2, 'bob@example.test', 'short bio');
UPDATE cdc_users SET email = 'bob.souza@example.test' WHERE id = 2;
SELECT data FROM pg_logical_slot_get_changes('s306', NULL, NULL) WHERE data LIKE '%UPDATE%';
\echo
\echo '## C) a big (TOASTed) column that does not change is NOT sent'
ALTER TABLE cdc_users REPLICA IDENTITY DEFAULT;
INSERT INTO cdc_users VALUES (3, 'carla@example.test', (SELECT string_agg(md5(g::text), '') FROM generate_series(1, 400) g));
UPDATE cdc_users SET email = 'carla.lima@example.test' WHERE id = 3;
SELECT left(data, 150) AS data FROM pg_logical_slot_get_changes('s306', NULL, NULL) WHERE data LIKE '%UPDATE%';
\echo
\echo '## D) transactions: committed ones arrive whole, rolled-back ones never arrive; at-least-once with peek'
BEGIN; INSERT INTO cdc_users VALUES (10, 'a@example.test', 'x'); INSERT INTO cdc_users VALUES (11, 'b@example.test', 'x'); COMMIT;
BEGIN; INSERT INTO cdc_users VALUES (12, 'rolled-back@example.test', 'x'); ROLLBACK;
\echo '-- peek #1 (does not advance the slot)'
SELECT data FROM pg_logical_slot_peek_changes('s306', NULL, NULL);
\echo '-- peek #2 (a consumer that crashed before confirming sees the SAME events again)'
SELECT count(*) AS events_seen_again FROM pg_logical_slot_peek_changes('s306', NULL, NULL);
\echo '-- get (consume + confirm)'
SELECT count(*) AS events_consumed FROM pg_logical_slot_get_changes('s306', NULL, NULL);
SELECT count(*) AS events_left FROM pg_logical_slot_get_changes('s306', NULL, NULL);
SELECT pg_drop_replication_slot('s306');
SQL
} > "$OUT/out-s306-decoding.txt" 2>&1

{
echo '## E) the dangerous part: an abandoned slot makes the primary keep WAL (1M-row UPDATE ~ 390 MB of WAL)'
psqlx <<'SQL'
\pset pager off
SELECT 'slot created: ' || slot_name FROM pg_create_logical_replication_slot('idle_slot', 'test_decoding');
SELECT 'before: retained WAL ' || pg_size_pretty(pg_wal_lsn_diff(pg_current_wal_lsn(), restart_lsn)) || ', wal_status ' || wal_status FROM pg_replication_slots WHERE slot_name = 'idle_slot';
UPDATE transfers SET status = status;
CHECKPOINT;
SELECT 'after 1M-row UPDATE: retained WAL ' || pg_size_pretty(pg_wal_lsn_diff(pg_current_wal_lsn(), restart_lsn)) || ', wal_status ' || wal_status || ', active=' || active FROM pg_replication_slots WHERE slot_name = 'idle_slot';
SELECT 'pg_wal directory: ' || pg_size_pretty(sum(size)) FROM pg_ls_waldir();
\echo '-- the protection: max_slot_wal_keep_size = 64MB (slot is INVALIDATED instead of filling the disk)'
ALTER SYSTEM SET max_slot_wal_keep_size = '64MB'; SELECT pg_reload_conf();
SELECT pg_sleep(1);
CHECKPOINT;
SELECT 'right after the checkpoint: wal_status ' || wal_status || ', safe_wal_size ' || coalesce(pg_size_pretty(safe_wal_size), '-') FROM pg_replication_slots WHERE slot_name = 'idle_slot';
UPDATE transfers SET status = status WHERE id IN (SELECT id FROM transfers LIMIT 50000);
CHECKPOINT;
SELECT 'wal_status: ' || wal_status || ', invalidation_reason: ' || coalesce(invalidation_reason, '-') FROM pg_replication_slots WHERE slot_name = 'idle_slot';
SELECT 'pg_wal directory: ' || pg_size_pretty(sum(size)) FROM pg_ls_waldir();
\echo '-- a consumer that comes back to an invalidated slot:'
\set ON_ERROR_STOP off
SELECT count(*) AS changes FROM pg_logical_slot_get_changes('idle_slot', NULL, NULL);
\set ON_ERROR_STOP on
SELECT pg_drop_replication_slot('idle_slot');
ALTER SYSTEM RESET max_slot_wal_keep_size; SELECT pg_reload_conf();
SQL
} > "$OUT/out-s306-slot.txt" 2>&1

{
echo '## F) Outbox + CDC: the outbox_events table of P3.09 as the slot sees it (INSERT by the app, UPDATE by the polling relay, DELETE by the cleanup)'
psqlx <<'SQL'
\pset pager off
CREATE TABLE outbox_events (id uuid PRIMARY KEY DEFAULT uuidv7(), aggregate_id text NOT NULL, type text NOT NULL, payload text NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(), published_at timestamptz, attempts int NOT NULL DEFAULT 0);
SELECT 'slot: ' || slot_name FROM pg_create_logical_replication_slot('s306_outbox', 'test_decoding');
BEGIN;
INSERT INTO outbox_events (aggregate_id, type, payload) VALUES ('acc-1', 'TransferCompleted', '{"amount":"10.00","currency":"BRL"}');
COMMIT;
UPDATE outbox_events SET published_at = now(), attempts = attempts + 1;
DELETE FROM outbox_events;
SELECT regexp_replace(regexp_replace(data, '[0-9a-f]{8}-[0-9a-f-]{27}', '<uuid>', 'g'), '20[0-9-]+ [0-9:.+]+', '<ts>', 'g') AS data FROM pg_logical_slot_get_changes('s306_outbox', NULL, NULL);
SELECT pg_drop_replication_slot('s306_outbox');
SQL
} > "$OUT/out-s306-outbox.txt" 2>&1
cat "$OUT"/out-s306-*.txt | cut -c1-260

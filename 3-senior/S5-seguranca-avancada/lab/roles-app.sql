\set ON_ERROR_STOP 0

INSERT INTO ledger_entries(account_id, amount) VALUES (gen_random_uuid(), 10.00);
UPDATE ledger_entries SET amount = 9999;
DELETE FROM ledger_entries;
TRUNCATE ledger_entries;
DELETE FROM idempotency_keys;
SELECT purge_idempotency_keys(interval '0 seconds');
SELECT purge_idempotency_keys(interval '24 hours') AS purged_old_keys;
SELECT count(*) AS keys_left FROM idempotency_keys;
CREATE TABLE evil (x int);
DROP TABLE users;
INSERT INTO audit_events(actor, action, resource, outcome, request_id) VALUES ('user:42', 'TRANSFER', 'account/1', 'ALLOWED', 'req-1');
SELECT count(*) FROM audit_events;

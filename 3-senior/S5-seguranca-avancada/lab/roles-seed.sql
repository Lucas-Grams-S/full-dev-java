\set ON_ERROR_STOP 0

INSERT INTO users(name, phone_e164) VALUES ('Ana', '+5511999990000');
INSERT INTO idempotency_keys(key, created_at) VALUES ('old-1', now() - interval '3 days'), ('old-2', now() - interval '2 days'), ('fresh-1', now());

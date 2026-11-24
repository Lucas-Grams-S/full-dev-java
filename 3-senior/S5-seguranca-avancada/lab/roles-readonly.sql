\set ON_ERROR_STOP 0

SELECT id, name FROM users;
SELECT phone_e164 FROM users;
INSERT INTO users(name) VALUES ('x');
SELECT count(*) FROM audit_events;

-- S5.01 / S5.06: least privilege for the PayFlow database, tested on a throwaway PostgreSQL 18 (lab-s5-pg).
-- Superuser (postgres) only creates the roles; the OWNER of the tables is NOT a login role used by the application.
\set ON_ERROR_STOP 1
CREATE ROLE payflow_owner NOLOGIN;                       -- owns the objects; nobody logs in as it
CREATE ROLE payflow_migrator LOGIN PASSWORD 'mig' IN ROLE payflow_owner;   -- Flyway in the pipeline only
CREATE ROLE payflow_app LOGIN PASSWORD 'app';            -- the application: DML only where needed
CREATE ROLE payflow_readonly LOGIN PASSWORD 'ro';        -- analysts, support tooling
CREATE ROLE breakglass_ana LOGIN PASSWORD 'bg' VALID UNTIL 'infinity' IN ROLE payflow_owner;  -- human, time-boxed by an approved ticket
CREATE DATABASE payflow OWNER payflow_owner;
\c payflow
SET ROLE payflow_owner;
CREATE TABLE users (id uuid PRIMARY KEY DEFAULT uuidv7(), name text NOT NULL, phone_e164 text);
CREATE TABLE ledger_entries (id uuid PRIMARY KEY DEFAULT uuidv7(), account_id uuid NOT NULL, amount numeric(19,2) NOT NULL, created_at timestamptz NOT NULL DEFAULT now());
CREATE TABLE idempotency_keys (key text PRIMARY KEY, created_at timestamptz NOT NULL DEFAULT now());
CREATE TABLE audit_events (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  ts timestamptz NOT NULL DEFAULT now(),
  actor text NOT NULL, action text NOT NULL, resource text NOT NULL,
  outcome text NOT NULL CHECK (outcome IN ('ALLOWED','DENIED','ERROR')),
  request_id text NOT NULL, detail jsonb NOT NULL DEFAULT '{}'
);
CREATE FUNCTION audit_events_append_only() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'audit_events is append-only (% blocked)', TG_OP USING ERRCODE = 'insufficient_privilege'; END $$;
CREATE TRIGGER audit_events_no_update BEFORE UPDATE OR DELETE ON audit_events FOR EACH ROW EXECUTE FUNCTION audit_events_append_only();
CREATE TRIGGER audit_events_no_truncate BEFORE TRUNCATE ON audit_events FOR EACH STATEMENT EXECUTE FUNCTION audit_events_append_only();
-- the idempotency cleanup is a FUNCTION with a built-in floor: nobody (not even the app) can say "delete everything"
CREATE FUNCTION purge_idempotency_keys(older_than interval) RETURNS bigint LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE n bigint;
BEGIN
  IF older_than < interval '24 hours' THEN RAISE EXCEPTION 'refusing to purge keys younger than 24 hours (asked: %)', older_than; END IF;
  DELETE FROM idempotency_keys WHERE created_at < now() - older_than; GET DIAGNOSTICS n = ROW_COUNT; RETURN n;
END $$;
REVOKE ALL ON FUNCTION purge_idempotency_keys(interval) FROM PUBLIC;
RESET ROLE;
-- grants (default: nothing)
REVOKE ALL ON ALL TABLES IN SCHEMA public FROM PUBLIC;
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
GRANT USAGE ON SCHEMA public TO payflow_app, payflow_readonly;
GRANT SELECT, INSERT, UPDATE ON users TO payflow_app;
GRANT SELECT, INSERT ON ledger_entries TO payflow_app;            -- append-only ledger: no UPDATE, no DELETE
GRANT SELECT, INSERT ON idempotency_keys TO payflow_app;          -- no DELETE: cleanup goes through the function
GRANT INSERT ON audit_events TO payflow_app;                      -- may write, may not read or change
GRANT EXECUTE ON FUNCTION purge_idempotency_keys(interval) TO payflow_app;
GRANT SELECT ON users, ledger_entries, idempotency_keys TO payflow_readonly;   -- no audit_events, no phone? (columns: see below)
REVOKE SELECT ON users FROM payflow_readonly;
GRANT SELECT (id, name) ON users TO payflow_readonly;             -- column-level: no phone_e164

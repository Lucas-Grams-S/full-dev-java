-- PayFlow tables as they stand after P2/P10 (simplified): the S3 lab starts here.
CREATE TABLE users (
    id UUID NOT NULL DEFAULT uuidv7(), email VARCHAR(255) NOT NULL, full_name VARCHAR(120) NOT NULL,
    password_hash VARCHAR(100), role VARCHAR(10) NOT NULL DEFAULT 'USER', phone VARCHAR(20),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), CONSTRAINT pk_users PRIMARY KEY (id), CONSTRAINT uk_users_email UNIQUE (email));
CREATE TABLE accounts (
    id UUID NOT NULL DEFAULT uuidv7(), owner_id UUID NOT NULL, balance NUMERIC(19,2) NOT NULL DEFAULT 0,
    status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE', currency CHAR(3) NOT NULL DEFAULT 'BRL', version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), CONSTRAINT pk_accounts PRIMARY KEY (id),
    CONSTRAINT fk_accounts_owner FOREIGN KEY (owner_id) REFERENCES users (id));
CREATE TABLE transfers (
    id UUID NOT NULL DEFAULT uuidv7(), source_account_id UUID NOT NULL, target_account_id UUID NOT NULL,
    amount NUMERIC(19,2) NOT NULL, status VARCHAR(12) NOT NULL, idempotency_key VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), CONSTRAINT pk_transfers PRIMARY KEY (id),
    CONSTRAINT uk_transfers_idem UNIQUE (idempotency_key));
CREATE TABLE ledger_entries (
    id UUID NOT NULL DEFAULT uuidv7(), account_id UUID NOT NULL, transfer_id UUID, type VARCHAR(10) NOT NULL,
    amount NUMERIC(19,2) NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_ledger_entries PRIMARY KEY (id), CONSTRAINT ck_ledger_type CHECK (type IN ('DEBIT','CREDIT')),
    CONSTRAINT ck_ledger_amount CHECK (amount > 0));
CREATE INDEX idx_ledger_entries_account_created ON ledger_entries (account_id, created_at DESC);
CREATE FUNCTION ledger_append_only() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'ledger_entries is append-only (% blocked)', TG_OP; END $$;
CREATE TRIGGER trg_ledger_append_only BEFORE UPDATE OR DELETE ON ledger_entries
    FOR EACH ROW EXECUTE FUNCTION ledger_append_only();

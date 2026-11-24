-- V1__create_users.sql
CREATE TABLE users (
    id            UUID         NOT NULL DEFAULT uuidv7(),
    email         VARCHAR(255) NOT NULL,
    full_name     VARCHAR(120) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'USER',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('USER', 'ADMIN'))
);

-- V2__create_accounts.sql
CREATE TABLE accounts (
    id         UUID           NOT NULL DEFAULT uuidv7(),
    owner_id   UUID           NOT NULL,
    balance    NUMERIC(19, 2) NOT NULL DEFAULT 0,
    status     VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    version    BIGINT         NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT pk_accounts PRIMARY KEY (id),
    CONSTRAINT fk_accounts_owner FOREIGN KEY (owner_id) REFERENCES users (id),
    CONSTRAINT ck_accounts_balance CHECK (balance >= 0),
    CONSTRAINT ck_accounts_status CHECK (status IN ('ACTIVE', 'BLOCKED', 'CLOSED'))
);

-- V3__create_transfers_and_ledger_entries.sql
CREATE TABLE transfers (
    id                UUID           NOT NULL DEFAULT uuidv7(),
    source_account_id UUID           NOT NULL,
    target_account_id UUID           NOT NULL,
    amount            NUMERIC(19, 2) NOT NULL,
    status            VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    idempotency_key   VARCHAR(100)   NOT NULL,
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT pk_transfers PRIMARY KEY (id),
    CONSTRAINT fk_transfers_source FOREIGN KEY (source_account_id) REFERENCES accounts (id),
    CONSTRAINT fk_transfers_target FOREIGN KEY (target_account_id) REFERENCES accounts (id),
    CONSTRAINT uk_transfers_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT ck_transfers_amount CHECK (amount > 0),
    CONSTRAINT ck_transfers_status CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED')),
    CONSTRAINT ck_transfers_different_accounts CHECK (source_account_id <> target_account_id)
);

CREATE TABLE ledger_entries (
    id          UUID           NOT NULL DEFAULT uuidv7(),
    account_id  UUID           NOT NULL,
    transfer_id UUID,
    type        VARCHAR(10)    NOT NULL,
    amount      NUMERIC(19, 2) NOT NULL,
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT pk_ledger_entries PRIMARY KEY (id),
    CONSTRAINT fk_ledger_entries_account FOREIGN KEY (account_id) REFERENCES accounts (id),
    CONSTRAINT fk_ledger_entries_transfer FOREIGN KEY (transfer_id) REFERENCES transfers (id),
    CONSTRAINT ck_ledger_entries_type CHECK (type IN ('DEBIT', 'CREDIT')),
    CONSTRAINT ck_ledger_entries_amount CHECK (amount > 0)
);

-- V4__create_indexes.sql
-- Índices nas colunas de chave estrangeira e no caminho do extrato (J3.09)
CREATE INDEX idx_accounts_owner_id ON accounts (owner_id);
CREATE INDEX idx_transfers_source_account_id ON transfers (source_account_id);
CREATE INDEX idx_transfers_target_account_id ON transfers (target_account_id);
CREATE INDEX idx_ledger_entries_account_created ON ledger_entries (account_id, created_at DESC);
CREATE INDEX idx_ledger_entries_transfer_id ON ledger_entries (transfer_id);


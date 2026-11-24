CREATE TABLE ledger_entries (
    id         BIGSERIAL PRIMARY KEY,
    account_id INT         NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    type       VARCHAR(10) NOT NULL,
    amount     NUMERIC(19, 2) NOT NULL
);
INSERT INTO ledger_entries (account_id, type, amount) SELECT g % 1000, 'CREDIT', 1 FROM generate_series(1, 200000) g;

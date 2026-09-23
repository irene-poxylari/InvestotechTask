CREATE TABLE users (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    api_key_hash CHAR(64) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE accounts (
    id VARCHAR(100) PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    currency CHAR(3) NOT NULL,
    balance_minor BIGINT NOT NULL CHECK (balance_minor >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_accounts_customer_id ON accounts(customer_id);

CREATE TABLE transfers (
    id UUID PRIMARY KEY,
    source_account_id VARCHAR(100) NOT NULL REFERENCES accounts(id),
    destination_account_id VARCHAR(100) NOT NULL REFERENCES accounts(id),
    amount_minor BIGINT NOT NULL CHECK (amount_minor > 0),
    currency CHAR(3) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_transfers_source_history
    ON transfers(source_account_id, created_at DESC, id DESC);
CREATE INDEX idx_transfers_destination_history
    ON transfers(destination_account_id, created_at DESC, id DESC);

CREATE TABLE idempotency_keys (
    customer_id UUID NOT NULL REFERENCES customers(id),
    idempotency_key VARCHAR(200) NOT NULL,
    request_hash CHAR(64) NOT NULL,
    transfer_id UUID NOT NULL REFERENCES transfers(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (customer_id, idempotency_key)
);

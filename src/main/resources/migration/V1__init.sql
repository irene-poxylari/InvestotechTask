CREATE TABLE users (
                       id BINARY(16) PRIMARY KEY,
                       api_key_hash CHAR(64) NOT NULL UNIQUE,
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE accounts (
                          id VARCHAR(100) PRIMARY KEY,
                          user_id BINARY(16) NOT NULL,
                          currency CHAR(3) NOT NULL,
                          balance_minor BIGINT NOT NULL,
                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                          CONSTRAINT fk_accounts_user
                              FOREIGN KEY (user_id)
                                  REFERENCES users(id),

                          CONSTRAINT chk_accounts_balance
                              CHECK (balance_minor >= 0)
);

CREATE INDEX idx_accounts_user_id
    ON accounts(user_id);


CREATE TABLE transfers (
                           id BINARY(16) PRIMARY KEY,
                           source_account_id VARCHAR(100) NOT NULL,
                           destination_account_id VARCHAR(100) NOT NULL,
                           amount_minor BIGINT NOT NULL,
                           currency CHAR(3) NOT NULL,
                           created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                           CONSTRAINT fk_transfer_source
                               FOREIGN KEY (source_account_id)
                                   REFERENCES accounts(id),

                           CONSTRAINT fk_transfer_destination
                               FOREIGN KEY (destination_account_id)
                                   REFERENCES accounts(id),

                           CONSTRAINT chk_transfer_amount
                               CHECK (amount_minor > 0)
);

CREATE INDEX idx_transfers_source_history
    ON transfers(source_account_id, created_at DESC, id DESC);

CREATE INDEX idx_transfers_destination_history
    ON transfers(destination_account_id, created_at DESC, id DESC);


CREATE TABLE idempotency_keys (
                                  user_id BINARY(16) NOT NULL,
                                  idempotency_key VARCHAR(200) NOT NULL,
                                  request_hash CHAR(64) NOT NULL,
                                  transfer_id BINARY(16) NOT NULL,
                                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                  PRIMARY KEY (user_id, idempotency_key),

                                  CONSTRAINT fk_idempotency_user
                                      FOREIGN KEY (user_id)
                                          REFERENCES users(id),

                                  CONSTRAINT fk_idempotency_transfer
                                      FOREIGN KEY (transfer_id)
                                          REFERENCES transfers(id)
);

CREATE TABLE idempotency_locks (
    lock_key VARCHAR(255) PRIMARY KEY
);
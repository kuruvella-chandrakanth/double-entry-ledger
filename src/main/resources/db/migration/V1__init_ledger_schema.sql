CREATE TABLE accounts (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    name       VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE ledger_transactions (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    idempotency_key VARCHAR(255) NOT NULL,
    description     VARCHAR(1000),
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    -- UNIQUE constraint is the DB-level safety net for idempotency.
    -- Even if two threads race past the application check, only one INSERT wins.
    UNIQUE KEY uk_idempotency_key (idempotency_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE ledger_entries (
    id             BIGINT     NOT NULL AUTO_INCREMENT,
    transaction_id BIGINT     NOT NULL,
    account_id     BIGINT     NOT NULL,
    entry_type     ENUM('DEBIT','CREDIT') NOT NULL,
    amount         BIGINT     NOT NULL,  -- minor units (cents), always > 0
    created_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_entry_transaction FOREIGN KEY (transaction_id) REFERENCES ledger_transactions(id),
    CONSTRAINT fk_entry_account     FOREIGN KEY (account_id)     REFERENCES accounts(id),
    CONSTRAINT chk_positive_amount  CHECK (amount > 0),
    INDEX idx_account_created (account_id, created_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

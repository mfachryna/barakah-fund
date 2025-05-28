CREATE TABLE
    transaction_logs (
        log_id VARCHAR(36) PRIMARY KEY,
        transaction_id VARCHAR(36) NOT NULL,
        account_id VARCHAR(36) NOT NULL,
        account_number VARCHAR(20) NOT NULL,
        direction VARCHAR(30) NOT NULL CHECK (direction IN ('DEBIT', 'CREDIT')),
        amount DECIMAL(19, 0) NOT NULL,
        balance_before DECIMAL(19, 0) NOT NULL,
        balance_after DECIMAL(19, 0) NOT NULL,
        timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        notes TEXT,
        CONSTRAINT fk_transaction_log_transaction FOREIGN KEY (transaction_id) REFERENCES transactions (transaction_id) ON DELETE CASCADE,
        CONSTRAINT chk_log_amount_positive CHECK (amount > 0),
        CONSTRAINT chk_log_balance_non_negative
            CHECK (balance_before >= 0 AND balance_after >= 0)
    );

CREATE INDEX idx_log_transaction ON transaction_logs (transaction_id);

CREATE INDEX idx_log_account ON transaction_logs (account_id);

CREATE INDEX idx_log_timestamp ON transaction_logs (timestamp);

CREATE INDEX idx_log_account_number ON transaction_logs (account_number);

CREATE INDEX idx_log_direction ON transaction_logs (direction);

CREATE INDEX idx_log_account_timestamp ON transaction_logs (account_id, timestamp);

CREATE INDEX idx_log_account_direction ON transaction_logs (account_id, direction);

CREATE INDEX idx_log_transaction_direction ON transaction_logs (transaction_id, direction);

CREATE INDEX idx_log_account_timestamp_desc ON transaction_logs (account_id, timestamp DESC);
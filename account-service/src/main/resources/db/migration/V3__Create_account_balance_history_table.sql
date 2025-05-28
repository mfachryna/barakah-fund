CREATE TABLE
    account_balance_history (
        history_id VARCHAR(36) PRIMARY KEY,
        account_id VARCHAR(36) NOT NULL,
        account_number VARCHAR(20) NOT NULL,
        operation VARCHAR(20) NOT NULL  CHECK (operation IN ('CREDIT', 'DEBIT')),
        amount DECIMAL(19, 2) NOT NULL,
        balance_before DECIMAL(19, 2) NOT NULL,
        balance_after DECIMAL(19, 2) NOT NULL,
        transaction_id VARCHAR(36),
        description VARCHAR(500),
        timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

        FOREIGN KEY (account_id) REFERENCES accounts (id) ON DELETE CASCADE,
        
        CONSTRAINT chk_balance_history_amount_positive CHECK (amount > 0),
        CONSTRAINT chk_balance_history_balance_non_negative CHECK (
            balance_before >= 0
            AND balance_after >= 0
        )
    );

-- Create indexes
CREATE INDEX idx_balance_history_account ON account_balance_history (account_id);

CREATE INDEX idx_balance_history_transaction ON account_balance_history (transaction_id);

CREATE INDEX idx_balance_history_timestamp ON account_balance_history (timestamp);

CREATE INDEX idx_balance_history_account_timestamp ON account_balance_history (account_id, timestamp);

CREATE INDEX idx_balance_history_operation ON account_balance_history (operation);

CREATE INDEX idx_balance_history_account_number ON account_balance_history (account_number);
CREATE TABLE transactions (
    transaction_id VARCHAR(36) PRIMARY KEY,
    reference_number VARCHAR(50) NOT NULL UNIQUE,
    type VARCHAR NOT NULL DEFAULT 'TRANSFER' CHECK (type IN ('TRANSFER', 'DEPOSIT', 'WITHDRAWAL', 'PAYMENT', 'REFUND', 'FEE', 'INTEREST')),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED', 'REVERSED')),
    direction VARCHAR(30) NOT NULL DEFAULT 'DEBIT' CHECK (direction IN ('DEBIT', 'CREDIT')),
    transfer_type VARCHAR(30) DEFAULT 'INTERNAL' CHECK (transfer_type IN ('INTERNAL', 'EXTERNAL')),
    from_account_id VARCHAR(36),
    from_account_number VARCHAR(20),
    to_account_id VARCHAR(36),
    to_account_number VARCHAR(20),
    amount DECIMAL(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'IDR',
    description VARCHAR(500),
    notes TEXT,
    category_id VARCHAR(36),
    balance_before DECIMAL(19, 2),
    balance_after DECIMAL(19, 2),
    external_reference VARCHAR(100),
    external_provider VARCHAR(50),
    created_by VARCHAR(36),
    updated_by VARCHAR(36),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    

    CONSTRAINT fk_transaction_category 
        FOREIGN KEY (category_id) 
        REFERENCES transaction_categories (category_id) 
        ON DELETE SET NULL,
    

    CONSTRAINT chk_amount_positive 
        CHECK (amount > 0),
    CONSTRAINT chk_balance_non_negative 
        CHECK (balance_before >= 0 AND balance_after >= 0),
    CONSTRAINT chk_account_transfer 
        CHECK (
            (type = 'TRANSFER' AND from_account_id IS NOT NULL AND to_account_id IS NOT NULL) OR
            (type != 'TRANSFER')
        ),
    CONSTRAINT chk_external_transfer
        CHECK (
            (transfer_type = 'EXTERNAL' AND external_reference IS NOT NULL) OR
            (transfer_type != 'EXTERNAL' OR transfer_type IS NULL)
        )
);

CREATE INDEX idx_transaction_reference ON transactions(reference_number);
CREATE INDEX idx_transaction_from_account ON transactions(from_account_id);
CREATE INDEX idx_transaction_to_account ON transactions(to_account_id);
CREATE INDEX idx_transaction_status ON transactions(status);
CREATE INDEX idx_transaction_type ON transactions(type);
CREATE INDEX idx_transaction_created_at ON transactions(created_at);
CREATE INDEX idx_transaction_category ON transactions(category_id);
CREATE INDEX idx_transaction_direction ON transactions(direction);
CREATE INDEX idx_transaction_transfer_type ON transactions(transfer_type);
CREATE INDEX idx_transaction_from_account_number ON transactions(from_account_number);
CREATE INDEX idx_transaction_to_account_number ON transactions(to_account_number);
CREATE INDEX idx_transaction_external_ref ON transactions(external_reference);
CREATE INDEX idx_transaction_created_by ON transactions(created_by);

CREATE INDEX idx_transaction_account_status ON transactions(from_account_id, status);
CREATE INDEX idx_transaction_account_type ON transactions(from_account_id, type);
CREATE INDEX idx_transaction_date_status ON transactions(created_at, status);

CREATE TRIGGER update_transactions_updated_at
    BEFORE UPDATE ON transactions
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

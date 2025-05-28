-- Additional composite indexes for better query performance

-- For transaction filtering and pagination
CREATE INDEX idx_transaction_account_status_created ON transactions(from_account_id, status, created_at);
CREATE INDEX idx_transaction_account_type_created ON transactions(to_account_id, type, created_at);
CREATE INDEX idx_transaction_category_status ON transactions(category_id, status);
CREATE INDEX idx_transaction_status_created ON transactions(status, created_at);

-- For transaction log queries
CREATE INDEX idx_log_account_direction_timestamp ON transaction_logs(account_id, direction, timestamp);
CREATE INDEX idx_log_transaction_account ON transaction_logs(transaction_id, account_id);

-- For category queries
CREATE INDEX idx_category_active_system ON transaction_categories(is_active, is_system);
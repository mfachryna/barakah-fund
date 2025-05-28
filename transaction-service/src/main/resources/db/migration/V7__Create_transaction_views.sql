CREATE VIEW v_account_transaction_summary AS
SELECT 
    COALESCE(from_account_id, to_account_id) as account_id,
    COALESCE(from_account_number, to_account_number) as account_number,
    COUNT(*) as total_transactions,
    SUM(CASE WHEN direction = 'DEBIT' THEN amount ELSE 0 END) as total_debits,
    SUM(CASE WHEN direction = 'CREDIT' THEN amount ELSE 0 END) as total_credits,
    MIN(created_at) as first_transaction,
    MAX(created_at) as last_transaction
FROM transactions 
WHERE status = 'COMPLETED'
GROUP BY COALESCE(from_account_id, to_account_id), COALESCE(from_account_number, to_account_number);

-- View for daily transaction summary
CREATE VIEW v_daily_transaction_summary AS
SELECT 
    DATE(created_at) as transaction_date,
    type,
    status,
    COUNT(*) as transaction_count,
    SUM(amount) as total_amount,
    AVG(amount) as average_amount
FROM transactions 
GROUP BY DATE(created_at), type, status;

-- View for category usage statistics
CREATE VIEW v_category_usage_stats AS
SELECT 
    tc.category_id,
    tc.name as category_name,
    tc.is_system,
    COUNT(t.transaction_id) as usage_count,
    COALESCE(SUM(t.amount), 0) as total_amount,
    MAX(t.created_at) as last_used
FROM transaction_categories tc
LEFT JOIN transactions t ON tc.category_id = t.category_id AND t.status = 'COMPLETED'
GROUP BY tc.category_id, tc.name, tc.is_system;
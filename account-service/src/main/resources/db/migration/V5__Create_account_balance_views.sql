CREATE VIEW v_account_balance_trends AS
SELECT 
    account_id,
    account_number,
    DATE(timestamp) as balance_date,
    MIN(balance_before) as min_balance,
    MAX(balance_after) as max_balance,
    COUNT(*) as transaction_count,
    SUM(CASE WHEN operation = 'CREDIT' THEN amount ELSE 0 END) as total_credits,
    SUM(CASE WHEN operation = 'DEBIT' THEN amount ELSE 0 END) as total_debits
FROM account_balance_history
GROUP BY account_id, account_number, DATE(timestamp);

CREATE VIEW v_recent_account_activity AS
SELECT 
    abh.*,
    a.account_name,
    a.account_type,
    a.status as account_status
FROM account_balance_history abh
JOIN accounts a ON abh.account_id = a.id
WHERE abh.timestamp >= NOW() - INTERVAL '30 days'
ORDER BY abh.timestamp DESC;
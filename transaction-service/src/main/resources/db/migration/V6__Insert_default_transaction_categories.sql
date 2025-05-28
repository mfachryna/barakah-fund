CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

INSERT INTO transaction_categories (category_id, name, description, icon, color, is_active, is_system, created_by) VALUES
(uuid_generate_v4(), 'Transfer', 'Money transfer between accounts', 'transfer', '#2196F3', TRUE, TRUE, 'SYSTEM'),
(uuid_generate_v4(), 'Deposit', 'Money deposit', 'deposit', '#4CAF50', TRUE, TRUE, 'SYSTEM'),
(uuid_generate_v4(), 'Withdrawal', 'Money withdrawal', 'withdrawal', '#FF9800', TRUE, TRUE, 'SYSTEM'),
(uuid_generate_v4(), 'Payment', 'Payment to external parties', 'payment', '#F44336', TRUE, TRUE, 'SYSTEM'),
(uuid_generate_v4(), 'Refund', 'Refund from external parties', 'refund', '#9C27B0', TRUE, TRUE, 'SYSTEM'),
(uuid_generate_v4(), 'Fee', 'Service fees', 'fee', '#607D8B', TRUE, TRUE, 'SYSTEM'),
(uuid_generate_v4(), 'Interest', 'Interest earned or paid', 'interest', '#00BCD4', TRUE, TRUE, 'SYSTEM'),
(uuid_generate_v4(), 'Food & Dining', 'Restaurant, grocery, etc.', 'restaurant', '#FF5722', TRUE, TRUE, 'SYSTEM'),
(uuid_generate_v4(), 'Transportation', 'Gas, public transport, etc.', 'car', '#795548', TRUE, TRUE, 'SYSTEM'),
(uuid_generate_v4(), 'Shopping', 'Clothing, electronics, etc.', 'shopping', '#E91E63', TRUE, TRUE, 'SYSTEM'),
(uuid_generate_v4(), 'Entertainment', 'Movies, games, etc.', 'entertainment', '#9C27B0', TRUE, TRUE, 'SYSTEM'),
(uuid_generate_v4(), 'Bills & Utilities', 'Electricity, water, internet, etc.', 'bill', '#FF9800', TRUE, TRUE, 'SYSTEM'),
(uuid_generate_v4(), 'Healthcare', 'Medical expenses', 'health', '#4CAF50', TRUE, TRUE, 'SYSTEM'),
(uuid_generate_v4(), 'Education', 'School, courses, books', 'education', '#2196F3', TRUE, TRUE, 'SYSTEM'),
(uuid_generate_v4(), 'Investment', 'Stocks, bonds, etc.', 'investment', '#FF9800', TRUE, TRUE, 'SYSTEM'),
(uuid_generate_v4(), 'Salary', 'Monthly salary', 'salary', '#4CAF50', TRUE, TRUE, 'SYSTEM'),
(uuid_generate_v4(), 'Other', 'Miscellaneous transactions', 'other', '#9E9E9E', TRUE, TRUE, 'SYSTEM');
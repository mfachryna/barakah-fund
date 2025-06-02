CREATE DATABASE barakah_users;
CREATE DATABASE barakah_accounts;
CREATE DATABASE barakah_transactions;

CREATE USER barakah_user WITH PASSWORD 'barakah_user_password';
CREATE USER barakah_account WITH PASSWORD 'barakah_account_password';
CREATE USER barakah_transaction WITH PASSWORD 'barakah_transaction_password';

GRANT ALL PRIVILEGES ON DATABASE barakah_users TO barakah_user;
\c barakah_users;
GRANT ALL ON SCHEMA public TO barakah_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO barakah_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO barakah_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO barakah_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO barakah_user;

\c barakah_main;
GRANT ALL PRIVILEGES ON DATABASE barakah_accounts TO barakah_account;
\c barakah_accounts;
GRANT ALL ON SCHEMA public TO barakah_account;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO barakah_account;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO barakah_account;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO barakah_account;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO barakah_account;

\c barakah_main;
GRANT ALL PRIVILEGES ON DATABASE barakah_transactions TO barakah_transaction;
\c barakah_transactions;
GRANT ALL ON SCHEMA public TO barakah_transaction;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO barakah_transaction;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO barakah_transaction;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO barakah_transaction;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO barakah_transaction;
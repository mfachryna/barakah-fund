-- Create user service database
CREATE DATABASE barakah_users;
CREATE USER barakah_user WITH ENCRYPTED PASSWORD 'barakah_password';
GRANT ALL PRIVILEGES ON DATABASE barakah_users TO barakah_user;

-- Create account service database (for future use)
CREATE DATABASE barakah_accounts;
CREATE USER barakah_account WITH ENCRYPTED PASSWORD 'barakah_account_password';
GRANT ALL PRIVILEGES ON DATABASE barakah_accounts TO barakah_account;

-- Create transaction service database (for future use)
CREATE DATABASE barakah_transactions;
CREATE USER barakah_transaction WITH ENCRYPTED PASSWORD 'barakah_transaction_password';
GRANT ALL PRIVILEGES ON DATABASE barakah_transactions TO barakah_transaction;

-- Connect to user database and set up initial schema
\c barakah_users;
ALTER DATABASE barakah_users OWNER TO barakah_user;
GRANT ALL ON SCHEMA public TO barakah_user;

-- Connect to account database and set up initial schema
\c barakah_accounts;
ALTER DATABASE barakah_accounts OWNER TO barakah_account;
GRANT ALL ON SCHEMA public TO barakah_account;

-- Connect to transaction database and set up initial schema
\c barakah_transactions;
ALTER DATABASE barakah_transactions OWNER TO barakah_transaction;
GRANT ALL ON SCHEMA public TO barakah_transaction;
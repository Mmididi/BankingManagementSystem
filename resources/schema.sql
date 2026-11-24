CREATE TABLE IF NOT EXISTS users (
    user_id TEXT PRIMARY KEY,
    username TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    role TEXT NOT NULL CHECK (
        role IN ('CUSTOMER', 'TELLER', 'ADMIN')
    ),
    active INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0, 1))
);

CREATE TABLE IF NOT EXISTS customers (
    customer_id TEXT PRIMARY KEY,
    customer_type TEXT NOT NULL CHECK (
        customer_type IN ('INDIVIDUAL', 'ORGANISATION')
    ),
    first_name TEXT,
    surname TEXT,
    address TEXT,
    gender TEXT,
    email TEXT,
    cell_no TEXT,
    identity_number TEXT UNIQUE,
    organisation_name TEXT,
    registration_number TEXT UNIQUE,
    business_address TEXT,
    user_id TEXT UNIQUE,
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    CHECK (
        (customer_type = 'INDIVIDUAL'
            AND first_name IS NOT NULL
            AND surname IS NOT NULL)
        OR
        (customer_type = 'ORGANISATION'
            AND organisation_name IS NOT NULL)
    )
);

CREATE TABLE IF NOT EXISTS customer_id_sequence (
    id INTEGER PRIMARY KEY AUTOINCREMENT
);

CREATE TABLE IF NOT EXISTS employment_details (
    customer_id TEXT PRIMARY KEY,
    employer_name TEXT NOT NULL,
    employer_address TEXT NOT NULL,
    job_title TEXT,
    employed INTEGER NOT NULL CHECK (employed IN (0, 1)),
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
);

CREATE TABLE IF NOT EXISTS accounts (
    account_number TEXT PRIMARY KEY,
    customer_id TEXT NOT NULL,
    account_type TEXT NOT NULL CHECK (
        account_type IN ('SAVINGS', 'INVESTMENT', 'CHEQUE')
    ),
    balance NUMERIC NOT NULL DEFAULT 0 CHECK (balance >= 0),
    branch TEXT NOT NULL,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
);

CREATE TABLE IF NOT EXISTS transactions (
    transaction_id TEXT PRIMARY KEY,
    account_number TEXT NOT NULL,
    transaction_date TEXT NOT NULL,
    transaction_type TEXT NOT NULL CHECK (
        transaction_type IN ('DEPOSIT', 'WITHDRAWAL', 'INTEREST')
    ),
    amount NUMERIC NOT NULL CHECK (amount > 0),
    created_by TEXT,
    FOREIGN KEY (account_number) REFERENCES accounts(account_number),
    FOREIGN KEY (created_by) REFERENCES users(user_id)
);

CREATE TABLE IF NOT EXISTS demo_setup (
    id INTEGER PRIMARY KEY CHECK (id = 1)
);

CREATE TABLE IF NOT EXISTS presentation_setup (
    id INTEGER PRIMARY KEY CHECK (id = 1)
);

CREATE INDEX IF NOT EXISTS accounts_owner
    ON accounts(customer_id);

CREATE INDEX IF NOT EXISTS transactions_account
    ON transactions(account_number, transaction_date);

CREATE VIEW IF NOT EXISTS transaction_ledger AS
SELECT t.*,
       CAST(ROUND(a.balance * 100) AS INTEGER) - COALESCE(SUM(
           CASE t.transaction_type WHEN 'WITHDRAWAL' THEN -1 ELSE 1 END
           * CAST(ROUND(t.amount * 100) AS INTEGER)
       ) OVER (
           PARTITION BY t.account_number ORDER BY t.rowid
           ROWS BETWEEN 1 FOLLOWING AND UNBOUNDED FOLLOWING
       ), 0) AS balance_after_cents
FROM transactions t
JOIN accounts a ON a.account_number = t.account_number;
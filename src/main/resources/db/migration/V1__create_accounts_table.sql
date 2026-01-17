-- Create accounts table
CREATE TABLE accounts (
    account_id UUID PRIMARY KEY,
    account_type VARCHAR(50) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT check_account_type CHECK (account_type IN ('ASSET', 'LIABILITY', 'EQUITY', 'REVENUE', 'EXPENSE')),
    CONSTRAINT check_account_status CHECK (status IN ('ACTIVE', 'BLOCKED', 'CLOSED'))
);

-- Indexes for common queries
CREATE INDEX idx_accounts_status ON accounts(status);
CREATE INDEX idx_accounts_type ON accounts(account_type);
CREATE INDEX idx_accounts_currency ON accounts(currency);

-- Comments for documentation
COMMENT ON TABLE accounts IS 'Financial accounts in the ledger system';
COMMENT ON COLUMN accounts.account_id IS 'Unique identifier for the account';
COMMENT ON COLUMN accounts.account_type IS 'Type of account (ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE)';
COMMENT ON COLUMN accounts.currency IS 'ISO 4217 currency code';
COMMENT ON COLUMN accounts.status IS 'Account status (ACTIVE, BLOCKED, CLOSED)';
COMMENT ON COLUMN accounts.created_at IS 'Timestamp when account was created';

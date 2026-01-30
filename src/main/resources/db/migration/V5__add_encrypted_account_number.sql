-- Migration to add encrypted account number field to accounts table
-- This enables encryption at rest for sensitive account information

ALTER TABLE accounts 
ADD COLUMN account_number VARCHAR(500);

-- Add index for account number lookup (even though encrypted)
-- Note: Encrypted fields cannot be efficiently searched, so consider this carefully
CREATE INDEX idx_accounts_account_number ON accounts(account_number);

-- Add comment to document encryption
COMMENT ON COLUMN accounts.account_number IS 'Encrypted account number using AES-256 encryption';

-- Create ledger_entries table
CREATE TABLE ledger_entries (
    ledger_entry_id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL,
    account_id UUID NOT NULL,
    amount DECIMAL(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    entry_type VARCHAR(10) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    event_time TIMESTAMP NOT NULL,
    recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT check_amount_positive CHECK (amount > 0),
    CONSTRAINT check_entry_type CHECK (entry_type IN ('DEBIT', 'CREDIT')),
    CONSTRAINT fk_entry_transaction FOREIGN KEY (transaction_id) REFERENCES ledger_transactions(transaction_id),
    CONSTRAINT fk_entry_account FOREIGN KEY (account_id) REFERENCES accounts(account_id)
);

-- Indexes for common queries
CREATE INDEX idx_entries_transaction_id ON ledger_entries(transaction_id);
CREATE INDEX idx_entries_account_id ON ledger_entries(account_id);
CREATE INDEX idx_entries_event_time ON ledger_entries(event_time DESC);
CREATE INDEX idx_entries_recorded_at ON ledger_entries(recorded_at DESC);

-- Composite index for account balance queries
CREATE INDEX idx_entries_account_event_time ON ledger_entries(account_id, event_time DESC);

-- Comments for documentation
COMMENT ON TABLE ledger_entries IS 'Individual ledger entries (lines in double-entry bookkeeping)';
COMMENT ON COLUMN ledger_entries.ledger_entry_id IS 'Unique identifier for the entry';
COMMENT ON COLUMN ledger_entries.transaction_id IS 'Transaction this entry belongs to';
COMMENT ON COLUMN ledger_entries.account_id IS 'Account this entry affects';
COMMENT ON COLUMN ledger_entries.amount IS 'Positive amount (direction indicated by entry_type)';
COMMENT ON COLUMN ledger_entries.currency IS 'ISO 4217 currency code';
COMMENT ON COLUMN ledger_entries.entry_type IS 'DEBIT or CREDIT';
COMMENT ON COLUMN ledger_entries.event_type IS 'Type of business event that created this entry';
COMMENT ON COLUMN ledger_entries.event_time IS 'When the business event occurred';
COMMENT ON COLUMN ledger_entries.recorded_at IS 'When the entry was persisted to the ledger';

-- Create balance_snapshots table for snapshot-based balance calculation
-- This table stores pre-calculated balances at specific points in time
-- to optimize balance queries for accounts with large transaction histories

CREATE TABLE balance_snapshots (
    snapshot_id UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    balance_amount DECIMAL(19, 4) NOT NULL,
    balance_currency VARCHAR(3) NOT NULL,
    snapshot_time TIMESTAMP NOT NULL,
    last_entry_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_snapshot_account FOREIGN KEY (account_id) REFERENCES accounts(account_id)
);

-- Unique constraint to prevent duplicate snapshots for the same account at the same time
CREATE UNIQUE INDEX idx_snapshots_account_time ON balance_snapshots(account_id, snapshot_time DESC);

-- Index for finding the latest snapshot for an account
CREATE INDEX idx_snapshots_account_id ON balance_snapshots(account_id);

-- Index for time-based queries
CREATE INDEX idx_snapshots_time ON balance_snapshots(snapshot_time DESC);

-- Index for finding snapshots before a specific time
CREATE INDEX idx_snapshots_account_time_desc ON balance_snapshots(account_id, snapshot_time DESC);

-- Comments for documentation
COMMENT ON TABLE balance_snapshots IS 'Pre-calculated account balances at specific points in time for performance optimization';
COMMENT ON COLUMN balance_snapshots.snapshot_id IS 'Unique identifier for the snapshot';
COMMENT ON COLUMN balance_snapshots.account_id IS 'Account this snapshot belongs to';
COMMENT ON COLUMN balance_snapshots.balance_amount IS 'Calculated balance at snapshot_time';
COMMENT ON COLUMN balance_snapshots.balance_currency IS 'ISO 4217 currency code (should match account currency)';
COMMENT ON COLUMN balance_snapshots.snapshot_time IS 'Point in time this snapshot represents';
COMMENT ON COLUMN balance_snapshots.last_entry_id IS 'Last ledger entry included in this snapshot (optional, for auditing)';
COMMENT ON COLUMN balance_snapshots.created_at IS 'When this snapshot was created';

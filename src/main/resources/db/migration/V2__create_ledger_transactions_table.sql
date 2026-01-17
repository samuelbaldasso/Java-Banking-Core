-- Create ledger_transactions table
CREATE TABLE ledger_transactions (
    transaction_id UUID PRIMARY KEY,
    external_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    reversal_transaction_id UUID,
    CONSTRAINT check_transaction_status CHECK (status IN ('PENDING', 'POSTED', 'REVERSED', 'FAILED')),
    CONSTRAINT fk_reversal_transaction FOREIGN KEY (reversal_transaction_id) REFERENCES ledger_transactions(transaction_id)
);

-- Unique index for idempotency
CREATE UNIQUE INDEX idx_transactions_external_id ON ledger_transactions(external_id);

-- Indexes for common queries
CREATE INDEX idx_transactions_status ON ledger_transactions(status);
CREATE INDEX idx_transactions_created_at ON ledger_transactions(created_at DESC);
CREATE INDEX idx_transactions_event_type ON ledger_transactions(event_type);

-- Comments for documentation
COMMENT ON TABLE ledger_transactions IS 'Financial transactions in the ledger (container for entries)';
COMMENT ON COLUMN ledger_transactions.transaction_id IS 'Unique identifier for the transaction';
COMMENT ON COLUMN ledger_transactions.external_id IS 'External/client-provided ID for idempotency';
COMMENT ON COLUMN ledger_transactions.event_type IS 'Type of business event (TRANSFER, PIX, FEE, etc)';
COMMENT ON COLUMN ledger_transactions.status IS 'Transaction status (PENDING, POSTED, REVERSED, FAILED)';
COMMENT ON COLUMN ledger_transactions.reversal_transaction_id IS 'ID of reversal transaction if this was reversed';

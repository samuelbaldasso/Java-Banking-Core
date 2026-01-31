-- Create outbox_events table for Transactional Outbox pattern
-- This table stores domain events in the same transaction as business data
-- guaranteeing at-least-once delivery to Kafka

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    retry_count INTEGER NOT NULL DEFAULT 0,
    last_error TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    
    CONSTRAINT chk_status CHECK (status IN ('PENDING', 'PROCESSED', 'FAILED'))
);

-- Index for efficient polling of pending events
CREATE INDEX idx_outbox_status_created 
    ON outbox_events(status, created_at) 
    WHERE status = 'PENDING';

-- Index for querying events by aggregate
CREATE INDEX idx_outbox_aggregate 
    ON outbox_events(aggregate_id);

-- Index for monitoring failed events
CREATE INDEX idx_outbox_failed 
    ON outbox_events(status, retry_count) 
    WHERE status = 'FAILED';

COMMENT ON TABLE outbox_events IS 'Transactional outbox for guaranteed event delivery to Kafka';
COMMENT ON COLUMN outbox_events.aggregate_id IS 'ID of the aggregate that produced the event (e.g., transaction_id)';
COMMENT ON COLUMN outbox_events.event_type IS 'Type of domain event (e.g., TRANSACTION_POSTED, TRANSACTION_REVERSED)';
COMMENT ON COLUMN outbox_events.payload IS 'JSON serialized event data';
COMMENT ON COLUMN outbox_events.processed_at IS 'Timestamp when event was successfully published to Kafka';
COMMENT ON COLUMN outbox_events.retry_count IS 'Number of failed publishing attempts';
COMMENT ON COLUMN outbox_events.status IS 'Event processing status: PENDING, PROCESSED, or FAILED';

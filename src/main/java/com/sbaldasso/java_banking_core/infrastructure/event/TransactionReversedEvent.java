package com.sbaldasso.java_banking_core.infrastructure.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a transaction is reversed.
 */
public class TransactionReversedEvent {
    private UUID transactionId;
    private UUID originalTransactionId;
    private Instant timestamp;

    public TransactionReversedEvent() {
    }

    public TransactionReversedEvent(UUID transactionId, UUID originalTransactionId, Instant timestamp) {
        this.transactionId = transactionId;
        this.originalTransactionId = originalTransactionId;
        this.timestamp = timestamp;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
    }

    public UUID getOriginalTransactionId() {
        return originalTransactionId;
    }

    public void setOriginalTransactionId(UUID originalTransactionId) {
        this.originalTransactionId = originalTransactionId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}

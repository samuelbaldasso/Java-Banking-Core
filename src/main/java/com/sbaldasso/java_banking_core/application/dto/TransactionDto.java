package com.sbaldasso.java_banking_core.application.dto;

import com.sbaldasso.java_banking_core.domain.valueobject.EventType;
import com.sbaldasso.java_banking_core.domain.valueobject.TransactionStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO for Transaction information.
 */
public class TransactionDto {
    private UUID transactionId;
    private UUID externalId;
    private EventType eventType;
    private TransactionStatus status;
    private Instant createdAt;
    private List<LedgerEntryDto> entries;
    private UUID reversalTransactionId;

    public TransactionDto() {
    }

    // Constructor
    public TransactionDto(UUID transactionId, UUID externalId, EventType eventType,
            TransactionStatus status, Instant createdAt,
            List<LedgerEntryDto> entries, UUID reversalTransactionId) {
        this.transactionId = transactionId;
        this.externalId = externalId;
        this.eventType = eventType;
        this.status = status;
        this.createdAt = createdAt;
        this.entries = entries;
        this.reversalTransactionId = reversalTransactionId;
    }

    // Getters and setters
    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
    }

    public UUID getExternalId() {
        return externalId;
    }

    public void setExternalId(UUID externalId) {
        this.externalId = externalId;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<LedgerEntryDto> getEntries() {
        return entries;
    }

    public void setEntries(List<LedgerEntryDto> entries) {
        this.entries = entries;
    }

    public UUID getReversalTransactionId() {
        return reversalTransactionId;
    }

    public void setReversalTransactionId(UUID reversalTransactionId) {
        this.reversalTransactionId = reversalTransactionId;
    }
}

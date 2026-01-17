package com.sbaldasso.java_banking_core.infrastructure.persistence.entity;

import com.sbaldasso.java_banking_core.domain.valueobject.EventType;
import com.sbaldasso.java_banking_core.domain.valueobject.TransactionStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA entity for ledger_transactions table.
 * Maps to the domain LedgerTransaction model.
 */
@Entity
@Table(name = "ledger_transactions")
public class LedgerTransactionJpaEntity {

    @Id
    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "external_id", nullable = false, unique = true)
    private UUID externalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private EventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransactionStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "reversal_transaction_id")
    private UUID reversalTransactionId;

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<LedgerEntryJpaEntity> entries = new ArrayList<>();

    // JPA requires a default constructor
    protected LedgerTransactionJpaEntity() {
    }

    public LedgerTransactionJpaEntity(UUID transactionId, UUID externalId, EventType eventType,
            TransactionStatus status, Instant createdAt,
            UUID reversalTransactionId) {
        this.transactionId = transactionId;
        this.externalId = externalId;
        this.eventType = eventType;
        this.status = status;
        this.createdAt = createdAt;
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

    public UUID getReversalTransactionId() {
        return reversalTransactionId;
    }

    public void setReversalTransactionId(UUID reversalTransactionId) {
        this.reversalTransactionId = reversalTransactionId;
    }

    public List<LedgerEntryJpaEntity> getEntries() {
        return entries;
    }

    public void setEntries(List<LedgerEntryJpaEntity> entries) {
        this.entries = entries;
    }

    public void addEntry(LedgerEntryJpaEntity entry) {
        entries.add(entry);
        entry.setTransaction(this);
    }
}

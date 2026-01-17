package com.sbaldasso.java_banking_core.infrastructure.persistence.entity;

import com.sbaldasso.java_banking_core.domain.valueobject.EntryType;
import com.sbaldasso.java_banking_core.domain.valueobject.EventType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for ledger_entries table.
 * Maps to the domain LedgerEntry model.
 */
@Entity
@Table(name = "ledger_entries")
public class LedgerEntryJpaEntity {

    @Id
    @Column(name = "ledger_entry_id", nullable = false)
    private UUID ledgerEntryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private LedgerTransactionJpaEntity transaction;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 10)
    private EntryType entryType;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private EventType eventType;

    @Column(name = "event_time", nullable = false)
    private Instant eventTime;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    // JPA requires a default constructor
    protected LedgerEntryJpaEntity() {
    }

    public LedgerEntryJpaEntity(UUID ledgerEntryId, UUID accountId, BigDecimal amount,
            String currency, EntryType entryType, EventType eventType,
            Instant eventTime, Instant recordedAt) {
        this.ledgerEntryId = ledgerEntryId;
        this.accountId = accountId;
        this.amount = amount;
        this.currency = currency;
        this.entryType = entryType;
        this.eventType = eventType;
        this.eventTime = eventTime;
        this.recordedAt = recordedAt;
    }

    // Getters and setters
    public UUID getLedgerEntryId() {
        return ledgerEntryId;
    }

    public void setLedgerEntryId(UUID ledgerEntryId) {
        this.ledgerEntryId = ledgerEntryId;
    }

    public LedgerTransactionJpaEntity getTransaction() {
        return transaction;
    }

    public void setTransaction(LedgerTransactionJpaEntity transaction) {
        this.transaction = transaction;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public EntryType getEntryType() {
        return entryType;
    }

    public void setEntryType(EntryType entryType) {
        this.entryType = entryType;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public Instant getEventTime() {
        return eventTime;
    }

    public void setEventTime(Instant eventTime) {
        this.eventTime = eventTime;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }
}

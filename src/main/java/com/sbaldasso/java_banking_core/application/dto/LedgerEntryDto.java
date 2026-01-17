package com.sbaldasso.java_banking_core.application.dto;

import com.sbaldasso.java_banking_core.domain.valueobject.EntryType;
import com.sbaldasso.java_banking_core.domain.valueobject.EventType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO for Ledger Entry information.
 */
public class LedgerEntryDto {
    private UUID ledgerEntryId;
    private UUID transactionId;
    private UUID accountId;
    private BigDecimal amount;
    private String currency;
    private EntryType entryType;
    private EventType eventType;
    private Instant eventTime;
    private Instant recordedAt;

    public LedgerEntryDto() {
    }

    // Constructor
    public LedgerEntryDto(UUID ledgerEntryId, UUID transactionId, UUID accountId,
            BigDecimal amount, String currency, EntryType entryType,
            EventType eventType, Instant eventTime, Instant recordedAt) {
        this.ledgerEntryId = ledgerEntryId;
        this.transactionId = transactionId;
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

    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
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

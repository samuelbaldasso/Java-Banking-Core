package com.sbaldasso.java_banking_core.domain.model;

import com.sbaldasso.java_banking_core.domain.valueobject.EntryType;
import com.sbaldasso.java_banking_core.domain.valueobject.EventType;
import com.sbaldasso.java_banking_core.domain.valueobject.Money;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Ledger Entry entity.
 * Represents a single line in the ledger (one side of double-entry
 * bookkeeping).
 * 
 * Business Rules:
 * - IMMUTABLE after creation (never updated or deleted)
 * - Always part of a LedgerTransaction
 * - Amount must be positive (direction indicated by entryType)
 * - eventTime = when the business event occurred
 * - recordedAt = when the entry was persisted to the ledger
 */
public class LedgerEntry {
    private final UUID ledgerEntryId;
    private final UUID transactionId;
    private final UUID accountId;
    private final Money amount;
    private final EntryType entryType;
    private final EventType eventType;
    private final Instant eventTime;
    private final Instant recordedAt;

    // Package-private constructor for reconstitution
    LedgerEntry(UUID ledgerEntryId, UUID transactionId, UUID accountId,
            Money amount, EntryType entryType, EventType eventType,
            Instant eventTime, Instant recordedAt) {
        this.ledgerEntryId = ledgerEntryId;
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.amount = amount;
        this.entryType = entryType;
        this.eventType = eventType;
        this.eventTime = eventTime;
        this.recordedAt = recordedAt;
    }

    /**
     * Creates a new ledger entry (factory method).
     */
    public static LedgerEntry create(
            UUID transactionId,
            UUID accountId,
            Money amount,
            EntryType entryType,
            EventType eventType,
            Instant eventTime) {

        Objects.requireNonNull(transactionId, "Transaction ID cannot be null");
        Objects.requireNonNull(accountId, "Account ID cannot be null");
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(entryType, "Entry type cannot be null");
        Objects.requireNonNull(eventType, "Event type cannot be null");
        Objects.requireNonNull(eventTime, "Event time cannot be null");

        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Entry amount must be positive: " + amount);
        }

        return new LedgerEntry(
                UUID.randomUUID(),
                transactionId,
                accountId,
                amount,
                entryType,
                eventType,
                eventTime,
                Instant.now());
    }

    /**
     * Creates a reversal entry (mirror of this entry with opposite type).
     * Used when reversing transactions.
     */
    public LedgerEntry createReversal(UUID newTransactionId, Instant reversalTime) {
        return new LedgerEntry(
                UUID.randomUUID(),
                newTransactionId,
                this.accountId,
                this.amount,
                this.entryType.opposite(), // Debit becomes Credit and vice versa
                EventType.REVERSAL,
                reversalTime,
                Instant.now());
    }

    // Getters
    public UUID getLedgerEntryId() {
        return ledgerEntryId;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public Money getAmount() {
        return amount;
    }

    public EntryType getEntryType() {
        return entryType;
    }

    public EventType getEventType() {
        return eventType;
    }

    public Instant getEventTime() {
        return eventTime;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public boolean isDebit() {
        return entryType == EntryType.DEBIT;
    }

    public boolean isCredit() {
        return entryType == EntryType.CREDIT;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        LedgerEntry that = (LedgerEntry) o;
        return ledgerEntryId.equals(that.ledgerEntryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ledgerEntryId);
    }

    @Override
    public String toString() {
        return String.format("LedgerEntry{id=%s, account=%s, %s %s, eventType=%s}",
                ledgerEntryId, accountId, entryType, amount, eventType);
    }
}

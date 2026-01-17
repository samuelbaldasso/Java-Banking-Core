package com.sbaldasso.java_banking_core.domain.model;

import com.sbaldasso.java_banking_core.domain.exception.InvalidTransactionException;
import com.sbaldasso.java_banking_core.domain.valueobject.EventType;
import com.sbaldasso.java_banking_core.domain.valueobject.Money;
import com.sbaldasso.java_banking_core.domain.valueobject.TransactionStatus;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * LedgerTransaction aggregate root.
 * Represents a complete financial transaction in the double-entry ledger.
 * 
 * Business Rules:
 * - Must have at least 2 entries (double-entry bookkeeping)
 * - Sum of debits MUST equal sum of credits
 * - All entries must have the same currency
 * - Atomic: all entries are persisted together or none at all
 * - externalId provides idempotency (same externalId = same transaction)
 * - Only POSTED transactions impact balances
 */
public class LedgerTransaction {
    private final UUID transactionId;
    private final UUID externalId;
    private final EventType eventType;
    private final List<LedgerEntry> entries;
    private TransactionStatus status;
    private final Instant createdAt;
    private UUID reversalTransactionId; // If this transaction was reversed

    // Package-private constructor for reconstitution
    LedgerTransaction(UUID transactionId, UUID externalId, EventType eventType,
            List<LedgerEntry> entries, TransactionStatus status,
            Instant createdAt, UUID reversalTransactionId) {
        this.transactionId = transactionId;
        this.externalId = externalId;
        this.eventType = eventType;
        this.entries = new ArrayList<>(entries);
        this.status = status;
        this.createdAt = createdAt;
        this.reversalTransactionId = reversalTransactionId;
    }

    /**
     * Creates a new transaction in PENDING status (factory method).
     * Validates double-entry rules before creation.
     */
    public static LedgerTransaction create(UUID externalId, EventType eventType,
            List<LedgerEntry> entries) {
        Objects.requireNonNull(externalId, "External ID cannot be null");
        Objects.requireNonNull(eventType, "Event type cannot be null");
        Objects.requireNonNull(entries, "Entries cannot be null");

        UUID transactionId = UUID.randomUUID();

        // Create transaction to run validations
        LedgerTransaction transaction = new LedgerTransaction(
                transactionId,
                externalId,
                eventType,
                entries,
                TransactionStatus.PENDING,
                Instant.now(),
                null);

        transaction.validate();

        return transaction;
    }

    /**
     * Validates the transaction follows double-entry bookkeeping rules.
     * 
     * @throws InvalidTransactionException if validation fails
     */
    private void validate() {
        // Must have at least 2 entries
        if (entries.size() < 2) {
            throw new InvalidTransactionException(
                    String.format("Transaction must have at least 2 entries, has %d", entries.size()));
        }

        // All entries must belong to this transaction
        for (LedgerEntry entry : entries) {
            if (!entry.getTransactionId().equals(this.transactionId)) {
                throw new InvalidTransactionException(
                        "Entry transactionId does not match transaction");
            }
        }

        // Calculate total debits and credits
        Map<String, Money> debitsByCurrency = new HashMap<>();
        Map<String, Money> creditsByCurrency = new HashMap<>();

        for (LedgerEntry entry : entries) {
            String currency = entry.getAmount().getCurrencyCode();

            if (entry.isDebit()) {
                debitsByCurrency.merge(currency, entry.getAmount(), Money::add);
            } else {
                creditsByCurrency.merge(currency, entry.getAmount(), Money::add);
            }
        }

        // All currencies in debits must be in credits
        if (!debitsByCurrency.keySet().equals(creditsByCurrency.keySet())) {
            throw new InvalidTransactionException(
                    "Debits and credits must have the same currencies");
        }

        // For each currency, debits must equal credits
        for (String currency : debitsByCurrency.keySet()) {
            Money totalDebits = debitsByCurrency.get(currency);
            Money totalCredits = creditsByCurrency.get(currency);

            if (!totalDebits.equals(totalCredits)) {
                throw new InvalidTransactionException(
                        String.format("Unbalanced transaction for %s: debits=%s, credits=%s",
                                currency, totalDebits, totalCredits));
            }
        }
    }

    /**
     * Posts the transaction to the ledger.
     * Transitions from PENDING to POSTED.
     */
    public void post() {
        if (status != TransactionStatus.PENDING) {
            throw new InvalidTransactionException(
                    String.format("Cannot post transaction in status %s", status));
        }
        this.status = TransactionStatus.POSTED;
    }

    /**
     * Marks the transaction as failed.
     */
    public void markAsFailed() {
        if (status != TransactionStatus.PENDING) {
            throw new InvalidTransactionException(
                    String.format("Cannot mark transaction as failed in status %s", status));
        }
        this.status = TransactionStatus.FAILED;
    }

    /**
     * Marks the transaction as reversed.
     */
    public void markAsReversed(UUID reversalTransactionId) {
        if (!status.canBeReversed()) {
            throw new InvalidTransactionException(
                    String.format("Cannot reverse transaction in status %s", status));
        }
        this.status = TransactionStatus.REVERSED;
        this.reversalTransactionId = reversalTransactionId;
    }

    /**
     * Creates a reversal transaction (mirror of this transaction).
     * All debits become credits and vice versa.
     */
    public LedgerTransaction createReversal(UUID reversalExternalId) {
        if (!status.canBeReversed()) {
            throw new InvalidTransactionException(
                    String.format("Cannot reverse transaction in status %s", status));
        }

        UUID reversalTxId = UUID.randomUUID();
        Instant reversalTime = Instant.now();

        // Create mirror entries with opposite entry types
        List<LedgerEntry> reversalEntries = entries.stream()
                .map(entry -> entry.createReversal(reversalTxId, reversalTime))
                .collect(Collectors.toList());

        return new LedgerTransaction(
                reversalTxId,
                reversalExternalId,
                EventType.REVERSAL,
                reversalEntries,
                TransactionStatus.PENDING,
                reversalTime,
                null);
    }

    // Getters
    public UUID getTransactionId() {
        return transactionId;
    }

    public UUID getExternalId() {
        return externalId;
    }

    public EventType getEventType() {
        return eventType;
    }

    public List<LedgerEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Optional<UUID> getReversalTransactionId() {
        return Optional.ofNullable(reversalTransactionId);
    }

    public boolean isPosted() {
        return status == TransactionStatus.POSTED;
    }

    public boolean isReversed() {
        return status == TransactionStatus.REVERSED;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        LedgerTransaction that = (LedgerTransaction) o;
        return transactionId.equals(that.transactionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId);
    }

    @Override
    public String toString() {
        return String.format("LedgerTransaction{id=%s, externalId=%s, status=%s, entries=%d}",
                transactionId, externalId, status, entries.size());
    }
}

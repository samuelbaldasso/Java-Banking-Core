package com.sbaldasso.java_banking_core.infrastructure.event;

import com.sbaldasso.java_banking_core.domain.valueobject.EventType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Event published when a transaction is posted to the ledger.
 */
public class TransactionPostedEvent {
    private UUID transactionId;
    private UUID externalId;
    private EventType eventType;
    private List<EntryInfo> entries;
    private Instant timestamp;

    public TransactionPostedEvent() {
    }

    public TransactionPostedEvent(UUID transactionId, UUID externalId, EventType eventType,
            List<EntryInfo> entries, Instant timestamp) {
        this.transactionId = transactionId;
        this.externalId = externalId;
        this.eventType = eventType;
        this.entries = entries;
        this.timestamp = timestamp;
    }

    // Nested class for entry information
    public static class EntryInfo {
        private UUID accountId;
        private String amount;
        private String currency;
        private String entryType;

        public EntryInfo() {
        }

        public EntryInfo(UUID accountId, String amount, String currency, String entryType) {
            this.accountId = accountId;
            this.amount = amount;
            this.currency = currency;
            this.entryType = entryType;
        }

        public UUID getAccountId() {
            return accountId;
        }

        public void setAccountId(UUID accountId) {
            this.accountId = accountId;
        }

        public String getAmount() {
            return amount;
        }

        public void setAmount(String amount) {
            this.amount = amount;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public String getEntryType() {
            return entryType;
        }

        public void setEntryType(String entryType) {
            this.entryType = entryType;
        }
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

    public List<EntryInfo> getEntries() {
        return entries;
    }

    public void setEntries(List<EntryInfo> entries) {
        this.entries = entries;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}

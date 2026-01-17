package com.sbaldasso.java_banking_core.application.command;

import com.sbaldasso.java_banking_core.domain.valueobject.EventType;

import java.util.List;
import java.util.UUID;

/**
 * Command for posting a new transaction to the ledger.
 * Includes externalId for idempotency.
 */
public class PostTransactionCommand {
    private UUID externalId;
    private EventType eventType;
    private List<EntryCommand> entries;

    public PostTransactionCommand() {
    }

    public PostTransactionCommand(UUID externalId, EventType eventType, List<EntryCommand> entries) {
        this.externalId = externalId;
        this.eventType = eventType;
        this.entries = entries;
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

    public List<EntryCommand> getEntries() {
        return entries;
    }

    public void setEntries(List<EntryCommand> entries) {
        this.entries = entries;
    }
}

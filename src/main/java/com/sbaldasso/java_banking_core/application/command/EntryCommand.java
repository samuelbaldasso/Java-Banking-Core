package com.sbaldasso.java_banking_core.application.command;

import com.sbaldasso.java_banking_core.domain.valueobject.EntryType;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Command for creating a ledger entry within a transaction.
 */
public class EntryCommand {
    private UUID accountId;
    private BigDecimal amount;
    private String currency;
    private EntryType entryType;

    public EntryCommand() {
    }

    public EntryCommand(UUID accountId, BigDecimal amount, String currency, EntryType entryType) {
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
}

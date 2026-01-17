package com.sbaldasso.java_banking_core.application.dto;

import com.sbaldasso.java_banking_core.domain.valueobject.AccountStatus;
import com.sbaldasso.java_banking_core.domain.valueobject.AccountType;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for Account information.
 */
public class AccountDto {
    private UUID accountId;
    private AccountType accountType;
    private String currency;
    private AccountStatus status;
    private Instant createdAt;

    public AccountDto() {
    }

    public AccountDto(UUID accountId, AccountType accountType, String currency,
            AccountStatus status, Instant createdAt) {
        this.accountId = accountId;
        this.accountType = accountType;
        this.currency = currency;
        this.status = status;
        this.createdAt = createdAt;
    }

    // Getters and setters
    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

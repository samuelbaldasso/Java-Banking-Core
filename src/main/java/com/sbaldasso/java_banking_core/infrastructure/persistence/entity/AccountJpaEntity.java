package com.sbaldasso.java_banking_core.infrastructure.persistence.entity;

import com.sbaldasso.java_banking_core.domain.valueobject.AccountStatus;
import com.sbaldasso.java_banking_core.domain.valueobject.AccountType;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for accounts table.
 * Maps to the domain Account model.
 */
@Entity
@Table(name = "accounts")
public class AccountJpaEntity {

    @Id
    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 50)
    private AccountType accountType;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AccountStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // JPA requires a default constructor
    protected AccountJpaEntity() {
    }

    public AccountJpaEntity(UUID accountId, AccountType accountType, String currency,
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

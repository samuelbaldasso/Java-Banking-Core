package com.sbaldasso.java_banking_core.infrastructure.persistence.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for balance_snapshots table.
 * Maps to the domain BalanceSnapshot model.
 */
@Entity
@Table(name = "balance_snapshots")
public class BalanceSnapshotJpaEntity {

    @Id
    @Column(name = "snapshot_id", nullable = false)
    private UUID snapshotId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "balance_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceAmount;

    @Column(name = "balance_currency", nullable = false, length = 3)
    private String balanceCurrency;

    @Column(name = "snapshot_time", nullable = false)
    private Instant snapshotTime;

    @Column(name = "last_entry_id")
    private UUID lastEntryId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // JPA requires a default constructor
    protected BalanceSnapshotJpaEntity() {
    }

    public BalanceSnapshotJpaEntity(UUID snapshotId, UUID accountId, BigDecimal balanceAmount,
            String balanceCurrency, Instant snapshotTime, UUID lastEntryId, Instant createdAt) {
        this.snapshotId = snapshotId;
        this.accountId = accountId;
        this.balanceAmount = balanceAmount;
        this.balanceCurrency = balanceCurrency;
        this.snapshotTime = snapshotTime;
        this.lastEntryId = lastEntryId;
        this.createdAt = createdAt;
    }

    // Getters and setters
    public UUID getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(UUID snapshotId) {
        this.snapshotId = snapshotId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public BigDecimal getBalanceAmount() {
        return balanceAmount;
    }

    public void setBalanceAmount(BigDecimal balanceAmount) {
        this.balanceAmount = balanceAmount;
    }

    public String getBalanceCurrency() {
        return balanceCurrency;
    }

    public void setBalanceCurrency(String balanceCurrency) {
        this.balanceCurrency = balanceCurrency;
    }

    public Instant getSnapshotTime() {
        return snapshotTime;
    }

    public void setSnapshotTime(Instant snapshotTime) {
        this.snapshotTime = snapshotTime;
    }

    public UUID getLastEntryId() {
        return lastEntryId;
    }

    public void setLastEntryId(UUID lastEntryId) {
        this.lastEntryId = lastEntryId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

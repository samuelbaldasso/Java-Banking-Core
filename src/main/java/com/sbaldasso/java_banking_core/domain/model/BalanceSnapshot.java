package com.sbaldasso.java_banking_core.domain.model;

import com.sbaldasso.java_banking_core.domain.valueobject.Money;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Balance Snapshot entity.
 * Represents a pre-calculated balance for an account at a specific point in
 * time.
 * 
 * Business Rules:
 * - IMMUTABLE after creation (snapshots are historical records)
 * - Snapshot time cannot be in the future
 * - Balance currency must match account currency
 * - Used to optimize balance calculations for accounts with many transactions
 * 
 * Implementation Pattern:
 * - Event Sourcing Snapshot Pattern
 * - Instead of replaying all events (ledger entries), we can start from a
 * snapshot
 * and only apply events after the snapshot time
 */
public class BalanceSnapshot {
    private final UUID snapshotId;
    private final UUID accountId;
    private final Money balance;
    private final Instant snapshotTime;
    private final UUID lastEntryId; // Optional: for auditing
    private final Instant createdAt;

    // Package-private constructor for reconstitution from repository
    BalanceSnapshot(UUID snapshotId, UUID accountId, Money balance,
            Instant snapshotTime, UUID lastEntryId, Instant createdAt) {
        this.snapshotId = snapshotId;
        this.accountId = accountId;
        this.balance = balance;
        this.snapshotTime = snapshotTime;
        this.lastEntryId = lastEntryId;
        this.createdAt = createdAt;
    }

    /**
     * Creates a new balance snapshot (factory method).
     * 
     * @param accountId    Account this snapshot belongs to
     * @param balance      Calculated balance at snapshot time
     * @param snapshotTime Point in time this snapshot represents
     * @param lastEntryId  Optional: last entry included in calculation
     * @return New BalanceSnapshot instance
     */
    public static BalanceSnapshot create(
            UUID accountId,
            Money balance,
            Instant snapshotTime,
            UUID lastEntryId) {

        Objects.requireNonNull(accountId, "Account ID cannot be null");
        Objects.requireNonNull(balance, "Balance cannot be null");
        Objects.requireNonNull(snapshotTime, "Snapshot time cannot be null");

        // Business rule: snapshot time cannot be in the future
        if (snapshotTime.isAfter(Instant.now())) {
            throw new IllegalArgumentException(
                    "Snapshot time cannot be in the future: " + snapshotTime);
        }

        return new BalanceSnapshot(
                UUID.randomUUID(),
                accountId,
                balance,
                snapshotTime,
                lastEntryId,
                Instant.now());
    }

    /**
     * Checks if this snapshot can be used for calculating balance at a given time.
     * A snapshot is valid if its snapshot_time is before or equal to the query
     * time.
     */
    public boolean isValidForTime(Instant queryTime) {
        return !snapshotTime.isAfter(queryTime);
    }

    /**
     * Checks if this snapshot is more recent than another snapshot.
     */
    public boolean isNewerThan(BalanceSnapshot other) {
        return this.snapshotTime.isAfter(other.snapshotTime);
    }

    // Getters
    public UUID getSnapshotId() {
        return snapshotId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public Money getBalance() {
        return balance;
    }

    public Instant getSnapshotTime() {
        return snapshotTime;
    }

    public UUID getLastEntryId() {
        return lastEntryId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        BalanceSnapshot that = (BalanceSnapshot) o;
        return snapshotId.equals(that.snapshotId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(snapshotId);
    }

    @Override
    public String toString() {
        return String.format("BalanceSnapshot{id=%s, account=%s, balance=%s, time=%s}",
                snapshotId, accountId, balance, snapshotTime);
    }
}

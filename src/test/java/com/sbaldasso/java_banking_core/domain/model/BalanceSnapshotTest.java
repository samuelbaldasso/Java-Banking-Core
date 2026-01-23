package com.sbaldasso.java_banking_core.domain.model;

import com.sbaldasso.java_banking_core.domain.valueobject.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BalanceSnapshot domain model.
 */
class BalanceSnapshotTest {

    @Test
    void shouldCreateValidSnapshot() {
        // Given
        UUID accountId = UUID.randomUUID();
        Money balance = Money.of(new BigDecimal("1000.00"), "BRL");
        Instant snapshotTime = Instant.now().minusSeconds(3600); // 1 hour ago
        UUID lastEntryId = UUID.randomUUID();

        // When
        BalanceSnapshot snapshot = BalanceSnapshot.create(accountId, balance, snapshotTime, lastEntryId);

        // Then
        assertNotNull(snapshot);
        assertNotNull(snapshot.getSnapshotId());
        assertEquals(accountId, snapshot.getAccountId());
        assertEquals(balance, snapshot.getBalance());
        assertEquals(snapshotTime, snapshot.getSnapshotTime());
        assertEquals(lastEntryId, snapshot.getLastEntryId());
        assertNotNull(snapshot.getCreatedAt());
    }

    @Test
    void shouldRejectFutureSnapshotTime() {
        // Given
        UUID accountId = UUID.randomUUID();
        Money balance = Money.of(new BigDecimal("1000.00"), "BRL");
        Instant futureTime = Instant.now().plusSeconds(3600); // 1 hour in future

        // When/Then
        assertThrows(IllegalArgumentException.class, () -> {
            BalanceSnapshot.create(accountId, balance, futureTime, null);
        });
    }

    @Test
    void shouldRejectNullAccountId() {
        // Given
        Money balance = Money.of(new BigDecimal("1000.00"), "BRL");
        Instant snapshotTime = Instant.now().minusSeconds(3600);

        // When/Then
        assertThrows(NullPointerException.class, () -> {
            BalanceSnapshot.create(null, balance, snapshotTime, null);
        });
    }

    @Test
    void shouldRejectNullBalance() {
        // Given
        UUID accountId = UUID.randomUUID();
        Instant snapshotTime = Instant.now().minusSeconds(3600);

        // When/Then
        assertThrows(NullPointerException.class, () -> {
            BalanceSnapshot.create(accountId, null, snapshotTime, null);
        });
    }

    @Test
    void shouldRejectNullSnapshotTime() {
        // Given
        UUID accountId = UUID.randomUUID();
        Money balance = Money.of(new BigDecimal("1000.00"), "BRL");

        // When/Then
        assertThrows(NullPointerException.class, () -> {
            BalanceSnapshot.create(accountId, balance, null, null);
        });
    }

    @Test
    void shouldAllowNullLastEntryId() {
        // Given: account with no entries
        UUID accountId = UUID.randomUUID();
        Money balance = Money.zero("BRL");
        Instant snapshotTime = Instant.now().minusSeconds(3600);

        // When
        BalanceSnapshot snapshot = BalanceSnapshot.create(accountId, balance, snapshotTime, null);

        // Then
        assertNotNull(snapshot);
        assertNull(snapshot.getLastEntryId());
    }

    @Test
    void shouldValidateSnapshotForTime() {
        // Given
        UUID accountId = UUID.randomUUID();
        Money balance = Money.of(new BigDecimal("1000.00"), "BRL");
        Instant snapshotTime = Instant.now().minusSeconds(7200); // 2 hours ago
        BalanceSnapshot snapshot = BalanceSnapshot.create(accountId, balance, snapshotTime, null);

        // When/Then: snapshot is valid for times after snapshot time
        assertTrue(snapshot.isValidForTime(Instant.now()));
        assertTrue(snapshot.isValidForTime(snapshotTime));
        assertTrue(snapshot.isValidForTime(snapshotTime.plusSeconds(1)));

        // But not valid for times before snapshot time
        assertFalse(snapshot.isValidForTime(snapshotTime.minusSeconds(1)));
    }

    @Test
    void shouldCompareSnapshotsByTime() {
        // Given: two snapshots at different times
        UUID accountId = UUID.randomUUID();
        Money balance = Money.of(new BigDecimal("1000.00"), "BRL");

        Instant olderTime = Instant.now().minusSeconds(7200); // 2 hours ago
        Instant newerTime = Instant.now().minusSeconds(3600); // 1 hour ago

        BalanceSnapshot olderSnapshot = BalanceSnapshot.create(accountId, balance, olderTime, null);
        BalanceSnapshot newerSnapshot = BalanceSnapshot.create(accountId, balance, newerTime, null);

        // When/Then
        assertTrue(newerSnapshot.isNewerThan(olderSnapshot));
        assertFalse(olderSnapshot.isNewerThan(newerSnapshot));
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        // Given: two snapshots with same ID
        UUID snapshotId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        Money balance = Money.of(new BigDecimal("1000.00"), "BRL");
        Instant snapshotTime = Instant.now().minusSeconds(3600);
        Instant createdAt = Instant.now();

        BalanceSnapshot snapshot1 = new BalanceSnapshot(
                snapshotId, accountId, balance, snapshotTime, null, createdAt);
        BalanceSnapshot snapshot2 = new BalanceSnapshot(
                snapshotId, accountId, balance, snapshotTime, null, createdAt);

        // Then
        assertEquals(snapshot1, snapshot2);
        assertEquals(snapshot1.hashCode(), snapshot2.hashCode());
    }
}

package com.sbaldasso.java_banking_core.domain.service;

import com.sbaldasso.java_banking_core.domain.model.Account;
import com.sbaldasso.java_banking_core.domain.model.LedgerEntry;
import com.sbaldasso.java_banking_core.domain.valueobject.AccountType;
import com.sbaldasso.java_banking_core.domain.valueobject.EntryType;
import com.sbaldasso.java_banking_core.domain.valueobject.EventType;
import com.sbaldasso.java_banking_core.domain.valueobject.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BalanceCalculator domain service.
 * Verifies balance calculation logic for different account types.
 */
class BalanceCalculatorTest {

    private BalanceCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new BalanceCalculator();
    }

    @Test
    void shouldCalculateAssetAccountBalance() {
        // Given: ASSET account (debits increase, credits decrease)
        Account account = Account.create(AccountType.ASSET, "BRL");

        List<LedgerEntry> entries = List.of(
                createEntry(EntryType.DEBIT, "100"),
                createEntry(EntryType.CREDIT, "30"),
                createEntry(EntryType.DEBIT, "50"));

        // When: calculate balance
        Money balance = calculator.calculateBalance(account, entries);

        // Then: balance = 100 - 30 + 50 = 120
        assertEquals(new BigDecimal("120.00"), balance.getAmount());
    }

    @Test
    void shouldCalculateLiabilityAccountBalance() {
        // Given: LIABILITY account (credits increase, debits decrease)
        Account account = Account.create(AccountType.LIABILITY, "BRL");

        List<LedgerEntry> entries = List.of(
                createEntry(EntryType.CREDIT, "100"),
                createEntry(EntryType.DEBIT, "30"),
                createEntry(EntryType.CREDIT, "50"));

        // When: calculate balance
        Money balance = calculator.calculateBalance(account, entries);

        // Then: balance = 100 - 30 + 50 = 120
        assertEquals(new BigDecimal("120.00"), balance.getAmount());
    }

    @Test
    void shouldReturnZeroForNoEntries() {
        // Given: account with no entries
        Account account = Account.create(AccountType.ASSET, "BRL");

        // When: calculate balance
        Money balance = calculator.calculateBalance(account, List.of());

        // Then: balance = 0
        assertTrue(balance.isZero());
    }

    @Test
    void shouldCalculateHistoricalBalance() {
        // Given: entries at different times
        Account account = Account.create(AccountType.ASSET, "BRL");
        Instant now = Instant.now();
        Instant yesterday = now.minusSeconds(86400);
        Instant tomorrow = now.plusSeconds(86400);

        List<LedgerEntry> entries = List.of(
                createEntryAt(EntryType.DEBIT, "100", yesterday),
                createEntryAt(EntryType.CREDIT, "30", now),
                createEntryAt(EntryType.DEBIT, "50", tomorrow));

        // When: calculate balance as of 'now'
        Money balance = calculator.calculateBalanceAsOf(account, entries, now);

        // Then: only includes entries up to 'now': 100 - 30 = 70
        assertEquals(new BigDecimal("70.00"), balance.getAmount());
    }

    private LedgerEntry createEntry(EntryType entryType, String amount) {
        return createEntryAt(entryType, amount, Instant.now());
    }

    private LedgerEntry createEntryAt(EntryType entryType, String amount, Instant eventTime) {
        return LedgerEntry.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                Money.of(new BigDecimal(amount), "BRL"),
                entryType,
                EventType.TRANSFER,
                eventTime);
    }
}

package com.sbaldasso.java_banking_core.domain.model;

import com.sbaldasso.java_banking_core.domain.exception.InvalidTransactionException;
import com.sbaldasso.java_banking_core.domain.valueobject.EntryType;
import com.sbaldasso.java_banking_core.domain.valueobject.EventType;
import com.sbaldasso.java_banking_core.domain.valueobject.Money;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for LedgerTransaction domain model.
 * Verifies double-entry bookkeeping rules.
 */
class LedgerTransactionTest {

    @Test
    void shouldCreateBalancedTransaction() {
        // Given: balanced entries
        UUID transactionId = UUID.randomUUID();
        List<LedgerEntry> entries = List.of(
                createEntry(transactionId, EntryType.DEBIT, "100", "BRL"),
                createEntry(transactionId, EntryType.CREDIT, "100", "BRL"));

        // When: create transaction
        LedgerTransaction transaction = LedgerTransaction.create(
                UUID.randomUUID(),
                EventType.TRANSFER,
                entries);

        // Then: transaction is created successfully
        assertNotNull(transaction);
        assertEquals(2, transaction.getEntries().size());
    }

    @Test
    void shouldRejectUnbalancedTransaction() {
        // Given: unbalanced entries (debits != credits)
        UUID transactionId = UUID.randomUUID();
        List<LedgerEntry> entries = List.of(
                createEntry(transactionId, EntryType.DEBIT, "100", "BRL"),
                createEntry(transactionId, EntryType.CREDIT, "50", "BRL"));

        // When/Then: should throw exception
        InvalidTransactionException ex = assertThrows(
                InvalidTransactionException.class,
                () -> LedgerTransaction.create(UUID.randomUUID(), EventType.TRANSFER, entries));

        assertTrue(ex.getMessage().contains("Unbalanced"));
    }

    @Test
    void shouldRejectTransactionWithLessThanTwoEntries() {
        // Given: only one entry
        UUID transactionId = UUID.randomUUID();
        List<LedgerEntry> entries = List.of(
                createEntry(transactionId, EntryType.DEBIT, "100", "BRL"));

        // When/Then: should throw exception
        InvalidTransactionException ex = assertThrows(
                InvalidTransactionException.class,
                () -> LedgerTransaction.create(UUID.randomUUID(), EventType.TRANSFER, entries));

        assertTrue(ex.getMessage().contains("at least 2 entries"));
    }

    @Test
    void shouldRejectMixedCurrencies() {
        // Given: entries with different currencies
        UUID transactionId = UUID.randomUUID();
        List<LedgerEntry> entries = List.of(
                createEntry(transactionId, EntryType.DEBIT, "100", "BRL"),
                createEntry(transactionId, EntryType.CREDIT, "100", "USD"));

        // When/Then: should throw exception
        InvalidTransactionException ex = assertThrows(
                InvalidTransactionException.class,
                () -> LedgerTransaction.create(UUID.randomUUID(), EventType.TRANSFER, entries));

        assertTrue(ex.getMessage().contains("same currencies") || ex.getMessage().contains("Currency mismatch"));
    }

    @Test
    void shouldCreateReversalTransaction() {
        // Given: a posted transaction
        UUID transactionId = UUID.randomUUID();
        List<LedgerEntry> entries = List.of(
                createEntry(transactionId, EntryType.DEBIT, "100", "BRL"),
                createEntry(transactionId, EntryType.CREDIT, "100", "BRL"));

        LedgerTransaction original = LedgerTransaction.create(
                UUID.randomUUID(),
                EventType.TRANSFER,
                entries);
        original.post();

        // When: create reversal
        LedgerTransaction reversal = original.createReversal(UUID.randomUUID());

        // Then: reversal has opposite entry types
        assertNotNull(reversal);
        assertEquals(2, reversal.getEntries().size());

        // Original debits should become credits in reversal
        LedgerEntry originalDebit = original.getEntries().get(0);
        LedgerEntry reversalForDebit = reversal.getEntries().stream()
                .filter(e -> e.getAccountId().equals(originalDebit.getAccountId()))
                .findFirst()
                .orElseThrow();

        assertEquals(EntryType.CREDIT, reversalForDebit.getEntryType());
    }

    private LedgerEntry createEntry(UUID transactionId, EntryType entryType,
            String amount, String currency) {
        return LedgerEntry.create(
                transactionId,
                UUID.randomUUID(),
                Money.of(new java.math.BigDecimal(amount), currency),
                entryType,
                EventType.TRANSFER,
                Instant.now());
    }
}

package com.sbaldasso.java_banking_core.domain.service;

import com.sbaldasso.java_banking_core.domain.exception.InvalidTransactionException;
import com.sbaldasso.java_banking_core.domain.model.LedgerEntry;
import com.sbaldasso.java_banking_core.domain.valueobject.Money;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Domain service for validating double-entry bookkeeping rules.
 * 
 * Core Rule: Sum of Debits = Sum of Credits (for each currency)
 */
@Service
public class DoubleEntryValidator {

    /**
     * Validates that the entries follow double-entry rules.
     * 
     * @param entries List of ledger entries to validate
     * @throws InvalidTransactionException if validation fails
     */
    public void validate(List<LedgerEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            throw new InvalidTransactionException("Entries cannot be null or empty");
        }

        if (entries.size() < 2) {
            throw new InvalidTransactionException(
                    String.format("Double-entry requires at least 2 entries, found %d", entries.size()));
        }

        validateBalance(entries);
    }

    /**
     * Validates that debits equal credits for each currency.
     */
    private void validateBalance(List<LedgerEntry> entries) {
        Map<String, Money> debitsByCurrency = new HashMap<>();
        Map<String, Money> creditsByCurrency = new HashMap<>();

        // Accumulate debits and credits by currency
        for (LedgerEntry entry : entries) {
            String currency = entry.getAmount().getCurrencyCode();
            Money amount = entry.getAmount();

            if (entry.isDebit()) {
                debitsByCurrency.merge(currency, amount, Money::add);
            } else {
                creditsByCurrency.merge(currency, amount, Money::add);
            }
        }

        // Validate all currencies match
        if (!debitsByCurrency.keySet().equals(creditsByCurrency.keySet())) {
            throw new InvalidTransactionException(
                    String.format("Currency mismatch between debits %s and credits %s",
                            debitsByCurrency.keySet(), creditsByCurrency.keySet()));
        }

        // Validate debits = credits for each currency
        for (String currency : debitsByCurrency.keySet()) {
            Money totalDebits = debitsByCurrency.get(currency);
            Money totalCredits = creditsByCurrency.get(currency);

            if (!totalDebits.equals(totalCredits)) {
                throw new InvalidTransactionException(
                        String.format("Unbalanced entries for currency %s: debits=%s, credits=%s",
                                currency, totalDebits, totalCredits));
            }
        }
    }

    /**
     * Checks if entries are balanced (returns true/false without throwing).
     */
    public boolean isBalanced(List<LedgerEntry> entries) {
        try {
            validate(entries);
            return true;
        } catch (InvalidTransactionException e) {
            return false;
        }
    }
}

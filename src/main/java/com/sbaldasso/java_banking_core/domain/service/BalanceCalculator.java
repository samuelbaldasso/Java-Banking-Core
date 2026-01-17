package com.sbaldasso.java_banking_core.domain.service;

import com.sbaldasso.java_banking_core.domain.model.Account;
import com.sbaldasso.java_banking_core.domain.model.LedgerEntry;
import com.sbaldasso.java_banking_core.domain.valueobject.AccountType;
import com.sbaldasso.java_banking_core.domain.valueobject.Money;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Domain service for calculating account balances from ledger entries.
 * 
 * Core Principle: Balance is DERIVED from ledger entries, never stored as
 * source of truth.
 * 
 * Balance Calculation Rules:
 * - ASSET accounts: debits increase, credits decrease
 * - LIABILITY accounts: credits increase, debits decrease
 * - EQUITY accounts: credits increase, debits decrease
 * - REVENUE accounts: credits increase, debits decrease
 * - EXPENSE accounts: debits increase, credits decrease
 * 
 * Only POSTED transactions impact balance.
 */
@Service
public class BalanceCalculator {

    /**
     * Calculates the current balance for an account from its ledger entries.
     * Only includes POSTED transactions.
     * 
     * @param account The account to calculate balance for
     * @param entries All ledger entries for this account (filtered to POSTED only)
     * @return Current balance as Money
     */
    public Money calculateBalance(Account account, List<LedgerEntry> entries) {
        return calculateBalanceAsOf(account, entries, null);
    }

    /**
     * Calculates the balance for an account as of a specific point in time.
     * Only includes POSTED transactions with eventTime <= asOfTime.
     * 
     * @param account  The account to calculate balance for
     * @param entries  All ledger entries for this account (filtered to POSTED only)
     * @param asOfTime Point in time to calculate balance (null = current)
     * @return Balance as of the specified time
     */
    public Money calculateBalanceAsOf(Account account, List<LedgerEntry> entries, Instant asOfTime) {
        Money balance = Money.zero(account.getCurrencyCode());
        AccountType accountType = account.getAccountType();

        for (LedgerEntry entry : entries) {
            // Skip entries after the cutoff time (if specified)
            if (asOfTime != null && entry.getEventTime().isAfter(asOfTime)) {
                continue;
            }

            // Apply entry to balance based on account type and entry type
            balance = applyEntryToBalance(balance, entry, accountType);
        }

        return balance;
    }

    /**
     * Applies a single entry to the balance based on account type and entry type.
     */
    private Money applyEntryToBalance(Money currentBalance, LedgerEntry entry, AccountType accountType) {
        boolean isIncrease = shouldIncreaseBalance(accountType, entry.isDebit());

        if (isIncrease) {
            return currentBalance.add(entry.getAmount());
        } else {
            return currentBalance.subtract(entry.getAmount());
        }
    }

    /**
     * Determines if an entry should increase the balance based on account type and
     * entry type.
     * 
     * Rules:
     * - ASSET & EXPENSE: debits increase, credits decrease
     * - LIABILITY, EQUITY & REVENUE: credits increase, debits decrease
     */
    private boolean shouldIncreaseBalance(AccountType accountType, boolean isDebit) {
        return switch (accountType) {
            case ASSET, EXPENSE -> isDebit;
            case LIABILITY, EQUITY, REVENUE -> !isDebit;
        };
    }
}

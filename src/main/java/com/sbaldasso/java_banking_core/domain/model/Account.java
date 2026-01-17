package com.sbaldasso.java_banking_core.domain.model;

import com.sbaldasso.java_banking_core.domain.exception.InvalidAccountException;
import com.sbaldasso.java_banking_core.domain.valueobject.AccountStatus;
import com.sbaldasso.java_banking_core.domain.valueobject.AccountType;

import java.time.Instant;
import java.util.Currency;
import java.util.Objects;
import java.util.UUID;

/**
 * Account aggregate root.
 * Represents a financial account in the ledger system.
 * 
 * Business Rules:
 * - One account, one currency (multi-currency requires multiple accounts)
 * - Accounts are never deleted, only closed
 * - Only ACTIVE accounts can accept new transactions
 */
public class Account {
    private final UUID accountId;
    private final AccountType accountType;
    private final Currency currency;
    private AccountStatus status;
    private final Instant createdAt;

    // Package-private constructor for reconstitution from repository
    Account(UUID accountId, AccountType accountType, Currency currency,
            AccountStatus status, Instant createdAt) {
        this.accountId = accountId;
        this.accountType = accountType;
        this.currency = currency;
        this.status = status;
        this.createdAt = createdAt;
    }

    /**
     * Creates a new account (factory method).
     */
    public static Account create(AccountType accountType, String currencyCode) {
        Objects.requireNonNull(accountType, "Account type cannot be null");
        Objects.requireNonNull(currencyCode, "Currency code cannot be null");

        return new Account(
                UUID.randomUUID(),
                accountType,
                Currency.getInstance(currencyCode),
                AccountStatus.ACTIVE,
                Instant.now());
    }

    /**
     * Validates if this account can accept a transaction.
     * 
     * @throws InvalidAccountException if account cannot accept transactions
     */
    public void validateCanAcceptTransaction() {
        if (!status.canAcceptTransactions()) {
            throw new InvalidAccountException(
                    String.format("Account %s is %s and cannot accept transactions",
                            accountId, status));
        }
    }

    /**
     * Validates if the given currency matches this account's currency.
     * 
     * @throws InvalidAccountException if currencies don't match
     */
    public void validateCurrency(String currencyCode) {
        if (!this.currency.getCurrencyCode().equals(currencyCode)) {
            throw new InvalidAccountException(
                    String.format("Currency mismatch: account currency is %s, transaction currency is %s",
                            this.currency.getCurrencyCode(), currencyCode));
        }
    }

    /**
     * Blocks this account, preventing new transactions.
     */
    public void block() {
        if (status == AccountStatus.CLOSED) {
            throw new InvalidAccountException("Cannot block a closed account");
        }
        this.status = AccountStatus.BLOCKED;
    }

    /**
     * Unblocks this account, allowing new transactions.
     */
    public void unblock() {
        if (status == AccountStatus.CLOSED) {
            throw new InvalidAccountException("Cannot unblock a closed account");
        }
        this.status = AccountStatus.ACTIVE;
    }

    /**
     * Closes this account permanently.
     * Note: In a real system, you'd typically check if balance is zero before
     * closing.
     */
    public void close() {
        this.status = AccountStatus.CLOSED;
    }

    // Getters
    public UUID getAccountId() {
        return accountId;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public Currency getCurrency() {
        return currency;
    }

    public String getCurrencyCode() {
        return currency.getCurrencyCode();
    }

    public AccountStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Account account = (Account) o;
        return accountId.equals(account.accountId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId);
    }

    @Override
    public String toString() {
        return String.format("Account{id=%s, type=%s, currency=%s, status=%s}",
                accountId, accountType, currency.getCurrencyCode(), status);
    }
}

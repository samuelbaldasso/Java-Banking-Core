package com.sbaldasso.java_banking_core.domain.valueobject;

/**
 * Status of an account in its lifecycle.
 */
public enum AccountStatus {
    /**
     * Account is active and can accept transactions
     */
    ACTIVE,

    /**
     * Account is temporarily blocked.
     * Cannot accept new transactions but maintains balance and history.
     */
    BLOCKED,

    /**
     * Account is permanently closed.
     * Cannot accept new transactions.
     * Balance should be zero before closing.
     */
    CLOSED;

    /**
     * Checks if the account can accept new transactions.
     */
    public boolean canAcceptTransactions() {
        return this == ACTIVE;
    }
}

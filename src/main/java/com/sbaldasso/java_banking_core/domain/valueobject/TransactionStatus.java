package com.sbaldasso.java_banking_core.domain.valueobject;

/**
 * Status of a ledger transaction in its lifecycle.
 */
public enum TransactionStatus {
    /**
     * Transaction created but not yet posted to the ledger.
     * Does not impact account balances.
     */
    PENDING,

    /**
     * Transaction successfully posted to the ledger.
     * Impacts account balances.
     * This is the only status that affects balance calculations.
     */
    POSTED,

    /**
     * Transaction has been reversed by a subsequent reversal transaction.
     * The original entries remain, but a mirror transaction was created.
     */
    REVERSED,

    /**
     * Transaction failed validation or processing.
     * Does not impact account balances.
     */
    FAILED;

    /**
     * Checks if this status impacts account balances.
     * Only POSTED transactions should be included in balance calculations.
     */
    public boolean impactsBalance() {
        return this == POSTED;
    }

    /**
     * Checks if the transaction can be reversed.
     * Only POSTED transactions can be reversed.
     */
    public boolean canBeReversed() {
        return this == POSTED;
    }
}

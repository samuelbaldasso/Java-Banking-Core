package com.sbaldasso.java_banking_core.domain.valueobject;

/**
 * Type of ledger entry in double-entry bookkeeping.
 */
public enum EntryType {
    /**
     * Debit entry - increases assets and expenses, decreases liabilities, equity,
     * and revenue
     */
    DEBIT,

    /**
     * Credit entry - decreases assets and expenses, increases liabilities, equity,
     * and revenue
     */
    CREDIT;

    /**
     * Returns the opposite entry type.
     * Used for reversals where debits become credits and vice versa.
     */
    public EntryType opposite() {
        return this == DEBIT ? CREDIT : DEBIT;
    }
}

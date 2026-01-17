package com.sbaldasso.java_banking_core.domain.valueobject;

/**
 * Type of financial event that generates ledger entries.
 * Represents the business operation that triggered the transaction.
 */
public enum EventType {
    /**
     * Internal transfer between accounts
     */
    TRANSFER,

    /**
     * PIX payment (Brazilian instant payment)
     */
    PIX,

    /**
     * TED transfer (Brazilian electronic transfer)
     */
    TED,

    /**
     * DOC transfer (Brazilian document of credit)
     */
    DOC,

    /**
     * Fee charged
     */
    FEE,

    /**
     * Interest credit or debit
     */
    INTEREST,

    /**
     * Reversal/chargeback of a previous transaction
     */
    REVERSAL,

    /**
     * Initial deposit or account funding
     */
    DEPOSIT,

    /**
     * Withdrawal from account
     */
    WITHDRAWAL,

    /**
     * Payment processing
     */
    PAYMENT,

    /**
     * Refund of a previous payment
     */
    REFUND,

    /**
     * Adjustment entry (manual correction)
     */
    ADJUSTMENT;
}

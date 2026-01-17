package com.sbaldasso.java_banking_core.domain.service;

import com.sbaldasso.java_banking_core.domain.exception.InvalidTransactionException;
import com.sbaldasso.java_banking_core.domain.model.LedgerTransaction;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Domain service for processing transaction state transitions.
 * Encapsulates transaction lifecycle business logic.
 */
@Service
public class TransactionProcessor {

    private final DoubleEntryValidator validator;

    public TransactionProcessor(DoubleEntryValidator validator) {
        this.validator = validator;
    }

    /**
     * Posts a transaction to the ledger.
     * Validates double-entry rules before posting.
     * 
     * @param transaction The transaction to post
     * @throws InvalidTransactionException if validation fails or transaction cannot
     *                                     be posted
     */
    public void postTransaction(LedgerTransaction transaction) {
        // Validate double-entry rules
        validator.validate(transaction.getEntries());

        // Post the transaction (state transition)
        transaction.post();
    }

    /**
     * Creates and validates a reversal transaction.
     * 
     * @param originalTransaction The transaction to reverse
     * @param reversalExternalId  External ID for the reversal (for idempotency)
     * @return The reversal transaction (in PENDING status)
     * @throws InvalidTransactionException if transaction cannot be reversed
     */
    public LedgerTransaction createReversal(LedgerTransaction originalTransaction,
            UUID reversalExternalId) {
        // Create reversal transaction
        LedgerTransaction reversalTransaction = originalTransaction.createReversal(reversalExternalId);

        // Validate the reversal
        validator.validate(reversalTransaction.getEntries());

        return reversalTransaction;
    }

    /**
     * Marks an original transaction as reversed and posts the reversal.
     * 
     * @param originalTransaction The original transaction to mark as reversed
     * @param reversalTransaction The reversal transaction to post
     */
    public void executeReversal(LedgerTransaction originalTransaction,
            LedgerTransaction reversalTransaction) {
        // Post the reversal transaction
        postTransaction(reversalTransaction);

        // Mark original as reversed
        originalTransaction.markAsReversed(reversalTransaction.getTransactionId());
    }
}

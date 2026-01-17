package com.sbaldasso.java_banking_core.domain.exception;

/**
 * Thrown when transaction operations violate business rules.
 */
public class InvalidTransactionException extends DomainException {

    public InvalidTransactionException(String message) {
        super(message);
    }
}

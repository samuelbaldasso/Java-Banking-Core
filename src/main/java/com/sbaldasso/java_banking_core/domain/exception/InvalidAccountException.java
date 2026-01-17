package com.sbaldasso.java_banking_core.domain.exception;

/**
 * Thrown when account operations violate business rules.
 */
public class InvalidAccountException extends DomainException {

    public InvalidAccountException(String message) {
        super(message);
    }
}

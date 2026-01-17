package com.sbaldasso.java_banking_core.domain.exception;

/**
 * Base exception for all domain-level business rule violations.
 */
public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }

    public DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}

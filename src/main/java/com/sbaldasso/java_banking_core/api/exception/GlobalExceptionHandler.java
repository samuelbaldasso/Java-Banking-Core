package com.sbaldasso.java_banking_core.api.exception;

import com.sbaldasso.java_banking_core.domain.exception.DomainException;
import com.sbaldasso.java_banking_core.domain.exception.InvalidAccountException;
import com.sbaldasso.java_banking_core.domain.exception.InvalidTransactionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

/**
 * Global exception handler for REST API.
 * Returns RFC 7807 Problem Details for errors.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles invalid account exceptions (404 or 400).
     */
    @ExceptionHandler(InvalidAccountException.class)
    public ProblemDetail handleInvalidAccountException(InvalidAccountException ex) {
        // If message contains "not found", return 404, otherwise 400
        HttpStatus status = ex.getMessage().toLowerCase().contains("not found")
                ? HttpStatus.NOT_FOUND
                : HttpStatus.BAD_REQUEST;

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problemDetail.setTitle("Invalid Account");
        problemDetail.setType(URI.create("https://banking-ledger.com/errors/invalid-account"));
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }

    /**
     * Handles invalid transaction exceptions (400 or 404).
     */
    @ExceptionHandler(InvalidTransactionException.class)
    public ProblemDetail handleInvalidTransactionException(InvalidTransactionException ex) {
        HttpStatus status = ex.getMessage().toLowerCase().contains("not found")
                ? HttpStatus.NOT_FOUND
                : HttpStatus.BAD_REQUEST;

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problemDetail.setTitle("Invalid Transaction");
        problemDetail.setType(URI.create("https://banking-ledger.com/errors/invalid-transaction"));
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }

    /**
     * Handles general domain exceptions (400).
     */
    @ExceptionHandler(DomainException.class)
    public ProblemDetail handleDomainException(DomainException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage());
        problemDetail.setTitle("Business Rule Violation");
        problemDetail.setType(URI.create("https://banking-ledger.com/errors/domain-error"));
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }

    /**
     * Handles illegal argument exceptions (400).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgumentException(IllegalArgumentException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage());
        problemDetail.setTitle("Invalid Request");
        problemDetail.setType(URI.create("https://banking-ledger.com/errors/invalid-request"));
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }

    /**
     * Handles all other exceptions (500).
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred");
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setType(URI.create("https://banking-ledger.com/errors/internal-error"));
        problemDetail.setProperty("timestamp", Instant.now());

        // Log the actual exception for debugging
        // logger.error("Unexpected error", ex);

        return problemDetail;
    }
}

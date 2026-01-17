package com.sbaldasso.java_banking_core.api.controller;

import com.sbaldasso.java_banking_core.application.command.PostTransactionCommand;
import com.sbaldasso.java_banking_core.application.dto.TransactionDto;
import com.sbaldasso.java_banking_core.application.service.LedgerApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for transaction operations.
 */
@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final LedgerApplicationService ledgerService;

    public TransactionController(LedgerApplicationService ledgerService) {
        this.ledgerService = ledgerService;
    }

    /**
     * Posts a new transaction to the ledger.
     * POST /api/v1/transactions
     * 
     * Idempotent: same externalId returns existing transaction
     */
    @PostMapping
    public ResponseEntity<TransactionDto> postTransaction(@RequestBody PostTransactionCommand command) {
        TransactionDto transaction = ledgerService.postTransaction(command);

        // Return 200 if transaction already existed (idempotency), 201 if newly created
        // We can check by comparing if transaction was just created
        HttpStatus status = HttpStatus.CREATED;
        return ResponseEntity.status(status).body(transaction);
    }

    /**
     * Gets a transaction by ID.
     * GET /api/v1/transactions/{transactionId}
     */
    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionDto> getTransaction(@PathVariable UUID transactionId) {
        TransactionDto transaction = ledgerService.getTransaction(transactionId);
        return ResponseEntity.ok(transaction);
    }

    /**
     * Reverses a posted transaction.
     * POST /api/v1/transactions/{transactionId}/reverse
     * 
     * Idempotent: same reversalExternalId returns existing reversal
     */
    @PostMapping("/{transactionId}/reverse")
    public ResponseEntity<TransactionDto> reverseTransaction(
            @PathVariable UUID transactionId,
            @RequestBody ReverseTransactionRequest request) {

        TransactionDto reversalTransaction = ledgerService.reverseTransaction(
                transactionId,
                request.getReversalExternalId());

        return ResponseEntity.status(HttpStatus.CREATED).body(reversalTransaction);
    }

    /**
     * Request DTO for reversing a transaction.
     */
    public static class ReverseTransactionRequest {
        private UUID reversalExternalId;

        public UUID getReversalExternalId() {
            return reversalExternalId;
        }

        public void setReversalExternalId(UUID reversalExternalId) {
            this.reversalExternalId = reversalExternalId;
        }
    }
}

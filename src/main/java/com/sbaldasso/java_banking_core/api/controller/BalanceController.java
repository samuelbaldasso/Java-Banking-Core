package com.sbaldasso.java_banking_core.api.controller;

import com.sbaldasso.java_banking_core.application.dto.BalanceDto;
import com.sbaldasso.java_banking_core.application.service.BalanceApplicationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

/**
 * REST controller for balance queries.
 */
@RestController
@RequestMapping("/api/v1/balances")
public class BalanceController {

    private final BalanceApplicationService balanceService;

    public BalanceController(BalanceApplicationService balanceService) {
        this.balanceService = balanceService;
    }

    /**
     * Gets the current balance for an account.
     * GET /api/v1/balances/{accountId}
     */
    @GetMapping("/{accountId}")
    public ResponseEntity<BalanceDto> getBalance(@PathVariable UUID accountId) {
        BalanceDto balance = balanceService.getBalance(accountId);
        return ResponseEntity.ok(balance);
    }

    /**
     * Gets the balance for an account as of a specific time.
     * GET /api/v1/balances/{accountId}/as-of?time=2024-01-01T00:00:00Z
     */
    @GetMapping("/{accountId}/as-of")
    public ResponseEntity<BalanceDto> getBalanceAsOf(
            @PathVariable UUID accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant time) {

        BalanceDto balance = balanceService.getBalanceAsOf(accountId, time);
        return ResponseEntity.ok(balance);
    }
}

package com.sbaldasso.java_banking_core.api.controller;

import com.sbaldasso.java_banking_core.application.dto.AccountDto;
import com.sbaldasso.java_banking_core.application.service.AccountApplicationService;
import com.sbaldasso.java_banking_core.domain.valueobject.AccountType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for account operations.
 */
@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountApplicationService accountService;

    public AccountController(AccountApplicationService accountService) {
        this.accountService = accountService;
    }

    /**
     * Creates a new account.
     * POST /api/v1/accounts
     */
    @PostMapping
    public ResponseEntity<AccountDto> createAccount(@RequestBody CreateAccountRequest request) {
        AccountDto account = accountService.createAccount(
                request.getAccountType(),
                request.getCurrency());
        return ResponseEntity.status(HttpStatus.CREATED).body(account);
    }

    /**
     * Gets an account by ID.
     * GET /api/v1/accounts/{accountId}
     */
    @GetMapping("/{accountId}")
    public ResponseEntity<AccountDto> getAccount(@PathVariable UUID accountId) {
        AccountDto account = accountService.getAccount(accountId);
        return ResponseEntity.ok(account);
    }

    /**
     * Lists all accounts with pagination.
     * GET /api/v1/accounts?page=0&size=20&sort=createdAt,desc
     * 
     * @param pageable Pagination parameters (page, size, sort)
     * @return Paginated list of accounts
     */
    @GetMapping
    public ResponseEntity<Page<AccountDto>> listAccounts(Pageable pageable) {
        Page<AccountDto> accounts = accountService.listAccounts(pageable);
        return ResponseEntity.ok(accounts);
    }

    /**
     * Blocks an account.
     * POST /api/v1/accounts/{accountId}/block
     */
    @PostMapping("/{accountId}/block")
    public ResponseEntity<AccountDto> blockAccount(@PathVariable UUID accountId) {
        AccountDto account = accountService.blockAccount(accountId);
        return ResponseEntity.ok(account);
    }

    /**
     * Unblocks an account.
     * POST /api/v1/accounts/{accountId}/unblock
     */
    @PostMapping("/{accountId}/unblock")
    public ResponseEntity<AccountDto> unblockAccount(@PathVariable UUID accountId) {
        AccountDto account = accountService.unblockAccount(accountId);
        return ResponseEntity.ok(account);
    }

    /**
     * Closes an account.
     * POST /api/v1/accounts/{accountId}/close
     */
    @PostMapping("/{accountId}/close")
    public ResponseEntity<AccountDto> closeAccount(@PathVariable UUID accountId) {
        AccountDto account = accountService.closeAccount(accountId);
        return ResponseEntity.ok(account);
    }

    /**
     * Request DTO for creating an account.
     */
    public static class CreateAccountRequest {
        private AccountType accountType;
        private String currency;

        public AccountType getAccountType() {
            return accountType;
        }

        public void setAccountType(AccountType accountType) {
            this.accountType = accountType;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }
    }
}

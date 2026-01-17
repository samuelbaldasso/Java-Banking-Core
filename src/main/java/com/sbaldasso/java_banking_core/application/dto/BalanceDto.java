package com.sbaldasso.java_banking_core.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for Balance information.
 */
public class BalanceDto {
    private UUID accountId;
    private BigDecimal balance;
    private String currency;

    public BalanceDto() {
    }

    public BalanceDto(UUID accountId, BigDecimal balance, String currency) {
        this.accountId = accountId;
        this.balance = balance;
        this.currency = currency;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}

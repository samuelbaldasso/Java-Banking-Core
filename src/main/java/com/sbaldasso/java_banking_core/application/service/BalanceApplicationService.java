package com.sbaldasso.java_banking_core.application.service;

import com.sbaldasso.java_banking_core.application.dto.BalanceDto;
import com.sbaldasso.java_banking_core.domain.exception.InvalidAccountException;
import com.sbaldasso.java_banking_core.domain.model.Account;
import com.sbaldasso.java_banking_core.domain.model.LedgerEntry;
import com.sbaldasso.java_banking_core.domain.service.BalanceCalculator;
import com.sbaldasso.java_banking_core.domain.valueobject.Money;
import com.sbaldasso.java_banking_core.infrastructure.persistence.entity.AccountJpaEntity;
import com.sbaldasso.java_banking_core.infrastructure.persistence.entity.LedgerEntryJpaEntity;
import com.sbaldasso.java_banking_core.infrastructure.persistence.mapper.AccountMapper;
import com.sbaldasso.java_banking_core.infrastructure.persistence.mapper.LedgerEntryMapper;
import com.sbaldasso.java_banking_core.infrastructure.persistence.repository.AccountJpaRepository;
import com.sbaldasso.java_banking_core.infrastructure.persistence.repository.LedgerEntryJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Application service for balance queries.
 * Calculates balances on-demand from ledger entries.
 */
@Service
@Transactional(readOnly = true)
public class BalanceApplicationService {

    private final AccountJpaRepository accountRepository;
    private final LedgerEntryJpaRepository entryRepository;
    private final BalanceCalculator balanceCalculator;
    private final AccountMapper accountMapper;
    private final LedgerEntryMapper entryMapper;

    public BalanceApplicationService(
            AccountJpaRepository accountRepository,
            LedgerEntryJpaRepository entryRepository,
            BalanceCalculator balanceCalculator,
            AccountMapper accountMapper,
            LedgerEntryMapper entryMapper) {
        this.accountRepository = accountRepository;
        this.entryRepository = entryRepository;
        this.balanceCalculator = balanceCalculator;
        this.accountMapper = accountMapper;
        this.entryMapper = entryMapper;
    }

    /**
     * Gets the current balance for an account.
     * Calculates from all POSTED entries.
     */
    public BalanceDto getBalance(UUID accountId) {
        // Load account
        AccountJpaEntity accountEntity = accountRepository.findById(accountId)
                .orElseThrow(() -> new InvalidAccountException("Account not found: " + accountId));

        Account account = accountMapper.toDomain(accountEntity);

        // Load all posted entries for this account
        List<LedgerEntryJpaEntity> entryEntities = entryRepository.findPostedEntriesByAccount(accountId);

        List<LedgerEntry> entries = entryEntities.stream()
                .map(entryMapper::toDomain)
                .collect(Collectors.toList());

        // Calculate balance
        Money balance = balanceCalculator.calculateBalance(account, entries);

        return new BalanceDto(
                accountId,
                balance.getAmount(),
                balance.getCurrencyCode());
    }

    /**
     * Gets the balance for an account as of a specific point in time.
     */
    public BalanceDto getBalanceAsOf(UUID accountId, Instant asOfTime) {
        // Load account
        AccountJpaEntity accountEntity = accountRepository.findById(accountId)
                .orElseThrow(() -> new InvalidAccountException("Account not found: " + accountId));

        Account account = accountMapper.toDomain(accountEntity);

        // Load posted entries up to the specified time
        List<LedgerEntryJpaEntity> entryEntities = entryRepository.findPostedEntriesByAccountAsOf(accountId, asOfTime);

        List<LedgerEntry> entries = entryEntities.stream()
                .map(entryMapper::toDomain)
                .collect(Collectors.toList());

        // Calculate historical balance
        Money balance = balanceCalculator.calculateBalanceAsOf(account, entries, asOfTime);

        return new BalanceDto(
                accountId,
                balance.getAmount(),
                balance.getCurrencyCode());
    }
}

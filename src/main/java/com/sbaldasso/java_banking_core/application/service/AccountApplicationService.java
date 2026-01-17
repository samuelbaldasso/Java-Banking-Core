package com.sbaldasso.java_banking_core.application.service;

import com.sbaldasso.java_banking_core.application.dto.AccountDto;
import com.sbaldasso.java_banking_core.domain.exception.InvalidAccountException;
import com.sbaldasso.java_banking_core.domain.model.Account;
import com.sbaldasso.java_banking_core.domain.valueobject.AccountType;
import com.sbaldasso.java_banking_core.infrastructure.persistence.entity.AccountJpaEntity;
import com.sbaldasso.java_banking_core.infrastructure.persistence.mapper.AccountMapper;
import com.sbaldasso.java_banking_core.infrastructure.persistence.repository.AccountJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Application service for account operations.
 * Orchestrates account creation and management.
 */
@Service
@Transactional
public class AccountApplicationService {

    private final AccountJpaRepository accountRepository;
    private final AccountMapper accountMapper;

    public AccountApplicationService(AccountJpaRepository accountRepository,
            AccountMapper accountMapper) {
        this.accountRepository = accountRepository;
        this.accountMapper = accountMapper;
    }

    /**
     * Creates a new account.
     */
    public AccountDto createAccount(AccountType accountType, String currency) {
        Account account = Account.create(accountType, currency);

        AccountJpaEntity entity = accountMapper.toEntity(account);
        accountRepository.save(entity);

        return toDto(account);
    }

    /**
     * Retrieves an account by ID.
     */
    @Transactional(readOnly = true)
    public AccountDto getAccount(UUID accountId) {
        AccountJpaEntity entity = accountRepository.findById(accountId)
                .orElseThrow(() -> new InvalidAccountException("Account not found: " + accountId));

        Account account = accountMapper.toDomain(entity);
        return toDto(account);
    }

    /**
     * Lists all accounts.
     */
    @Transactional(readOnly = true)
    public List<AccountDto> listAccounts() {
        return accountRepository.findAll().stream()
                .map(accountMapper::toDomain)
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Blocks an account.
     */
    public AccountDto blockAccount(UUID accountId) {
        AccountJpaEntity entity = accountRepository.findById(accountId)
                .orElseThrow(() -> new InvalidAccountException("Account not found: " + accountId));

        Account account = accountMapper.toDomain(entity);
        account.block();

        entity.setStatus(account.getStatus());
        accountRepository.save(entity);

        return toDto(account);
    }

    /**
     * Unblocks an account.
     */
    public AccountDto unblockAccount(UUID accountId) {
        AccountJpaEntity entity = accountRepository.findById(accountId)
                .orElseThrow(() -> new InvalidAccountException("Account not found: " + accountId));

        Account account = accountMapper.toDomain(entity);
        account.unblock();

        entity.setStatus(account.getStatus());
        accountRepository.save(entity);

        return toDto(account);
    }

    /**
     * Closes an account.
     */
    public AccountDto closeAccount(UUID accountId) {
        AccountJpaEntity entity = accountRepository.findById(accountId)
                .orElseThrow(() -> new InvalidAccountException("Account not found: " + accountId));

        Account account = accountMapper.toDomain(entity);
        account.close();

        entity.setStatus(account.getStatus());
        accountRepository.save(entity);

        return toDto(account);
    }

    /**
     * Converts Account domain model to DTO.
     */
    private AccountDto toDto(Account account) {
        return new AccountDto(
                account.getAccountId(),
                account.getAccountType(),
                account.getCurrencyCode(),
                account.getStatus(),
                account.getCreatedAt());
    }
}

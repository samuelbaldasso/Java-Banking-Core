package com.sbaldasso.java_banking_core.application.service;

import com.sbaldasso.java_banking_core.domain.exception.InvalidAccountException;
import com.sbaldasso.java_banking_core.domain.model.Account;
import com.sbaldasso.java_banking_core.domain.model.BalanceSnapshot;
import com.sbaldasso.java_banking_core.domain.service.BalanceCalculator;
import com.sbaldasso.java_banking_core.domain.valueobject.AccountType;
import com.sbaldasso.java_banking_core.domain.valueobject.Money;
import com.sbaldasso.java_banking_core.infrastructure.persistence.entity.AccountJpaEntity;
import com.sbaldasso.java_banking_core.infrastructure.persistence.entity.BalanceSnapshotJpaEntity;
import com.sbaldasso.java_banking_core.infrastructure.persistence.mapper.AccountMapper;
import com.sbaldasso.java_banking_core.infrastructure.persistence.mapper.BalanceSnapshotMapper;
import com.sbaldasso.java_banking_core.infrastructure.persistence.mapper.LedgerEntryMapper;
import com.sbaldasso.java_banking_core.infrastructure.persistence.repository.AccountJpaRepository;
import com.sbaldasso.java_banking_core.infrastructure.persistence.repository.BalanceSnapshotJpaRepository;
import com.sbaldasso.java_banking_core.infrastructure.persistence.repository.LedgerEntryJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SnapshotApplicationService.
 */
@ExtendWith(MockitoExtension.class)
class SnapshotApplicationServiceTest {

    @Mock
    private AccountJpaRepository accountRepository;

    @Mock
    private LedgerEntryJpaRepository entryRepository;

    @Mock
    private BalanceSnapshotJpaRepository snapshotRepository;

    @Mock
    private BalanceCalculator balanceCalculator;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private LedgerEntryMapper entryMapper;

    @Mock
    private BalanceSnapshotMapper snapshotMapper;

    private SnapshotApplicationService service;

    @BeforeEach
    void setUp() {
        service = new SnapshotApplicationService(
                accountRepository,
                entryRepository,
                snapshotRepository,
                balanceCalculator,
                accountMapper,
                entryMapper,
                snapshotMapper);
    }

    @Test
    void shouldCreateSnapshotForAccount() {
        // Given
        UUID accountId = UUID.randomUUID();
        Instant snapshotTime = Instant.now().minusSeconds(3600);

        AccountJpaEntity accountEntity = createAccountEntity(accountId, AccountType.ASSET, "BRL");

        Account account = Account.create(AccountType.ASSET, "BRL");
        Money balance = Money.of(new BigDecimal("1000.00"), "BRL");

        when(snapshotRepository.existsByAccountIdAndSnapshotTime(accountId, snapshotTime))
                .thenReturn(false);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(accountEntity));
        when(accountMapper.toDomain(accountEntity)).thenReturn(account);
        when(entryRepository.findPostedEntriesByAccountAsOf(accountId, snapshotTime))
                .thenReturn(List.of());
        when(balanceCalculator.calculateBalanceAsOf(any(), any(), any())).thenReturn(balance);

        when(snapshotMapper.toEntity(any(BalanceSnapshot.class)))
                .thenReturn(createSnapshotEntity());

        // When
        BalanceSnapshot result = service.createSnapshotForAccount(accountId, snapshotTime);

        // Then
        assertNotNull(result);
        verify(snapshotRepository).save(any(BalanceSnapshotJpaEntity.class));
    }

    @Test
    void shouldSkipCreatingDuplicateSnapshot() {
        // Given
        UUID accountId = UUID.randomUUID();
        Instant snapshotTime = Instant.now().minusSeconds(3600);

        BalanceSnapshotJpaEntity existingEntity = createSnapshotEntity();
        BalanceSnapshot existingSnapshot = BalanceSnapshot.create(
                accountId, Money.zero("BRL"), snapshotTime, null);

        when(snapshotRepository.existsByAccountIdAndSnapshotTime(accountId, snapshotTime))
                .thenReturn(true);
        when(snapshotRepository.findByAccountIdAndSnapshotTime(accountId, snapshotTime))
                .thenReturn(Optional.of(existingEntity));
        when(snapshotMapper.toDomain(existingEntity)).thenReturn(existingSnapshot);

        // When
        BalanceSnapshot result = service.createSnapshotForAccount(accountId, snapshotTime);

        // Then
        assertNotNull(result);
        verify(snapshotRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionForNonExistentAccount() {
        // Given
        UUID accountId = UUID.randomUUID();
        Instant snapshotTime = Instant.now().minusSeconds(3600);

        when(snapshotRepository.existsByAccountIdAndSnapshotTime(accountId, snapshotTime))
                .thenReturn(false);
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(InvalidAccountException.class, () -> {
            service.createSnapshotForAccount(accountId, snapshotTime);
        });
    }

    @Test
    void shouldCreateSnapshotsForAllAccounts() {
        // Given
        Instant snapshotTime = Instant.now().minusSeconds(3600);

        UUID account1Id = UUID.randomUUID();
        UUID account2Id = UUID.randomUUID();

        AccountJpaEntity account1 = createAccountEntity(account1Id, AccountType.ASSET, "BRL");
        AccountJpaEntity account2 = createAccountEntity(account2Id, AccountType.LIABILITY, "BRL");

        when(accountRepository.findAll()).thenReturn(List.of(account1, account2));
        when(snapshotRepository.existsByAccountIdAndSnapshotTime(any(), any())).thenReturn(false);
        when(accountRepository.findById(any())).thenAnswer(invocation -> {
            UUID id = invocation.getArgument(0);
            if (id.equals(account1.getAccountId())) {
                return Optional.of(account1);
            } else if (id.equals(account2.getAccountId())) {
                return Optional.of(account2);
            }
            return Optional.empty();
        });
        when(accountMapper.toDomain(any())).thenAnswer(invocation -> {
            AccountJpaEntity entity = invocation.getArgument(0);
            return Account.create(entity.getAccountType(), entity.getCurrency());
        });
        when(entryRepository.findPostedEntriesByAccountAsOf(any(), any())).thenReturn(List.of());
        when(balanceCalculator.calculateBalanceAsOf(any(), any(), any()))
                .thenReturn(Money.zero("BRL"));
        when(snapshotMapper.toEntity(any())).thenReturn(createSnapshotEntity());

        // When
        int count = service.createSnapshotsForAllAccounts(snapshotTime);

        // Then
        assertEquals(2, count);
        verify(snapshotRepository, times(2)).save(any(BalanceSnapshotJpaEntity.class));
    }

    @Test
    void shouldContinueWithOtherAccountsIfOneFails() {
        // Given
        Instant snapshotTime = Instant.now().minusSeconds(3600);

        UUID account1Id = UUID.randomUUID();
        UUID account2Id = UUID.randomUUID();

        AccountJpaEntity account1 = createAccountEntity(account1Id, AccountType.ASSET, "BRL");
        AccountJpaEntity account2 = createAccountEntity(account2Id, AccountType.LIABILITY, "BRL");

        when(accountRepository.findAll()).thenReturn(List.of(account1, account2));
        when(snapshotRepository.existsByAccountIdAndSnapshotTime(any(), any())).thenReturn(false);

        // First account throws exception
        when(accountRepository.findById(account1.getAccountId()))
                .thenThrow(new RuntimeException("Database error"));

        // Second account succeeds
        when(accountRepository.findById(account2.getAccountId())).thenReturn(Optional.of(account2));
        when(accountMapper.toDomain(account2)).thenReturn(Account.create(AccountType.LIABILITY, "BRL"));
        when(entryRepository.findPostedEntriesByAccountAsOf(any(), any())).thenReturn(List.of());
        when(balanceCalculator.calculateBalanceAsOf(any(), any(), any()))
                .thenReturn(Money.zero("BRL"));
        when(snapshotMapper.toEntity(any())).thenReturn(createSnapshotEntity());

        // When
        int count = service.createSnapshotsForAllAccounts(snapshotTime);

        // Then: only one snapshot created (account2)
        assertEquals(1, count);
        verify(snapshotRepository, times(1)).save(any(BalanceSnapshotJpaEntity.class));
    }

    // Helper methods to create test entities
    private AccountJpaEntity createAccountEntity(UUID accountId, AccountType accountType, String currency) {
        AccountJpaEntity entity = new AccountJpaEntity(
                accountId,
                accountType,
                currency,
                com.sbaldasso.java_banking_core.domain.valueobject.AccountStatus.ACTIVE,
                Instant.now());
        return entity;
    }

    private BalanceSnapshotJpaEntity createSnapshotEntity() {
        return new BalanceSnapshotJpaEntity(
                UUID.randomUUID(),
                UUID.randomUUID(),
                BigDecimal.ZERO,
                "BRL",
                Instant.now(),
                null,
                Instant.now());
    }
}

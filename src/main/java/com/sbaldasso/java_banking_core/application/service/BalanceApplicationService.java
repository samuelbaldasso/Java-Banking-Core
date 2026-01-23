package com.sbaldasso.java_banking_core.application.service;

import com.sbaldasso.java_banking_core.application.dto.BalanceDto;
import com.sbaldasso.java_banking_core.domain.exception.InvalidAccountException;
import com.sbaldasso.java_banking_core.domain.model.Account;
import com.sbaldasso.java_banking_core.domain.model.BalanceSnapshot;
import com.sbaldasso.java_banking_core.domain.model.LedgerEntry;
import com.sbaldasso.java_banking_core.domain.service.BalanceCalculator;
import com.sbaldasso.java_banking_core.domain.valueobject.Money;
import com.sbaldasso.java_banking_core.infrastructure.persistence.entity.AccountJpaEntity;
import com.sbaldasso.java_banking_core.infrastructure.persistence.entity.BalanceSnapshotJpaEntity;
import com.sbaldasso.java_banking_core.infrastructure.persistence.entity.LedgerEntryJpaEntity;
import com.sbaldasso.java_banking_core.infrastructure.persistence.mapper.AccountMapper;
import com.sbaldasso.java_banking_core.infrastructure.persistence.mapper.BalanceSnapshotMapper;
import com.sbaldasso.java_banking_core.infrastructure.persistence.mapper.LedgerEntryMapper;
import com.sbaldasso.java_banking_core.infrastructure.persistence.repository.AccountJpaRepository;
import com.sbaldasso.java_banking_core.infrastructure.persistence.repository.BalanceSnapshotJpaRepository;
import com.sbaldasso.java_banking_core.infrastructure.persistence.repository.LedgerEntryJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Application service for balance queries.
 * Calculates balances using snapshot optimization when available.
 * 
 * Performance Optimization:
 * - Uses latest snapshot as starting point
 * - Only loads entries after snapshot time
 * - Falls back to full calculation if no snapshot exists
 */
@Service
@Transactional(readOnly = true)
public class BalanceApplicationService {

        private static final Logger logger = LoggerFactory.getLogger(BalanceApplicationService.class);

        private final AccountJpaRepository accountRepository;
        private final LedgerEntryJpaRepository entryRepository;
        private final BalanceSnapshotJpaRepository snapshotRepository;
        private final BalanceCalculator balanceCalculator;
        private final AccountMapper accountMapper;
        private final LedgerEntryMapper entryMapper;
        private final BalanceSnapshotMapper snapshotMapper;

        public BalanceApplicationService(
                        AccountJpaRepository accountRepository,
                        LedgerEntryJpaRepository entryRepository,
                        BalanceSnapshotJpaRepository snapshotRepository,
                        BalanceCalculator balanceCalculator,
                        AccountMapper accountMapper,
                        LedgerEntryMapper entryMapper,
                        BalanceSnapshotMapper snapshotMapper) {
                this.accountRepository = accountRepository;
                this.entryRepository = entryRepository;
                this.snapshotRepository = snapshotRepository;
                this.balanceCalculator = balanceCalculator;
                this.accountMapper = accountMapper;
                this.entryMapper = entryMapper;
                this.snapshotMapper = snapshotMapper;
        }

        /**
         * Gets the current balance for an account.
         * Uses snapshot optimization when available.
         */
        public BalanceDto getBalance(UUID accountId) {
                // Load account
                AccountJpaEntity accountEntity = accountRepository.findById(accountId)
                                .orElseThrow(() -> new InvalidAccountException("Account not found: " + accountId));

                Account account = accountMapper.toDomain(accountEntity);

                // Try to load latest snapshot
                Optional<BalanceSnapshotJpaEntity> snapshotEntity = snapshotRepository
                                .findLatestSnapshotByAccount(accountId);

                Money balance;

                if (snapshotEntity.isPresent()) {
                        // Snapshot exists - use optimized calculation
                        BalanceSnapshot snapshot = snapshotMapper.toDomain(snapshotEntity.get());
                        balance = calculateBalanceFromSnapshot(account, snapshot, null);

                        logger.debug("Calculated balance for account {} using snapshot from {}",
                                        accountId, snapshot.getSnapshotTime());
                } else {
                        // No snapshot - fall back to full calculation
                        balance = calculateBalanceFull(account, null);

                        logger.debug("Calculated balance for account {} without snapshot (full calculation)",
                                        accountId);
                }

                return new BalanceDto(
                                accountId,
                                balance.getAmount(),
                                balance.getCurrencyCode());
        }

        /**
         * Gets the balance for an account as of a specific point in time.
         * Uses snapshot optimization when available.
         */
        public BalanceDto getBalanceAsOf(UUID accountId, Instant asOfTime) {
                // Load account
                AccountJpaEntity accountEntity = accountRepository.findById(accountId)
                                .orElseThrow(() -> new InvalidAccountException("Account not found: " + accountId));

                Account account = accountMapper.toDomain(accountEntity);

                // Try to load latest snapshot before the asOfTime
                Optional<BalanceSnapshotJpaEntity> snapshotEntity = snapshotRepository
                                .findLatestSnapshotByAccountBeforeTime(accountId, asOfTime);

                Money balance;

                if (snapshotEntity.isPresent()) {
                        // Snapshot exists - use optimized calculation
                        BalanceSnapshot snapshot = snapshotMapper.toDomain(snapshotEntity.get());
                        balance = calculateBalanceFromSnapshot(account, snapshot, asOfTime);

                        logger.debug("Calculated historical balance for account {} as of {} using snapshot from {}",
                                        accountId, asOfTime, snapshot.getSnapshotTime());
                } else {
                        // No snapshot - fall back to full calculation
                        balance = calculateBalanceFull(account, asOfTime);

                        logger.debug("Calculated historical balance for account {} as of {} without snapshot",
                                        accountId, asOfTime);
                }

                return new BalanceDto(
                                accountId,
                                balance.getAmount(),
                                balance.getCurrencyCode());
        }

        /**
         * Calculates balance from a snapshot by adding entries after snapshot time.
         * This is the optimized path that loads only incremental entries.
         */
        private Money calculateBalanceFromSnapshot(Account account, BalanceSnapshot snapshot, Instant asOfTime) {
                // Start with snapshot balance
                Money balance = snapshot.getBalance();

                // Load only entries after snapshot time (and before asOfTime if specified)
                List<LedgerEntryJpaEntity> entryEntities;

                if (asOfTime != null) {
                        entryEntities = entryRepository.findPostedEntriesBetween(
                                        snapshot.getSnapshotTime().plusMillis(1), // Exclusive of snapshot time
                                        asOfTime);
                } else {
                        // For current balance, we need entries after snapshot
                        entryEntities = findEntriesAfterSnapshot(account.getAccountId(), snapshot.getSnapshotTime());
                }

                // Filter to only this account's entries
                List<LedgerEntry> entries = entryEntities.stream()
                                .filter(e -> e.getAccountId().equals(account.getAccountId()))
                                .map(entryMapper::toDomain)
                                .collect(Collectors.toList());

                // Apply incremental entries to snapshot balance
                for (LedgerEntry entry : entries) {
                        if (asOfTime == null || !entry.getEventTime().isAfter(asOfTime)) {
                                balance = applyEntryToBalance(balance, entry, account.getAccountType());
                        }
                }

                return balance;
        }

        /**
         * Falls back to full balance calculation when no snapshot is available.
         * This is the original behavior - loads all entries.
         */
        private Money calculateBalanceFull(Account account, Instant asOfTime) {
                List<LedgerEntryJpaEntity> entryEntities;

                if (asOfTime != null) {
                        entryEntities = entryRepository.findPostedEntriesByAccountAsOf(account.getAccountId(),
                                        asOfTime);
                } else {
                        entryEntities = entryRepository.findPostedEntriesByAccount(account.getAccountId());
                }

                List<LedgerEntry> entries = entryEntities.stream()
                                .map(entryMapper::toDomain)
                                .collect(Collectors.toList());

                return balanceCalculator.calculateBalanceAsOf(account, entries, asOfTime);
        }

        /**
         * Helper method to find entries after snapshot time.
         * Uses the findPostedEntriesBetween query with a far future end time.
         */
        private List<LedgerEntryJpaEntity> findEntriesAfterSnapshot(UUID accountId, Instant snapshotTime) {
                // Use a far future time as the end boundary
                Instant farFuture = Instant.now().plusSeconds(315360000); // ~10 years

                return entryRepository.findPostedEntriesBetween(
                                snapshotTime.plusMillis(1), // Exclusive of snapshot time
                                farFuture);
        }

        /**
         * Applies a single entry to the balance (copied from BalanceCalculator logic).
         */
        private Money applyEntryToBalance(Money currentBalance, LedgerEntry entry,
                        com.sbaldasso.java_banking_core.domain.valueobject.AccountType accountType) {
                boolean isIncrease = shouldIncreaseBalance(accountType, entry.isDebit());

                if (isIncrease) {
                        return currentBalance.add(entry.getAmount());
                } else {
                        return currentBalance.subtract(entry.getAmount());
                }
        }

        /**
         * Determines if an entry should increase the balance (copied from
         * BalanceCalculator logic).
         */
        private boolean shouldIncreaseBalance(
                        com.sbaldasso.java_banking_core.domain.valueobject.AccountType accountType,
                        boolean isDebit) {
                return switch (accountType) {
                        case ASSET, EXPENSE -> isDebit;
                        case LIABILITY, EQUITY, REVENUE -> !isDebit;
                };
        }
}

package com.sbaldasso.java_banking_core.application.service;

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
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Application service for managing balance snapshots.
 * Responsible for creating and persisting balance snapshots.
 */
@Service
public class SnapshotApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(SnapshotApplicationService.class);

    private final AccountJpaRepository accountRepository;
    private final LedgerEntryJpaRepository entryRepository;
    private final BalanceSnapshotJpaRepository snapshotRepository;
    private final BalanceCalculator balanceCalculator;
    private final AccountMapper accountMapper;
    private final LedgerEntryMapper entryMapper;
    private final BalanceSnapshotMapper snapshotMapper;

    public SnapshotApplicationService(
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
     * Creates a balance snapshot for a specific account at a given time.
     * 
     * @param accountId    The account to snapshot
     * @param snapshotTime The point in time for the snapshot
     * @return The created snapshot
     * @throws InvalidAccountException if account doesn't exist
     */
    @Transactional
    public BalanceSnapshot createSnapshotForAccount(UUID accountId, Instant snapshotTime) {
        // Check if snapshot already exists
        if (snapshotRepository.existsByAccountIdAndSnapshotTime(accountId, snapshotTime)) {
            logger.info("Snapshot already exists for account {} at {}, skipping", accountId, snapshotTime);
            return snapshotMapper.toDomain(
                    snapshotRepository.findByAccountIdAndSnapshotTime(accountId, snapshotTime)
                            .orElseThrow());
        }

        // Load account
        AccountJpaEntity accountEntity = accountRepository.findById(accountId)
                .orElseThrow(() -> new InvalidAccountException("Account not found: " + accountId));

        Account account = accountMapper.toDomain(accountEntity);

        // Calculate balance up to snapshot time (full calculation, no optimization)
        List<LedgerEntryJpaEntity> entryEntities = entryRepository
                .findPostedEntriesByAccountAsOf(accountId, snapshotTime);

        List<LedgerEntry> entries = entryEntities.stream()
                .map(entryMapper::toDomain)
                .collect(Collectors.toList());

        Money balance = balanceCalculator.calculateBalanceAsOf(account, entries, snapshotTime);

        // Find last entry ID for auditing
        UUID lastEntryId = entries.isEmpty() ? null
                : entries.get(entries.size() - 1).getLedgerEntryId();

        // Create snapshot domain model
        BalanceSnapshot snapshot = BalanceSnapshot.create(
                accountId,
                balance,
                snapshotTime,
                lastEntryId);

        // Persist snapshot
        BalanceSnapshotJpaEntity snapshotEntity = snapshotMapper.toEntity(snapshot);
        snapshotRepository.save(snapshotEntity);

        logger.info("Created snapshot for account {} at {} with balance {}",
                accountId, snapshotTime, balance);

        return snapshot;
    }

    /**
     * Creates snapshots for all active accounts at a given time.
     * Typically called by scheduled job.
     * 
     * @param snapshotTime The point in time for the snapshots
     * @return Number of snapshots created
     */
    @Transactional
    public int createSnapshotsForAllAccounts(Instant snapshotTime) {
        logger.info("Starting snapshot creation for all accounts at {}", snapshotTime);

        List<AccountJpaEntity> accounts = accountRepository.findAll();
        int snapshotsCreated = 0;

        for (AccountJpaEntity accountEntity : accounts) {
            try {
                // Only create snapshots for active accounts
                if (accountEntity.getStatus().canAcceptTransactions()) {
                    createSnapshotForAccount(accountEntity.getAccountId(), snapshotTime);
                    snapshotsCreated++;
                }
            } catch (Exception e) {
                logger.error("Failed to create snapshot for account {}: {}",
                        accountEntity.getAccountId(), e.getMessage(), e);
                // Continue with other accounts even if one fails
            }
        }

        logger.info("Completed snapshot creation: {} snapshots created for {} accounts",
                snapshotsCreated, accounts.size());

        return snapshotsCreated;
    }

    /**
     * Creates a snapshot for the current time (convenience method).
     */
    @Transactional
    public BalanceSnapshot createCurrentSnapshotForAccount(UUID accountId) {
        return createSnapshotForAccount(accountId, Instant.now());
    }

    /**
     * Creates snapshots for all accounts at the current time (convenience method).
     */
    @Transactional
    public int createCurrentSnapshotsForAllAccounts() {
        return createSnapshotsForAllAccounts(Instant.now());
    }
}

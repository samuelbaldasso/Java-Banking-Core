package com.sbaldasso.java_banking_core.application.service;

import com.sbaldasso.java_banking_core.application.command.EntryCommand;
import com.sbaldasso.java_banking_core.application.command.PostTransactionCommand;
import com.sbaldasso.java_banking_core.application.dto.LedgerEntryDto;
import com.sbaldasso.java_banking_core.application.dto.TransactionDto;
import com.sbaldasso.java_banking_core.domain.exception.InvalidAccountException;
import com.sbaldasso.java_banking_core.domain.exception.InvalidTransactionException;
import com.sbaldasso.java_banking_core.domain.model.Account;
import com.sbaldasso.java_banking_core.domain.model.LedgerEntry;
import com.sbaldasso.java_banking_core.domain.model.LedgerTransaction;
import com.sbaldasso.java_banking_core.domain.service.TransactionProcessor;
import com.sbaldasso.java_banking_core.domain.valueobject.Money;
import com.sbaldasso.java_banking_core.infrastructure.persistence.entity.AccountJpaEntity;
import com.sbaldasso.java_banking_core.infrastructure.persistence.entity.LedgerTransactionJpaEntity;
import com.sbaldasso.java_banking_core.infrastructure.persistence.mapper.AccountMapper;
import com.sbaldasso.java_banking_core.infrastructure.persistence.mapper.LedgerTransactionMapper;
import com.sbaldasso.java_banking_core.infrastructure.persistence.repository.AccountJpaRepository;
import com.sbaldasso.java_banking_core.infrastructure.persistence.repository.LedgerTransactionJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Application service for ledger operations.
 * Orchestrates transaction posting, reversal, and queries with idempotency.
 */
@Service
@Transactional
public class LedgerApplicationService {

        private static final Logger logger = LoggerFactory.getLogger(LedgerApplicationService.class);

        private final AccountJpaRepository accountRepository;
        private final LedgerTransactionJpaRepository transactionRepository;
        private final TransactionProcessor transactionProcessor;
        private final OutboxService outboxService;
        private final AccountMapper accountMapper;
        private final LedgerTransactionMapper transactionMapper;

        public LedgerApplicationService(
                        AccountJpaRepository accountRepository,
                        LedgerTransactionJpaRepository transactionRepository,
                        TransactionProcessor transactionProcessor,
                        OutboxService outboxService,
                        AccountMapper accountMapper,
                        LedgerTransactionMapper transactionMapper) {
                this.accountRepository = accountRepository;
                this.transactionRepository = transactionRepository;
                this.transactionProcessor = transactionProcessor;
                this.outboxService = outboxService;
                this.accountMapper = accountMapper;
                this.transactionMapper = transactionMapper;
        }

        /**
         * Posts a transaction to the ledger.
         * Implements idempotency via externalId - same externalId returns existing
         * transaction.
         */
        public TransactionDto postTransaction(PostTransactionCommand command) {
                // Check for idempotency - if transaction with this externalId already exists,
                // return it
                Optional<LedgerTransactionJpaEntity> existingEntity = transactionRepository
                                .findByExternalId(command.getExternalId());

                if (existingEntity.isPresent()) {
                        logger.info("Transaction with externalId {} already exists, returning existing transaction",
                                        command.getExternalId());
                        LedgerTransaction existing = transactionMapper.toDomain(existingEntity.get());
                        return toDto(existing);
                }

                // Load and lock all affected accounts
                Map<UUID, Account> accounts = loadAndLockAccounts(command);

                // Validate all accounts can accept transactions and currency matches
                validateAccounts(command, accounts);

                // Create ledger entries
                List<LedgerEntry> entries = createEntries(command, accounts);

                // Create and validate transaction
                LedgerTransaction transaction = LedgerTransaction.create(
                                command.getExternalId(),
                                command.getEventType(),
                                entries);

                // Post the transaction (validates and changes status to POSTED)
                transactionProcessor.postTransaction(transaction);

                // Persist the transaction
                LedgerTransactionJpaEntity entity = transactionMapper.toEntity(transaction);
                transactionRepository.save(entity);

                logger.info("Posted transaction {} with externalId {}",
                                transaction.getTransactionId(), command.getExternalId());

                // Save event to outbox (in same transaction - guaranteed delivery)
                outboxService.saveTransactionPostedEvent(transaction);

                return toDto(transaction);
        }

        /**
         * Reverses a posted transaction.
         * Creates a mirror transaction with opposite entry types.
         */
        public TransactionDto reverseTransaction(UUID transactionId, UUID reversalExternalId) {
                // Load original transaction
                LedgerTransactionJpaEntity originalEntity = transactionRepository.findByIdWithEntries(transactionId)
                                .orElseThrow(() -> new InvalidTransactionException(
                                                "Transaction not found: " + transactionId));

                LedgerTransaction originalTransaction = transactionMapper.toDomain(originalEntity);

                // Check for idempotency of reversal
                Optional<LedgerTransactionJpaEntity> existingReversalEntity = transactionRepository
                                .findByExternalId(reversalExternalId);

                if (existingReversalEntity.isPresent()) {
                        logger.info("Reversal with externalId {} already exists, returning existing reversal",
                                        reversalExternalId);
                        LedgerTransaction existingReversal = transactionMapper.toDomain(existingReversalEntity.get());
                        return toDto(existingReversal);
                }

                // Create reversal transaction
                LedgerTransaction reversalTransaction = transactionProcessor.createReversal(
                                originalTransaction,
                                reversalExternalId);

                // Post the reversal
                transactionProcessor.executeReversal(originalTransaction, reversalTransaction);

                // Update original transaction status
                originalEntity.setStatus(originalTransaction.getStatus());
                originalEntity.setReversalTransactionId(reversalTransaction.getTransactionId());
                transactionRepository.save(originalEntity);

                // Persist reversal transaction
                LedgerTransactionJpaEntity reversalEntity = transactionMapper.toEntity(reversalTransaction);
                transactionRepository.save(reversalEntity);

                logger.info("Reversed transaction {} with reversal transaction {}",
                                transactionId, reversalTransaction.getTransactionId());

                // Save event to outbox (in same transaction - guaranteed delivery)
                outboxService.saveTransactionReversedEvent(
                                reversalTransaction.getTransactionId(),
                                transactionId);

                return toDto(reversalTransaction);
        }

        /**
         * Retrieves a transaction by ID.
         */
        @Transactional(readOnly = true)
        public TransactionDto getTransaction(UUID transactionId) {
                LedgerTransactionJpaEntity entity = transactionRepository.findByIdWithEntries(transactionId)
                                .orElseThrow(() -> new InvalidTransactionException(
                                                "Transaction not found: " + transactionId));

                LedgerTransaction transaction = transactionMapper.toDomain(entity);
                return toDto(transaction);
        }

        /**
         * Loads and locks all accounts involved in the transaction.
         * Uses pessimistic locking to ensure serialized access.
         */
        private Map<UUID, Account> loadAndLockAccounts(PostTransactionCommand command) {
                // 1. Extrai os IDs
                // 2. Remove duplicatas (distinct) para não travar o mesmo ID duas vezes
                // 3. Ordena os UUIDs (Comparable) para garantir a ordem de travamento
                // 4. Coleta para LISTA (que mantém a ordem)
                List<UUID> sortedAccountIds = command.getEntries().stream()
                                .map(EntryCommand::getAccountId)
                                .distinct()
                                .sorted()
                                .toList(); // Java 16+ ou .collect(Collectors.toList())

                Map<UUID, Account> accounts = new HashMap<>();

                // Agora sim: iteração determinística
                for (UUID accountId : sortedAccountIds) {
                        AccountJpaEntity entity = accountRepository.findByIdWithLock(accountId)
                                        .orElseThrow(() -> new InvalidAccountException(
                                                        "Account not found: " + accountId));

                        accounts.put(accountId, accountMapper.toDomain(entity));
                }
                return accounts;
        }

        /**
         * Validates all accounts can accept transactions and currencies match.
         */
        private void validateAccounts(PostTransactionCommand command, Map<UUID, Account> accounts) {
                for (EntryCommand entryCmd : command.getEntries()) {
                        Account account = accounts.get(entryCmd.getAccountId());

                        // Validate account can accept transactions
                        account.validateCanAcceptTransaction();

                        // Validate currency matches
                        account.validateCurrency(entryCmd.getCurrency());
                }
        }

        /**
         * Creates ledger entries from command.
         */
        private List<LedgerEntry> createEntries(PostTransactionCommand command, Map<UUID, Account> accounts) {
                // We need a transaction ID for the entries, but we don't have it yet
                // The LedgerTransaction.create will assign the same transaction ID to all
                // entries
                UUID transactionId = UUID.randomUUID();
                Instant eventTime = Instant.now();

                return command.getEntries().stream()
                                .map(entryCmd -> LedgerEntry.create(
                                                transactionId,
                                                entryCmd.getAccountId(),
                                                Money.of(entryCmd.getAmount(), entryCmd.getCurrency()),
                                                entryCmd.getEntryType(),
                                                command.getEventType(),
                                                eventTime))
                                .collect(Collectors.toList());
        }

        /**
         * Converts LedgerTransaction domain model to DTO.
         */
        private TransactionDto toDto(LedgerTransaction transaction) {
                List<LedgerEntryDto> entryDtos = transaction.getEntries().stream()
                                .map(this::toEntryDto)
                                .collect(Collectors.toList());

                return new TransactionDto(
                                transaction.getTransactionId(),
                                transaction.getExternalId(),
                                transaction.getEventType(),
                                transaction.getStatus(),
                                transaction.getCreatedAt(),
                                entryDtos,
                                transaction.getReversalTransactionId().orElse(null));
        }

        /**
         * Converts LedgerEntry domain model to DTO.
         */
        private LedgerEntryDto toEntryDto(LedgerEntry entry) {
                return new LedgerEntryDto(
                                entry.getLedgerEntryId(),
                                entry.getTransactionId(),
                                entry.getAccountId(),
                                entry.getAmount().getAmount(),
                                entry.getAmount().getCurrencyCode(),
                                entry.getEntryType(),
                                entry.getEventType(),
                                entry.getEventTime(),
                                entry.getRecordedAt());
        }
}

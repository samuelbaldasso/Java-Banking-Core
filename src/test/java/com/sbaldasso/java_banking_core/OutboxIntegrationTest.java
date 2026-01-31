package com.sbaldasso.java_banking_core;

import com.sbaldasso.java_banking_core.application.command.EntryCommand;
import com.sbaldasso.java_banking_core.application.command.PostTransactionCommand;
import com.sbaldasso.java_banking_core.application.dto.AccountDto;
import com.sbaldasso.java_banking_core.application.dto.TransactionDto;
import com.sbaldasso.java_banking_core.application.service.AccountApplicationService;
import com.sbaldasso.java_banking_core.application.service.LedgerApplicationService;
import com.sbaldasso.java_banking_core.domain.valueobject.AccountType;
import com.sbaldasso.java_banking_core.domain.valueobject.EntryType;
import com.sbaldasso.java_banking_core.domain.valueobject.EventType;
import com.sbaldasso.java_banking_core.domain.valueobject.TransactionStatus;
import com.sbaldasso.java_banking_core.infrastructure.persistence.entity.OutboxEventJpaEntity;
import com.sbaldasso.java_banking_core.infrastructure.persistence.repository.OutboxEventRepository;
import com.sbaldasso.java_banking_core.infrastructure.scheduler.OutboxProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Integration test for the Transactional Outbox pattern.
 * Verifies that:
 * 1. Events are saved to outbox in the same transaction as ledger entries
 * 2. Outbox processor successfully publishes events to Kafka
 * 3. Retry logic works for failed publishing attempts
 * 4. Failed events are marked appropriately after max retries
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:outbox_testdb",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "ledger.outbox.processor.enabled=true",
        "ledger.outbox.processor.polling-interval-ms=1000",
        "ledger.outbox.processor.batch-size=10",
        "ledger.outbox.processor.max-retries=3"
})
@Transactional
class OutboxIntegrationTest {

    @Autowired
    private AccountApplicationService accountService;

    @Autowired
    private LedgerApplicationService ledgerService;

    @Autowired
    private OutboxEventRepository outboxRepository;

    @Autowired
    private OutboxProcessor outboxProcessor;

    @MockitoBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void shouldSaveEventToOutboxInSameTransactionAsLedgerEntry() {
        // Given: Two accounts
        AccountDto accountA = accountService.createAccount(AccountType.ASSET, "BRL");
        AccountDto accountB = accountService.createAccount(AccountType.LIABILITY, "BRL");

        UUID externalId = UUID.randomUUID();

        // When: Post a transaction
        PostTransactionCommand command = new PostTransactionCommand(
                externalId,
                EventType.DEPOSIT,
                List.of(
                        new EntryCommand(accountA.getAccountId(), new BigDecimal("100"), "BRL", EntryType.DEBIT),
                        new EntryCommand(accountB.getAccountId(), new BigDecimal("100"), "BRL", EntryType.CREDIT)));

        TransactionDto transaction = ledgerService.postTransaction(command);

        // Then: Transaction is posted
        assertEquals(TransactionStatus.POSTED, transaction.getStatus());

        // And: Event is saved to outbox
        List<OutboxEventJpaEntity> outboxEvents = outboxRepository.findByAggregateIdOrderByCreatedAtAsc(
                transaction.getTransactionId());

        assertEquals(1, outboxEvents.size());
        OutboxEventJpaEntity outboxEvent = outboxEvents.get(0);

        assertEquals("TRANSACTION_POSTED", outboxEvent.getEventType());
        assertEquals(transaction.getTransactionId(), outboxEvent.getAggregateId());
        assertEquals(OutboxEventJpaEntity.OutboxEventStatus.PENDING, outboxEvent.getStatus());
        assertEquals(0, outboxEvent.getRetryCount());
        assertNull(outboxEvent.getProcessedAt());
        assertNotNull(outboxEvent.getPayload());
    }

    @Test
    void shouldPublishEventToKafkaAndMarkAsProcessed() throws Exception {
        // Given: Event in outbox
        AccountDto accountA = accountService.createAccount(AccountType.ASSET, "BRL");
        AccountDto accountB = accountService.createAccount(AccountType.LIABILITY, "BRL");

        PostTransactionCommand command = new PostTransactionCommand(
                UUID.randomUUID(),
                EventType.DEPOSIT,
                List.of(
                        new EntryCommand(accountA.getAccountId(), new BigDecimal("50"), "BRL", EntryType.DEBIT),
                        new EntryCommand(accountB.getAccountId(), new BigDecimal("50"), "BRL", EntryType.CREDIT)));

        TransactionDto transaction = ledgerService.postTransaction(command);

        // Mock successful Kafka publish
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        // When: Outbox processor runs
        outboxProcessor.processOutboxEvents();

        // Then: Event is published to Kafka
        verify(kafkaTemplate, times(1)).send(anyString(), anyString(), any());

        // And: Event is marked as processed
        List<OutboxEventJpaEntity> outboxEvents = outboxRepository.findByAggregateIdOrderByCreatedAtAsc(
                transaction.getTransactionId());

        assertEquals(1, outboxEvents.size());
        OutboxEventJpaEntity processedEvent = outboxEvents.get(0);

        assertEquals(OutboxEventJpaEntity.OutboxEventStatus.PROCESSED, processedEvent.getStatus());
        assertNotNull(processedEvent.getProcessedAt());
    }

    @Test
    void shouldRetryFailedPublishingAttempts() throws Exception {
        // Given: Event in outbox
        AccountDto accountA = accountService.createAccount(AccountType.ASSET, "USD");
        AccountDto accountB = accountService.createAccount(AccountType.LIABILITY, "USD");

        PostTransactionCommand command = new PostTransactionCommand(
                UUID.randomUUID(),
                EventType.TRANSFER,
                List.of(
                        new EntryCommand(accountA.getAccountId(), new BigDecimal("25"), "USD", EntryType.CREDIT),
                        new EntryCommand(accountB.getAccountId(), new BigDecimal("25"), "USD", EntryType.DEBIT)));

        TransactionDto transaction = ledgerService.postTransaction(command);
        UUID transactionId = transaction.getTransactionId();

        // Mock Kafka failure on first attempt
        CompletableFuture<SendResult<String, Object>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka unavailable"));

        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(failedFuture);

        // When: First processing attempt (fails)
        outboxProcessor.processOutboxEvents();

        // Then: Event has retry count incremented
        List<OutboxEventJpaEntity> outboxEvents = outboxRepository.findByAggregateIdOrderByCreatedAtAsc(transactionId);
        OutboxEventJpaEntity failedEvent = outboxEvents.get(0);

        assertEquals(1, failedEvent.getRetryCount());
        assertEquals(OutboxEventJpaEntity.OutboxEventStatus.PENDING, failedEvent.getStatus());
        assertNotNull(failedEvent.getLastError());
        assertTrue(failedEvent.getLastError().contains("Kafka unavailable"));

        // Given: Kafka is now available
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        // When: Second processing attempt (succeeds)
        outboxProcessor.processOutboxEvents();

        // Then: Event is successfully processed
        outboxEvents = outboxRepository.findByAggregateIdOrderByCreatedAtAsc(transactionId);
        OutboxEventJpaEntity processedEvent = outboxEvents.get(0);

        assertEquals(OutboxEventJpaEntity.OutboxEventStatus.PROCESSED, processedEvent.getStatus());
        assertNotNull(processedEvent.getProcessedAt());
    }

    @Test
    void shouldMarkEventAsFailedAfterMaxRetries() throws Exception {
        // Given: Event in outbox
        AccountDto accountA = accountService.createAccount(AccountType.ASSET, "EUR");
        AccountDto accountB = accountService.createAccount(AccountType.LIABILITY, "EUR");

        PostTransactionCommand command = new PostTransactionCommand(
                UUID.randomUUID(),
                EventType.WITHDRAWAL,
                List.of(
                        new EntryCommand(accountA.getAccountId(), new BigDecimal("75"), "EUR", EntryType.CREDIT),
                        new EntryCommand(accountB.getAccountId(), new BigDecimal("75"), "EUR", EntryType.DEBIT)));

        TransactionDto transaction = ledgerService.postTransaction(command);
        UUID transactionId = transaction.getTransactionId();

        // Mock persistent Kafka failure
        CompletableFuture<SendResult<String, Object>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Persistent failure"));

        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(failedFuture);

        // When: Process multiple times (exceeding max retries = 3)
        for (int i = 0; i < 4; i++) {
            outboxProcessor.processOutboxEvents();
        }

        // Then: Event is marked as FAILED
        List<OutboxEventJpaEntity> outboxEvents = outboxRepository.findByAggregateIdOrderByCreatedAtAsc(transactionId);
        OutboxEventJpaEntity failedEvent = outboxEvents.get(0);

        assertEquals(OutboxEventJpaEntity.OutboxEventStatus.FAILED, failedEvent.getStatus());
        assertTrue(failedEvent.getRetryCount() >= 3);
        assertNotNull(failedEvent.getLastError());
    }

    @Test
    void shouldHandleTransactionReversalEvents() {
        // Given: Posted transaction
        AccountDto accountA = accountService.createAccount(AccountType.ASSET, "BRL");
        AccountDto accountB = accountService.createAccount(AccountType.LIABILITY, "BRL");

        PostTransactionCommand command = new PostTransactionCommand(
                UUID.randomUUID(),
                EventType.DEPOSIT,
                List.of(
                        new EntryCommand(accountA.getAccountId(), new BigDecimal("200"), "BRL", EntryType.DEBIT),
                        new EntryCommand(accountB.getAccountId(), new BigDecimal("200"), "BRL", EntryType.CREDIT)));

        TransactionDto transaction = ledgerService.postTransaction(command);

        // When: Reverse the transaction
        UUID reversalExternalId = UUID.randomUUID();
        TransactionDto reversal = ledgerService.reverseTransaction(
                transaction.getTransactionId(),
                reversalExternalId);

        // Then: Reversal event is saved to outbox
        List<OutboxEventJpaEntity> reversalEvents = outboxRepository.findByAggregateIdOrderByCreatedAtAsc(
                reversal.getTransactionId());

        assertEquals(1, reversalEvents.size());
        OutboxEventJpaEntity reversalEvent = reversalEvents.get(0);

        assertEquals("TRANSACTION_REVERSED", reversalEvent.getEventType());
        assertEquals(reversal.getTransactionId(), reversalEvent.getAggregateId());
        assertEquals(OutboxEventJpaEntity.OutboxEventStatus.PENDING, reversalEvent.getStatus());
    }

    @Test
    void shouldProcessMultipleEventsInBatch() {
        // Given: Multiple transactions creating multiple outbox events
        AccountDto accountA = accountService.createAccount(AccountType.ASSET, "BRL");
        AccountDto accountB = accountService.createAccount(AccountType.LIABILITY, "BRL");

        for (int i = 0; i < 5; i++) {
            PostTransactionCommand command = new PostTransactionCommand(
                    UUID.randomUUID(),
                    EventType.DEPOSIT,
                    List.of(
                            new EntryCommand(accountA.getAccountId(), new BigDecimal("10"), "BRL", EntryType.DEBIT),
                            new EntryCommand(accountB.getAccountId(), new BigDecimal("10"), "BRL", EntryType.CREDIT)));
            ledgerService.postTransaction(command);
        }

        // When: Kafka is available
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        // And: Processor runs
        outboxProcessor.processOutboxEvents();

        // Then: All events are processed
        long pendingCount = outboxRepository.countByStatus(OutboxEventJpaEntity.OutboxEventStatus.PENDING);
        long processedCount = outboxRepository.countByStatus(OutboxEventJpaEntity.OutboxEventStatus.PROCESSED);

        assertEquals(0, pendingCount);
        assertEquals(5, processedCount);
    }
}

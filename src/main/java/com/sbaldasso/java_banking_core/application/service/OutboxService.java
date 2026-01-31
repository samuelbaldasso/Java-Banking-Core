package com.sbaldasso.java_banking_core.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sbaldasso.java_banking_core.domain.model.LedgerTransaction;
import com.sbaldasso.java_banking_core.infrastructure.event.TransactionPostedEvent;
import com.sbaldasso.java_banking_core.infrastructure.event.TransactionReversedEvent;
import com.sbaldasso.java_banking_core.infrastructure.persistence.entity.OutboxEventJpaEntity;
import com.sbaldasso.java_banking_core.infrastructure.persistence.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for persisting domain events to the transactional outbox.
 * Events are saved in the same transaction as business data, guaranteeing
 * delivery.
 */
@Service
public class OutboxService {

    private static final Logger logger = LoggerFactory.getLogger(OutboxService.class);

    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxEventRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Saves a TransactionPosted event to the outbox.
     * This method is called within the same transaction as the ledger persistence.
     */
    public void saveTransactionPostedEvent(LedgerTransaction transaction) {
        TransactionPostedEvent event = new TransactionPostedEvent(
                transaction.getTransactionId(),
                transaction.getExternalId(),
                transaction.getEventType(),
                transaction.getEntries().stream()
                        .map(entry -> new TransactionPostedEvent.EntryInfo(
                                entry.getAccountId(),
                                entry.getAmount().getAmount().toPlainString(),
                                entry.getAmount().getCurrencyCode(),
                                entry.getEntryType().name()))
                        .collect(Collectors.toList()),
                Instant.now());

        saveEvent(
                transaction.getTransactionId(),
                "TRANSACTION_POSTED",
                event);

        logger.info("Saved TRANSACTION_POSTED event to outbox for transaction {}",
                transaction.getTransactionId());
    }

    /**
     * Saves a TransactionReversed event to the outbox.
     * This method is called within the same transaction as the reversal
     * persistence.
     */
    public void saveTransactionReversedEvent(UUID reversalTransactionId, UUID originalTransactionId) {
        TransactionReversedEvent event = new TransactionReversedEvent(
                reversalTransactionId,
                originalTransactionId,
                Instant.now());

        saveEvent(
                reversalTransactionId,
                "TRANSACTION_REVERSED",
                event);

        logger.info("Saved TRANSACTION_REVERSED event to outbox for reversal transaction {}",
                reversalTransactionId);
    }

    /**
     * Generic method to save any event to the outbox.
     * Serializes the event payload to JSON and persists it.
     */
    private void saveEvent(UUID aggregateId, String eventType, Object eventPayload) {
        try {
            String payload = objectMapper.writeValueAsString(eventPayload);

            OutboxEventJpaEntity outboxEvent = new OutboxEventJpaEntity(
                    aggregateId,
                    eventType,
                    payload);

            outboxRepository.save(outboxEvent);

            logger.debug("Persisted outbox event: id={}, type={}, aggregateId={}",
                    outboxEvent.getId(), eventType, aggregateId);

        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize event payload for type {}: {}",
                    eventType, e.getMessage(), e);
            throw new RuntimeException("Failed to serialize event payload", e);
        }
    }
}

package com.sbaldasso.java_banking_core.infrastructure.event;

import com.sbaldasso.java_banking_core.domain.model.LedgerTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Publisher for domain events to Kafka.
 * Publishes transaction lifecycle events asynchronously.
 */
@Service
public class DomainEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(DomainEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${ledger.kafka.topics.transaction-posted}")
    private String transactionPostedTopic;

    @Value("${ledger.kafka.topics.transaction-reversed}")
    private String transactionReversedTopic;

    public DomainEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publishes a TransactionPostedEvent when a transaction is posted.
     */
    public void publishTransactionPosted(LedgerTransaction transaction) {
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

        publishEvent(transactionPostedTopic, transaction.getTransactionId().toString(), event);
    }

    /**
     * Publishes a TransactionReversedEvent when a transaction is reversed.
     */
    public void publishTransactionReversed(UUID reversalTransactionId, UUID originalTransactionId) {
        TransactionReversedEvent event = new TransactionReversedEvent(
                reversalTransactionId,
                originalTransactionId,
                Instant.now());

        publishEvent(transactionReversedTopic, reversalTransactionId.toString(), event);
    }

    /**
     * Generic method to publish an event to Kafka with error handling.
     */
    private void publishEvent(String topic, String key, Object event) {
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                logger.info("Successfully published event to topic '{}' with key '{}'", topic, key);
            } else {
                logger.error("Failed to publish event to topic '{}' with key '{}': {}",
                        topic, key, ex.getMessage(), ex);
            }
        });
    }
}

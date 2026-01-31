package com.sbaldasso.java_banking_core.infrastructure.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sbaldasso.java_banking_core.infrastructure.event.TransactionPostedEvent;
import com.sbaldasso.java_banking_core.infrastructure.event.TransactionReversedEvent;
import com.sbaldasso.java_banking_core.infrastructure.persistence.entity.OutboxEventJpaEntity;
import com.sbaldasso.java_banking_core.infrastructure.persistence.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Outbox Processor - Polls the outbox table and publishes events to Kafka.
 * Implements the relay component of the Transactional Outbox pattern.
 * 
 * This ensures:
 * - At-least-once delivery (events are retried on failure)
 * - Eventual consistency (events are eventually published)
 * - Fault tolerance (survives application crashes)
 */
@Component
public class OutboxProcessor {

    private static final Logger logger = LoggerFactory.getLogger(OutboxProcessor.class);

    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ledger.outbox.processor.batch-size:100}")
    private int batchSize;

    @Value("${ledger.outbox.processor.max-retries:5}")
    private int maxRetries;

    @Value("${ledger.kafka.topics.transaction-posted}")
    private String transactionPostedTopic;

    @Value("${ledger.kafka.topics.transaction-reversed}")
    private String transactionReversedTopic;

    public OutboxProcessor(
            OutboxEventRepository outboxRepository,
            KafkaTemplate<String, Object> kafkaTemplate,
            ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Polls the outbox table for pending events and publishes them to Kafka.
     * Runs periodically based on the configured polling interval.
     */
    @Scheduled(fixedDelayString = "${ledger.outbox.processor.polling-interval-ms:5000}")
    @Transactional
    public void processOutboxEvents() {
        try {
            List<OutboxEventJpaEntity> pendingEvents = outboxRepository.findPendingEvents(batchSize);

            if (pendingEvents.isEmpty()) {
                logger.debug("No pending outbox events to process");
                return;
            }

            logger.info("Processing {} pending outbox events", pendingEvents.size());

            for (OutboxEventJpaEntity event : pendingEvents) {
                processEvent(event);
            }

            logger.info("Completed processing {} outbox events", pendingEvents.size());

        } catch (Exception e) {
            logger.error("Error during outbox processing: {}", e.getMessage(), e);
        }
    }

    /**
     * Processes a single outbox event.
     * Publishes to Kafka and updates the event status.
     */
    private void processEvent(OutboxEventJpaEntity event) {
        try {
            logger.debug("Processing outbox event: id={}, type={}, aggregateId={}",
                    event.getId(), event.getEventType(), event.getAggregateId());

            // Publish to Kafka based on event type
            publishToKafka(event);

            // Mark as processed
            event.markAsProcessed();
            outboxRepository.save(event);

            logger.info("Successfully processed outbox event: id={}, type={}",
                    event.getId(), event.getEventType());

        } catch (Exception e) {
            handleProcessingFailure(event, e);
        }
    }

    /**
     * Publishes the event to the appropriate Kafka topic.
     */
    private void publishToKafka(OutboxEventJpaEntity event) throws Exception {
        String topic = getTopicForEventType(event.getEventType());
        String key = event.getAggregateId().toString();

        // Deserialize the payload based on event type
        Object eventPayload = deserializePayload(event);

        // Publish to Kafka synchronously to ensure delivery before marking as processed
        kafkaTemplate.send(topic, key, eventPayload).get();

        logger.debug("Published event to Kafka: topic={}, key={}", topic, key);
    }

    /**
     * Deserializes the JSON payload to the appropriate event type.
     */
    private Object deserializePayload(OutboxEventJpaEntity event) throws Exception {
        return switch (event.getEventType()) {
            case "TRANSACTION_POSTED" ->
                objectMapper.readValue(event.getPayload(), TransactionPostedEvent.class);
            case "TRANSACTION_REVERSED" ->
                objectMapper.readValue(event.getPayload(), TransactionReversedEvent.class);
            default -> throw new IllegalArgumentException(
                    "Unknown event type: " + event.getEventType());
        };
    }

    /**
     * Determines the Kafka topic for the given event type.
     */
    private String getTopicForEventType(String eventType) {
        return switch (eventType) {
            case "TRANSACTION_POSTED" -> transactionPostedTopic;
            case "TRANSACTION_REVERSED" -> transactionReversedTopic;
            default -> throw new IllegalArgumentException(
                    "Unknown event type: " + eventType);
        };
    }

    /**
     * Handles failures during event processing.
     * Implements retry logic with max retry limit.
     */
    private void handleProcessingFailure(OutboxEventJpaEntity event, Exception exception) {
        logger.error("Failed to process outbox event: id={}, type={}, attempt={}, error={}",
                event.getId(), event.getEventType(), event.getRetryCount() + 1,
                exception.getMessage());

        event.recordFailure(exception.getMessage());

        // Check if max retries exceeded
        if (event.getRetryCount() >= maxRetries) {
            logger.error("Max retries exceeded for outbox event: id={}, marking as FAILED",
                    event.getId());
            event.markAsFailed();
        }

        outboxRepository.save(event);
    }

    /**
     * Monitors outbox health by logging statistics.
     * Runs every minute to provide visibility into outbox state.
     */
    @Scheduled(fixedRate = 60000) // Every minute
    @Transactional(readOnly = true)
    public void monitorOutboxHealth() {
        try {
            long pendingCount = outboxRepository.countByStatus(
                    OutboxEventJpaEntity.OutboxEventStatus.PENDING);
            long failedCount = outboxRepository.countByStatus(
                    OutboxEventJpaEntity.OutboxEventStatus.FAILED);

            if (pendingCount > 0 || failedCount > 0) {
                logger.info("Outbox status - Pending: {}, Failed: {}",
                        pendingCount, failedCount);
            }

            if (failedCount > 0) {
                logger.warn("Outbox has {} FAILED events requiring manual intervention",
                        failedCount);
            }

        } catch (Exception e) {
            logger.error("Error monitoring outbox health: {}", e.getMessage(), e);
        }
    }
}

package com.sbaldasso.java_banking_core.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA Entity for the Transactional Outbox pattern.
 * Stores domain events in the same transaction as business data to guarantee
 * event delivery.
 */
@Entity
@Table(name = "outbox_events")
public class OutboxEventJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private OutboxEventStatus status;

    public OutboxEventJpaEntity() {
        this.id = UUID.randomUUID();
        this.createdAt = Instant.now();
        this.status = OutboxEventStatus.PENDING;
        this.retryCount = 0;
    }

    public OutboxEventJpaEntity(UUID aggregateId, String eventType, String payload) {
        this();
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(UUID aggregateId) {
        this.aggregateId = aggregateId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public OutboxEventStatus getStatus() {
        return status;
    }

    public void setStatus(OutboxEventStatus status) {
        this.status = status;
    }

    /**
     * Marks the event as successfully processed.
     */
    public void markAsProcessed() {
        this.status = OutboxEventStatus.PROCESSED;
        this.processedAt = Instant.now();
    }

    /**
     * Records a failed processing attempt.
     */
    public void recordFailure(String errorMessage) {
        this.retryCount++;
        this.lastError = errorMessage;
    }

    /**
     * Marks the event as permanently failed after max retries.
     */
    public void markAsFailed() {
        this.status = OutboxEventStatus.FAILED;
    }

    public enum OutboxEventStatus {
        PENDING,
        PROCESSED,
        FAILED
    }
}

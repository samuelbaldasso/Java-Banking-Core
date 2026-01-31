package com.sbaldasso.java_banking_core.infrastructure.persistence.repository;

import com.sbaldasso.java_banking_core.infrastructure.persistence.entity.OutboxEventJpaEntity;
import com.sbaldasso.java_banking_core.infrastructure.persistence.entity.OutboxEventJpaEntity.OutboxEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for accessing outbox events.
 * Provides queries for the outbox processor to retrieve and process events.
 */
@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEventJpaEntity, UUID> {

    /**
     * Finds pending events ordered by creation time (FIFO processing).
     * Limits results to prevent overwhelming the processor.
     *
     * @param limit Maximum number of events to retrieve
     * @return List of pending events
     */
    @Query("SELECT e FROM OutboxEventJpaEntity e " +
            "WHERE e.status = 'PENDING' " +
            "ORDER BY e.createdAt ASC " +
            "LIMIT :limit")
    List<OutboxEventJpaEntity> findPendingEvents(@Param("limit") int limit);

    /**
     * Finds failed events that are eligible for retry.
     * Events are retried if they haven't exceeded the max retry count.
     *
     * @param maxRetries Maximum number of retry attempts
     * @param limit      Maximum number of events to retrieve
     * @return List of failed events eligible for retry
     */
    @Query("SELECT e FROM OutboxEventJpaEntity e " +
            "WHERE e.status = 'PENDING' " +
            "AND e.retryCount > 0 " +
            "AND e.retryCount < :maxRetries " +
            "ORDER BY e.createdAt ASC " +
            "LIMIT :limit")
    List<OutboxEventJpaEntity> findEventsForRetry(
            @Param("maxRetries") int maxRetries,
            @Param("limit") int limit);

    /**
     * Counts pending events for monitoring.
     */
    long countByStatus(OutboxEventStatus status);

    /**
     * Finds all events for a specific aggregate (useful for debugging).
     */
    List<OutboxEventJpaEntity> findByAggregateIdOrderByCreatedAtAsc(UUID aggregateId);
}

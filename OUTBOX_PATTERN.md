# Transactional Outbox Pattern

## Overview

This system implements the **Transactional Outbox Pattern** to guarantee event delivery to Kafka. This pattern ensures that domain events are never lost, even if the application crashes after committing database transactions.

## The Problem

In a distributed system, we need to:
1. Save data to the database
2. Publish events to Kafka

If we do these as separate operations, we risk **inconsistency**:
- Database commits successfully
- Application crashes before publishing to Kafka
- ❌ **Event is lost forever**

## The Solution

The Transactional Outbox pattern solves this by:

1. **Writing events to a database table (`outbox_events`) in the same transaction as business data**
   - Both ledger entries AND events are saved atomically
   - If the transaction fails, nothing is saved
   - If it succeeds, both are guaranteed to be persisted

2. **Using a separate process to read from the outbox and publish to Kafka**
   - The `OutboxProcessor` polls the outbox table
   - Publishes pending events to Kafka
   - Marks successfully published events as processed
   - Retries failed events with exponential backoff

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    LedgerApplicationService                 │
│                                                             │
│  1. Save Ledger Entry                                      │
│  2. Save Event to Outbox  ◄── Same Transaction!           │
│     (OutboxService)                                         │
└─────────────────────────────────────────────────────────────┘
                            │
                            │ Commit
                            ▼
                 ┌─────────────────────┐
                 │  PostgreSQL         │
                 │                     │
                 │  • ledger_entries   │
                 │  • outbox_events    │
                 └─────────────────────┘
                            │
                            │ Poll every 5s
                            ▼
                  ┌─────────────────────┐
                  │  OutboxProcessor    │
                  │  (Scheduled Job)    │
                  │                     │
                  │  • Read pending     │
                  │  • Publish to Kafka │
                  │  • Mark processed   │
                  │  • Retry on failure │
                  └─────────────────────┘
                            │
                            │ Publish
                            ▼
                    ┌───────────────┐
                    │    Kafka      │
                    │               │
                    │  • txn-posted │
                    │  • txn-reversed│
                    └───────────────┘
```

## Components

### 1. OutboxEventJpaEntity

Database entity representing events in the outbox:

```java
@Entity
@Table(name = "outbox_events")
public class OutboxEventJpaEntity {
    UUID id;
    UUID aggregateId;        // Transaction ID
    String eventType;        // TRANSACTION_POSTED, TRANSACTION_REVERSED
    String payload;          // JSON serialized event
    Instant createdAt;
    Instant processedAt;     // null until successfully published
    Integer retryCount;
    String lastError;
    OutboxEventStatus status; // PENDING, PROCESSED, FAILED
}
```

### 2. OutboxService

Application service that saves events to the outbox:

```java
@Service
public class OutboxService {
    public void saveTransactionPostedEvent(LedgerTransaction transaction) {
        // Serialize event to JSON
        // Save to outbox table
        // This happens in the SAME transaction as ledger persistence!
    }
}
```

### 3. OutboxProcessor

Scheduled job that publishes events to Kafka:

```java
@Component
public class OutboxProcessor {
    @Scheduled(fixedDelayString = "${ledger.outbox.processor.polling-interval-ms}")
    @Transactional
    public void processOutboxEvents() {
        // 1. Poll for pending events
        // 2. Publish to Kafka
        // 3. Mark as processed or retry on failure
    }
}
```

## Configuration

In `application.yaml`:

```yaml
ledger:
  outbox:
    processor:
      enabled: true
      polling-interval-ms: 5000  # Poll every 5 seconds
      batch-size: 100            # Process up to 100 events per batch
      max-retries: 5             # Max retry attempts before FAILED
      retry-backoff-ms: 1000     # Wait between retries
```

## Delivery Guarantees

### ✅ At-Least-Once Delivery

- Events are **guaranteed to be published** (eventually)
- If Kafka is down, events remain in the outbox and are retried
- Events may be published multiple times if the processor crashes after publishing but before marking as processed

### ⚠️ Idempotency Required

Kafka consumers **MUST** be idempotent:
- Use the `transactionId` as a deduplication key
- Check if you've already processed an event before applying changes

## Monitoring

### Health Metrics

The `OutboxProcessor` logs statistics every minute:

```
Outbox status - Pending: 0, Failed: 0
```

### Failed Events

Events that fail after max retries are marked as `FAILED`:
- Check the `last_error` column for failure reason
- Requires manual intervention to republish

Query failed events:
```sql
SELECT * FROM outbox_events 
WHERE status = 'FAILED' 
ORDER BY created_at DESC;
```

## Testing

See `OutboxIntegrationTest.java` for comprehensive tests covering:
- ✅ Events saved in same transaction
- ✅ Successful publishing to Kafka
- ✅ Retry logic on failures
- ✅ Failed event handling
- ✅ Batch processing

## Advantages

1. **Guaranteed Delivery**: Events are never lost
2. **Consistency**: Ledger and events are always in sync
3. **Resilience**: Survives application crashes
4. **Decoupling**: Event publishing doesn't slow down business logic
5. **Observability**: Failed events are visible in the database

## Trade-offs

1. **Latency**: Events are published asynchronously (5-second delay by default)
2. **At-Least-Once**: Consumers must handle duplicate events
3. **Database Load**: Additional table to maintain and poll
4. **Complexity**: More moving parts than direct publishing

## Future Enhancements

### Debezium (Change Data Capture)

For high-throughput systems, consider migrating to **Debezium**:
- Reads database transaction log directly
- Publishes events in real-time
- Lower latency than polling
- Same outbox table structure (no code changes needed!)

### Distributed Locking

For multi-instance deployments, add distributed locking:
- Use ShedLock or similar
- Prevents multiple processors from processing the same events
- Ensures exactly-once processing across instances

## References

- [Microservices Pattern: Transactional Outbox](https://microservices.io/patterns/data/transactional-outbox.html)
- [Debezium Outbox Event Router](https://debezium.io/documentation/reference/transformations/outbox-event-router.html)

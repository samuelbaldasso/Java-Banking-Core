package com.sbaldasso.java_banking_core.infrastructure.persistence.mapper;

import com.sbaldasso.java_banking_core.domain.model.LedgerEntry;
import com.sbaldasso.java_banking_core.domain.valueobject.Money;
import com.sbaldasso.java_banking_core.infrastructure.persistence.entity.LedgerEntryJpaEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper between LedgerEntry domain model and LedgerEntryJpaEntity.
 */
@Component
public class LedgerEntryMapper {

    /**
     * Converts domain LedgerEntry to JPA entity.
     */
    public LedgerEntryJpaEntity toEntity(LedgerEntry entry) {
        return new LedgerEntryJpaEntity(
                entry.getLedgerEntryId(),
                entry.getAccountId(),
                entry.getAmount().getAmount(),
                entry.getAmount().getCurrencyCode(),
                entry.getEntryType(),
                entry.getEventType(),
                entry.getEventTime(),
                entry.getRecordedAt());
    }

    /**
     * Converts JPA entity to domain LedgerEntry.
     */
    public LedgerEntry toDomain(LedgerEntryJpaEntity entity) {
        Money amount = Money.of(entity.getAmount(), entity.getCurrency());

        return LedgerEntry.create(
                entity.getLedgerEntryId(),
                entity.getAccountId(),
                amount,
                entity.getEntryType(),
                entity.getEventType(),
                entity.getEventTime());
    }
}

package com.sbaldasso.java_banking_core.infrastructure.persistence.mapper;

import com.sbaldasso.java_banking_core.domain.model.LedgerEntry;
import com.sbaldasso.java_banking_core.domain.model.LedgerTransaction;
import com.sbaldasso.java_banking_core.infrastructure.persistence.entity.LedgerEntryJpaEntity;
import com.sbaldasso.java_banking_core.infrastructure.persistence.entity.LedgerTransactionJpaEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper between LedgerTransaction domain model and Ledger
 * TransactionJpaEntity.
 */
@Component
public class LedgerTransactionMapper {

    private final LedgerEntryMapper entryMapper;

    public LedgerTransactionMapper(LedgerEntryMapper entryMapper) {
        this.entryMapper = entryMapper;
    }

    /**
     * Converts domain LedgerTransaction to JPA entity with entries.
     */
    public LedgerTransactionJpaEntity toEntity(LedgerTransaction transaction) {
        LedgerTransactionJpaEntity entity = new LedgerTransactionJpaEntity(
                transaction.getTransactionId(),
                transaction.getExternalId(),
                transaction.getEventType(),
                transaction.getStatus(),
                transaction.getCreatedAt(),
                transaction.getReversalTransactionId().orElse(null));

        // Convert and add entries
        List<LedgerEntryJpaEntity> entryEntities = transaction.getEntries().stream()
                .map(entryMapper::toEntity)
                .collect(Collectors.toList());

        entryEntities.forEach(entity::addEntry);

        return entity;
    }

    /**
     * Converts JPA entity to domain LedgerTransaction with entries.
     */
    public LedgerTransaction toDomain(LedgerTransactionJpaEntity entity) {
        List<LedgerEntry> entries = entity.getEntries().stream()
                .map(entryMapper::toDomain)
                .collect(Collectors.toList());

        return LedgerTransaction.create(
                entity.getExternalId(),
                entity.getEventType(),
                entries);
    }
}

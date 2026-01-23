package com.sbaldasso.java_banking_core.infrastructure.persistence.mapper;

import com.sbaldasso.java_banking_core.domain.model.BalanceSnapshot;
import com.sbaldasso.java_banking_core.domain.valueobject.Money;
import com.sbaldasso.java_banking_core.infrastructure.persistence.entity.BalanceSnapshotJpaEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper between BalanceSnapshot domain model and BalanceSnapshotJpaEntity.
 */
@Component
public class BalanceSnapshotMapper {

    /**
     * Converts JPA entity to domain model.
     */
    public BalanceSnapshot toDomain(BalanceSnapshotJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        Money balance = Money.of(entity.getBalanceAmount(), entity.getBalanceCurrency());

        // Use package-private constructor for reconstitution
        return BalanceSnapshot.create(
                entity.getAccountId(),
                balance,
                entity.getSnapshotTime(),
                entity.getLastEntryId());
    }

    /**
     * Converts domain model to JPA entity.
     */
    public BalanceSnapshotJpaEntity toEntity(BalanceSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }

        return new BalanceSnapshotJpaEntity(
                snapshot.getSnapshotId(),
                snapshot.getAccountId(),
                snapshot.getBalance().getAmount(),
                snapshot.getBalance().getCurrencyCode(),
                snapshot.getSnapshotTime(),
                snapshot.getLastEntryId(),
                snapshot.getCreatedAt());
    }
}

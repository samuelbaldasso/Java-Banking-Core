package com.sbaldasso.java_banking_core.infrastructure.persistence.repository;

import com.sbaldasso.java_banking_core.infrastructure.persistence.entity.LedgerTransactionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for LedgerTransaction entities.
 * Supports idempotency via externalId lookup.
 */
@Repository
public interface LedgerTransactionJpaRepository extends JpaRepository<LedgerTransactionJpaEntity, UUID> {

    /**
     * Finds a transaction by its external ID (for idempotency).
     * 
     * @param externalId The external ID
     * @return Optional containing the transaction
     */
    Optional<LedgerTransactionJpaEntity> findByExternalId(UUID externalId);

    /**
     * Checks if a transaction exists with the given external ID.
     * Useful for idempotency checks without loading the full entity.
     * 
     * @param externalId The external ID
     * @return true if exists, false otherwise
     */
    boolean existsByExternalId(UUID externalId);

    /**
     * Finds a transaction with its entries eagerly loaded.
     * 
     * @param transactionId The transaction ID
     * @return Optional containing the transaction with entries
     */
    @Query("SELECT t FROM LedgerTransactionJpaEntity t LEFT JOIN FETCH t.entries WHERE t.transactionId = :transactionId")
    Optional<LedgerTransactionJpaEntity> findByIdWithEntries(@Param("transactionId") UUID transactionId);
}

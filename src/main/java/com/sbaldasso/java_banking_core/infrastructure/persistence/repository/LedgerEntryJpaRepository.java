package com.sbaldasso.java_banking_core.infrastructure.persistence.repository;

import com.sbaldasso.java_banking_core.infrastructure.persistence.entity.LedgerEntryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for LedgerEntry entities.
 * Supports queries for balance calculation and ledger reports.
 */
@Repository
public interface LedgerEntryJpaRepository extends JpaRepository<LedgerEntryJpaEntity, UUID> {

        /**
         * Finds all POSTED entries for an account, ordered by event time.
         * Used for balance calculation.
         * 
         * @param accountId The account ID
         * @return List of posted entries
         */
        @Query("""
                        SELECT e FROM LedgerEntryJpaEntity e
                        WHERE e.accountId = :accountId
                        AND e.transaction.status = 'POSTED'
                        ORDER BY e.eventTime ASC
                        """)
        List<LedgerEntryJpaEntity> findPostedEntriesByAccount(@Param("accountId") UUID accountId);

        /**
         * Finds all POSTED entries for an account up to a specific time.
         * Used for historical balance calculation.
         * 
         * @param accountId The account ID
         * @param asOfTime  The cutoff time
         * @return List of posted entries
         */
        @Query("""
                        SELECT e FROM LedgerEntryJpaEntity e
                        WHERE e.accountId = :accountId
                        AND e.transaction.status = 'POSTED'
                        AND e.eventTime <= :asOfTime
                        ORDER BY e.eventTime ASC
                        """)
        List<LedgerEntryJpaEntity> findPostedEntriesByAccountAsOf(
                        @Param("accountId") UUID accountId,
                        @Param("asOfTime") Instant asOfTime);

        /**
         * Finds all entries for a transaction.
         * 
         * @param transactionId The transaction ID
         * @return List of entries
         */
        @Query("SELECT e FROM LedgerEntryJpaEntity e WHERE e.transaction.transactionId = :transactionId")
        List<LedgerEntryJpaEntity> findByTransactionId(@Param("transactionId") UUID transactionId);

        /**
         * Finds all POSTED entries within a time range.
         * Used for ledger reports.
         * 
         * @param startTime Start of time range
         * @param endTime   End of time range
         * @return List of posted entries
         */
        @Query("""
                        SELECT e FROM LedgerEntryJpaEntity e
                        WHERE e.transaction.status = 'POSTED'
                        AND e.eventTime BETWEEN :startTime AND :endTime
                        ORDER BY e.eventTime ASC
                        """)
        List<LedgerEntryJpaEntity> findPostedEntriesBetween(
                        @Param("startTime") Instant startTime,
                        @Param("endTime") Instant endTime);
}

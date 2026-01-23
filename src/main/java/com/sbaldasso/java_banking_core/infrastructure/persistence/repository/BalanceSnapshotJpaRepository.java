package com.sbaldasso.java_banking_core.infrastructure.persistence.repository;

import com.sbaldasso.java_banking_core.infrastructure.persistence.entity.BalanceSnapshotJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for BalanceSnapshot entities.
 * Supports queries for snapshot-based balance calculation.
 */
@Repository
public interface BalanceSnapshotJpaRepository extends JpaRepository<BalanceSnapshotJpaEntity, UUID> {

    /**
     * Finds the most recent snapshot for an account.
     * Used for current balance calculation.
     * 
     * @param accountId The account ID
     * @return Optional containing the latest snapshot, or empty if no snapshots
     *         exist
     */
    @Query("""
            SELECT s FROM BalanceSnapshotJpaEntity s
            WHERE s.accountId = :accountId
            ORDER BY s.snapshotTime DESC
            LIMIT 1
            """)
    Optional<BalanceSnapshotJpaEntity> findLatestSnapshotByAccount(@Param("accountId") UUID accountId);

    /**
     * Finds the most recent snapshot for an account before a specific time.
     * Used for historical balance calculation.
     * 
     * @param accountId The account ID
     * @param time      The cutoff time
     * @return Optional containing the latest snapshot before the time, or empty if
     *         none exist
     */
    @Query("""
            SELECT s FROM BalanceSnapshotJpaEntity s
            WHERE s.accountId = :accountId
            AND s.snapshotTime <= :time
            ORDER BY s.snapshotTime DESC
            LIMIT 1
            """)
    Optional<BalanceSnapshotJpaEntity> findLatestSnapshotByAccountBeforeTime(
            @Param("accountId") UUID accountId,
            @Param("time") Instant time);

    /**
     * Finds a specific snapshot by account and exact snapshot time.
     * 
     * @param accountId    The account ID
     * @param snapshotTime The exact snapshot time
     * @return Optional containing the snapshot, or empty if not found
     */
    Optional<BalanceSnapshotJpaEntity> findByAccountIdAndSnapshotTime(
            UUID accountId,
            Instant snapshotTime);

    /**
     * Finds all snapshots for an account, ordered by time descending.
     * Useful for auditing and analysis.
     * 
     * @param accountId The account ID
     * @return List of snapshots
     */
    @Query("""
            SELECT s FROM BalanceSnapshotJpaEntity s
            WHERE s.accountId = :accountId
            ORDER BY s.snapshotTime DESC
            """)
    List<BalanceSnapshotJpaEntity> findAllByAccountIdOrderBySnapshotTimeDesc(@Param("accountId") UUID accountId);

    /**
     * Checks if a snapshot exists for an account at a specific time.
     * Used to prevent duplicate snapshot creation.
     * 
     * @param accountId    The account ID
     * @param snapshotTime The snapshot time
     * @return true if snapshot exists, false otherwise
     */
    boolean existsByAccountIdAndSnapshotTime(UUID accountId, Instant snapshotTime);
}

package com.sbaldasso.java_banking_core.infrastructure.persistence.repository;

import com.sbaldasso.java_banking_core.infrastructure.persistence.entity.AccountJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for Account entities.
 * Provides pessimistic locking for transaction processing.
 */
@Repository
public interface AccountJpaRepository extends JpaRepository<AccountJpaEntity, UUID> {

    /**
     * Finds an account by ID with pessimistic write lock.
     * This ensures serialized access during transaction processing.
     * 
     * @param accountId The account ID
     * @return Optional containing the locked account
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AccountJpaEntity a WHERE a.accountId = :accountId")
    Optional<AccountJpaEntity> findByIdWithLock(@Param("accountId") UUID accountId);
}

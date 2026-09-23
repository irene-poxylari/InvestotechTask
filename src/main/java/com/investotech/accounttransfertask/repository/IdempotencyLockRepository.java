package com.investotech.accounttransfertask.repository;

import com.investotech.accounttransfertask.exceptions.BadRequestException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class IdempotencyLockRepository {
    private final EntityManager entityManager;

    /**
     * Serializes requests for the same user and idempotency key.
     * The caller's transaction holds the lock until commit or rollback.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void lock(UUID userId, String idempotencyKey) {
        String lockKey = userId + ":" + idempotencyKey;

        entityManager.createNativeQuery("""
                INSERT IGNORE INTO idempotency_locks (lock_key)
                VALUES (:lockKey)
                """)
                .setParameter("lockKey", lockKey)
                .executeUpdate();

        entityManager.createNativeQuery("""
                SELECT lock_key
                FROM idempotency_locks
                WHERE lock_key = :lockKey
                FOR UPDATE
                """)
                .setParameter("lockKey", lockKey)
                .getSingleResult();
    }
}

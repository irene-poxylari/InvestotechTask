package com.investotech.accounttransfertask.repository;

import com.investotech.accounttransfertask.entity.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TransferRepository extends JpaRepository<Transfer, UUID> {
    @Query(value = """
            SELECT t.*
            FROM transfers t
            WHERE (t.source_account_id = :accountId OR t.destination_account_id = :accountId)
              AND (
                    CAST(:cursorCreatedAt AS timestamptz) IS NULL
                    OR t.created_at < CAST(:cursorCreatedAt AS timestamptz)
                    OR (
                        t.created_at = CAST(:cursorCreatedAt AS timestamptz)
                        AND t.id < CAST(:cursorId AS uuid)
                    )
              )
            ORDER BY t.created_at DESC, t.id DESC
            """, nativeQuery = true)
    List<Transfer> findHistory(
            @Param("accountId") String accountId,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") UUID cursorId,
            org.springframework.data.domain.Pageable pageable
    );
}

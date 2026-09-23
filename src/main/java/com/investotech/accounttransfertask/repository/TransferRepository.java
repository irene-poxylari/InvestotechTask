package com.investotech.accounttransfertask.repository;

import com.investotech.accounttransfertask.entity.Transfer;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TransferRepository extends JpaRepository<Transfer, UUID> {

    @Query("""
    select t
    from Transfer t
    where t.sourceAccount.id = :accountId
       or t.destinationAccount.id = :accountId
    order by t.createdAt desc, t.id desc
    """)
    List<Transfer> findFirstHistoryPage(
            @Param("accountId") String accountId,
            Pageable pageable
    );
    @Query("""
    select t
    from Transfer t
    where (
        t.sourceAccount.id = :accountId
        or t.destinationAccount.id = :accountId
    )
    and (
        t.createdAt < :cursorCreatedAt
        or (
            t.createdAt = :cursorCreatedAt
            and t.id < :cursorId
        )
    )
    order by t.createdAt desc, t.id desc
    """)
    List<Transfer> findHistoryAfter(
            @Param("accountId") String accountId,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );
}
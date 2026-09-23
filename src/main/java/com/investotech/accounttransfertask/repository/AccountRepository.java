package com.investotech.accounttransfertask.repository;

import com.investotech.accounttransfertask.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, String> {
    @Query("select a from Account a where a.user.id = :userId order by a.id")
    List<Account> findAllByUserId(@Param("userId") UUID customerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id in :ids order by a.id")
    List<Account> findAllForUpdate(@Param("ids") Collection<String> ids);
}
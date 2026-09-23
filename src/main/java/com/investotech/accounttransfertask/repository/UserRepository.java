package com.investotech.accounttransfertask.repository;

import com.investotech.accounttransfertask.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByApiKeyHash(String apiKeyHash);
}

package com.investotech.accounttransfertask.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    private UUID id;

    @Column(
            name = "api_key_hash",
            nullable = false,
            unique = true,
            length = 64
    )
    private String apiKeyHash;

    public User(UUID id, String apiKeyHash) {
        this.id = id;
        this.apiKeyHash = apiKeyHash;
    }
}
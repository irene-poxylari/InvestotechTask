package com.investotech.accounttransfertask.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@Table(name = "accounts")
@AllArgsConstructor
public class Account {
    @Id
    @Column(length = 100)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "balance_minor", nullable = false)
    private long balanceMinor;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Account() {
    }

    public Account(String id, User user, String currency, int balanceMinor, Instant createdAt) {
        this.id = id;
        this.user = user;
        this.currency = currency;
        this.balanceMinor = balanceMinor;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getCurrency() {
        return currency;
    }

    public long getBalanceMinor() {
        return balanceMinor;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void debit(int amount) {
        this.balanceMinor -= amount;
    }

    public void credit(int amount) {
        this.balanceMinor = Math.addExact(this.balanceMinor, amount);
    }
}
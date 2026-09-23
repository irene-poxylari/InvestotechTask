package com.investotech.accounttransfertask.entity;

import com.investotech.accounttransfertask.exceptions.BusinessRuleException;
import jakarta.persistence.*;
import lombok.Getter;


import java.time.Instant;

@Entity
@Getter
@Table(name = "accounts")
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

    public void debit(int amount) {
        requirePositiveAmount(amount);

        if (balanceMinor < 0) {
            throw new IllegalArgumentException(
                    "Initial balance must not be negative"
            );
        }

        balanceMinor = Math.subtractExact(balanceMinor, amount);
    }

    public void credit(int amount) {
        requirePositiveAmount(amount);
        balanceMinor = Math.addExact(balanceMinor, amount);
    }

    private static void requirePositiveAmount(int amount) {
        if (amount <= 0) {
            throw new BusinessRuleException(
                    "invalid_amount",
                    "Amount must be greater than zero"
            );
        }
    }
}
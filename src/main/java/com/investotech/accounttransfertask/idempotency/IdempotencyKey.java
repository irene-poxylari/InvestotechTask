package com.investotech.accounttransfertask.idempotency;

import com.investotech.accounttransfertask.entity.Transfer;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "idempotency_keys")
@IdClass(IdempotencyKeyId.class)
public class IdempotencyKey {
    @Id
    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Id
    @Column(name = "idempotency_key", nullable = false, length = 200)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transfer_id", nullable = false)
    private Transfer transfer;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected IdempotencyKey() {
    }

    public IdempotencyKey(UUID customerId, String idempotencyKey, String requestHash,
                          Transfer transfer, Instant createdAt) {
        this.customerId = customerId;
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        this.transfer = transfer;
        this.createdAt = createdAt;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public Transfer getTransfer() {
        return transfer;
    }
}

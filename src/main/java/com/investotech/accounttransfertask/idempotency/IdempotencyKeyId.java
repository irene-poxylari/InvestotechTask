package com.investotech.accounttransfertask.idempotency;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class IdempotencyKeyId implements Serializable {
    private UUID userId;
    private String idempotencyKey;

    public IdempotencyKeyId() {
    }

    public IdempotencyKeyId(UUID userId, String idempotencyKey) {
        this.userId = userId;
        this.idempotencyKey = idempotencyKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IdempotencyKeyId that)) return false;
        return Objects.equals(userId, that.userId)
                && Objects.equals(idempotencyKey, that.idempotencyKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, idempotencyKey);
    }
}

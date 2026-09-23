package com.investotech.accounttransfertask.idempotency;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class IdempotencyKeyId implements Serializable {
    private UUID customerId;
    private String idempotencyKey;

    public IdempotencyKeyId() {
    }

    public IdempotencyKeyId(UUID customerId, String idempotencyKey) {
        this.customerId = customerId;
        this.idempotencyKey = idempotencyKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IdempotencyKeyId that)) return false;
        return Objects.equals(customerId, that.customerId)
                && Objects.equals(idempotencyKey, that.idempotencyKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerId, idempotencyKey);
    }
}

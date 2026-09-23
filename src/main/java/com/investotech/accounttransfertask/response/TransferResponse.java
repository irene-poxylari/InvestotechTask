package com.investotech.accounttransfertask.response;

import com.investotech.accounttransfertask.entity.Transfer;

import java.time.Instant;
import java.util.UUID;

public record TransferResponse(
        UUID id,
        String sourceAccountId,
        String destinationAccountId,
        long amount,
        String currency,
        Instant createdAt
) {
    public static TransferResponse from(Transfer transfer) {
        return new TransferResponse(
                transfer.getId(),
                transfer.getSourceAccount().getId(),
                transfer.getDestinationAccount().getId(),
                transfer.getAmountMinor(),
                transfer.getCurrency(),
                transfer.getCreatedAt()
        );
    }
}

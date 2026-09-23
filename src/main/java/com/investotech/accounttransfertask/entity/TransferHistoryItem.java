package com.investotech.accounttransfertask.entity;

import java.time.Instant;
import java.util.UUID;

public record TransferHistoryItem(
        UUID id,
        String sourceAccountId,
        String destinationAccountId,
        long amount,
        String currency,
        String direction,
        Instant createdAt
) {
    public static TransferHistoryItem from(Transfer transfer, String accountId) {
        String direction = transfer.getSourceAccount().getId().equals(accountId) ? "DEBIT" : "CREDIT";
        return new TransferHistoryItem(
                transfer.getId(),
                transfer.getSourceAccount().getId(),
                transfer.getDestinationAccount().getId(),
                transfer.getAmountMinor(),
                transfer.getCurrency(),
                direction,
                transfer.getCreatedAt()
        );
    }
}

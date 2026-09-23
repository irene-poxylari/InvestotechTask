package com.investotech.accounttransfertask.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record TransferHistoryResponse(
        List<TransferHistoryItem> items,
        @JsonProperty("next_cursor") String nextCursor
) {
}

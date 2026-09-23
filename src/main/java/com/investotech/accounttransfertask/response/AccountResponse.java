package com.investotech.accounttransfertask.response;

import com.investotech.accounttransfertask.entity.Account;

public record AccountResponse(
        String id,
        String currency,
        long balance
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(account.getId(), account.getCurrency(), account.getBalanceMinor());
    }
}

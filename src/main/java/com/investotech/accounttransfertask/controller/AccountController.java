package com.investotech.accounttransfertask.controller;

import com.investotech.accounttransfertask.response.AccountResponse;
import com.investotech.accounttransfertask.service.AccountService;
import lombok.AllArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/accounts")
@Validated
@AllArgsConstructor
public class AccountController {
    private final AccountService accountService;
    // private final TransferService transferService;

    @GetMapping
    public List<AccountResponse> listAccounts(
            @AuthenticationPrincipal UUID userId
    ) {
        return accountService.listAccounts(userId);
    }

//    @GetMapping("/{accountId}/transfers")
//    public TransferHistoryResponse history(
//            @RequestAttribute(TokenAuthenticationFilter.CUSTOMER_ID_ATTRIBUTE) UUID customerId,
//            @PathVariable String accountId,
//            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
//            @RequestParam(required = false) String cursor
//    ) {
//        return transferService.history(customerId, accountId, limit, cursor);
//    }
}

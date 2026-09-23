package com.investotech.accounttransfertask.service;

import com.investotech.accounttransfertask.entity.Account;
import com.investotech.accounttransfertask.exceptions.ForbiddenException;
import com.investotech.accounttransfertask.exceptions.NotFoundException;
import com.investotech.accounttransfertask.repository.AccountRepository;
import com.investotech.accounttransfertask.response.AccountResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AccountService {
    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> listAccounts(UUID userId) {
        System.out.println("Authenticated userId = " + userId);

        List<Account> accounts =
                accountRepository.findAllByUserId(userId);

        System.out.println("Accounts found = " + accounts.size());

        return accounts.stream()
                .map(AccountResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Account requireOwnedAccount(UUID customerId, String accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Account not found"));
        if (!account.getUser().getId().equals(customerId)) {
            throw new ForbiddenException("You do not own this account");
        }
        return account;
    }
}

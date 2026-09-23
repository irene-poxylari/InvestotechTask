package com.investotech.accounttransfertask.service;

import com.investotech.accounttransfertask.entity.*;
import com.investotech.accounttransfertask.exceptions.*;
import com.investotech.accounttransfertask.idempotency.IdempotencyKey;
import com.investotech.accounttransfertask.idempotency.IdempotencyKeyId;
import com.investotech.accounttransfertask.idempotency.IdempotencyKeyRepository;
import com.investotech.accounttransfertask.repository.AccountRepository;
import com.investotech.accounttransfertask.repository.IdempotencyLockRepository;
import com.investotech.accounttransfertask.repository.TransferRepository;
import com.investotech.accounttransfertask.response.TransferResponse;
import com.investotech.accounttransfertask.security.ApiKeyHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransferService {
    private final ApiKeyHasher hasher;
    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;
    private final IdempotencyKeyRepository idempotencyRepository;
    private final CursorCodec cursorCodec;
    private final IdempotencyLockRepository idempotencyLockRepository;


    @Transactional
    public TransferResponse createTransfer(UUID customerId, String idempotencyKey, CreateTransferRequest request) {
        validateIdempotencyKey(idempotencyKey);

        if (request.sourceAccountId().equals(request.destinationAccountId())) {
            throw new BusinessRuleException("same_account", "Source and destination accounts must be different");
        }

        String requestHash = requestHash(request);
        idempotencyLockRepository.lock(customerId, idempotencyKey);

        IdempotencyKeyId keyId = new IdempotencyKeyId(customerId, idempotencyKey);
        var existing = idempotencyRepository.findById(keyId);
        if (existing.isPresent()) {
            if (!existing.get().getRequestHash().equals(requestHash)) {
                throw new ConflictException("Idempotency-Key was already used with a different request body");
            }
            return TransferResponse.from(existing.get().getTransfer());
        }

        List<Account> lockedAccounts = accountRepository.findAllForUpdate(
                List.of(request.sourceAccountId(), request.destinationAccountId())
        );
        if (lockedAccounts.size() != 2) {
            throw new NotFoundException("One or both accounts were not found");
        }

        Map<String, Account> byId = new HashMap<>();
        lockedAccounts.forEach(account -> byId.put(account.getId(), account));
        Account source = byId.get(request.sourceAccountId());
        Account destination = byId.get(request.destinationAccountId());

        businessRulesChecks(customerId, request, source, destination);

        source.debit(request.amount());
        try {
            destination.credit(request.amount());
        } catch (ArithmeticException ex) {
            throw new BusinessRuleException("amount_out_of_range", "Destination balance would exceed the supported range");
        }

        Instant now = Instant.now();
        Transfer transfer = new Transfer(
                UUID.randomUUID(),
                source,
                destination,
                request.amount(),
                request.currency(),
                now
        );
        transferRepository.save(transfer);
        idempotencyRepository.save(new IdempotencyKey(
                customerId,
                idempotencyKey,
                requestHash,
                transfer,
                now
        ));

        return TransferResponse.from(transfer);
    }

    private static void businessRulesChecks(UUID customerId, CreateTransferRequest request, Account source, Account destination) {
        if (!source.getUser().getId().equals(customerId)) {
            throw new ForbiddenException("You can transfer money only from an account you own");
        }
        if (!source.getCurrency().equals(destination.getCurrency())) {
            throw new BusinessRuleException("currency_mismatch", "Source and destination accounts must use the same currency");
        }
        if (!source.getCurrency().equals(request.currency())) {
            throw new BusinessRuleException("currency_mismatch", "Request currency must match both accounts");
        }
        if (source.getBalanceMinor() < request.amount()) {
            throw new BusinessRuleException("insufficient_funds", "Source account has insufficient funds");
        }
    }

    @Transactional(readOnly = true)
    public TransferHistoryResponse history(
            UUID customerId,
            String accountId,
            int limit,
            String cursor
    ) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() ->
                        new NotFoundException("Account not found")
                );

        if (!account.getUser().getId().equals(customerId)) {
            throw new ForbiddenException(
                    "You do not own this account"
            );
        }

        CursorCodec.Cursor decoded = cursorCodec.decode(cursor);

        PageRequest pageRequest = PageRequest.of(0, limit + 1);

        List<Transfer> rows;

        if (decoded == null) {
            rows = transferRepository.findFirstHistoryPage(
                    accountId,
                    pageRequest
            );
        } else {
            rows = transferRepository.findHistoryAfter(
                    accountId,
                    decoded.createdAt(),
                    decoded.id(),
                    pageRequest
            );
        }

        boolean hasMore = rows.size() > limit;

        List<Transfer> page =
                hasMore ? rows.subList(0, limit) : rows;

        String nextCursor =
                hasMore && !page.isEmpty()
                        ? cursorCodec.encode(page.get(page.size() - 1))
                        : null;

        return new TransferHistoryResponse(
                page.stream()
                        .map(t -> TransferHistoryItem.from(t, accountId))
                        .toList(),
                nextCursor
        );
    }

    private String requestHash(CreateTransferRequest request) {
        String canonical = request.sourceAccountId() + "\u0000"
                + request.destinationAccountId() + "\u0000"
                + request.amount() + "\u0000"
                + request.currency();
        return hasher.sha256(canonical);
    }
    public void validateIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BadRequestException("missing_idempotency_key", "Idempotency-Key header is required");
        }
        if (idempotencyKey.length() > 200) {
            throw new BadRequestException("invalid_idempotency_key", "Idempotency-Key must be at most 200 characters");
        }
    }
}


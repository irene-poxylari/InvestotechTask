package com.investotech.accounttransfertask;

import com.investotech.accounttransfertask.entity.*;
import com.investotech.accounttransfertask.exceptions.*;
import com.investotech.accounttransfertask.idempotency.*;
import com.investotech.accounttransfertask.repository.*;
import com.investotech.accounttransfertask.response.TransferResponse;
import com.investotech.accounttransfertask.security.ApiKeyHasher;
import com.investotech.accounttransfertask.service.TransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Pure unit tests: no Spring context, database, or real credentials. */
@ExtendWith(MockitoExtension.class)
class TransferServiceTest {
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Instant TIME = Instant.parse("2026-01-01T12:00:00Z");
    private static final String KEY = "transfer-001";

    @Mock AccountRepository accounts;
    @Mock TransferRepository transfers;
    @Mock IdempotencyKeyRepository idempotency;
    @Mock IdempotencyLockRepository locks;

    private final CursorCodec cursors = new CursorCodec();
    private TransferService service;
    private Account source;
    private Account destination;

    @BeforeEach
    void setUp() {
        service = new TransferService(new ApiKeyHasher(), accounts, transfers,
                idempotency, cursors, locks);
        source = account("source", USER_ID, "EUR", 1000);
        destination = account("destination", OTHER_USER_ID, "EUR", 250);
    }

    @Test
    void successfulTransferMovesMoneyAndPersistsMatchingRecords() {
        stubAccounts();
        TransferResponse response = service.createTransfer(USER_ID, KEY, request(300));

        assertAll(
                () -> assertEquals(700L, source.getBalanceMinor()),
                () -> assertEquals(550L, destination.getBalanceMinor()),
                () -> assertEquals(1_250L, source.getBalanceMinor() + destination.getBalanceMinor()),
                () -> assertEquals("source", response.sourceAccountId()),
                () -> assertEquals("destination", response.destinationAccountId()),
                () -> assertEquals(300L, response.amount()),
                () -> assertEquals("EUR", response.currency()),
                () -> assertNotNull(response.id()),
                () -> assertNotNull(response.createdAt())
        );
        ArgumentCaptor<Transfer> savedTransfer = ArgumentCaptor.forClass(Transfer.class);
        ArgumentCaptor<IdempotencyKey> savedKey = ArgumentCaptor.forClass(IdempotencyKey.class);
        verify(transfers).save(savedTransfer.capture());
        verify(idempotency).save(savedKey.capture());
        assertEquals(response, TransferResponse.from(savedTransfer.getValue()));
        assertSame(savedTransfer.getValue(), savedKey.getValue().getTransfer());

        // Checking an existing request must happen after acquiring its lock.
        var order = inOrder(locks, idempotency, accounts);
        order.verify(locks).lock(USER_ID, KEY);
        order.verify(idempotency).findById(new IdempotencyKeyId(USER_ID, KEY));
        order.verify(accounts).findAllForUpdate(List.of("source", "destination"));
    }

    @Test
    void exactAvailableBalanceCanBeTransferred() {
        stubAccounts();
        service.createTransfer(USER_ID, KEY, request(1_000));
        assertEquals(0L, source.getBalanceMinor());
        assertEquals(1_250L, destination.getBalanceMinor());
    }

    @Test
    void retryReturnsOriginalTransferWithoutMovingMoneyAgain() {
        stubAccounts();
        TransferResponse first = service.createTransfer(USER_ID, KEY, request(300));
        ArgumentCaptor<IdempotencyKey> captured = ArgumentCaptor.forClass(IdempotencyKey.class);
        verify(idempotency).save(captured.capture());
        when(idempotency.findById(new IdempotencyKeyId(USER_ID, KEY)))
                .thenReturn(Optional.of(captured.getValue()));

        TransferResponse retry = service.createTransfer(USER_ID, KEY, request(300));
        assertEquals(first, retry);
        assertEquals(700L, source.getBalanceMinor());
        assertEquals(550L, destination.getBalanceMinor());
        verify(accounts, times(1)).findAllForUpdate(anyCollection());
        verify(transfers, times(1)).save(any(Transfer.class));
        verify(idempotency, times(1)).save(any(IdempotencyKey.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"source", "destination", "amount", "currency"})
    void retryWithAnyChangedRequestFieldIsRejected(String changedField) {
        stubAccounts();
        service.createTransfer(USER_ID, KEY, request(300));
        ArgumentCaptor<IdempotencyKey> captured = ArgumentCaptor.forClass(IdempotencyKey.class);
        verify(idempotency).save(captured.capture());
        when(idempotency.findById(new IdempotencyKeyId(USER_ID, KEY)))
                .thenReturn(Optional.of(captured.getValue()));
        CreateTransferRequest changed = switch (changedField) {
            case "source" -> new CreateTransferRequest("another-source", "destination", 300, "EUR");
            case "destination" -> new CreateTransferRequest("source", "another-destination", 300, "EUR");
            case "amount" -> request(301);
            default -> new CreateTransferRequest("source", "destination", 300, "USD");
        };

        assertThrows(ConflictException.class, () -> service.createTransfer(USER_ID, KEY, changed));
        assertEquals(700L, source.getBalanceMinor());
        assertEquals(550L, destination.getBalanceMinor());
        verify(accounts, times(1)).findAllForUpdate(anyCollection());
        verify(transfers, times(1)).save(any(Transfer.class));
        verify(idempotency, times(1)).save(any(IdempotencyKey.class));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void missingOrBlankKeyIsRejectedBeforePersistence(String key) {
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.createTransfer(USER_ID, key, request(300)));
        assertEquals("missing_idempotency_key", ex.getCode());
        verifyNoInteractions(accounts, transfers, idempotency, locks);
    }

    @Test
    void keyLongerThan200CharactersIsRejected() {
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.createTransfer(USER_ID, "x".repeat(201), request(300)));
        assertEquals("invalid_idempotency_key", ex.getCode());
        verifyNoInteractions(accounts, transfers, idempotency, locks);
    }

    @Test
    void keyAt200CharacterLimitIsAccepted() {
        stubAccounts();
        assertNotNull(service.createTransfer(USER_ID, "x".repeat(200), request(300)));
    }

    @Test
    void transferToSameAccountIsRejected() {
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> service.createTransfer(USER_ID, KEY,
                        new CreateTransferRequest("source", "source", 300, "EUR")));
        assertEquals("same_account", ex.getCode());
        verifyNoInteractions(accounts, transfers, idempotency, locks);
    }

    @Test
    void missingAccountDoesNotMoveMoney() {
        when(accounts.findAllForUpdate(anyCollection())).thenReturn(List.of(source));
        assertThrows(NotFoundException.class, () -> service.createTransfer(USER_ID, KEY, request(300)));
        assertUnchangedAndNoWrites();
    }

    @Test
    void someoneElsesSourceAccountIsRejected() {
        stubAccounts();
        assertThrows(ForbiddenException.class,
                () -> service.createTransfer(OTHER_USER_ID, KEY, request(300)));
        assertUnchangedAndNoWrites();
    }

    @Test
    void mismatchedAccountCurrenciesAreRejected() {
        destination = account("destination", OTHER_USER_ID, "USD", 250);
        stubAccounts();
        assertBusinessFailure("currency_mismatch", request(300));
        assertUnchangedAndNoWrites();
    }

    @Test
    void mismatchedRequestCurrencyIsRejected() {
        stubAccounts();
        assertBusinessFailure("currency_mismatch",
                new CreateTransferRequest("source", "destination", 300, "USD"));
        assertUnchangedAndNoWrites();
    }

    @Test
    void insufficientFundsDoNotMoveMoney() {
        stubAccounts();
        assertBusinessFailure("insufficient_funds", request(1_001));
        assertUnchangedAndNoWrites();
    }

    @Test
    void destinationOverflowIsReportedWithoutSavingTransfer() {
        destination = account("destination", OTHER_USER_ID, "EUR", Integer.MAX_VALUE);
        stubAccounts();
        assertBusinessFailure("amount_out_of_range", request(1));
        assertEquals(Long.MAX_VALUE, destination.getBalanceMinor());
        verify(transfers, never()).save(any(Transfer.class));
        verify(idempotency, never()).save(any(IdempotencyKey.class));
        // Do not assert rollback here: a directly constructed service has no transaction proxy.
        // Database rollback belongs in a MySQL-backed integration test.
    }

    @Test
    void transferPersistenceFailureIsPropagatedAndKeyIsNotSaved() {
        stubAccounts();
        RuntimeException failure = new IllegalStateException("database unavailable");
        when(transfers.save(any(Transfer.class))).thenThrow(failure);
        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> service.createTransfer(USER_ID, KEY, request(300))));
        verify(idempotency, never()).save(any(IdempotencyKey.class));
    }

    @Test
    void historyOfAnotherUsersAccountIsRejectedBeforeQueryingTransfers() {
        when(accounts.findById("source")).thenReturn(Optional.of(source));
        assertThrows(ForbiddenException.class,
                () -> service.history(OTHER_USER_ID, "source", 20, null));
        verifyNoInteractions(transfers);
    }

    @Test
    void missingHistoryAccountIsReported() {
        assertThrows(NotFoundException.class, () -> service.history(USER_ID, "missing", 20, null));
        verifyNoInteractions(transfers);
    }

    @Test
    void invalidHistoryCursorIsRejectedBeforeQueryingTransfers() {
        when(accounts.findById("source")).thenReturn(Optional.of(source));
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.history(USER_ID, "source", 20, "not-a-valid-cursor!"));
        assertEquals("invalid_cursor", ex.getCode());
        verifyNoInteractions(transfers);
    }

    @Test
    void historyUsesLookaheadAndEncodesLastReturnedRow() {
        when(accounts.findById("source")).thenReturn(Optional.of(source));
        Transfer first = transfer(source, destination, TIME.plusSeconds(3));
        Transfer second = transfer(destination, source, TIME.plusSeconds(2));
        Transfer lookahead = transfer(source, destination, TIME.plusSeconds(1));
        when(transfers.findFirstHistoryPage("source", PageRequest.of(0, 3)))
                .thenReturn(List.of(first, second, lookahead));

        TransferHistoryResponse response = service.history(USER_ID, "source", 2, null);
        assertEquals(List.of(first.getId(), second.getId()),
                response.items().stream().map(TransferHistoryItem::id).toList());
        assertEquals(List.of("DEBIT", "CREDIT"),
                response.items().stream().map(TransferHistoryItem::direction).toList());
        assertEquals(new CursorCodec.Cursor(second.getCreatedAt(), second.getId()),
                cursors.decode(response.nextCursor()));
    }

    @Test
    void subsequentHistoryPageUsesDecodedCursorAndEndsWithoutNextCursor() {
        when(accounts.findById("source")).thenReturn(Optional.of(source));
        Transfer previous = transfer(source, destination, TIME.plusSeconds(2));
        Transfer last = transfer(source, destination, TIME);
        when(transfers.findHistoryAfter("source", previous.getCreatedAt(), previous.getId(),
                PageRequest.of(0, 2))).thenReturn(List.of(last));

        TransferHistoryResponse response = service.history(USER_ID, "source", 1, cursors.encode(previous));
        assertEquals(List.of(last.getId()), response.items().stream().map(TransferHistoryItem::id).toList());
        assertNull(response.nextCursor());
        verify(transfers, never()).findFirstHistoryPage(anyString(), any());
    }

    @Test
    void emptyHistoryHasNoNextCursor() {
        when(accounts.findById("source")).thenReturn(Optional.of(source));
        when(transfers.findFirstHistoryPage("source", PageRequest.of(0, 21))).thenReturn(List.of());
        TransferHistoryResponse response = service.history(USER_ID, "source", 20, null);
        assertTrue(response.items().isEmpty());
        assertNull(response.nextCursor());
    }

    private void stubAccounts() {
        // Deliberately reversed: the service must identify accounts by ID, not list position.
        when(accounts.findAllForUpdate(List.of("source", "destination")))
                .thenReturn(List.of(destination, source));
    }

    private void assertBusinessFailure(String code, CreateTransferRequest request) {
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> service.createTransfer(USER_ID, KEY, request));
        assertEquals(code, ex.getCode());
    }

    private void assertUnchangedAndNoWrites() {
        assertEquals(1_000L, source.getBalanceMinor());
        assertEquals(250L, destination.getBalanceMinor());
        verify(transfers, never()).save(any(Transfer.class));
        verify(idempotency, never()).save(any(IdempotencyKey.class));
    }

    private static CreateTransferRequest request(int amount) {
        return new CreateTransferRequest("source", "destination", amount, "EUR");
    }

    private static Account account(String id, UUID owner, String currency, int balance) {
        return new Account(id, new User(owner, "test-only-hash"), currency, balance, TIME);
    }

    private static Transfer transfer(Account from, Account to, Instant time) {
        return new Transfer(UUID.randomUUID(), from, to, 100L, "EUR", time);
    }
}


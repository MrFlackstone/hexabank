package com.hexabank.ledger.application.service;

import com.hexabank.ledger.application.port.in.CallerContext;
import com.hexabank.ledger.application.port.out.AccountOwnershipPort;
import com.hexabank.ledger.application.port.out.LedgerBroadcastPort;
import com.hexabank.ledger.application.port.out.LedgerEntryRepositoryPort;
import com.hexabank.ledger.application.port.out.ProcessedEventPort;
import com.hexabank.ledger.application.port.out.TransferViewRepositoryPort;
import com.hexabank.ledger.domain.EntryType;
import com.hexabank.ledger.domain.LedgerEntry;
import com.hexabank.ledger.domain.TransferView;
import com.hexabank.ledger.domain.exception.AccountNotFoundException;
import com.hexabank.ledger.domain.exception.TransferViewNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LedgerServiceTest {

    private LedgerEntryRepositoryPort ledgerEntries;
    private TransferViewRepositoryPort transferViews;
    private ProcessedEventPort processedEvents;
    private LedgerBroadcastPort broadcast;
    private AccountOwnershipPort accountOwnership;
    private LedgerService service;

    @BeforeEach
    void setUp() {
        ledgerEntries = mock(LedgerEntryRepositoryPort.class);
        transferViews = mock(TransferViewRepositoryPort.class);
        processedEvents = mock(ProcessedEventPort.class);
        broadcast = mock(LedgerBroadcastPort.class);
        accountOwnership = mock(AccountOwnershipPort.class);
        service = new LedgerService(ledgerEntries, transferViews, processedEvents, broadcast, accountOwnership);
    }

    @Test
    @DisplayName("MoneyDebited registra asiento DEBIT, rellena la proyección y difunde con su propietario")
    void moneyDebitedRecordsEntryAndProjection() {
        UUID eventId = UUID.randomUUID();
        UUID transferId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        when(processedEvents.alreadyProcessed(eventId)).thenReturn(false);
        when(transferViews.find(transferId)).thenReturn(Optional.empty());
        when(accountOwnership.ownerOf(accountId)).thenReturn(Optional.of("alice-sub"));

        service.onMoneyDebited(eventId, transferId, accountId, new BigDecimal("40.00"));

        ArgumentCaptor<LedgerEntry> entryCaptor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntries).append(entryCaptor.capture());
        assertThat(entryCaptor.getValue().type()).isEqualTo(EntryType.DEBIT);
        assertThat(entryCaptor.getValue().entryId()).isEqualTo(eventId);

        ArgumentCaptor<TransferView> viewCaptor = ArgumentCaptor.forClass(TransferView.class);
        verify(transferViews).save(viewCaptor.capture());
        assertThat(viewCaptor.getValue().sourceAccountId()).isEqualTo(accountId);
        assertThat(viewCaptor.getValue().amount()).isEqualByComparingTo("40.00");

        verify(broadcast).broadcast(any(LedgerEntry.class), eq("alice-sub"));
        verify(processedEvents).markProcessed(eventId);
    }

    @Test
    @DisplayName("un evento ya procesado no produce ningún efecto (idempotencia)")
    void duplicateEventIsNoOp() {
        UUID eventId = UUID.randomUUID();
        when(processedEvents.alreadyProcessed(eventId)).thenReturn(true);

        service.onMoneyDebited(eventId, UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("40.00"));

        verifyNoInteractions(ledgerEntries, broadcast);
        verify(transferViews, never()).save(any());
        verify(processedEvents, never()).markProcessed(any());
    }

    @Test
    @DisplayName("TransferCompleted marca la proyección como COMPLETED")
    void transferCompletedMarksProjection() {
        UUID eventId = UUID.randomUUID();
        UUID transferId = UUID.randomUUID();
        when(processedEvents.alreadyProcessed(eventId)).thenReturn(false);
        when(transferViews.find(transferId)).thenReturn(Optional.of(TransferView.pending(transferId)));

        service.onTransferCompleted(eventId, transferId);

        ArgumentCaptor<TransferView> viewCaptor = ArgumentCaptor.forClass(TransferView.class);
        verify(transferViews).save(viewCaptor.capture());
        assertThat(viewCaptor.getValue().status()).isEqualTo(TransferView.COMPLETED);
        verify(ledgerEntries, never()).append(any());
        verify(processedEvents).markProcessed(eventId);
    }

    @Test
    @DisplayName("AccountOpened materializa la proyección de propietarios de forma idempotente")
    void accountOpenedRegistersOwnershipIdempotently() {
        UUID eventId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        when(processedEvents.alreadyProcessed(eventId)).thenReturn(false);

        service.onAccountOpened(eventId, accountId, "alice-sub");

        verify(accountOwnership).register(accountId, "alice-sub");
        verify(processedEvents).markProcessed(eventId);

        when(processedEvents.alreadyProcessed(eventId)).thenReturn(true);
        service.onAccountOpened(eventId, accountId, "alice-sub");

        // La segunda entrega no re-registra ni re-marca.
        verify(accountOwnership).register(accountId, "alice-sub");
        verify(processedEvents).markProcessed(eventId);
    }

    @Test
    @DisplayName("entriesForAccount: el dueño y STAFF consultan; otro usuario recibe 404-exception")
    void entriesForAccountEnforcesOwnership() {
        UUID accountId = UUID.randomUUID();
        when(accountOwnership.ownerOf(accountId)).thenReturn(Optional.of("alice-sub"));

        service.entriesForAccount(accountId, CallerContext.user("alice-sub"));
        service.entriesForAccount(accountId, CallerContext.staff("staff-sub"));
        assertThatThrownBy(() -> service.entriesForAccount(accountId, CallerContext.user("bob-sub")))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    @DisplayName("getTransfer: visible para dueños de origen/destino y STAFF; 404 para terceros")
    void getTransferEnforcesInvolvement() {
        UUID transferId = UUID.randomUUID();
        UUID source = UUID.randomUUID();
        UUID destination = UUID.randomUUID();
        TransferView view = new TransferView(transferId, source, destination,
                new BigDecimal("40.00"), TransferView.PENDING);
        when(transferViews.find(transferId)).thenReturn(Optional.of(view));
        when(accountOwnership.ownerOf(source)).thenReturn(Optional.of("alice-sub"));
        when(accountOwnership.ownerOf(destination)).thenReturn(Optional.of("bob-sub"));

        assertThat(service.getTransfer(transferId, CallerContext.user("alice-sub"))).isEqualTo(view);
        assertThat(service.getTransfer(transferId, CallerContext.user("bob-sub"))).isEqualTo(view);
        assertThat(service.getTransfer(transferId, CallerContext.staff("staff-sub"))).isEqualTo(view);
        assertThatThrownBy(() -> service.getTransfer(transferId, CallerContext.user("carol-sub")))
                .isInstanceOf(TransferViewNotFoundException.class);
    }
}

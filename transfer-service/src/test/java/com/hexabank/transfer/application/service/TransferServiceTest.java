package com.hexabank.transfer.application.service;

import com.hexabank.shared.events.CreditRequested;
import com.hexabank.shared.events.DebitRequested;
import com.hexabank.shared.events.DomainEvent;
import com.hexabank.shared.events.RefundRequested;
import com.hexabank.shared.events.TransferCompleted;
import com.hexabank.shared.events.TransferFailed;
import com.hexabank.transfer.application.port.in.CallerContext;
import com.hexabank.transfer.application.port.in.RequestTransferCommand;
import com.hexabank.transfer.application.port.in.PageResult;
import com.hexabank.transfer.application.port.in.TransferSearchQuery;
import com.hexabank.transfer.application.port.in.TransferSort;
import com.hexabank.transfer.application.port.out.AccountOwnershipPort;
import com.hexabank.transfer.application.port.out.EventPublisherPort;
import com.hexabank.transfer.application.port.out.LoadTransferPort;
import com.hexabank.transfer.application.port.out.ProcessedEventPort;
import com.hexabank.transfer.application.port.out.SaveTransferPort;
import com.hexabank.transfer.application.port.out.SearchTransfersPort;
import com.hexabank.transfer.application.port.out.TransferMetricsPort;
import com.hexabank.transfer.application.port.out.TransferSearchCriteria;
import com.hexabank.transfer.domain.Transfer;
import com.hexabank.transfer.domain.TransferStatus;
import com.hexabank.transfer.domain.exception.AccountNotFoundException;
import com.hexabank.transfer.domain.exception.TransferNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios del orquestador de la saga: sin Spring ni Kafka, solo dominio + puertos simulados.
 */
@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    LoadTransferPort loadTransferPort;
    @Mock
    SaveTransferPort saveTransferPort;
    @Mock
    EventPublisherPort eventPublisher;
    @Mock
    ProcessedEventPort processedEvents;
    @Mock
    TransferMetricsPort metrics;
    @Mock
    AccountOwnershipPort accountOwnership;
    @Mock
    SearchTransfersPort searchTransfersPort;

    TransferService service;

    private final UUID source = UUID.randomUUID();
    private final UUID destination = UUID.randomUUID();
    private final UUID eventId = UUID.randomUUID();
    private final CallerContext alice = CallerContext.user("alice-sub");

    @BeforeEach
    void setUp() {
        service = new TransferService(loadTransferPort, saveTransferPort, eventPublisher, processedEvents,
                metrics, accountOwnership, searchTransfersPort);
    }

    @Test
    @DisplayName("requestTransfer persiste PENDING y publica DebitRequested")
    void requestTransferPublishesDebitRequested() {
        when(saveTransferPort.save(any(Transfer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(accountOwnership.ownerOf(source)).thenReturn(Optional.of("alice-sub"));

        service.requestTransfer(new RequestTransferCommand(source, destination, new BigDecimal("50.00")), alice);

        ArgumentCaptor<Transfer> savedCaptor = ArgumentCaptor.forClass(Transfer.class);
        verify(saveTransferPort).save(savedCaptor.capture());
        assertThat(savedCaptor.getValue().status()).isEqualTo(TransferStatus.PENDING);

        DomainEvent published = capturePublished();
        assertThat(published).isInstanceOf(DebitRequested.class);
        assertThat(((DebitRequested) published).accountId()).isEqualTo(source);
        assertThat(((DebitRequested) published).amount()).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("requestTransfer desde una cuenta ajena o desconocida lanza AccountNotFoundException (404)")
    void requestTransferRejectsForeignSource() {
        when(accountOwnership.ownerOf(source)).thenReturn(Optional.of("bob-sub"));

        assertThatThrownBy(() -> service.requestTransfer(
                new RequestTransferCommand(source, destination, new BigDecimal("50.00")), alice))
                .isInstanceOf(AccountNotFoundException.class);

        when(accountOwnership.ownerOf(source)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.requestTransfer(
                new RequestTransferCommand(source, destination, new BigDecimal("50.00")), alice))
                .isInstanceOf(AccountNotFoundException.class);

        verifyNoInteractions(saveTransferPort, eventPublisher);
    }

    @Test
    @DisplayName("requestTransfer con rol STAFF no exige propiedad de la cuenta origen")
    void requestTransferAllowsStaffFromAnyAccount() {
        when(saveTransferPort.save(any(Transfer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.requestTransfer(new RequestTransferCommand(source, destination, new BigDecimal("50.00")),
                CallerContext.staff("staff-sub"));

        verify(saveTransferPort).save(any(Transfer.class));
        verifyNoInteractions(accountOwnership);
    }

    @Test
    @DisplayName("getTransfer es visible para el dueño del origen o del destino; para otros es 404")
    void getTransferVisibleOnlyToInvolvedOwners() {
        Transfer transfer = Transfer.request(source, destination, new BigDecimal("50.00"));
        when(loadTransferPort.findById(transfer.id())).thenReturn(Optional.of(transfer));
        when(accountOwnership.ownerOf(source)).thenReturn(Optional.of("bob-sub"));
        when(accountOwnership.ownerOf(destination)).thenReturn(Optional.of("alice-sub"));

        assertThat(service.getTransfer(transfer.id(), alice)).isEqualTo(transfer);
        assertThat(service.getTransfer(transfer.id(), CallerContext.staff("staff-sub"))).isEqualTo(transfer);
        assertThatThrownBy(() -> service.getTransfer(transfer.id(), CallerContext.user("carol-sub")))
                .isInstanceOf(TransferNotFoundException.class);
    }

    @Test
    @DisplayName("searchTransfers de un USER restringe la consulta a sus cuentas")
    void searchRestrictsVisibilityToOwnedAccounts() {
        when(accountOwnership.accountsOwnedBy("alice-sub")).thenReturn(java.util.Set.of(source));
        when(searchTransfersPort.search(any(TransferSearchCriteria.class)))
                .thenReturn(PageResult.empty(0, 20));

        service.searchTransfers(defaultQuery(), alice);

        ArgumentCaptor<TransferSearchCriteria> captor = ArgumentCaptor.forClass(TransferSearchCriteria.class);
        verify(searchTransfersPort).search(captor.capture());
        assertThat(captor.getValue().visibleAccountIds()).containsExactly(source);
    }

    @Test
    @DisplayName("searchTransfers de un USER sin cuentas devuelve página vacía sin consultar la BD")
    void searchWithoutOwnedAccountsShortCircuits() {
        when(accountOwnership.accountsOwnedBy("alice-sub")).thenReturn(java.util.Set.of());

        PageResult<Transfer> result = service.searchTransfers(defaultQuery(), alice);

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
        verifyNoInteractions(searchTransfersPort);
    }

    @Test
    @DisplayName("searchTransfers de STAFF no aplica filtro de visibilidad")
    void searchAsStaffSeesEverything() {
        when(searchTransfersPort.search(any(TransferSearchCriteria.class)))
                .thenReturn(PageResult.empty(0, 20));

        service.searchTransfers(defaultQuery(), CallerContext.staff("staff-sub"));

        ArgumentCaptor<TransferSearchCriteria> captor = ArgumentCaptor.forClass(TransferSearchCriteria.class);
        verify(searchTransfersPort).search(captor.capture());
        assertThat(captor.getValue().visibleAccountIds()).isNull();
        verifyNoInteractions(accountOwnership);
    }

    private TransferSearchQuery defaultQuery() {
        return new TransferSearchQuery(null, null, null, 0, 20, TransferSort.CREATED_AT, false);
    }

    @Test
    @DisplayName("onMoneyDebited avanza a DEBITED y publica CreditRequested al destino")
    void onMoneyDebitedPublishesCreditRequested() {
        Transfer transfer = Transfer.request(source, destination, new BigDecimal("50.00"));
        when(processedEvents.alreadyProcessed(eventId)).thenReturn(false);
        when(loadTransferPort.findById(any())).thenReturn(Optional.of(transfer));

        service.onMoneyDebited(eventId, transfer.id().value());

        assertThat(transfer.status()).isEqualTo(TransferStatus.DEBITED);
        verify(processedEvents).markProcessed(eventId);
        DomainEvent published = capturePublished();
        assertThat(published).isInstanceOf(CreditRequested.class);
        assertThat(((CreditRequested) published).accountId()).isEqualTo(destination);
    }

    @Test
    @DisplayName("onCreditFailed pasa a COMPENSATING y publica RefundRequested al origen")
    void onCreditFailedPublishesRefundRequested() {
        Transfer transfer = Transfer.request(source, destination, new BigDecimal("50.00"));
        transfer.markDebited();
        when(processedEvents.alreadyProcessed(eventId)).thenReturn(false);
        when(loadTransferPort.findById(any())).thenReturn(Optional.of(transfer));

        service.onCreditFailed(eventId, transfer.id().value(), "Cuenta destino bloqueada");

        assertThat(transfer.status()).isEqualTo(TransferStatus.COMPENSATING);
        verify(processedEvents).markProcessed(eventId);
        DomainEvent published = capturePublished();
        assertThat(published).isInstanceOf(RefundRequested.class);
        assertThat(((RefundRequested) published).accountId()).isEqualTo(source);
    }

    @Test
    @DisplayName("evento ya procesado: idempotente, no toca la transferencia ni publica")
    void alreadyProcessedIsNoOp() {
        when(processedEvents.alreadyProcessed(eventId)).thenReturn(true);

        service.onMoneyDebited(eventId, UUID.randomUUID());

        verifyNoInteractions(loadTransferPort, saveTransferPort, eventPublisher, metrics);
        verify(processedEvents, never()).markProcessed(any());
    }

    @Test
    @DisplayName("onMoneyCredited completa la transferencia e incrementa la métrica de éxito")
    void onMoneyCreditedCountsCompleted() {
        Transfer transfer = Transfer.request(source, destination, new BigDecimal("50.00"));
        transfer.markDebited();
        when(processedEvents.alreadyProcessed(eventId)).thenReturn(false);
        when(loadTransferPort.findById(any())).thenReturn(Optional.of(transfer));

        service.onMoneyCredited(eventId, transfer.id().value());

        assertThat(transfer.status()).isEqualTo(TransferStatus.COMPLETED);
        assertThat(capturePublished()).isInstanceOf(TransferCompleted.class);
        verify(metrics).transferCompleted();
        verify(metrics, never()).transferFailed();
    }

    @Test
    @DisplayName("onDebitFailed marca FAILED e incrementa la métrica de fallo")
    void onDebitFailedCountsFailed() {
        Transfer transfer = Transfer.request(source, destination, new BigDecimal("50.00"));
        when(processedEvents.alreadyProcessed(eventId)).thenReturn(false);
        when(loadTransferPort.findById(any())).thenReturn(Optional.of(transfer));

        service.onDebitFailed(eventId, transfer.id().value(), "Fondos insuficientes");

        assertThat(transfer.status()).isEqualTo(TransferStatus.FAILED);
        assertThat(capturePublished()).isInstanceOf(TransferFailed.class);
        verify(metrics).transferFailed();
        verify(metrics, never()).transferCompleted();
    }

    private DomainEvent capturePublished() {
        ArgumentCaptor<DomainEvent> captor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher).publish(captor.capture());
        return captor.getValue();
    }
}

package com.hexabank.transfer.application.service;

import com.hexabank.shared.events.CreditRequested;
import com.hexabank.shared.events.DebitRequested;
import com.hexabank.shared.events.DomainEvent;
import com.hexabank.shared.events.RefundRequested;
import com.hexabank.transfer.application.port.in.RequestTransferCommand;
import com.hexabank.transfer.application.port.out.EventPublisherPort;
import com.hexabank.transfer.application.port.out.LoadTransferPort;
import com.hexabank.transfer.application.port.out.ProcessedEventPort;
import com.hexabank.transfer.application.port.out.SaveTransferPort;
import com.hexabank.transfer.domain.Transfer;
import com.hexabank.transfer.domain.TransferStatus;
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

    TransferService service;

    private final UUID source = UUID.randomUUID();
    private final UUID destination = UUID.randomUUID();
    private final UUID eventId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new TransferService(loadTransferPort, saveTransferPort, eventPublisher, processedEvents);
    }

    @Test
    @DisplayName("requestTransfer persiste PENDING y publica DebitRequested")
    void requestTransferPublishesDebitRequested() {
        when(saveTransferPort.save(any(Transfer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.requestTransfer(new RequestTransferCommand(source, destination, new BigDecimal("50.00")));

        ArgumentCaptor<Transfer> savedCaptor = ArgumentCaptor.forClass(Transfer.class);
        verify(saveTransferPort).save(savedCaptor.capture());
        assertThat(savedCaptor.getValue().status()).isEqualTo(TransferStatus.PENDING);

        DomainEvent published = capturePublished();
        assertThat(published).isInstanceOf(DebitRequested.class);
        assertThat(((DebitRequested) published).accountId()).isEqualTo(source);
        assertThat(((DebitRequested) published).amount()).isEqualByComparingTo("50.00");
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

        verifyNoInteractions(loadTransferPort, saveTransferPort, eventPublisher);
        verify(processedEvents, never()).markProcessed(any());
    }

    private DomainEvent capturePublished() {
        ArgumentCaptor<DomainEvent> captor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher).publish(captor.capture());
        return captor.getValue();
    }
}

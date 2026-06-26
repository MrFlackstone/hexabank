package com.hexabank.transfer.application.service;

import com.hexabank.shared.events.CreditRequested;
import com.hexabank.shared.events.DebitRequested;
import com.hexabank.shared.events.RefundRequested;
import com.hexabank.shared.events.TransferCompleted;
import com.hexabank.shared.events.TransferFailed;
import com.hexabank.transfer.application.port.in.GetTransferUseCase;
import com.hexabank.transfer.application.port.in.RequestTransferCommand;
import com.hexabank.transfer.application.port.in.RequestTransferUseCase;
import com.hexabank.transfer.application.port.in.TransferSagaUseCase;
import com.hexabank.transfer.application.port.out.EventPublisherPort;
import com.hexabank.transfer.application.port.out.LoadTransferPort;
import com.hexabank.transfer.application.port.out.ProcessedEventPort;
import com.hexabank.transfer.application.port.out.SaveTransferPort;
import com.hexabank.transfer.domain.Transfer;
import com.hexabank.transfer.domain.TransferId;
import com.hexabank.transfer.domain.exception.TransferNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Orquestador de la saga de transferencia: crea la transferencia, reacciona a los eventos de
 * account-service haciendo avanzar la máquina de estados y publica el siguiente comando o el evento
 * terminal.
 *
 * <p>Las reacciones de la saga son idempotentes (ignoran un {@code eventId} ya procesado) y se ejecutan
 * en una transacción que abarca el cambio de estado y el registro de idempotencia.</p>
 */
public class TransferService implements RequestTransferUseCase, GetTransferUseCase, TransferSagaUseCase {

    private final LoadTransferPort loadTransferPort;
    private final SaveTransferPort saveTransferPort;
    private final EventPublisherPort eventPublisher;
    private final ProcessedEventPort processedEvents;

    public TransferService(LoadTransferPort loadTransferPort,
                           SaveTransferPort saveTransferPort,
                           EventPublisherPort eventPublisher,
                           ProcessedEventPort processedEvents) {
        this.loadTransferPort = loadTransferPort;
        this.saveTransferPort = saveTransferPort;
        this.eventPublisher = eventPublisher;
        this.processedEvents = processedEvents;
    }

    @Override
    @Transactional
    public Transfer requestTransfer(RequestTransferCommand command) {
        Transfer transfer = saveTransferPort.save(Transfer.request(
                command.sourceAccountId(), command.destinationAccountId(), command.amount()));
        eventPublisher.publish(new DebitRequested(
                UUID.randomUUID(), transfer.id().value(), transfer.sourceAccountId(), transfer.amount()));
        return transfer;
    }

    @Override
    @Transactional(readOnly = true)
    public Transfer getTransfer(TransferId id) {
        return load(id);
    }

    @Override
    @Transactional
    public void onMoneyDebited(UUID eventId, UUID transferId) {
        if (processedEvents.alreadyProcessed(eventId)) {
            return;
        }
        Transfer transfer = load(new TransferId(transferId));
        transfer.markDebited();
        saveTransferPort.save(transfer);
        processedEvents.markProcessed(eventId);
        eventPublisher.publish(new CreditRequested(
                UUID.randomUUID(), transferId, transfer.destinationAccountId(), transfer.amount()));
    }

    @Override
    @Transactional
    public void onDebitFailed(UUID eventId, UUID transferId, String reason) {
        if (processedEvents.alreadyProcessed(eventId)) {
            return;
        }
        Transfer transfer = load(new TransferId(transferId));
        transfer.markDebitFailed();
        saveTransferPort.save(transfer);
        processedEvents.markProcessed(eventId);
        eventPublisher.publish(new TransferFailed(UUID.randomUUID(), transferId, reason));
    }

    @Override
    @Transactional
    public void onMoneyCredited(UUID eventId, UUID transferId) {
        if (processedEvents.alreadyProcessed(eventId)) {
            return;
        }
        Transfer transfer = load(new TransferId(transferId));
        transfer.markCompleted();
        saveTransferPort.save(transfer);
        processedEvents.markProcessed(eventId);
        eventPublisher.publish(new TransferCompleted(UUID.randomUUID(), transferId));
    }

    @Override
    @Transactional
    public void onCreditFailed(UUID eventId, UUID transferId, String reason) {
        if (processedEvents.alreadyProcessed(eventId)) {
            return;
        }
        Transfer transfer = load(new TransferId(transferId));
        transfer.markCompensating();
        saveTransferPort.save(transfer);
        processedEvents.markProcessed(eventId);
        eventPublisher.publish(new RefundRequested(
                UUID.randomUUID(), transferId, transfer.sourceAccountId(), transfer.amount()));
    }

    @Override
    @Transactional
    public void onMoneyRefunded(UUID eventId, UUID transferId) {
        if (processedEvents.alreadyProcessed(eventId)) {
            return;
        }
        Transfer transfer = load(new TransferId(transferId));
        transfer.markRefunded();
        saveTransferPort.save(transfer);
        processedEvents.markProcessed(eventId);
        eventPublisher.publish(new TransferFailed(
                UUID.randomUUID(), transferId, "Transferencia revertida: el crédito al destino falló"));
    }

    private Transfer load(TransferId id) {
        return loadTransferPort.findById(id).orElseThrow(() -> new TransferNotFoundException(id));
    }
}

package com.hexabank.transfer.application.service;

import com.hexabank.shared.events.CreditRequested;
import com.hexabank.shared.events.DebitRequested;
import com.hexabank.shared.events.RefundRequested;
import com.hexabank.shared.events.TransferCompleted;
import com.hexabank.shared.events.TransferFailed;
import com.hexabank.transfer.application.port.in.CallerContext;
import com.hexabank.transfer.application.port.in.GetTransferUseCase;
import com.hexabank.transfer.application.port.in.PageResult;
import com.hexabank.transfer.application.port.in.RequestTransferCommand;
import com.hexabank.transfer.application.port.in.ReapStaleTransfersUseCase;
import com.hexabank.transfer.application.port.in.RequestTransferUseCase;
import com.hexabank.transfer.application.port.in.SearchTransfersUseCase;
import com.hexabank.transfer.application.port.in.TransferSagaUseCase;
import com.hexabank.transfer.application.port.in.TransferSearchQuery;
import com.hexabank.transfer.application.exception.IdempotencyConflictException;
import com.hexabank.transfer.application.port.out.AccountOwnershipPort;
import com.hexabank.transfer.application.port.out.EventPublisherPort;
import com.hexabank.transfer.application.port.out.IdempotencyStorePort;
import com.hexabank.transfer.application.port.out.IdempotencyStorePort.StoredIdempotencyKey;
import com.hexabank.transfer.application.port.out.LoadTransferPort;
import com.hexabank.transfer.application.port.out.ProcessedEventPort;
import com.hexabank.transfer.application.port.out.SaveTransferPort;
import com.hexabank.transfer.application.port.out.SearchTransfersPort;
import com.hexabank.transfer.application.port.out.StaleTransferPort;
import com.hexabank.transfer.application.port.out.TransferMetricsPort;
import com.hexabank.transfer.application.port.out.TransferSearchCriteria;
import com.hexabank.transfer.domain.Transfer;
import com.hexabank.transfer.domain.TransferId;
import com.hexabank.transfer.domain.TransferStatus;
import com.hexabank.transfer.domain.exception.AccountNotFoundException;
import com.hexabank.transfer.domain.exception.TransferNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Orquestador de la saga de transferencia: crea la transferencia, reacciona a los eventos de
 * account-service haciendo avanzar la máquina de estados y publica el siguiente comando o el evento
 * terminal.
 *
 * <p>Las reacciones de la saga son idempotentes (ignoran un {@code eventId} ya procesado) y se ejecutan
 * en una transacción que abarca el cambio de estado y el registro de idempotencia.</p>
 */
public class TransferService implements RequestTransferUseCase, GetTransferUseCase,
        SearchTransfersUseCase, TransferSagaUseCase, ReapStaleTransfersUseCase {

    /** Estados no terminales que el reaper vigila. */
    private static final Set<TransferStatus> NON_TERMINAL_STATES =
            Set.of(TransferStatus.PENDING, TransferStatus.DEBITED, TransferStatus.COMPENSATING);

    private final LoadTransferPort loadTransferPort;
    private final SaveTransferPort saveTransferPort;
    private final EventPublisherPort eventPublisher;
    private final ProcessedEventPort processedEvents;
    private final TransferMetricsPort metrics;
    private final AccountOwnershipPort accountOwnership;
    private final SearchTransfersPort searchTransfersPort;
    private final IdempotencyStorePort idempotencyStore;
    private final StaleTransferPort staleTransferPort;

    public TransferService(LoadTransferPort loadTransferPort,
                           SaveTransferPort saveTransferPort,
                           EventPublisherPort eventPublisher,
                           ProcessedEventPort processedEvents,
                           TransferMetricsPort metrics,
                           AccountOwnershipPort accountOwnership,
                           SearchTransfersPort searchTransfersPort,
                           IdempotencyStorePort idempotencyStore,
                           StaleTransferPort staleTransferPort) {
        this.loadTransferPort = loadTransferPort;
        this.saveTransferPort = saveTransferPort;
        this.eventPublisher = eventPublisher;
        this.processedEvents = processedEvents;
        this.metrics = metrics;
        this.accountOwnership = accountOwnership;
        this.searchTransfersPort = searchTransfersPort;
        this.idempotencyStore = idempotencyStore;
        this.staleTransferPort = staleTransferPort;
    }

    @Override
    @Transactional
    public Transfer requestTransfer(RequestTransferCommand command, CallerContext caller, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return createTransfer(command, caller);
        }
        String requestHash = hash(command);
        Optional<StoredIdempotencyKey> existing = idempotencyStore.find(idempotencyKey);
        if (existing.isPresent()) {
            if (!existing.get().requestHash().equals(requestHash)) {
                throw new IdempotencyConflictException(idempotencyKey);
            }
            // Reintento con la misma clave y payload: se devuelve el transfer ya creado, sin duplicar.
            return load(new TransferId(existing.get().transferId()));
        }
        Transfer transfer = createTransfer(command, caller);
        // La PK única de la clave aísla la carrera concurrente: si otra petición con la misma clave
        // gana, este insert falla y toda la transacción (transfer + outbox) hace rollback → sin duplicado.
        idempotencyStore.save(idempotencyKey, requestHash, transfer.id().value());
        return transfer;
    }

    private Transfer createTransfer(RequestTransferCommand command, CallerContext caller) {
        requireSourceOwnedByCaller(command, caller);
        Transfer transfer = saveTransferPort.save(Transfer.request(
                command.sourceAccountId(), command.destinationAccountId(), command.amount()));
        eventPublisher.publish(new DebitRequested(
                UUID.randomUUID(), transfer.id().value(), transfer.sourceAccountId(), transfer.amount()));
        return transfer;
    }

    /** Huella SHA-256 (hex) del payload, para detectar el reúso de una clave con datos distintos. */
    private String hash(RequestTransferCommand command) {
        String canonical = command.sourceAccountId() + "|" + command.destinationAccountId()
                + "|" + command.amount().stripTrailingZeros().toPlainString();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible en la JVM", e);
        }
    }

    /**
     * Recupera las sagas atascadas creadas antes de {@code olderThan}, forzando la transición segura
     * según su estado no terminal:
     * <ul>
     *   <li>{@code PENDING}: nunca llegó respuesta al débito → se marca {@code FAILED} (aún no se movió
     *       dinero en el caso de timeout esperado).</li>
     *   <li>{@code DEBITED}: se debitó pero el crédito no confirmó → se compensa emitiendo
     *       {@code RefundRequested}.</li>
     *   <li>{@code COMPENSATING}: el reembolso no confirmó → se reemite {@code RefundRequested}. El
     *       reembolso de account-service es idempotente por {@code transferId}, así que reemitir no
     *       duplica el abono.</li>
     * </ul>
     */
    @Override
    @Transactional
    public int reapStaleTransfers(Instant olderThan) {
        List<Transfer> stale = staleTransferPort.findStale(NON_TERMINAL_STATES, olderThan);
        for (Transfer transfer : stale) {
            switch (transfer.status()) {
                case PENDING -> {
                    transfer.markDebitFailed();
                    saveTransferPort.save(transfer);
                    metrics.transferFailed();
                    eventPublisher.publish(new TransferFailed(UUID.randomUUID(), transfer.id().value(),
                            "Saga expirada: sin respuesta al débito dentro del plazo"));
                }
                case DEBITED -> {
                    transfer.markCompensating();
                    saveTransferPort.save(transfer);
                    eventPublisher.publish(new RefundRequested(UUID.randomUUID(), transfer.id().value(),
                            transfer.sourceAccountId(), transfer.amount()));
                }
                case COMPENSATING -> eventPublisher.publish(new RefundRequested(UUID.randomUUID(),
                        transfer.id().value(), transfer.sourceAccountId(), transfer.amount()));
                default -> {
                    // Estados terminales: el reaper no los selecciona; nada que hacer.
                }
            }
            metrics.sagaReaped();
        }
        return stale.size();
    }

    @Override
    @Transactional(readOnly = true)
    public Transfer getTransfer(TransferId id, CallerContext caller) {
        Transfer transfer = load(id);
        // Visible para el dueño del origen o del destino (el receptor ve sus movimientos entrantes).
        // La denegación es un 404 idéntico al no-existente: sin oráculo de existencia.
        if (!caller.staff() && !isInvolved(transfer, caller.subject())) {
            throw new TransferNotFoundException(id);
        }
        return transfer;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Transfer> searchTransfers(TransferSearchQuery query, CallerContext caller) {
        Set<UUID> visibleAccounts = null;
        if (!caller.staff()) {
            visibleAccounts = accountOwnership.accountsOwnedBy(caller.subject());
            if (visibleAccounts.isEmpty()) {
                // Sin cuentas propias no hay nada visible: se evita la consulta.
                return PageResult.empty(query.page(), query.size());
            }
        }
        return searchTransfersPort.search(new TransferSearchCriteria(
                query.status(), query.from(), query.to(), visibleAccounts,
                query.page(), query.size(), query.sortBy(), query.ascending()));
    }

    /**
     * La cuenta origen debe pertenecer al llamante (es a quien se debita). Cuenta desconocida por la
     * proyección o de otro usuario → mismo 404, para no revelar existencia. STAFF puede operar
     * cualquier cuenta.
     */
    private void requireSourceOwnedByCaller(RequestTransferCommand command, CallerContext caller) {
        if (caller.staff()) {
            return;
        }
        boolean owned = accountOwnership.ownerOf(command.sourceAccountId())
                .map(owner -> owner.equals(caller.subject()))
                .orElse(false);
        if (!owned) {
            throw new AccountNotFoundException(command.sourceAccountId());
        }
    }

    private boolean isInvolved(Transfer transfer, String subject) {
        return accountOwnership.ownerOf(transfer.sourceAccountId()).map(subject::equals).orElse(false)
                || accountOwnership.ownerOf(transfer.destinationAccountId()).map(subject::equals).orElse(false);
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
        metrics.transferFailed();
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
        metrics.transferCompleted();
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
        metrics.transferFailed();
        eventPublisher.publish(new TransferFailed(
                UUID.randomUUID(), transferId, "Transferencia revertida: el crédito al destino falló"));
    }

    private Transfer load(TransferId id) {
        return loadTransferPort.findById(id).orElseThrow(() -> new TransferNotFoundException(id));
    }
}

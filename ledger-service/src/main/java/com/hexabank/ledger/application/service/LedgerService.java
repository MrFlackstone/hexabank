package com.hexabank.ledger.application.service;

import com.hexabank.ledger.application.port.in.CallerContext;
import com.hexabank.ledger.application.port.in.GetTransferViewUseCase;
import com.hexabank.ledger.application.port.in.QueryLedgerUseCase;
import com.hexabank.ledger.application.port.in.RecordLedgerUseCase;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Servicio de aplicación del ledger: materializa los eventos de la saga en el libro mayor append-only y
 * en la proyección de transferencias, y resuelve las consultas de lectura.
 *
 * <p>Clase plana (sin {@code @Service}): se cablea explícitamente en {@code config/BeanConfiguration},
 * de modo que la capa de aplicación no depende de anotaciones de Spring (inversión de dependencias).</p>
 *
 * <p>Cada ingesta es idempotente: si el {@code eventId} ya se procesó, no hace nada. La guarda, el
 * registro del asiento, la actualización de la proyección y la marca de procesado ocurren en la
 * <strong>misma transacción</strong>, por lo que un reintento del consumidor no duplica efectos.</p>
 */
public class LedgerService implements RecordLedgerUseCase, QueryLedgerUseCase, GetTransferViewUseCase {

    private static final int RECENT_LIMIT = 100;

    private final LedgerEntryRepositoryPort ledgerEntries;
    private final TransferViewRepositoryPort transferViews;
    private final ProcessedEventPort processedEvents;
    private final LedgerBroadcastPort broadcast;
    private final AccountOwnershipPort accountOwnership;

    public LedgerService(LedgerEntryRepositoryPort ledgerEntries,
                         TransferViewRepositoryPort transferViews,
                         ProcessedEventPort processedEvents,
                         LedgerBroadcastPort broadcast,
                         AccountOwnershipPort accountOwnership) {
        this.ledgerEntries = ledgerEntries;
        this.transferViews = transferViews;
        this.processedEvents = processedEvents;
        this.broadcast = broadcast;
        this.accountOwnership = accountOwnership;
    }

    // ===== Ingesta (RecordLedgerUseCase) =====

    @Override
    @Transactional
    public void onMoneyDebited(UUID eventId, UUID transferId, UUID accountId, BigDecimal amount) {
        if (processedEvents.alreadyProcessed(eventId)) {
            return;
        }
        appendEntry(eventId, transferId, accountId, EntryType.DEBIT, amount);
        TransferView view = transferViews.find(transferId).orElseGet(() -> TransferView.pending(transferId));
        view.recordDebit(accountId, amount);
        transferViews.save(view);
        processedEvents.markProcessed(eventId);
    }

    @Override
    @Transactional
    public void onMoneyCredited(UUID eventId, UUID transferId, UUID accountId, BigDecimal amount) {
        if (processedEvents.alreadyProcessed(eventId)) {
            return;
        }
        appendEntry(eventId, transferId, accountId, EntryType.CREDIT, amount);
        TransferView view = transferViews.find(transferId).orElseGet(() -> TransferView.pending(transferId));
        view.recordCredit(accountId);
        transferViews.save(view);
        processedEvents.markProcessed(eventId);
    }

    @Override
    @Transactional
    public void onMoneyRefunded(UUID eventId, UUID transferId, UUID accountId, BigDecimal amount) {
        if (processedEvents.alreadyProcessed(eventId)) {
            return;
        }
        appendEntry(eventId, transferId, accountId, EntryType.REFUND, amount);
        processedEvents.markProcessed(eventId);
    }

    @Override
    @Transactional
    public void onTransferCompleted(UUID eventId, UUID transferId) {
        if (processedEvents.alreadyProcessed(eventId)) {
            return;
        }
        TransferView view = transferViews.find(transferId).orElseGet(() -> TransferView.pending(transferId));
        view.markCompleted();
        transferViews.save(view);
        processedEvents.markProcessed(eventId);
    }

    @Override
    @Transactional
    public void onTransferFailed(UUID eventId, UUID transferId) {
        if (processedEvents.alreadyProcessed(eventId)) {
            return;
        }
        TransferView view = transferViews.find(transferId).orElseGet(() -> TransferView.pending(transferId));
        view.markFailed();
        transferViews.save(view);
        processedEvents.markProcessed(eventId);
    }

    @Override
    @Transactional
    public void onAccountOpened(UUID eventId, UUID accountId, String ownerId) {
        if (processedEvents.alreadyProcessed(eventId)) {
            return;
        }
        accountOwnership.register(accountId, ownerId);
        processedEvents.markProcessed(eventId);
    }

    private void appendEntry(UUID eventId, UUID transferId, UUID accountId, EntryType type, BigDecimal amount) {
        LedgerEntry entry = LedgerEntry.of(eventId, transferId, accountId, type, amount);
        ledgerEntries.append(entry);
        broadcast.broadcast(entry, accountOwnership.ownerOf(accountId).orElse(null));
    }

    // ===== Consulta (QueryLedgerUseCase / GetTransferViewUseCase) =====

    @Override
    @Transactional(readOnly = true)
    public List<LedgerEntry> recentEntries() {
        return ledgerEntries.findRecent(RECENT_LIMIT);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LedgerEntry> entriesForAccount(UUID accountId, CallerContext caller) {
        // Denegación indistinguible del no-existente (404): no se revela que la cuenta de otro existe.
        if (!caller.staff() && !isOwnedBy(accountId, caller.subject())) {
            throw new AccountNotFoundException(accountId);
        }
        return ledgerEntries.findByAccount(accountId, RECENT_LIMIT);
    }

    @Override
    @Transactional(readOnly = true)
    public TransferView getTransfer(UUID transferId, CallerContext caller) {
        TransferView view = transferViews.find(transferId)
                .orElseThrow(() -> new TransferViewNotFoundException(transferId));
        // Visible para el dueño del origen o del destino; para el resto es un 404 idéntico.
        if (!caller.staff() && !isInvolved(view, caller.subject())) {
            throw new TransferViewNotFoundException(transferId);
        }
        return view;
    }

    private boolean isOwnedBy(UUID accountId, String subject) {
        return accountId != null
                && accountOwnership.ownerOf(accountId).map(subject::equals).orElse(false);
    }

    private boolean isInvolved(TransferView view, String subject) {
        return isOwnedBy(view.sourceAccountId(), subject) || isOwnedBy(view.destinationAccountId(), subject);
    }
}

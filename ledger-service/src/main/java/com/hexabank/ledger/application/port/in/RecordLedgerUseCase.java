package com.hexabank.ledger.application.port.in;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Puerto de entrada de ingesta: materializa los eventos de la saga en el libro mayor y la proyección.
 * El adaptador de mensajería traduce cada evento de Kafka a una de estas llamadas.
 */
public interface RecordLedgerUseCase {

    void onMoneyDebited(UUID eventId, UUID transferId, UUID accountId, BigDecimal amount);

    void onMoneyCredited(UUID eventId, UUID transferId, UUID accountId, BigDecimal amount);

    void onMoneyRefunded(UUID eventId, UUID transferId, UUID accountId, BigDecimal amount);

    void onTransferCompleted(UUID eventId, UUID transferId);

    void onTransferFailed(UUID eventId, UUID transferId);
}

package com.hexabank.transfer.application.port.in;

import java.util.UUID;

/**
 * Puerto de entrada: reacciones de la saga a los eventos de account-service.
 *
 * <p>Cada método hace avanzar la máquina de estados de la transferencia y publica el siguiente comando
 * o el evento terminal. Recibe el {@code eventId} del evento original para garantizar idempotencia.</p>
 */
public interface TransferSagaUseCase {

    void onMoneyDebited(UUID eventId, UUID transferId);

    void onDebitFailed(UUID eventId, UUID transferId, String reason);

    void onMoneyCredited(UUID eventId, UUID transferId);

    void onCreditFailed(UUID eventId, UUID transferId, String reason);

    void onMoneyRefunded(UUID eventId, UUID transferId);
}

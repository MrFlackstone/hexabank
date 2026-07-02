package com.hexabank.transfer.application.port.in;

import java.util.UUID;

/**
 * Puerto de entrada: mantiene la proyección local de propietarios de cuenta a partir de los eventos
 * {@code AccountOpened} consumidos de {@code account-events}.
 */
public interface AccountProjectionUseCase {

    void onAccountOpened(UUID eventId, UUID accountId, String ownerId);
}

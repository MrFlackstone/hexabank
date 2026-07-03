package com.hexabank.ledger.application.port.out;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida sobre la proyección local de propietarios de cuenta ({@code account_ownership}),
 * materializada desde los eventos {@code AccountOpened} de account-service.
 */
public interface AccountOwnershipPort {

    /** Registra (o re-registra, idempotente) el propietario de una cuenta. */
    void register(UUID accountId, String ownerId);

    /** Propietario de la cuenta, si la proyección la conoce. */
    Optional<String> ownerOf(UUID accountId);
}

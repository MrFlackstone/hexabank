package com.hexabank.transfer.application.port.out;

import java.util.Optional;
import java.util.Set;
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

    /** Cuentas cuyo propietario es el sujeto dado. */
    Set<UUID> accountsOwnedBy(String ownerId);
}

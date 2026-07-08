package com.hexabank.transfer.application.port.out;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida del almacén de claves de idempotencia de cliente (cabecera {@code Idempotency-Key}
 * de {@code POST /transfers}).
 *
 * <p>Permite que un reintento del cliente con la misma clave devuelva el transfer ya creado en lugar
 * de crear uno nuevo. El {@code requestHash} guardado permite detectar el reúso de la clave con un
 * payload distinto.</p>
 */
public interface IdempotencyStorePort {

    Optional<StoredIdempotencyKey> find(String idempotencyKey);

    void save(String idempotencyKey, String requestHash, UUID transferId);

    /** Registro asociado a una clave de idempotencia ya vista. */
    record StoredIdempotencyKey(String requestHash, UUID transferId) {
    }
}

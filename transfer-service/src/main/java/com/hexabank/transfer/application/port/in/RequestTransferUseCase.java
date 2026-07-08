package com.hexabank.transfer.application.port.in;

import com.hexabank.transfer.domain.Transfer;

/**
 * Puerto de entrada: caso de uso "solicitar una transferencia".
 *
 * <p>Autorizado por propiedad: un llamante sin rol STAFF solo puede transferir desde una cuenta de
 * la que sea propietario; la denegación es indistinguible de "cuenta inexistente" (404).</p>
 */
public interface RequestTransferUseCase {

    /**
     * Solicita una transferencia con soporte opcional de idempotencia de cliente.
     *
     * @param idempotencyKey clave de idempotencia (cabecera {@code Idempotency-Key}); si es
     *                       {@code null} o vacía no se deduplica. Un reintento con la misma clave y
     *                       payload devuelve el transfer ya creado; con payload distinto lanza
     *                       {@code IdempotencyConflictException}.
     */
    Transfer requestTransfer(RequestTransferCommand command, CallerContext caller, String idempotencyKey);

    /** Variante sin idempotencia (equivale a pasar una clave {@code null}). */
    default Transfer requestTransfer(RequestTransferCommand command, CallerContext caller) {
        return requestTransfer(command, caller, null);
    }
}

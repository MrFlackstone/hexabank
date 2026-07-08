package com.hexabank.account.application.port.out;

import java.util.UUID;

/**
 * Puerto de salida para la idempotencia del reembolso por transferencia: asegura que una misma
 * transferencia no se reembolsa dos veces aunque llegue más de un {@code RefundRequested}.
 */
public interface RefundedTransferPort {

    /** Devuelve {@code true} si la transferencia ya fue reembolsada. */
    boolean alreadyRefunded(UUID transferId);

    /** Marca la transferencia como reembolsada. */
    void markRefunded(UUID transferId);
}

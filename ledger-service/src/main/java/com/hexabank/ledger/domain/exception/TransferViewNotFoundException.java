package com.hexabank.ledger.domain.exception;

import java.util.UUID;

/**
 * Se solicitó la proyección de una transferencia que el ledger todavía no ha observado.
 */
public class TransferViewNotFoundException extends RuntimeException {

    public TransferViewNotFoundException(UUID transferId) {
        super("No existe proyección para la transferencia " + transferId);
    }
}

package com.hexabank.transfer.domain.exception;

import com.hexabank.transfer.domain.TransferId;

/**
 * Se lanza cuando se solicita una transferencia que no existe.
 */
public class TransferNotFoundException extends RuntimeException {

    public TransferNotFoundException(TransferId transferId) {
        super("No existe la transferencia " + transferId.value());
    }
}

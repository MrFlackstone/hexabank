package com.hexabank.transfer.domain.exception;

import com.hexabank.transfer.domain.TransferStatus;

/**
 * Se lanza cuando se intenta una transición de estado no permitida sobre una transferencia.
 */
public class InvalidTransferStateException extends RuntimeException {

    public InvalidTransferStateException(TransferStatus from, TransferStatus to) {
        super("Transición de estado no permitida: " + from + " -> " + to);
    }
}

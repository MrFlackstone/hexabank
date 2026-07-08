package com.hexabank.transfer.application.exception;

/**
 * Se lanza cuando una cabecera {@code Idempotency-Key} ya vista se reutiliza con un payload distinto
 * al original. Semánticamente es un conflicto de contenido → HTTP 422.
 */
public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException(String idempotencyKey) {
        super("La Idempotency-Key '" + idempotencyKey + "' ya se usó con un payload distinto");
    }
}

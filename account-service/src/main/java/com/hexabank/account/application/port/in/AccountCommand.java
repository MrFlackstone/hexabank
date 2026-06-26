package com.hexabank.account.application.port.in;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Comando de aplicación para una operación sobre el saldo de una cuenta (débito, crédito o reembolso).
 *
 * @param eventId    identificador del evento original; clave de idempotencia.
 * @param transferId transferencia a la que pertenece la operación.
 * @param accountId  cuenta sobre la que se opera.
 * @param amount     importe de la operación.
 */
public record AccountCommand(UUID eventId, UUID transferId, UUID accountId, BigDecimal amount) {
}

package com.hexabank.transfer.application.port.in;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Comando de entrada para solicitar una transferencia.
 *
 * @param sourceAccountId      cuenta origen (se debita).
 * @param destinationAccountId cuenta destino (se acredita).
 * @param amount               importe a transferir.
 */
public record RequestTransferCommand(UUID sourceAccountId, UUID destinationAccountId, BigDecimal amount) {
}

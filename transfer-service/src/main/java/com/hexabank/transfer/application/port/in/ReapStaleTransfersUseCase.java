package com.hexabank.transfer.application.port.in;

import java.time.Instant;

/**
 * Puerto de entrada: recupera las sagas atascadas (sin respuesta dentro del plazo) forzando la
 * transición adecuada según su estado.
 *
 * <p>Lo dispara un planificador de infraestructura; la lógica de recuperación vive en la aplicación.</p>
 */
public interface ReapStaleTransfersUseCase {

    /**
     * Procesa las transferencias en estado no terminal creadas antes de {@code olderThan}.
     *
     * @return número de transferencias recuperadas.
     */
    int reapStaleTransfers(Instant olderThan);
}

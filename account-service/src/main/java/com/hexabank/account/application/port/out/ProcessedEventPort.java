package com.hexabank.account.application.port.out;

import java.util.UUID;

/**
 * Puerto de salida para la idempotencia del consumidor: registra qué eventos se han procesado para
 * no aplicar dos veces el mismo efecto.
 */
public interface ProcessedEventPort {

    /** Devuelve {@code true} si el evento ya fue procesado. */
    boolean alreadyProcessed(UUID eventId);

    /** Marca el evento como procesado. */
    void markProcessed(UUID eventId);
}

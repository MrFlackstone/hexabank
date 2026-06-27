package com.hexabank.ledger.application.port.out;

import java.util.UUID;

/**
 * Puerto de salida de idempotencia: registra qué eventos ya se materializaron para tolerar el
 * at-least-once de Kafka.
 */
public interface ProcessedEventPort {

    boolean alreadyProcessed(UUID eventId);

    void markProcessed(UUID eventId);
}

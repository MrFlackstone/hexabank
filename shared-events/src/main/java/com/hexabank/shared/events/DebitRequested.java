package com.hexabank.shared.events;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Comando: solicita debitar {@code amount} de la cuenta {@code accountId}.
 */
public record DebitRequested(UUID eventId, UUID transferId, UUID accountId, BigDecimal amount)
        implements DomainEvent {
}

package com.hexabank.shared.events;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Evento: se acreditó {@code amount} en la cuenta {@code accountId}.
 */
public record MoneyCredited(UUID eventId, UUID transferId, UUID accountId, BigDecimal amount)
        implements DomainEvent {
}

package com.hexabank.shared.events;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Evento: se devolvió {@code amount} a la cuenta {@code accountId} (resultado de una compensación).
 */
public record MoneyRefunded(UUID eventId, UUID transferId, UUID accountId, BigDecimal amount)
        implements DomainEvent {
}

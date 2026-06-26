package com.hexabank.shared.events;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Evento: se debitó {@code amount} de la cuenta {@code accountId}.
 */
public record MoneyDebited(UUID eventId, UUID transferId, UUID accountId, BigDecimal amount)
        implements DomainEvent {
}

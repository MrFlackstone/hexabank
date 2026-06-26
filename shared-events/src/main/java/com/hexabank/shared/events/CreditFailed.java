package com.hexabank.shared.events;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Evento: el crédito de {@code amount} en la cuenta {@code accountId} no pudo aplicarse.
 * {@code reason} describe la causa.
 */
public record CreditFailed(UUID eventId, UUID transferId, UUID accountId, BigDecimal amount, String reason)
        implements DomainEvent {
}

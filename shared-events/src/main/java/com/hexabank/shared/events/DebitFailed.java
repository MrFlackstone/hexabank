package com.hexabank.shared.events;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Evento: el débito de {@code amount} en la cuenta {@code accountId} no pudo aplicarse.
 * {@code reason} describe la causa.
 */
public record DebitFailed(UUID eventId, UUID transferId, UUID accountId, BigDecimal amount, String reason)
        implements DomainEvent {
}

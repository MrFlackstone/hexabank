package com.hexabank.shared.events;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Comando de compensación: solicita devolver {@code amount} a la cuenta {@code accountId}.
 */
public record RefundRequested(UUID eventId, UUID transferId, UUID accountId, BigDecimal amount)
        implements DomainEvent {
}

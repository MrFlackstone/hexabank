package com.hexabank.shared.events;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Comando: solicita acreditar {@code amount} en la cuenta {@code accountId}.
 */
public record CreditRequested(UUID eventId, UUID transferId, UUID accountId, BigDecimal amount)
        implements DomainEvent {
}

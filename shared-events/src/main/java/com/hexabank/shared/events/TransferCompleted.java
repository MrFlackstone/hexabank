package com.hexabank.shared.events;

import java.util.UUID;

/**
 * Evento terminal: la transferencia {@code transferId} se completó con éxito.
 */
public record TransferCompleted(UUID eventId, UUID transferId) implements DomainEvent {
}

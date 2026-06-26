package com.hexabank.shared.events;

import java.util.UUID;

/**
 * Evento terminal: la transferencia {@code transferId} terminó en fallo. {@code reason} describe la causa.
 */
public record TransferFailed(UUID eventId, UUID transferId, String reason) implements DomainEvent {
}

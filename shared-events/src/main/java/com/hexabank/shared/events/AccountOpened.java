package com.hexabank.shared.events;

import java.util.UUID;

/**
 * Hecho: se ha abierto la cuenta {@code accountId} propiedad del usuario {@code ownerId}
 * (claim {@code sub} del token del creador).
 *
 * <p>Los servicios consumidores materializan con él su proyección local de propietarios
 * ({@code account_ownership}) para autorizar operaciones sin consultas síncronas entre servicios.</p>
 */
public record AccountOpened(UUID eventId, UUID accountId, String ownerId)
        implements DomainEvent {
}

package com.hexabank.transfer.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object de identidad de una transferencia.
 *
 * <p>Envuelve un {@link UUID} en un tipo propio del dominio para dar seguridad de tipos y un punto
 * único de validación de la identidad.</p>
 */
public record TransferId(UUID value) {

    public TransferId {
        Objects.requireNonNull(value, "El identificador de transferencia no puede ser nulo");
    }

    /** Genera una identidad nueva (la asigna el dominio al crear la transferencia). */
    public static TransferId newId() {
        return new TransferId(UUID.randomUUID());
    }

    /** Reconstruye la identidad desde su representación textual (p. ej. un path param REST). */
    public static TransferId of(String value) {
        return new TransferId(UUID.fromString(value));
    }
}

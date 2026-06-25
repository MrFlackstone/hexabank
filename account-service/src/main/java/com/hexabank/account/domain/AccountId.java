package com.hexabank.account.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object de identidad de una cuenta.
 *
 * <p>Envuelve un {@link UUID} en un tipo propio del dominio en lugar de pasar UUID/String "desnudos".
 * Esto da seguridad de tipos (no se puede confundir un id de cuenta con cualquier otro id) y un punto
 * único donde validar el formato de la identidad.</p>
 */
public record AccountId(UUID value) {

    public AccountId {
        Objects.requireNonNull(value, "El identificador de cuenta no puede ser nulo");
    }

    /** Genera una identidad nueva (la asigna el dominio al crear la cuenta). */
    public static AccountId newId() {
        return new AccountId(UUID.randomUUID());
    }

    /** Reconstruye la identidad desde su representación textual (p. ej. un path param REST). */
    public static AccountId of(String value) {
        return new AccountId(UUID.fromString(value));
    }
}

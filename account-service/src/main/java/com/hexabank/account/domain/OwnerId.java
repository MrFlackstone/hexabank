package com.hexabank.account.domain;

import java.util.Objects;

/**
 * Value Object de identidad del propietario de una cuenta.
 *
 * <p>Envuelve el claim {@code sub} del token del usuario que abrió la cuenta. Un tipo propio evita
 * confundirlo con otros String del agregado (p. ej. {@code holder}, que es un dato descriptivo,
 * no una identidad).</p>
 */
public record OwnerId(String value) {

    public OwnerId {
        Objects.requireNonNull(value, "El propietario de la cuenta no puede ser nulo");
        if (value.isBlank()) {
            throw new IllegalArgumentException("El propietario de la cuenta no puede estar en blanco");
        }
    }

    /** Reconstruye la identidad desde el claim {@code sub} del JWT. */
    public static OwnerId of(String value) {
        return new OwnerId(value);
    }
}

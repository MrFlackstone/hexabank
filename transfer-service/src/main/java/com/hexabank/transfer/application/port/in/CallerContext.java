package com.hexabank.transfer.application.port.in;

import java.util.Objects;

/**
 * Identidad del llamante de un caso de uso: el {@code sub} del token y si tiene el rol STAFF.
 *
 * <p>Es un contrato plano de la aplicación (sin Spring Security): el adaptador REST lo construye a
 * partir de la {@code Authentication} y la aplicación decide con él la autorización por propiedad.</p>
 */
public record CallerContext(String subject, boolean staff) {

    public CallerContext {
        Objects.requireNonNull(subject, "subject");
    }

    /** Llamante cliente (rol USER): solo opera sobre sus propias cuentas. */
    public static CallerContext user(String subject) {
        return new CallerContext(subject, false);
    }

    /** Llamante con rol STAFF: acceso a todas las cuentas. */
    public static CallerContext staff(String subject) {
        return new CallerContext(subject, true);
    }
}

package com.hexabank.account.infrastructure.rest;

import com.hexabank.account.application.port.in.CallerContext;
import org.springframework.security.core.Authentication;

/**
 * Traduce la {@code Authentication} de Spring Security al {@link CallerContext} de la aplicación:
 * {@code sub} del JWT como sujeto y presencia de {@code ROLE_STAFF} como marca de staff.
 */
final class CallerContexts {

    private static final String STAFF_AUTHORITY = "ROLE_STAFF";

    private CallerContexts() {
    }

    static CallerContext from(Authentication authentication) {
        boolean staff = authentication.getAuthorities().stream()
                .anyMatch(authority -> STAFF_AUTHORITY.equals(authority.getAuthority()));
        return new CallerContext(authentication.getName(), staff);
    }
}

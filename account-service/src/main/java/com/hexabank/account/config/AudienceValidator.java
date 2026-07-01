package com.hexabank.account.config;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Valida que el access token trae la audiencia esperada (claim {@code aud}). Sin esta comprobacion un
 * token legitimo emitido por Keycloak para OTRO cliente/servicio seria aceptado aqui. Keycloak inyecta
 * {@code aud=hexabank-api} mediante un protocol-mapper definido en el realm (realm-as-code).
 */
public record AudienceValidator(String audience) implements OAuth2TokenValidator<Jwt> {

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        if (jwt.getAudience() != null && jwt.getAudience().contains(audience)) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", "Falta la audiencia requerida: " + audience, null));
    }
}

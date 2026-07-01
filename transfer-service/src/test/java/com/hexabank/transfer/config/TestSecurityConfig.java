package com.hexabank.transfer.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Seguridad permisiva para los IT que NO verifican autenticacion (p. ej. {@code TransferControllerIT}
 * prueba la publicacion en Kafka, no el 401). Bajo el perfil {@code test} la {@code SecurityConfig} de
 * produccion se desactiva (@Profile("!test")); esta config abre todos los endpoints y aporta un
 * {@link JwtDecoder} ficticio para que la autoconfiguracion de Resource Server no intente contactar con
 * Keycloak en el arranque. La seguridad real se cubre en el IT dedicado con Testcontainers Keycloak.
 */
@TestConfiguration
public class TestSecurityConfig {

    @Bean
    SecurityFilterChain permitAllChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    JwtDecoder noOpJwtDecoder() {
        return token -> {
            throw new UnsupportedOperationException("Perfil test: sin validacion de JWT");
        };
    }
}

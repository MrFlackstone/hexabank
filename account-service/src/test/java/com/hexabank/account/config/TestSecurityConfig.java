package com.hexabank.account.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Seguridad permisiva para los IT que NO verifican autenticacion (p. ej. {@code AccountSagaIT} y
 * {@code AccountPersistenceAdapterIT}, que prueban Kafka/persistencia, no el 401). Bajo el perfil
 * {@code test} la {@code SecurityConfig} de produccion se desactiva (@Profile("!test")); esta config
 * abre todos los endpoints, autentica cada peticion como un usuario STAFF ficticio y aporta un
 * {@link JwtDecoder} ficticio para que la autoconfiguracion de Resource Server no intente contactar
 * con Keycloak en el arranque. La seguridad real se cubre en {@code AccountSecurityIT} con
 * Testcontainers Keycloak.
 */
@TestConfiguration
public class TestSecurityConfig {

    @Bean
    SecurityFilterChain permitAllChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .addFilterAfter(staffPrincipalFilter(), AnonymousAuthenticationFilter.class);
        return http.build();
    }

    /** Autentica toda peticion como "it-staff" con ROLE_STAFF. */
    private OncePerRequestFilter staffPrincipalFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain filterChain) throws ServletException, IOException {
                TestingAuthenticationToken staff =
                        new TestingAuthenticationToken("it-staff", "n/a", "ROLE_STAFF");
                staff.setAuthenticated(true);
                SecurityContextHolder.getContext().setAuthentication(staff);
                filterChain.doFilter(request, response);
            }
        };
    }

    @Bean
    JwtDecoder noOpJwtDecoder() {
        return token -> {
            throw new UnsupportedOperationException("Perfil test: sin validacion de JWT");
        };
    }
}

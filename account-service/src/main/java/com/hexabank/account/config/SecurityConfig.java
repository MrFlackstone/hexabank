package com.hexabank.account.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Seguridad HTTP. El servicio actua como OAuth2 Resource Server: cada peticion REST exige un Bearer
 * JWT valido emitido por Keycloak; sin token -> 401. Los endpoints de Actuator (health/info/prometheus)
 * quedan abiertos: Prometheus los raspa sin token.
 *
 * <p>API stateless: sin sesion HTTP y sin CSRF (no hay cookies de sesion que proteger; el token viaja
 * en cada peticion). Los roles de realm de Keycloak ({@code realm_access.roles}) se mapean a
 * authorities {@code ROLE_*}, habilitando method security ({@code @PreAuthorize}) y las decisiones
 * de autorizacion por rol/propiedad en la capa de aplicacion.</p>
 */
// Solo aplica en apps web servlet (los E2E arrancan los servicios con WebApplicationType.NONE) y fuera
// del perfil "test" (los IT que no verifican seguridad relajan el filtro; ver TestSecurityConfig).
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Profile("!test")
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtDecoder jwtDecoder) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**", "/actuator/info", "/actuator/prometheus").permitAll()
                        // Swagger UI y el JSON OpenAPI son publicos (Fase 13a); el resto exige JWT.
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/v3/api-docs.yaml").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
                        .decoder(jwtDecoder)
                        .jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    /**
     * Traduce los roles de realm de Keycloak (claim {@code realm_access.roles}) a authorities de
     * Spring Security con el prefijo {@code ROLE_} (p. ej. {@code STAFF} -> {@code ROLE_STAFF}).
     */
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        Converter<Jwt, Collection<GrantedAuthority>> realmRoles = jwt -> {
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess == null || !(realmAccess.get("roles") instanceof Collection<?> roles)) {
                return List.of();
            }
            return roles.stream()
                    .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                    .toList();
        };
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(realmRoles);
        return converter;
    }

    /**
     * Decoder JWT. Desacopla que emisor se confia de donde se descargan las claves (split
     * frontchannel/backchannel):
     * <ul>
     *   <li><b>Con {@code jwk-set-uri} (compose):</b> las claves (JWKS) se descargan por el canal
     *       interno ({@code keycloak:8080}), pero el {@code iss} que se valida es el emisor publico
     *       ({@code issuer-uri = localhost:8180}), que es el que lleva el token emitido al navegador.</li>
     *   <li><b>Sin {@code jwk-set-uri} (IT de Testcontainers):</b> resolucion perezosa via
     *       {@code withIssuerLocation}: emisor y JWKS comparten un unico URL dinamico.</li>
     * </ul>
     * En ambos casos se valida firma + {@code iss}/{@code exp} y la audiencia {@code hexabank-api}.
     */
    @Bean
    JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}") String jwkSetUri) {
        NimbusJwtDecoder decoder = (jwkSetUri == null || jwkSetUri.isBlank())
                ? NimbusJwtDecoder.withIssuerLocation(issuerUri).build()
                : NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuerUri),
                new AudienceValidator("hexabank-api")));
        return decoder;
    }
}

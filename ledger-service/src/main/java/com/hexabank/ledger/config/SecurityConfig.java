package com.hexabank.ledger.config;

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
 * <p>Excepcion consciente: {@code GET /ledger/stream} (SSE) queda ABIERTO. El navegador usa
 * {@code EventSource}, que no permite enviar la cabecera {@code Authorization}; protegerlo ahora
 * romperia el ledger en vivo de la UI. Se securizara (token por query-param o proxy autenticado)
 * cuando el frontend incorpore login OIDC.</p>
 *
 * <p>API stateless: sin sesion HTTP y sin CSRF. Los roles de realm de Keycloak
 * ({@code realm_access.roles}) se mapean a authorities {@code ROLE_*}, habilitando method security
 * ({@code @PreAuthorize}) y las decisiones de autorizacion por rol/propiedad en la aplicacion.</p>
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
                        .requestMatchers("/ledger/stream").permitAll()   // SSE abierto por ahora (ver javadoc)
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
     * Decoder que resuelve la clave publica (JWKS) de Keycloak de forma perezosa (en el primer token,
     * no en el arranque: el servicio no depende de que Keycloak este arriba para iniciar). Valida firma
     * + {@code iss}/{@code exp} por defecto y, ademas, la audiencia {@code hexabank-api}.
     */
    @Bean
    JwtDecoder jwtDecoder(@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(issuerUri).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuerUri),
                new AudienceValidator("hexabank-api")));
        return decoder;
    }
}

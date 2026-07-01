package com.hexabank.account.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Seguridad HTTP. El servicio actua como OAuth2 Resource Server: cada peticion REST exige un Bearer
 * JWT valido emitido por Keycloak; sin token -> 401. Los endpoints de Actuator (health/info/prometheus)
 * quedan abiertos: Prometheus los raspa sin token.
 *
 * <p>API stateless: sin sesion HTTP y sin CSRF (no hay cookies de sesion que proteger; el token viaja
 * en cada peticion). La autorizacion fina (ownership/roles) se abordara mas adelante.</p>
 */
// Solo aplica en apps web servlet (los E2E arrancan los servicios con WebApplicationType.NONE) y fuera
// del perfil "test" (los IT que no verifican seguridad relajan el filtro; ver TestSecurityConfig).
@Configuration
@EnableWebSecurity
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
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder)));
        return http.build();
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

package com.hexabank.account.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger UI del account-service (Fase 13a). Publica el contrato de la API {@code /v1} y
 * declara un esquema de seguridad OAuth2 (authorization-code + PKCE) contra Keycloak: el boton
 * "Authorize" de Swagger UI dispara el login OIDC y adjunta el Bearer resultante a cada peticion.
 *
 * <p>Los tokens los emite el cliente publico {@code hexabank-frontend}, que ya inyecta la
 * audiencia {@code hexabank-api} exigida por el resource server, de modo que el "Try it out"
 * autenticado pasa la validacion sin configuracion extra.
 */
@Configuration
public class OpenApiConfig {

    private static final String OAUTH_SCHEME = "keycloak";
    private static final String AUTH_URL =
            "http://localhost:8180/realms/hexabank/protocol/openid-connect/auth";
    private static final String TOKEN_URL =
            "http://localhost:8180/realms/hexabank/protocol/openid-connect/token";

    @Bean
    public OpenAPI accountOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("HexaBank Account API")
                        .version("1.0")
                        .description("Cuentas y saldos (fuente de verdad). Endpoints versionados bajo /v1.")
                        .contact(new Contact()
                                .name("Diego Pastor Hernandez")
                                .url("https://diegophdev.com")))
                .components(new Components().addSecuritySchemes(OAUTH_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.OAUTH2)
                        .description("OAuth2 authorization-code + PKCE contra Keycloak (realm hexabank).")
                        .flows(new OAuthFlows().authorizationCode(new OAuthFlow()
                                .authorizationUrl(AUTH_URL)
                                .tokenUrl(TOKEN_URL)
                                .scopes(new Scopes()
                                        .addString("openid", "OpenID Connect")
                                        .addString("profile", "Perfil del usuario"))))))
                .addSecurityItem(new SecurityRequirement().addList(OAUTH_SCHEME));
    }
}

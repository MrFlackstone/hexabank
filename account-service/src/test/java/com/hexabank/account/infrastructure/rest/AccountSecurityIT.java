package com.hexabank.account.infrastructure.rest;

import com.hexabank.account.infrastructure.rest.dto.AccountResponse;
import com.hexabank.account.infrastructure.rest.dto.CreateAccountRequest;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test de integracion de seguridad (AuthN). Levanta un Keycloak real con Testcontainers,
 * importa el realm {@code hexabank} y comprueba que el servicio actua como OAuth2 Resource Server:
 * <ul>
 *   <li>peticion REST sin token &rarr; 401,</li>
 *   <li>peticion con un Bearer JWT valido emitido por Keycloak &rarr; pasa a la logica de negocio.</li>
 * </ul>
 * El issuer que valida el servicio y el que emite el token comparten URL (la del contenedor), por lo
 * que no aplica la gotcha de {@code iss}/hostname que si afecta al arranque en Docker Compose.
 * Requiere Docker.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ImportAutoConfiguration(exclude = KafkaAutoConfiguration.class)
class AccountSecurityIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static KeycloakContainer keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:26.0")
            .withRealmImportFile("realm-hexabank.json");

    @DynamicPropertySource
    static void oauthProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                () -> keycloak.getAuthServerUrl() + "/realms/hexabank");
    }

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    @DisplayName("sin token -> 401 Unauthorized")
    void rejectsRequestWithoutToken() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/accounts/" + UUID.randomUUID(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("con JWT valido -> crea (201) y recupera (200) la cuenta")
    void allowsRequestWithValidToken() {
        HttpHeaders auth = new HttpHeaders();
        auth.setBearerAuth(tokenFor("alice", "alice"));

        HttpHeaders postHeaders = new HttpHeaders();
        postHeaders.addAll(auth);
        postHeaders.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<AccountResponse> created = restTemplate.postForEntity("/accounts",
                new HttpEntity<>(new CreateAccountRequest("Alice", new BigDecimal("100.00")), postHeaders),
                AccountResponse.class);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody()).isNotNull();

        ResponseEntity<AccountResponse> fetched = restTemplate.exchange(
                "/accounts/" + created.getBody().id(), HttpMethod.GET,
                new HttpEntity<>(auth), AccountResponse.class);

        assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fetched.getBody()).isNotNull();
        assertThat(fetched.getBody().holder()).isEqualTo("Alice");
    }

    /** Obtiene un access token real de Keycloak por password-grant (client publico hexabank-frontend). */
    @SuppressWarnings("unchecked")
    private String tokenFor(String username, String password) {
        String tokenUrl = keycloak.getAuthServerUrl() + "/realms/hexabank/protocol/openid-connect/token";
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", "hexabank-frontend");
        form.add("username", username);
        form.add("password", password);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        Map<String, Object> body = new RestTemplate()
                .postForObject(tokenUrl, new HttpEntity<>(form, headers), Map.class);

        assertThat(body).isNotNull();
        return (String) body.get("access_token");
    }
}

package com.hexabank.ledger.infrastructure.rest;

import com.hexabank.ledger.infrastructure.messaging.KafkaTopics;
import com.hexabank.ledger.infrastructure.persistence.AccountOwnershipJpaRepository;
import com.hexabank.shared.events.AccountOpened;
import com.hexabank.shared.events.MoneyDebited;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Test de integración de la autorización de consultas del ledger con la pila real: Keycloak (roles de
 * realm en el JWT: valida también el mapeo realm_access → ROLE_*), Kafka (proyección de ownership
 * sembrada con {@code AccountOpened} reales) y Postgres. Cubre el gate de rol del libro completo
 * (403), el 404 anti existence-oracle por cuenta y el SSE autenticado por cabecera Bearer. Requiere Docker.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class LedgerAuthorizationIT {

    private static final UUID ALICE_ACCOUNT = UUID.randomUUID();

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.15-alpine");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka:3.9.1"));

    @Container
    static KeycloakContainer keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:26.8.1")
            .withRealmImportFile("realm-hexabank.json");

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                () -> keycloak.getAuthServerUrl() + "/realms/hexabank");
    }

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    AccountOwnershipJpaRepository ownershipRepository;

    @Autowired
    KafkaTemplate<String, Object> kafkaTemplate;

    private static String aliceSub;

    @Test
    @DisplayName("GET /v1/ledger (libro completo): staff 200, alice 403 (gate puro de rol)")
    void fullLedgerIsStaffOnly() {
        ensureProjectionSeeded();

        assertThat(get("/v1/ledger", "staff").getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(get("/v1/ledger", "alice").getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("GET /v1/ledger?accountId=: dueño 200; otro usuario 404 identico al inexistente")
    void ledgerByAccountEnforcesOwnership() {
        ensureProjectionSeeded();

        assertThat(get("/v1/ledger?accountId=" + ALICE_ACCOUNT, "alice").getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(get("/v1/ledger?accountId=" + ALICE_ACCOUNT, "bob").getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(get("/v1/ledger?accountId=" + ALICE_ACCOUNT, "staff").getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("GET /v1/transfers/{id} (vista): involucrado 200, tercero 404")
    void transferViewEnforcesInvolvement() {
        ensureProjectionSeeded();
        UUID transferId = UUID.randomUUID();
        kafkaTemplate.send(KafkaTopics.ACCOUNT_EVENTS, ALICE_ACCOUNT.toString(),
                new MoneyDebited(UUID.randomUUID(), transferId, ALICE_ACCOUNT, new BigDecimal("10.00")));
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(get("/v1/transfers/" + transferId, "alice").getStatusCode()).isEqualTo(HttpStatus.OK));

        assertThat(get("/v1/transfers/" + transferId, "bob").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(get("/v1/transfers/" + transferId, "staff").getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("SSE /v1/ledger/stream: sin token 401; con cabecera Bearer 200; el query param NO autentica")
    void sseRequiresBearerHeader() throws Exception {
        ensureProjectionSeeded();

        ResponseEntity<String> anonymous = restTemplate.getForEntity("/v1/ledger/stream", String.class);
        assertThat(anonymous.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // El token por query ya no se acepta (se retiro el resolver que lo permitia): sin cabecera -> 401.
        HttpURLConnection queryParam = open("/v1/ledger/stream?access_token=" + tokenFor("alice", "alice"), null);
        try {
            assertThat(queryParam.getResponseCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        } finally {
            queryParam.disconnect();
        }

        // Con la cabecera Authorization: Bearer -> 200 y stream de eventos.
        HttpURLConnection header = open("/v1/ledger/stream", tokenFor("alice", "alice"));
        try {
            assertThat(header.getResponseCode()).isEqualTo(200);
            assertThat(header.getContentType()).contains(MediaType.TEXT_EVENT_STREAM_VALUE);
        } finally {
            header.disconnect();
        }
    }

    /** Abre una conexion GET al puerto del servicio; si {@code bearer} no es null lo manda en Authorization. */
    private HttpURLConnection open(String path, String bearer) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) URI.create("http://localhost:" + port + path)
                .toURL().openConnection();
        connection.setConnectTimeout(5_000);
        connection.setReadTimeout(10_000);
        if (bearer != null) {
            connection.setRequestProperty("Authorization", "Bearer " + bearer);
        }
        return connection;
    }

    /** Publica AccountOpened para la cuenta de alice (una vez) y espera a la proyección. */
    private void ensureProjectionSeeded() {
        if (aliceSub == null) {
            aliceSub = subjectOf("alice");
            kafkaTemplate.send(KafkaTopics.ACCOUNT_EVENTS, ALICE_ACCOUNT.toString(),
                    new AccountOpened(UUID.randomUUID(), ALICE_ACCOUNT, aliceSub));
        }
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(ownershipRepository.existsById(ALICE_ACCOUNT)).isTrue());
    }

    private ResponseEntity<String> get(String path, String username) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenFor(username, username));
        return restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    /** El claim sub del usuario, extraído de su propio access token. */
    private String subjectOf(String username) {
        String payload = tokenFor(username, username).split("\\.")[1];
        String json = new String(Base64.getUrlDecoder().decode(payload));
        return json.replaceAll(".*\"sub\"\\s*:\\s*\"([^\"]+)\".*", "$1");
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

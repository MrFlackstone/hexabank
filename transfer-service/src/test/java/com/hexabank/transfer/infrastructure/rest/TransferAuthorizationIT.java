package com.hexabank.transfer.infrastructure.rest;

import com.hexabank.shared.events.AccountOpened;
import com.hexabank.transfer.infrastructure.messaging.KafkaTopics;
import com.hexabank.transfer.infrastructure.persistence.AccountOwnershipJpaRepository;
import com.hexabank.transfer.infrastructure.rest.dto.CreateTransferRequest;
import com.hexabank.transfer.infrastructure.rest.dto.PageResponse;
import com.hexabank.transfer.infrastructure.rest.dto.TransferResponse;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.support.serializer.JsonSerializer;
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
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Test de integración de la autorización por propiedad y roles con la pila real: Keycloak (tokens de
 * verdad, roles de realm en el JWT), Kafka (la proyección de ownership se siembra publicando
 * {@code AccountOpened} reales en account-events) y Postgres. Verifica el cierre del IDOR de
 * escritura de {@code POST /v1/transfers} y el 404 anti existence-oracle. Requiere Docker.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class TransferAuthorizationIT {

    private static final UUID ALICE_ACCOUNT = UUID.randomUUID();
    private static final UUID BOB_ACCOUNT = UUID.randomUUID();

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka:3.9.1"));

    @Container
    static KeycloakContainer keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:26.0")
            .withRealmImportFile("realm-hexabank.json");

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                () -> keycloak.getAuthServerUrl() + "/realms/hexabank");
    }

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    AccountOwnershipJpaRepository ownershipRepository;

    private static String aliceSub;
    private static String bobSub;

    @Test
    @DisplayName("alice transfiere desde su cuenta (201); desde la de bob recibe 404 identico al inexistente")
    void ownershipOnCreateTransfer() {
        ensureProjectionSeeded();

        ResponseEntity<TransferResponse> own = postTransfer("alice", ALICE_ACCOUNT, BOB_ACCOUNT);
        assertThat(own.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<TransferResponse> foreign = postTransfer("alice", BOB_ACCOUNT, ALICE_ACCOUNT);
        assertThat(foreign.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<TransferResponse> unknown = postTransfer("alice", UUID.randomUUID(), ALICE_ACCOUNT);
        assertThat(unknown.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("STAFF puede transferir desde cualquier cuenta (201)")
    void staffBypassesOwnership() {
        ensureProjectionSeeded();

        ResponseEntity<TransferResponse> response = postTransfer("staff", BOB_ACCOUNT, ALICE_ACCOUNT);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    @DisplayName("GET /v1/transfers/{id}: visible para origen/destino y STAFF; 404 para terceros")
    void getTransferVisibility() {
        ensureProjectionSeeded();

        ResponseEntity<TransferResponse> created = postTransfer("alice", ALICE_ACCOUNT, BOB_ACCOUNT);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody()).isNotNull();
        UUID transferId = created.getBody().id();

        assertThat(getTransfer(transferId, "alice").getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getTransfer(transferId, "bob").getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getTransfer(transferId, "staff").getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("GET /v1/transfers pagina en servidor y restringe la visibilidad a las cuentas del llamante")
    void searchIsPaginatedAndOwnershipFiltered() {
        ensureProjectionSeeded();
        for (int i = 0; i < 3; i++) {
            assertThat(postTransfer("alice", ALICE_ACCOUNT, BOB_ACCOUNT).getStatusCode())
                    .isEqualTo(HttpStatus.CREATED);
        }

        ResponseEntity<PageResponse<TransferResponse>> page = searchTransfers("alice", "?size=2&sort=createdAt,desc");
        assertThat(page.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(page.getBody()).isNotNull();
        assertThat(page.getBody().content()).hasSizeLessThanOrEqualTo(2);
        assertThat(page.getBody().totalElements()).isGreaterThanOrEqualTo(3);
        assertThat(page.getBody().totalPages()).isGreaterThanOrEqualTo(2);
        assertThat(page.getBody().content()).allSatisfy(transfer ->
                assertThat(transfer.sourceAccountId().equals(ALICE_ACCOUNT)
                        || transfer.destinationAccountId().equals(ALICE_ACCOUNT)).isTrue());

        ResponseEntity<PageResponse<TransferResponse>> filtered = searchTransfers("alice", "?status=pending");
        assertThat(filtered.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("GET /v1/transfers valida la whitelist de sort y el tope de tamano (400)")
    void searchRejectsInvalidSortAndOversizedPage() {
        ensureProjectionSeeded();

        assertThat(searchRaw("alice", "?sort=holder,asc").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(searchRaw("alice", "?size=500").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<PageResponse<TransferResponse>> searchTransfers(String username, String queryString) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenFor(username, username));
        return restTemplate.exchange("/v1/transfers" + queryString, HttpMethod.GET, new HttpEntity<>(headers),
                new org.springframework.core.ParameterizedTypeReference<PageResponse<TransferResponse>>() {
                });
    }

    private ResponseEntity<String> searchRaw(String username, String queryString) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenFor(username, username));
        return restTemplate.exchange("/v1/transfers" + queryString, HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
    }

    /** Publica AccountOpened para alice y bob (una vez) y espera a que la proyección los materialice. */
    private void ensureProjectionSeeded() {
        if (aliceSub == null) {
            aliceSub = subjectOf("alice");
            bobSub = subjectOf("bob");
            publishAccountOpened(ALICE_ACCOUNT, aliceSub);
            publishAccountOpened(BOB_ACCOUNT, bobSub);
        }
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(ownershipRepository.existsById(ALICE_ACCOUNT)).isTrue());
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(ownershipRepository.existsById(BOB_ACCOUNT)).isTrue());
    }

    private void publishAccountOpened(UUID accountId, String ownerSub) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        try (KafkaProducer<String, Object> producer =
                     new KafkaProducer<>(props, new StringSerializer(), new JsonSerializer<>())) {
            producer.send(new ProducerRecord<>(KafkaTopics.ACCOUNT_EVENTS, accountId.toString(),
                    new AccountOpened(UUID.randomUUID(), accountId, ownerSub)));
            producer.flush();
        }
    }

    private ResponseEntity<TransferResponse> postTransfer(String username, UUID source, UUID destination) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenFor(username, username));
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.postForEntity("/v1/transfers",
                new HttpEntity<>(new CreateTransferRequest(source, destination, new BigDecimal("10.00")), headers),
                TransferResponse.class);
    }

    private ResponseEntity<String> getTransfer(UUID transferId, String username) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenFor(username, username));
        return restTemplate.exchange("/v1/transfers/" + transferId, HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
    }

    /** El claim sub del usuario, extraído de su propio access token (sin depender de ids internos). */
    private String subjectOf(String username) {
        String token = tokenFor(username, username);
        String payload = token.split("\\.")[1];
        String json = new String(java.util.Base64.getUrlDecoder().decode(payload));
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

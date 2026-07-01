package com.hexabank.transfer.infrastructure.rest;

import com.hexabank.shared.events.DebitRequested;
import com.hexabank.shared.events.DomainEvent;
import com.hexabank.transfer.config.TestSecurityConfig;
import com.hexabank.transfer.infrastructure.messaging.KafkaTopics;
import com.hexabank.transfer.infrastructure.rest.dto.CreateTransferRequest;
import com.hexabank.transfer.infrastructure.rest.dto.TransferResponse;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Test de integración del endpoint de transferencias contra Kafka + Postgres reales (Testcontainers).
 * Verifica que {@code POST /transfers} crea la transferencia en estado {@code PENDING} y publica
 * {@code DebitRequested} en account-commands. Requiere Docker.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")            // desactiva la SecurityConfig de produccion (@Profile("!test"))
@Import(TestSecurityConfig.class)  // ...y la sustituye por seguridad permisiva (este IT prueba Kafka)
class TransferControllerIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static org.testcontainers.kafka.KafkaContainer kafka =
            new org.testcontainers.kafka.KafkaContainer(DockerImageName.parse("apache/kafka:3.9.1"));

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    TestRestTemplate restTemplate;

    private KafkaConsumer<String, DomainEvent> commandConsumer;

    @AfterEach
    void closeConsumer() {
        if (commandConsumer != null) {
            commandConsumer.close();
        }
    }

    @Test
    @DisplayName("POST /transfers crea PENDING y publica DebitRequested")
    void createTransferPublishesDebitRequested() {
        UUID source = UUID.randomUUID();
        UUID destination = UUID.randomUUID();
        List<DomainEvent> commands = subscribeToAccountCommands();

        ResponseEntity<TransferResponse> response = restTemplate.postForEntity("/transfers",
                new CreateTransferRequest(source, destination, new BigDecimal("50.00")), TransferResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("PENDING");
        UUID transferId = response.getBody().id();

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            drain(commands);
            assertThat(commands).anySatisfy(event -> assertThat(event)
                    .isInstanceOf(DebitRequested.class)
                    .satisfies(e -> {
                        DebitRequested debit = (DebitRequested) e;
                        assertThat(debit.transferId()).isEqualTo(transferId);
                        assertThat(debit.accountId()).isEqualTo(source);
                        assertThat(debit.amount()).isEqualByComparingTo("50.00");
                    }));
        });
    }

    private List<DomainEvent> subscribeToAccountCommands() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "it-verifier-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        JsonDeserializer<DomainEvent> valueDeserializer = new JsonDeserializer<>(DomainEvent.class);
        valueDeserializer.addTrustedPackages("com.hexabank.shared.events");
        commandConsumer = new KafkaConsumer<>(props, new StringDeserializer(), valueDeserializer);
        commandConsumer.subscribe(List.of(KafkaTopics.ACCOUNT_COMMANDS));
        return new CopyOnWriteArrayList<>();
    }

    private void drain(List<DomainEvent> sink) {
        ConsumerRecords<String, DomainEvent> records = commandConsumer.poll(Duration.ofMillis(500));
        for (ConsumerRecord<String, DomainEvent> record : records) {
            sink.add(record.value());
        }
    }
}

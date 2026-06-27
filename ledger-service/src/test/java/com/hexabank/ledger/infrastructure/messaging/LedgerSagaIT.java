package com.hexabank.ledger.infrastructure.messaging;

import com.hexabank.ledger.domain.TransferView;
import com.hexabank.ledger.infrastructure.persistence.LedgerEntryJpaRepository;
import com.hexabank.ledger.infrastructure.persistence.ProcessedEventJpaRepository;
import com.hexabank.ledger.infrastructure.persistence.TransferViewJpaRepository;
import com.hexabank.shared.events.MoneyCredited;
import com.hexabank.shared.events.MoneyDebited;
import com.hexabank.shared.events.TransferCompleted;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Test de integración de la ingesta del ledger contra Kafka + Postgres reales (Testcontainers).
 * Publica directamente en los topics los eventos de una transferencia con final feliz y verifica:
 * <ol>
 *   <li>se materializan dos asientos (DEBIT + CREDIT) y la proyección queda {@code COMPLETED};</li>
 *   <li>reenviar el mismo {@code MoneyDebited} (mismo {@code eventId}) no crea un asiento duplicado
 *       (idempotencia).</li>
 * </ol>
 *
 * <p>Requiere Docker.</p>
 */
@SpringBootTest
@Testcontainers
class LedgerSagaIT {

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
    LedgerEntryJpaRepository ledgerEntries;

    @Autowired
    TransferViewJpaRepository transferViews;

    @Autowired
    ProcessedEventJpaRepository processedEvents;

    @Autowired
    KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    @DisplayName("materializa la saga en asientos + proyección y es idempotente")
    void materializesSagaAndIsIdempotent() {
        UUID transferId = UUID.randomUUID();
        UUID source = UUID.randomUUID();
        UUID destination = UUID.randomUUID();
        UUID debitEventId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("40.00");

        MoneyDebited debited = new MoneyDebited(debitEventId, transferId, source, amount);
        kafkaTemplate.send(KafkaTopics.ACCOUNT_EVENTS, source.toString(), debited);
        kafkaTemplate.send(KafkaTopics.ACCOUNT_EVENTS, destination.toString(),
                new MoneyCredited(UUID.randomUUID(), transferId, destination, amount));
        kafkaTemplate.send(KafkaTopics.TRANSFER_EVENTS, transferId.toString(),
                new TransferCompleted(UUID.randomUUID(), transferId));

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            assertThat(ledgerEntries.count()).isEqualTo(2);
            assertThat(transferViews.findById(transferId))
                    .get()
                    .extracting(view -> view.getStatus())
                    .isEqualTo(TransferView.COMPLETED);
        });

        // Reenvío del mismo MoneyDebited: el consumidor debe ignorarlo (no duplica el asiento).
        kafkaTemplate.send(KafkaTopics.ACCOUNT_EVENTS, source.toString(), debited);
        await().pollDelay(Duration.ofSeconds(3)).atMost(Duration.ofSeconds(8)).untilAsserted(() -> {
            assertThat(ledgerEntries.count()).isEqualTo(2);
            assertThat(processedEvents.existsById(debitEventId)).isTrue();
        });
    }
}

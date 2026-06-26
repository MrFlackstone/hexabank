package com.hexabank.account.infrastructure.messaging;

import com.hexabank.account.domain.Account;
import com.hexabank.account.domain.AccountId;
import com.hexabank.account.domain.Money;
import com.hexabank.account.infrastructure.persistence.AccountPersistenceAdapter;
import com.hexabank.account.infrastructure.persistence.ProcessedEventJpaRepository;
import com.hexabank.shared.events.DebitRequested;
import com.hexabank.shared.events.DomainEvent;
import com.hexabank.shared.events.MoneyDebited;
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
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.core.KafkaTemplate;
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
 * Test de integración de la mensajería de account-service contra Kafka + Postgres reales
 * (Testcontainers). Verifica:
 * <ol>
 *   <li>publicar {@code DebitRequested} → el saldo baja y se publica {@code MoneyDebited};</li>
 *   <li>reenviar el mismo evento (mismo {@code eventId}) → no se aplica dos veces (idempotencia).</li>
 * </ol>
 *
 * <p>Requiere Docker.</p>
 */
@SpringBootTest
@Testcontainers
class AccountSagaIT {

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
    AccountPersistenceAdapter accounts;

    @Autowired
    ProcessedEventJpaRepository processedEvents;

    @Autowired
    KafkaTemplate<String, Object> kafkaTemplate;

    private KafkaConsumer<String, DomainEvent> eventConsumer;

    @AfterEach
    void closeConsumer() {
        if (eventConsumer != null) {
            eventConsumer.close();
        }
    }

    @Test
    @DisplayName("DebitRequested debita el saldo y publica MoneyDebited")
    void debitRequestedProducesMoneyDebited() {
        UUID accountId = accounts.save(Account.open("Origen", Money.of("100.00"))).id().value();
        List<DomainEvent> received = subscribeToAccountEvents();

        UUID eventId = UUID.randomUUID();
        UUID transferId = UUID.randomUUID();
        kafkaTemplate.send(KafkaTopics.ACCOUNT_COMMANDS, accountId.toString(),
                new DebitRequested(eventId, transferId, accountId, new BigDecimal("40.00")));

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            drain(received);
            assertThat(balanceOf(accountId)).isEqualByComparingTo("60.00");
            assertThat(received).anySatisfy(e -> assertThat(e)
                    .isInstanceOf(MoneyDebited.class)
                    .extracting(ev -> ((MoneyDebited) ev).transferId())
                    .isEqualTo(transferId));
        });
    }

    @Test
    @DisplayName("reenviar el mismo DebitRequested no debita dos veces (idempotencia)")
    void duplicateDebitRequestedIsIdempotent() {
        UUID accountId = accounts.save(Account.open("Origen", Money.of("100.00"))).id().value();
        UUID eventId = UUID.randomUUID();
        DebitRequested command =
                new DebitRequested(eventId, UUID.randomUUID(), accountId, new BigDecimal("40.00"));

        kafkaTemplate.send(KafkaTopics.ACCOUNT_COMMANDS, accountId.toString(), command);
        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(balanceOf(accountId)).isEqualByComparingTo("60.00"));

        // Reenvío del mismo eventId: el consumidor debe ignorarlo.
        kafkaTemplate.send(KafkaTopics.ACCOUNT_COMMANDS, accountId.toString(), command);
        await().pollDelay(Duration.ofSeconds(3)).atMost(Duration.ofSeconds(6))
                .untilAsserted(() -> {
                    assertThat(balanceOf(accountId)).isEqualByComparingTo("60.00");
                    assertThat(processedEvents.existsById(eventId)).isTrue();
                });
    }

    private BigDecimal balanceOf(UUID accountId) {
        return accounts.findById(new AccountId(accountId))
                .map(account -> account.balance().amount())
                .orElseThrow();
    }

    private List<DomainEvent> subscribeToAccountEvents() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "it-verifier-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        JsonDeserializer<DomainEvent> valueDeserializer = new JsonDeserializer<>(DomainEvent.class);
        valueDeserializer.addTrustedPackages("com.hexabank.shared.events");
        eventConsumer = new KafkaConsumer<>(props, new StringDeserializer(), valueDeserializer);
        eventConsumer.subscribe(List.of(KafkaTopics.ACCOUNT_EVENTS));
        return new CopyOnWriteArrayList<>();
    }

    private void drain(List<DomainEvent> sink) {
        ConsumerRecords<String, DomainEvent> records = eventConsumer.poll(Duration.ofMillis(500));
        for (ConsumerRecord<String, DomainEvent> record : records) {
            sink.add(record.value());
        }
    }
}

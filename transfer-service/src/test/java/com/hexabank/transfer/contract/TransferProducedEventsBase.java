package com.hexabank.transfer.contract;

import com.hexabank.shared.events.CreditRequested;
import com.hexabank.shared.events.DebitRequested;
import com.hexabank.shared.events.RefundRequested;
import com.hexabank.shared.events.TransferCompleted;
import com.hexabank.shared.events.TransferFailed;
import com.hexabank.transfer.application.port.out.EventPublisherPort;
import com.hexabank.transfer.config.TestSecurityConfig;
import com.hexabank.transfer.infrastructure.messaging.OutboxRelay;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Clase base de los tests de contrato generados por Spring Cloud Contract para el productor
 * transfer-service: comandos en {@code account-commands} (DebitRequested, CreditRequested,
 * RefundRequested) y eventos terminales en {@code transfer-events} (TransferCompleted,
 * TransferFailed). Un único base test cubre ambos topics (Fase 13b).
 *
 * <p>Los triggers publican el evento representativo por la ruta real (outbox → relay → KafkaTemplate),
 * de modo que el mensaje en el hilo (body JSON plano + header {@code __TypeId__}) es idéntico al de
 * producción; la lógica de negocio de la saga (precondiciones de ownership, estados) ya la cubren los
 * IT de saga. El {@code @BeforeEach} posiciona el receptor al final de ambos topics para aislar el
 * mensaje del trigger.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, TransferProducedEventsBase.MessageVerifierTestConfig.class})
@AutoConfigureMessageVerifier
public abstract class TransferProducedEventsBase {

    private static final String ACCOUNT_COMMANDS = "account-commands";
    private static final String TRANSFER_EVENTS = "transfer-events";

    // Contenedores singleton (patrón recomendado de Testcontainers): arrancan una vez y NO se paran
    // entre clases de test. Necesario porque los dos tests de contrato generados (Account_commandsIT
    // y Transfer_eventsIT) comparten el mismo contexto Spring cacheado; con @Container/@Testcontainers,
    // JUnit pararía el contenedor tras la primera clase y la segunda reusaría un DataSource apuntando a
    // un Postgres ya caído.
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    static final org.testcontainers.kafka.KafkaContainer kafka =
            new org.testcontainers.kafka.KafkaContainer(DockerImageName.parse("apache/kafka:3.9.1"));

    static {
        postgres.start();
        kafka.start();
    }

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    EventPublisherPort eventPublisher;
    @Autowired
    OutboxRelay outboxRelay;
    @Autowired
    KafkaMessageVerifierReceiver receiver;

    @BeforeEach
    void positionReceiverAtEnd() {
        receiver.seekToEnd(ACCOUNT_COMMANDS);
        receiver.seekToEnd(TRANSFER_EVENTS);
    }

    private void publish(Object event) {
        eventPublisher.publish((com.hexabank.shared.events.DomainEvent) event);
        outboxRelay.publishPending();
    }

    public void triggerDebitRequested() {
        publish(new DebitRequested(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("40.00")));
    }

    public void triggerCreditRequested() {
        publish(new CreditRequested(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("40.00")));
    }

    public void triggerRefundRequested() {
        publish(new RefundRequested(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("40.00")));
    }

    public void triggerTransferCompleted() {
        publish(new TransferCompleted(UUID.randomUUID(), UUID.randomUUID()));
    }

    public void triggerTransferFailed() {
        publish(new TransferFailed(UUID.randomUUID(), UUID.randomUUID(), "Fondos insuficientes en la cuenta origen"));
    }

    @TestConfiguration
    static class MessageVerifierTestConfig {
        @Bean
        KafkaMessageVerifierReceiver producedEventsReceiver(
                @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
            return new KafkaMessageVerifierReceiver(bootstrapServers);
        }
    }
}

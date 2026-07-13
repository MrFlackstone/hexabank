package com.hexabank.account.contract;

import com.hexabank.account.application.port.in.AccountCommand;
import com.hexabank.account.application.port.in.CreateAccountCommand;
import com.hexabank.account.application.port.in.CreateAccountUseCase;
import com.hexabank.account.application.port.in.CreditAccountUseCase;
import com.hexabank.account.application.port.in.DebitAccountUseCase;
import com.hexabank.account.application.port.in.RefundAccountUseCase;
import com.hexabank.account.config.TestSecurityConfig;
import com.hexabank.account.domain.Account;
import com.hexabank.account.domain.Money;
import com.hexabank.account.domain.OwnerId;
import com.hexabank.account.infrastructure.messaging.OutboxRelay;
import com.hexabank.account.infrastructure.persistence.AccountPersistenceAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Clase base de los tests de contrato generados por Spring Cloud Contract para el productor
 * account-service (topic {@code account-events}, Fase 13b). Cada contrato de
 * {@code src/test/resources/contracts/account-events/} referencia un método {@code trigger*()} de
 * aquí; el test generado lo invoca y luego verifica el mensaje recibido de Kafka contra el contrato.
 *
 * <p>Levanta Kafka + Postgres reales (Testcontainers). Antes de cada test posiciona el receptor al
 * final del topic ({@link KafkaMessageVerifierReceiver#seekToEnd}) para aislar el mensaje del trigger.
 * Los triggers siembran el estado con el adaptador de persistencia (no publica eventos) y publican el
 * outbox de forma síncrona con {@link OutboxRelay#publishPending()} para hacer el test determinista.</p>
 *
 * <p>Nota: {@code CreditFailed} no tiene contrato porque el código actual nunca lo emite (evento no
 * usado); un contrato de productor solo puede verificar un mensaje que realmente se envía.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, AccountEventsBase.MessageVerifierTestConfig.class})
@AutoConfigureMessageVerifier
@Testcontainers
public abstract class AccountEventsBase {

    private static final String ACCOUNT_EVENTS = "account-events";

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
    CreateAccountUseCase createAccount;
    @Autowired
    DebitAccountUseCase debitAccount;
    @Autowired
    CreditAccountUseCase creditAccount;
    @Autowired
    RefundAccountUseCase refundAccount;
    @Autowired
    AccountPersistenceAdapter accounts;
    @Autowired
    OutboxRelay outboxRelay;
    @Autowired
    KafkaMessageVerifierReceiver receiver;

    @BeforeEach
    void positionReceiverAtEnd() {
        receiver.seekToEnd(ACCOUNT_EVENTS);
    }

    private UUID seedAccount(String holder, String balance, String owner) {
        return accounts.save(Account.open(holder, Money.of(balance), OwnerId.of(owner))).id().value();
    }

    public void triggerAccountOpened() {
        createAccount.createAccount(new CreateAccountCommand("Ana", Money.of("100.00"), OwnerId.of("alice")));
        outboxRelay.publishPending();
    }

    public void triggerMoneyDebited() {
        UUID accountId = seedAccount("Origen", "100.00", "origen-sub");
        debitAccount.debit(new AccountCommand(UUID.randomUUID(), UUID.randomUUID(), accountId, new BigDecimal("40.00")));
        outboxRelay.publishPending();
    }

    public void triggerDebitFailed() {
        UUID accountId = seedAccount("Origen", "10.00", "origen-sub");
        debitAccount.debit(new AccountCommand(UUID.randomUUID(), UUID.randomUUID(), accountId, new BigDecimal("40.00")));
        outboxRelay.publishPending();
    }

    public void triggerMoneyCredited() {
        UUID accountId = seedAccount("Destino", "0.00", "destino-sub");
        creditAccount.credit(new AccountCommand(UUID.randomUUID(), UUID.randomUUID(), accountId, new BigDecimal("40.00")));
        outboxRelay.publishPending();
    }

    public void triggerMoneyRefunded() {
        UUID accountId = seedAccount("Origen", "60.00", "origen-sub");
        refundAccount.refund(new AccountCommand(UUID.randomUUID(), UUID.randomUUID(), accountId, new BigDecimal("40.00")));
        outboxRelay.publishPending();
    }

    @TestConfiguration
    static class MessageVerifierTestConfig {
        @Bean
        KafkaMessageVerifierReceiver accountEventsReceiver(
                @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
            return new KafkaMessageVerifierReceiver(bootstrapServers);
        }
    }
}

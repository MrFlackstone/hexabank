package com.hexabank.transfer.contract;

import com.hexabank.transfer.application.port.out.AccountOwnershipPort;
import com.hexabank.transfer.application.port.out.LoadTransferPort;
import com.hexabank.transfer.application.port.out.SaveTransferPort;
import com.hexabank.transfer.config.TestSecurityConfig;
import com.hexabank.transfer.domain.Transfer;
import com.hexabank.transfer.domain.TransferId;
import com.hexabank.transfer.domain.TransferStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.contract.stubrunner.StubTrigger;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Verificación del lado consumidor (Spring Cloud Contract, Fase 13b): transfer-service consume
 * {@code account-events} producidos por account-service. Usa el {@code stubs} jar publicado por
 * account-service (stub-runner en modo LOCAL, resuelto del repositorio local) y un
 * {@link KafkaMessageVerifierSender} que envía el {@code outputMessage} del contrato a Kafka; el
 * {@code @KafkaListener} real ({@code AccountEventListener}) lo procesa y se asevera el efecto de la
 * saga. Así, si account-service rompe el esquema de un evento, este test lo detecta antes de romper
 * al consumidor real.
 *
 * <p>Los {@code transferId}/{@code accountId} de los contratos fijan un valor de consumidor conocido
 * (lado {@code consumer(...)} del contrato) para poder sembrar el {@link Transfer} en el estado
 * correcto antes de disparar el stub.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, AccountEventsConsumerContractIT.MessageVerifierTestConfig.class})
@AutoConfigureStubRunner(
        stubsMode = StubRunnerProperties.StubsMode.LOCAL,
        ids = "com.hexabank:account-service:+:stubs")
class AccountEventsConsumerContractIT {

    // Valores fijos del lado consumidor de los contratos de account-events.
    private static final UUID TRANSFER_MONEY_DEBITED = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID TRANSFER_DEBIT_FAILED = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID TRANSFER_MONEY_CREDITED = UUID.fromString("10000000-0000-0000-0000-000000000003");
    private static final UUID TRANSFER_MONEY_REFUNDED = UUID.fromString("10000000-0000-0000-0000-000000000004");
    private static final UUID ACCOUNT_OPENED = UUID.fromString("20000000-0000-0000-0000-000000000009");
    private static final String ACCOUNT_OPENED_OWNER = "alice";

    private static final UUID SOURCE = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID DESTINATION = UUID.fromString("30000000-0000-0000-0000-000000000002");
    private static final BigDecimal AMOUNT = new BigDecimal("40.00");

    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.15-alpine");
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
    StubTrigger stubTrigger;
    @Autowired
    SaveTransferPort saveTransfer;
    @Autowired
    LoadTransferPort loadTransfer;
    @Autowired
    AccountOwnershipPort accountOwnership;

    private void seed(UUID transferId, TransferStatus status) {
        saveTransfer.save(new Transfer(new TransferId(transferId), SOURCE, DESTINATION, AMOUNT, status, 0L));
    }

    private void awaitStatus(UUID transferId, TransferStatus expected) {
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(loadTransfer.findById(new TransferId(transferId)))
                        .isPresent()
                        .get()
                        .extracting(Transfer::status)
                        .isEqualTo(expected));
    }

    @Test
    @DisplayName("MoneyDebited del stub lleva la transferencia PENDING a DEBITED")
    void moneyDebitedTransitionsToDebited() {
        seed(TRANSFER_MONEY_DEBITED, TransferStatus.PENDING);
        stubTrigger.trigger("moneyDebited");
        awaitStatus(TRANSFER_MONEY_DEBITED, TransferStatus.DEBITED);
    }

    @Test
    @DisplayName("DebitFailed del stub lleva la transferencia PENDING a FAILED")
    void debitFailedTransitionsToFailed() {
        seed(TRANSFER_DEBIT_FAILED, TransferStatus.PENDING);
        stubTrigger.trigger("debitFailed");
        awaitStatus(TRANSFER_DEBIT_FAILED, TransferStatus.FAILED);
    }

    @Test
    @DisplayName("MoneyCredited del stub lleva la transferencia DEBITED a COMPLETED")
    void moneyCreditedTransitionsToCompleted() {
        seed(TRANSFER_MONEY_CREDITED, TransferStatus.DEBITED);
        stubTrigger.trigger("moneyCredited");
        awaitStatus(TRANSFER_MONEY_CREDITED, TransferStatus.COMPLETED);
    }

    @Test
    @DisplayName("MoneyRefunded del stub lleva la transferencia COMPENSATING a FAILED")
    void moneyRefundedTransitionsToFailed() {
        seed(TRANSFER_MONEY_REFUNDED, TransferStatus.COMPENSATING);
        stubTrigger.trigger("moneyRefunded");
        awaitStatus(TRANSFER_MONEY_REFUNDED, TransferStatus.FAILED);
    }

    @Test
    @DisplayName("AccountOpened del stub registra la propiedad de la cuenta")
    void accountOpenedRecordsOwnership() {
        stubTrigger.trigger("accountOpened");
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(accountOwnership.accountsOwnedBy(ACCOUNT_OPENED_OWNER)).contains(ACCOUNT_OPENED));
    }

    @TestConfiguration
    static class MessageVerifierTestConfig {
        @Bean
        KafkaMessageVerifierSender contractMessageSender(
                @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
            return new KafkaMessageVerifierSender(bootstrapServers);
        }
    }
}

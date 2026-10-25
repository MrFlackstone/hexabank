package com.hexabank.ledger.contract;

import com.hexabank.ledger.application.port.in.QueryLedgerUseCase;
import com.hexabank.ledger.application.port.out.AccountOwnershipPort;
import com.hexabank.ledger.application.port.out.TransferViewRepositoryPort;
import com.hexabank.ledger.config.TestSecurityConfig;
import com.hexabank.ledger.domain.EntryType;
import com.hexabank.ledger.domain.LedgerEntry;
import com.hexabank.ledger.domain.TransferView;
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

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Verificación del lado consumidor (Spring Cloud Contract, Fase 13b): ledger-service consume
 * {@code account-events} (account-service) y {@code transfer-events} (transfer-service). Usa los
 * {@code stubs} jar de ambos productores (stub-runner en modo LOCAL) + un
 * {@link KafkaMessageVerifierSender} a Kafka; el {@code @KafkaListener} real ({@code LedgerEventListener})
 * materializa el asiento / la proyección y se asevera el efecto.
 *
 * <p>Los handlers del ledger son autosuficientes (idempotentes, con creación perezosa de la
 * proyección), así que no hay que sembrar estado: basta disparar el stub. Se apoya en los
 * {@code transferId}/{@code accountId} fijos del lado consumidor de los contratos.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, LedgerEventsConsumerContractIT.MessageVerifierTestConfig.class})
@AutoConfigureStubRunner(
        stubsMode = StubRunnerProperties.StubsMode.LOCAL,
        ids = {"com.hexabank:account-service:+:stubs", "com.hexabank:transfer-service:+:stubs"})
class LedgerEventsConsumerContractIT {

    private static final UUID TRANSFER_MONEY_DEBITED = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID TRANSFER_MONEY_CREDITED = UUID.fromString("10000000-0000-0000-0000-000000000003");
    private static final UUID TRANSFER_MONEY_REFUNDED = UUID.fromString("10000000-0000-0000-0000-000000000004");
    private static final UUID TRANSFER_COMPLETED = UUID.fromString("50000000-0000-0000-0000-000000000001");
    private static final UUID TRANSFER_FAILED = UUID.fromString("50000000-0000-0000-0000-000000000002");
    private static final UUID ACCOUNT_OPENED = UUID.fromString("20000000-0000-0000-0000-000000000009");
    private static final String ACCOUNT_OPENED_OWNER = "alice";

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
    QueryLedgerUseCase ledgerQuery;
    @Autowired
    TransferViewRepositoryPort transferViews;
    @Autowired
    AccountOwnershipPort accountOwnership;

    private void awaitEntry(UUID transferId, EntryType type) {
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(ledgerQuery.recentEntries())
                        .anySatisfy(entry -> {
                            assertThat(entry.transferId()).isEqualTo(transferId);
                            assertThat(entry.type()).isEqualTo(type);
                        }));
    }

    private void awaitViewStatus(UUID transferId, String status) {
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(transferViews.find(transferId))
                        .isPresent()
                        .get()
                        .extracting(TransferView::status)
                        .isEqualTo(status));
    }

    @Test
    @DisplayName("MoneyDebited del stub registra un asiento DEBIT en el ledger")
    void moneyDebitedRecordsDebitEntry() {
        stubTrigger.trigger("moneyDebited");
        awaitEntry(TRANSFER_MONEY_DEBITED, EntryType.DEBIT);
    }

    @Test
    @DisplayName("MoneyCredited del stub registra un asiento CREDIT en el ledger")
    void moneyCreditedRecordsCreditEntry() {
        stubTrigger.trigger("moneyCredited");
        awaitEntry(TRANSFER_MONEY_CREDITED, EntryType.CREDIT);
    }

    @Test
    @DisplayName("MoneyRefunded del stub registra un asiento REFUND en el ledger")
    void moneyRefundedRecordsRefundEntry() {
        stubTrigger.trigger("moneyRefunded");
        awaitEntry(TRANSFER_MONEY_REFUNDED, EntryType.REFUND);
    }

    @Test
    @DisplayName("TransferCompleted del stub marca la proyección como COMPLETED")
    void transferCompletedMarksViewCompleted() {
        stubTrigger.trigger("transferCompleted");
        awaitViewStatus(TRANSFER_COMPLETED, TransferView.COMPLETED);
    }

    @Test
    @DisplayName("TransferFailed del stub marca la proyección como FAILED")
    void transferFailedMarksViewFailed() {
        stubTrigger.trigger("transferFailed");
        awaitViewStatus(TRANSFER_FAILED, TransferView.FAILED);
    }

    @Test
    @DisplayName("AccountOpened del stub registra la propiedad de la cuenta")
    void accountOpenedRegistersOwnership() {
        stubTrigger.trigger("accountOpened");
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(accountOwnership.ownerOf(ACCOUNT_OPENED)).contains(ACCOUNT_OPENED_OWNER));
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

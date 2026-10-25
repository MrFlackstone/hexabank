package com.hexabank.account.contract;

import com.hexabank.account.domain.Account;
import com.hexabank.account.domain.AccountId;
import com.hexabank.account.domain.Money;
import com.hexabank.account.domain.OwnerId;
import com.hexabank.account.config.TestSecurityConfig;
import com.hexabank.account.infrastructure.persistence.AccountPersistenceAdapter;
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
 * Verificación del lado consumidor (Spring Cloud Contract, Fase 13b): account-service consume
 * {@code account-commands} producidos por transfer-service. Usa el {@code stubs} jar publicado por
 * transfer-service (stub-runner en modo LOCAL, resuelto del repositorio local tras el {@code install}
 * de la primera fase del build) y un {@link KafkaMessageVerifierSender} que envía el
 * {@code outputMessage} del contrato a Kafka; el {@code @KafkaListener} real ({@code AccountCommandListener})
 * lo procesa y se asevera el efecto sobre el saldo de la cuenta.
 *
 * <p>Los contratos de account-commands fijan un {@code accountId} y un {@code amount} de consumidor
 * conocidos para poder sembrar la cuenta y comprobar el saldo resultante exacto.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, AccountCommandsConsumerContractIT.MessageVerifierTestConfig.class})
@AutoConfigureStubRunner(
        stubsMode = StubRunnerProperties.StubsMode.LOCAL,
        ids = "com.hexabank:transfer-service:+:stubs")
class AccountCommandsConsumerContractIT {

    private static final UUID ACCOUNT_DEBIT = UUID.fromString("40000000-0000-0000-0000-000000000001");
    private static final UUID ACCOUNT_CREDIT = UUID.fromString("40000000-0000-0000-0000-000000000002");
    private static final UUID ACCOUNT_REFUND = UUID.fromString("40000000-0000-0000-0000-000000000003");

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
    AccountPersistenceAdapter accounts;

    private void seed(UUID accountId, String balance) {
        accounts.save(new Account(new AccountId(accountId), "Titular", OwnerId.of("owner-" + accountId),
                Money.of(balance), 0L));
    }

    private void awaitBalance(UUID accountId, String expected) {
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(accounts.findById(new AccountId(accountId)))
                        .isPresent()
                        .get()
                        .extracting(account -> account.balance().amount())
                        .satisfies(amount -> assertThat((BigDecimal) amount).isEqualByComparingTo(expected)));
    }

    @Test
    @DisplayName("DebitRequested del stub debita el saldo de la cuenta")
    void debitRequestedDebitsBalance() {
        seed(ACCOUNT_DEBIT, "100.00");
        stubTrigger.trigger("debitRequested");
        awaitBalance(ACCOUNT_DEBIT, "60.00");
    }

    @Test
    @DisplayName("CreditRequested del stub acredita el saldo de la cuenta")
    void creditRequestedCreditsBalance() {
        seed(ACCOUNT_CREDIT, "0.00");
        stubTrigger.trigger("creditRequested");
        awaitBalance(ACCOUNT_CREDIT, "40.00");
    }

    @Test
    @DisplayName("RefundRequested del stub reembolsa el saldo de la cuenta")
    void refundRequestedRefundsBalance() {
        seed(ACCOUNT_REFUND, "60.00");
        stubTrigger.trigger("refundRequested");
        awaitBalance(ACCOUNT_REFUND, "100.00");
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

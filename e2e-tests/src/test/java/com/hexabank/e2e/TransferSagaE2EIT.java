package com.hexabank.e2e;

import com.hexabank.account.AccountServiceApplication;
import com.hexabank.account.application.port.in.CreateAccountCommand;
import com.hexabank.account.application.port.in.CreateAccountUseCase;
import com.hexabank.account.application.port.out.LoadAccountPort;
import com.hexabank.account.domain.Account;
import com.hexabank.account.domain.Money;
import com.hexabank.transfer.TransferServiceApplication;
import com.hexabank.transfer.application.port.in.GetTransferUseCase;
import com.hexabank.transfer.application.port.in.RequestTransferCommand;
import com.hexabank.transfer.application.port.in.RequestTransferUseCase;
import com.hexabank.transfer.domain.Transfer;
import com.hexabank.transfer.domain.TransferStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Test de extremo a extremo de la saga de transferencia: levanta account-service y transfer-service
 * como dos contextos Spring Boot independientes en el mismo JVM, conectados a un único Kafka y a un
 * Postgres por servicio (Testcontainers). Verifica el camino feliz y el de fallo. Requiere Docker.
 */
@Testcontainers
class TransferSagaE2EIT {

    @Container
    static org.testcontainers.kafka.KafkaContainer kafka =
            new org.testcontainers.kafka.KafkaContainer(DockerImageName.parse("apache/kafka:3.9.1"));

    @Container
    static PostgreSQLContainer<?> accountDb = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static PostgreSQLContainer<?> transferDb = new PostgreSQLContainer<>("postgres:16-alpine");

    static ConfigurableApplicationContext accountContext;
    static ConfigurableApplicationContext transferContext;

    @BeforeAll
    static void startServices() {
        accountContext = new SpringApplicationBuilder(AccountServiceApplication.class)
                .web(WebApplicationType.NONE).run(args(accountDb));
        transferContext = new SpringApplicationBuilder(TransferServiceApplication.class)
                .web(WebApplicationType.NONE).run(args(transferDb));
    }

    @AfterAll
    static void stopServices() {
        if (transferContext != null) {
            transferContext.close();
        }
        if (accountContext != null) {
            accountContext.close();
        }
    }

    @Test
    @DisplayName("camino feliz: la transferencia se completa y mueve los saldos")
    void happyPathCompletesTransfer() {
        Account origin = createAccount("Origen", "100.00");
        Account destination = createAccount("Destino", "0.00");

        Transfer transfer = requestTransfer(origin, destination, "40.00");

        await().atMost(Duration.ofSeconds(60))
                .until(() -> status(transfer) == TransferStatus.COMPLETED);
        assertThat(balanceOf(origin)).isEqualByComparingTo("60.00");
        assertThat(balanceOf(destination)).isEqualByComparingTo("40.00");
    }

    @Test
    @DisplayName("fondos insuficientes: la transferencia falla y los saldos quedan intactos")
    void insufficientFundsFailsTransferAndKeepsBalances() {
        Account origin = createAccount("Origen", "50.00");
        Account destination = createAccount("Destino", "0.00");

        Transfer transfer = requestTransfer(origin, destination, "100.00");

        await().atMost(Duration.ofSeconds(60))
                .until(() -> status(transfer) == TransferStatus.FAILED);
        assertThat(balanceOf(origin)).isEqualByComparingTo("50.00");
        assertThat(balanceOf(destination)).isEqualByComparingTo("0.00");
    }

    private Account createAccount(String holder, String balance) {
        return accountContext.getBean(CreateAccountUseCase.class)
                .createAccount(new CreateAccountCommand(holder, Money.of(balance)));
    }

    private Transfer requestTransfer(Account from, Account to, String amount) {
        return transferContext.getBean(RequestTransferUseCase.class)
                .requestTransfer(new RequestTransferCommand(
                        from.id().value(), to.id().value(), new BigDecimal(amount)));
    }

    private TransferStatus status(Transfer transfer) {
        return transferContext.getBean(GetTransferUseCase.class).getTransfer(transfer.id()).status();
    }

    private BigDecimal balanceOf(Account account) {
        return accountContext.getBean(LoadAccountPort.class).findById(account.id())
                .orElseThrow().balance().amount();
    }

    // Se pasan como argumentos de línea de comandos (alta precedencia) para sobreescribir el
    // application.yml de cada servicio con los datos de los contenedores.
    //
    // Flyway se desactiva y el esquema lo genera Hibernate: ambos servicios ubican sus migraciones en
    // 'classpath:db/migration' y, al cargar los dos jars en el mismo classpath de este test, Flyway
    // detectaría dos versiones V1 en conflicto. Cada contexto solo escanea las entidades de su paquete,
    // así que Hibernate crea el esquema correcto. Las migraciones reales ya se prueban en los IT de
    // cada servicio.
    private static String[] args(PostgreSQLContainer<?> database) {
        return new String[]{
                "--spring.datasource.url=" + database.getJdbcUrl(),
                "--spring.datasource.username=" + database.getUsername(),
                "--spring.datasource.password=" + database.getPassword(),
                "--spring.kafka.bootstrap-servers=" + kafka.getBootstrapServers(),
                "--spring.flyway.enabled=false",
                "--spring.jpa.hibernate.ddl-auto=create"
        };
    }
}

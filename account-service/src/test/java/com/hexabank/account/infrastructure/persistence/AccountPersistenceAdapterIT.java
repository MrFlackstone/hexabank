package com.hexabank.account.infrastructure.persistence;

import com.hexabank.account.domain.Account;
import com.hexabank.account.domain.AccountId;
import com.hexabank.account.domain.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test de integración del adaptador de persistencia contra un Postgres real (Testcontainers).
 *
 * <p>{@code @ServiceConnection} (Spring Boot 3.1+) autocablea la URL/credenciales del contenedor al
 * {@code DataSource} de Spring: no hace falta {@code @DynamicPropertySource}. Flyway aplica las
 * migraciones al arrancar el contexto, por lo que se prueba el esquema real, no uno generado por
 * Hibernate. Requiere Docker en ejecución.</p>
 */
@SpringBootTest
@Testcontainers
class AccountPersistenceAdapterIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    AccountPersistenceAdapter adapter;

    @Autowired
    AccountJpaRepository repository;

    @Test
    @DisplayName("persiste una cuenta y la recupera por id")
    void persistsAndLoads() {
        Account saved = adapter.save(Account.open("Diego", Money.of("100.00")));

        Optional<Account> loaded = adapter.findById(saved.id());

        assertThat(loaded).isPresent();
        assertThat(loaded.get().holder()).isEqualTo("Diego");
        assertThat(loaded.get().balance().amount()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("findById devuelve vacío si la cuenta no existe")
    void findByIdEmptyWhenMissing() {
        assertThat(adapter.findById(AccountId.newId())).isEmpty();
    }

    @Test
    @DisplayName("bloqueo optimista: una escritura con versión obsoleta es rechazada (@Version)")
    void optimisticLockingRejectsStaleWrite() {
        // Estado inicial en BD: versión 0.
        Account account = adapter.save(Account.open("Diego", Money.of("100.00")));
        UUID id = account.id().value();

        // Primera escritura (versión 0 == BD) → éxito; la BD pasa a versión 1.
        repository.saveAndFlush(new AccountJpaEntity(id, "Diego", new BigDecimal("90.00"), 0L));

        // Segunda escritura con la MISMA versión 0 (obsoleta) simula un update concurrente perdido.
        AccountJpaEntity stale = new AccountJpaEntity(id, "Diego", new BigDecimal("80.00"), 0L);
        assertThatThrownBy(() -> repository.saveAndFlush(stale))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }
}

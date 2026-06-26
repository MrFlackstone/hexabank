package com.hexabank.transfer.domain;

import com.hexabank.transfer.domain.exception.InvalidTransferStateException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitarios del agregado {@link Transfer}: invariantes de creación y transiciones de estado.
 */
class TransferTest {

    private final UUID source = UUID.randomUUID();
    private final UUID destination = UUID.randomUUID();

    @Test
    @DisplayName("request crea la transferencia en estado PENDING")
    void requestCreatesPending() {
        Transfer transfer = Transfer.request(source, destination, new BigDecimal("50.00"));

        assertThat(transfer.status()).isEqualTo(TransferStatus.PENDING);
        assertThat(transfer.sourceAccountId()).isEqualTo(source);
        assertThat(transfer.destinationAccountId()).isEqualTo(destination);
        assertThat(transfer.amount()).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("request rechaza misma cuenta origen y destino")
    void requestRejectsSameAccount() {
        assertThatThrownBy(() -> Transfer.request(source, source, new BigDecimal("10.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("request rechaza importe no positivo")
    void requestRejectsNonPositiveAmount() {
        assertThatThrownBy(() -> Transfer.request(source, destination, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("request rechaza importe por encima del máximo")
    void requestRejectsAmountOverLimit() {
        assertThatThrownBy(() -> Transfer.request(source, destination, new BigDecimal("1000000.01")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("camino feliz: PENDING -> DEBITED -> COMPLETED")
    void happyPath() {
        Transfer transfer = Transfer.request(source, destination, new BigDecimal("50.00"));

        transfer.markDebited();
        assertThat(transfer.status()).isEqualTo(TransferStatus.DEBITED);

        transfer.markCompleted();
        assertThat(transfer.status()).isEqualTo(TransferStatus.COMPLETED);
    }

    @Test
    @DisplayName("compensación: DEBITED -> COMPENSATING -> FAILED")
    void compensationPath() {
        Transfer transfer = Transfer.request(source, destination, new BigDecimal("50.00"));
        transfer.markDebited();

        transfer.markCompensating();
        assertThat(transfer.status()).isEqualTo(TransferStatus.COMPENSATING);

        transfer.markRefunded();
        assertThat(transfer.status()).isEqualTo(TransferStatus.FAILED);
    }

    @Test
    @DisplayName("débito rechazado: PENDING -> FAILED")
    void debitFailedPath() {
        Transfer transfer = Transfer.request(source, destination, new BigDecimal("50.00"));

        transfer.markDebitFailed();

        assertThat(transfer.status()).isEqualTo(TransferStatus.FAILED);
    }

    @Test
    @DisplayName("transición ilegal: completar sin haber debitado")
    void illegalTransition() {
        Transfer transfer = Transfer.request(source, destination, new BigDecimal("50.00"));

        assertThatThrownBy(transfer::markCompleted)
                .isInstanceOf(InvalidTransferStateException.class);
    }
}

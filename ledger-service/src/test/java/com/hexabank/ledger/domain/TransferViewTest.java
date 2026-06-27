package com.hexabank.ledger.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TransferViewTest {

    @Test
    @DisplayName("pending() arranca en PENDING sin cuentas ni importe conocidos")
    void startsPending() {
        TransferView view = TransferView.pending(UUID.randomUUID());

        assertThat(view.status()).isEqualTo(TransferView.PENDING);
        assertThat(view.sourceAccountId()).isNull();
        assertThat(view.destinationAccountId()).isNull();
        assertThat(view.amount()).isNull();
    }

    @Test
    @DisplayName("la proyección se completa de forma incremental con cada evento")
    void buildsUpIncrementally() {
        UUID transferId = UUID.randomUUID();
        UUID source = UUID.randomUUID();
        UUID destination = UUID.randomUUID();
        TransferView view = TransferView.pending(transferId);

        view.recordDebit(source, new BigDecimal("40.00"));
        view.recordCredit(destination);
        view.markCompleted();

        assertThat(view.sourceAccountId()).isEqualTo(source);
        assertThat(view.destinationAccountId()).isEqualTo(destination);
        assertThat(view.amount()).isEqualByComparingTo("40.00");
        assertThat(view.status()).isEqualTo(TransferView.COMPLETED);
    }

    @Test
    @DisplayName("markFailed() deja la proyección en FAILED")
    void marksFailed() {
        TransferView view = TransferView.pending(UUID.randomUUID());

        view.markFailed();

        assertThat(view.status()).isEqualTo(TransferView.FAILED);
    }
}

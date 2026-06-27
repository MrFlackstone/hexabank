package com.hexabank.ledger.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LedgerEntryTest {

    @Test
    @DisplayName("of() crea un asiento con marca de tiempo y los datos del movimiento")
    void ofCreatesEntry() {
        UUID eventId = UUID.randomUUID();
        UUID transferId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        LedgerEntry entry = LedgerEntry.of(eventId, transferId, accountId, EntryType.DEBIT, new BigDecimal("40.00"));

        assertThat(entry.entryId()).isEqualTo(eventId);
        assertThat(entry.transferId()).isEqualTo(transferId);
        assertThat(entry.accountId()).isEqualTo(accountId);
        assertThat(entry.type()).isEqualTo(EntryType.DEBIT);
        assertThat(entry.amount()).isEqualByComparingTo("40.00");
        assertThat(entry.createdAt()).isNotNull();
    }

    @Test
    @DisplayName("un importe no positivo es inválido")
    void rejectsNonPositiveAmount() {
        assertThatThrownBy(() ->
                LedgerEntry.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), EntryType.CREDIT, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("faltan identificadores obligatorios → inválido")
    void rejectsMissingIdentifiers() {
        assertThatThrownBy(() ->
                LedgerEntry.of(null, UUID.randomUUID(), UUID.randomUUID(), EntryType.CREDIT, new BigDecimal("1.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

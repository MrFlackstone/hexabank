package com.hexabank.account.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Tests del Value Object {@link Money}: invariantes y operaciones inmutables. Sin Spring. */
class MoneyTest {

    @Test
    @DisplayName("normaliza la escala a 2 decimales")
    void normalizesScale() {
        assertThat(Money.of("10").amount()).isEqualByComparingTo("10.00");
        assertThat(Money.of(new BigDecimal("5.5")).amount().scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("rechaza importes con más de 2 decimales")
    void rejectsTooManyDecimals() {
        assertThatThrownBy(() -> Money.of(new BigDecimal("1.234")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("rechaza importes negativos")
    void rejectsNegative() {
        assertThatThrownBy(() -> Money.of(new BigDecimal("-1.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("rechaza importe nulo")
    void rejectsNull() {
        assertThatThrownBy(() -> Money.of((BigDecimal) null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("add y subtract devuelven nuevas instancias (inmutabilidad)")
    void addAndSubtractAreImmutable() {
        Money ten = Money.of("10.00");
        Money three = Money.of("3.00");

        assertThat(ten.add(three).amount()).isEqualByComparingTo("13.00");
        assertThat(ten.subtract(three).amount()).isEqualByComparingTo("7.00");
        // El original no cambia.
        assertThat(ten.amount()).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("isLessThan compara correctamente")
    void comparesAmounts() {
        assertThat(Money.of("5.00").isLessThan(Money.of("10.00"))).isTrue();
        assertThat(Money.of("10.00").isLessThan(Money.of("10.00"))).isFalse();
    }
}

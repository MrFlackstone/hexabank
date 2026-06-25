package com.hexabank.account.domain;

import com.hexabank.account.domain.exception.InsufficientFundsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Tests del agregado {@link Account}: reglas de negocio de débito/crédito. Sin Spring. */
class AccountTest {

    @Test
    @DisplayName("open crea una cuenta con saldo inicial y versión 0")
    void opensWithInitialBalance() {
        Account account = Account.open("Diego", Money.of("100.00"));

        assertThat(account.holder()).isEqualTo("Diego");
        assertThat(account.balance().amount()).isEqualByComparingTo("100.00");
        assertThat(account.version()).isZero();
        assertThat(account.id()).isNotNull();
    }

    @Test
    @DisplayName("debit reduce el saldo cuando hay fondos")
    void debitWithFunds() {
        Account account = Account.open("Diego", Money.of("100.00"));

        account.debit(Money.of("40.00"));

        assertThat(account.balance().amount()).isEqualByComparingTo("60.00");
    }

    @Test
    @DisplayName("credit aumenta el saldo")
    void creditIncreasesBalance() {
        Account account = Account.open("Diego", Money.of("100.00"));

        account.credit(Money.of("25.50"));

        assertThat(account.balance().amount()).isEqualByComparingTo("125.50");
    }

    @Test
    @DisplayName("debit por encima del saldo lanza InsufficientFundsException y no muta el saldo")
    void debitWithoutFundsFails() {
        Account account = Account.open("Diego", Money.of("30.00"));

        assertThatThrownBy(() -> account.debit(Money.of("50.00")))
                .isInstanceOf(InsufficientFundsException.class);
        assertThat(account.balance().amount()).isEqualByComparingTo("30.00");
    }

    @Test
    @DisplayName("rechaza titular en blanco")
    void rejectsBlankHolder() {
        assertThatThrownBy(() -> Account.open("  ", Money.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

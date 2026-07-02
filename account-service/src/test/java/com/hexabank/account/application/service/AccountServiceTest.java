package com.hexabank.account.application.service;

import com.hexabank.account.application.port.in.CallerContext;
import com.hexabank.account.application.port.in.CreateAccountCommand;
import com.hexabank.account.application.port.out.EventPublisherPort;
import com.hexabank.account.application.port.out.LoadAccountPort;
import com.hexabank.account.application.port.out.SaveAccountPort;
import com.hexabank.account.domain.Account;
import com.hexabank.account.domain.AccountId;
import com.hexabank.account.domain.Money;
import com.hexabank.account.domain.OwnerId;
import com.hexabank.account.domain.exception.AccountNotFoundException;
import com.hexabank.shared.events.AccountOpened;
import com.hexabank.shared.events.DomainEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Tests unitarios de {@link AccountService}: creación con propietario y publicación de {@link AccountOpened}. */
class AccountServiceTest {

    private LoadAccountPort loadAccountPort;
    private SaveAccountPort saveAccountPort;
    private EventPublisherPort eventPublisher;
    private AccountService service;

    @BeforeEach
    void setUp() {
        loadAccountPort = mock(LoadAccountPort.class);
        saveAccountPort = mock(SaveAccountPort.class);
        eventPublisher = mock(EventPublisherPort.class);
        service = new AccountService(loadAccountPort, saveAccountPort, eventPublisher);
        when(saveAccountPort.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("createAccount persiste la cuenta con su propietario")
    void createAccountPersistsOwner() {
        Account account = service.createAccount(
                new CreateAccountCommand("Diego", Money.of("100.00"), OwnerId.of("diego-sub")));

        assertThat(account.ownerId()).isEqualTo(OwnerId.of("diego-sub"));
        verify(saveAccountPort).save(any(Account.class));
    }

    @Test
    @DisplayName("createAccount publica AccountOpened con la cuenta y el propietario")
    void createAccountPublishesAccountOpened() {
        Account account = service.createAccount(
                new CreateAccountCommand("Diego", Money.of("100.00"), OwnerId.of("diego-sub")));

        ArgumentCaptor<DomainEvent> captor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue()).isInstanceOfSatisfying(AccountOpened.class, event -> {
            assertThat(event.accountId()).isEqualTo(account.id().value());
            assertThat(event.ownerId()).isEqualTo("diego-sub");
            assertThat(event.eventId()).isNotNull();
        });
    }

    @Test
    @DisplayName("getAccount devuelve la cuenta a su propietario")
    void getAccountAllowsOwner() {
        Account account = Account.open("Diego", Money.of("50.00"), OwnerId.of("diego-sub"));
        when(loadAccountPort.findById(account.id())).thenReturn(Optional.of(account));

        assertThat(service.getAccount(account.id(), CallerContext.user("diego-sub"))).isEqualTo(account);
    }

    @Test
    @DisplayName("getAccount de una cuenta ajena lanza AccountNotFoundException (404, sin oráculo de existencia)")
    void getAccountHidesForeignAccountAsNotFound() {
        Account account = Account.open("Diego", Money.of("50.00"), OwnerId.of("diego-sub"));
        when(loadAccountPort.findById(account.id())).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.getAccount(account.id(), CallerContext.user("otro-sub")))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    @DisplayName("getAccount con rol STAFF accede a cualquier cuenta, incluidas las sin propietario")
    void getAccountAllowsStaff() {
        Account legacy = new Account(AccountId.newId(), "Legado", null, Money.of("50.00"), 0L);
        when(loadAccountPort.findById(legacy.id())).thenReturn(Optional.of(legacy));

        assertThat(service.getAccount(legacy.id(), CallerContext.staff("staff-sub"))).isEqualTo(legacy);
        assertThatThrownBy(() -> service.getAccount(legacy.id(), CallerContext.user("diego-sub")))
                .isInstanceOf(AccountNotFoundException.class);
    }
}

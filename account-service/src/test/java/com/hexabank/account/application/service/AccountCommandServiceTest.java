package com.hexabank.account.application.service;

import com.hexabank.account.application.port.in.AccountCommand;
import com.hexabank.account.application.port.out.EventPublisherPort;
import com.hexabank.account.application.port.out.LoadAccountPort;
import com.hexabank.account.application.port.out.ProcessedEventPort;
import com.hexabank.account.application.port.out.SaveAccountPort;
import com.hexabank.account.domain.Account;
import com.hexabank.account.domain.AccountId;
import com.hexabank.account.domain.Money;
import com.hexabank.shared.events.DebitFailed;
import com.hexabank.shared.events.DomainEvent;
import com.hexabank.shared.events.MoneyDebited;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios de los casos de uso de la saga: sin Spring ni Kafka, solo dominio + puertos
 * simulados (Mockito). Verifican idempotencia y la elección del evento de resultado.
 */
@ExtendWith(MockitoExtension.class)
class AccountCommandServiceTest {

    @Mock
    LoadAccountPort loadAccountPort;
    @Mock
    SaveAccountPort saveAccountPort;
    @Mock
    EventPublisherPort eventPublisher;
    @Mock
    ProcessedEventPort processedEvents;

    AccountCommandService service;

    private final UUID accountId = UUID.randomUUID();
    private final UUID transferId = UUID.randomUUID();
    private final UUID eventId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new AccountCommandService(loadAccountPort, saveAccountPort, eventPublisher, processedEvents);
    }

    @Test
    @DisplayName("débito con fondos suficientes guarda el saldo y publica MoneyDebited")
    void debitSuccessPublishesMoneyDebited() {
        when(processedEvents.alreadyProcessed(eventId)).thenReturn(false);
        when(loadAccountPort.findById(any(AccountId.class)))
                .thenReturn(Optional.of(account("100.00")));

        service.debit(command(new BigDecimal("40.00")));

        verify(saveAccountPort).save(any(Account.class));
        verify(processedEvents).markProcessed(eventId);
        DomainEvent published = capturePublished();
        assertThat(published).isInstanceOf(MoneyDebited.class);
        assertThat(((MoneyDebited) published).amount()).isEqualByComparingTo("40.00");
    }

    @Test
    @DisplayName("débito con fondos insuficientes publica DebitFailed y no guarda")
    void debitInsufficientPublishesDebitFailed() {
        when(processedEvents.alreadyProcessed(eventId)).thenReturn(false);
        when(loadAccountPort.findById(any(AccountId.class)))
                .thenReturn(Optional.of(account("30.00")));

        service.debit(command(new BigDecimal("40.00")));

        verify(saveAccountPort, never()).save(any());
        verify(processedEvents).markProcessed(eventId);
        assertThat(capturePublished()).isInstanceOf(DebitFailed.class);
    }

    @Test
    @DisplayName("evento ya procesado: idempotente, no toca cuenta ni publica")
    void alreadyProcessedIsNoOp() {
        when(processedEvents.alreadyProcessed(eventId)).thenReturn(true);

        service.debit(command(new BigDecimal("40.00")));

        verifyNoInteractions(loadAccountPort, saveAccountPort, eventPublisher);
        verify(processedEvents, never()).markProcessed(any());
    }

    private Account account(String balance) {
        return new Account(new AccountId(accountId), "Diego", Money.of(balance), 0L);
    }

    private AccountCommand command(BigDecimal amount) {
        return new AccountCommand(eventId, transferId, accountId, amount);
    }

    private DomainEvent capturePublished() {
        ArgumentCaptor<DomainEvent> captor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher).publish(captor.capture());
        return captor.getValue();
    }
}

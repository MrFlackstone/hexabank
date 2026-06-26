package com.hexabank.account.application.service;

import com.hexabank.account.application.port.in.AccountCommand;
import com.hexabank.account.application.port.in.CreditAccountUseCase;
import com.hexabank.account.application.port.in.DebitAccountUseCase;
import com.hexabank.account.application.port.in.RefundAccountUseCase;
import com.hexabank.account.application.port.out.EventPublisherPort;
import com.hexabank.account.application.port.out.LoadAccountPort;
import com.hexabank.account.application.port.out.ProcessedEventPort;
import com.hexabank.account.application.port.out.SaveAccountPort;
import com.hexabank.account.domain.Account;
import com.hexabank.account.domain.AccountId;
import com.hexabank.account.domain.Money;
import com.hexabank.account.domain.exception.AccountNotFoundException;
import com.hexabank.account.domain.exception.InsufficientFundsException;
import com.hexabank.shared.events.DebitFailed;
import com.hexabank.shared.events.MoneyCredited;
import com.hexabank.shared.events.MoneyDebited;
import com.hexabank.shared.events.MoneyRefunded;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Casos de uso de la saga sobre el saldo de una cuenta: débito, crédito y reembolso.
 *
 * <p>Cada método carga la cuenta, aplica la regla de dominio, persiste y publica el evento resultado.
 * Es idempotente: ignora un evento cuyo {@code eventId} ya esté registrado. Cada operación se ejecuta
 * en una transacción ({@link Transactional}) que abarca el cambio de saldo y el registro de idempotencia.</p>
 */
public class AccountCommandService implements DebitAccountUseCase, CreditAccountUseCase, RefundAccountUseCase {

    private final LoadAccountPort loadAccountPort;
    private final SaveAccountPort saveAccountPort;
    private final EventPublisherPort eventPublisher;
    private final ProcessedEventPort processedEvents;

    public AccountCommandService(LoadAccountPort loadAccountPort,
                                 SaveAccountPort saveAccountPort,
                                 EventPublisherPort eventPublisher,
                                 ProcessedEventPort processedEvents) {
        this.loadAccountPort = loadAccountPort;
        this.saveAccountPort = saveAccountPort;
        this.eventPublisher = eventPublisher;
        this.processedEvents = processedEvents;
    }

    /** Debita el importe y publica {@link MoneyDebited}, o {@link DebitFailed} si no hay fondos. */
    @Override
    @Transactional
    public void debit(AccountCommand command) {
        if (processedEvents.alreadyProcessed(command.eventId())) {
            return;
        }
        Account account = loadAccount(command.accountId());
        try {
            account.debit(Money.of(command.amount()));
            saveAccountPort.save(account);
            processedEvents.markProcessed(command.eventId());
            eventPublisher.publish(new MoneyDebited(
                    UUID.randomUUID(), command.transferId(), command.accountId(), command.amount()));
        } catch (InsufficientFundsException e) {
            processedEvents.markProcessed(command.eventId());
            eventPublisher.publish(new DebitFailed(
                    UUID.randomUUID(), command.transferId(), command.accountId(), command.amount(),
                    "Fondos insuficientes"));
        }
    }

    /** Acredita el importe y publica {@link MoneyCredited}. */
    @Override
    @Transactional
    public void credit(AccountCommand command) {
        if (processedEvents.alreadyProcessed(command.eventId())) {
            return;
        }
        Account account = loadAccount(command.accountId());
        account.credit(Money.of(command.amount()));
        saveAccountPort.save(account);
        processedEvents.markProcessed(command.eventId());
        eventPublisher.publish(new MoneyCredited(
                UUID.randomUUID(), command.transferId(), command.accountId(), command.amount()));
    }

    /** Devuelve el importe a la cuenta (compensación) y publica {@link MoneyRefunded}. */
    @Override
    @Transactional
    public void refund(AccountCommand command) {
        if (processedEvents.alreadyProcessed(command.eventId())) {
            return;
        }
        Account account = loadAccount(command.accountId());
        account.credit(Money.of(command.amount()));
        saveAccountPort.save(account);
        processedEvents.markProcessed(command.eventId());
        eventPublisher.publish(new MoneyRefunded(
                UUID.randomUUID(), command.transferId(), command.accountId(), command.amount()));
    }

    private Account loadAccount(UUID accountId) {
        AccountId id = new AccountId(accountId);
        return loadAccountPort.findById(id).orElseThrow(() -> new AccountNotFoundException(id));
    }
}

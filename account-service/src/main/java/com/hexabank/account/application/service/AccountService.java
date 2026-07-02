package com.hexabank.account.application.service;

import com.hexabank.account.application.port.in.CreateAccountCommand;
import com.hexabank.account.application.port.in.CreateAccountUseCase;
import com.hexabank.account.application.port.in.GetAccountUseCase;
import com.hexabank.account.application.port.out.EventPublisherPort;
import com.hexabank.account.application.port.out.LoadAccountPort;
import com.hexabank.account.application.port.out.SaveAccountPort;
import com.hexabank.account.domain.Account;
import com.hexabank.account.domain.AccountId;
import com.hexabank.account.domain.exception.AccountNotFoundException;
import com.hexabank.shared.events.AccountOpened;

import java.util.UUID;

/**
 * Casos de uso de creación y consulta de cuentas: orquesta el dominio a través de los puertos de salida.
 *
 * <p>Es una clase plana, sin anotaciones de Spring; se instancia como bean en
 * {@code config/BeanConfiguration}.</p>
 */
public class AccountService implements CreateAccountUseCase, GetAccountUseCase {

    private final LoadAccountPort loadAccountPort;
    private final SaveAccountPort saveAccountPort;
    private final EventPublisherPort eventPublisherPort;

    public AccountService(LoadAccountPort loadAccountPort,
                          SaveAccountPort saveAccountPort,
                          EventPublisherPort eventPublisherPort) {
        this.loadAccountPort = loadAccountPort;
        this.saveAccountPort = saveAccountPort;
        this.eventPublisherPort = eventPublisherPort;
    }

    @Override
    public Account createAccount(CreateAccountCommand command) {
        Account account = Account.open(command.holder(), command.initialBalance(), command.ownerId());
        Account saved = saveAccountPort.save(account);
        // Dual-write sin outbox: si la publicación falla tras el commit, la proyección de ownership
        // queda desincronizada. Se asume la misma limitación que el resto de publicaciones del
        // servicio hasta introducir el patrón outbox.
        eventPublisherPort.publish(new AccountOpened(
                UUID.randomUUID(), saved.id().value(), saved.ownerId().value()));
        return saved;
    }

    @Override
    public Account getAccount(AccountId id) {
        return loadAccountPort.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }
}

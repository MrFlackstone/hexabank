package com.hexabank.account.application.service;

import com.hexabank.account.application.port.in.CreateAccountCommand;
import com.hexabank.account.application.port.in.CreateAccountUseCase;
import com.hexabank.account.application.port.in.GetAccountUseCase;
import com.hexabank.account.application.port.out.LoadAccountPort;
import com.hexabank.account.application.port.out.SaveAccountPort;
import com.hexabank.account.domain.Account;
import com.hexabank.account.domain.AccountId;
import com.hexabank.account.domain.exception.AccountNotFoundException;

/**
 * Implementación de los casos de uso de cuentas: orquesta el dominio a través de los puertos de salida.
 *
 * <p>Es una clase <strong>plana</strong>, sin anotaciones de Spring ({@code @Service}), a propósito:
 * mantiene la capa de aplicación libre de framework. Su instanciación como bean se hace por
 * configuración explícita en {@code config/BeanConfiguration}. Así el "qué hace la aplicación" queda
 * separado del "cómo se cablea en Spring".</p>
 */
public class AccountService implements CreateAccountUseCase, GetAccountUseCase {

    private final LoadAccountPort loadAccountPort;
    private final SaveAccountPort saveAccountPort;

    public AccountService(LoadAccountPort loadAccountPort, SaveAccountPort saveAccountPort) {
        this.loadAccountPort = loadAccountPort;
        this.saveAccountPort = saveAccountPort;
    }

    @Override
    public Account createAccount(CreateAccountCommand command) {
        Account account = Account.open(command.holder(), command.initialBalance());
        return saveAccountPort.save(account);
    }

    @Override
    public Account getAccount(AccountId id) {
        return loadAccountPort.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }
}

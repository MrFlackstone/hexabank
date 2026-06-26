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
 * Casos de uso de creación y consulta de cuentas: orquesta el dominio a través de los puertos de salida.
 *
 * <p>Es una clase plana, sin anotaciones de Spring; se instancia como bean en
 * {@code config/BeanConfiguration}.</p>
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

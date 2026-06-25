package com.hexabank.account.domain.exception;

import com.hexabank.account.domain.AccountId;

/**
 * Se lanza cuando se solicita una cuenta que no existe.
 *
 * <p>Excepción de dominio independiente de HTTP; el adaptador REST la mapea a 404 Not Found.</p>
 */
public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(AccountId accountId) {
        super("No existe la cuenta " + accountId.value());
    }
}

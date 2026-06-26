package com.hexabank.account.domain.exception;

import com.hexabank.account.domain.AccountId;

/**
 * Se lanza cuando se intenta debitar más dinero del disponible en una cuenta.
 *
 * <p>Es una excepción de <em>dominio</em>: no conoce HTTP ni ningún framework. El adaptador REST
 * la traduce a un código de estado en su capa (409 Conflict).</p>
 */
public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(AccountId accountId) {
        super("Fondos insuficientes en la cuenta " + accountId.value());
    }
}

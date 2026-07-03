package com.hexabank.ledger.domain.exception;

import java.util.UUID;

/**
 * Se lanza cuando la cuenta consultada no existe <em>para el llamante</em>: bien porque la proyección
 * de propietarios no la conoce, bien porque pertenece a otro usuario. El mensaje es idéntico en ambos
 * casos para no revelar la existencia de cuentas ajenas (anti existence-oracle).
 */
public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(UUID accountId) {
        super("No existe la cuenta " + accountId);
    }
}

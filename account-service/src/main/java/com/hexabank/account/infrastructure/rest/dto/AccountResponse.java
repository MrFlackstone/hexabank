package com.hexabank.account.infrastructure.rest.dto;

import com.hexabank.account.domain.Account;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO de salida que expone el estado de una cuenta hacia el cliente REST.
 *
 * <p>No se serializa el agregado de dominio directamente: este record es el contrato público y
 * desacopla la API de la estructura interna del dominio.</p>
 */
public record AccountResponse(UUID id, String holder, BigDecimal balance, long version) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.id().value(),
                account.holder(),
                account.balance().amount(),
                account.version());
    }
}

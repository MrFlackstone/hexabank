package com.hexabank.account.application.port.in;

import com.hexabank.account.domain.Money;

/**
 * Comando inmutable de entrada al caso de uso de creación de cuenta.
 *
 * <p>Es el contrato de la aplicación, ya validado y traducido a tipos de dominio ({@link Money}).
 * Mantiene el dominio aislado del formato de transporte (los DTO de REST viven en infraestructura).</p>
 */
public record CreateAccountCommand(String holder, Money initialBalance) {
}

package com.hexabank.account.application.port.in;

/**
 * Puerto de entrada: caso de uso "reembolsar una cuenta" (compensación).
 */
public interface RefundAccountUseCase {

    void refund(AccountCommand command);
}

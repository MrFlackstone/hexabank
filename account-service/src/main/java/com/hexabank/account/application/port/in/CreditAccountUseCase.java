package com.hexabank.account.application.port.in;

/**
 * Puerto de entrada: caso de uso "acreditar una cuenta".
 */
public interface CreditAccountUseCase {

    void credit(AccountCommand command);
}

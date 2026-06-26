package com.hexabank.account.application.port.in;

/**
 * Puerto de entrada: caso de uso "debitar una cuenta".
 */
public interface DebitAccountUseCase {

    void debit(AccountCommand command);
}

package com.hexabank.account.application.port.in;

import com.hexabank.account.domain.Account;
import com.hexabank.account.domain.AccountId;

/**
 * Puerto de entrada: caso de uso "consultar una cuenta por su identidad".
 */
public interface GetAccountUseCase {

    Account getAccount(AccountId id);
}

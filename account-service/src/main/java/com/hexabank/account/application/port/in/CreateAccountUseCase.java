package com.hexabank.account.application.port.in;

import com.hexabank.account.domain.Account;

/**
 * Puerto de entrada: caso de uso "crear una cuenta".
 *
 * <p>Define <em>qué</em> ofrece la aplicación sin acoplarse a <em>quién</em> lo invoca (REST, un test,
 * más adelante un consumidor Kafka). El adaptador de entrada (controller) depende de esta interfaz,
 * no de la implementación.</p>
 */
public interface CreateAccountUseCase {

    Account createAccount(CreateAccountCommand command);
}

package com.hexabank.account.application.port.out;

import com.hexabank.account.domain.Account;
import com.hexabank.account.domain.AccountId;

import java.util.Optional;

/**
 * Puerto de salida: cargar una cuenta desde el almacén de persistencia.
 *
 * <p>Lo <em>declara</em> la aplicación (lo que necesita) y lo <em>implementa</em> la infraestructura
 * (adaptador JPA). Esto es la inversión de dependencias: el núcleo no conoce JPA; JPA conoce al núcleo.</p>
 */
public interface LoadAccountPort {

    Optional<Account> findById(AccountId id);
}

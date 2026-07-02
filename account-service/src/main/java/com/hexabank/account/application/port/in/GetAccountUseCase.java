package com.hexabank.account.application.port.in;

import com.hexabank.account.domain.Account;
import com.hexabank.account.domain.AccountId;

/**
 * Puerto de entrada: caso de uso "consultar una cuenta por su identidad".
 *
 * <p>La consulta está autorizada por propiedad: un llamante sin rol STAFF solo puede ver sus propias
 * cuentas. La denegación se materializa como "no encontrada" (404) para no revelar la existencia de
 * cuentas ajenas.</p>
 */
public interface GetAccountUseCase {

    Account getAccount(AccountId id, CallerContext caller);
}

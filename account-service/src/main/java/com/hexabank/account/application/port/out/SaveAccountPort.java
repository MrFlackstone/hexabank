package com.hexabank.account.application.port.out;

import com.hexabank.account.domain.Account;

/**
 * Puerto de salida: persistir una cuenta (alta o actualización).
 *
 * <p>Devuelve la cuenta persistida para reflejar el estado post-guardado (p. ej. el contador de
 * versión actualizado por el bloqueo optimista).</p>
 */
public interface SaveAccountPort {

    Account save(Account account);
}

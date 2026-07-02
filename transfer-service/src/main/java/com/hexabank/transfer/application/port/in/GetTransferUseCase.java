package com.hexabank.transfer.application.port.in;

import com.hexabank.transfer.domain.Transfer;
import com.hexabank.transfer.domain.TransferId;

/**
 * Puerto de entrada: caso de uso "consultar una transferencia por su identidad".
 *
 * <p>Visible para el propietario de la cuenta origen o destino, o para STAFF; en otro caso la
 * respuesta es idéntica a "no existe" (404).</p>
 */
public interface GetTransferUseCase {

    Transfer getTransfer(TransferId id, CallerContext caller);
}

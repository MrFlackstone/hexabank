package com.hexabank.transfer.application.port.in;

import com.hexabank.transfer.domain.Transfer;
import com.hexabank.transfer.domain.TransferId;

/**
 * Puerto de entrada: caso de uso "consultar una transferencia por su identidad".
 */
public interface GetTransferUseCase {

    Transfer getTransfer(TransferId id);
}

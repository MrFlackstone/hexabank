package com.hexabank.transfer.application.port.in;

import com.hexabank.transfer.domain.Transfer;

/**
 * Puerto de entrada: caso de uso "solicitar una transferencia".
 */
public interface RequestTransferUseCase {

    Transfer requestTransfer(RequestTransferCommand command);
}

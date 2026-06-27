package com.hexabank.ledger.application.port.in;

import com.hexabank.ledger.domain.TransferView;

import java.util.UUID;

/**
 * Puerto de entrada de consulta de la proyección de una transferencia.
 */
public interface GetTransferViewUseCase {

    /**
     * @throws com.hexabank.ledger.domain.exception.TransferViewNotFoundException si el ledger aún no la observó
     */
    TransferView getTransfer(UUID transferId);
}

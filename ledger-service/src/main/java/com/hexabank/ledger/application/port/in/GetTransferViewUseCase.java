package com.hexabank.ledger.application.port.in;

import com.hexabank.ledger.domain.TransferView;

import java.util.UUID;

/**
 * Puerto de entrada de consulta de la proyección de una transferencia. Visible para el propietario
 * de la cuenta origen o destino, o para STAFF; en otro caso responde como inexistente (404).
 */
public interface GetTransferViewUseCase {

    /**
     * @throws com.hexabank.ledger.domain.exception.TransferViewNotFoundException si el ledger aún no
     *         la observó o el llamante no está involucrado en ella
     */
    TransferView getTransfer(UUID transferId, CallerContext caller);
}

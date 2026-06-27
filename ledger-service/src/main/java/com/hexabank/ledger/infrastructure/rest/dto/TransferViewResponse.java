package com.hexabank.ledger.infrastructure.rest.dto;

import com.hexabank.ledger.domain.TransferView;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Representación de transporte de la proyección de una transferencia.
 */
public record TransferViewResponse(
        UUID transferId,
        UUID sourceAccountId,
        UUID destinationAccountId,
        BigDecimal amount,
        String status) {

    public static TransferViewResponse from(TransferView view) {
        return new TransferViewResponse(
                view.transferId(),
                view.sourceAccountId(),
                view.destinationAccountId(),
                view.amount(),
                view.status());
    }
}

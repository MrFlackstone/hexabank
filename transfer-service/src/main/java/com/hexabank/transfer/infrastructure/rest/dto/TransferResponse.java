package com.hexabank.transfer.infrastructure.rest.dto;

import com.hexabank.transfer.domain.Transfer;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO de salida que expone el estado de una transferencia hacia el cliente REST.
 */
public record TransferResponse(UUID id, UUID sourceAccountId, UUID destinationAccountId,
                               BigDecimal amount, String status, long version) {

    public static TransferResponse from(Transfer transfer) {
        return new TransferResponse(
                transfer.id().value(),
                transfer.sourceAccountId(),
                transfer.destinationAccountId(),
                transfer.amount(),
                transfer.status().name(),
                transfer.version());
    }
}

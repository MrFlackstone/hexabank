package com.hexabank.transfer.infrastructure.rest.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO de entrada del endpoint de solicitud de transferencia.
 */
public record CreateTransferRequest(

        @NotNull(message = "La cuenta origen es obligatoria")
        UUID sourceAccountId,

        @NotNull(message = "La cuenta destino es obligatoria")
        UUID destinationAccountId,

        @NotNull(message = "El importe es obligatorio")
        @Positive(message = "El importe debe ser positivo")
        @Digits(integer = 17, fraction = 2, message = "El importe excede la precisión permitida")
        @DecimalMax(value = "1000000.00", message = "El importe supera el máximo por transferencia")
        BigDecimal amount) {
}

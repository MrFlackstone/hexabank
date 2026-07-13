package com.hexabank.transfer.infrastructure.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO de entrada del endpoint de solicitud de transferencia.
 */
@Schema(description = "Peticion de solicitud de transferencia")
public record CreateTransferRequest(

        @Schema(description = "UUID de la cuenta origen", example = "11111111-1111-1111-1111-111111111111")
        @NotNull(message = "La cuenta origen es obligatoria")
        UUID sourceAccountId,

        @Schema(description = "UUID de la cuenta destino", example = "22222222-2222-2222-2222-222222222222")
        @NotNull(message = "La cuenta destino es obligatoria")
        UUID destinationAccountId,

        @Schema(description = "Importe (positivo, max 1000000.00, 2 decimales)", example = "150.00")
        @NotNull(message = "El importe es obligatorio")
        @Positive(message = "El importe debe ser positivo")
        @Digits(integer = 17, fraction = 2, message = "El importe excede la precisión permitida")
        @DecimalMax(value = "1000000.00", message = "El importe supera el máximo por transferencia")
        BigDecimal amount) {
}

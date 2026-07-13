package com.hexabank.account.infrastructure.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * DTO de entrada del endpoint de creación de cuenta.
 *
 * <p>Vive en infraestructura: es el formato de transporte (JSON) y lleva las anotaciones de
 * Bean Validation. El controller lo traduce al comando de aplicación con tipos de dominio. Así la
 * validación de forma queda en el borde y el dominio solo recibe datos ya saneados.</p>
 */
@Schema(description = "Peticion de alta de cuenta")
public record CreateAccountRequest(

        @Schema(description = "Titular de la cuenta", example = "Alice")
        @NotBlank(message = "El titular es obligatorio")
        String holder,

        @Schema(description = "Saldo inicial (>= 0)", example = "1000.00")
        @NotNull(message = "El saldo inicial es obligatorio")
        @PositiveOrZero(message = "El saldo inicial no puede ser negativo")
        BigDecimal initialBalance) {
}

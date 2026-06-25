package com.hexabank.account.infrastructure.rest.dto;

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
public record CreateAccountRequest(

        @NotBlank(message = "El titular es obligatorio")
        String holder,

        @NotNull(message = "El saldo inicial es obligatorio")
        @PositiveOrZero(message = "El saldo inicial no puede ser negativo")
        BigDecimal initialBalance) {
}

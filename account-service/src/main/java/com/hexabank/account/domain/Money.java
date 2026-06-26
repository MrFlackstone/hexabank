package com.hexabank.account.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Value Object que representa una cantidad de dinero en EUR.
 *
 * <p>Es un {@code record} inmutable: una vez creado no cambia, por lo que es seguro compartirlo
 * entre hilos. Encapsula sus invariantes en el constructor compacto, evitando el
 * <em>primitive obsession</em> de pasar {@link BigDecimal} sueltos por todo el dominio.</p>
 *
 * <p>Invariantes garantizadas:</p>
 * <ul>
 *   <li>{@code amount} no nulo.</li>
 *   <li>Escala &le; 2 (los céntimos son la unidad mínima del EUR).</li>
 *   <li>Importe no negativo (un saldo o un importe de operación nunca es {@code < 0}).</li>
 * </ul>
 *
 * <p>Moneda fija EUR (sin conversión de divisas).</p>
 */
public record Money(BigDecimal amount) {

    /** Cantidad neutra (0,00 €), útil como saldo inicial por defecto. */
    public static final Money ZERO = new Money(BigDecimal.ZERO.setScale(2));

    /** Constructor compacto: valida y normaliza la escala a 2 decimales. */
    public Money {
        Objects.requireNonNull(amount, "El importe no puede ser nulo");
        if (amount.scale() > 2) {
            throw new IllegalArgumentException(
                    "El importe no puede tener más de 2 decimales (céntimos): " + amount);
        }
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("El importe no puede ser negativo: " + amount);
        }
        amount = amount.setScale(2);
    }

    /** Factoría de conveniencia desde un {@link BigDecimal}. */
    public static Money of(BigDecimal amount) {
        return new Money(amount);
    }

    /** Factoría de conveniencia desde un valor numérico simple (p. ej. en tests). */
    public static Money of(String amount) {
        return new Money(new BigDecimal(amount));
    }

    /** Devuelve una nueva instancia con la suma (inmutabilidad: no muta {@code this}). */
    public Money add(Money other) {
        return new Money(this.amount.add(other.amount));
    }

    /**
     * Devuelve una nueva instancia con la resta. La invariante de no-negatividad del constructor
     * impide construir un resultado negativo: la regla de "fondos suficientes" la decide el
     * agregado {@link Account}, no este VO.
     */
    public Money subtract(Money other) {
        return new Money(this.amount.subtract(other.amount));
    }

    /** {@code true} si este importe es estrictamente menor que {@code other}. */
    public boolean isLessThan(Money other) {
        return this.amount.compareTo(other.amount) < 0;
    }
}

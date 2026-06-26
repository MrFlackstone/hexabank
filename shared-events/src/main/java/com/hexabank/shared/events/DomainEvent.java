package com.hexabank.shared.events;

import java.util.UUID;

/**
 * Contrato base de los eventos de dominio que viajan por Kafka entre servicios.
 *
 * <p>Es una {@code sealed interface}: la lista de eventos permitidos es cerrada y explícita, lo que
 * permite el pattern matching exhaustivo de Java 21 en los consumidores. Este módulo es Java puro:
 * sin Spring, JPA ni Kafka.</p>
 *
 * <p>Todo evento expone {@link #eventId()} (identificador único del mensaje) y {@link #transferId()}
 * (identificador de la transferencia a la que pertenece).</p>
 */
public sealed interface DomainEvent
        permits DebitRequested, CreditRequested, RefundRequested,
                MoneyDebited, DebitFailed, MoneyCredited, CreditFailed, MoneyRefunded,
                TransferCompleted, TransferFailed {

    /** Identificador único del evento. */
    UUID eventId();

    /** Identificador de la transferencia a la que pertenece el evento. */
    UUID transferId();
}

package com.hexabank.shared.events;

import java.util.UUID;

/**
 * Contrato base de los eventos de dominio que viajan por Kafka entre servicios.
 *
 * <p>Es una {@code sealed interface}: la lista de eventos permitidos es cerrada y explícita, lo que
 * permite el pattern matching exhaustivo de Java 21 en los consumidores. Este módulo es Java puro:
 * sin Spring, JPA ni Kafka.</p>
 *
 * <p>Todo evento expone {@link #eventId()} (identificador único del mensaje). Los eventos que
 * participan en la saga de transferencia extienden {@link SagaEvent}, que añade el identificador
 * de la transferencia.</p>
 */
public sealed interface DomainEvent permits SagaEvent, AccountOpened {

    /** Identificador único del evento. */
    UUID eventId();
}

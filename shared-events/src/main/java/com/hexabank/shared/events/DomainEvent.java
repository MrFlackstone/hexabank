package com.hexabank.shared.events;

/**
 * Contrato base de todos los eventos de dominio que viajan por Kafka entre servicios.
 *
 * <p>Es una {@code sealed interface}: la lista de eventos permitidos es cerrada y explicita,
 * de modo que el compilador obliga a tratar todos los casos en los {@code switch} de los
 * consumidores (pattern matching exhaustivo de Java 21). Esto evita eventos "huerfanos"
 * sin manejar.</p>
 *
 * <p>En la Fase 0 solo se establece el modulo y el paquete. Los records concretos
 * (p. ej. {@code DebitRequested}, {@code MoneyDebited}, {@code TransferCompleted}) se anaden
 * a la clausula {@code permits} en la Fase 2, cuando se implemente la saga Kafka.</p>
 */
public sealed interface DomainEvent permits DomainEvent.Placeholder {

    /**
     * Marcador temporal para que la interfaz selle correctamente sin eventos aun definidos.
     * Se elimina en la Fase 2 al introducir los eventos reales de la saga.
     */
    record Placeholder() implements DomainEvent {
    }
}

package com.hexabank.account.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hexabank.account.application.port.out.EventPublisherPort;
import com.hexabank.account.infrastructure.persistence.OutboxJpaEntity;
import com.hexabank.account.infrastructure.persistence.OutboxJpaRepository;
import com.hexabank.shared.events.DomainEvent;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Adaptador de salida del puerto {@link EventPublisherPort} que implementa el patrón Outbox: en lugar
 * de enviar a Kafka, serializa el evento e inserta una fila en la tabla {@code outbox}.
 *
 * <p>Como se invoca dentro de la transacción del caso de uso, la fila se persiste de forma atómica con
 * el cambio de saldo: si la transacción hace rollback, no queda evento huérfano. El
 * {@code OutboxRelay} publica la fila a Kafka después del commit.</p>
 */
@Component
public class OutboxEventPublisher implements EventPublisherPort {

    private final OutboxJpaRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OutboxEventPublisher(OutboxJpaRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(DomainEvent event) {
        outboxRepository.save(new OutboxJpaEntity(
                UUID.randomUUID(),
                event.eventId(),
                EventRouting.topicFor(event),
                EventRouting.keyFor(event),
                event.getClass().getName(),
                serialize(event)));
    }

    private String serialize(DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "No se pudo serializar el evento para la outbox: " + event.getClass().getSimpleName(), e);
        }
    }
}

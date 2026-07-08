package com.hexabank.account.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hexabank.account.infrastructure.persistence.OutboxJpaEntity;
import com.hexabank.account.infrastructure.persistence.OutboxJpaRepository;
import com.hexabank.shared.events.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Relay del patrón Outbox: sondea periódicamente las filas de {@code outbox} aún no publicadas, las
 * envía a Kafka con las mismas cabeceras de tipo que usaba la publicación directa y marca
 * {@code published_at}.
 *
 * <p>Es at-least-once: si el proceso cae tras enviar y antes de marcar la fila, el siguiente sondeo
 * reenvía el evento; el consumidor lo deduplica por {@code eventId} contra {@code processed_events}.
 * Un fallo de envío deja la fila pendiente para el próximo ciclo, sin bloquear al resto del lote.</p>
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxJpaRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OutboxRelay(OutboxJpaRepository outboxRepository,
                       KafkaTemplate<String, Object> kafkaTemplate,
                       ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${hexabank.outbox.poll-interval-ms:200}")
    public void publishPending() {
        List<OutboxJpaEntity> pending = outboxRepository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();
        for (OutboxJpaEntity row : pending) {
            try {
                DomainEvent event = objectMapper.readValue(row.getPayload(), eventClass(row.getEventType()));
                kafkaTemplate.send(row.getTopic(), row.getPartitionKey(), event).get();
                row.markPublished(OffsetDateTime.now());
                outboxRepository.save(row);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.warn("Outbox relay: fallo publicando eventId={}, se reintentará en el próximo sondeo",
                        row.getEventId(), e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Class<? extends DomainEvent> eventClass(String fqn) throws ClassNotFoundException {
        return (Class<? extends DomainEvent>) Class.forName(fqn);
    }
}

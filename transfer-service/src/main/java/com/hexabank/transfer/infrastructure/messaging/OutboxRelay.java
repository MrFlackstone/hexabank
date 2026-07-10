package com.hexabank.transfer.infrastructure.messaging;

import com.hexabank.shared.events.DomainEvent;
import com.hexabank.transfer.infrastructure.persistence.OutboxJpaEntity;
import com.hexabank.transfer.infrastructure.persistence.OutboxJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
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
 *
 * <p><strong>Tracing (Fase 12):</strong> reinyecta el {@code traceparent} capturado al encolar como
 * cabecera Kafka, de modo que el consumidor continúe la misma traza pese al salto de hilo
 * outbox -&gt; relay. La observación del productor está desactivada en este {@code KafkaTemplate} para
 * que no sobrescriba esa cabecera con el contexto del hilo del scheduler (que sería otra traza).</p>
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    /** Cabecera W3C de contexto de traza; el consumidor la lee para continuar la traza original. */
    private static final String TRACEPARENT_HEADER = "traceparent";

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
                kafkaTemplate.send(toRecord(row, event)).get();
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

    /**
     * Construye el {@link ProducerRecord} y, si la fila guardó un {@code traceparent}, lo añade como
     * cabecera para que el consumidor prolongue la traza original a través del broker.
     */
    private ProducerRecord<String, Object> toRecord(OutboxJpaEntity row, DomainEvent event) {
        ProducerRecord<String, Object> record =
                new ProducerRecord<>(row.getTopic(), row.getPartitionKey(), event);
        String traceparent = row.getTraceparent();
        if (traceparent != null && !traceparent.isBlank()) {
            record.headers().add(TRACEPARENT_HEADER, traceparent.getBytes(StandardCharsets.UTF_8));
        }
        return record;
    }

    @SuppressWarnings("unchecked")
    private Class<? extends DomainEvent> eventClass(String fqn) throws ClassNotFoundException {
        return (Class<? extends DomainEvent>) Class.forName(fqn);
    }
}

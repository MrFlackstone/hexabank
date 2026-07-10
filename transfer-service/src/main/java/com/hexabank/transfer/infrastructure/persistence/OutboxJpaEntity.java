package com.hexabank.transfer.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidad JPA de la tabla {@code outbox}: un evento de dominio pendiente (o ya) publicado a Kafka.
 *
 * <p>La fila se inserta en la misma transacción que el cambio de estado del agregado, cerrando el
 * dual-write. El {@code OutboxRelay} la publica después del commit y sella {@link #publishedAt}.</p>
 */
@Entity
@Table(name = "outbox")
public class OutboxJpaEntity {

    @Id
    private UUID id;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(nullable = false, length = 100)
    private String topic;

    @Column(name = "partition_key", length = 100)
    private String partitionKey;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    // Fase 12: traceparent W3C capturado al encolar; el relay lo reinyecta como cabecera Kafka.
    @Column(name = "traceparent", length = 64)
    private String traceparent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    /** Constructor requerido por JPA. */
    protected OutboxJpaEntity() {
    }

    public OutboxJpaEntity(UUID id, UUID eventId, String topic, String partitionKey,
                           String eventType, String payload, String traceparent) {
        this.id = id;
        this.eventId = eventId;
        this.topic = topic;
        this.partitionKey = partitionKey;
        this.eventType = eventType;
        this.payload = payload;
        this.traceparent = traceparent;
    }

    public void markPublished(OffsetDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getTopic() {
        return topic;
    }

    public String getPartitionKey() {
        return partitionKey;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public String getTraceparent() {
        return traceparent;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }
}

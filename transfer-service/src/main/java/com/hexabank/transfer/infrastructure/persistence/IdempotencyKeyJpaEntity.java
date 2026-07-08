package com.hexabank.transfer.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidad JPA de la tabla {@code idempotency_keys}: asocia una clave de idempotencia de cliente al
 * transfer creado y guarda el hash del payload para detectar reúsos con datos distintos.
 */
@Entity
@Table(name = "idempotency_keys")
public class IdempotencyKeyJpaEntity {

    @Id
    @Column(name = "idempotency_key", length = 255)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "transfer_id", nullable = false)
    private UUID transferId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /** Constructor requerido por JPA. */
    protected IdempotencyKeyJpaEntity() {
    }

    public IdempotencyKeyJpaEntity(String idempotencyKey, String requestHash, UUID transferId) {
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        this.transferId = transferId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public UUID getTransferId() {
        return transferId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}

package com.hexabank.account.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidad JPA de la tabla {@code refunded_transfers}: cada fila es una transferencia ya reembolsada.
 */
@Entity
@Table(name = "refunded_transfers")
public class RefundedTransferJpaEntity {

    @Id
    @Column(name = "transfer_id")
    private UUID transferId;

    @CreationTimestamp
    @Column(name = "refunded_at", nullable = false, updatable = false)
    private OffsetDateTime refundedAt;

    /** Constructor requerido por JPA. */
    protected RefundedTransferJpaEntity() {
    }

    public RefundedTransferJpaEntity(UUID transferId) {
        this.transferId = transferId;
    }

    public UUID getTransferId() {
        return transferId;
    }

    public OffsetDateTime getRefundedAt() {
        return refundedAt;
    }
}

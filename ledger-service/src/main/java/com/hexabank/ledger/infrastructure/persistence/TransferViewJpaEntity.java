package com.hexabank.ledger.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidad JPA de la tabla {@code transfer_projection} (modelo de lectura por transferencia).
 * {@code updated_at} lo gestiona Hibernate en cada inserción/actualización.
 */
@Entity
@Table(name = "transfer_projection")
public class TransferViewJpaEntity {

    @Id
    @Column(name = "transfer_id")
    private UUID transferId;

    @Column(name = "source_account_id")
    private UUID sourceAccountId;

    @Column(name = "destination_account_id")
    private UUID destinationAccountId;

    @Column
    private BigDecimal amount;

    @Column(nullable = false)
    private String status;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /** Constructor requerido por JPA. */
    protected TransferViewJpaEntity() {
    }

    public TransferViewJpaEntity(UUID transferId, UUID sourceAccountId, UUID destinationAccountId,
                                 BigDecimal amount, String status) {
        this.transferId = transferId;
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
        this.amount = amount;
        this.status = status;
    }

    public UUID getTransferId() {
        return transferId;
    }

    public UUID getSourceAccountId() {
        return sourceAccountId;
    }

    public UUID getDestinationAccountId() {
        return destinationAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getStatus() {
        return status;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}

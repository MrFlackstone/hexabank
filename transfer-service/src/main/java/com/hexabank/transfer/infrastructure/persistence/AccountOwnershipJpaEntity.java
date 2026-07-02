package com.hexabank.transfer.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidad JPA de la proyección {@code account_ownership}: propietario de cada cuenta según los
 * eventos {@code AccountOpened}. La PK es el propio {@code account_id}: re-guardar es un upsert.
 */
@Entity
@Table(name = "account_ownership")
public class AccountOwnershipJpaEntity {

    @Id
    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "owner_id", nullable = false, length = 64)
    private String ownerId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /** Constructor requerido por JPA. */
    protected AccountOwnershipJpaEntity() {
    }

    public AccountOwnershipJpaEntity(UUID accountId, String ownerId) {
        this.accountId = accountId;
        this.ownerId = ownerId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public String getOwnerId() {
        return ownerId;
    }
}

package com.hexabank.transfer.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

/**
 * Repositorio Spring Data JPA sobre {@link TransferJpaEntity}. Extiende
 * {@link JpaSpecificationExecutor} para las búsquedas paginadas con filtros componibles.
 */
public interface TransferJpaRepository
        extends JpaRepository<TransferJpaEntity, UUID>, JpaSpecificationExecutor<TransferJpaEntity> {
}

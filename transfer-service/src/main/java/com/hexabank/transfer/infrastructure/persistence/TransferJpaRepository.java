package com.hexabank.transfer.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Repositorio Spring Data JPA sobre {@link TransferJpaEntity}. Extiende
 * {@link JpaSpecificationExecutor} para las búsquedas paginadas con filtros componibles.
 */
public interface TransferJpaRepository
        extends JpaRepository<TransferJpaEntity, UUID>, JpaSpecificationExecutor<TransferJpaEntity> {

    /** Transferencias en alguno de los estados dados creadas antes del corte (para el reaper). */
    List<TransferJpaEntity> findByStatusInAndCreatedAtBefore(Collection<String> statuses, OffsetDateTime cutoff);
}

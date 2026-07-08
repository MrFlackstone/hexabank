package com.hexabank.account.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio Spring Data JPA sobre {@link OutboxJpaEntity}. El relay lee por lotes las filas aún no
 * publicadas en orden de creación.
 */
public interface OutboxJpaRepository extends JpaRepository<OutboxJpaEntity, UUID> {

    List<OutboxJpaEntity> findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();
}

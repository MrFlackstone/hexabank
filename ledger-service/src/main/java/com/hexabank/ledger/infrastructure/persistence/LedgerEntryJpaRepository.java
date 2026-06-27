package com.hexabank.ledger.infrastructure.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio Spring Data JPA sobre {@link LedgerEntryJpaEntity}.
 */
public interface LedgerEntryJpaRepository extends JpaRepository<LedgerEntryJpaEntity, UUID> {

    List<LedgerEntryJpaEntity> findByOrderByCreatedAtDesc(Pageable pageable);

    List<LedgerEntryJpaEntity> findByAccountIdOrderByCreatedAtDesc(UUID accountId, Pageable pageable);
}

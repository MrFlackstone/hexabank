package com.hexabank.ledger.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repositorio Spring Data JPA sobre {@link TransferViewJpaEntity}.
 */
public interface TransferViewJpaRepository extends JpaRepository<TransferViewJpaEntity, UUID> {
}

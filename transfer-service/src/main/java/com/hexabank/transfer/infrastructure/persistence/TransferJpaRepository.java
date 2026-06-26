package com.hexabank.transfer.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repositorio Spring Data JPA sobre {@link TransferJpaEntity}.
 */
public interface TransferJpaRepository extends JpaRepository<TransferJpaEntity, UUID> {
}

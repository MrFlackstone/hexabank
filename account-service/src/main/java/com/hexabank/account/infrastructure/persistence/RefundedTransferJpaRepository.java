package com.hexabank.account.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repositorio Spring Data JPA sobre {@link RefundedTransferJpaEntity}.
 */
public interface RefundedTransferJpaRepository extends JpaRepository<RefundedTransferJpaEntity, UUID> {
}

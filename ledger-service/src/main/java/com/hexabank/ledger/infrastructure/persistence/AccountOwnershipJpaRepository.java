package com.hexabank.ledger.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AccountOwnershipJpaRepository extends JpaRepository<AccountOwnershipJpaEntity, UUID> {
}

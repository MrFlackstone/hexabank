package com.hexabank.transfer.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AccountOwnershipJpaRepository extends JpaRepository<AccountOwnershipJpaEntity, UUID> {

    List<AccountOwnershipJpaEntity> findByOwnerId(String ownerId);
}

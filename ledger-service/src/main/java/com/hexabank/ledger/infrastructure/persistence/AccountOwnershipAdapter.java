package com.hexabank.ledger.infrastructure.persistence;

import com.hexabank.ledger.application.port.out.AccountOwnershipPort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de salida que implementa la proyección de propietarios sobre Spring Data JPA.
 */
@Component
public class AccountOwnershipAdapter implements AccountOwnershipPort {

    private final AccountOwnershipJpaRepository repository;

    public AccountOwnershipAdapter(AccountOwnershipJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void register(UUID accountId, String ownerId) {
        repository.save(new AccountOwnershipJpaEntity(accountId, ownerId));
    }

    @Override
    public Optional<String> ownerOf(UUID accountId) {
        return repository.findById(accountId).map(AccountOwnershipJpaEntity::getOwnerId);
    }
}

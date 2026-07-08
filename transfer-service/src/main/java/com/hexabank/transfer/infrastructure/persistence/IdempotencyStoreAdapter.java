package com.hexabank.transfer.infrastructure.persistence;

import com.hexabank.transfer.application.port.out.IdempotencyStorePort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de salida que implementa el almacén de claves de idempotencia sobre la tabla
 * {@code idempotency_keys}.
 */
@Component
public class IdempotencyStoreAdapter implements IdempotencyStorePort {

    private final IdempotencyKeyJpaRepository repository;

    public IdempotencyStoreAdapter(IdempotencyKeyJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<StoredIdempotencyKey> find(String idempotencyKey) {
        return repository.findById(idempotencyKey)
                .map(entity -> new StoredIdempotencyKey(entity.getRequestHash(), entity.getTransferId()));
    }

    @Override
    public void save(String idempotencyKey, String requestHash, UUID transferId) {
        repository.save(new IdempotencyKeyJpaEntity(idempotencyKey, requestHash, transferId));
    }
}

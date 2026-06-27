package com.hexabank.ledger.infrastructure.persistence;

import com.hexabank.ledger.application.port.out.TransferViewRepositoryPort;
import com.hexabank.ledger.domain.TransferView;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de salida de la proyección de transferencias sobre Spring Data JPA. El {@code save} es un
 * upsert por clave primaria ({@code transferId}).
 */
@Component
public class TransferViewPersistenceAdapter implements TransferViewRepositoryPort {

    private final TransferViewJpaRepository repository;

    public TransferViewPersistenceAdapter(TransferViewJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<TransferView> find(UUID transferId) {
        return repository.findById(transferId).map(this::toDomain);
    }

    @Override
    public void save(TransferView view) {
        repository.save(toEntity(view));
    }

    private TransferView toDomain(TransferViewJpaEntity entity) {
        return new TransferView(
                entity.getTransferId(),
                entity.getSourceAccountId(),
                entity.getDestinationAccountId(),
                entity.getAmount(),
                entity.getStatus());
    }

    private TransferViewJpaEntity toEntity(TransferView view) {
        return new TransferViewJpaEntity(
                view.transferId(),
                view.sourceAccountId(),
                view.destinationAccountId(),
                view.amount(),
                view.status());
    }
}

package com.hexabank.transfer.infrastructure.persistence;

import com.hexabank.transfer.application.port.out.LoadTransferPort;
import com.hexabank.transfer.application.port.out.SaveTransferPort;
import com.hexabank.transfer.domain.Transfer;
import com.hexabank.transfer.domain.TransferId;
import com.hexabank.transfer.domain.TransferStatus;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Adaptador de salida que implementa los puertos de persistencia del dominio sobre Spring Data JPA.
 *
 * <p>Traduce en ambos sentidos entre el agregado {@link Transfer} y la entidad {@link TransferJpaEntity}.</p>
 */
@Component
public class TransferPersistenceAdapter implements LoadTransferPort, SaveTransferPort {

    private final TransferJpaRepository repository;

    public TransferPersistenceAdapter(TransferJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Transfer> findById(TransferId id) {
        return repository.findById(id.value()).map(this::toDomain);
    }

    @Override
    public Transfer save(Transfer transfer) {
        TransferJpaEntity saved = repository.saveAndFlush(toEntity(transfer));
        return toDomain(saved);
    }

    private Transfer toDomain(TransferJpaEntity entity) {
        return new Transfer(
                new TransferId(entity.getId()),
                entity.getSourceAccountId(),
                entity.getDestinationAccountId(),
                entity.getAmount(),
                TransferStatus.valueOf(entity.getStatus()),
                entity.getVersion());
    }

    private TransferJpaEntity toEntity(Transfer transfer) {
        return new TransferJpaEntity(
                transfer.id().value(),
                transfer.sourceAccountId(),
                transfer.destinationAccountId(),
                transfer.amount(),
                transfer.status().name(),
                transfer.version());
    }
}

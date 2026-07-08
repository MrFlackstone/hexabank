package com.hexabank.transfer.infrastructure.persistence;

import com.hexabank.transfer.application.port.in.PageResult;
import com.hexabank.transfer.application.port.out.LoadTransferPort;
import com.hexabank.transfer.application.port.out.SaveTransferPort;
import com.hexabank.transfer.application.port.out.SearchTransfersPort;
import com.hexabank.transfer.application.port.out.StaleTransferPort;
import com.hexabank.transfer.application.port.out.TransferSearchCriteria;
import com.hexabank.transfer.domain.Transfer;
import com.hexabank.transfer.domain.TransferId;
import com.hexabank.transfer.domain.TransferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Adaptador de salida que implementa los puertos de persistencia del dominio sobre Spring Data JPA.
 *
 * <p>Traduce en ambos sentidos entre el agregado {@link Transfer} y la entidad {@link TransferJpaEntity},
 * y las búsquedas paginadas de {@link SearchTransfersPort} a Specifications + {@code PageRequest}.</p>
 */
@Component
public class TransferPersistenceAdapter
        implements LoadTransferPort, SaveTransferPort, SearchTransfersPort, StaleTransferPort {

    private final TransferJpaRepository repository;

    public TransferPersistenceAdapter(TransferJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Transfer> findById(TransferId id) {
        return repository.findById(id.value()).map(this::toDomain);
    }

    @Override
    public List<Transfer> findStale(Set<TransferStatus> statuses, Instant olderThan) {
        List<String> statusNames = statuses.stream().map(Enum::name).toList();
        OffsetDateTime cutoff = olderThan.atOffset(ZoneOffset.UTC);
        return repository.findByStatusInAndCreatedAtBefore(statusNames, cutoff).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Transfer save(Transfer transfer) {
        TransferJpaEntity saved = repository.saveAndFlush(toEntity(transfer));
        return toDomain(saved);
    }

    @Override
    public PageResult<Transfer> search(TransferSearchCriteria criteria) {
        Specification<TransferJpaEntity> spec = Specification
                .allOf(TransferSpecifications.statusIs(criteria.status()),
                        TransferSpecifications.createdAtOrAfter(criteria.from()),
                        TransferSpecifications.createdAtOrBefore(criteria.to()),
                        TransferSpecifications.involvesAccounts(criteria.visibleAccountIds()));
        Sort sort = Sort.by(criteria.ascending() ? Sort.Direction.ASC : Sort.Direction.DESC,
                sortProperty(criteria));
        Page<TransferJpaEntity> page = repository.findAll(spec,
                PageRequest.of(criteria.page(), criteria.size(), sort));
        return new PageResult<>(page.map(this::toDomain).getContent(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    private String sortProperty(TransferSearchCriteria criteria) {
        return switch (criteria.sortBy()) {
            case CREATED_AT -> "createdAt";
            case AMOUNT -> "amount";
            case STATUS -> "status";
        };
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

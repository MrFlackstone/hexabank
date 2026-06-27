package com.hexabank.ledger.infrastructure.persistence;

import com.hexabank.ledger.application.port.out.LedgerEntryRepositoryPort;
import com.hexabank.ledger.domain.EntryType;
import com.hexabank.ledger.domain.LedgerEntry;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Adaptador de salida que implementa el libro mayor sobre Spring Data JPA. Traduce entre el agregado
 * {@link LedgerEntry} y la entidad {@link LedgerEntryJpaEntity}.
 */
@Component
public class LedgerEntryPersistenceAdapter implements LedgerEntryRepositoryPort {

    private final LedgerEntryJpaRepository repository;

    public LedgerEntryPersistenceAdapter(LedgerEntryJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void append(LedgerEntry entry) {
        repository.save(toEntity(entry));
    }

    @Override
    public List<LedgerEntry> findRecent(int limit) {
        return repository.findByOrderByCreatedAtDesc(PageRequest.of(0, limit))
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<LedgerEntry> findByAccount(UUID accountId, int limit) {
        return repository.findByAccountIdOrderByCreatedAtDesc(accountId, PageRequest.of(0, limit))
                .stream().map(this::toDomain).toList();
    }

    private LedgerEntry toDomain(LedgerEntryJpaEntity entity) {
        return new LedgerEntry(
                entity.getId(),
                entity.getTransferId(),
                entity.getAccountId(),
                EntryType.valueOf(entity.getEntryType()),
                entity.getAmount(),
                entity.getCreatedAt());
    }

    private LedgerEntryJpaEntity toEntity(LedgerEntry entry) {
        return new LedgerEntryJpaEntity(
                entry.entryId(),
                entry.transferId(),
                entry.accountId(),
                entry.type().name(),
                entry.amount(),
                entry.createdAt());
    }
}

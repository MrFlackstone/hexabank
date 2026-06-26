package com.hexabank.transfer.infrastructure.persistence;

import com.hexabank.transfer.application.port.out.ProcessedEventPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Adaptador de salida que implementa la idempotencia sobre la tabla {@code processed_events}.
 */
@Component
public class ProcessedEventAdapter implements ProcessedEventPort {

    private final ProcessedEventJpaRepository repository;

    public ProcessedEventAdapter(ProcessedEventJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean alreadyProcessed(UUID eventId) {
        return repository.existsById(eventId);
    }

    @Override
    public void markProcessed(UUID eventId) {
        repository.save(new ProcessedEventJpaEntity(eventId));
    }
}

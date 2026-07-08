package com.hexabank.account.infrastructure.persistence;

import com.hexabank.account.application.port.out.RefundedTransferPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Adaptador de salida que implementa la idempotencia del reembolso sobre la tabla
 * {@code refunded_transfers}.
 */
@Component
public class RefundedTransferAdapter implements RefundedTransferPort {

    private final RefundedTransferJpaRepository repository;

    public RefundedTransferAdapter(RefundedTransferJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean alreadyRefunded(UUID transferId) {
        return repository.existsById(transferId);
    }

    @Override
    public void markRefunded(UUID transferId) {
        repository.save(new RefundedTransferJpaEntity(transferId));
    }
}

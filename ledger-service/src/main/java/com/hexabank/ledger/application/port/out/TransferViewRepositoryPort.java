package com.hexabank.ledger.application.port.out;

import com.hexabank.ledger.domain.TransferView;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida de persistencia de la proyección de transferencias (upsert por {@code transferId}).
 */
public interface TransferViewRepositoryPort {

    Optional<TransferView> find(UUID transferId);

    void save(TransferView view);
}

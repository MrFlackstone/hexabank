package com.hexabank.transfer.application.port.out;

import com.hexabank.transfer.domain.Transfer;
import com.hexabank.transfer.domain.TransferId;

import java.util.Optional;

/**
 * Puerto de salida: cargar una transferencia desde el almacén de persistencia.
 */
public interface LoadTransferPort {

    Optional<Transfer> findById(TransferId id);
}

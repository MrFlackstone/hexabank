package com.hexabank.transfer.application.port.out;

import com.hexabank.transfer.domain.Transfer;

/**
 * Puerto de salida: persistir una transferencia (alta o actualización de estado).
 */
public interface SaveTransferPort {

    Transfer save(Transfer transfer);
}
